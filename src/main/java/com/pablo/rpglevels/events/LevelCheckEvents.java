package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AuraStatHelper;
import com.pablo.rpglevels.data.GreekGod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.init.ModBlocks;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class LevelCheckEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerRpgData data = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            boolean rolledFirstTime = false;
            if (data != null) {
                rolledFirstTime = data.rollBirthStatsIfNeeded(serverPlayer.getRandom());
            }

            ModAttributesHandler.updatePlayerAttributes(serverPlayer);

            if (rolledFirstTime && data != null) {
                serverPlayer.setHealth(serverPlayer.getMaxHealth());
                data.setCurrentMana(serverPlayer, data.getMaxMana(serverPlayer));

                int fort = data.getPermanentPoints(StatType.FORTITUDE);
                int dex = data.getPermanentPoints(StatType.DEXTERITY);
                int spi = data.getPermanentPoints(StatType.SPIRIT);
                int luck = data.getPermanentPoints(StatType.LUCK);

                serverPlayer.sendSystemMessage(Component.empty());
                serverPlayer.sendSystemMessage(Component.literal("§6✦ « DESPERTAR EN EL MUNDO » ✦").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                serverPlayer.sendSystemMessage(Component.literal("§7Has nacido con las siguientes estadísticas base innatas §8(Estándar = 5)§7:"));
                serverPlayer.sendSystemMessage(Component.literal("  §c🛡 Fortitud: §f" + fort + " §8| §b🎯 Destreza: §f" + dex + " §8| §d✨ Espíritu: §f" + spi + " §8| §a🍀 Suerte: §f" + luck));
                serverPlayer.sendSystemMessage(Component.literal("§e§oAbate a tu primer enemigo y duerme en una cama para acudir al Plano Astral y recibir la Bendición de un Dios."));
                serverPlayer.sendSystemMessage(Component.empty());
            }

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
            if (!event.isEndConquered()) {
                serverPlayer.setHealth(serverPlayer.getMaxHealth());
            }
            ModNetworking.syncToPlayer(serverPlayer);
            ModNetworking.syncMana(serverPlayer);
            ModNetworking.syncQuestsToPlayer(serverPlayer);
        }
    }

    /**
     * Grant RPG XP when looting naturally generated chests/containers for the first time
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof RandomizableContainerBlockEntity lootContainer && lootContainer.getLootTable() != null) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null && data.hasDivineBlessingUnlocked()) {
                int xpGain = 55;
                data.addRpgXp(player, xpGain);
                player.displayClientMessage(Component.literal("§b✦ +" + xpGain + " XP RPG §7(Tesoro Saqueado)"), true);
            }
        }
    }

    /**
     * Grant RPG XP when mining valuable ores
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasDivineBlessingUnlocked()) return;

        BlockState state = event.getState();
        int xpGain = 0;
        if (state.is(ModBlocks.VOIDSTEEL_ORE.get()) || state.is(Blocks.ANCIENT_DEBRIS)) {
            xpGain = 50;
        } else if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
            xpGain = 25;
        } else if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)
                || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
                || state.is(Blocks.NETHER_QUARTZ_ORE) || state.is(Blocks.NETHER_GOLD_ORE)) {
            xpGain = 8;
        }

        if (xpGain > 0) {
            data.addRpgXp(player, xpGain);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerRpgData rpgData = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
        long gameTime = serverPlayer.level().getGameTime();

        // Control y protección de la Cinemática de Resonancia Espiritual
        if (rpgData != null && rpgData.isSpiritualResonanceCinematicActive(gameTime)) {
            handleSpiritualResonanceCinematicTick(serverPlayer, rpgData, gameTime);
        }

        if (serverPlayer.onGround() || serverPlayer.isInWater() || serverPlayer.isInLava() || serverPlayer.onClimbable()) {
            if (rpgData != null && !rpgData.canDoubleJumpAirborne()) {
                rpgData.setCanDoubleJumpAirborne(true);
            }
        }

        if (serverPlayer.tickCount % 10 == 0) {
            if (rpgData != null && rpgData.isOppressiveAuraActive(gameTime)) {
                applyOppressiveAuraTick(serverPlayer, rpgData);
            }
        }

        if (serverPlayer.tickCount % 20 == 0) {
            ModAttributesHandler.updatePlayerAttributes(serverPlayer);
            checkAndPromptMilestones(serverPlayer);
            checkAndNotifyDawnAscension(serverPlayer);

            if (!com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
                PlayerRpgData data = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null && data.hasSkill("magic_affinity") && !PlayerRpgData.isHoldingReleasedSpiritualSword(serverPlayer)) {
                    int spirit = data.getTotalStatPoints(serverPlayer, StatType.SPIRIT);
                    float regen = 1.0f + (Math.max(0, spirit - 5) * 0.05f);
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

            if (target instanceof TamableAnimal tamed && tamed.isOwnedBy(player)) {
                continue;
            }

            float targetAura = AuraStatHelper.calculateEntityAura(target);
            float diff = casterAura - targetAura;

            if (diff <= 0) {
                continue;
            }

            if (diff < 15.0f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 0.75f);
            } else if (diff < 35.0f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 1.5f);
            } else if (diff < 60.0f) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 2.5f);
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 3, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 35, 0, false, false));
                target.hurt(player.damageSources().indirectMagic(player, player), 4.0f);
            }

            if (player.serverLevel().random.nextFloat() < 0.40f) {
                player.serverLevel().sendParticles(
                        ParticleTypes.PORTAL,
                        target.getX(), target.getY() + (target.getBbHeight() * 0.5), target.getZ(),
                        5, 0.25, 0.35, 0.25, -0.2
                );
            }
        }
    }

    private static void handleSpiritualResonanceCinematicTick(ServerPlayer player, PlayerRpgData data, long gameTime) {
        // 1. Invulnerabilidad absoluta mediante Resistencia V
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 4, false, false, false));

        // 2. Estabilizar movimiento horizontal durante la pose cinemática
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.1, 1.0, 0.1));

        // 3. Ralentizar fuertemente a enemigos cercanos (Slowness V, Weakness III) en radio de 16 bloques
        if (player.tickCount % 5 == 0) {
            AABB auraBox = player.getBoundingBox().inflate(16.0);
            java.util.List<LivingEntity> nearby = player.serverLevel().getEntitiesOfClass(
                    LivingEntity.class, auraBox,
                    e -> e != player && e.isAlive() && !e.isAlliedTo(player) && !e.isSpectator()
            );
            for (LivingEntity mob : nearby) {
                if (mob instanceof TamableAnimal tamed && tamed.isOwnedBy(player)) continue;
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 4, false, false, false)); // Slowness V
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 2, false, false, false)); // Weakness III
            }
        }

        // 4. Repeler proyectiles en radio de 10 bloques
        AABB projBox = player.getBoundingBox().inflate(10.0);
        java.util.List<Projectile> projectiles = player.serverLevel().getEntitiesOfClass(
                Projectile.class, projBox,
                p -> p.getOwner() != player
        );
        for (Projectile proj : projectiles) {
            Vec3 diff = proj.position().subtract(player.position());
            Vec3 repelDir = new Vec3(diff.x, 0.35, diff.z).normalize().scale(1.25);
            proj.setDeltaMovement(repelDir);
            proj.hasImpulse = true;
            player.serverLevel().sendParticles(ParticleTypes.CRIT, proj.getX(), proj.getY(), proj.getZ(), 4, 0.1, 0.1, 0.1, 0.05);
        }

        // 5. Partículas de carga o explosión de shockwave
        long remaining = data.getSpiritualResonanceCinematicUntilTick() - gameTime;
        if (remaining > 35) {
            // Fase de concentración de energía hacia el personaje
            if (player.tickCount % 2 == 0) {
                player.serverLevel().sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX() + (Math.random() - 0.5) * 1.8,
                        player.getY() + Math.random() * 2.0,
                        player.getZ() + (Math.random() - 0.5) * 1.8,
                        3, 0.05, 0.1, 0.05, 0.02
                );
            }
        } else if (remaining == 35) {
            // ¡MOMENTO EXACTO DE LA EXPLOSIÓN SHOCKWAVE EN TICK 35!
            triggerSpiritualResonanceShockwave(player);
        }
    }

    private static void triggerSpiritualResonanceShockwave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // Sonidos colosales de explosión y trueno
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.85f);
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.8f, 1.15f);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.4f, 1.35f);

        // Flash blanco y onda expansiva de partículas
        level.sendParticles(ParticleTypes.FLASH, px, py + 1.2, pz, 2, 0, 0, 0, 0);

        // Anillo de choque expansivo (36 partículas en círculo)
        int ringParticles = 36;
        for (int i = 0; i < ringParticles; i++) {
            double angle = (2 * Math.PI * i) / ringParticles;
            double vx = Math.cos(angle) * 0.85;
            double vz = Math.sin(angle) * 0.85;
            level.sendParticles(ParticleTypes.SONIC_BOOM, px, py + 1.0, pz, 1, vx, 0.0, vz, 0.4);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py + 0.5, pz, 2, vx * 0.5, 0.1, vz * 0.5, 0.2);
        }

        // Empujar hacia atrás (knockback) a todos los enemigos en un radio de 10 bloques
        AABB blastBox = player.getBoundingBox().inflate(10.0);
        java.util.List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, blastBox,
                e -> e != player && e.isAlive() && !e.isAlliedTo(player) && !e.isSpectator()
        );
        for (LivingEntity target : targets) {
            if (target instanceof TamableAnimal tamed && tamed.isOwnedBy(player)) continue;
            Vec3 dir = target.position().subtract(player.position());
            Vec3 knock = new Vec3(dir.x, 0.45, dir.z).normalize().scale(1.85);
            target.setDeltaMovement(knock);
            target.hurtMarked = true;
        }

        // Recalcular atributos inmediatamente
        ModAttributesHandler.updatePlayerAttributes(player);
    }

    public static void checkAndNotifyDawnAscension(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasDivineBlessingUnlocked() || data.getRank().isMaxRank()) {
            return;
        }

        if (!data.isAtMilestoneCap()) {
            return;
        }

        long dayTime = player.level().getDayTime();
        long currentDay = dayTime / 24000L;
        long timeOfDay = dayTime % 24000L;

        long lastDawnNotified = player.getPersistentData().getLong("LastAscensionDawnDay");
        if (currentDay != lastDawnNotified && timeOfDay >= 0 && timeOfDay < 600) {
            player.getPersistentData().putLong("LastAscensionDawnDay", currentDay);

            com.pablo.rpglevels.data.AdventurerRank nextRank = data.getRank().getNextRank();
            GreekGod god = data.getPatronGod();
            String godName = (god != null) ? god.getDisplayName() : "tu Dios";

            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§d⚡ « LLAMADO ASTRAL DE ASCENSIÓN »").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("§eHas alcanzado el Hito de Nivel " + data.getCurrentMilestoneCap() + ". §b" + godName + " §ete aguarda en sueños para otorgarte la Ascensión a ").append(nextRank.getBadge()).append(Component.literal("§e.")));
            player.sendSystemMessage(Component.literal("§7Duerme o descansa en una cama para ingresar al §dEspacio Astral§7."));
            player.sendSystemMessage(Component.empty());
            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public static void checkAndPromptMilestones(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasDivineBlessingUnlocked()) {
            return;
        }

        int currentLevel = data.getRpgLevel();

        if (!data.getRank().isMaxRank()) {
            int reqAscensionLevel = data.getRank().getRequiredAscensionLevel();
            if (currentLevel >= reqAscensionLevel && data.getLastNotifiedMilestone() < reqAscensionLevel) {
                data.setLastNotifiedMilestone(reqAscensionLevel);
                sendAscensionPrompt(player, data.getRank().getNextRank(), reqAscensionLevel);
            }
        }
    }

    public static void sendAscensionPrompt(ServerPlayer player, com.pablo.rpglevels.data.AdventurerRank nextRank, int reqLevel) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        GreekGod god = (data != null) ? data.getPatronGod() : null;
        String godName = (god != null) ? god.getDisplayName() : "tu Dios";

        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§d« HITO DE RANGO ALCANZADO - ASCENSIÓN DISPONIBLE »").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("§e¡Has alcanzado el Nivel RPG " + reqLevel + "! §b" + godName + " §eha reconocido tus hazañas para ascender a ").append(nextRank.getBadge()).append(Component.literal("§e.")));
        player.sendSystemMessage(Component.literal("§6Duerme en cualquier cama para viajar al Espacio Astral y recibir tu Ascensión Divina."));
        player.sendSystemMessage(Component.empty());
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static void sendMilestonePrompt(ServerPlayer player, int level, int pendingPoints) {
        player.sendSystemMessage(Component.literal("§6⚔ ¡Puntos de Atributos Disponibles! §e(Nivel RPG " + level + ") §7- Tienes §a" + pendingPoints + " §7punto(s) pendiente(s). Pulsa §b[K] §7para asignarlos."));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75f, 1.2f);
    }
}
