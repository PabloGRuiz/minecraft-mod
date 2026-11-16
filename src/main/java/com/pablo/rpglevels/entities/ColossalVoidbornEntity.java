package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.ThreatLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Colossal Voidborn:
 * Coloso brutal que combate golpeando el suelo con ambos puños (creando ondas sísmicas de tierra)
 * y arrancando grandes piedras del suelo para arrojárselas a sus enemigos a distancia.
 */
public class ColossalVoidbornEntity extends Monster {

    private static final EntityDataAccessor<Integer> DATA_SLAM_TICKS =
            SynchedEntityData.defineId(ColossalVoidbornEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_THROW_TICKS =
            SynchedEntityData.defineId(ColossalVoidbornEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_ELITE =
            SynchedEntityData.defineId(ColossalVoidbornEntity.class, EntityDataSerializers.BOOLEAN);

    private int slamCooldown = 40;
    private int throwCooldown = 50;

    public ColossalVoidbornEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 150;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SLAM_TICKS, 0);
        builder.define(DATA_THROW_TICKS, 0);
        builder.define(DATA_IS_ELITE, false);
    }

    public int getSlamTicks() {
        return this.entityData.get(DATA_SLAM_TICKS);
    }

    public void setSlamTicks(int ticks) {
        this.entityData.set(DATA_SLAM_TICKS, ticks);
    }

    public int getThrowTicks() {
        return this.entityData.get(DATA_THROW_TICKS);
    }

    public void setThrowTicks(int ticks) {
        this.entityData.set(DATA_THROW_TICKS, ticks);
    }

    public boolean isElite() {
        return this.entityData.get(DATA_IS_ELITE);
    }

    public void setElite(boolean elite) {
        this.entityData.set(DATA_IS_ELITE, elite);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 155.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.80D)
                .add(Attributes.FOLLOW_RANGE, 38.0D)
                .add(Attributes.STEP_HEIGHT, 1.2D)
                .add(Attributes.SCALE, 1.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, ColossalVoidbornEntity.class, HornedVoidbornEntity.class, HornlessVoidbornEntity.class, VoidbornEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AdventurerNpcEntity.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, TownswomanNpcEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        if (this.random.nextFloat() < 0.25F) {
            setElite(true);
            setBaseStat(Attributes.MAX_HEALTH, 195.0D + this.random.nextDouble() * 30.0D);
            setBaseStat(Attributes.ATTACK_DAMAGE, 22.0D + this.random.nextDouble() * 4.0D);
            setBaseStat(Attributes.ARMOR, 15.0D);
            setBaseStat(Attributes.SCALE, 1.40D);
            this.xpReward = 240;
        } else {
            setElite(false);
            setBaseStat(Attributes.MAX_HEALTH, 145.0D + this.random.nextDouble() * 25.0D);
            setBaseStat(Attributes.ATTACK_DAMAGE, 18.0D + this.random.nextDouble() * 3.0D);
            setBaseStat(Attributes.ARMOR, 12.0D);
            setBaseStat(Attributes.SCALE, 1.25D);
            this.xpReward = 150;
        }

        this.setHealth(this.getMaxHealth());
        updateNameAndThreatTitle();
        return data;
    }

    private void setBaseStat(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double val) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst != null) {
            inst.setBaseValue(val);
        }
    }

    public void updateNameAndThreatTitle() {
        ThreatLevel threat = isElite() ? ThreatLevel.HAZARD : ThreatLevel.A;
        this.setData(ModAttachments.MOB_RANK, threat.getRankIndex());

        MutableComponent name = Component.empty()
                .append(threat.getBadge())
                .append(isElite() ? " §6§lColossal Voidborn" : " §d§lColossal Voidborn");
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();

        if (!this.isAlive()) return;

        if (slamCooldown > 0) slamCooldown--;
        if (throwCooldown > 0) throwCooldown--;

        LivingEntity target = this.getTarget();

        // 1. Procesar animación y efecto de Golpe al Suelo (Ground Slam)
        int currentSlam = getSlamTicks();
        if (currentSlam > 0) {
            this.getNavigation().stop();
            if (target != null) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            currentSlam--;
            setSlamTicks(currentSlam);

            if (currentSlam == 10 && this.level() instanceof ServerLevel sl) {
                executeGroundSlamImpact(sl);
            }
            return;
        }

        // 2. Procesar animación y lanzamiento de Piedra (Boulder Throw)
        int currentThrow = getThrowTicks();
        if (currentThrow > 0) {
            this.getNavigation().stop();
            if (target != null) {
                this.getLookControl().setLookAt(target, 35.0F, 35.0F);
            }
            currentThrow--;
            setThrowTicks(currentThrow);

            if (currentThrow == 10 && this.level() instanceof ServerLevel sl && target != null && target.isAlive()) {
                executeBoulderThrow(sl, target);
            }
            return;
        }

        if (target == null || !target.isAlive()) return;

        double distSqr = this.distanceToSqr(target);

        // Si el enemigo está cerca (<= 5.5 bloques), iniciar Golpe al Suelo
        if (slamCooldown <= 0 && distSqr <= 30.25D && this.onGround()) {
            setSlamTicks(24);
            slamCooldown = 85 + this.random.nextInt(35);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4F, 0.65F);
            return;
        }

        // Si el enemigo está a media o larga distancia (4.5 a 26 bloques), arrancarle una piedra del suelo y arrojársela
        if (throwCooldown <= 0 && distSqr >= 20.25D && distSqr <= 676.0D && this.hasLineOfSight(target)) {
            setThrowTicks(22);
            throwCooldown = 70 + this.random.nextInt(35);
            if (this.level() instanceof ServerLevel sl) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 1.8F, 0.6F);
                BlockState below = sl.getBlockState(this.blockPosition().below());
                if (below.isAir()) below = Blocks.COBBLESTONE.defaultBlockState();
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below),
                        this.getX(), this.getY() + 0.2D, this.getZ(), 25, 0.6D, 0.2D, 0.6D, 0.12D);
            }
        }
    }

    private void executeGroundSlamImpact(ServerLevel sl) {
        this.swing(InteractionHand.MAIN_HAND);
        this.swing(InteractionHand.OFF_HAND);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.75F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.8F, 0.6F);

        BlockPos belowPos = this.blockPosition().below();
        BlockState groundState = sl.getBlockState(belowPos);
        if (groundState.isAir()) {
            groundState = Blocks.COBBLESTONE.defaultBlockState();
        }
        BlockParticleOption groundParticles = new BlockParticleOption(ParticleTypes.BLOCK, groundState);

        // Anillo expansivo de tierra y piedras levantadas del suelo
        for (int r = 1; r <= 5; r++) {
            int points = r * 8;
            for (int i = 0; i < points; i++) {
                double angle = (2.0 * Math.PI * i) / points;
                double px = this.getX() + Math.cos(angle) * r;
                double pz = this.getZ() + Math.sin(angle) * r;
                sl.sendParticles(groundParticles, px, this.getY() + 0.15D, pz, 3, 0.2D, 0.15D, 0.2D, 0.15D);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3D, this.getZ(), 4, 1.5D, 0.2D, 1.5D, 0.02D);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.2D, this.getZ(), 20, 2.0D, 0.2D, 2.0D, 0.04D);

        float slamDmg = isElite() ? 22.0F : 17.0F;
        AABB box = this.getBoundingBox().inflate(5.5D, 2.5D, 5.5D);
        for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != this && !(e instanceof ColossalVoidbornEntity) && e.isAlive())) {
            nearby.hurt(this.damageSources().mobAttack(this), slamDmg);
            Vec3 knock = nearby.position().subtract(this.position()).normalize();
            nearby.setDeltaMovement(knock.x * 0.95D, 0.58D, knock.z * 0.95D);
            nearby.hurtMarked = true;
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
        }
    }

    private void executeBoulderThrow(ServerLevel sl, LivingEntity target) {
        this.swing(InteractionHand.MAIN_HAND);
        float rockDamage = isElite() ? 20.0F : 15.0F;

        HollowRockProjectileEntity rock = new HollowRockProjectileEntity(sl, this, rockDamage);
        rock.setPos(this.getX(), this.getEyeY() - 0.2D, this.getZ());

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.4D) - rock.getY();
        double dz = target.getZ() - this.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        rock.shoot(dx, dy + horizDist * 0.18D, dz, 1.45F, 2.0F);
        sl.addFreshEntity(rock);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.8F, 0.7F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0F, 0.65F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsElite", isElite());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IsElite")) {
            setElite(tag.getBoolean("IsElite"));
        }
        updateNameAndThreatTitle();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.60F;
    }
}
