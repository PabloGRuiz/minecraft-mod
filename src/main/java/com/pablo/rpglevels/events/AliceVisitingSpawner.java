package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.entities.AliceNpcEntity;
import com.pablo.rpglevels.entities.ModEntities;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Controla la aparición aleatoria y periódica de Alice cerca de la base o ubicación
 * del jugador al estilo del Comerciante Errante (Wandering Trader).
 *
 * Características:
 * 1. Aparece de forma periódica en el Overworld cerca de la base del jugador (prioriza camas/puntos de reaparición).
 * 2. Su estancia dura 48000 ticks (2 días de Minecraft = 40 minutos).
 * 3. Si el jugador está realizando activamente su misión de purificación, NO se marcha ni despawnea hasta completarla.
 * 4. Efectos visuales de portal y sonido de campana de amatista al llegar y partir.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class AliceVisitingSpawner {

    public static final String TAG_LAST_VISIT = "LastAliceVisitTick";
    public static final long VISIT_COOLDOWN_TICKS = 48000L; // 2 días de Minecraft (40 minutos)
    public static final int VISIT_DURATION_TICKS = 36000;   // 30 minutos de visita

    /**
     * Comprobación periódica cada 2400 ticks (2 minutos).
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        try {
            ServerLevel overworld = event.getServer().overworld();
            long gameTime = overworld.getGameTime();

            if (gameTime % 2400 != 0) {
                return;
            }

            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                if (player.isSpectator() || !player.isAlive()) continue;
                if (player.level().dimension() != Level.OVERWORLD) continue;

                // Cooldown por jugador
                long lastVisit = player.getPersistentData().getLong(TAG_LAST_VISIT);
                if (lastVisit > 0 && (gameTime - lastVisit) < VISIT_COOLDOWN_TICKS) {
                    continue;
                }

                // Evitar duplicados si ya hay una Alice en 80 bloques
                List<AliceNpcEntity> nearbyAlices = player.serverLevel().getEntitiesOfClass(
                        AliceNpcEntity.class,
                        player.getBoundingBox().inflate(80, 24, 80)
                );
                if (!nearbyAlices.isEmpty()) {
                    player.getPersistentData().putLong(TAG_LAST_VISIT, gameTime);
                    continue;
                }

                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                boolean hasSword = data != null && data.hasReceivedSpiritualSword();

                // Probabilidad balanceada: 20% si no tiene espada; 5% si ya la tiene
                float spawnChance = hasSword ? 0.05f : 0.20f;
                if (player.getRandom().nextFloat() > spawnChance) {
                    // Reintentar tras 15 minutos en vez de saturar
                    player.getPersistentData().putLong(TAG_LAST_VISIT, gameTime - VISIT_COOLDOWN_TICKS + 18000L);
                    continue;
                }

                trySpawnAliceNearPlayer(player, false);
            }
        } catch (Throwable t) {
            RpgLevelsMod.LOGGER.error("Error en AliceVisitingSpawner onServerTick: ", t);
        }
    }

    /**
     * Intenta hacer aparecer a Alice cerca de la base o posición del jugador.
     */
    public static boolean trySpawnAliceNearPlayer(ServerPlayer player, boolean force) {
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        // 1. Identificar punto de anclaje de base: Si tiene cama / spawn point cercano (a menos de 120 bloques), usar su base
        BlockPos anchor = playerPos;
        BlockPos respawnPos = player.getRespawnPosition();
        if (respawnPos != null && player.level().dimension() == Level.OVERWORLD) {
            if (respawnPos.closerThan(playerPos, 120)) {
                anchor = respawnPos;
            }
        }

        // 2. Buscar posición segura al aire libre a 14-26 bloques de distancia
        BlockPos spawnPos = findSafeVisitingSpawnPos(level, anchor);
        if (spawnPos == null) {
            spawnPos = findSafeVisitingSpawnPos(level, playerPos);
        }

        if (spawnPos == null) {
            return false;
        }

        // 3. Crear y configurar a Alice como visitante
        AliceNpcEntity alice = ModEntities.ALICE.get().create(level);
        if (alice == null) return false;

        alice.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.getRandom().nextFloat() * 360f, 0.0f);
        alice.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
        alice.setDespawnDelay(VISIT_DURATION_TICKS);
        level.addFreshEntity(alice);

        // 4. Actualizar cooldown en los datos del jugador
        player.getPersistentData().putLong(TAG_LAST_VISIT, level.getGameTime());

        // 5. Efectos de llegada
        level.sendParticles(ParticleTypes.PORTAL,
                spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                35, 0.4, 0.7, 0.4, 0.15);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT,
                spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                25, 0.4, 0.5, 0.4, 0.1);
        level.playSound(null, spawnPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.2F, 1.2F);
        level.playSound(null, spawnPos, SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 0.8F, 1.5F);

        // 6. Mensaje en chat al jugador
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§5[✦] §dUna misteriosa viajera de los Segadores Espirituales ha llegado cerca de tu base...")
                .withStyle(ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal(" §7«Busca a Alice para conocer los secretos de las Espadas Espirituales.»")
                .withStyle(ChatFormatting.DARK_GRAY));
        player.sendSystemMessage(Component.empty());

        return true;
    }

    private static BlockPos findSafeVisitingSpawnPos(ServerLevel level, BlockPos origin) {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < 40; attempt++) {
            int dx = random.nextIntBetweenInclusive(14, 26) * (random.nextBoolean() ? 1 : -1);
            int dz = random.nextIntBetweenInclusive(14, 26) * (random.nextBoolean() ? 1 : -1);

            BlockPos checkPos = origin.offset(dx, 0, dz);
            if (!level.hasChunkAt(checkPos)) continue;

            for (int dy = -4; dy <= 6; dy++) {
                BlockPos candidate = checkPos.above(dy);
                BlockPos ground = candidate.below();
                BlockState groundState = level.getBlockState(ground);
                BlockState feetState = level.getBlockState(candidate);
                BlockState headState = level.getBlockState(candidate.above());

                boolean isWalkable = groundState.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP)
                        || groundState.is(Blocks.DIRT_PATH)
                        || groundState.is(Blocks.FARMLAND)
                        || groundState.is(net.minecraft.tags.BlockTags.DIRT)
                        || groundState.blocksMotion();

                boolean isClear = (feetState.isAir() || feetState.canBeReplaced() || feetState.getCollisionShape(level, candidate).isEmpty())
                        && (headState.isAir() || headState.canBeReplaced() || headState.getCollisionShape(level, candidate.above()).isEmpty())
                        && !feetState.liquid()
                        && !groundState.is(Blocks.LAVA)
                        && !groundState.is(Blocks.MAGMA_BLOCK);

                if (isWalkable && isClear) {
                    return candidate;
                }
            }
        }

        return null;
    }
}

