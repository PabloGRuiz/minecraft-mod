package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.entities.ModEntities;
import com.pablo.tenseicraft.entities.SwordSpiritEntity;
import com.pablo.tenseicraft.events.ModAttributesHandler;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleSwordReleasePayload(boolean unused) implements CustomPacketPayload {

    public static final Type<ToggleSwordReleasePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "toggle_sword_release"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSwordReleasePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    ToggleSwordReleasePayload::unused,
                    ToggleSwordReleasePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleSwordReleasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.isSpectator() || player.isDeadOrDying()) {
                return;
            }

            ItemStack stack = findHeldSpiritualSword(player);
            if (stack == null || stack.isEmpty()) {
                player.displayClientMessage(Component.literal("§7Debes empuñar tu Espada Espiritual para canalizar su liberación."), true);
                return;
            }

            if (!SpiritualSwordItem.isSoulBound(stack)) {
                player.displayClientMessage(Component.literal("§cLa espada aún no tiene vínculo forjado. Nómbrala y canaliza 200 de Maná."), true);
                return;
            }

            String ownerUuid = SpiritualSwordItem.getOwnerUUID(stack);
            if (!ownerUuid.isEmpty() && !ownerUuid.equals(player.getUUID().toString())) {
                player.displayClientMessage(Component.literal("§cEsta espada espiritual está vinculada al alma de " + SpiritualSwordItem.getOwnerName(stack) + " y rechaza tu control."), true);
                return;
            }

            // 1. Si la Primera Liberación YA está desbloqueada: Alternar estado activo / inactivo
            if (SpiritualSwordItem.isFirstReleaseUnlocked(stack)) {
                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                boolean current = SpiritualSwordItem.isFirstRelease(stack);
                boolean next = !current;

                int aff = SpiritualSwordItem.getAffinity(stack);
                float progress = Math.min(1.0f, Math.max(0.0f, (aff - 100) / 100.0f));
                // Coste bestial de activación: 80 MP a afinidad 100, reduciéndose a 25 MP a afinidad 200
                float activationCost = 80.0f - (progress * 55.0f);

                if (next && data != null) {
                    float curMana = data.getCurrentMana(player);
                    if (curMana < activationCost) {
                        player.displayClientMessage(
                                Component.literal("§c¡Tu energía espiritual es insuficiente para desatar la Primera Liberación! Requiere §e"
                                        + (int) Math.ceil(activationCost) + " MP §c(Tienes: " + (int) curMana + " MP)."),
                                true
                        );
                        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.4f);
                        return;
                    }

                    data.consumeMana(player, activationCost);
                }

                SpiritualSwordItem.setFirstRelease(stack, next);
                SpiritualSwordItem.syncAwakenedSpell(stack);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        player,
                        new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                );

                if (data != null) {
                    data.setFirstReleaseUnlocked(true);
                    if (next) {
                        data.setLastReleasedManaCap(data.getCurrentMana(player));
                    } else {
                        data.setLastReleasedManaCap(-1.0f);
                    }
                }

                ModAttributesHandler.updatePlayerAttributes(player);
                ModNetworking.syncToPlayer(player);

                ServerLevel sl = player.serverLevel();
                if (next) {
                    if (SpiritualSwordItem.isSenbonzakura(stack)) {
                        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, player.getX(), player.getY() + 1.0, player.getZ(), 80, 0.6, 0.8, 0.6, 0.15);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHERRY_WOOD_BREAK, SoundSource.PLAYERS, 1.3f, 1.0f);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.4f, 1.2f);
                        player.displayClientMessage(Component.literal("§d✦ §l« Dispersa... Senbonzakura » §e(+50% Poder General)"), true);
                    } else {
                        SpiritualSwordItem.BladeElement elem = SpiritualSwordItem.getBladeElement(stack);
                        SpiritualSwordItem.spawnHeldElementalAura(sl, player, elem);
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.4, 0.5, 0.4, 0.15);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0f, 1.2f);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.4f);
                        player.displayClientMessage(Component.literal("§d✦ §l¡Primera Liberación Activada! §e(+50% Poder General: Daño, Atributos y Hechizos)"), true);
                    }
                    com.pablo.tenseicraft.advancements.ModAdvancements.award(player, com.pablo.tenseicraft.advancements.ModAdvancements.FIRST_RELEASE);
                } else {
                    if (SpiritualSwordItem.isSenbonzakura(stack)) {
                        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                        player.displayClientMessage(Component.literal("§7Senbonzakura se recompone en su vaina."), true);
                    } else {
                        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                        player.displayClientMessage(Component.literal("§7Primera Liberación desactivada."), true);
                    }
                }
                return;
            }

            // 2. Si aún NO está desbloqueada: Comprobar requisitos para el Duelo contra el Espíritu
            int affinity = SpiritualSwordItem.getAffinity(stack);
            if (affinity < 100) {
                player.displayClientMessage(Component.literal("§cTu afinidad con la espada es de " + affinity + "/100. Necesitas alcanzar 100 de afinidad antes de la prueba."), true);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return;
            }

            if (!SpiritualSwordItem.isTrialReady(stack)) {
                player.displayClientMessage(Component.literal("§dTu espada vibra con máxima afinidad. Habla primero con §bAlice §dpara recibir sus instrucciones."), true);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.8f, 1.2f);
                return;
            }

            // 3. Confirmación de combate de 10 segundos
            long gameTime = player.level().getGameTime();
            long confirmUntil = player.getPersistentData().getLong("SwordSpiritTrialConfirmUntil");

            if (gameTime > confirmUntil) {
                // Primer toque: solicitar confirmación
                player.getPersistentData().putLong("SwordSpiritTrialConfirmUntil", gameTime + 200L); // 10s

                player.sendSystemMessage(Component.empty());
                player.sendSystemMessage(Component.literal("§c§l[ADVERTENCIA] ¿Estás verdaderamente preparado para desafiar al Espíritu de tu Espada?"));
                player.sendSystemMessage(Component.literal("§eAsegúrate de estar en una zona despejada y con tu mejor equipo."));
                player.sendSystemMessage(Component.literal("§6Presiona §e[Z] §6nuevamente en los próximos §c10 segundos §6para iniciar el combate a muerte."));
                player.sendSystemMessage(Component.empty());

                player.displayClientMessage(Component.literal("§c¿Listo para el combate? §ePresiona [Z] de nuevo para confirmar."), true);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7f, 1.4f);
            } else {
                // Segundo toque dentro de 10 segundos: ¡Invocar al Espíritu de la Espada!
                player.getPersistentData().remove("SwordSpiritTrialConfirmUntil");

                ServerLevel sl = player.serverLevel();
                Vec3 look = player.getLookAngle().normalize();
                Vec3 spawnPos = player.position().add(look.scale(3.5));

                SwordSpiritEntity boss = ModEntities.SWORD_SPIRIT.get().create(sl);
                if (boss != null) {
                    boss.moveTo(spawnPos.x, player.getY(), spawnPos.z, player.getYRot() + 180.0F, 0.0F);
                    boss.setupFromSummoner(player, stack);
                    sl.addFreshEntity(boss);

                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spawnPos.x, player.getY() + 1.0, spawnPos.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.PORTAL, spawnPos.x, player.getY() + 1.0, spawnPos.z, 60, 0.5, 0.8, 0.5, 0.2);
                    sl.playSound(null, spawnPos.x, player.getY(), spawnPos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.7f, 1.2f);
                    sl.playSound(null, spawnPos.x, player.getY(), spawnPos.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 0.9f);

                    player.sendSystemMessage(Component.empty());
                    player.sendSystemMessage(Component.literal("§5§l[✦] ¡El Espíritu de tu Espada se ha materializado ante ti!"));
                    player.sendSystemMessage(Component.literal("§dDerroca a tu propio reflejo para obtener tu §6Primera Liberación§d."));
                    player.sendSystemMessage(Component.empty());
                }
            }
        });
    }

    private static ItemStack findHeldSpiritualSword(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getOffhandItem();
        }
        return null;
    }
}

