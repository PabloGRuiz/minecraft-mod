package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.entities.ModEntities;
import com.pablo.tenseicraft.entities.WindMasterNpcEntity;
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
 * Controla la aparición aleatoria y periódica de Kaelen cerca de la base o ubicación
 * del jugador al estilo del Comerciante Errante (Wandering Trader), con una probabilidad
 * ligeramente menor a la de Alice.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class KaelenVisitingSpawner {

    public static final String TAG_LAST_VISIT = "LastKaelenVisitTick";
    public static final long VISIT_COOLDOWN_TICKS = 48000L; // 2 días de Minecraft (40 minutos)
    public static final int VISIT_DURATION_TICKS = 36000;   // 30 minutos de visita

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

                // Evitar duplicados si ya hay una Kaelen en 80 bloques
                List<WindMasterNpcEntity> nearbyKaelens = player.serverLevel().getEntitiesOfClass(
                        WindMasterNpcEntity.class,
                        player.getBoundingBox().inflate(80, 24, 80)
                );
                if (!nearbyKaelens.isEmpty()) {
                    player.getPersistentData().putLong(TAG_LAST_VISIT, gameTime);
                    continue;
                }

                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                boolean hasDash = data != null && data.hasSkill("agility_dash");

                // Probabilidad balanceada: 15% si no tiene Dash; 3% si ya lo tiene
                float spawnChance = hasDash ? 0.03f : 0.15f;
                if (player.getRandom().nextFloat() > spawnChance) {
                    player.getPersistentData().putLong(TAG_LAST_VISIT, gameTime - VISIT_COOLDOWN_TICKS + 18000L);
                    continue;
                }

                trySpawnKaelenNearPlayer(player, false);
            }
        } catch (Throwable t) {
            TenseiCraftMod.LOGGER.error("Error en KaelenVisitingSpawner onServerTick: ", t);
        }
    }

    public static boolean trySpawnKaelenNearPlayer(ServerPlayer player, boolean force) {
        ServerLevel level = player.serverLevel();
        BlockPos playerPos = player.blockPosition();

        // 1. Identificar anclaje de base
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

        // 3. Crear y configurar a Kaelen como visitante
        WindMasterNpcEntity kaelen = ModEntities.WIND_MASTER_NPC.get().create(level);
        if (kaelen == null) return false;

        kaelen.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                level.getRandom().nextFloat() * 360f, 0.0f);
        kaelen.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
        kaelen.setCustomName(Component.literal("§a[Maestra del Dash] §fKaelen"));
        kaelen.setCustomNameVisible(true);
        kaelen.setDespawnDelay(VISIT_DURATION_TICKS);
        level.addFreshEntity(kaelen);

        // 4. Actualizar cooldown
        player.getPersistentData().putLong(TAG_LAST_VISIT, level.getGameTime());

        // 5. Efectos de llegada
        level.sendParticles(ParticleTypes.GUST,
                spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                30, 0.4, 0.6, 0.4, 0.1);
        level.sendParticles(ParticleTypes.CLOUD,
                spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, spawnPos, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.NEUTRAL, 1.2F, 1.3F);

        // 6. Mensaje en chat al jugador
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§a[✦] §bUna brisa tempestuosa anuncia la llegada de Kaelen cerca de tu base...")
                .withStyle(ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal(" §7«Búscala si deseas aprender el secreto del Dash o batirte en duelo con ella.»")
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

