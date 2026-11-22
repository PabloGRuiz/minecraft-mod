package com.pablo.tenseicraft.entities.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Intelligent Action-RPG Ranged AI for Bow-wielding NPCs (Bandits & Adventurers).
 *
 * Features:
 * 1. Tactical Range Awareness: Maintains optimal firing distance (8-14 blocks).
 * 2. Evasive Backdash: When an enemy rushes into melee (<= 5.5 blocks), the archer
 *    dashea hacia atrás con impulso físico, partículas de aire y sonido de corte/viento.
 * 3. Lateral Leap / Strafe: Si tiene una pared detrás o está acorralado, salta hacia un lateral
 *    para esquivar y reposicionarse.
 * 4. Kiting & Circling: Se mueve en arcos laterales mientras tensa el arco, evitando ser un blanco fácil.
 */
public class RpgTacticalBowGoal<T extends PathfinderMob & RangedAttackMob> extends Goal {

    protected final T mob;
    protected final double speedModifier;
    protected int attackIntervalMin;
    protected final float maxAttackDistance;
    protected int seeTime = 0;
    protected int strafeTime = 0;
    protected boolean strafeClockwise = false;
    protected int backdashCooldown = 0;
    protected int attackCooldown = -1;
    protected int pathRecalcCooldown = 0;

    public RpgTacticalBowGoal(T mob, double speedModifier, int attackIntervalMin, float maxAttackDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.attackIntervalMin = attackIntervalMin;
        this.maxAttackDistance = maxAttackDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    protected boolean isHoldingBow() {
        return this.mob.isHolding(item -> item.getItem() instanceof BowItem);
    }

    @Override
    public boolean canUse() {
        if (this.mob instanceof RpgCombatDuelist duelist && duelist.isCombatSuspended()) return false;
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.isHoldingBow();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob instanceof RpgCombatDuelist duelist && duelist.isCombatSuspended()) return false;
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.isHoldingBow();
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAggressive(true);
        this.backdashCooldown = 10;
        this.strafeTime = 0;
        this.strafeClockwise = this.mob.getRandom().nextBoolean();
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAggressive(false);
        this.seeTime = 0;
        this.attackCooldown = -1;
        this.mob.stopUsingItem();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        double distSq = this.mob.distanceToSqr(target);
        double dist = Math.sqrt(distSq);
        boolean canSee = this.mob.getSensing().hasLineOfSight(target);

        if (canSee) {
            this.seeTime++;
        } else {
            this.seeTime = 0;
        }

        if (this.backdashCooldown > 0) {
            this.backdashCooldown--;
        }

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // --- 1. TACTICAL RETREAT / BACKDASH ---
        // If enemy enters close combat range (<= 5.5 blocks), dash backwards to create distance!
        if (dist <= 5.5 && this.backdashCooldown <= 0 && this.mob.onGround()) {
            executeBackdash(target);
        }

        // --- 2. KITING & LATERAL STRAFING MOVEMENT ---
        if (this.pathRecalcCooldown > 0) {
            this.pathRecalcCooldown--;
        }
        if (dist > this.maxAttackDistance) {
            // Far away: close in (throttled A* pathfinding)
            if (this.pathRecalcCooldown <= 0 || this.mob.getNavigation().isDone()) {
                this.pathRecalcCooldown = 10 + this.mob.getRandom().nextInt(5);
                this.mob.getNavigation().moveTo(target, this.speedModifier);
            }
        } else if (dist < 6.5) {
            // Uncomfortably close: back away while walking
            if (this.pathRecalcCooldown <= 0 || this.mob.getNavigation().isDone()) {
                this.pathRecalcCooldown = 8;
                Vec3 retreatPos = this.mob.position().subtract(target.position()).normalize().scale(3.0).add(this.mob.position());
                this.mob.getNavigation().moveTo(retreatPos.x, retreatPos.y, retreatPos.z, this.speedModifier * 1.15);
            }
        } else {
            // Optimal shooting distance (6.5 to max distance): Strafe laterally around target!
            this.mob.getNavigation().stop();
            if (--this.strafeTime <= 0) {
                this.strafeTime = 20 + this.mob.getRandom().nextInt(25);
                this.strafeClockwise = !this.strafeClockwise;
            }
            float strafeDir = this.strafeClockwise ? 0.75F : -0.75F;
            float forwardDir = dist > 11.0 ? 0.3F : (dist < 8.0 ? -0.3F : 0.0F);
            this.mob.getMoveControl().strafe(forwardDir, strafeDir);
        }

        // --- 3. BOW CHARGING AND FIRING ---
        if (this.mob.isUsingItem()) {
            if (!canSee && this.seeTime < -60) {
                this.mob.stopUsingItem();
            } else if (canSee) {
                int useDuration = this.mob.getTicksUsingItem();
                // When bow is fully charged (20 ticks / 1 second)
                if (useDuration >= 20) {
                    this.mob.stopUsingItem();
                    this.mob.performRangedAttack(target, 1.0F);
                    this.attackCooldown = this.attackIntervalMin + this.mob.getRandom().nextInt(10);
                }
            }
        } else if (--this.attackCooldown <= 0 && this.seeTime >= -60) {
            // Start drawing the bow
            this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, item -> item instanceof BowItem));
        }
    }

    /**
     * Executes an explosive evasive backdash leap away from the enemy.
     * If trapped against a wall, dashes laterally.
     */
    private void executeBackdash(LivingEntity target) {
        Vec3 awayFromEnemy = this.mob.position().subtract(target.position());
        awayFromEnemy = new Vec3(awayFromEnemy.x, 0, awayFromEnemy.z).normalize();

        // Check if there is free space behind the mob
        BlockPos behindPos = this.mob.blockPosition().offset(
                (int) Math.round(awayFromEnemy.x * 2.0),
                0,
                (int) Math.round(awayFromEnemy.z * 2.0)
        );

        boolean wallBehind = this.mob.level().getBlockState(behindPos).isSolid();

        Vec3 dashDir;
        if (!wallBehind) {
            // Direct retreat leap backwards
            dashDir = awayFromEnemy;
        } else {
            // Trapped against a wall: Tactical lateral escape leap!
            float sideSign = this.mob.getRandom().nextBoolean() ? 1.0F : -1.0F;
            dashDir = new Vec3(-awayFromEnemy.z * sideSign, 0, awayFromEnemy.x * sideSign).normalize();
        }

        // Apply explosive velocity leap
        this.mob.setDeltaMovement(dashDir.x * 0.95, 0.34, dashDir.z * 0.95);
        this.mob.hasImpulse = true;

        if (this.mob.level() instanceof ServerLevel serverLevel) {
            // Visual wind/dash particles
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.mob.getX(), this.mob.getY() + 0.3, this.mob.getZ(),
                    8, 0.2, 0.1, 0.2, 0.06);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    this.mob.getX(), this.mob.getY() + 0.4, this.mob.getZ(),
                    5, 0.2, 0.2, 0.2, 0.04);

            // Whoosh audio feedback
            serverLevel.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.6F);
        }

        this.backdashCooldown = 45; // 2.25 seconds cooldown
    }
}
