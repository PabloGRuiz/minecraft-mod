package com.pablo.tenseicraft.mobs;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import com.pablo.tenseicraft.entities.TownswomanNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class MobActionAiHandler {

    private static final String TAG_LAST_ACTION_TICK = "RpgLastActionTick";

    /**
     * Cooldown scaling per Rank (Rank F = 10s down to Rank SSS = 1.2s)
     */
    public static int getActionCooldownTicks(int rankIndex) {
        return Math.max(24, 200 - (rankIndex * 22));
    }

    /**
     * Determines whether the entity is an eligible hostile or neutral combatant.
     */
    public static boolean isEligibleCombatant(Mob mob) {
        if (mob.level().isClientSide() || !mob.isAlive()) {
            return false;
        }

        if (mob.isPassenger() || mob.isSleeping()) {
            return false;
        }

        // Exclude peaceful townspeople NPCs and blacklisted/special bosses
        if (mob instanceof TownswomanNpcEntity || mob instanceof AdventurerNpcEntity) {
            return false;
        }

        if (MobRankHandler.isBossOrBlacklisted(mob)) {
            return false;
        }

        // Hostile monsters, neutral mobs, or any mob currently targeting someone
        return mob instanceof Enemy || mob instanceof NeutralMob || mob.getTarget() != null;
    }

    public static int getMobRank(Mob mob) {
        if (mob.hasData(ModAttachments.MOB_RANK)) {
            return mob.getData(ModAttachments.MOB_RANK);
        }
        return 0; // Default Rank F
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (mob.level().isClientSide() || !mob.isAlive()) {
            return;
        }

        // CRITICAL PERFORMANCE OPTIMIZATION:
        // Only mobs actively engaged in combat with an alive target run combat AI calculations!
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (!isEligibleCombatant(mob)) {
            return;
        }

        // Stagger checks across ticks based on entity ID (every 8 ticks = ~2.5 times/sec)
        if ((mob.getId() + mob.tickCount) % 8 != 0) {
            return;
        }

        boolean isIrregular = IrregularMobHandler.isIrregular(mob);
        ThreatLevel threat = ThreatLevel.determineThreatLevel(mob);
        int rankIndex = threat.ordinal();
        long currentTick = mob.level().getGameTime();

        // Staggered state check: dizzy particles and freeze mob
        long staggerUntil = mob.getPersistentData().getLong("RpgStaggerUntilTick");
        if (staggerUntil > currentTick) {
            mob.setDeltaMovement(0, Math.min(0, mob.getDeltaMovement().y), 0);
            if (mob.tickCount % 4 == 0 && mob.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WAX_OFF, mob.getX(), mob.getY() + mob.getEyeHeight() + 0.35, mob.getZ(), 2, 0.15, 0.05, 0.15, 0.02);
                sl.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + mob.getEyeHeight() + 0.25, mob.getZ(), 2, 0.15, 0.05, 0.15, 0.02);
            }
            return;
        }

        long lastAction = mob.getPersistentData().getLong(TAG_LAST_ACTION_TICK);
        int actionCooldown = isIrregular ? 60 : getActionCooldownTicks(rankIndex);

        if (currentTick - lastAction < actionCooldown) {
            return;
        }

        // --- 1. TACTICAL EXTINCTION: Dash to Water when Burning ---
        if ((mob.isOnFire() || mob.getRemainingFireTicks() > 20) && (isIrregular || rankIndex >= 1)) {
            if (tryExtinguishFireDash(mob, rankIndex, currentTick)) {
                return;
            }
        }

        // If inside water or lava, do not perform airborne jump/gap dashes
        if (mob.isInWater() || mob.isInLava()) {
            return;
        }

        // --- 2. OBSTACLE NAVIGATION: Double Jump over 2-Block Walls ---
        if (isIrregular || rankIndex >= 2) {
            if (tryObstacleDoubleJump(mob, rankIndex, currentTick)) {
                return;
            }
        }

        // --- 3. COMBAT DASHES (Irregular Ranged Retreat / Melee Gap-Closer) ---
        if (mob.onGround()) {
            if (isIrregular && IrregularMobHandler.isRanged(mob)) {
                if (tryRangedRetreatDash(mob, target, currentTick)) {
                    return;
                }
            } else {
                tryOffensiveGapDash(mob, target, rankIndex, currentTick);
            }
        }
    }

    /**
     * Survival Instinct: When burning on fire, searches for closest water block and dashes into it.
     */
    private static boolean tryExtinguishFireDash(Mob mob, int rankIndex, long currentTick) {
        Level level = mob.level();
        BlockPos mobPos = mob.blockPosition();
        int radius = Math.min(12, 6 + rankIndex);

        BlockPos closestWater = null;
        double closestDistSq = Double.MAX_VALUE;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -2; y <= 2; y++) {
            for (int x = -radius; x <= radius; x += 2) {
                for (int z = -radius; z <= radius; z += 2) {
                    cursor.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);
                    if (level.getFluidState(cursor).is(FluidTags.WATER)) {
                        double dist = mobPos.distSqr(cursor);
                        if (dist < closestDistSq) {
                            closestDistSq = dist;
                            closestWater = cursor.immutable();
                        }
                    }
                }
            }
        }

        if (closestWater != null) {
            Vec3 toWater = Vec3.atCenterOf(closestWater).subtract(mob.position());
            Vec3 dir = new Vec3(toWater.x, 0, toWater.z).normalize();
            double speed = 1.05 + (rankIndex * 0.07);

            mob.setDeltaMovement(dir.x * speed, 0.28, dir.z * speed);
            mob.hasImpulse = true;
            mob.hurtMarked = true;
            mob.getPersistentData().putLong(TAG_LAST_ACTION_TICK, currentTick);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 0.9f, 1.4f);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        mob.getX(), mob.getY() + 0.5, mob.getZ(),
                        5, 0.2, 0.2, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        mob.getX(), mob.getY() + 0.3, mob.getZ(),
                        4, 0.2, 0.1, 0.2, 0.03);
            }
            return true;
        }
        return false;
    }

    /**
     * Obstacle Clearing: Double jump to leap over 2-block tall obstacles while chasing target.
     */
    private static boolean tryObstacleDoubleJump(Mob mob, int rankIndex, long currentTick) {
        Level level = mob.level();
        LivingEntity target = mob.getTarget();

        boolean inAirCollision = !mob.onGround() && mob.horizontalCollision && mob.getDeltaMovement().y > -0.2;
        boolean facingTwoBlockWall = mob.onGround() && isFacingTwoBlockObstacle(mob);

        if (inAirCollision || facingTwoBlockWall) {
            Vec3 forward = (target != null) ? target.position().subtract(mob.position()) : mob.getLookAngle();
            Vec3 dir = new Vec3(forward.x, 0, forward.z).normalize();
            if (dir.lengthSqr() < 0.001) {
                dir = mob.getLookAngle();
            }

            double forwardBoost = 0.38 + (rankIndex * 0.04);
            double upwardImpulse = 0.58; // Clears 2 full blocks easily

            mob.setDeltaMovement(dir.x * forwardBoost, upwardImpulse, dir.z * forwardBoost);
            mob.hasImpulse = true;
            mob.hurtMarked = true;
            mob.resetFallDistance();
            mob.getPersistentData().putLong(TAG_LAST_ACTION_TICK, currentTick);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 0.8f, 1.6f);
                for (int i = 0; i < 6; i++) {
                    double angle = (i / 6.0) * Math.PI * 2;
                    double px = Math.cos(angle) * 0.35;
                    double pz = Math.sin(angle) * 0.35;
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                            mob.getX() + px, mob.getY(), mob.getZ() + pz,
                            1, px * 0.1, -0.05, pz * 0.1, 0.03);
                }
                serverLevel.sendParticles(ParticleTypes.GUST,
                        mob.getX(), mob.getY(), mob.getZ(), 1, 0, 0, 0, 0.05);
            }
            return true;
        }
        return false;
    }

    private static boolean isFacingTwoBlockObstacle(Mob mob) {
        Direction facing = mob.getDirection();
        BlockPos feetFront = mob.blockPosition().relative(facing);
        BlockPos headFront = feetFront.above();
        BlockPos clearAboveObstacle = feetFront.above(2);
        BlockPos clearAboveSelf = mob.blockPosition().above(2);

        Level level = mob.level();
        boolean wallAtFeet = !level.getBlockState(feetFront).getCollisionShape(level, feetFront).isEmpty();
        boolean wallAtHead = !level.getBlockState(headFront).getCollisionShape(level, headFront).isEmpty();
        boolean clearAbove = level.getBlockState(clearAboveObstacle).getCollisionShape(level, clearAboveObstacle).isEmpty();
        boolean clearSelf = level.getBlockState(clearAboveSelf).getCollisionShape(level, clearAboveSelf).isEmpty();

        return (wallAtFeet || wallAtHead) && clearAbove && clearSelf;
    }

    /**
     * Ranged Irregular mob tactical retreat dash:
     * When target (player) gets close, dashes AWAY to take distance and keep shooting.
     */
    private static boolean tryRangedRetreatDash(Mob mob, LivingEntity target, long currentTick) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        double distSq = mob.distanceToSqr(target);
        // Only retreat if target is close (within 8.5 blocks)
        if (distSq > 72.0) {
            return false;
        }

        Vec3 away = mob.position().subtract(target.position());
        Vec3 dir = new Vec3(away.x, 0, away.z).normalize();
        if (dir.lengthSqr() < 0.001) {
            dir = mob.getLookAngle().reverse();
        }

        double retreatSpeed = 1.25;
        mob.setDeltaMovement(dir.x * retreatSpeed, 0.22, dir.z * retreatSpeed);
        mob.hasImpulse = true;
        mob.hurtMarked = true;
        mob.getPersistentData().putLong(TAG_LAST_ACTION_TICK, currentTick);

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 1.0f, 1.5f);
            for (int i = 0; i < 6; i++) {
                double offset = (i / 6.0) * 1.5;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        mob.getX() + dir.x * offset, mob.getY() + 0.3, mob.getZ() + dir.z * offset,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
            serverLevel.sendParticles(ParticleTypes.GUST,
                    mob.getX(), mob.getY() + 0.4, mob.getZ(), 1, 0, 0, 0, 0.05);
        }
        return true;
    }

    /**
     * Offensive Gap-Closer: Mob lunges forward towards player/target with a wind burst dash.
     */
    private static void tryOffensiveGapDash(Mob mob, LivingEntity target, int rankIndex, long currentTick) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        boolean isIrregular = IrregularMobHandler.isIrregular(mob);
        double distSq = mob.distanceToSqr(target);
        double maxDistSq = isIrregular ? 144.0 : (64.0 + (rankIndex * 12.0)); // Irregulars dash up to 12 blocks
        double minDistSq = isIrregular ? 9.0 : 12.0;

        if (distSq < minDistSq || distSq > maxDistSq) {
            return;
        }

        if (!mob.getSensing().hasLineOfSight(target)) {
            return;
        }

        // Irregulars gap-close with balanced 45% chance instead of 85%
        float triggerChance = isIrregular ? 0.45f : (0.25f + (rankIndex * 0.08f));
        if (mob.getRandom().nextFloat() > triggerChance) {
            return;
        }

        Vec3 toTarget = target.position().subtract(mob.position());
        Vec3 dir = new Vec3(toTarget.x, 0, toTarget.z).normalize();
        double dashSpeed = isIrregular ? 1.05 : (0.95 + (rankIndex * 0.075));

        mob.setDeltaMovement(dir.x * dashSpeed, 0.22, dir.z * dashSpeed);
        mob.hasImpulse = true;
        mob.hurtMarked = true;
        mob.getPersistentData().putLong(TAG_LAST_ACTION_TICK, currentTick);

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 1.0f, 1.2f);
            for (int i = 0; i < 5; i++) {
                double offset = (i / 5.0) * 1.2;
                serverLevel.sendParticles(isIrregular ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.CLOUD,
                        mob.getX() - dir.x * offset, mob.getY() + 0.3, mob.getZ() - dir.z * offset,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
            serverLevel.sendParticles(ParticleTypes.GUST,
                    mob.getX(), mob.getY() + 0.4, mob.getZ(), 1, 0, 0, 0, 0.05);
        }
    }

    /**
     * 4. DEFENSIVE EVASION & MORTAL DODGE:
     * When taking damage, higher rank mobs can dodge backwards or to the side with I-Frames.
     */
    @SubscribeEvent
    public static void onMobIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!isEligibleCombatant(mob) || mob.isInWater() || mob.isInLava()) {
            return;
        }

        boolean isIrregular = IrregularMobHandler.isIrregular(mob);
        ThreatLevel threat = ThreatLevel.determineThreatLevel(mob);
        int rankIndex = isIrregular ? 5 : threat.ordinal();
        if (rankIndex < 1 && !isIrregular) { // Rank F mobs are too primitive to dodge
            return;
        }

        // --- 0. ELITE MOB PARRY (Rank A, S, SS, SSS can parry frontal attacks from player) ---
        if (event.getSource().getEntity() instanceof Player player && rankIndex >= 5) {
            Vec3 mobLook = mob.getLookAngle();
            Vec3 toPlayer = player.position().subtract(mob.position()).normalize();
            double dot = mobLook.dot(toPlayer);

            if (dot > 0.35) { // Attacking head-on
                float parryChance = 0.15f + ((rankIndex - 5) * 0.07f); // Rank A: 15%, S: 22%, SS: 29%, SSS: 36%
                if (mob.getRandom().nextFloat() < parryChance) {
                    event.setCanceled(true);

                    if (mob.level() instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, mob.getX(), mob.getY() + 1.0, mob.getZ(),
                                SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2f, 1.7f);
                        serverLevel.playSound(null, mob.getX(), mob.getY() + 1.0, mob.getZ(),
                                SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.8f, 1.7f);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                mob.getX(), mob.getY() + 1.0, mob.getZ(), 12, 0.2, 0.2, 0.2, 0.1);
                        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                mob.getX(), mob.getY() + 0.8, mob.getZ(), 1, 0, 0, 0, 0);
                    }

                    player.displayClientMessage(
                            Component.translatable("tenseicraft.combat.parried"),
                            true
                    );

                    // Recoil push and brief stumble cooldown
                    Vec3 push = player.position().subtract(mob.position()).normalize().scale(0.35);
                    player.setDeltaMovement(player.getDeltaMovement().add(push.x, 0.15, push.z));
                    player.hurtMarked = true;
                    if (!player.getMainHandItem().isEmpty()) {
                        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 15);
                    }
                    return;
                }
            }
        }

        long currentTick = mob.level().getGameTime();
        long lastAction = mob.getPersistentData().getLong(TAG_LAST_ACTION_TICK);
        int cooldown = getActionCooldownTicks(rankIndex);

        float damage = event.getAmount();
        boolean isMortal = (mob.getHealth() - damage <= 0.0f) || (mob.getHealth() <= mob.getMaxHealth() * 0.35f);

        // Emergency reaction speed for mortal blows
        if (isMortal) {
            cooldown = (int) (cooldown * 0.6);
        }

        if (currentTick - lastAction < cooldown) {
            return;
        }

        // Dodge chance scales with rank and desperation
        float baseChance = 0.15f + (rankIndex * 0.10f); // Rank E: 25%, Rank D: 35% ... Rank SSS: 95%
        if (isMortal) {
            baseChance += 0.25f;
        }

        if (mob.getRandom().nextFloat() > baseChance) {
            return;
        }

        // --- SUCCESSFUL DODGE (I-Frames negate incoming hit) ---
        event.setCanceled(true);

        Entity attacker = event.getSource().getEntity();
        Vec3 away;
        if (attacker != null) {
            away = mob.position().subtract(attacker.position());
        } else {
            away = mob.getLookAngle().reverse();
        }

        Vec3 flatAway = new Vec3(away.x, 0, away.z).normalize();
        if (flatAway.lengthSqr() < 0.001) {
            flatAway = new Vec3(0, 0, -1);
        }

        // 60% Backstep, 20% Left Side-step, 20% Right Side-step
        float roll = mob.getRandom().nextFloat();
        Vec3 dodgeDir;
        if (roll < 0.60f) {
            dodgeDir = flatAway;
        } else if (roll < 0.80f) {
            dodgeDir = new Vec3(-flatAway.z, 0, flatAway.x);
        } else {
            dodgeDir = new Vec3(flatAway.z, 0, -flatAway.x);
        }

        double speed = 1.05 + (rankIndex * 0.075);
        mob.setDeltaMovement(dodgeDir.x * speed, 0.24, dodgeDir.z * speed);
        mob.hasImpulse = true;
        mob.hurtMarked = true;
        mob.getPersistentData().putLong(TAG_LAST_ACTION_TICK, currentTick);

        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE, 0.8f, 1.4f);
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 0.8f, 1.6f);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    mob.getX(), mob.getY() + 0.6, mob.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    mob.getX(), mob.getY() + 0.5, mob.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    mob.getX(), mob.getY() + 0.3, mob.getZ(), 3, 0.2, 0.1, 0.2, 0.03);
        }
    }
}
