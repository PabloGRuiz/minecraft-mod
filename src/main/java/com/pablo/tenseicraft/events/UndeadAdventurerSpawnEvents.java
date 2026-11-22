package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.ModEntities;
import com.pablo.tenseicraft.entities.UndeadAdventurerBossEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class UndeadAdventurerSpawnEvents {

    public static final String LAST_BOSS_KEY = "LastUndeadBossTime";
    public static final String DEFEATED_BOSS_KEY = "DefeatedUndeadBossOnce";
    private static final long BOSS_COOLDOWN_FIRST_TIME = 48000L;    // 40 min (2 días de Minecraft) antes de derrotarlo
    private static final long BOSS_COOLDOWN_AFTER_DEFEAT = 120000L; // 100 min (5 días de Minecraft) tras derrotarlo

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();

        // Check every 1200 ticks (60 seconds)
        if (gameTime % 1200 != 0) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            if (player.level().dimension() != Level.OVERWORLD) continue;

            // Trigger requirement: Minecraft Experience Level >= 30
            if (player.experienceLevel < 30) continue;

            ServerLevel level = player.serverLevel();

            // Solo aparece de noche o durante tormenta eléctrica
            if (!level.isNight() && !level.isThundering()) continue;

            long lastSpawn = player.getPersistentData().getLong(LAST_BOSS_KEY);
            boolean hasDefeatedBefore = player.getPersistentData().getBoolean(DEFEATED_BOSS_KEY);

            long cooldownRequired = hasDefeatedBefore ? BOSS_COOLDOWN_AFTER_DEFEAT : BOSS_COOLDOWN_FIRST_TIME;
            if (lastSpawn > 0 && (gameTime - lastSpawn) < cooldownRequired) continue;

            // Check if there is already an Undead Adventurer in the vicinity
            if (!level.getEntitiesOfClass(UndeadAdventurerBossEntity.class, player.getBoundingBox().inflate(100.0)).isEmpty()) {
                player.getPersistentData().putLong(LAST_BOSS_KEY, gameTime);
                continue;
            }

            // Probabilidad reducida y controlada: 8% antes de la 1ª derrota, 1.5% tras haberlo derrotado
            RandomSource random = level.getRandom();
            float spawnChance = hasDefeatedBefore ? 0.015F : 0.08F;
            if (random.nextFloat() < spawnChance) {
                BlockPos spawnPos = findBossSpawnPos(level, player.blockPosition());
                if (spawnPos != null) {
                    UndeadAdventurerBossEntity boss = ModEntities.UNDEAD_ADVENTURER.get().create(level);
                    if (boss != null) {
                        boss.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                random.nextFloat() * 360F, 0.0F);
                        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                        boss.setTarget(player);
                        level.addFreshEntity(boss);

                        player.getPersistentData().putLong(LAST_BOSS_KEY, gameTime);

                        // Atmospheric descent effects
                        level.playSound(null, spawnPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 0.8F);
                        level.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.6F, 0.75F);
                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, 1, 0, 0, 0, 0);

                        player.sendSystemMessage(Component.literal("§4§l[!] Un aura sepulcral desciende sobre la tierra..."));
                        player.sendSystemMessage(Component.literal("§5§l[Undead Adventurer]: §c\"¡Siento el poder de un mortal que ha alcanzado la cúspide... Veamos si eres digno de conservar tu alma!\""));
                    }
                }
            } else {
                // Si la probabilidad falló, aplicar un retraso de 12000 ticks (10 minutos) antes de volver a intentar
                player.getPersistentData().putLong(LAST_BOSS_KEY, gameTime - cooldownRequired + 12000L);
            }
        }
    }

    private static BlockPos findBossSpawnPos(ServerLevel level, BlockPos origin) {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < 16; attempt++) {
            // Spawn distance: 16 to 22 blocks away from player
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dist = 16 + random.nextInt(7);
            int dx = (int) Math.round(Math.cos(angle) * dist);
            int dz = (int) Math.round(Math.sin(angle) * dist);

            BlockPos checkCol = origin.offset(dx, 0, dz);
            if (!level.hasChunkAt(checkCol)) continue;

            for (int dy = 4; dy >= -4; dy--) {
                BlockPos candidate = checkCol.above(dy);
                BlockPos ground = candidate.below();
                BlockState groundState = level.getBlockState(ground);
                BlockState feetState = level.getBlockState(candidate);
                BlockState headState = level.getBlockState(candidate.above());

                if (groundState.isSolid() && feetState.isAir() && headState.isAir()) {
                    return candidate;
                }
            }
        }

        return null;
    }
}
