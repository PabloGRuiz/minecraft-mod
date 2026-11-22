package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.ThreatLevel;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Horned Voidborn (Depredador Cuerpo a Cuerpo):
 * Bestia salvaje de combate cercano que ataca exclusivamente con zarpazos desgarradores
 * de sus enormes garras y embestidas brutales con sus cuernos.
 */
public class HornedVoidbornEntity extends Monster {

    private static final EntityDataAccessor<Integer> DATA_CHARGE_TICKS =
            SynchedEntityData.defineId(HornedVoidbornEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CLAW_SLASH_TICKS =
            SynchedEntityData.defineId(HornedVoidbornEntity.class, EntityDataSerializers.INT);

    private int chargeCooldown = 45;
    private int clawFlurryCooldown = 50;
    private Vec3 chargeDirection = Vec3.ZERO;

    public HornedVoidbornEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 105;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGE_TICKS, 0);
        builder.define(DATA_CLAW_SLASH_TICKS, 0);
    }

    public int getChargeTicks() {
        return this.entityData.get(DATA_CHARGE_TICKS);
    }

    public void setChargeTicks(int ticks) {
        this.entityData.set(DATA_CHARGE_TICKS, ticks);
    }

    public boolean isCharging() {
        return getChargeTicks() > 0;
    }

    public int getClawSlashTicks() {
        return this.entityData.get(DATA_CLAW_SLASH_TICKS);
    }

    public void setClawSlashTicks(int ticks) {
        this.entityData.set(DATA_CLAW_SLASH_TICKS, ticks);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.ARMOR, 9.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 3.5D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.55D)
                .add(Attributes.FOLLOW_RANGE, 36.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, HornedVoidbornEntity.class, HornlessVoidbornEntity.class, ColossalVoidbornEntity.class, VoidbornEntity.class).setAlertOthers());
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
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        updateNameAndThreatTitle();
        return data;
    }

    public void updateNameAndThreatTitle() {
        ThreatLevel threat = ThreatLevel.B;
        this.setData(ModAttachments.MOB_RANK, threat.getRankIndex());

        MutableComponent name = Component.empty()
                .append(threat.getBadge())
                .append(" §5Horned Voidborn");
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (!this.isAlive()) return;

        if (chargeCooldown > 0) chargeCooldown--;
        if (clawFlurryCooldown > 0) clawFlurryCooldown--;
        if (getClawSlashTicks() > 0) setClawSlashTicks(getClawSlashTicks() - 1);

        LivingEntity target = this.getTarget();
        int cTicks = getChargeTicks();

        // 1. Ejecución activa de la Embestida con Cuernos
        if (cTicks > 0) {
            cTicks--;
            setChargeTicks(cTicks);

            if (chargeDirection.lengthSqr() > 0.001D) {
                this.setDeltaMovement(chargeDirection.x * 0.95D, this.getDeltaMovement().y, chargeDirection.z * 0.95D);
                this.hurtMarked = true;
            }

            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.1D, this.getZ(), 4, 0.25D, 0.1D, 0.25D, 0.03D);
                sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5D, this.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.05D);

                // Cornada brutal al impactar durante la embestida
                for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.85D),
                        e -> e != this && !(e instanceof HornedVoidbornEntity) && !(e instanceof HornlessVoidbornEntity) && e.isAlive())) {
                    if (hit == target) {
                        hit.hurt(this.damageSources().mobAttack(this), 20.0F);
                        hit.setDeltaMovement(chargeDirection.x * 1.40D, 0.56D, chargeDirection.z * 1.40D);
                        hit.hurtMarked = true;
                        sl.playSound(null, hit.getX(), hit.getY(), hit.getZ(),
                                SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 1.5F, 0.9F);
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.getX(), hit.getY() + 1.0D, hit.getZ(), 3, 0.3D, 0.3D, 0.3D, 0.0D);
                        setChargeTicks(0);
                        break;
                    }
                }
            }
            return;
        }

        if (target == null || !target.isAlive()) return;

        double distSqr = this.distanceToSqr(target);

        // 2. Zarpazo Feroz en Abanico cuando está a corta distancia (<= 3.8 bloques)
        if (clawFlurryCooldown <= 0 && distSqr <= 14.4D && this.hasLineOfSight(target)) {
            setClawSlashTicks(12);
            clawFlurryCooldown = 55 + this.random.nextInt(25);
            this.swing(InteractionHand.MAIN_HAND);

            if (this.level() instanceof ServerLevel sl) {
                Vec3 look = this.getLookAngle();
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.65F);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        this.getX() + look.x * 1.4D, this.getY() + 1.4D, this.getZ() + look.z * 1.4D,
                        4, 0.4D, 0.3D, 0.4D, 0.0D);

                for (LivingEntity hit : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.2D),
                        e -> e != this && !(e instanceof HornedVoidbornEntity) && e.isAlive())) {
                    if (this.distanceToSqr(hit) <= 12.25D) {
                        hit.hurt(this.damageSources().mobAttack(this), 17.5F);
                        hit.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
                    }
                }
            }
            return;
        }

        // 3. Iniciar Embestida con Cuernos cuando está a media distancia (3.8 a 16 bloques)
        if (chargeCooldown <= 0 && this.onGround() && distSqr >= 14.5D && distSqr <= 256.0D && this.hasLineOfSight(target)) {
            this.chargeDirection = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            setChargeTicks(18);
            chargeCooldown = 60 + this.random.nextInt(30);
            this.swing(InteractionHand.MAIN_HAND);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4F, 0.90F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.setClawSlashTicks(10);
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            Vec3 push = living.position().subtract(this.position()).normalize();
            living.setDeltaMovement(living.getDeltaMovement().add(push.x * 0.55D, 0.25D, push.z * 0.55D));
            living.hurtMarked = true;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, living.getX(), living.getY() + 1.1D, living.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
                sl.playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.2F, 0.75F);
            }
        }
        return hurt;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        updateNameAndThreatTitle();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }
}
