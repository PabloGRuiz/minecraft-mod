package com.pablo.tenseicraft.entities.ai;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Authentic RPG Melee Duel AI for Adventurers and Bandits.
 *
 * Features:
 * 1. 3-Hit Attack Combos (Right Slash -> Left Slash -> Heavy Thrust / Overhead Chop).
 * 2. Visual weapon sweep arcs, critical sparks, and metallic impact audio.
 * 3. Frontal Cleave Damage (area sweep matching Better Combat weapon mechanics).
 * 4. Blade Clash / Parry system: When two duelists strike at each other, their weapons
 *    can clash with ringing steel audio, sparks, damage mitigation, and recoil pushback!
 * 5. Tactical Footwork: Circling and strafing instead of robotic straight-line walking.
 */
public class RpgDuelCombatGoal<T extends PathfinderMob & RpgCombatDuelist> extends Goal {

    protected final T mob;
    protected final double speedModifier;
    protected int attackCooldown = 0;
    protected int strafeTime = 0;
    protected boolean strafeClockwise = false;
    protected int ticksSinceLastAttack = 0;
    protected int pathRecalcCooldown = 0;

    public RpgDuelCombatGoal(T mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.isCombatSuspended()) return false;
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.isCombatSuspended()) return false;
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public void start() {
        this.attackCooldown = 8;
        this.strafeTime = 0;
        this.strafeClockwise = this.mob.getRandom().nextBoolean();
        this.mob.setCombatCombo(0);
        this.mob.setBlockingOrParrying(false);
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.mob.setCombatCombo(0);
        this.mob.setBlockingOrParrying(false);
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        double distSq = this.mob.distanceToSqr(target);
        double dist = Math.sqrt(distSq);

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        this.ticksSinceLastAttack++;

        // Reset combo if inactive for more than 28 ticks (1.4 seconds)
        if (this.ticksSinceLastAttack > 28 && this.mob.getCombatCombo() != 0) {
            this.mob.setCombatCombo(0);
        }

        // --- 1. TACTICAL FOOTWORK & STRAFE ---
        // Distance > 5.5 blocks: sprint directly toward target (throttled A* pathfinding every 8 ticks)
        // Distance 2.2 - 5.5 blocks: circle / strafe around target using MoveControl
        // Distance < 2.2 blocks: maintain combat stance
        if (this.pathRecalcCooldown > 0) {
            this.pathRecalcCooldown--;
        }
        if (dist > 5.5) {
            if (this.pathRecalcCooldown <= 0 || this.mob.getNavigation().isDone()) {
                this.pathRecalcCooldown = 8 + this.mob.getRandom().nextInt(5);
                this.mob.getNavigation().moveTo(target, this.speedModifier);
            }
        } else if (dist >= 2.0) {
            this.mob.getNavigation().stop();
            if (--this.strafeTime <= 0) {
                this.strafeTime = 20 + this.mob.getRandom().nextInt(25);
                this.strafeClockwise = !this.strafeClockwise;
            }
            float strafeDir = this.strafeClockwise ? 0.7F : -0.7F;
            float forwardDir = dist > 3.0 ? 0.5F : (dist < 2.3 ? -0.3F : 0.0F);
            this.mob.getMoveControl().strafe(forwardDir, strafeDir);
        } else {
            // Close quarters: stand firm or backstep slightly if crowded
            this.mob.getNavigation().stop();
        }

        // --- 2. MELEE ATTACK COMBO EXECUTION ---
        double reach = 2.8;
        if (dist <= reach && this.attackCooldown <= 0) {
            executeComboAttack(target);
        }
    }

    private void executeComboAttack(LivingEntity target) {
        if (!(this.mob.level() instanceof ServerLevel level)) return;

        int currentCombo = this.mob.getCombatCombo();
        // Advance combo 1 -> 2 -> 3 -> 1
        int nextCombo = (currentCombo % 3) + 1;
        this.mob.setCombatCombo(nextCombo);
        this.mob.setLastAttackGameTime(level.getGameTime());
        this.ticksSinceLastAttack = 0;

        this.mob.swing(InteractionHand.MAIN_HAND);

        Vec3 look = this.mob.getLookAngle();
        Vec3 midPoint = this.mob.position().add(look.scale(1.3)).add(0, this.mob.getEyeHeight() * 0.7, 0);

        // --- BLADE CLASH / PARRY CHECK ---
        // If the target is also a duelist fighting back and facing this mob, chance to clash swords!
        boolean isClash = false;
        if (target instanceof RpgCombatDuelist targetDuelist) {
            Vec3 targetLook = target.getLookAngle();
            double facingDot = look.dot(targetLook); // Negative when facing each other
            if (facingDot < -0.3 && level.getRandom().nextFloat() < 0.28f) {
                isClash = true;
            }
        }

        if (isClash) {
            // Metallic clash sound & spark explosion
            level.playSound(null, midPoint.x, midPoint.y, midPoint.z,
                    SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 1.35F + level.getRandom().nextFloat() * 0.2F);
            level.playSound(null, midPoint.x, midPoint.y, midPoint.z,
                    SoundEvents.ANVIL_HIT, SoundSource.HOSTILE, 0.4F, 1.8F);

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    midPoint.x, midPoint.y, midPoint.z,
                    10, 0.2, 0.2, 0.2, 0.15);
            level.sendParticles(ParticleTypes.CRIT,
                    midPoint.x, midPoint.y, midPoint.z,
                    6, 0.15, 0.15, 0.15, 0.1);

            // Both combatants take minimal damage and recoil slightly
            DamageSource source = this.mob.damageSources().mobAttack(this.mob);
            target.hurt(source, 1.0F);

            Vec3 recoilDir = this.mob.position().subtract(target.position()).normalize().scale(0.35);
            this.mob.push(recoilDir.x, 0.1, recoilDir.z);
            target.push(-recoilDir.x, 0.1, -recoilDir.z);

            this.mob.setBlockingOrParrying(true);
            if (target instanceof RpgCombatDuelist td) {
                td.setBlockingOrParrying(true);
            }

            this.attackCooldown = 14;
            return;
        }

        // --- NORMAL COMBO HIT ---
        this.mob.setBlockingOrParrying(false);

        switch (nextCombo) {
            case 1 -> {
                // Combo 1: Horizontal Slash (Right to Left)
                level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.1F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        midPoint.x, midPoint.y, midPoint.z,
                        1, 0, 0, 0, 0);

                this.mob.doHurtTarget(target);
                applyCleaveDamage(level, target, 0.4F);
                this.attackCooldown = 11;
            }
            case 2 -> {
                // Combo 2: Fast Reverse Slash (Left to Right)
                level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.25F);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        midPoint.x, midPoint.y, midPoint.z,
                        1, 0, 0, 0, 0);

                this.mob.doHurtTarget(target);
                applyCleaveDamage(level, target, 0.45F);
                this.attackCooldown = 12;
            }
            case 3 -> {
                // Combo 3: Heavy Thrust / Downward Finisher (+35% damage + bonus knockback)
                level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 0.9F);
                level.playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.8F, 1.0F);

                level.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        12, 0.3, 0.3, 0.3, 0.15);

                double baseDmg = this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
                DamageSource source = this.mob.damageSources().mobAttack(this.mob);
                target.hurt(source, (float) (baseDmg * 1.35));

                // Extra knockback
                Vec3 pushVec = look.scale(0.45).add(0, 0.1, 0);
                target.push(pushVec.x, pushVec.y, pushVec.z);

                applyCleaveDamage(level, target, 0.55F);
                this.attackCooldown = 22; // Longer recovery after heavy finisher
                this.mob.setCombatCombo(0); // Reset combo chain
            }
        }
    }

    /**
     * Deals cleave / sweep damage to other nearby enemies within a 90° frontal cone.
     */
    private void applyCleaveDamage(ServerLevel level, LivingEntity primaryTarget, float damageRatio) {
        double baseDmg = this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float cleaveDamage = Math.max(1.0F, (float) (baseDmg * damageRatio));

        Vec3 look = this.mob.getLookAngle();
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                primaryTarget.getBoundingBox().inflate(1.8, 0.8, 1.8)
        );

        for (LivingEntity other : nearby) {
            if (other != this.mob && other != primaryTarget && this.mob.canAttack(other)) {
                // Check if in front of attacker
                Vec3 toOther = other.position().subtract(this.mob.position()).normalize();
                if (look.dot(toOther) > 0.35) {
                    other.hurt(this.mob.damageSources().mobAttack(this.mob), cleaveDamage);
                    other.knockback(0.25F, -look.x, -look.z);
                }
            }
        }
    }
}
