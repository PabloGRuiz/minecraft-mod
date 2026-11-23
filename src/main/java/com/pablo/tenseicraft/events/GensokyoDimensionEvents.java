package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.BanditEntity;
import com.pablo.tenseicraft.entities.EnergyAttackEntity;
import com.pablo.tenseicraft.entities.ModEntities;
import com.pablo.tenseicraft.entities.ReimuBossEntity;
import com.pablo.tenseicraft.entities.ReimuNpcEntity;
import com.pablo.tenseicraft.entities.TownswomanNpcEntity;
import com.pablo.tenseicraft.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Eventos y lógica de teletransporte para la dimensión opcional El Gensokyo (bioma único de cerezos)
 * y el santuario de combate de Reimu Hakurei.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class GensokyoDimensionEvents {

    private static final int SURFACE_Y = 64;

    /**
     * Mantiene El Gensokyo libre de monstruos/bandidos naturales fuera del duelo con Reimu.
     */
    @SubscribeEvent
    public static void onEntityJoinGensokyo(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!ModDimensions.isGensokyo(event.getLevel())) return;

        Entity entity = event.getEntity();
        if (entity instanceof ReimuBossEntity boss) {
            if (event.getLevel() instanceof ServerLevel sl) {
                for (ReimuNpcEntity npc : new ArrayList<>(sl.getEntities(ModEntities.REIMU_NPC.get(), e -> true))) {
                    npc.discard();
                }
                for (ReimuBossEntity other : new ArrayList<>(sl.getEntities(ModEntities.REIMU_BOSS.get(), e -> e != boss))) {
                    other.discard();
                }
            }
            return;
        }
        if (entity instanceof ReimuNpcEntity npc) {
            if (event.getLevel() instanceof ServerLevel sl) {
                for (ReimuBossEntity boss : new ArrayList<>(sl.getEntities(ModEntities.REIMU_BOSS.get(), e -> !e.isDeadOrDying()))) {
                    boss.discard();
                }
                for (EnergyAttackEntity orb : new ArrayList<>(sl.getEntities(ModEntities.ENERGY_ATTACK.get(), e -> true))) {
                    orb.discard();
                }
                for (ReimuNpcEntity other : new ArrayList<>(sl.getEntities(ModEntities.REIMU_NPC.get(), e -> e != npc))) {
                    other.discard();
                }
            }
            return;
        }
        if (entity instanceof Enemy || entity instanceof Monster
                || entity instanceof BanditEntity || entity instanceof TownswomanNpcEntity) {
            event.setCanceled(true);
        }
    }

    /**
     * Si muere un jugador dentro del Gensokyo y no quedan otros jugadores vivos,
     * despawnea al jefe Reimu y limpia sus proyectiles activos.
     */
    @SubscribeEvent
    public static void onPlayerDeathInGensokyo(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModDimensions.isGensokyo(player.level())) return;

        cleanupBossIfGensokyoEmpty(player.serverLevel(), player);
    }

    /**
     * Si un jugador abandona El Gensokyo y la dimensión queda vacía, despawnea al jefe Reimu.
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getFrom().equals(ModDimensions.GENSOKYO_LEVEL) && player.getServer() != null) {
            ServerLevel gensokyo = player.getServer().getLevel(ModDimensions.GENSOKYO_LEVEL);
            if (gensokyo != null) {
                cleanupBossIfGensokyoEmpty(gensokyo, player);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (ModDimensions.isGensokyo(player.level()) && player.getServer() != null) {
            ServerLevel gensokyo = player.getServer().getLevel(ModDimensions.GENSOKYO_LEVEL);
            if (gensokyo != null) {
                cleanupBossIfGensokyoEmpty(gensokyo, player);
            }
        }
    }

    private static void cleanupBossIfGensokyoEmpty(ServerLevel gensokyo, @Nullable ServerPlayer excludingPlayer) {
        boolean anyAliveRemaining = gensokyo.players().stream()
                .anyMatch(p -> p != excludingPlayer && p.isAlive() && !p.isSpectator());
        if (!anyAliveRemaining) {
            clearAllBossesAndOrbs(gensokyo);
        }
    }

    private static void clearAllBossesAndOrbs(ServerLevel gensokyo) {
        List<ReimuBossEntity> bosses = new ArrayList<>();
        gensokyo.getEntities(ModEntities.REIMU_BOSS.get(), e -> true).forEach(bosses::add);
        for (ReimuBossEntity boss : bosses) {
            boss.discard();
        }

        List<EnergyAttackEntity> orbs = new ArrayList<>();
        gensokyo.getEntities(ModEntities.ENERGY_ATTACK.get(), e -> true).forEach(orbs::add);
        for (EnergyAttackEntity orb : orbs) {
            orb.discard();
        }
    }

    private static void clearAllNpcs(ServerLevel gensokyo) {
        List<ReimuNpcEntity> npcs = new ArrayList<>();
        gensokyo.getEntities(ModEntities.REIMU_NPC.get(), e -> true).forEach(npcs::add);
        for (ReimuNpcEntity npc : npcs) {
            npc.discard();
        }
    }

    /**
     * Maneja el viaje bidireccional entre Overworld (bajo Bioma de Cerezos) y El Gensokyo
     * al lanzar la Técnica Espiritual Nro. 20: Conectar Realidades.
     */
    public static boolean tryHandleConnectRealitiesTeleport(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;

        if (ModDimensions.isGensokyo(player.level())) {
            exitGensokyo(player);
            return true;
        }

        if (player.level().dimension().equals(Level.OVERWORLD)) {
            boolean inCherryGrove = player.level().getBiome(player.blockPosition()).is(Biomes.CHERRY_GROVE);
            if (inCherryGrove) {
                enterGensokyo(player);
                return true;
            }
        }
        return false;
    }

    private static void enterGensokyo(ServerPlayer player) {
        ServerLevel gensokyo = player.getServer().getLevel(ModDimensions.GENSOKYO_LEVEL);
        if (gensokyo == null) {
            player.sendSystemMessage(Component.literal("§cLa dimensión El Gensokyo no está disponible en este mundo."));
            return;
        }

        // Guardar coordenadas de retorno en el Overworld
        player.getPersistentData().putDouble("GensokyoReturnX", player.getX());
        player.getPersistentData().putDouble("GensokyoReturnY", player.getY());
        player.getPersistentData().putDouble("GensokyoReturnZ", player.getZ());

        // Teletransportar primero al jugador para asegurar chunks cargados en la arena
        player.teleportTo(gensokyo, 0.5, SURFACE_Y, -3.5, Set.of(), 0.0F, 0.0F);
        player.resetFallDistance();

        // Limpiar el claro de cerezos para el duelo
        prepareBattleClearing(gensokyo);

        List<ReimuNpcEntity> existingNpcs = new ArrayList<>();
        gensokyo.getEntities(ModEntities.REIMU_NPC.get(), e -> true).forEach(existingNpcs::add);
        boolean defeatedReimu = player.getPersistentData().getBoolean("DefeatedReimuBoss");

        if (defeatedReimu) {
            // Modo NPC: eliminar cualquier jefe/orbe y garantizar exactamente 1 Reimu NPC
            clearAllBossesAndOrbs(gensokyo);
            if (existingNpcs.isEmpty()) {
                ReimuNpcEntity npc = ModEntities.REIMU_NPC.get().create(gensokyo);
                if (npc != null) {
                    npc.moveTo(0.5, SURFACE_Y, 20.5, 180.0F, 0.0F);
                    npc.finalizeSpawn(gensokyo, gensokyo.getCurrentDifficultyAt(new BlockPos(0, SURFACE_Y, 20)), MobSpawnType.EVENT, null);
                    gensokyo.addFreshEntity(npc);
                }
            } else if (existingNpcs.size() > 1) {
                for (int i = 1; i < existingNpcs.size(); i++) {
                    existingNpcs.get(i).discard();
                }
            }
        } else {
            // Modo Jefe: nunca permitir NPC común junto al jefe y evitar duplicados
            clearAllNpcs(gensokyo);
            List<ReimuBossEntity> existingBosses = new ArrayList<>();
            gensokyo.getEntities(ModEntities.REIMU_BOSS.get(), e -> true).forEach(existingBosses::add);
            boolean otherAlivePlayerPresent = gensokyo.players().stream()
                    .anyMatch(p -> p != player && p.isAlive() && !p.isSpectator());

            if (!otherAlivePlayerPresent || existingBosses.isEmpty()) {
                clearAllBossesAndOrbs(gensokyo);
                ReimuBossEntity boss = ModEntities.REIMU_BOSS.get().create(gensokyo);
                if (boss != null) {
                    boss.moveTo(0.5, SURFACE_Y, 20.5, 180.0F, 0.0F);
                    boss.finalizeSpawn(gensokyo, gensokyo.getCurrentDifficultyAt(new BlockPos(0, SURFACE_Y, 20)), MobSpawnType.EVENT, null);
                    boss.setAnchorPos(0.5, SURFACE_Y, 20.5);
                    boss.startIntroSequence(180);
                    gensokyo.addFreshEntity(boss);
                }
            } else if (existingBosses.size() > 1) {
                for (int i = 1; i < existingBosses.size(); i++) {
                    existingBosses.get(i).discard();
                }
            }
        }

        gensokyo.playSound(null, 0.5, SURFACE_Y, -3.5, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.15F);
        gensokyo.sendParticles(ParticleTypes.CHERRY_LEAVES, 0.5, SURFACE_Y + 1.5, -3.5, 60, 1.5, 1.0, 1.5, 0.1);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§lEL GENSOKYO")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§dSantuario Hakurei entre los Cerezos Eternos")));

        player.sendSystemMessage(Component.literal("§d✦ §l[CONECTAR REALIDADES] §fHas cruzado la Gran Barrera Hakurei hacia §cEl Gensokyo§f."));
        player.sendSystemMessage(Component.literal("§7Lanza §dConectar Realidades §7en cualquier momento aquí dentro para regresar a tu mundo."));
    }

    private static void exitGensokyo(ServerPlayer player) {
        ServerLevel overworld = player.getServer().overworld();
        ServerLevel gensokyo = player.getServer().getLevel(ModDimensions.GENSOKYO_LEVEL);
        if (overworld == null) return;

        double rx;
        double ry;
        double rz;
        if (player.getPersistentData().contains("GensokyoReturnX")) {
            rx = player.getPersistentData().getDouble("GensokyoReturnX");
            ry = player.getPersistentData().getDouble("GensokyoReturnY");
            rz = player.getPersistentData().getDouble("GensokyoReturnZ");
        } else {
            BlockPos spawn = overworld.getSharedSpawnPos();
            rx = spawn.getX() + 0.5;
            ry = spawn.getY() + 1.0;
            rz = spawn.getZ() + 0.5;
        }

        player.teleportTo(overworld, rx, ry, rz, Set.of(), player.getYRot(), player.getXRot());
        player.resetFallDistance();

        if (gensokyo != null) {
            cleanupBossIfGensokyoEmpty(gensokyo, null);
        }

        overworld.playSound(null, rx, ry, rz, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 1.25F);
        overworld.sendParticles(ParticleTypes.CHERRY_LEAVES, rx, ry + 1.0, rz, 40, 1.0, 0.8, 1.0, 0.08);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§lRETORNO AL OVERWORLD")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Has atravesado de vuelta la Gran Barrera")));
    }

    private static void prepareBattleClearing(ServerLevel level) {
        final int flags = Block.UPDATE_CLIENTS;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();

        for (int x = -20; x <= 20; x++) {
            for (int z = -18; z <= 34; z++) {
                BlockPos floorPos = new BlockPos(x, SURFACE_Y - 1, z);
                if (level.getBlockState(floorPos).isAir() || level.getBlockState(floorPos).is(BlockTags.LOGS)) {
                    level.setBlock(floorPos, grass, flags);
                }
                for (int y = SURFACE_Y; y <= SURFACE_Y + 24; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        level.setBlock(pos, air, flags);
                    }
                }
            }
        }
    }
}
