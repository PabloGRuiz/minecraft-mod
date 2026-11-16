package com.pablo.rpglevels.entities.ai;

import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;

import java.util.EnumSet;

public class TownswomanSleepGoal extends Goal {

    private final TownswomanNpcEntity npc;
    private int sleepParticleTimer = 0;

    public TownswomanSleepGoal(TownswomanNpcEntity npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!npc.isHiredWorker() || npc.getWorkerStatus() != TownswomanNpcEntity.WorkerStatus.WORKING) {
            return false;
        }
        if (npc.getBedPos() == null) {
            return false;
        }
        long dayTime = npc.level().getDayTime() % 24000L;
        return dayTime >= 13500L && dayTime <= 23500L;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        BlockPos bedPos = npc.getBedPos();
        if (bedPos != null) {
            npc.getNavigation().moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.9);
        }
    }

    @Override
    public void tick() {
        BlockPos bedPos = npc.getBedPos();
        if (bedPos == null) return;

        double distSq = npc.distanceToSqr(bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5);

        if (distSq > 2.0 * 2.0) {
            if (npc.isSleeping()) {
                npc.stopSleeping();
            }
            if (npc.tickCount % 15 == 0 || npc.getNavigation().isDone()) {
                npc.getNavigation().moveTo(bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5, 0.9);
            }
        } else {
            npc.getNavigation().stop();

            // Lie down on the bed horizontally
            if (!npc.isSleeping() && npc.level().getBlockState(bedPos).getBlock() instanceof BedBlock) {
                npc.startSleeping(bedPos);
            }

            if (npc.isSleeping()) {
                sleepParticleTimer++;
                if (sleepParticleTimer % 40 == 0 && npc.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            bedPos.getX() + 0.5, bedPos.getY() + 0.8, bedPos.getZ() + 0.5,
                            2, 0.2, 0.1, 0.2, 0.0);
                }
            }
        }
    }

    @Override
    public void stop() {
        if (npc.isSleeping()) {
            npc.stopSleeping();
            npc.setPose(Pose.STANDING);
        }
        npc.getNavigation().stop();
    }
}
