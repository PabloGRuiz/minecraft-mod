package com.pablo.rpglevels.entities.ai;

import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class TownswomanEscortGoal extends Goal {

    private final TownswomanNpcEntity npc;
    private Player owner;
    private int warningCooldown = 0;

    public TownswomanEscortGoal(TownswomanNpcEntity npc) {
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!npc.isHiredWorker() || npc.getWorkerStatus() != TownswomanNpcEntity.WorkerStatus.ESCORTING) {
            return false;
        }
        this.owner = npc.getHiringPlayer();
        return this.owner != null && this.owner.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.npc.getNavigation().moveTo(owner, 1.25);
    }

    @Override
    public void stop() {
        this.npc.getNavigation().stop();
        this.owner = null;
    }

    @Override
    public void tick() {
        if (owner == null) return;

        // Verify same dimension
        if (owner.level() != npc.level()) {
            npc.dismissWorkerContract(owner, "cambio de dimensión");
            return;
        }

        double distSq = npc.distanceToSqr(owner);

        // Cancel if owner runs too far ahead (> 45 blocks)
        if (distSq > 45.0 * 45.0) {
            npc.dismissWorkerContract(owner, "distancia");
            return;
        }

        // Warning to player if falling behind (between 22 and 45 blocks)
        if (distSq > 22.0 * 22.0) {
            warningCooldown--;
            if (warningCooldown <= 0) {
                warningCooldown = 100; // Every 5 seconds
                owner.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§e[" + npc.getName().getString() + "] §c¡Espera, te estás adelantando mucho! Me voy a perder..."),
                        true
                );
                if (npc.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, npc.getX(), npc.getY() + 1.8, npc.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                }
            }
        }

        // Follow navigation
        if (distSq > 4.0 * 4.0) {
            npc.getNavigation().moveTo(owner, 1.25);
        } else {
            npc.getNavigation().stop();
            npc.getLookControl().setLookAt(owner, 10.0F, npc.getMaxHeadXRot());
        }
    }
}

