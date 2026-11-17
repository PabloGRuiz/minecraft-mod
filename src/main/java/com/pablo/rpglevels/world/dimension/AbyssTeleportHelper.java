package com.pablo.rpglevels.world.dimension;

import com.pablo.rpglevels.init.ModBlocks;
import com.pablo.rpglevels.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Set;
import com.pablo.rpglevels.entities.CompanionStance;

public class AbyssTeleportHelper {

    public static Component getZoneComponent(double distance) {
        if (distance < 250) {
            return Component.translatable("rpglevels.abyss.zone_entrance");
        } else if (distance < 600) {
            return Component.translatable("rpglevels.abyss.zone_intermediate");
        } else if (distance < 1100) {
            return Component.translatable("rpglevels.abyss.zone_catacombs");
        } else if (distance < 1800) {
            return Component.translatable("rpglevels.abyss.zone_deep");
        } else {
            return Component.translatable("rpglevels.abyss.zone_heart");
        }
    }

    public static String getZoneName(double distance) {
        return getZoneComponent(distance).getString();
    }

    public static Component getThreatBadge(double distance) {
        if (distance < 250) {
            return Component.literal("§a").append(Component.translatable("rpglevels.abyss.threat")).append(": F-E");
        } else if (distance < 600) {
            return Component.literal("§e").append(Component.translatable("rpglevels.abyss.threat")).append(": D-C");
        } else if (distance < 1100) {
            return Component.literal("§6").append(Component.translatable("rpglevels.abyss.threat")).append(": B-A");
        } else if (distance < 1800) {
            return Component.literal("§c").append(Component.translatable("rpglevels.abyss.threat")).append(": S-SS");
        } else {
            return Component.literal("§5§l").append(Component.translatable("rpglevels.abyss.threat")).append(": SSS EXTREME");
        }
    }

    public static void enterAbyssRift(ServerPlayer player) {
        if (player.level().dimension().equals(ModDimensions.ABYSS_RIFT_LEVEL)) {
            player.sendSystemMessage(Component.translatable("rpglevels.abyss.already_in_rift"));
            return;
        }

        ServerLevel abyssLevel = player.getServer().getLevel(ModDimensions.ABYSS_RIFT_LEVEL);
        if (abyssLevel == null) {
            player.sendSystemMessage(Component.translatable("rpglevels.abyss.rift_unavailable"));
            return;
        }

        // Validación de Rechazo del Vacío: Requiere el efecto de Inmunidad de Alice
        if (!player.isCreative() && !player.isSpectator()) {
            boolean hasWard = player.hasEffect(com.pablo.rpglevels.init.ModMobEffects.ABYSSAL_WARD);
            if (!hasWard) {
                AbyssPortalBlock.executeVoidRejection(player);
                return;
            }
        }

        // Store player's Overworld entry position so they can return
        player.getPersistentData().putDouble("RpgOverworldEntryX", player.getX());
        player.getPersistentData().putDouble("RpgOverworldEntryY", player.getY());
        player.getPersistentData().putDouble("RpgOverworldEntryZ", player.getZ());

        // Collect hired companions within 64 blocks BEFORE teleporting player
        List<com.pablo.rpglevels.entities.AdventurerNpcEntity> companions = player.serverLevel().getEntitiesOfClass(
                com.pablo.rpglevels.entities.AdventurerNpcEntity.class,
                player.getBoundingBox().inflate(64.0),
                adv -> adv.isAlive() && adv.isHiredBy(player)
        );

        // Find or carve safe central sanctuary platform at (8, y, 8) inside a single chunk (0, 0)
        int targetY = findOrCreateSanctuary(abyssLevel);

        // Teleport player safely next to the monolith (offset 9.5, 8.5)
        player.teleportTo(abyssLevel, 9.5, targetY, 8.5, Set.of(), -90.0f, 0.0f);
        player.resetFallDistance();

        // Teleport companions onto sanctuary platform
        double compOffset = 0.5;
        for (var comp : companions) {
            comp.setStance(CompanionStance.FOLLOW);
            comp.teleportTo(abyssLevel, 7.5 + compOffset, targetY, 7.5 + compOffset, Set.of(), -90.0f, 0.0f);
            compOffset += 0.7;
        }

        com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.ENTER_ABYSS);

        // Ensure player has an Escape Scroll to return safely
        boolean hasScroll = false;
        for (ItemStack item : player.getInventory().items) {
            if (item.is(ModItems.ESCAPE_SCROLL.get())) {
                hasScroll = true;
                break;
            }
        }
        if (!hasScroll) {
            ItemStack scrollStack = new ItemStack(ModItems.ESCAPE_SCROLL.get(), 1);
            if (!player.getInventory().add(scrollStack)) {
                player.drop(scrollStack, false);
            }
            player.sendSystemMessage(Component.translatable("rpglevels.abyss.scroll_received"));
        }

        // Atmospheric darkness & sound transition
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 35, 0, false, false, false));
        abyssLevel.playSound(null, 8.5, targetY + 1.0, 8.5, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0f, 0.8f);
        abyssLevel.playSound(null, 8.5, targetY + 1.0, 8.5, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.8f);

        // Cinematic Title
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§5§l").append(Component.translatable("rpglevels.abyss.enter_title"))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7").append(Component.translatable("rpglevels.abyss.enter_sub"))));

        player.sendSystemMessage(Component.translatable("rpglevels.abyss.descended"));
    }

    public static void exitAbyssRift(ServerPlayer player) {
        ServerLevel overworld = player.getServer().overworld();
        if (overworld == null) return;

        double targetX;
        double targetY;
        double targetZ;

        if (player.getPersistentData().contains("RpgOverworldEntryX")) {
            targetX = player.getPersistentData().getDouble("RpgOverworldEntryX");
            targetY = player.getPersistentData().getDouble("RpgOverworldEntryY");
            targetZ = player.getPersistentData().getDouble("RpgOverworldEntryZ");
        } else {
            BlockPos spawn = overworld.getSharedSpawnPos();
            targetX = spawn.getX() + 0.5;
            targetY = spawn.getY() + 1.0;
            targetZ = spawn.getZ() + 0.5;
        }

        // Collect companions in Abyss within 64 blocks
        List<com.pablo.rpglevels.entities.AdventurerNpcEntity> companions = player.serverLevel().getEntitiesOfClass(
                com.pablo.rpglevels.entities.AdventurerNpcEntity.class,
                player.getBoundingBox().inflate(64.0),
                adv -> adv.isAlive() && adv.isHiredBy(player)
        );

        player.teleportTo(overworld, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot());
        player.resetFallDistance();

        double exitOffset = 0.5;
        for (var comp : companions) {
            comp.teleportTo(overworld, targetX + exitOffset, targetY, targetZ + exitOffset, Set.of(), comp.getYRot(), comp.getXRot());
            exitOffset += 0.7;
        }

        overworld.playSound(null, targetX, targetY, targetZ, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0f, 1.2f);
        overworld.playSound(null, targetX, targetY, targetZ, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§l").append(Component.translatable("rpglevels.abyss.overworld_title"))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7").append(Component.translatable("rpglevels.abyss.overworld_sub"))));
    }

    private static int cachedSanctuaryFloorY = -1;

    private static int findOrCreateSanctuary(ServerLevel level) {
        final int centerX = 8;
        final int centerZ = 8;

        // Fast-path: if already constructed in this session and the monolith is intact, return immediately without block writes
        if (cachedSanctuaryFloorY > 0) {
            BlockPos monolithCheck = new BlockPos(centerX, cachedSanctuaryFloorY, centerZ);
            if (level.getBlockState(monolithCheck).is(ModBlocks.RIFT_MONOLITH.get())) {
                return cachedSanctuaryFloorY;
            }
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(centerX, 50, centerZ);
        int floorY = -1;

        for (int y = 35; y <= 85; y++) {
            cursor.set(centerX, y, centerZ);
            if (level.getBlockState(cursor).is(ModBlocks.RIFT_MONOLITH.get())) {
                cachedSanctuaryFloorY = y;
                return y;
            }
            boolean solidBelow = !level.getBlockState(cursor.below()).isAir();
            boolean air1 = level.getBlockState(cursor).isAir();
            boolean air2 = level.getBlockState(cursor.above()).isAir();
            boolean air3 = level.getBlockState(cursor.above(2)).isAir();
            if (solidBelow && air1 && air2 && air3) {
                floorY = y;
                break;
            }
        }

        if (floorY < 0) {
            floorY = 50;
        }

        // Use Block.UPDATE_CLIENTS (2) inside a single chunk (X: 6..10, Z: 6..10 in chunk 0,0)
        // to avoid cross-chunk neighbor cascades and lighting deadlocks during dimension entry.
        final int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS;
        var deepslate = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        var air = Blocks.AIR.defaultBlockState();
        var torch = Blocks.SOUL_TORCH.defaultBlockState();

        // Guarantee a solid 7x7 platform at floorY - 1 and clear air space above it
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int bx = centerX + dx;
                int bz = centerZ + dz;
                // Solid floor
                level.setBlock(new BlockPos(bx, floorY - 1, bz), deepslate, flags);
                // Clear headspace
                for (int dy = 0; dy <= 4; dy++) {
                    level.setBlock(new BlockPos(bx, floorY + dy, bz), air, flags);
                }
            }
        }

        // Construir Portal de Retorno de Obsidiana Llorosa en (centerX, floorY, centerZ - 2)
        var cryingObsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        int pz = centerZ - 2;
        // Base
        level.setBlock(new BlockPos(centerX, floorY, pz), cryingObsidian, flags);
        level.setBlock(new BlockPos(centerX + 1, floorY, pz), cryingObsidian, flags);
        // Columnas laterales
        for (int dy = 0; dy <= 4; dy++) {
            level.setBlock(new BlockPos(centerX - 1, floorY + dy, pz), cryingObsidian, flags);
            level.setBlock(new BlockPos(centerX + 2, floorY + dy, pz), cryingObsidian, flags);
        }
        // Techo
        level.setBlock(new BlockPos(centerX, floorY + 4, pz), cryingObsidian, flags);
        level.setBlock(new BlockPos(centerX + 1, floorY + 4, pz), cryingObsidian, flags);

        // Rellenar interior del portal (2 ancho x 3 alto)
        var portalState = ModBlocks.ABYSS_PORTAL.get().defaultBlockState().setValue(AbyssPortalBlock.AXIS, net.minecraft.core.Direction.Axis.X);
        for (int dy = 1; dy <= 3; dy++) {
            level.setBlock(new BlockPos(centerX, floorY + dy, pz), portalState, flags);
            level.setBlock(new BlockPos(centerX + 1, floorY + dy, pz), portalState, flags);
        }

        // Place Central Return Monolith at (8, floorY, 9)
        BlockPos monolithPos = new BlockPos(centerX, floorY, centerZ + 1);
        level.setBlock(monolithPos, ModBlocks.RIFT_MONOLITH.get().defaultBlockState(), flags);

        // Soul Torches on corners to illuminate the sanctuary
        level.setBlock(new BlockPos(centerX + 3, floorY, centerZ + 3), torch, flags);
        level.setBlock(new BlockPos(centerX - 3, floorY, centerZ - 3), torch, flags);
        level.setBlock(new BlockPos(centerX - 3, floorY, centerZ + 3), torch, flags);
        level.setBlock(new BlockPos(centerX + 3, floorY, centerZ - 3), torch, flags);

        cachedSanctuaryFloorY = floorY;
        return floorY;
    }
}
