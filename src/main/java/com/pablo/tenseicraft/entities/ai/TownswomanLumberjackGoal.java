package com.pablo.tenseicraft.entities.ai;

import com.pablo.tenseicraft.entities.TownswomanNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class TownswomanLumberjackGoal extends Goal {

    private final TownswomanNpcEntity worker;
    private BlockPos targetLogPos = null;
    private BlockPos targetStandPos = null;
    private int chopTimer = 0;
    private int scanCooldown = 0;
    private boolean isDepositing = false;

    // Stuck detection
    private BlockPos lastPos = BlockPos.ZERO;
    private int stuckTicks = 0;
    private final Map<BlockPos, Long> unreachableLogs = new HashMap<>();

    // Break & Eating state
    private boolean isTakingBreak = false;
    private int breakTimer = 0;
    private ItemStack currentFood = ItemStack.EMPTY;
    private int eatingTicks = 0;
    private int workTimer = 0;
    private int wanderCooldown = 0;

    // Notification cooldowns to avoid message spam
    private int askAxeCooldown = 0;
    private int askFoodCooldown = 0;

    public TownswomanLumberjackGoal(TownswomanNpcEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!worker.isHiredWorker() || worker.getWorkerStatus() != TownswomanNpcEntity.WorkerStatus.WORKING) {
            return false;
        }
        if (worker.getWorkRole() != TownswomanNpcEntity.WorkRole.LUMBERJACK) {
            return false;
        }
        if (worker.getBaseCenterPos() == null) {
            return false;
        }
        // Work shift is between 1000L and 11500L (Minecraft daytime). Outside this window, off-duty AI or sleep takes over.
        long dayTime = worker.level().getDayTime() % 24000L;
        return dayTime >= 1000L && dayTime <= 11500L;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (askAxeCooldown > 0) askAxeCooldown--;
        if (askFoodCooldown > 0) askFoodCooldown--;

        // --- 1. Scheduled Lunch / Resting Break ---
        if (isTakingBreak) {
            handleBreakTick();
            return;
        }

        // Periodic break every 3000 ticks (~2.5 minutes of continuous work)
        workTimer++;
        if (workTimer >= 3000) {
            workTimer = 0;
            startBreak();
            return;
        }

        // --- 2. Check if we need to deposit collected lumber/items ---
        if (isDepositing || worker.isWorkerStorageFull()) {
            handleDeposit();
            return;
        }

        // --- 3. Check Tool: Does worker have an axe in slot 0 (or slot 1/2)? ---
        if (!worker.hasAxeInToolSlot()) {
            worker.setHeldAxeVisible(false);
            handleMissingAxe();
            return;
        }

        // --- 4. Locate reachable wood if no target ---
        if (targetLogPos == null || !worker.level().getBlockState(targetLogPos).is(BlockTags.LOGS)) {
            worker.setHeldAxeVisible(false);
            scanCooldown--;
            if (scanCooldown <= 0) {
                scanCooldown = 20; // Scan every 1 second
                findNearestReachableLog();
            }

            if (targetLogPos == null) {
                // If storage has items and no trees left to chop, deposit them!
                if (worker.hasLumberInStorage() && worker.getDepositChestPos() != null) {
                    isDepositing = true;
                    return;
                }

                // Idle wander peacefully within work area
                wanderCooldown--;
                if (wanderCooldown <= 0 && worker.getBaseCenterPos() != null) {
                    wanderCooldown = 80;
                    Vec3 wander = DefaultRandomPos.getPos(worker, worker.getWorkRadius(), 3);
                    if (wander != null) {
                        worker.getNavigation().moveTo(wander.x, wander.y, wander.z, 0.7);
                    }
                }
                return;
            }
        }

        // --- 5. Move to and chop target tree log ---
        double dx = worker.getX() - (targetLogPos.getX() + 0.5);
        double dz = worker.getZ() - (targetLogPos.getZ() + 0.5);
        double horizDistSq = dx * dx + dz * dz;
        double dy = (targetLogPos.getY() + 0.5) - worker.getY();

        // In reach if horizontally close (<= 4.5 blocks) and vertical reach up to 24 blocks high!
        boolean inReach = horizDistSq <= 4.5 * 4.5 && dy >= -3.0 && dy <= 24.0;

        // Equip axe in hand while working
        worker.setHeldAxeVisible(true);

        if (!inReach) {
            // Pathfind to adjacent stand position (throttled every 15 ticks to avoid A* pathfinding spam)
            if (worker.tickCount % 15 == 0 || worker.getNavigation().isDone()) {
                BlockPos dest = targetStandPos != null ? targetStandPos : targetLogPos;
                float speedMod = 1.0F + (getWorkerAffinity() / 100.0F) * 0.25F;
                worker.getNavigation().moveTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, speedMod);
            }
            chopTimer = 0;

            // Anti-stuck detection: if running against a wall or fence for > 70 ticks, skip this tree
            checkStuck();
        } else {
            // In range: chop the log!
            worker.getNavigation().stop();
            worker.getLookControl().setLookAt(targetLogPos.getX() + 0.5, targetLogPos.getY() + 0.5, targetLogPos.getZ() + 0.5, 30.0F, worker.getMaxHeadXRot());
            stuckTicks = 0;

            chopTimer++;
            if (chopTimer % 5 == 0) {
                worker.swing(InteractionHand.MAIN_HAND);
                worker.level().playSound(null, targetLogPos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8F, 1.0F);

                if (worker.level() instanceof ServerLevel serverLevel) {
                    BlockState logState = worker.level().getBlockState(targetLogPos);
                    serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, logState),
                            targetLogPos.getX() + 0.5, targetLogPos.getY() + 0.5, targetLogPos.getZ() + 0.5,
                            4, 0.2, 0.2, 0.2, 0.05);
                }
            }

            int requiredTicks = getRequiredChopTicks();

            if (chopTimer >= requiredTicks) {
                chopTimer = 0;
                harvestLogBlock(targetLogPos);
            }
        }
    }

    private int getWorkerAffinity() {
        Player owner = worker.getHiringPlayer();
        return owner != null ? worker.getAffinity(owner) : 0;
    }

    private int getRequiredChopTicks() {
        int affinity = getWorkerAffinity();
        float factor = 1.0F + (affinity / 100.0F) * 1.5F;
        if (worker.isDevoted()) {
            factor += 0.5F * worker.getVassalRank(); // Rank S adds +2.0F factor!
        }
        return Math.max(5, Math.round(25.0F / factor));
    }

    private void handleMissingAxe() {
        // Step 1: Check if an axe is inside any assigned chest
        BlockPos chestWithAxe = worker.findChestWithAxe();
        if (chestWithAxe != null) {
            double distSq = worker.distanceToSqr(chestWithAxe.getX() + 0.5, chestWithAxe.getY() + 0.5, chestWithAxe.getZ() + 0.5);
            if (distSq > 3.2 * 3.2) {
                worker.getNavigation().moveTo(chestWithAxe.getX() + 0.5, chestWithAxe.getY(), chestWithAxe.getZ() + 0.5, 1.0);
            } else {
                worker.getNavigation().stop();
                boolean gotAxe = worker.takeAxeFromChest(chestWithAxe);
                if (gotAxe) {
                    worker.level().playSound(null, chestWithAxe, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
                    if (worker.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, chestWithAxe.getX() + 0.5, chestWithAxe.getY() + 1.0, chestWithAxe.getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }
            return;
        }

        // Step 2: No axe in chests! Approach player if nearby and ask for one
        Player owner = worker.getHiringPlayer();
        if (owner != null && owner.level() == worker.level() && worker.distanceToSqr(owner) <= 24.0 * 24.0) {
            double distSq = worker.distanceToSqr(owner);
            if (distSq > 3.0 * 3.0) {
                worker.getNavigation().moveTo(owner, 1.1);
            } else {
                worker.getNavigation().stop();
                worker.getLookControl().setLookAt(owner, 15.0F, worker.getMaxHeadXRot());

                if (askAxeCooldown <= 0) {
                    askAxeCooldown = 160; // Every 8 seconds
                    owner.sendSystemMessage(Component.literal("§e[" + worker.getName().getString() + "] §c¡No tengo ningún hacha en los cofres ni en mi inventario! Por favor, pon una en el cofre o dámela para poder talar.").withStyle(ChatFormatting.GOLD));
                    worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
                    if (worker.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, worker.getX(), worker.getY() + 1.8, worker.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }
        } else {
            // Player is far away: stay still waiting peacefully at base
            worker.getNavigation().stop();
        }
    }

    private void checkStuck() {
        BlockPos current = worker.blockPosition();
        if (current.distSqr(lastPos) < 1.0) {
            stuckTicks++;
            if (stuckTicks > 70) { // 3.5 seconds stuck against wall/structure
                if (targetLogPos != null) {
                    unreachableLogs.put(targetLogPos, worker.level().getGameTime() + 300L); // 15s blacklist
                }
                targetLogPos = null;
                targetStandPos = null;
                stuckTicks = 0;
                worker.getNavigation().stop();
                worker.setHeldAxeVisible(false);
                worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.8F, 1.0F);
            }
        } else {
            stuckTicks = 0;
            lastPos = current;
        }
    }

    private void harvestLogBlock(BlockPos pos) {
        BlockState logState = worker.level().getBlockState(pos);
        if (!logState.is(BlockTags.LOGS)) {
            targetLogPos = null;
            targetStandPos = null;
            return;
        }

        Item logItem = logState.getBlock().asItem();
        if (logItem != Items.AIR) {
            ItemStack drop = new ItemStack(logItem, 1);

            // High affinity or Devoted vassal bonus yield!
            int affinity = getWorkerAffinity();
            float bonusChance = 0.0F;
            if (worker.isDevoted()) {
                bonusChance = 0.30F + (worker.getVassalRank() * 0.15F); // Up to 90% at Rank S!
            } else if (affinity >= 50) {
                bonusChance = affinity / 180.0F;
            }

            if (bonusChance > 0.0F && worker.level().getRandom().nextFloat() < bonusChance) {
                drop.grow(1);
                if (worker.isDevoted() && worker.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
                }
            }

            ItemStack remainder = worker.addToWorkerStorage(drop);
            if (!remainder.isEmpty()) {
                isDepositing = true;
            }
        }

        worker.damageAxe(1);
        worker.level().destroyBlock(pos, false);

        // Check if there is another log directly above or diagonal branch
        BlockPos nextLog = findConnectedLogAbove(pos);
        if (nextLog != null) {
            targetLogPos = nextLog; // Ascend the trunk!
        } else {
            // Reached the top of the tree!
            // Clean up and decay leaves around the tree canopy so no floating leaves remain!
            decayLeavesAround(pos);
            replantSaplingAtBase(pos);
            targetLogPos = null;
            targetStandPos = null;
            worker.setHeldAxeVisible(false);
        }
    }

    private BlockPos findConnectedLogAbove(BlockPos current) {
        // Direct upward trunk check (up to 3 blocks higher in case of small branch gaps)
        for (int yOff = 1; yOff <= 3; yOff++) {
            BlockPos up = current.above(yOff);
            if (worker.level().getBlockState(up).is(BlockTags.LOGS)) {
                return up;
            }
        }
        // Diagonal and horizontal branch check for large branching trees and canopies
        for (int yOff = 0; yOff <= 2; yOff++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dz == 0 && yOff == 0) continue;
                    BlockPos candidate = current.offset(dx, yOff, dz);
                    if (worker.level().getBlockState(candidate).is(BlockTags.LOGS)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private void decayLeavesAround(BlockPos center) {
        Level level = worker.level();
        // Clear leaves in a generous 5-block horizontal and -3 to +6 vertical radius around the canopy so no floating leaves remain!
        BlockPos.betweenClosedStream(center.offset(-5, -3, -5), center.offset(5, 6, 5)).forEach(p -> {
            BlockState state = level.getBlockState(p);
            if (state.is(BlockTags.LEAVES)) {
                level.destroyBlock(p, true); // true drops saplings, apples, sticks!
            }
        });
    }

    private void replantSaplingAtBase(BlockPos trunkBase) {
        BlockPos current = trunkBase;
        while (current.getY() > worker.level().getMinBuildHeight() && worker.level().getBlockState(current).isAir()) {
            current = current.below();
        }

        BlockState groundState = worker.level().getBlockState(current);
        BlockPos plantPos = current.above();

        if ((groundState.is(BlockTags.DIRT) || groundState.is(Blocks.GRASS_BLOCK)) && worker.level().getBlockState(plantPos).isAir()) {
            ItemStack sapling = worker.findAndConsumeSapling();
            Block saplingBlock = !sapling.isEmpty() ? Block.byItem(sapling.getItem()) : Blocks.OAK_SAPLING;
            if (saplingBlock == Blocks.AIR) saplingBlock = Blocks.OAK_SAPLING;

            worker.level().setBlock(plantPos, saplingBlock.defaultBlockState(), 3);
            worker.level().playSound(null, plantPos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

            if (worker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, plantPos.getX() + 0.5, plantPos.getY() + 0.5, plantPos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    private void handleDeposit() {
        BlockPos chestPos = worker.getDepositChestPos();
        if (chestPos == null) {
            isDepositing = false;
            return;
        }

        worker.setHeldAxeVisible(false);
        double distSq = worker.distanceToSqr(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5);

        if (distSq > 3.2 * 3.2) {
            BlockPos standPos = findReachableStandPos(chestPos);
            BlockPos dest = standPos != null ? standPos : chestPos;
            worker.getNavigation().moveTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, 1.0);
        } else {
            worker.getNavigation().stop();
            worker.getLookControl().setLookAt(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5, 15.0F, worker.getMaxHeadXRot());

            BlockEntity be = worker.level().getBlockEntity(chestPos);
            if (be instanceof Container container) {
                boolean depositedAny = false;

                // Move items from storage slots 1 and 2 into chest
                for (int s = 1; s <= 2; s++) {
                    ItemStack st = worker.getWorkerInventory().getItem(s);
                    if (st.isEmpty()) continue;

                    for (int cSlot = 0; cSlot < container.getContainerSize(); cSlot++) {
                        if (container.canPlaceItem(cSlot, st)) {
                            ItemStack inChest = container.getItem(cSlot);
                            if (inChest.isEmpty()) {
                                container.setItem(cSlot, st.copy());
                                worker.getWorkerInventory().setItem(s, ItemStack.EMPTY);
                                depositedAny = true;
                                break;
                            } else if (ItemStack.isSameItemSameComponents(inChest, st) && inChest.getCount() < inChest.getMaxStackSize()) {
                                int canAdd = Math.min(st.getCount(), inChest.getMaxStackSize() - inChest.getCount());
                                inChest.grow(canAdd);
                                st.shrink(canAdd);
                                depositedAny = true;
                                if (st.isEmpty()) {
                                    worker.getWorkerInventory().setItem(s, ItemStack.EMPTY);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (depositedAny) {
                    container.setChanged();
                    worker.level().playSound(null, chestPos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
                    if (worker.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, chestPos.getX() + 0.5, chestPos.getY() + 1.0, chestPos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.0);
                    }
                }
            }

            isDepositing = false;
            // Take a rest break after a successful delivery
            startBreak();
        }
    }

    private void startBreak() {
        this.isTakingBreak = true;
        this.breakTimer = worker.isDevoted() ? 50 : 100; // Devoted vassals recover stamina quicker!
        this.currentFood = ItemStack.EMPTY;
        this.eatingTicks = 0;
        this.worker.setHeldAxeVisible(false);
    }

    private void handleBreakTick() {
        breakTimer--;

        // If not holding food, retrieve real food from food chest
        if (currentFood.isEmpty()) {
            BlockPos chestWithFood = worker.findChestWithFood();
            if (chestWithFood != null) {
                double distSq = worker.distanceToSqr(chestWithFood.getX() + 0.5, chestWithFood.getY() + 0.5, chestWithFood.getZ() + 0.5);
                if (distSq > 3.2 * 3.2) {
                    worker.getNavigation().moveTo(chestWithFood.getX() + 0.5, chestWithFood.getY(), chestWithFood.getZ() + 0.5, 1.0);
                } else {
                    worker.getNavigation().stop();
                    ItemStack food = worker.takeFoodFromChest(chestWithFood);
                    if (!food.isEmpty()) {
                        currentFood = food;
                        worker.setHeldFood(food);
                        eatingTicks = 0;
                        worker.level().playSound(null, chestWithFood, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8F, 1.0F);
                    }
                }
            } else {
                // No food in chests: Ask nearby owner for food!
                Player owner = worker.getHiringPlayer();
                if (owner != null && owner.level() == worker.level() && worker.distanceToSqr(owner) <= 24.0 * 24.0) {
                    double distSq = worker.distanceToSqr(owner);
                    if (distSq > 3.0 * 3.0) {
                        worker.getNavigation().moveTo(owner, 1.1);
                    } else {
                        worker.getNavigation().stop();
                        worker.getLookControl().setLookAt(owner, 15.0F, worker.getMaxHeadXRot());

                        if (askFoodCooldown <= 0) {
                            askFoodCooldown = 160; // Every 8 seconds
                            owner.sendSystemMessage(Component.literal("§e[" + worker.getName().getString() + "] §c¡Tengo hambre y el cofre de comida está vacío! Por favor, abastece el cofre con algo de comer.").withStyle(ChatFormatting.GOLD));
                            worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
                            if (worker.level() instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, worker.getX(), worker.getY() + 1.8, worker.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                            }
                        }
                    }
                } else {
                    worker.getNavigation().stop();
                }
            }
        } else {
            // Holding real food from chest: play realistic eating sequence
            eatingTicks++;
            worker.getNavigation().stop();

            if (eatingTicks % 6 == 0) {
                worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.7F, 1.0F);

                if (worker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, currentFood),
                            worker.getX(), worker.getY() + 1.25, worker.getZ(),
                            3, 0.1, 0.1, 0.1, 0.05);
                }
            }

            if (eatingTicks == 45) {
                worker.level().playSound(null, worker.getX(), worker.getY(), worker.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 0.8F, 1.1F);
                if (worker.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            worker.getX(), worker.getY() + 1.2, worker.getZ(),
                            3, 0.2, 0.2, 0.2, 0.0);
                }
                worker.setHeldFood(ItemStack.EMPTY);
                currentFood = ItemStack.EMPTY;
            }
        }

        // When break timer finishes, always resume work safely (no lockup!)
        if (breakTimer <= 0) {
            isTakingBreak = false;
            worker.setHeldFood(ItemStack.EMPTY);
            currentFood = ItemStack.EMPTY;
        }
    }

    private void findNearestReachableLog() {
        BlockPos center = worker.getBaseCenterPos();
        int radius = Math.max(6, worker.getWorkRadius());
        BlockPos nearest = null;
        BlockPos nearestStand = null;
        double nearestDistSq = Double.MAX_VALUE;
        long gameTime = worker.level().getGameTime();

        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int minY = Math.max(worker.level().getMinBuildHeight(), center.getY() - 5);
        int maxY = Math.min(worker.level().getMaxBuildHeight(), center.getY() + 14);

        // Clean expired blacklists
        unreachableLogs.entrySet().removeIf(entry -> entry.getValue() < gameTime);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        // Thorough scan: step 1 block at a time so no 1x1 tree is skipped!
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    BlockState bs = worker.level().getBlockState(cursor);
                    if (bs.is(BlockTags.LOGS)) {
                        // Find bottom-most log of this tree
                        BlockPos bottom = cursor.immutable();
                        while (worker.level().getBlockState(bottom.below()).is(BlockTags.LOGS)) {
                            bottom = bottom.below();
                        }

                        // Skip temporarily blacklisted trees
                        if (unreachableLogs.containsKey(bottom)) {
                            break;
                        }

                        // Find reachable stand position adjacent to the tree base
                        BlockPos stand = findReachableStandPos(bottom);
                        double d = worker.distanceToSqr(stand.getX() + 0.5, stand.getY() + 0.5, stand.getZ() + 0.5);
                        if (d < nearestDistSq) {
                            nearestDistSq = d;
                            nearest = bottom;
                            nearestStand = stand;
                        }
                        break;
                    }
                }
            }
        }

        this.targetLogPos = nearest;
        this.targetStandPos = nearestStand;
    }

    private BlockPos findReachableStandPos(BlockPos target) {
        // Project target down to solid ground level so the worker never tries to stand in thin air!
        BlockPos groundLevel = target;
        while (groundLevel.getY() > worker.level().getMinBuildHeight() && !worker.level().getBlockState(groundLevel.below()).blocksMotion()) {
            groundLevel = groundLevel.below();
        }

        Direction[] dirs = Direction.values();
        for (Direction dir : dirs) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockPos candidate = groundLevel.relative(dir);
            BlockState feet = worker.level().getBlockState(candidate);
            BlockState head = worker.level().getBlockState(candidate.above());
            BlockState ground = worker.level().getBlockState(candidate.below());

            // Passable includes air, replaceable blocks, flowers, and leaves!
            boolean isFeetPassable = feet.isAir() || feet.canBeReplaced() || feet.is(BlockTags.LEAVES) || feet.getCollisionShape(worker.level(), candidate).isEmpty();
            boolean isHeadPassable = head.isAir() || head.canBeReplaced() || head.is(BlockTags.LEAVES) || head.getCollisionShape(worker.level(), candidate.above()).isEmpty();
            boolean isGroundSolid = ground.blocksMotion() || ground.is(BlockTags.DIRT) || ground.is(Blocks.DIRT_PATH) || ground.isFaceSturdy(worker.level(), candidate.below(), Direction.UP);

            if (isFeetPassable && isHeadPassable && isGroundSolid) {
                return candidate;
            }
        }

        // Also check directly under the log if it's floating
        BlockState underFeet = worker.level().getBlockState(groundLevel);
        BlockState underGround = worker.level().getBlockState(groundLevel.below());
        if ((underFeet.isAir() || underFeet.canBeReplaced() || underFeet.is(BlockTags.LEAVES)) && underGround.blocksMotion()) {
            return groundLevel;
        }

        // Fallback: ground level adjacent
        return groundLevel.relative(Direction.NORTH);
    }

    @Override
    public void stop() {
        worker.setHeldAxeVisible(false);
        worker.setHeldFood(ItemStack.EMPTY);
        worker.getNavigation().stop();
        targetLogPos = null;
        targetStandPos = null;
        currentFood = ItemStack.EMPTY;
        isTakingBreak = false;
    }
}
