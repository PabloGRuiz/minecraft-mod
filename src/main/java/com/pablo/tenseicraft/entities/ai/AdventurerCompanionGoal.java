package com.pablo.tenseicraft.entities.ai;

import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class AdventurerCompanionGoal extends Goal {

    private final AdventurerNpcEntity npc;
    private Player owner;
    private final Level level;

    // Movement speeds
    private final double followSpeed = 1.25;
    private final double inspectSpeed = 0.85;

    // Distances
    private final float stopDistance = 3.5f;
    private final float startFollowDistance = 6.0f;
    private final float maxFollowDistance = 30.0f;
    private final float stationaryRadius = 10.0f;

    // State tracking
    private Vec3 lastOwnerPos = Vec3.ZERO;
    private int stationaryTicks = 0;
    private boolean isIdleInspecting = false;

    // Inspection targets (Points of interest)
    private BlockPos targetPoiPos = null;
    private Entity targetAnimal = null;
    private int inspectTimer = 0;
    private int poiCooldown = 0;
    private int timeToRecalcPath = 0;

    private static final Set<Block> INTERESTING_BLOCKS = Set.of(
            Blocks.CRAFTING_TABLE,
            Blocks.FURNACE,
            Blocks.BLAST_FURNACE,
            Blocks.SMOKER,
            Blocks.CHEST,
            Blocks.BARREL,
            Blocks.ANVIL,
            Blocks.CHIPPED_ANVIL,
            Blocks.DAMAGED_ANVIL,
            Blocks.ENCHANTING_TABLE,
            Blocks.BOOKSHELF,
            Blocks.CHISELED_BOOKSHELF,
            Blocks.BREWING_STAND,
            Blocks.LOOM,
            Blocks.CARTOGRAPHY_TABLE,
            Blocks.SMITHING_TABLE,
            Blocks.GRINDSTONE
    );

    public AdventurerCompanionGoal(AdventurerNpcEntity npc) {
        this.npc = npc;
        this.level = npc.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!npc.isHired() || npc.isStaying() || npc.getStance() != com.pablo.tenseicraft.entities.CompanionStance.FOLLOW) {
            return false;
        }

        // When engaging in combat, prioritize combat dueling goal
        if (npc.getTarget() != null && npc.getTarget().isAlive()) {
            return false;
        }

        Player player = npc.getHiringPlayer();
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return false;
        }

        this.owner = player;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.stationaryTicks = 0;
        this.isIdleInspecting = false;
        this.targetPoiPos = null;
        this.targetAnimal = null;
        if (owner != null) {
            this.lastOwnerPos = owner.position();
        }
    }

    @Override
    public void stop() {
        this.npc.getNavigation().stop();
        this.owner = null;
        this.isIdleInspecting = false;
        this.targetPoiPos = null;
        this.targetAnimal = null;
    }

    @Override
    public void tick() {
        if (owner == null) return;

        double distToOwnerSq = npc.distanceToSqr(owner);
        double distToOwner = Math.sqrt(distToOwnerSq);

        // Always face owner if very close or looking around
        if (distToOwner < 4.0 && !isIdleInspecting) {
            npc.getLookControl().setLookAt(owner, 10.0F, npc.getMaxHeadXRot());
        }

        // 1. Long distance teleport fallback (over mountains/ravines)
        if (distToOwner > maxFollowDistance && !npc.isLeashed()) {
            teleportNearOwner();
            return;
        }

        // 2. Track owner stationary status (radius of 10 blocks)
        double ownerDisplacement = owner.position().distanceTo(lastOwnerPos);
        if (ownerDisplacement < 1.2) {
            stationaryTicks++;
        } else {
            stationaryTicks = 0;
            lastOwnerPos = owner.position();
            if (isIdleInspecting) {
                // Owner started moving away: immediately snap out of inspection!
                stopInspection();
            }
        }

        // 3. Mode Selection: Follow vs Intelligent Inspection
        boolean ownerIsStationary = stationaryTicks > 60 && distToOwner <= stationaryRadius;

        if (ownerIsStationary && npc.getTarget() == null) {
            // Intelligent inspection routine
            handleIdleInspection(distToOwner);
        } else {
            // Active following routine
            if (isIdleInspecting) {
                stopInspection();
            }
            handleActiveFollow(distToOwner);
        }
    }

    private void handleActiveFollow(double distToOwner) {
        if (distToOwner > startFollowDistance) {
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                // Sprint/accelerate if getting left behind
                double speed = distToOwner > 14.0 ? followSpeed * 1.2 : followSpeed;
                npc.getNavigation().moveTo(owner, speed);
            }
        } else if (distToOwner <= stopDistance) {
            npc.getNavigation().stop();
            this.timeToRecalcPath = 0;
        }
    }

    private void handleIdleInspection(double distToOwner) {
        isIdleInspecting = true;

        // If wandered too far from owner (> 9 blocks), head back towards owner
        if (distToOwner > 9.0) {
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                npc.getNavigation().moveTo(owner, inspectSpeed);
            }
            targetPoiPos = null;
            targetAnimal = null;
            return;
        }

        if (poiCooldown > 0) {
            poiCooldown--;
        }

        // Check active inspection target
        if (targetPoiPos != null) {
            npc.getLookControl().setLookAt(
                    targetPoiPos.getX() + 0.5,
                    targetPoiPos.getY() + 0.5,
                    targetPoiPos.getZ() + 0.5,
                    20.0F, npc.getMaxHeadXRot()
            );

            double distToPoi = npc.position().distanceTo(Vec3.atCenterOf(targetPoiPos));
            if (distToPoi < 2.2) {
                npc.getNavigation().stop();
                inspectTimer--;
                if (inspectTimer <= 0) {
                    targetPoiPos = null;
                    poiCooldown = 80 + npc.getRandom().nextInt(60);
                }
            }
            return;
        }

        if (targetAnimal != null && targetAnimal.isAlive()) {
            npc.getLookControl().setLookAt(targetAnimal, 20.0F, npc.getMaxHeadXRot());
            double distToAnimal = npc.distanceTo(targetAnimal);
            if (distToAnimal < 2.5) {
                npc.getNavigation().stop();
                inspectTimer--;
                if (inspectTimer <= 0) {
                    targetAnimal = null;
                    poiCooldown = 80 + npc.getRandom().nextInt(60);
                }
            }
            return;
        }

        // Find a new Point of Interest if cooldown ready
        if (poiCooldown <= 0) {
            poiCooldown = 100 + npc.getRandom().nextInt(80);
            if (npc.getRandom().nextFloat() < 0.6F) {
                // Seek workstation / container
                BlockPos foundPoi = scanForPoi(6);
                if (foundPoi != null) {
                    targetPoiPos = foundPoi;
                    inspectTimer = 60 + npc.getRandom().nextInt(60); // 3 to 6 seconds
                    npc.getNavigation().moveTo(
                            foundPoi.getX() + 0.5,
                            foundPoi.getY(),
                            foundPoi.getZ() + 0.5,
                            inspectSpeed
                    );
                    return;
                }
            } else {
                // Seek nearby friendly animal
                Animal foundAnimal = scanForAnimal(7);
                if (foundAnimal != null) {
                    targetAnimal = foundAnimal;
                    inspectTimer = 50 + npc.getRandom().nextInt(50);
                    npc.getNavigation().moveTo(foundAnimal, inspectSpeed);
                    return;
                }
            }

            // Casual wander around owner if no POI
            Vec3 wanderPos = getGentleWanderPosAroundOwner();
            if (wanderPos != null) {
                npc.getNavigation().moveTo(wanderPos.x, wanderPos.y, wanderPos.z, inspectSpeed * 0.9);
            }
        }
    }

    private void stopInspection() {
        isIdleInspecting = false;
        targetPoiPos = null;
        targetAnimal = null;
        inspectTimer = 0;
    }

    private BlockPos scanForPoi(int radius) {
        BlockPos center = npc.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(cursor);
                    if (INTERESTING_BLOCKS.contains(state.getBlock())) {
                        // Check that it is accessible and close to owner
                        if (owner.distanceToSqr(cursor.getX() + 0.5, cursor.getY() + 0.5, cursor.getZ() + 0.5) <= 100.0) {
                            candidates.add(cursor.immutable());
                        }
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(npc.getRandom().nextInt(candidates.size()));
        }
        return null;
    }

    private Animal scanForAnimal(int radius) {
        List<Animal> animals = level.getEntitiesOfClass(
                Animal.class,
                npc.getBoundingBox().inflate(radius, 2, radius),
                a -> a.isAlive() && a.distanceTo(owner) <= 10.0
        );
        if (!animals.isEmpty()) {
            return animals.get(npc.getRandom().nextInt(animals.size()));
        }
        return null;
    }

    private Vec3 getGentleWanderPosAroundOwner() {
        double angle = npc.getRandom().nextDouble() * Math.PI * 2;
        double radius = 3.0 + npc.getRandom().nextDouble() * 4.0; // 3 to 7 blocks
        double targetX = owner.getX() + Math.cos(angle) * radius;
        double targetZ = owner.getZ() + Math.sin(angle) * radius;
        BlockPos targetPos = BlockPos.containing(targetX, owner.getY(), targetZ);

        // Find walkable ground
        for (int yOffset = 2; yOffset >= -3; yOffset--) {
            BlockPos check = targetPos.offset(0, yOffset, 0);
            if (level.getBlockState(check).isAir() && level.getBlockState(check.below()).isSolid()) {
                return new Vec3(check.getX() + 0.5, check.getY(), check.getZ() + 0.5);
            }
        }
        return null;
    }

    private void teleportNearOwner() {
        BlockPos ownerPos = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = npc.getRandom().nextInt(5) - 2;
            int dz = npc.getRandom().nextInt(5) - 2;
            int dy = npc.getRandom().nextInt(3) - 1;

            BlockPos target = ownerPos.offset(dx, dy, dz);
            if (level.getBlockState(target).isAir() && level.getBlockState(target.below()).isSolid()) {
                npc.moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, npc.getYRot(), npc.getXRot());
                npc.getNavigation().stop();
                stopInspection();
                return;
            }
        }
    }
}
