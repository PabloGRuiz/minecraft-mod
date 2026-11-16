package com.pablo.rpglevels.world.trial;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.ModNetworking;
import com.pablo.rpglevels.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class WarGodTrialManager {

    private static final AtomicInteger NEXT_ARENA_INDEX = new AtomicInteger(0);
    private static final int ARENA_SPACING = 64;

    private static class TrialSession {
        final UUID playerUuid;
        final int centerX;
        final int centerZ;
        int tickCounter = 0;
        int state = 0; // 0 = intro, 1 = dialogue 2, 2 = spawn mobs, 3 = fighting, 4 = victory wait, 5 = finished
        final Set<UUID> spawnedZombies = new HashSet<>();
        int returnDelay = 0;

        TrialSession(UUID playerUuid, int centerX, int centerZ) {
            this.playerUuid = playerUuid;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }
    }

    private static final Map<UUID, TrialSession> ACTIVE_SESSIONS = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_TRIALS = new HashMap<>();

    public static boolean isInTrial(UUID playerUuid) {
        return ACTIVE_SESSIONS.containsKey(playerUuid) || PENDING_TRIALS.containsKey(playerUuid);
    }

    /**
     * 1. Detect when player kills a monster for the first time
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // Check if a trial zombie died
        UUID deadUuid = event.getEntity().getUUID();
        for (TrialSession session : ACTIVE_SESSIONS.values()) {
            if (session.spawnedZombies.remove(deadUuid)) {
                // One trial zombie died
                if (session.spawnedZombies.isEmpty() && session.state == 3) {
                    session.state = 4; // Victory achieved!
                }
            }
        }

        // Check if player killed their first monster
        if (event.getEntity() instanceof Monster && event.getSource().getEntity() instanceof ServerPlayer player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null && !data.hasKilledFirstMonster()) {
                data.setHasKilledFirstMonster(true);
                ModNetworking.syncToPlayer(player);
                player.sendSystemMessage(Component.literal("§8§o[").append(Component.translatable("rpglevels.trial.blood_spilled")).append("]"));
            }
        }
    }

    /**
     * 2. Detect when player wakes up from bed to trigger trial if condition is met.
     * Desactivado para la versión Alpha: el sistema de Técnicas Espirituales reemplaza este juicio.
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        // Obsoleto en la versión Alpha: las técnicas se aprenden con Alice y se desbloquean con la Espada Espiritual
        return;
    }

    /**
     * Start the War God Trial (Desactivado en Alpha)
     */
    public static void startTrial(ServerPlayer player) {
        player.sendSystemMessage(Component.literal("§7[RPG Levels] El Juicio del Dios de la Guerra está desactivado para la versión Alpha (obsoleto con el sistema de Técnicas Espirituales de Alice)."));
    }

    private static void buildBarrierRoom(ServerLevel level, int centerX, int centerZ) {
        // Floor at 64, walls from 65 to 68, ceiling at 69
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                // Invisible Barrier Floor
                level.setBlockAndUpdate(new BlockPos(x, 64, z), Blocks.BARRIER.defaultBlockState());
                // Invisible Barrier Ceiling
                level.setBlockAndUpdate(new BlockPos(x, 69, z), Blocks.BARRIER.defaultBlockState());

                // Air inside
                for (int y = 65; y <= 68; y++) {
                    if (dx == -4 || dx == 4 || dz == -4 || dz == 4) {
                        // Invisible Barrier Walls
                        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.BARRIER.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    /**
     * 3. Server Tick Handler for Trial Sequences
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Handle pending scheduled trials (delayed after waking up)
        if (!PENDING_TRIALS.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> pIt = PENDING_TRIALS.entrySet().iterator();
            while (pIt.hasNext()) {
                Map.Entry<UUID, Integer> pEntry = pIt.next();
                int delay = pEntry.getValue() - 1;
                if (delay <= 0) {
                    pIt.remove();
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(pEntry.getKey());
                    if (player != null && player.isAlive() && !ACTIVE_SESSIONS.containsKey(pEntry.getKey())) {
                        startTrial(player);
                    }
                } else {
                    pEntry.setValue(delay);
                }
            }
        }

        if (ACTIVE_SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, TrialSession>> it = ACTIVE_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrialSession> entry = it.next();
            TrialSession session = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.playerUuid);

            if (player == null || !player.isAlive()) {
                cleanupSessionZombies(event.getServer().getLevel(ModDimensions.VOID_TRIAL_LEVEL), session);
                it.remove();
                continue;
            }

            session.tickCounter++;

            // Step 0 -> Step 1 (Tick 20: 1 second)
            if (session.state == 0 && session.tickCounter >= 20) {
                session.state = 1;
                player.sendSystemMessage(Component.literal("§6§l[???] §f").append(Component.translatable("rpglevels.trial.dialogue_1")));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.8f);
            }

            // Step 1 -> Step 2 (Tick 60: 3 seconds)
            else if (session.state == 1 && session.tickCounter >= 60) {
                session.state = 2;
                player.sendSystemMessage(Component.literal("§6§l[???] §f").append(Component.translatable("rpglevels.trial.dialogue_2")));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            // Step 2 -> Step 3 (Tick 100: 5 seconds - Spawn 4 zombies & give sword)
            else if (session.state == 2 && session.tickCounter >= 100) {
                session.state = 3;
                player.sendSystemMessage(Component.literal("§6§l[???] §f").append(Component.translatable("rpglevels.trial.dialogue_3")));

                // Deliver "Primera Espada"
                ItemStack firstSword = new ItemStack(Items.STONE_SWORD);
                firstSword.set(DataComponents.CUSTOM_NAME, Component.literal("§6§l").append(Component.translatable("rpglevels.item.first_sword.name")));
                List<Component> lore = List.of(
                        Component.literal("§7[").append(Component.translatable("rpglevels.item.first_sword.lore_1")).append("]"),
                        Component.literal("§e").append(Component.translatable("rpglevels.item.first_sword.lore_2")),
                        Component.literal("§f").append(Component.translatable("rpglevels.item.first_sword.lore_3"))
                );
                firstSword.set(DataComponents.LORE, new ItemLore(lore));

                player.serverLevel().registryAccess().lookup(Registries.ENCHANTMENT).ifPresent(reg -> {
                    reg.get(Enchantments.SHARPNESS).ifPresent(h -> firstSword.enchant(h, 1));
                    reg.get(Enchantments.UNBREAKING).ifPresent(h -> firstSword.enchant(h, 1));
                });

                if (!player.getInventory().add(firstSword)) {
                    player.drop(firstSword, false);
                }

                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§l").append(Component.translatable("rpglevels.trial.challenge_title"))));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7").append(Component.translatable("rpglevels.trial.challenge_subtitle"))));

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 0.9f);

                // Spawn 4 zombies in the 4 corners of the 9x9 invisible room
                spawnCornerZombies(player.serverLevel(), session, player);
            }

            // Step 4: Victory! All zombies dead -> Ares reveals himself
            else if (session.state == 4) {
                session.state = 5;
                session.returnDelay = 80; // 4 seconds before returning to Overworld

                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    data.setWarTrialCompleted(true);
                    data.addPhysicalSkillPoints(1);
                    ModNetworking.syncToPlayer(player);
                }

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l").append(Component.translatable("rpglevels.trial.victory_title"))));
                player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§a").append(Component.translatable("rpglevels.trial.victory_subtitle"))));

                Component warGodPrefix = Component.literal("§6§l[").append(Component.translatable("rpglevels.trial.god_of_war_name")).append("] ");
                player.sendSystemMessage(warGodPrefix.copy().append(Component.literal("§e")).append(Component.translatable("rpglevels.trial.ares_1")));
                player.sendSystemMessage(warGodPrefix.copy().append(Component.literal("§f")).append(Component.translatable("rpglevels.trial.ares_2")));
                player.sendSystemMessage(warGodPrefix.copy().append(Component.literal("§a")).append(Component.translatable("rpglevels.trial.ares_3")));
                player.sendSystemMessage(warGodPrefix.copy().append(Component.literal("§7")).append(Component.translatable("rpglevels.trial.ares_4")));
            }

            // Step 5: Countdown to teleport back to Overworld
            else if (session.state == 5) {
                session.returnDelay--;
                if (session.returnDelay <= 0) {
                    returnPlayerToOverworld(player);
                    it.remove();
                }
            }
        }
    }

    private static void cleanupSessionZombies(ServerLevel voidLevel, TrialSession session) {
        if (voidLevel == null) return;
        for (UUID zId : session.spawnedZombies) {
            Entity entity = voidLevel.getEntity(zId);
            if (entity != null) {
                entity.discard();
            }
        }
        session.spawnedZombies.clear();
    }

    private static void spawnCornerZombies(ServerLevel level, TrialSession session, ServerPlayer player) {
        double[][] corners = {
                {session.centerX - 2.5, 65.0, session.centerZ - 2.5},
                {session.centerX + 2.5, 65.0, session.centerZ - 2.5},
                {session.centerX - 2.5, 65.0, session.centerZ + 2.5},
                {session.centerX + 2.5, 65.0, session.centerZ + 2.5}
        };

        for (double[] c : corners) {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null) {
                zombie.moveTo(c[0], c[1], c[2], 0.0f, 0.0f);
                zombie.setCustomName(Component.literal("§c§l").append(Component.translatable("rpglevels.entity.trial_zombie")));
                zombie.setCustomNameVisible(true);
                zombie.setPersistenceRequired();
                zombie.setTarget(player);
                level.addFreshEntity(zombie);

                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c[0], c[1] + 1.0, c[2], 15, 0.3, 0.5, 0.3, 0.05);
                session.spawnedZombies.add(zombie.getUUID());
            }
        }
    }

    private static void returnPlayerToOverworld(ServerPlayer player) {
        ServerLevel targetLevel = player.getServer().overworld();
        if (player.getPersistentData().contains("WarTrialReturnDim")) {
            String dimStr = player.getPersistentData().getString("WarTrialReturnDim");
            ResourceLocation dimLoc = ResourceLocation.tryParse(dimStr);
            if (dimLoc != null) {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
                ServerLevel resolved = player.getServer().getLevel(dimKey);
                if (resolved != null) {
                    targetLevel = resolved;
                }
            }
        }
        if (targetLevel == null) return;

        double targetX = player.getPersistentData().contains("WarTrialReturnX")
                ? player.getPersistentData().getDouble("WarTrialReturnX")
                : targetLevel.getSharedSpawnPos().getX() + 0.5;
        double targetY = player.getPersistentData().contains("WarTrialReturnY")
                ? player.getPersistentData().getDouble("WarTrialReturnY")
                : targetLevel.getSharedSpawnPos().getY() + 1.0;
        double targetZ = player.getPersistentData().contains("WarTrialReturnZ")
                ? player.getPersistentData().getDouble("WarTrialReturnZ")
                : targetLevel.getSharedSpawnPos().getZ() + 0.5;

        player.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot());
        player.resetFallDistance();

        targetLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0f, 1.2f);
    }
}
