package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import com.pablo.rpglevels.events.ModAttributesHandler;
import com.pablo.rpglevels.social.NpcSocialManager;
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

    public static final Type<NpcDialogueActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "npc_dialogue_action"));

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
            } else if (entity instanceof com.pablo.rpglevels.entities.GuildMasterNpcEntity guildMaster) {
                handleGuildMasterAction(player, guildMaster, payload.actionType());
            } else if (entity instanceof com.pablo.rpglevels.entities.AliceNpcEntity alice) {
                handleAliceAction(player, alice, payload.actionType());
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
                    adventurer.setStance(com.pablo.rpglevels.entities.CompanionStance.FOLLOW);
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
                com.pablo.rpglevels.data.PlayerRpgData rpgData = player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
                if (rpgData != null && rpgData.getReputationTier() == com.pablo.rpglevels.data.ReputationTier.PUBLIC_ENEMY) {
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

                        com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.HIRE_ADVENTURER);

                        NpcSocialManager.spawnAffinityEffects(adventurer, true);
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

                        // Hero bonus dialogue vs standard
                        if (rpgData != null && rpgData.getReputationTier() == com.pablo.rpglevels.data.ReputationTier.HERO_LEGENDARY) {
                            responseDialogue = "§6\"¡Es un inmenso honor marchar junto al Héroe de nuestras tierras! Daré mi vida por tu causa.\"";
                        } else {
                            responseDialogue = "§a\"¡Trato hecho, camarada! Cubriré tu retaguardia durante todo el día. Marca el paso y te seguiré.\"";
                        }

                        player.sendSystemMessage(Component.translatable("rpglevels.social.hired_broadcast", npcName).withStyle(ChatFormatting.BOLD));
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
                    adventurer.setStance(com.pablo.rpglevels.entities.CompanionStance.FOLLOW);
                    responseDialogue = "§a\"¡A tus órdenes! Te sigo de cerca a donde vayas.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            }
            case ACTION_STANCE_BASE -> {
                if (adventurer.isHiredBy(player)) {
                    adventurer.setHomeBasePos(adventurer.blockPosition());
                    adventurer.setStance(com.pablo.rpglevels.entities.CompanionStance.WANDER_BASE);
                    responseDialogue = "§b\"Comprendido. Me quedaré aquí en casa patrullando y descansando mientras estés fuera.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            }
            case ACTION_STANCE_GUARD -> {
                if (adventurer.isHiredBy(player)) {
                    adventurer.setStance(com.pablo.rpglevels.entities.CompanionStance.STAY_GUARD);
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
                    ItemStack cane = com.pablo.rpglevels.items.OverseerCaneItem.createCaneForWorker(townswoman);
                    if (!player.getInventory().add(cane)) {
                        player.drop(cane, false);
                    }
                    com.pablo.rpglevels.items.OverseerCaneItem.sendSetupGuide(player);
                    responseDialogue = "§a\"¡Excelente lugar para nuestra base! Toma este Bastón del Patrón. Delimita la zona de tala y asigna los cofres y mi cama para ponerme manos a la obra.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
                }
            }
            case ACTION_EDIT_WORK -> {
                if (townswoman.isHiredBy(player)) {
                    ItemStack cane = com.pablo.rpglevels.items.OverseerCaneItem.createCaneForWorker(townswoman, true);
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
                        com.pablo.rpglevels.data.PlayerRpgData rpgData = player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
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

    private static void handleGuildMasterAction(ServerPlayer player, com.pablo.rpglevels.entities.GuildMasterNpcEntity guildMaster, int action) {
        String masterName = guildMaster.getName().getString();
        String playerName = player.getName().getString();
        com.pablo.rpglevels.data.PlayerRpgData rpgData = player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
        if (rpgData == null) return;

        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                if (rpgData.getReputationScore() < 0) {
                    responseDialogue = "§c\"No tengo nada de qué hablar contigo, forajido. Las autoridades del gremio no olvidan tus crímenes.\"";
                } else if (!rpgData.isGuildRegistered()) {
                    responseDialogue = "§e\"El gremio conecta a los aventureros con las necesidades del reino. Desde encargos de recolección hasta cazar amenazas calamitosas.\"";
                } else if (!rpgData.hasReceivedAdventurerCard()) {
                    rpgData.setReceivedAdventurerCard(true);
                    ModNetworking.syncToPlayer(player);

                    ItemStack card = com.pablo.rpglevels.items.AdventurerCardItem.createPlayerCard(player, rpgData);
                    if (!player.getInventory().add(card)) {
                        player.drop(card, false);
                    }

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            guildMaster.getX(), guildMaster.getY() + 1.2, guildMaster.getZ(), 15, 0.4, 0.4, 0.4, 0.05);

                    responseDialogue = "§6\"¡Ah, " + playerName + "! Veo que eres un aventurero formalmente registrado pero aún no tenías tu credencial. ¡Aquí tienes tu Tarjeta Oficial de Aventurero! Llévala contigo para identificarte y consultar tu estado.\"";
                } else {
                    responseDialogue = "§a\"¡Sigue cumpliendo misiones con honor, " + playerName + "! Tu rango de aventurero no solo te da prestigio, sino mayor responsabilidad.\"";
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
                    rpgData.setGuildRegistered(true);
                    rpgData.setRank(com.pablo.rpglevels.data.AdventurerRank.F);
                    rpgData.setReceivedAdventurerCard(true);
                    ModNetworking.syncToPlayer(player);

                    // Deliver official Adventurer Card
                    ItemStack card = com.pablo.rpglevels.items.AdventurerCardItem.createPlayerCard(player, rpgData);
                    if (!player.getInventory().add(card)) {
                        player.drop(card, false);
                    }

                    // Deliver Patchouli Guide Book if available
                    ItemStack guideBook = com.pablo.rpglevels.compat.patchouli.PatchouliCompat.getGuideBookStack();
                    if (!guideBook.isEmpty()) {
                        if (!player.getInventory().add(guideBook)) {
                            player.drop(guideBook, false);
                        }
                    }

                    com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.FIND_GUILD);

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
                    player.sendSystemMessage(Component.literal("§eHas sido registrado como: §7Aventurero de Rango F"));
                    player.sendSystemMessage(Component.literal("§a- ¡Has recibido tu Tarjeta Oficial de Aventurero personalizada!"));
                    player.sendSystemMessage(Component.literal("§7- Tu rango ahora se muestra sobre tu cabeza y en tu ficha de estadísticas."));
                    player.sendSystemMessage(Component.literal("§7- Ahora puedes aceptar misiones de rango F."));
                    player.sendSystemMessage(Component.literal("§7- Tus hazañas y reputación ahora cuentan con el multiplicador oficial del gremio."));
                    player.sendSystemMessage(Component.empty());

                    responseDialogue = "§6\"¡Felicidades, " + playerName + "! Has sido registrado oficialmente como Aventurero de Rango F y te he entregado tu Tarjeta de Aventurero. ¡Que tus hazañas traigan honor y gloria a nuestro gremio!\"";
                }
            }
            case ACTION_GUILD_ASCEND -> {
                if (!rpgData.isGuildRegistered()) {
                    responseDialogue = "§c\"Primero debes estar inscrito en el Gremio de Aventureros para poder ascender de rango.\"";
                } else if (rpgData.getRank().isMaxRank()) {
                    responseDialogue = "§6\"¡Ya has alcanzado el rango máximo de aventurero! Eres una leyenda viviente del gremio.\"";
                } else {
                    int reqLevel = rpgData.getRank().getRequiredAscensionLevel();
                    if (player.experienceLevel < reqLevel) {
                        responseDialogue = "§c\"Aún no tienes suficiente experiencia para el siguiente rango. Necesitas nivel " + reqLevel + " de XP (actual: " + player.experienceLevel + ").\"";
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    } else {
                        boolean success = rpgData.performAscension();
                        if (success) {
                            // Reset player XP to 0
                            player.setExperienceLevels(0);
                            player.setExperiencePoints(0);

                            // Update attributes & sync
                            ModAttributesHandler.updatePlayerAttributes(player);
                            ModNetworking.syncToPlayer(player);

                            // Visual & Audio fanfare
                            ServerLevel sl = player.serverLevel();
                            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 0.5, 0.5, 0.2);
                            sl.sendParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 0.5, player.getZ(), 40, 0.5, 0.3, 0.5, 0.1);
                            player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

                            com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.FIRST_ASCENSION);
                            if (rpgData.getRank().getIndex() >= com.pablo.rpglevels.data.AdventurerRank.B.getIndex()) {
                                com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.RANK_B);
                            }

                            Component rankBadge = rpgData.getRank().getBadge();
                            player.sendSystemMessage(Component.empty());
                            player.sendSystemMessage(Component.translatable("rpglevels.ascension.success.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                            player.sendSystemMessage(Component.translatable("rpglevels.ascension.success.desc", rankBadge).withStyle(ChatFormatting.GREEN));
                            player.sendSystemMessage(Component.empty());

                            responseDialogue = "§6\"¡Ascensión concedida, " + playerName + "! Por tus méritos y dedicación, te nombro oficialmente " + rankBadge.getString() + "§6 del Gremio de Aventureros. ¡Que tu temple inspire a toda la orden!\"";
                        } else {
                            responseDialogue = "§c\"Ha ocurrido un error durante la ascensión formal.\"";
                        }
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

    private static void handleAliceAction(ServerPlayer player, com.pablo.rpglevels.entities.AliceNpcEntity alice, int action) {
        String playerName = player.getName().getString();
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (rpgData == null) return;

        com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.MEET_ALICE);

        String responseDialogue = "";

        switch (action) {
            case ACTION_CHAT -> {
                ItemStack sword = findSpiritualSword(player);
                if (sword != null && com.pablo.rpglevels.items.SpiritualSwordItem.isFirstReleaseUnlocked(sword)) {
                    responseDialogue = "§d\"¡Excelente trabajo al dominar la Primera Liberación de tu espada espiritual, " + playerName + "! Siente su filo... Aún queda mucho poder dormido por extraer de su esencia. Cuando estés listo y alcances una mayor maestría, podrás buscar dominar y conseguir tu Segunda Liberación.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);
                } else if (sword != null && com.pablo.rpglevels.items.SpiritualSwordItem.getAffinity(sword) >= 100) {
                    com.pablo.rpglevels.items.SpiritualSwordItem.setTrialReady(sword, true);
                    responseDialogue = "§d\"¡Magnífico, " + playerName + "! Siento la inmensa resonancia que emana de tu espada... ¡Has alcanzado el 100% de afinidad con ella! Has forjado un lazo indestructible, y el Espíritu de tu Espada está listo para manifestarse.\n\nEscúchame bien: para obtener tu §6Primera Liberación§d, deberás enfrentarte al mismísimo §cEspíritu de tu Espada§d en un duelo a muerte. Tendrá exactamente tus mismas estadísticas, tus mismas habilidades físicas y empuñará el arma liberada. Prepárate con tu mejor armadura, busca una zona despejada y tranquila, y cuando estés listo presiona §e[Z]§d para iniciar la prueba.\"";
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.1F);
                    player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.6, 0.4, 0.2);
                } else if (sword != null && com.pablo.rpglevels.items.SpiritualSwordItem.isSoulBound(sword)) {
                    int affinity = com.pablo.rpglevels.items.SpiritualSwordItem.getAffinity(sword);
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

                com.pablo.rpglevels.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
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

                    com.pablo.rpglevels.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                    if (questData != null) {
                        questData.claimReward("alice_spiritual_trial", player);
                        ModNetworking.syncQuestsToPlayer(player);
                    }

                    // Deliver Base Spiritual Sword and Spiritual Spellbook
                    ItemStack sword = new ItemStack(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SWORD.get());
                    if (!player.addItem(sword)) {
                        player.drop(sword, false);
                    }
                    ItemStack spellbook = new ItemStack(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SPELLBOOK.get());
                    if (!player.addItem(spellbook)) {
                        player.drop(spellbook, false);
                    }

                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);

                    com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.OBTAIN_SPIRITUAL_SWORD);

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
                    com.pablo.rpglevels.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
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
                com.pablo.rpglevels.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
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
                    com.pablo.rpglevels.quests.PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                    if (questData != null && !questData.isQuestActive("alice_cultist_hunt") && !questData.isQuestClaimed("alice_cultist_hunt")) {
                        questData.acceptQuest("alice_cultist_hunt");
                        ModNetworking.sendQuestToast(player, "Misión Rango A: El Segador Corrupto", "0/1", false);
                        ModNetworking.syncQuestsToPlayer(player);
                    }

                    List<ServerPlayer> travelers = new java.util.ArrayList<>();
                    travelers.add(player);

                    var party = com.pablo.rpglevels.party.PartyManager.getParty(player.getUUID());
                    if (party != null) {
                        for (java.util.UUID memberUuid : party.getMemberUUIDs()) {
                            ServerPlayer member = player.serverLevel().getServer().getPlayerList().getPlayer(memberUuid);
                            if (member != null && !member.getUUID().equals(player.getUUID()) && member.distanceToSqr(player) <= 1024.0) {
                                travelers.add(member);
                            }
                        }
                    }

                    ServerLevel abyssLevel = player.getServer().getLevel(com.pablo.rpglevels.world.dimension.ModDimensions.ABYSS_RIFT_LEVEL);

                    for (ServerPlayer traveler : travelers) {
                        traveler.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                com.pablo.rpglevels.init.ModMobEffects.ABYSSAL_WARD, 24000, 0, false, false, true
                        ));
                        com.pablo.rpglevels.world.dimension.AbyssTeleportHelper.enterAbyssRift(traveler);
                        traveler.sendSystemMessage(Component.literal("§5[✦] §dAlice ha abierto una brecha y protegido tu alma con su manto abisal (20m)."));
                    }

                    return;
                }
            }
            case ACTION_ALICE_REQUEST_WARD -> {
                float maxMana = rpgData != null ? rpgData.getMaxMana(player) : 0.0f;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.pablo.rpglevels.init.ModMobEffects.ABYSSAL_WARD, 24000, 0, false, false, true
                ));

                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2F, 1.5F);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                        player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.4, 0.6, 0.4, 0.15);
                player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX(), player.getY() + 1.2, player.getZ(), 25, 0.3, 0.5, 0.3, 0.08);

                if (maxMana >= 500.0f) {
                    responseDialogue = "§5«Tu energía espiritual ya es colosal (" + (int)maxMana + " MP). El Vacío del Abismo no tiene poder para expulsarte, pero aun así te otorgo mi bendición. ¡Ve con cuidado!»";
                } else {
                    responseDialogue = "§5«He sellado tu alma con mi manto protector. Durante los próximos 20 minutos el Vacío del Abismo no te expulsará. Aprovecha para explorar y hallar al Cultista. Si logras alcanzar 500 de Maná Máximo, no volverás a necesitar mi protección.»";
                }
            }
            case ACTION_GIFT -> {
                ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
                boolean alreadyDelivered = player.getPersistentData().getBoolean("AliceShadowbladeGifted");
                NpcSocialManager.GiftResult result = NpcSocialManager.evaluateAliceGift(heldItem, alreadyDelivered);

                if (result.affinityGain() > 0 && !heldItem.isEmpty()) {
                    boolean wasShadowblade = heldItem.is(com.pablo.rpglevels.items.ModItems.CULTIST_SPIRITUAL_SWORD.get());
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
                    ItemStack newSword = new ItemStack(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SWORD.get());
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
                    com.pablo.rpglevels.items.SpiritualSwordItem.BladeStyle newStyle =
                            com.pablo.rpglevels.items.SpiritualSwordItem.BladeStyle.values()[random.nextInt(com.pablo.rpglevels.items.SpiritualSwordItem.BladeStyle.values().length)];
                    com.pablo.rpglevels.items.SpiritualSwordItem.BladeElement newElement =
                            com.pablo.rpglevels.items.SpiritualSwordItem.BladeElement.ACTIVE_ELEMENTS[random.nextInt(com.pablo.rpglevels.items.SpiritualSwordItem.BladeElement.ACTIVE_ELEMENTS.length)];
                    com.pablo.rpglevels.items.SpiritualSwordItem.BladeRelease newRelease =
                            com.pablo.rpglevels.items.SpiritualSwordItem.BladeRelease.values()[random.nextInt(com.pablo.rpglevels.items.SpiritualSwordItem.BladeRelease.values().length)];
                    com.pablo.rpglevels.items.SpiritualSwordItem.BladeResonance newResonance =
                            com.pablo.rpglevels.items.SpiritualSwordItem.BladeResonance.values()[random.nextInt(com.pablo.rpglevels.items.SpiritualSwordItem.BladeResonance.values().length)];
                    int newVariant = random.nextInt(2);

                    // Update player RPG data (retains FirstReleaseUnlocked!)
                    rpgData.setSoulBladeTraits(newStyle.getId(), newElement.getId(), newRelease.getId(), newResonance.getId(), newVariant);

                    // Mutate any spiritual swords in inventory/hand
                    for (ItemStack item : player.getInventory().items) {
                        if (!item.isEmpty() && item.getItem() instanceof com.pablo.rpglevels.items.SpiritualSwordItem) {
                            com.pablo.rpglevels.items.SpiritualSwordItem.setBladeStyle(item, newStyle);
                            com.pablo.rpglevels.items.SpiritualSwordItem.setBladeElement(item, newElement);
                            com.pablo.rpglevels.items.SpiritualSwordItem.setBladeRelease(item, newRelease);
                            com.pablo.rpglevels.items.SpiritualSwordItem.setBladeResonance(item, newResonance);
                            com.pablo.rpglevels.items.SpiritualSwordItem.setBladeVariant(item, newVariant);
                            if (com.pablo.rpglevels.items.SpiritualSwordItem.isFirstRelease(item)) {
                                com.pablo.rpglevels.items.SpiritualSwordItem.syncAwakenedSpell(item);
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
        if (player.getMainHandItem().getItem() instanceof com.pablo.rpglevels.items.SpiritualSwordItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof com.pablo.rpglevels.items.SpiritualSwordItem) {
            return player.getOffhandItem();
        }
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof com.pablo.rpglevels.items.SpiritualSwordItem) {
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
}
