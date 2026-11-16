package com.pablo.rpglevels.entities.ai;

import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ambient off-duty and domestic interaction AI for hired Townswoman workers.
 * Features:
 * 1. Player Aid: Tends to master returning injured or starving by offering food.
 * 2. Crafting Curiosity: Approaches curiously when master works at a crafting table.
 * 3. Domestic Tidiness: Detects open doors/gates around home and politely closes them.
 * 4. Base Stroll: Relaxed wandering near base center without straying far.
 */
public class TownswomanOffDutyGoal extends Goal {

    private final TownswomanNpcEntity worker;

    private int aidCooldown = 0;
    private int craftingCooldown = 0;
    private int doorCooldown = 0;
    private int strollCooldown = 0;

    private int playerNearCraftingTicks = 0;
    private BlockPos currentTargetDoor = null;
    private int doorPathingTicks = 0;

    public TownswomanOffDutyGoal(TownswomanNpcEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!worker.isHiredWorker() || worker.getWorkerStatus() != TownswomanNpcEntity.WorkerStatus.WORKING) {
            return false;
        }
        if (worker.getBaseCenterPos() == null) {
            return false;
        }
        if (worker.isSleeping()) {
            return false;
        }

        Player owner = worker.getHiringPlayer();

        // Emergency care check: Even during work, seeing the master bleeding or starving triggers aid
        if (owner != null && owner.level() == worker.level() && worker.distanceToSqr(owner) <= 16.0 * 16.0) {
            boolean ownerNeedsAid = owner.getHealth() <= owner.getMaxHealth() * 0.5F || owner.getFoodData().getFoodLevel() <= 6;
            if (ownerNeedsAid && aidCooldown <= 0) {
                return true;
            }
        }

        // Time schedule check
        long dayTime = worker.level().getDayTime() % 24000L;

        // If it is bedtime (13500 - 23500) and worker has an assigned bed, let TownswomanSleepGoal handle it
        if (dayTime >= 13500L && dayTime <= 23500L && worker.getBedPos() != null) {
            return false;
        }

        // Off-duty periods: Dusk/Evening (11500 - 13500) or Morning (23500 - 1000) or Night without a bed
        boolean isOffDutyHours = dayTime >= 11500L || dayTime < 1000L;
        return isOffDutyHours;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        worker.setHeldAxeVisible(false);
    }

    @Override
    public void stop() {
        currentTargetDoor = null;
        doorPathingTicks = 0;
    }

    @Override
    public void tick() {
        if (aidCooldown > 0) aidCooldown--;
        if (craftingCooldown > 0) craftingCooldown--;
        if (doorCooldown > 0) doorCooldown--;
        if (strollCooldown > 0) strollCooldown--;

        Player owner = worker.getHiringPlayer();

        // 1. Tending / Aid to Master (Highest Priority)
        if (owner != null && owner.level() == worker.level() && worker.distanceToSqr(owner) <= 16.0 * 16.0) {
            boolean ownerNeedsAid = owner.getHealth() <= owner.getMaxHealth() * 0.5F || owner.getFoodData().getFoodLevel() <= 6;
            if (ownerNeedsAid && aidCooldown <= 0) {
                handleAidMaster(owner);
                return;
            }
        }

        // 2. Domestic Tidiness: Close Open Doors
        if (handleDoorClosing()) {
            return;
        }

        // 3. Crafting Curiosity: Observe master at crafting table
        if (owner != null && owner.level() == worker.level() && worker.distanceToSqr(owner) <= 12.0 * 12.0) {
            if (handleCraftingCuriosity(owner)) {
                return;
            }
        } else {
            playerNearCraftingTicks = 0;
        }

        // 4. Base Stroll: Peaceful ambient wandering near base center
        handleBaseStroll();
    }

    private void handleAidMaster(Player owner) {
        double distSq = worker.distanceToSqr(owner);

        if (distSq > 3.2 * 3.2) {
            worker.getNavigation().moveTo(owner, 1.15);
            worker.getLookControl().setLookAt(owner, 20.0F, worker.getMaxHeadXRot());
        } else {
            worker.getNavigation().stop();
            worker.getLookControl().setLookAt(owner, 25.0F, worker.getMaxHeadXRot());

            // Prepare food to offer
            ItemStack foodItem = ItemStack.EMPTY;
            BlockPos foodChest = worker.findChestWithFood();
            if (foodChest != null) {
                foodItem = worker.takeFoodFromChest(foodChest);
            }
            if (foodItem.isEmpty()) {
                // Generous emergency ration from worker
                foodItem = worker.isDevoted() ? new ItemStack(Items.COOKED_BEEF, 2) : new ItemStack(Items.BREAD, 2);
            }

            // Toss food to player
            ItemEntity thrown = new ItemEntity(worker.level(), worker.getX(), worker.getEyeY() - 0.2, worker.getZ(), foodItem);
            Vec3 throwVec = owner.position().add(0, 0.4, 0).subtract(worker.position().add(0, worker.getEyeHeight() * 0.8, 0)).normalize().scale(0.35);
            thrown.setDeltaMovement(throwVec.x, 0.15, throwVec.z);
            thrown.setDefaultPickUpDelay();
            worker.level().addFreshEntity(thrown);
            worker.swing(InteractionHand.MAIN_HAND);

            // Audio & Particles
            worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0F, 1.0F);
            worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);

            if (worker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART, worker.getX(), worker.getY() + 1.6, worker.getZ(), 4, 0.3, 0.3, 0.3, 0.0);
            }

            // Chat message
            String name = worker.getName().getString();
            if (worker.isDevoted()) {
                owner.sendSystemMessage(Component.literal("§6[" + name + "] §e\"¡Mi señor! Se ve lastimado y sin fuerzas... Por favor, coma esto de inmediato. ¡No soportaría verle caer!\"").withStyle(ChatFormatting.GOLD));
            } else {
                owner.sendSystemMessage(Component.literal("§a[" + name + "] §f\"¡Jefe, te ves en muy mal estado! Come algo de esto antes de que te desplomes del cansancio.\"").withStyle(ChatFormatting.GREEN));
            }

            aidCooldown = 600; // 30 seconds cooldown
        }
    }

    private boolean handleDoorClosing() {
        if (doorCooldown > 0) return false;

        // Find open door if none targeted (check at most once every 60 ticks to avoid per-tick 1125-block scans)
        if (currentTargetDoor == null) {
            doorCooldown = 60;
            BlockPos pos = worker.blockPosition();
            for (BlockPos testPos : BlockPos.betweenClosed(pos.offset(-7, -2, -7), pos.offset(7, 2, 7))) {
                BlockState state = worker.level().getBlockState(testPos);
                if (state.getBlock() instanceof DoorBlock && !state.is(Blocks.IRON_DOOR)) {
                    if (state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER && state.getValue(DoorBlock.OPEN)) {
                        currentTargetDoor = testPos.immutable();
                        doorPathingTicks = 0;
                        break;
                    }
                } else if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN)) {
                    currentTargetDoor = testPos.immutable();
                    doorPathingTicks = 0;
                    break;
                }
            }
        }

        if (currentTargetDoor == null) {
            return false;
        }

        // Validate target is still open
        BlockState targetState = worker.level().getBlockState(currentTargetDoor);
        boolean isOpen = (targetState.getBlock() instanceof DoorBlock db && targetState.getValue(DoorBlock.OPEN))
                || (targetState.getBlock() instanceof FenceGateBlock && targetState.getValue(FenceGateBlock.OPEN));

        if (!isOpen) {
            currentTargetDoor = null;
            return false;
        }

        doorPathingTicks++;
        if (doorPathingTicks > 120) { // Timeout
            currentTargetDoor = null;
            doorCooldown = 100;
            return false;
        }

        double distSq = worker.distanceToSqr(currentTargetDoor.getX() + 0.5, currentTargetDoor.getY() + 0.5, currentTargetDoor.getZ() + 0.5);
        if (distSq > 2.2 * 2.2) {
            worker.getNavigation().moveTo(currentTargetDoor.getX() + 0.5, currentTargetDoor.getY(), currentTargetDoor.getZ() + 0.5, 0.95);
            worker.getLookControl().setLookAt(currentTargetDoor.getX() + 0.5, currentTargetDoor.getY() + 0.5, currentTargetDoor.getZ() + 0.5, 20.0F, worker.getMaxHeadXRot());
            return true;
        } else {
            // Close door
            worker.getNavigation().stop();
            worker.getLookControl().setLookAt(currentTargetDoor.getX() + 0.5, currentTargetDoor.getY() + 0.5, currentTargetDoor.getZ() + 0.5, 30.0F, worker.getMaxHeadXRot());
            worker.swing(InteractionHand.MAIN_HAND);

            if (targetState.getBlock() instanceof DoorBlock) {
                worker.level().setBlock(currentTargetDoor, targetState.setValue(DoorBlock.OPEN, false), 10);
                worker.level().playSound(null, currentTargetDoor, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.9F, 1.0F);
            } else if (targetState.getBlock() instanceof FenceGateBlock) {
                worker.level().setBlock(currentTargetDoor, targetState.setValue(FenceGateBlock.OPEN, false), 10);
                worker.level().playSound(null, currentTargetDoor, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 0.9F, 1.0F);
            }

            currentTargetDoor = null;
            doorCooldown = 160; // 8 seconds cooldown before closing another door
            return true;
        }
    }

    private boolean handleCraftingCuriosity(Player owner) {
        if (craftingCooldown > 0) return false;

        // Check if player is adjacent to a crafting table
        BlockPos playerPos = owner.blockPosition();
        boolean nearCraftingTable = false;
        for (BlockPos testPos : BlockPos.betweenClosed(playerPos.offset(-2, -1, -2), playerPos.offset(2, 1, 2))) {
            if (worker.level().getBlockState(testPos).is(Blocks.CRAFTING_TABLE)) {
                nearCraftingTable = true;
                break;
            }
        }

        if (!nearCraftingTable) {
            playerNearCraftingTicks = 0;
            return false;
        }

        playerNearCraftingTicks++;
        if (playerNearCraftingTicks < 140) { // Must craft for ~7 seconds before worker approaches
            return false;
        }

        double distSq = worker.distanceToSqr(owner);
        if (distSq > 2.8 * 2.8) {
            worker.getNavigation().moveTo(owner, 0.9);
            worker.getLookControl().setLookAt(owner, 20.0F, worker.getMaxHeadXRot());
            return true;
        } else {
            worker.getNavigation().stop();
            worker.getLookControl().setLookAt(owner, 25.0F, worker.getMaxHeadXRot());

            worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(),
                    SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.9F, 1.25F);

            if (worker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, worker.getX(), worker.getY() + 1.8, worker.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
            }

            String name = worker.getName().getString();
            if (worker.isDevoted()) {
                owner.sendSystemMessage(Component.literal("§6[" + name + "] §e\"¿Forjando nuevo equipo para nuestra fortaleza, mi señor? Si precisa que le acerque materiales, hágamelo saber.\"").withStyle(ChatFormatting.GOLD));
            } else {
                owner.sendSystemMessage(Component.literal("§a[" + name + "] §f\"¿Mucho trabajo en la mesa de crafteo, jefe? Se ve fascinante lo que estás creando.\"").withStyle(ChatFormatting.GREEN));
            }

            craftingCooldown = 1200; // 60s cooldown
            playerNearCraftingTicks = 0;
            return true;
        }
    }

    private void handleBaseStroll() {
        if (strollCooldown > 0) return;

        BlockPos center = worker.getBaseCenterPos();
        if (center == null) return;

        double distFromCenter = worker.distanceToSqr(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);

        // If drifted more than 10 blocks away from base center, return towards it
        if (distFromCenter > 10.0 * 10.0) {
            worker.getNavigation().moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0.85);
            strollCooldown = 80;
            return;
        }

        // Relaxed local stroll
        Vec3 target = DefaultRandomPos.getPosTowards(worker, 5, 2, Vec3.atCenterOf(center), Math.PI / 2);
        if (target != null) {
            worker.getNavigation().moveTo(target.x, target.y, target.z, 0.7);
        }
        strollCooldown = 80 + worker.getRandom().nextInt(80); // 4-8 seconds
    }
}

