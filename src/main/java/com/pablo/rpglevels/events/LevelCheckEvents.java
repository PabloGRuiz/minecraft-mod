package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import com.pablo.rpglevels.data.AuraStatHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class LevelCheckEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            ModAttributesHandler.updatePlayerAttributes(serverPlayer);
            ModNetworking.syncToPlayer(serverPlayer);
            ModNetworking.syncMana(serverPlayer);
            checkAndPromptMilestones(serverPlayer);
            MagicAdvancementEvents.checkRetroactiveAdvancement(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            ModAttributesHandler.updatePlayerAttributes(serverPlayer);
            ModNetworking.syncToPlayer(serverPlayer);
            ModNetworking.syncMana(serverPlayer);
            ModNetworking.syncQuestsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            ModAttributesHandler.updatePlayerAttributes(serverPlayer);
            ModNetworking.syncToPlayer(serverPlayer);
            ModNetworking.syncMana(serverPlayer);
            ModNetworking.syncQuestsToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Reset double jump availability when player lands on ground, enters water/lava, or climbs
        if (serverPlayer.onGround() || serverPlayer.isInWater() || serverPlayer.isInLava() || serverPlayer.onClimbable()) {
            PlayerRpgData rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            if (rpgData != null && !rpgData.canDoubleJumpAirborne()) {
                rpgData.setCanDoubleJumpAirborne(true);
            }
        }

        // Aura Opresiva (Presión Espiritual Cilíndrica): Se ejecuta cada 10 ticks (0.5s)
        if (serverPlayer.tickCount % 10 == 0) {
            PlayerRpgData rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            if (rpgData != null && rpgData.isOppressiveAuraActive(serverPlayer.level().getGameTime())) {
                applyOppressiveAuraTick(serverPlayer, rpgData);
            }
        }

        // Only run check every second (20 ticks) for efficiency
        if (serverPlayer.tickCount % 20 == 0) {
            ModAttributesHandler.updatePlayerAttributes(serverPlayer);
            checkAndPromptMilestones(serverPlayer);
            checkAndNotifyDawnAscension(serverPlayer);

            // Passive Mana Regeneration (Solo si Iron's Spells NO está instalado; con Iron's Spells la regeneración la gestiona su MagicManager nativo según MANA_REGEN)
            if (!com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
                PlayerRpgData data = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null && data.hasSkill("magic_affinity") && !PlayerRpgData.isHoldingReleasedSpiritualSword(serverPlayer)) {
                    int spirit = data.getTotalStatPoints(serverPlayer, com.pablo.rpglevels.data.StatType.SPIRIT);
                    float regen = 1.0f + (spirit * 0.05f);
                    if (data.isSpiritSurgeActive(serverPlayer.level().getGameTime())) {
                        regen *= 2.0f;
                    }
                    float before = data.getCurrentMana(serverPlayer);
                    data.regenerateMana(serverPlayer, regen);
                    float after = data.getCurrentMana(serverPlayer);
                    if (Math.abs(after - before) > 0.01f) {
                        ModNetworking.syncMana(serverPlayer);
                    }
                }
            }
        }
    }

    private static void applyOppressiveAuraTick(ServerPlayer player, PlayerRpgData data) {
        float casterAura = data.getAuraStat(player);
        double radius = Math.min(24.0, 6.0 + (casterAura * 0.10));
        AABB box = new AABB(
                player.getX() - radius, player.getY() - 3.0, player.getZ() - radius,
                player.getX() + radius, player.getY() + 10.0, player.getZ() + radius
        );

        java.util.List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive() && !e.isSpectator()
        );

        for (LivingEntity target : targets) {
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }

            // No afectar a mascotas domesticadas del propio jugador
            if (target instanceof TamableAnimal tamed && tamed.isOwnedBy(player)) {
                continue;
            }

            float targetAura = AuraStatHelper.calculateEntityAura(target);
            float diff = casterAura - targetAura;

            if (diff <= 0) {
                // El objetivo tiene igual o mayor aura: resiste la presión
                continue;
            }

            // Efectos según diferencia de poder de aura:
            // 1. diff < 15: Leve pesadez
            if (diff < 15.0f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 0.75f);
            }
            // 2. 15 <= diff < 35: Presión notable
            else if (diff < 35.0f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 1.5f);
            }
            // 3. 35 <= diff < 60: Presión aplastante
            else if (diff < 60.0f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 2.5f);
            }
            // 4. diff >= 60: Colapso gravitacional y ceguera sensorial
            else {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 3, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 35, 0, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 4.0f);
            }

            // Partículas de distorsión gravitacional sobre los afectados
            if (player.serverLevel().random.nextFloat() < 0.40f) {
                player.serverLevel().sendParticles(
                        ParticleTypes.PORTAL,
                        target.getX(), target.getY() + (target.getBbHeight() * 0.5), target.getZ(),
                        5, 0.25, 0.35, 0.25, -0.2
                );
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            checkAndNotifyDawnAscension(serverPlayer);
        }
    }

    public static void checkAndNotifyDawnAscension(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.isGuildRegistered() || data.getRank().isMaxRank()) {
            return;
        }

        int reqLevel = data.getRank().getRequiredAscensionLevel();
        if (player.experienceLevel < reqLevel) {
            return;
        }

        long dayTime = player.level().getDayTime();
        long currentDay = dayTime / 24000L;
        long timeOfDay = dayTime % 24000L;

        // Morning dawn window (0 <= timeOfDay < 600)
        long lastDawnNotified = player.getPersistentData().getLong("LastAscensionDawnDay");
        if (currentDay != lastDawnNotified && timeOfDay >= 0 && timeOfDay < 600) {
            player.getPersistentData().putLong("LastAscensionDawnDay", currentDay);

            com.pablo.rpglevels.data.AdventurerRank nextRank = data.getRank().getNextRank();
            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§6🌅 « EL ALBA TRAE NUEVOS DESAFÍOS »").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("§e¡El sol ilumina tus hazañas! Tienes el nivel suficiente (" + player.experienceLevel + "/" + reqLevel + ") para ascender a ").append(nextRank.getBadge()).append(Component.literal("§e.")));
            player.sendSystemMessage(Component.literal("§7Preséntate ante el §6Maestro del Gremio §7en cualquier aldea para formalizar tu nuevo rango."));
            player.sendSystemMessage(Component.empty());
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public static void checkAndPromptMilestones(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return;
        }

        int currentLevel = player.experienceLevel;

        // Check for Rank Ascension if registered in Guild
        if (data.isGuildRegistered() && !data.getRank().isMaxRank()) {
            int reqAscensionLevel = data.getRank().getRequiredAscensionLevel();
            if (currentLevel >= reqAscensionLevel && data.getLastNotifiedMilestone() < reqAscensionLevel) {
                data.setLastNotifiedMilestone(reqAscensionLevel);
                sendAscensionPrompt(player, data.getRank().getNextRank(), reqAscensionLevel);
                return;
            }
        }

        // Check for newly reached attribute point (every 2 levels)
        int currentPending = data.getPendingPointsCount(currentLevel);
        if (currentPending > 0 && currentLevel >= 2) {
            int currentMilestoneLevel = (currentLevel / 2) * 2;
            if (currentMilestoneLevel > data.getLastNotifiedMilestone()) {
                data.setLastNotifiedMilestone(currentMilestoneLevel);
                sendMilestonePrompt(player, currentLevel, currentPending);
            }
        }
    }

    public static void sendAscensionPrompt(ServerPlayer player, com.pablo.rpglevels.data.AdventurerRank nextRank, int reqLevel) {
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§6« ASCENSIÓN DE RANGO DISPONIBLE »").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("§e¡Has alcanzado el Nivel " + reqLevel + "! Tienes los méritos necesarios para ascender formalmente a ").append(nextRank.getBadge()).append(Component.literal("§e.")));
        player.sendSystemMessage(Component.literal("§6Visita a un Maestro del Gremio en cualquier aldea para formalizar tu Ascensión de Rango."));
        player.sendSystemMessage(Component.empty());
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void sendMilestonePrompt(ServerPlayer player, int level, int pendingPoints) {
        player.sendSystemMessage(Component.literal("§6⚔ ¡Puntos de Atributos Disponibles! §e(Nivel " + level + ") §7- Tienes §a" + pendingPoints + " §7punto(s) pendiente(s). Pulsa §b[K] §7para asignarlos."));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.2f);
    }
}
