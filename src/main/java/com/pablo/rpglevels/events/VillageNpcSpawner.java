package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.entities.ModEntities;
import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles realistic, safe village-bound NPC spawning for Townswomen (female villagers)
 * and visiting Adventurers.
 *
 * Ensures:
 * 1. Female villagers and adventurers spawn safely inside villages/settlements during server ticks.
 * 2. Absolutely NO dangerous calls to structureManager during chunk generation or EntityJoinLevelEvent.
 * 3. They persist permanently in the village and do not despawn.
 * 4. Village population is balanced and capped based on village size.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class VillageNpcSpawner {

    // Cooldown map per village chunk to prevent excessive spatial checks
    private static final Map<ChunkPos, Long> VILLAGE_COOLDOWNS = new HashMap<>();

    /**
     * Periodic check around active players in the Overworld (every 60 ticks / 3 seconds).
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        try {
            long gameTime = event.getServer().overworld().getGameTime();

            // Clean up old cooldowns every 1200 ticks (1 minute)
            if (gameTime % 1200 == 0) {
                VILLAGE_COOLDOWNS.entrySet().removeIf(entry -> entry.getValue() < gameTime);
            }

            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                if (player.isSpectator() || !player.isAlive()) continue;
                if (player.level().dimension() != Level.OVERWORLD) continue;

                // Stagger check across players
                if ((player.tickCount + player.getId()) % 60 != 0) continue;

                ServerLevel level = player.serverLevel();
                BlockPos playerPos = player.blockPosition();
                if (!level.hasChunkAt(playerPos)) continue;

                ChunkPos chunkPos = new ChunkPos(playerPos);

                // Fast cooldown skip
                Long cd = VILLAGE_COOLDOWNS.get(chunkPos);
                if (cd != null && cd > gameTime) {
                    continue;
                }

                // Check: Are there villagers nearby (within 40 blocks of player)?
                List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                        Villager.class,
                        player.getBoundingBox().inflate(40, 16, 40)
                );

                // Comprobar si el jugador está en o cerca de la Casa del Gremio independiente en la planicie
                boolean inGuildHouse = false;
                BlockPos guildCenter = null;
                try {
                    var structReg = level.registryAccess().registry(net.minecraft.core.registries.Registries.STRUCTURE);
                    if (structReg.isPresent()) {
                        var guildStructure = structReg.get().get(
                                net.minecraft.resources.ResourceKey.create(
                                        net.minecraft.core.registries.Registries.STRUCTURE,
                                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "guild_house")
                                )
                        );
                        if (guildStructure != null) {
                            var start = level.structureManager().getStructureAt(playerPos, guildStructure);
                            if (start != null && start.isValid()) {
                                inGuildHouse = true;
                                guildCenter = start.getBoundingBox().getCenter();
                            }
                        }
                    }
                } catch (Throwable ignored) {}

                // Se requiere o bien una aldea con aldeanos o bien la presencia de la Casa del Gremio
                if (nearbyVillagers.isEmpty() && !inGuildHouse) {
                    VILLAGE_COOLDOWNS.put(chunkPos, gameTime + 600L); // 30 segundos de cooldown para chunks sin asentamientos
                    continue;
                }

                BlockPos anchorPos = !nearbyVillagers.isEmpty() ? nearbyVillagers.get(0).blockPosition() : (guildCenter != null ? guildCenter : playerPos);
                if (!level.hasChunkAt(anchorPos)) continue;

                // Inspect full settlement / guild area (radius 48 blocks)
                AABB settlementBounds = new AABB(anchorPos).inflate(48, 18, 48);
                List<Villager> allVillagers = level.getEntitiesOfClass(Villager.class, settlementBounds);

                // Only count free civilian townswomen towards quota
                List<TownswomanNpcEntity> existingTownswomen = level.getEntitiesOfClass(
                        TownswomanNpcEntity.class,
                        settlementBounds,
                        tw -> tw.getWorkerStatus() == TownswomanNpcEntity.WorkerStatus.CIVILIAN
                );
                List<AdventurerNpcEntity> existingAdventurers = level.getEntitiesOfClass(
                        AdventurerNpcEntity.class,
                        settlementBounds,
                        adv -> !adv.isHired()
                );
                List<com.pablo.rpglevels.entities.GuildMasterNpcEntity> existingGuildMasters = level.getEntitiesOfClass(
                        com.pablo.rpglevels.entities.GuildMasterNpcEntity.class,
                        settlementBounds
                );

                // Calculate targets based on settlement / guild house:
                int villagerCount = allVillagers.size();
                int targetTownswomen = inGuildHouse ? 2 : Math.min(5, Math.max(2, (villagerCount + 1) / 2));
                int targetAdventurers = inGuildHouse ? 2 : ((villagerCount >= 8) ? 2 : 1);

                boolean spawnedAny = false;

                // 0. Ensure 1 Guild Master per village / guild house
                if (existingGuildMasters.isEmpty()) {
                    BlockPos anchor = !allVillagers.isEmpty() ? allVillagers.get(level.getRandom().nextInt(allVillagers.size())).blockPosition() : anchorPos;
                    BlockPos spawnPos = findSafeSpawnPos(level, anchor);

                    if (spawnPos != null && level.hasChunkAt(spawnPos)) {
                        com.pablo.rpglevels.entities.GuildMasterNpcEntity master = ModEntities.GUILD_MASTER.get().create(level);
                        if (master != null) {
                            master.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                    level.getRandom().nextFloat() * 360f, 0.0f);
                            master.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                            master.setPersistenceRequired();
                            level.addFreshEntity(master);

                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                    spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                                    8, 0.3, 0.3, 0.3, 0.02);
                            level.playSound(null, spawnPos, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8f, 1.2f);
                            spawnedAny = true;
                        }
                    }
                }

                // 1. Replenish Townswomen if below target
                if (existingTownswomen.size() < targetTownswomen) {
                    BlockPos anchor = !allVillagers.isEmpty() ? allVillagers.get(level.getRandom().nextInt(allVillagers.size())).blockPosition() : anchorPos;
                    BlockPos spawnPos = findSafeSpawnPos(level, anchor);

                    if (spawnPos != null && level.hasChunkAt(spawnPos)) {
                        TownswomanNpcEntity townswoman = ModEntities.TOWNSWOMAN_NPC.get().create(level);
                        if (townswoman != null) {
                            townswoman.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                    level.getRandom().nextFloat() * 360f, 0.0f);
                            townswoman.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                            townswoman.setPersistenceRequired();
                            level.addFreshEntity(townswoman);

                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                    spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                                    6, 0.3, 0.3, 0.3, 0.02);
                            level.playSound(null, spawnPos, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.8f, 1.25f);
                            spawnedAny = true;
                        }
                    }
                }

                // 2. Replenish Adventurers if below target (only spawn one per tick cycle to avoid lag)
                if (!spawnedAny && existingAdventurers.size() < targetAdventurers) {
                    BlockPos anchor = !allVillagers.isEmpty() ? allVillagers.get(level.getRandom().nextInt(allVillagers.size())).blockPosition() : anchorPos;
                    BlockPos spawnPos = findSafeSpawnPos(level, anchor);

                    if (spawnPos != null && level.hasChunkAt(spawnPos)) {
                        AdventurerNpcEntity adventurer = ModEntities.ADVENTURER_NPC.get().create(level);
                        if (adventurer != null) {
                            adventurer.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                    level.getRandom().nextFloat() * 360f, 0.0f);
                            adventurer.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                            adventurer.setPersistenceRequired();
                            level.addFreshEntity(adventurer);

                            level.sendParticles(ParticleTypes.CRIT,
                                    spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                                    6, 0.3, 0.3, 0.3, 0.05);
                            level.playSound(null, spawnPos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 0.8f, 1.0f);
                            spawnedAny = true;
                        }
                    }
                }

                // Only apply 2-minute cooldown if population quotas are actually satisfied.
                // If a spawn failed because of terrain or placement, retry quickly (60 ticks / 3 seconds).
                if (existingTownswomen.size() + (spawnedAny ? 1 : 0) >= targetTownswomen
                        && existingAdventurers.size() >= targetAdventurers
                        && (!existingGuildMasters.isEmpty() || spawnedAny)) {
                    VILLAGE_COOLDOWNS.put(chunkPos, gameTime + 2400L); // 2 minutes cooldown
                } else {
                    VILLAGE_COOLDOWNS.put(chunkPos, gameTime + 60L); // Fast retry
                }
            }
        } catch (Throwable t) {
            RpgLevelsMod.LOGGER.error("Safe catch in VillageNpcSpawner: ", t);
        }
    }

    /**
     * Finds a safe solid surface position with sufficient vertical clearance
     * within a 12-block horizontal radius of the anchor position.
     * Supports grass, dirt, dirt paths, farmlands, planks, and stone.
     */
    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos origin) {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < 32; attempt++) {
            int dx = random.nextIntBetweenInclusive(-12, 12);
            int dz = random.nextIntBetweenInclusive(-12, 12);
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue; // Don't clip inside the villager

            BlockPos checkPos = origin.offset(dx, 0, dz);
            if (!level.hasChunkAt(checkPos)) continue;

            for (int dy = -3; dy <= 4; dy++) {
                BlockPos candidate = checkPos.above(dy);
                BlockPos ground = candidate.below();
                BlockState groundState = level.getBlockState(ground);
                BlockState feetState = level.getBlockState(candidate);
                BlockState headState = level.getBlockState(candidate.above());

                boolean isWalkableGround = groundState.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP)
                        || groundState.is(Blocks.DIRT_PATH)
                        || groundState.is(Blocks.FARMLAND)
                        || groundState.is(net.minecraft.tags.BlockTags.DIRT)
                        || groundState.blocksMotion();

                boolean isClearSpace = (feetState.isAir() || feetState.canBeReplaced() || feetState.getCollisionShape(level, candidate).isEmpty())
                        && (headState.isAir() || headState.canBeReplaced() || headState.getCollisionShape(level, candidate.above()).isEmpty())
                        && !feetState.liquid()
                        && !groundState.is(Blocks.LAVA)
                        && !groundState.is(Blocks.MAGMA_BLOCK);

                if (isWalkableGround && isClearSpace) {
                    return candidate;
                }
            }
        }

        return null;
    }
}
