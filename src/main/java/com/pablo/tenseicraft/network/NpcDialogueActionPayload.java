package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import com.pablo.tenseicraft.entities.TownswomanNpcEntity;
import com.pablo.tenseicraft.events.ModAttributesHandler;
import com.pablo.tenseicraft.social.NpcSocialManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record NpcDialogueActionPayload(int entityId, int actionType) implements CustomPacketPayload {

    public static final int ACTION_CHAT = 1;
    public static final int ACTION_GIFT = 2;
    public static final int ACTION_HIRE = 3;
    public static final int ACTION_TOGGLE_STAY = 4;
    public static final int ACTION_DISMISS = 5;
    public static final int ACTION_HIRE_WORKER = 6;
    public static final int ACTION_ARRIVE_BASE = 7;
    public static final int ACTION_EDIT_WORK = 8;
    public static final int ACTION_DISMISS_WORKER = 9;
    public static final int ACTION_OPEN_WORKER_INVENTORY = 10;
    public static final int ACTION_DEVOTION_PACT = 11;
    public static final int ACTION_MANA_BAPTISM = 12;
    public static final int ACTION_REGISTER_GUILD = 13;
    public static final int ACTION_ALICE_START_QUEST = 14;
    public static final int ACTION_ALICE_CLAIM_REWARD = 15;
    public static final int ACTION_ALICE_TECHNIQUES_GUIDE = 16;
    public static final int ACTION_GUILD_ASCEND = 17;
    public static final int ACTION_STANCE_FOLLOW = 18;
    public static final int ACTION_STANCE_BASE = 19;
    public static final int ACTION_STANCE_GUARD = 20;
    public static final int ACTION_GUILD_ASCEND_COMPANION = 21;
    public static final int ACTION_ALICE_TELEPORT_ABYSS = 22;
    public static final int ACTION_ALICE_START_CULTIST_QUEST = 23;
    public static final int ACTION_ALICE_CLAIM_CULTIST_REWARD = 24;
    public static final int ACTION_OPEN_COMPANION_INVENTORY = 25;
    public static final int ACTION_ALICE_HIRE_SUPPORT = 26;
    public static final int ACTION_ALICE_BUY_REPLACEMENT_SWORD = 27;
    public static final int ACTION_ALICE_REQUEST_WARD = 28;
    public static final int ACTION_ALICE_REROLL_SOUL_BLADE = 29;
    public static final int ACTION_MEGU_REFORGE = 30;
    public static final int ACTION_MEGU_BUY_MAGIC_ANVIL = 31;
    public static final int ACTION_REIMU_CHALLENGE = 32;
    public static final int ACTION_REDFOX_TRAIN_PRIMORDIAL_FURY = 33;

    public static final Type<NpcDialogueActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "npc_dialogue_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NpcDialogueActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, NpcDialogueActionPayload::entityId,
                    ByteBufCodecs.VAR_INT, NpcDialogueActionPayload::actionType,
                    NpcDialogueActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NpcDialogueActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            Entity entity = player.serverLevel().getEntity(payload.entityId());
            if (entity == null || entity.distanceToSqr(player) > 64.0) {
                return;
            }

            if (entity instanceof AdventurerNpcEntity adventurer) {
                handleAdventurerAction(player, adventurer, payload.actionType());
            } else if (entity instanceof TownswomanNpcEntity townswoman) {
                handleTownswomanAction(player, townswoman, payload.actionType());
            } else if (entity instanceof Villager villager) {
                handleVillagerAction(player, villager, payload.actionType());
            } else if (entity instanceof com.pablo.tenseicraft.entities.GuildMasterNpcEntity guildMaster) {
                handleGuildMasterAction(player, guildMaster, payload.actionType());
            } else if (entity instanceof com.pablo.tenseicraft.entities.AliceNpcEntity alice) {
                handleAliceAction(player, alice, payload.actionType());
            } else if (entity instanceof com.pablo.tenseicraft.entities.RedfoxEntity redfox) {
                handleRedfoxAction(player, redfox, payload.actionType());
            } else if (entity instanceof com.pablo.tenseicraft.entities.MeguNpcEntity megu) {
                handleMeguAction(player, megu, payload.actionType());
            } else if (entity instanceof com.pablo.tenseicraft.entities.ReimuNpcEntity reimu) {
                handleReimuAction(player, reimu, payload.actionType());
            }
        });
    }

    private static void handleAdventurerAction(ServerPlayer player, AdventurerNpcEntity adventurer, int action) {
        String npcName = adventurer.hasCustomName() ? adventurer.getCustomName().getString() : "Aventurero";
        String playerName = player.getName().getString();
        int currentAffinity = adventurer.getAffinity(player);
        AdventurerRank rank = adventurer.getRank();

        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                if (adventurer.hasPendingFriendshipEvent(player)) {
                    adventurer.setEternalComrade(true);
                    adventurer.setStance(com.pablo.tenseicraft.entities.CompanionStance.FOLLOW);
                    player.serverLevel().playSound(null, adventurer.getX(), adventurer.getY(), adventurer.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    if (player.serverLevel() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, adventurer.getX(), adventurer.getY() + 1.5, adventurer.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, adventurer.getX(), adventurer.getY() + 1.0, adventurer.getZ(), 25, 0.4, 0.5, 0.4, 0.1);
                    }
                    responseDialogue = "§6\"Hemos compartido tantas batallas, peligros y victorias que ya no te veo como un empleador... Te has convertido en mi mejor amigo, en mi verdadera familia. A partir de hoy, jamás volveré a cobrarte una sola esmeralda. Marcharé contigo hasta el fin del mundo. Además, en estas tierras los plebeyos no portamos nombre... ¿Me concederías el honor de darme uno y acogerme formalmente en tu linaje?\"";
                    player.sendSystemMessage(Component.literal("§6§l[HERMANDAD ETERNA] §e¡Has sellado un vínculo sagrado con tu camarada! Ahora te seguirá gratis para siempre y puedes asignarle un nombre propio."));
                } else {
                    boolean canChat = adventurer.canChat(player);
                    if (canChat) {
                        adventurer.recordChat(player);
                        adventurer.addAffinity(player, 3);
                        currentAffinity = adventurer.getAffinity(player);
                        NpcSocialManager.spawnAffinityEffects(adventurer, false);
                    }
                    responseDialogue = NpcSocialManager.getChatDialogue("adventurer", currentAffinity, rank, npcName, playerName);
                }
            }
            case ACTION_GIFT -> {
                ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                NpcSocialManager.GiftResult result = NpcSocialManager.evaluateGift(heldItem, adventurer.getGiftPersonalityId(), true);
                if (result.affinityGain() > 0 && !heldItem.isEmpty()) {
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    adventurer.addAffinity(player, result.affinityGain());
                    currentAffinity = adventurer.getAffinity(player);
                    NpcSocialManager.spawnAffinityEffects(adventurer, result.isLoved());
                } else {
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                responseDialogue = result.responseDialogue();
            }
            case ACTION_HIRE -> {
                com.pablo.tenseicraft.data.PlayerRpgData rpgData = player.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA);
                if (rpgData != null && rpgData.getReputationTier() == com.pablo.tenseicraft.data.ReputationTier.PUBLIC_ENEMY) {
                    responseDialogue = "§4\"¡No acepto contratos de criminales ni forajidos! Aléjate de mí antes de que desenvaine mi espada.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else if (adventurer.isHired() && !adventurer.isHiredBy(player)) {
                    responseDialogue = "§c\"Lo siento, ya tengo un contrato activo con otro aventurero. ¡Búscame cuando quede libre!\"";
                } else {
                    int hireCost = adventurer.getRank().getHireCost();
                    int emeraldCount = countEmeralds(player);
                    if (emeraldCount < hireCost && !player.isCreative()) {
                        responseDialogue = "§c\"Necesitas al menos " + hireCost + " Esmeralda" + (hireCost > 1 ? "s" : "") + " para formalizar mi contrato de escolta de " + adventurer.getRank().getShortBadge().getString() + " §cpor hoy.\"";
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    } else {
                        consumeEmeralds(player, hireCost);

                        adventurer.hire(player, 1); // 1 Minecraft Day
                        adventurer.addAffinity(player, 5);
                        currentAffinity = adventurer.getAffinity(player);

                        com.pablo.tenseicraft.advancements.ModAdvancements.award(player, com.pablo.tenseicraft.advancements.ModAdvancements.HIRE_ADVENTURER);

                        NpcSocialManager.spawnAffinityEffects(adventurer, true);
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

                        // Hero bonus dialogue vs standard
                        if (rpgData != null && rpgData.getReputationTier() == com.pablo.tenseicraft.data.ReputationTier.HERO_LEGENDARY) {
                            responseDialogue = "§6\"¡Es un inmenso honor marchar junto al Héroe de nuestras tierras! Daré mi vida por tu causa.\"";
                        } else {
                            responseDialogue = "§a\"¡Trato hecho, camarada! Cubriré tu retaguardia durante todo el día. Marca el paso y te seguiré.\"";
                        }

                        player.sendSystemMessage(Component.translatable("tenseicraft.social.hired_broadcast", npcName).withStyle(ChatFormatting.BOLD));
                    }
                }
            }
            case ACTION_TOGGLE_STAY -> {
                if (adventurer.isHiredBy(player)) {
                    boolean nowStaying = adventurer.toggleStaying();
                    if (nowStaying) {
                        responseDialogue = "§e\"Comprendido. Me quedaré en esta posición montando guardia hasta que me ordenes avanzar.\"";
                    } else {
                        responseDialogue = "§a\"¡De vuelta a la marcha! Te sigo de cerca.\"";
                    }
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            }
            case ACTION_DISMISS -> {
                if (adventurer.isHiredBy(player)) {
                    adventurer.dismissContract();
                    responseDialogue = "§f\"Entendido, disolvemos el contrato por ahora. ¡Fue un honor luchar a tu lado! Nos vemos en el camino.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
            }
            case ACTION_STANCE_FOLLOW -> {
                if (adventurer.isHiredBy(player)) {
                    adventurer.setStance(com.pablo.tenseicraft.entities.CompanionStance.FOLLOW);
                    responseDialogue = "§a\"¡A tus órdenes! Te sigo de cerca a donde vayas.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            }
            case ACTION_STANCE_BASE -> {
                if (adventurer.isHiredBy(player)) {
                    adventurer.setHomeBasePos(adventurer.blockPosition());
                    adventurer.setStance(com.pablo.tenseicraft.entities.CompanionStance.WANDER_BASE);
                    responseDialogue = "§b\"Comprendido. Me quedaré aquí en casa patrullando y descansando mientras estés fuera.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            }
            case ACTION_STANCE_GUARD -> {
                if (adventurer.isHiredBy(player)) {
                    adventurer.setStance(com.pablo.tenseicraft.entities.CompanionStance.STAY_GUARD);
                    responseDialogue = "§e\"¡Entendido! Montaré guardia en este punto exacto sin moverme.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            }
            case ACTION_OPEN_COMPANION_INVENTORY -> {
                if (adventurer.isHiredBy(player)) {
                    adventurer.openCompanionInventory(player);
                    return;
                }
            }
        }

        // Send back updated payload to keep dialogue screen open with live state
        long remainingTicks = adventurer.getHireRemainingTicks(player.level().getGameTime());
        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                adventurer.getId(),
                npcName,
                "adventurer",
                adventurer.getSkinVariant(),
                rank.getIndex(),
                currentAffinity,
                adventurer.isHired(),
                adventurer.isHiredBy(player),
                remainingTicks,
                adventurer.isStaying(),
                0,
                false,
                0,
                adventurer.isEternalComrade(),
                adventurer.getStance().getId(),
                adventurer.isReadyToAscend(),
                responseDialogue
        ));
    }

    private static void handleTownswomanAction(ServerPlayer player, TownswomanNpcEntity townswoman, int action) {
        String npcName = townswoman.hasCustomName() ? townswoman.getCustomName().getString() : "Aldeana";
        String playerName = player.getName().getString();
        int currentAffinity = townswoman.getAffinity(player);
        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                boolean canChat = townswoman.canChat(player);
                if (canChat) {
                    townswoman.recordChat(player);
                    townswoman.addAffinity(player, 3);
                    currentAffinity = townswoman.getAffinity(player);
                    NpcSocialManager.spawnAffinityEffects(townswoman, false);
                }
                responseDialogue = NpcSocialManager.getChatDialogue("townswoman", currentAffinity, AdventurerRank.F, npcName, playerName);
            }
            case ACTION_GIFT -> {
                ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                NpcSocialManager.GiftResult result = NpcSocialManager.evaluateGift(heldItem, false);
                if (result.affinityGain() > 0 && !heldItem.isEmpty()) {
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    townswoman.addAffinity(player, result.affinityGain());
                    currentAffinity = townswoman.getAffinity(player);
                    NpcSocialManager.spawnAffinityEffects(townswoman, result.isLoved());
                } else {
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                responseDialogue = result.responseDialogue();
            }
            case ACTION_HIRE_WORKER -> {
                responseDialogue = "§e\"Por el momento estoy dedicada a las labores comunales de la aldea. ¡Quizá en una próxima temporada pueda trabajar en tu base!\"";
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
            }
            case ACTION_ARRIVE_BASE -> {
                if (townswoman.isHiredBy(player) && townswoman.getWorkerStatus() == TownswomanNpcEntity.WorkerStatus.ESCORTING) {
                    townswoman.arrivedAtBase(player);
                    ItemStack cane = com.pablo.tenseicraft.items.OverseerCaneItem.createCaneForWorker(townswoman);
                    if (!player.getInventory().add(cane)) {
                        player.drop(cane, false);
                    }
                    com.pablo.tenseicraft.items.OverseerCaneItem.sendSetupGuide(player);
                    responseDialogue = "§a\"¡Excelente lugar para nuestra base! Toma este Bastón del Patrón. Delimita la zona de tala y asigna los cofres y mi cama para ponerme manos a la obra.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
                }
            }
            case ACTION_EDIT_WORK -> {
                if (townswoman.isHiredBy(player)) {
                    ItemStack cane = com.pablo.tenseicraft.items.OverseerCaneItem.createCaneForWorker(townswoman, true);
                    if (!player.getInventory().add(cane)) {
                        player.drop(cane, false);
                    }
                    responseDialogue = "§b\"Aquí tienes el Bastón del Patrón nuevamente. Ajusta el área o los cofres como consideres necesario.\"";
                    player.sendSystemMessage(Component.literal("§b[Bastón del Patrón] Recibido para reconfigurar a " + npcName + ".").withStyle(ChatFormatting.AQUA));
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.2F);
                }
            }
            case ACTION_DISMISS_WORKER -> {
                if (townswoman.isHiredBy(player)) {
                    townswoman.dismissWorkerContract(player, "despido voluntario");
                    responseDialogue = "§7\"Entendido, quedo liberada de mis labores. ¡Gracias por la oportunidad!\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
            }
            case ACTION_OPEN_WORKER_INVENTORY -> {
                if (townswoman.isHiredBy(player)) {
                    townswoman.openWorkerInventory(player);
                    return; // Container screen opened, skip resending dialogue packet
                }
            }
            case ACTION_DEVOTION_PACT -> {
                if (townswoman.isHiredBy(player) && !townswoman.isDevoted()) {
                    if (townswoman.getAffinity(player) >= 100) {
                        townswoman.setDevoted(true);
                        townswoman.setVassalRank(1);

                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                        player.serverLevel().playSound(null, townswoman.getX(), townswoman.getY(), townswoman.getZ(),
                                SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 0.8F, 1.1F);

                        if (player.serverLevel() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, townswoman.getX(), townswoman.getY() + 1.0, townswoman.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
                            sl.sendParticles(ParticleTypes.HEART, townswoman.getX(), townswoman.getY() + 1.5, townswoman.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
                        }

                        player.sendSystemMessage(Component.literal("§6§l[PACTO DE DEVOCIÓN] §e¡Has sellado un juramento eterno con §6" + npcName + "§e! Ya no requerirá contratos ni esmeraldas y te servirá con lealtad absoluta.").withStyle(ChatFormatting.GOLD));

                        responseDialogue = "§6\"¡Mi amo! Desde este instante, mi vida, mi fuerza y mi devoción le pertenecen por completo. Jamás flaquearé en vuestro servicio.\"";
                    } else {
                        responseDialogue = "§c\"Aún debemos fortalecer nuestro vínculo antes de poder consagrar un juramento de devoción eterna (Requiere 100 Afinidad).\"";
                    }
                }
            }
            case ACTION_MANA_BAPTISM -> {
                if (townswoman.isHiredBy(player) && townswoman.isDevoted()) {
                    if (townswoman.getVassalRank() >= 4) {
                        responseDialogue = "§6\"¡Amo, ya he alcanzado la cumbre de mi evolución espiritual (Rango S)! Todo mi ser vibra en armonía con vuestra grandeza.\"";
                    } else {
                        com.pablo.tenseicraft.data.PlayerRpgData rpgData = player.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA);
                        float requiredMana = 50.0F;
                        if (rpgData != null && rpgData.consumeMana(player, requiredMana)) {
                            PacketDistributor.sendToPlayer(player, new SyncManaPayload(rpgData.getCurrentMana(player), rpgData.getMaxMana(player)));

                            townswoman.promoteVassalRank();

                            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
                            player.serverLevel().playSound(null, townswoman.getX(), townswoman.getY(), townswoman.getZ(),
                                    SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.3F);

                            if (player.serverLevel() instanceof ServerLevel sl) {
                                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, townswoman.getX(), townswoman.getY() + 1.2, townswoman.getZ(), 25, 0.4, 0.6, 0.4, 0.15);
                                sl.sendParticles(ParticleTypes.SOUL, townswoman.getX(), townswoman.getY() + 1.0, townswoman.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                            }

                            player.sendSystemMessage(Component.literal("§b§l[BAUTISMO DE MANÁ] §e¡Has imbuido 50 de Maná en §6" + npcName + "§e! Ha evolucionado a: §b" + townswoman.getVassalRankName()).withStyle(ChatFormatting.AQUA));

                            responseDialogue = "§b\"¡Increíble! Siento cómo vuestro inmenso maná fluye por mi ser, transformando mi fuerza y espíritu. ¡Dedicaré esta nueva bendición a vuestra gloria!\"";
                        } else {
                            responseDialogue = "§c\"Amo, su reserva de maná espiritual es insuficiente para realizar la bendición evolutiva (Requiere 50 Maná).\"";
                            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                        }
                    }
                }
            }
        }

        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                townswoman.getId(),
                npcName,
                "townswoman",
                townswoman.getSkinVariant(),
                0,
                currentAffinity,
                townswoman.isHiredWorker(),
                townswoman.isHiredBy(player),
                0,
                false,
                townswoman.getWorkerStatus().ordinal(),
                townswoman.isDevoted(),
                townswoman.getVassalRank(),
                false,
                0,
                false,
                responseDialogue
        ));
    }

    private static int countEmeralds(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(Items.EMERALD)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consumeEmeralds(Player player, int amount) {
        if (player.isCreative()) return;
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(Items.EMERALD)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
    }

    private static void handleVillagerAction(ServerPlayer player, Villager villager, int action) {
        String villagerName = villager.hasCustomName() ? villager.getCustomName().getString() : "Aldeano";
        String playerName = player.getName().getString();
        String affKey = "RpgAffinity_" + player.getUUID();
        int currentAffinity = villager.getPersistentData().getInt(affKey);
        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                String chatKey = "RpgLastChat_" + player.getUUID();
                long lastChat = villager.getPersistentData().getLong(chatKey);
                long now = player.level().getGameTime();

                if (now - lastChat >= 2400L) { // 2 minutes cooldown
                    villager.getPersistentData().putLong(chatKey, now);
                    currentAffinity = Math.min(100, currentAffinity + 3);
                    villager.getPersistentData().putInt(affKey, currentAffinity);
                    NpcSocialManager.spawnAffinityEffects(villager, false);
                }
                responseDialogue = NpcSocialManager.getChatDialogue("villager", currentAffinity, AdventurerRank.F, villagerName, playerName);
            }
            case ACTION_GIFT -> {
                ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                NpcSocialManager.GiftResult result = NpcSocialManager.evaluateGift(heldItem, false);
                if (result.affinityGain() > 0 && !heldItem.isEmpty()) {
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    currentAffinity = Math.min(100, currentAffinity + result.affinityGain());
                    villager.getPersistentData().putInt(affKey, currentAffinity);
                    NpcSocialManager.spawnAffinityEffects(villager, result.isLoved());
                } else {
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                responseDialogue = result.responseDialogue();
            }
        }

        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                villager.getId(),
                villagerName,
                "villager",
                0,
                0,
                currentAffinity,
                false,
                false,
                0,
                false,
                0,
                false,
                0,
                false,
                0,
                false,
                responseDialogue
        ));
    }

    private static void handleGuildMasterAction(ServerPlayer player, com.pablo.tenseicraft.entities.GuildMasterNpcEntity guildMaster, int action) {
        String masterName = guildMaster.getName().getString();
        String playerName = player.getName().getString();
        com.pablo.tenseicraft.data.PlayerRpgData rpgData = player.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA);
        if (rpgData == null) return;

        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                com.pablo.tenseicraft.data.GreekGod god = rpgData.getPatronGod();
                String godName = (god != null) ? god.getDisplayName() : "un Dios Olímpico";
                if (rpgData.getReputationScore() < 0) {
                    responseDialogue = "§c\"No tengo nada de qué hablar contigo, forajido. Las autoridades del gremio no olvidan tus crímenes, incluso si portas la bendición de " + godName + ".\"";
                } else if (!rpgData.isGuildRegistered()) {
                    responseDialogue = "§e\"El Gremio registra a todos los aventureros bendecidos por los Dioses. Para progresar en los rangos deberás alcanzar los Hitos de Nivel para que el Dios cuya bendición posees te otorgue la Ascensión al dormir en el Plano Astral.\"";
                } else if (!rpgData.hasReceivedAdventurerCard()) {
                    rpgData.setReceivedAdventurerCard(true);
                    ModNetworking.syncToPlayer(player);

                    ItemStack card = com.pablo.tenseicraft.items.AdventurerCardItem.createPlayerCard(player, rpgData);
                    if (!player.getInventory().add(card)) {
                        player.drop(card, false);
                    }

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            guildMaster.getX(), guildMaster.getY() + 1.2, guildMaster.getZ(), 15, 0.4, 0.4, 0.4, 0.05);

                    responseDialogue = "§6\"¡Ah, " + playerName + "! Veo que eres un aventurero formalmente registrado pero aún no tenías tu credencial. ¡Aquí tienes tu Tarjeta Oficial de Aventurero! Llévala contigo para identificarte y consultar tu estado.\"";
                } else if (!rpgData.hasDivineBlessingUnlocked()) {
                    responseDialogue = "§e\"Escucha bien, " + playerName + ": ningún mortal puede subir de nivel por su propia cuenta. Cuando abatas a tu primer enemigo, llamarás la atención de un Dios; ve a dormir y en el Espacio Astral te otorgará su Bendición para desbloquear tu progresión.\"";
                } else if (rpgData.isAtMilestoneCap()) {
                    responseDialogue = "§d\"¡Has alcanzado el Hito de Nivel " + rpgData.getCurrentMilestoneCap() + ", " + playerName + "! El Gremio no otorga las Ascensiones; ese poder pertenece únicamente a los Dioses. Ve a dormir a una cama para que " + godName + " te convoque al Espacio Astral y te ascienda al siguiente rango.\"";
                } else {
                    responseDialogue = "§a\"¡Sigue fortaleciendo tu existencia en nombre de " + godName + ", " + playerName + "! Para ascender a " + rpgData.getRank().getNextRank().getBadge().getString() + " §adebes alcanzar el Hito de Nivel " + rpgData.getCurrentMilestoneCap() + " (Nivel actual: " + rpgData.getRpgLevel() + ") y luego dormir para recibir la Ascensión en el Plano Astral.\"";
                }
            }
            case ACTION_REGISTER_GUILD -> {
                if (rpgData.getReputationScore() < 0) {
                    responseDialogue = "§4\"¡Rechazado! El Gremio de Aventureros jamás admitirá a un criminal forajido en sus filas.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else if (rpgData.isGuildRegistered()) {
                    responseDialogue = "§a\"Ya estás registrado formalmente en el gremio con " + rpgData.getRank().getBadge().getString() + ".\"";
                } else {
                    com.pablo.tenseicraft.data.AdventurerRank currentRank = rpgData.getRank();
                    rpgData.setGuildRegistered(true);
                    rpgData.setReceivedAdventurerCard(true);
                    ModNetworking.syncToPlayer(player);
                    com.pablo.tenseicraft.events.QuestEvents.syncMainQuests(player, true);
                    ModNetworking.syncQuestsToPlayer(player);

                    // Deliver official Adventurer Card
                    ItemStack card = com.pablo.tenseicraft.items.AdventurerCardItem.createPlayerCard(player, rpgData);
                    if (!player.getInventory().add(card)) {
                        player.drop(card, false);
                    }

                    // Deliver Patchouli Guide Book if available
                    ItemStack guideBook = com.pablo.tenseicraft.compat.patchouli.PatchouliCompat.getGuideBookStack();
                    if (!guideBook.isEmpty()) {
                        if (!player.getInventory().add(guideBook)) {
                            player.drop(guideBook, false);
                        }
                    }

                    com.pablo.tenseicraft.advancements.ModAdvancements.award(player, com.pablo.tenseicraft.advancements.ModAdvancements.FIND_GUILD);

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);

                    player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            guildMaster.getX(), guildMaster.getY() + 1.2, guildMaster.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
                    player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.4, 0.3, 0.1);

                    player.sendSystemMessage(Component.empty());
                    player.sendSystemMessage(Component.literal("§6§l[GREMIO DE AVENTUREROS] §f¡Bienvenido a la orden!"));
                    player.sendSystemMessage(Component.literal("§eHas sido registrado oficialmente como: ").append(currentRank.getBadge()));
                    player.sendSystemMessage(Component.literal("§a- ¡Has recibido tu Tarjeta Oficial de Aventurero personalizada!"));
                    player.sendSystemMessage(Component.literal("§7- Tu rango ahora se muestra sobre tu cabeza y en tu ficha de estadísticas."));
                    player.sendSystemMessage(Component.literal("§7- Ahora puedes aceptar misiones y contratos diarios del Gremio."));
                    player.sendSystemMessage(Component.literal("§7- Alcanza los Hitos de Nivel y duerme para que tu Dios te otorgue la Ascensión en el Plano Astral."));
                    player.sendSystemMessage(Component.empty());

                    if (currentRank.getIndex() > com.pablo.tenseicraft.data.AdventurerRank.F.getIndex()) {
                        responseDialogue = "§6\"¡Impresionante, " + playerName + "! ¡Veo que ya portabas el poder de un " + currentRank.getBadge().getString() + " §6antes de acercarte al Gremio! Te registro oficialmente conservando tu merecido rango y te entrego tu Tarjeta Oficial de Aventurero.\"";
                    } else {
                        responseDialogue = "§6\"¡Felicidades, " + playerName + "! Has sido registrado oficialmente como Aventurero de Rango F y te he entregado tu Tarjeta de Aventurero. Recuerda que para ascender en los rangos deberás alcanzar cada Hito de Nivel para que tu Dios te otorgue la Ascensión al dormir.\"";
                    }
                }
            }
            case ACTION_GUILD_ASCEND -> {
                com.pablo.tenseicraft.data.GreekGod god = rpgData.getPatronGod();
                String godName = (god != null) ? god.getDisplayName() : "tu Dios";
                if (!rpgData.isGuildRegistered()) {
                    responseDialogue = "§c\"Primero debes estar inscrito en el Gremio de Aventureros.\"";
                } else if (rpgData.getRank().isMaxRank()) {
                    responseDialogue = "§6\"¡Ya has alcanzado el rango máximo de aventurero! Eres una leyenda viviente bendecida por " + godName + ".\"";
                } else {
                    int reqLevel = rpgData.getCurrentMilestoneCap();
                    if (rpgData.getRpgLevel() < reqLevel) {
                        responseDialogue = "§e\"Para ascender de rango necesitas llegar al Hito de Nivel " + reqLevel + " (actual: Nivel " + rpgData.getRpgLevel() + "). Solo entonces " + godName + " te convocará al dormir al Espacio Astral para otorgarte la Ascensión.\"";
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    } else {
                        responseDialogue = "§d\"¡Has alcanzado el Hito de Nivel " + reqLevel + "! Ve a dormir en cualquier cama: " + godName + " te espera en el Espacio Astral para felicitarte por tus hazañas y otorgarte la Ascensión a " + rpgData.getRank().getNextRank().getBadge().getString() + "§d.\"";
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.1F);
                    }
                }
            }
            case ACTION_GUILD_ASCEND_COMPANION -> {
                List<AdventurerNpcEntity> readyComps = player.serverLevel().getEntitiesOfClass(
                        AdventurerNpcEntity.class,
                        player.getBoundingBox().inflate(16),
                        adv -> adv.isHiredBy(player) && adv.isReadyToAscend()
                );
                if (!readyComps.isEmpty()) {
                    AdventurerNpcEntity comp = readyComps.get(0);
                    String compName = comp.hasCustomName() ? comp.getCustomName().getString() : "tu camarada";
                    comp.ascendRank();
                    AdventurerRank newRank = comp.getRank();

                    player.serverLevel().playSound(null, guildMaster.getX(), guildMaster.getY(), guildMaster.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.serverLevel().playSound(null, comp.getX(), comp.getY(), comp.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);

                    responseDialogue = "§6\"¡Extraordinario trabajo, " + playerName + "! He evaluado los registros y hazañas de " + compName + ". Cumple con todos los estándares y méritos exigidos por la orden. ¡Por decreto del Gremio, queda formalmente ascendido a " + newRank.getBadge().getString() + "! Que continúen cosechando gloria juntos.\"";
                    player.sendSystemMessage(Component.literal("§6§l[GREMIO DE AVENTUREROS] §a¡Tu camarada §e" + compName + " §aha sido ascendido oficialmente a " + newRank.getBadge().getString() + "!"));
                } else {
                    responseDialogue = "§c\"No veo a ningún camarada tuyo cerca que cumpla con los méritos de nivel para ascender de rango. Tráelo a la sede cuando esté listo.\"";
                }
            }
        }

        List<AdventurerNpcEntity> companions = player.serverLevel().getEntitiesOfClass(
                AdventurerNpcEntity.class,
                player.getBoundingBox().inflate(16),
                adv -> adv.isHiredBy(player) && adv.isReadyToAscend()
        );
        boolean anyReady = !companions.isEmpty();

        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                guildMaster.getId(),
                masterName,
                "guild_master",
                0,
                rpgData.getRank().getIndex(),
                50,
                rpgData.isGuildRegistered(),
                false,
                0L,
                false,
                0,
                false,
                0,
                false,
                0,
                anyReady,
                responseDialogue
        ));
    }

    private static void handleAliceAction(ServerPlayer player, com.pablo.tenseicraft.entities.AliceNpcEntity alice, int action) {
        String playerName = player.getName().getString();
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (rpgData == null) return;

        com.pablo.tenseicraft.advancements.ModAdvancements.award(player, com.pablo.tenseicraft.advancements.ModAdvancements.MEET_ALICE);

        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                ItemStack sword = findSpiritualSword(player);
                if (rpgData.isPrimordialFuryTrained()) {
                    responseDialogue = "§d\"¡Increíble, " + playerName + "! Puedo sentir el eco de la §fFuria Primordial §ddormido en tu cuerpo. Redfox hizo un trabajo excepcional contigo... Mantén tu espada sellada y deja que el Manto Celestial hable en la batalla.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                } else if (rpgData.isAliceSentToRedfox()) {
                    responseDialogue = "§d\"Busca a §6Redfox§d, el aventurero Kitsune errante que recorre las aldeas. Él pasó por lo mismo que tú y es el único que puede enseñarte a luchar sin liberar tu espada.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.1F);
                } else if (rpgData.isSwordRejectionTriggered()) {
                    rpgData.setAliceSentToRedfox(true);
                    ModNetworking.syncToPlayer(player);
                    responseDialogue = "§d\"¿Qué dices, " + playerName + "...? ¿La Espada Espiritual rechazó por completo abrirse ante ti?!\n\n...No puede ser. Has nacido con un §f§lDestino Primordial§d. Tu energía espiritual es demasiado indomable para quedar encerrada dentro de una hoja; por eso el acero se niega a obedecerte.\n\nYo no puedo entrenarte en ese camino, pero conozco a un solo guerrero al que le ocurrió lo mismo y logró dominar el combate con su espada sellada: §6Redfox§d, el aventurero Kitsune que viaja por las aldeas. ¡Ve a hablar con él!\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.05F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.5, 0.7, 0.5, 0.05);
                    player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.1, player.getZ(), 25, 0.4, 0.6, 0.4, 0.08);
                } else if (sword != null && com.pablo.tenseicraft.items.SpiritualSwordItem.isFirstReleaseUnlocked(sword)) {
                    responseDialogue = "§d\"¡Excelente trabajo al dominar la Primera Liberación de tu espada espiritual, " + playerName + "! Siente su filo... Aún queda mucho poder dormido por extraer de su esencia. Cuando estés listo y alcances una mayor maestría, podrás buscar dominar y conseguir tu Segunda Liberación.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                } else if (sword != null && com.pablo.tenseicraft.items.SpiritualSwordItem.getAffinity(sword) >= 100) {
                    com.pablo.tenseicraft.items.SpiritualSwordItem.setTrialReady(sword, true);
                    responseDialogue = "§d\"¡Magnífico, " + playerName + "! Siento la inmensa resonancia que emana de tu espada... ¡Has alcanzado el 100% de afinidad con ella! Has forjado un lazo indestructible, y el Espíritu de tu Espada está listo para manifestarse.\n\nEscúchame bien: para obtener tu §6Primera Liberación§d, deberás enfrentarte al mismísimo §cEspíritu de tu Espada§d en un duelo a muerte. Tendrá exactamente tus mismas estadísticas, tus mismas habilidades físicas y empuñará el arma liberada. Prepárate con tu mejor armadura, busca una zona despejada y tranquila, y cuando estés listo presiona §e[Z]§d para iniciar la prueba.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.1F);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.6, 0.4, 0.2);
                } else if (sword != null && com.pablo.tenseicraft.items.SpiritualSwordItem.isSoulBound(sword)) {
                    int affinity = com.pablo.tenseicraft.items.SpiritualSwordItem.getAffinity(sword);
                    responseDialogue = "§d\"Tu espada espiritual está vinculada a tu alma, pero su afinidad actual es de " + affinity + "/100. Continúa usándola en combate contra monstruos para templarla. Al llegar a 100, sus efectos elementales despertarán por completo y te prepararé para su liberación.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else if (sword != null) {
                    responseDialogue = "§d\"Tienes una espada espiritual virgen en tus manos. Asóciale un nombre en un yunque y luego canaliza tu magia teniendo al menos 200 de Maná para forjar su Vínculo Maestro-Arma.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else {
                    responseDialogue = "§d\"Hola, " + playerName + ". Si estás buscando la senda de los Segadores Espirituales, cumple mi encargo y te otorgaré tu propia espada espiritual y nuestras técnicas ancestrales.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.1F);
                }
            }
            case ACTION_ALICE_START_QUEST -> {
                rpgData.setAliceQuestActive(true);
                rpgData.setAliceDMonstersKilled(0);
                rpgData.setAliceQuestCompleted(false);
                ModNetworking.syncToPlayer(player);

                com.pablo.tenseicraft.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                if (questData != null) {
                    questData.acceptQuest("alice_spiritual_trial");
                    ModNetworking.sendQuestToast(player, "Prueba de los Segadores", "0/5", false);
                    ModNetworking.syncQuestsToPlayer(player);
                }

                responseDialogue = "§d\"¡Excelente decisión, " + playerName + "! La misión ha comenzado. Elimina a 5 monstruos de Rango D (Brujas, Hoglins o Ahogados con tridente) y regresa conmigo.\"";
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            case ACTION_ALICE_CLAIM_REWARD -> {
                if (rpgData.isAliceQuestCompleted() && !rpgData.hasReceivedSpiritualSword()) {
                    rpgData.unlockSkill("magic_affinity"); // Despierta la afinidad espiritual del jugador
                    rpgData.setReceivedSpiritualSword(true);
                    ModNetworking.syncToPlayer(player);

                    com.pablo.tenseicraft.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                    if (questData != null) {
                        questData.claimReward("alice_spiritual_trial", player);
                        ModNetworking.syncQuestsToPlayer(player);
                    }

                    // Deliver Base Spiritual Sword and Spiritual Spellbook
                    ItemStack sword = new ItemStack(com.pablo.tenseicraft.items.ModItems.SPIRITUAL_SWORD.get());
                    if (!player.addItem(sword)) {
                        player.drop(sword, false);
                    }
                    ItemStack spellbook = new ItemStack(com.pablo.tenseicraft.items.ModItems.SPIRITUAL_SPELLBOOK.get());
                    if (!player.addItem(spellbook)) {
                        player.drop(spellbook, false);
                    }

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);

                    com.pablo.tenseicraft.advancements.ModAdvancements.award(player, com.pablo.tenseicraft.advancements.ModAdvancements.OBTAIN_SPIRITUAL_SWORD);

                    player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.5, 0.4, 0.1);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                            player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.5, 0.4, 0.15);

                    sendAliceTeachingsMessage(player);

                    responseDialogue = "§d\"¡Felicidades, " + playerName + "! La espada base de los Segadores y el Grimorio Espiritual ahora son tuyos. Equípalo en tu cinturón de Curios y afila tu hoja; ahora podrás aprender pergaminos de Técnicas Espirituales en mi tienda.\"";
                } else {
                    responseDialogue = "§d\"Aún debes cumplir con la tarea para recibir tu espada espiritual.\"";
                }
            }
            case ACTION_ALICE_TECHNIQUES_GUIDE -> {
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);

                sendAliceTeachingsMessage(player);

                responseDialogue = "§d\"Aquí tienes el compendio de las Técnicas Espirituales. Recuerda que mantienes [ALT IZQ] y entonas los sellos numéricos en tu teclado. Puedes aprender nuevas técnicas en mi tienda por 5 esmeraldas.\"";
            }
            case ACTION_ALICE_START_CULTIST_QUEST -> {
                if (!rpgData.hasReceivedSpiritualSword()) {
                    responseDialogue = "§c\"Aún no has completado tu iniciación como Segador Espiritual. Debes recibir tu primera espada espiritual y despertar tu vínculo primero.\"";
                } else {
                    com.pablo.tenseicraft.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                    if (questData != null) {
                        questData.acceptQuest("alice_cultist_hunt");
                        ModNetworking.sendQuestToast(player, "Misión de los Segadores: El Segador Corrupto", "0/1", false);
                        ModNetworking.syncQuestsToPlayer(player);
                    }
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.2F);
                    responseDialogue = "§d\"¡" + playerName + "! Ha ocurrido algo terrible en nuestra orden. Uno de nuestros Segadores Espirituales se ha corrompido en las sombras y ha fundado un culto oscuro en el Abismo para traer de regreso a la raza extinta de los Demonios. Debes detenerlo antes de que abra el portal demoníaco. Pídeme que te teletransporte al Abismo junto a tu grupo, encuéntralo y ¡elimínalo!\"";
                }
            }
            case ACTION_ALICE_CLAIM_CULTIST_REWARD -> {
                com.pablo.tenseicraft.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                boolean killed = player.getPersistentData().getBoolean("RpgKilledCultistBoss");
                if (questData != null && (questData.isCompleted("alice_cultist_hunt") || killed) && !questData.isQuestClaimed("alice_cultist_hunt")) {
                    questData.claimReward("alice_cultist_hunt", player);
                    ModNetworking.syncQuestsToPlayer(player);

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);

                    responseDialogue = "§d\"¡Increíble labor, " + playerName + "! Sentí la liberación de su alma desde aquí. Has derrotado al Cultista y salvado nuestro mundo de la resurrección de los Demonios. ¡Has completado exitosamente todo el contenido de la Alpha del mod! Muchísimas gracias por probar esta gran aventura.\"";
                } else {
                    responseDialogue = "§d\"El Cultista aún acecha en las profundidades del Abismo. Debes derrotarlo para cumplir el encargo.\"";
                }
            }
            case ACTION_ALICE_TELEPORT_ABYSS -> {
                if (!rpgData.hasReceivedSpiritualSword()) {
                    responseDialogue = "§c\"Aún no has completado tu iniciación como Segador Espiritual para cruzar al Abismo.\"";
                } else {
                    com.pablo.tenseicraft.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                    if (questData != null && !questData.isQuestActive("alice_cultist_hunt") && !questData.isQuestClaimed("alice_cultist_hunt")) {
                        questData.acceptQuest("alice_cultist_hunt");
                        ModNetworking.sendQuestToast(player, "Misión Rango A: El Segador Corrupto", "0/1", false);
                        ModNetworking.syncQuestsToPlayer(player);
                    }

                    List<ServerPlayer> travelers = new java.util.ArrayList<>();
                    travelers.add(player);

                    var party = com.pablo.tenseicraft.party.PartyManager.getParty(player.getUUID());
                    if (party != null) {
                        for (java.util.UUID memberUuid : party.getMemberUUIDs()) {
                            ServerPlayer member = player.serverLevel().getServer().getPlayerList().getPlayer(memberUuid);
                            if (member != null && !member.getUUID().equals(player.getUUID()) && member.distanceToSqr(player) <= 1024.0) {
                                travelers.add(member);
                            }
                        }
                    }

                    ServerLevel abyssLevel = player.getServer().getLevel(com.pablo.tenseicraft.world.dimension.ModDimensions.ABYSS_RIFT_LEVEL);

                    for (ServerPlayer traveler : travelers) {
                        traveler.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                com.pablo.tenseicraft.init.ModMobEffects.ABYSSAL_WARD, 24000, 0, false, false, true
                        ));
                        com.pablo.tenseicraft.world.dimension.AbyssTeleportHelper.enterAbyssRift(traveler);
                        traveler.sendSystemMessage(Component.literal("§5[✦] §dAlice ha abierto una brecha y protegido tu alma con su manto abisal (20m)."));
                    }

                    return;
                }
            }
            case ACTION_ALICE_REQUEST_WARD -> {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.pablo.tenseicraft.init.ModMobEffects.ABYSSAL_WARD, 24000, 0, false, false, true
                ));

                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.5F);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                        player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.4, 0.6, 0.4, 0.15);
                player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX(), player.getY() + 1.2, player.getZ(), 25, 0.3, 0.5, 0.3, 0.08);

                responseDialogue = "§5«He encantado tu alma con mi Manto de Inmunidad Abisal. El Vacío ejerce una fuerza de repulsión destructiva contra cualquier ser vivo, pero durante los próximos 20 minutos serás totalmente inmune a su rechazo. ¡Ve con cuidado y regresa antes de que se disipe!»";
            }
            case ACTION_GIFT -> {
                ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                boolean alreadyDelivered = player.getPersistentData().getBoolean("AliceShadowbladeGifted");
                NpcSocialManager.GiftResult result = NpcSocialManager.evaluateAliceGift(heldItem, alreadyDelivered);

                if (result.affinityGain() > 0 && !heldItem.isEmpty()) {
                    boolean wasShadowblade = heldItem.is(com.pablo.tenseicraft.items.ModItems.CULTIST_SPIRITUAL_SWORD.get());
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                    if (wasShadowblade) {
                        player.getPersistentData().putBoolean("AliceShadowbladeGifted", true);
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2F, 1.0F);
                        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, alice.getX(), alice.getY() + 1.2, alice.getZ(), 35, 0.4, 0.6, 0.4, 0.15);
                    }
                    alice.addAffinity(player, result.affinityGain());
                    NpcSocialManager.spawnAffinityEffects(alice, result.isLoved());
                } else {
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }
                responseDialogue = result.responseDialogue();
            }
            case ACTION_ALICE_HIRE_SUPPORT -> {
                boolean alreadyDelivered = player.getPersistentData().getBoolean("AliceShadowbladeGifted") || alice.getAffinity(player) >= 50;
                if (!alreadyDelivered) {
                    responseDialogue = "§d«Aún no hemos sellado un lazo de confianza suficiente. Purifica la espada Shadowblade del Cultista y tráemela para forjar nuestra alianza.»";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else if (alice.isEscorting()) {
                    if (alice.isEscorting(player)) {
                        responseDialogue = "§d«¡Ya me encuentro a tu lado! Lidera el camino y desataremos el poder espiritual contra nuestros enemigos.»";
                    } else {
                        responseDialogue = "§c«Me encuentro asistiendo a otro hermano de la orden en este momento. Vuelve a buscarme en cuanto concluya mi labor.»";
                    }
                } else {
                    alice.startEscort(player, 24000L); // 20 minutos (24,000 ticks)
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, alice.getX(), alice.getY() + 1.2, alice.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, alice.getX(), alice.getY() + 1.2, alice.getZ(), 25, 0.4, 0.6, 0.4, 0.1);
                    responseDialogue = "§5«¡Que así sea! Empuñaré mis artes de Segadora Espiritual a tu lado durante los próximos 20 minutos. Te asistiré con mis técnicas secretas de fortificación y sanación. ¡En marcha!»";
                }
            }
            case ACTION_ALICE_BUY_REPLACEMENT_SWORD -> {
                if (!rpgData.hasReceivedSpiritualSword()) {
                    responseDialogue = "§c«Aún no has completado tu iniciación para recibir tu primera espada espiritual.»";
                } else if (findSpiritualSword(player) != null) {
                    responseDialogue = "§d«Ya llevas una espada espiritual contigo. No puedo forjarte otra mientras conserves la tuya.»";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else if (countEmeralds(player) < 20 && !player.isCreative()) {
                    responseDialogue = "§c«Necesitas 20 esmeraldas para costear los materiales místicos del templo y forjar una nueva hoja.»";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else {
                    consumeEmeralds(player, 20);
                    ItemStack newSword = new ItemStack(com.pablo.tenseicraft.items.ModItems.SPIRITUAL_SWORD.get());
                    if (!player.addItem(newSword)) {
                        player.drop(newSword, false);
                    }

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                            player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.4, 0.5, 0.4, 0.15);

                    responseDialogue = "§d«Aquí tienes una nueva hoja espiritual virgen forjada en el templo. Recuerda que deberás forjar tu vínculo nuevamente: renómbrala en un yunque y canaliza tu energía espiritual [X] con al menos 200 de maná.»";
                }
            }
            case ACTION_ALICE_REROLL_SOUL_BLADE -> {
                if (!rpgData.hasReceivedSpiritualSword()) {
                    responseDialogue = "§c«Aún no has recibido tu espada espiritual para reconfigurar su alma.»";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else if (countEmeralds(player) < 25 && !player.isCreative()) {
                    responseDialogue = "§c«Requiero 25 esmeraldas como ofrenda para rearmonizar los canales espirituales de tu alma.»";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                } else {
                    consumeEmeralds(player, 25);

                    // Roll new traits
                    var random = player.getRandom();
                    com.pablo.tenseicraft.items.SpiritualSwordItem.BladeStyle newStyle =
                            com.pablo.tenseicraft.items.SpiritualSwordItem.BladeStyle.values()[random.nextInt(com.pablo.tenseicraft.items.SpiritualSwordItem.BladeStyle.values().length)];
                    com.pablo.tenseicraft.items.SpiritualSwordItem.BladeElement newElement =
                            com.pablo.tenseicraft.items.SpiritualSwordItem.BladeElement.ACTIVE_ELEMENTS[random.nextInt(com.pablo.tenseicraft.items.SpiritualSwordItem.BladeElement.ACTIVE_ELEMENTS.length)];
                    com.pablo.tenseicraft.items.SpiritualSwordItem.BladeRelease newRelease =
                            com.pablo.tenseicraft.items.SpiritualSwordItem.BladeRelease.values()[random.nextInt(com.pablo.tenseicraft.items.SpiritualSwordItem.BladeRelease.values().length)];
                    com.pablo.tenseicraft.items.SpiritualSwordItem.BladeResonance newResonance =
                            com.pablo.tenseicraft.items.SpiritualSwordItem.BladeResonance.values()[random.nextInt(com.pablo.tenseicraft.items.SpiritualSwordItem.BladeResonance.values().length)];
                    int newVariant = random.nextInt(2);

                    // Update player RPG data (retains FirstReleaseUnlocked!)
                    rpgData.setSoulBladeTraits(newStyle.getId(), newElement.getId(), newRelease.getId(), newResonance.getId(), newVariant);

                    // Mutate any spiritual swords in inventory/hand
                    for (ItemStack item : player.getInventory().items) {
                        if (!item.isEmpty() && item.getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
                            com.pablo.tenseicraft.items.SpiritualSwordItem.setBladeStyle(item, newStyle);
                            com.pablo.tenseicraft.items.SpiritualSwordItem.setBladeElement(item, newElement);
                            com.pablo.tenseicraft.items.SpiritualSwordItem.setBladeRelease(item, newRelease);
                            com.pablo.tenseicraft.items.SpiritualSwordItem.setBladeResonance(item, newResonance);
                            com.pablo.tenseicraft.items.SpiritualSwordItem.setBladeVariant(item, newVariant);
                            if (com.pablo.tenseicraft.items.SpiritualSwordItem.isFirstRelease(item)) {
                                com.pablo.tenseicraft.items.SpiritualSwordItem.syncAwakenedSpell(item);
                            }
                        }
                    }

                    // Special sound & particles
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2F, 0.9F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.6F);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 45, 0.5, 0.5, 0.5, 0.2);
                    player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.4, 0.4, 0.08);

                    String styleName = Component.translatable(newStyle.getTranslationKey()).getString();
                    String elemName = Component.translatable(newElement.getTranslationKey()).getString();
                    String releaseName = Component.translatable(newRelease.getTranslationKey()).getString();
                    String resName = Component.translatable(newResonance.getTranslationKey()).getString();
                    String varName = (newVariant == 1) ? "Variante II (Alternativa)" : "Variante I (Clásica)";

                    player.displayClientMessage(Component.literal("§5✦ ¡ALMA RECONFIGURADA! §e" + styleName + " §7| §b" + elemName + " §7| §d" + releaseName + " §7| §6" + varName), true);
                    player.sendSystemMessage(Component.literal("§5✦ [Alice] §d¡El alma de tu espada ha mutado exitosamente!"));
                    player.sendSystemMessage(Component.literal(" §7• Estilo de Combate: §e" + styleName));
                    player.sendSystemMessage(Component.literal(" §7• Imbuición Elemental: §b" + elemName));
                    player.sendSystemMessage(Component.literal(" §7• 1ª Liberación: §d" + releaseName));
                    player.sendSystemMessage(Component.literal(" §7• Resonancia RPG: §a" + resName));
                    player.sendSystemMessage(Component.literal(" §7• Aspecto de Piel: §6" + varName));
                    player.sendSystemMessage(Component.literal("§a(No requieres despertar el arma de nuevo. Al presionar [Z] para liberarla adoptará su nueva forma)"));

                    responseDialogue = "§5«¡Los hilos de tu alma han sido armonizados nuevamente! He reconfigurado la esencia de tu Espada Espiritual: ahora manifiesta el estilo " + styleName + " con afinidad al " + elemName + " (" + varName + "). Al liberarla [Z], mutará a esta forma al instante.»";
                }
            }
        }

        boolean hasDeliveredShadowblade = player.getPersistentData().getBoolean("AliceShadowbladeGifted");
        long remainingTicks = alice.getEscortRemainingTicks(player.level().getGameTime());

        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                alice.getId(),
                "Alice",
                "alice",
                0,
                rpgData.getRank().getIndex(),
                alice.getAffinity(player),
                alice.isEscorting(),
                alice.isEscorting(player),
                remainingTicks,
                false,
                0,
                hasDeliveredShadowblade,
                0,
                false,
                0,
                false,
                responseDialogue
        ));
    }

    private static ItemStack findSpiritualSword(ServerPlayer player) {
        if (player == null) return null;
        if (player.getMainHandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
            return player.getOffhandItem();
        }
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
                return item;
            }
        }
        return null;
    }

    private static void sendAliceTeachingsMessage(ServerPlayer player) {
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§5§l[SEGADORES ESPIRITUALES] §fCompendio de Técnicas Espirituales"));
        player.sendSystemMessage(Component.literal("§7Mantén presionado §f[ALT IZQ] §7y pulsa la secuencia de teclas en el teclado:"));
        player.sendSystemMessage(Component.literal(" §6✦ Nº 1: Physical Boost §e[1 -> 2 -> 3] §7(«Corpus Vigor Eruptio») — §b75 MP §7— Fuerza, Resistencia, Salto y Rapidez."));
        player.sendSystemMessage(Component.literal(" §a✦ Nº 2: Low Healing §e[1 -> 3 -> 2] §7(«Corpus Vitae Proiectio») — §b25 MP §7— Proyectil que cura al primer ente impactado."));
        player.sendSystemMessage(Component.literal(" §e✦ Nº 3: Low Paralize §e[2 -> 1 -> 3] §7(«Fulgur Vinculum Stasis») — §b60 MP §7— Inmoviliza y paraliza por 3 segundos."));
        player.sendSystemMessage(Component.literal(" §b✦ Nº 4: Low Purification §e[3 -> 2 -> 1] §7(«Anima Purgatio Dubia») — §b20 MP §7— Purifica un efecto al azar (puede errar)."));
        player.sendSystemMessage(Component.literal(" §c✦ Nº 5: Elemental Ball §e[3 -> 1 -> 2] §7(«Elementum Sphaera Iactus») — §b25 MP §7— Esfera elemental adaptativa (Fuego, Hielo, etc)."));
        player.sendSystemMessage(Component.literal(" §d✦ Nº 6: Afinidad Mágica §e[2 -> 3 -> 1] §7(«Anima Aether Fluctuo») — §b50 MP §7— Maestría de maná y +5 Espíritu por 15s."));
        player.sendSystemMessage(Component.literal(" §a✦ Nº 10: Fast Healing §e[3 -> 2 -> 1 -> 2] §7(«Anima Vitae Restitutio») — §b80 MP §7— Auto-apunta y cura masivamente al aliado más herido."));
        player.sendSystemMessage(Component.literal("§7Aprende o desbloquea nuevas técnicas con §dAlice §7en su tienda por §a5 Esmeraldas §7cada una."));
        player.sendSystemMessage(Component.empty());
    }

    private static void handleRedfoxAction(ServerPlayer player, com.pablo.tenseicraft.entities.RedfoxEntity redfox, int action) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        int rankIdx = data != null ? data.getRank().getIndex() : AdventurerRank.F.getIndex();
        int currentAffinity = redfox.getAffinity(player);

        String dialogue;
        switch (action) {
            case ACTION_CHAT -> {
                redfox.addAffinity(player, 1);
                if (data != null && data.isAliceSentToRedfox() && !data.isPrimordialFuryTrained()) {
                    dialogue = "§6\"¡Espera un segundo! Esa presión que emana de tu espada sellada... ¿Alice te envió conmigo porque el acero se negó a abrirse ante ti?\n\n¡Ja! A mí me pasó exactamente lo mismo hace mucho tiempo. Cuando tu espíritu no cabe dentro de una hoja, tienes que aprender a desatarlo desde tu propio cuerpo. Si quieres, puedo entrenarte y mostrarte cómo pelear a mi manera.\"";
                    player.serverLevel().playSound(null, redfox.getX(), redfox.getY(), redfox.getZ(),
                            SoundEvents.FOX_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.25F);
                    player.serverLevel().sendParticles(ParticleTypes.END_ROD,
                            redfox.getX(), redfox.getY() + 1.2, redfox.getZ(),
                            12, 0.3, 0.4, 0.3, 0.04);
                } else if (data != null && data.isPrimordialFuryTrained()) {
                    dialogue = "§6\"¿Cómo se siente desatar la §fFuria Primordial§6? Mantén tu espíritu ardiendo en combate con la espada sellada y deja que el Manto Celestial envuelva tus hombros cuando llegue el momento. ¡Haz temblar el suelo!\"";
                    player.serverLevel().playSound(null, redfox.getX(), redfox.getY(), redfox.getZ(),
                            SoundEvents.FOX_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.15F);
                } else {
                    dialogue = redfox.getRandomGreeting();
                    player.serverLevel().playSound(null, redfox.getX(), redfox.getY(), redfox.getZ(),
                            SoundEvents.FOX_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.1F);
                    player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            redfox.getX(), redfox.getY() + 1.2, redfox.getZ(),
                            4, 0.2, 0.2, 0.2, 0.02);
                }
            }
            case ACTION_REDFOX_TRAIN_PRIMORDIAL_FURY -> {
                if (data != null && data.isAliceSentToRedfox() && !data.isPrimordialFuryTrained()) {
                    data.setPrimordialFuryTabUnlocked(true);
                    data.setPrimordialFuryTrained(true);
                    data.setPrimordialFuryCharge(100.0f); // Lista al 100% para probar inmediatamente
                    ModNetworking.syncToPlayer(player);
                    redfox.addAffinity(player, 10);

                    ServerLevel sl = player.serverLevel();
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.1F, 1.0F);
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 1.3F);
                    sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 45, 0.6, 0.8, 0.6, 0.08);
                    sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.1, player.getZ(), 35, 0.5, 0.7, 0.5, 0.12);

                    // Aviso del sistema (fuera del diálogo inmersivo del NPC)
                    player.sendSystemMessage(Component.empty());
                    player.sendSystemMessage(Component.literal("§6§l[✦] §e¡Has despertado la §f§lFuria Primordial§e!"));
                    player.sendSystemMessage(Component.literal("§7• Entra a §eRPG §7en tu inventario (o presiona §e[K]§7) y revisa la nueva solapa §f§lFuria §7para ver tus habilidades y cómo activarlas."));
                    player.sendSystemMessage(Component.empty());

                    dialogue = "§6\"¡Eso es! Siente cómo la §f§lFuria Primordial §6desborda de tu interior y envuelve tus hombros como el §fManto Celestial§6.\n\nDe ahora en más, tu espada sellada no será una limitación, sino el arma con la que harás temblar la tierra. Pelea con fiereza para acumular esa energía, y cuando sientas que está al límite... ¡desátala sin contenerte!\"";
                } else {
                    dialogue = "§6\"Ya conoces mis secretos de combate. ¡Sal ahí fuera y demuestra el poder de la Furia Primordial!\"";
                }
            }
            case ACTION_GIFT -> {
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty()) {
                    dialogue = "¡Oh! Parece que tienes las manos vacías... Aunque agradezco mucho tu buena intención.";
                } else if (held.is(Items.SWEET_BERRIES) || held.is(Items.GLOW_BERRIES) || held.is(Items.SALMON) || held.is(Items.COOKED_SALMON) || held.is(Items.EMERALD)) {
                    held.shrink(1);
                    redfox.addAffinity(player, 5);
                    dialogue = "¡Waaa, muchas gracias! ¡Esto me encanta! Me aseguraré de recordarlo.";
                    player.serverLevel().playSound(null, redfox.getX(), redfox.getY(), redfox.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8F, 1.3F);
                    player.serverLevel().sendParticles(ParticleTypes.HEART,
                            redfox.getX(), redfox.getY() + 1.2, redfox.getZ(),
                            6, 0.3, 0.3, 0.3, 0.05);
                } else {
                    dialogue = "Gracias por el gesto, pero prefiero guardarlo para tus propias aventuras.";
                }
            }
            default -> dialogue = redfox.getRandomGreeting();
        }

        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                redfox.getId(),
                "Redfox",
                "redfox",
                0,
                rankIdx,
                redfox.getAffinity(player),
                false,
                false,
                0L,
                false,
                0,
                false,
                0,
                false,
                0,
                false,
                dialogue
        ));
    }

    private static boolean isAdvancedGear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) return true;
        if (stack.is(com.pablo.tenseicraft.items.ModItems.CULTIST_SPIRITUAL_SWORD.get())) return true;
        if (stack.is(com.pablo.tenseicraft.items.ModItems.RAW_VOIDSTEEL.get())
                || stack.is(com.pablo.tenseicraft.items.ModItems.VOIDSTEEL_INGOT.get())) return true;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = key.getPath();
        return path.contains("netherite") || path.contains("voidsteel");
    }

    private static void handleMeguAction(ServerPlayer player, com.pablo.tenseicraft.entities.MeguNpcEntity megu, int action) {
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (rpgData == null) return;

        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                megu.addAffinity(player, 2);
                String[] chats = {
                        "«Cada arma guarda un eco en su acero. Los Cristales de Alma solo despiertan lo que el portador ya lleva dentro.»",
                        "«Mi maestro solía decir que un buen herrero no domina el metal: escucha dónde pide ser templado.»",
                        "«He oído historias de reliquias del Abismo que vibran con energía espiritual propia... Me encantaría ver algún día hasta dónde llegas.»",
                        "«Si el yunque común te queda corto algún día, recuerda que existen yunques grabados con runas capaces de canalizar tu propia alma.»",
                        "«Entre golpe y golpe de martillo aprendí a reconocer a un aventurero destinado a superar la forja tradicional.»",
                        "«No olvides cuidar tus armas después de cada cacería; un filo equilibrado puede salvarte la vida.»"
                };
                responseDialogue = chats[player.getRandom().nextInt(chats.length)];
                player.serverLevel().playSound(null, megu.getX(), megu.getY(), megu.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.2F);
                player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        megu.getX(), megu.getY() + 1.2, megu.getZ(),
                        4, 0.2, 0.2, 0.2, 0.02);
            }
            case ACTION_GIFT -> {
                ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                boolean highTierMat = !held.isEmpty() && (
                        held.is(com.pablo.tenseicraft.items.ModItems.LESSER_SOUL_FRAGMENT.get())
                        || held.is(com.pablo.tenseicraft.items.ModItems.RESONANT_SOUL_CRYSTAL.get())
                        || held.is(com.pablo.tenseicraft.items.ModItems.RADIANT_SOUL_GEM.get())
                        || held.is(com.pablo.tenseicraft.items.ModItems.PRIMORDIAL_SOUL_CORE.get())
                        || held.is(com.pablo.tenseicraft.items.ModItems.RAW_VOIDSTEEL.get())
                        || held.is(com.pablo.tenseicraft.items.ModItems.VOIDSTEEL_INGOT.get())
                );
                boolean standardMat = !held.isEmpty() && (
                        held.is(Items.DIAMOND)
                        || held.is(Items.EMERALD)
                        || held.is(Items.IRON_INGOT)
                        || held.is(Items.GOLD_INGOT)
                        || held.is(Items.AMETHYST_SHARD)
                );

                if (highTierMat || standardMat) {
                    int gain = highTierMat ? 15 : 8;
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    megu.addAffinity(player, gain);
                    player.serverLevel().playSound(null, megu.getX(), megu.getY(), megu.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.4F);
                    player.serverLevel().sendParticles(ParticleTypes.HEART,
                            megu.getX(), megu.getY() + 1.3, megu.getZ(),
                            6, 0.3, 0.3, 0.3, 0.04);
                    responseDialogue = highTierMat
                            ? "§a«¡Increíble pureza! Un material así hace vibrar hasta las pinzas de mi taller. ¡Muchísimas gracias!»"
                            : "§e«¡Ooh, excelente material de forja! Lo aprovecharé muchísimo en el taller, ¡gracias!»";
                } else {
                    player.serverLevel().playSound(null, megu.getX(), megu.getY(), megu.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.1F);
                    responseDialogue = "§7«Agradezco el detalle, pero en mi taller aprecio sobre todo minerales, gemas, Cristales de Alma o Voidsteel.»";
                }
            }
            case ACTION_MEGU_REFORGE -> {
                boolean advancedGear = isAdvancedGear(player.getMainHandItem()) || isAdvancedGear(player.getOffhandItem());
                if (rpgData.getRank().getIndex() >= AdventurerRank.C.getIndex() || advancedGear) {
                    if (!rpgData.isMeguAnvilUnlocked()) {
                        rpgData.setMeguAnvilUnlocked(true);
                        ModNetworking.syncToPlayer(player);
                    }
                }
                PacketDistributor.sendToPlayer(player, new OpenReforgeScreenPayload(
                        false,
                        megu.getId(),
                        net.minecraft.core.BlockPos.ZERO
                ));
                return;
            }
            case ACTION_MEGU_BUY_MAGIC_ANVIL -> {
                if (!rpgData.isMeguAnvilUnlocked()) {
                    player.serverLevel().playSound(null, megu.getX(), megu.getY(), megu.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
                    responseDialogue = "§7«Todavía puedo trabajar tu equipamiento en mi yunque común. Ven cuando hayas puesto a prueba mi límite como herrera.»";
                } else if (countEmeralds(player) < 30 && !player.isCreative()) {
                    player.serverLevel().playSound(null, megu.getX(), megu.getY(), megu.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
                    responseDialogue = "§c«Necesitas §a30 Esmeraldas§c para adquirir la reliquia de mi maestro, el §dYunque Mágico§c.»";
                } else {
                    if (!player.isCreative()) {
                        consumeEmeralds(player, 30);
                    }
                    ItemStack anvilStack = new ItemStack(com.pablo.tenseicraft.init.ModBlocks.MAGIC_ANVIL.get());
                    if (!player.addItem(anvilStack)) {
                        player.drop(anvilStack, false);
                    }

                    player.serverLevel().playSound(null, megu.getX(), megu.getY(), megu.getZ(),
                            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.25F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.15F);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANT,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            40, 0.4, 0.6, 0.4, 0.18);

                    responseDialogue = "§d«¡Aquí tienes tu propio Yunque Mágico! Colócalo en tu base: sus runas responderán directamente a tu espíritu y a los Cristales de Alma para realizar las reforjas que mis manos ya no pueden alcanzar.»";
                }
            }
        }

        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                megu.getId(),
                "Megu",
                "megu",
                0,
                rpgData.getRank().getIndex(),
                megu.getAffinity(player),
                false,
                false,
                0L,
                false,
                rpgData.getMeguReforgeCount(),
                rpgData.isMeguAnvilUnlocked(),
                0,
                false,
                0,
                false,
                responseDialogue
        ));
    }

    private static void handleReimuAction(ServerPlayer player, com.pablo.tenseicraft.entities.ReimuNpcEntity reimu, int action) {
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (rpgData == null) return;

        if (action == ACTION_REIMU_CHALLENGE) {
            reimu.startRematch(player);
            return;
        }

        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                String[] touhouLoreQuotes = new String[] {
                        "«¿Ves estos cerezos sin fin? Si alguna vez notas que los pétalos se resisten a caer y se acerca una primavera eterna... seguro es culpa de Yuyuko Saigyouji queriendo hacer florecer el Saigyou Ayakashi allá en el Hakugyokurou.»",
                        "«La última vez que apareció una niebla escarlata tapando el sol, resultó ser Remilia Scarlet haciendo berrinche desde su Mansión Escarlata. Nada que un buen golpe con mi vara de purificación y unas cuantas Spell Cards no resolvieran.»",
                        "«Si alguna brujita con sombrero negro aparece diciendo 'ze~' y volando por mi techo con un láser gigante... dile a Marisa que devuelva los libros de Patchouli y el té que me robó del santuario.»",
                        "«Inventé las Reglas de las Spell Cards precisamente para que humanos y youkai puedan pelear con lluvia de Danmaku elegante sin destruirlo todo... bueno, salvo cuando Yukari se queda dormida en una brecha y rompe la barrera.»",
                        "«Ayer vi a un hada de hielo cerca del lago gritando que era 'la más fuerte'... pobre Cirno, le cuesta contar hasta nueve sin congelarse las ideas.»",
                        "«No le digas a nadie, pero odio que Aya Shameimaru saque titulares exagerados en el periódico Bunbunmaru diciendo que 'la miko acepta sobornos en el santuario'. ¡Es una caja de donaciones perfectamente legítima!»",
                        "«Desde que Sanae abrió la sucursal del Santuario Moriya en la montaña, todos los creyentes se van para allá prometiendo milagros de Kanako y Suwako... ¡y mi caja de ofrendas sigue igual de vacía!»",
                        "«Si de repente ves una oni pequeña llamada Suika organizando un banquete interminable bajo estos cerezos, esconde el sake o no podremos volver a caminar en tres días.»",
                        "«Esa técnica que te enseñé —Ha-ku-rei— condensa las esferas Yin-Yang en un patrón rebote. Úsala cuando estés rodeado, ¡aunque en el Gensokyo lo normal es esquivar mil proyectiles a la vez!»",
                        "«Yukari siempre dice que el Gensokyo existe entre la fantasía y la realidad gracias a la Gran Barrera Hakurei... Supongo que tu hechizo de 'Conectar Realidades' resonó justo debajo de los cerezos por la misma razón.»"
                };
                int idx = player.getRandom().nextInt(touhouLoreQuotes.length);
                responseDialogue = touhouLoreQuotes[idx];
                reimu.addAffinity(player, 3);
                player.serverLevel().playSound(null, reimu.getX(), reimu.getY(), reimu.getZ(),
                        SoundEvents.CHERRY_WOOD_STEP, SoundSource.NEUTRAL, 0.9F, 1.25F);
                player.serverLevel().sendParticles(ParticleTypes.CHERRY_LEAVES,
                        reimu.getX(), reimu.getY() + 1.2, reimu.getZ(),
                        6, 0.3, 0.3, 0.3, 0.03);
            }
            case ACTION_GIFT -> {
                ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
                boolean treasure = !held.isEmpty() && (
                        held.is(Items.EMERALD)
                        || held.is(Items.GOLD_INGOT)
                        || held.is(Items.GOLD_NUGGET)
                        || held.is(Items.DIAMOND)
                );
                boolean shrineItem = !held.isEmpty() && (
                        held.is(Items.CHERRY_SAPLING)
                        || held.is(Items.PINK_PETALS)
                        || held.is(Items.SWEET_BERRIES)
                        || held.is(Items.CAKE)
                        || held.is(Items.HONEY_BOTTLE)
                        || held.is(Items.PAPER)
                );

                if (treasure || shrineItem) {
                    int gain = treasure ? 15 : 8;
                    if (!player.isCreative()) {
                        held.shrink(1);
                    }
                    reimu.addAffinity(player, gain);
                    player.serverLevel().playSound(null, reimu.getX(), reimu.getY(), reimu.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 1.0F, 1.3F);
                    player.serverLevel().sendParticles(ParticleTypes.HEART,
                            reimu.getX(), reimu.getY() + 1.4, reimu.getZ(),
                            6, 0.3, 0.3, 0.3, 0.05);
                    responseDialogue = treasure
                            ? "§c«¡¿Una ofrenda real para la caja de donaciones?! ¡Por fin podré comprar té de buena calidad esta semana! ¡Gracias!»"
                            : "§d«Ooh, justo lo que hacía falta en el Santuario Hakurei. Siéntete como en casa bajo los cerezos.»";
                } else {
                    player.serverLevel().playSound(null, reimu.getX(), reimu.getY(), reimu.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.1F);
                    responseDialogue = "§7«Si quieres ayudar al santuario, acepta esmeraldas, lingotes de oro, dulces para el té o pétalos de cerezo. ¡Y no, el aire no paga impuestos!»";
                }
            }
        }

        PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                reimu.getId(),
                "Reimu Hakurei",
                "reimu",
                0,
                rpgData.getRank().getIndex(),
                reimu.getAffinity(player),
                false,
                false,
                0L,
                false,
                0,
                false,
                0,
                false,
                0,
                false,
                responseDialogue
        ));
    }
}
