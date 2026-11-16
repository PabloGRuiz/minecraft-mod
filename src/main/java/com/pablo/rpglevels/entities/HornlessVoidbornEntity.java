package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.ThreatLevel;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Hornless Voidborn (Tirador Abisal):
 * Especialista a distancia que no usa armas humanas: dispara espinas/proyectiles
 * directamente desde sus garras y desata una Lluvia de Proyectiles desde el cielo
 * sobre la posición de la entidad a la que le hace focus.
 */
public class HornlessVoidbornEntity extends Monster implements RangedAttackMob {

    private static final EntityDataAccessor<Integer> DATA_SHOOT_TICKS =
            SynchedEntityData.defineId(HornlessVoidbornEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RAIN_TICKS =
            SynchedEntityData.defineId(HornlessVoidbornEntity.class, EntityDataSerializers.INT);

    private int shootCooldown = 25;
    private int rainCooldown = 120;
    private int backdashCooldown = 0;
    private int strafeTime = 0;
    private boolean strafeClockwise = false;
    private Vec3 rainTargetPos = Vec3.ZERO;

    public HornlessVoidbornEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 90;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHOOT_TICKS, 0);
        builder.define(DATA_RAIN_TICKS, 0);
    }

    public int getShootTicks() {
        return this.entityData.get(DATA_SHOOT_TICKS);
    }

    public void setShootTicks(int ticks) {
        this.entityData.set(DATA_SHOOT_TICKS, ticks);
    }

    public int getRainTicks() {
        return this.entityData.get(DATA_RAIN_TICKS);
    }

    public void setRainTicks(int ticks) {
        this.entityData.set(DATA_RAIN_TICKS, ticks);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 95.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 2.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 38.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, HornlessVoidbornEntity.class, HornedVoidbornEntity.class, ColossalVoidbornEntity.class, VoidbornEntity.class).setAlertOthers());
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
                .append(" §5Hornless Voidborn");
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (!this.isAlive()) return;

        if (shootCooldown > 0) shootCooldown--;
        if (rainCooldown > 0) rainCooldown--;
        if (backdashCooldown > 0) backdashCooldown--;
        if (getShootTicks() > 0) setShootTicks(getShootTicks() - 1);

        LivingEntity target = this.getTarget();

        // 1. Procesar Lluvia de Proyectiles activa (Arrow Rain sobre el foco)
        int rTicks = getRainTicks();
        if (rTicks > 0) {
            this.getNavigation().stop();
            if (target != null && target.isAlive()) {
                this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                this.rainTargetPos = this.rainTargetPos.lerp(target.position(), 0.15D);
            }
            rTicks--;
            setRainTicks(rTicks);

            if (this.level() instanceof ServerLevel sl) {
                double radius = 3.8D;
                for (int i = 0; i < 16; i++) {
                    double angle = (2.0 * Math.PI * i) / 16.0;
                    double px = rainTargetPos.x + Math.cos(angle) * radius;
                    double pz = rainTargetPos.z + Math.sin(angle) * radius;
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, rainTargetPos.y + 0.15D, pz, 1, 0.05D, 0.05D, 0.05D, 0.01D);
                }

                if (rTicks <= 28 && rTicks % 2 == 0) {
                    for (int k = 0; k < 2; k++) {
                        double offsetX = (this.random.nextDouble() - 0.5D) * 2.0D * radius;
                        double offsetZ = (this.random.nextDouble() - 0.5D) * 2.0D * radius;
                        double spawnX = rainTargetPos.x + offsetX;
                        double spawnY = rainTargetPos.y + 13.0D + this.random.nextDouble() * 2.0D;
                        double spawnZ = rainTargetPos.z + offsetZ;

                        Arrow rainArrow = new Arrow(sl, this, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
                        rainArrow.setPos(spawnX, spawnY, spawnZ);
                        rainArrow.setBaseDamage(7.5D);
                        rainArrow.setCritArrow(true);
                        rainArrow.pickup = Arrow.Pickup.DISALLOWED;
                        rainArrow.shoot(0.0D, -1.0D, 0.0D, 1.85F, 3.0F);
                        sl.addFreshEntity(rainArrow);

                        sl.sendParticles(ParticleTypes.PORTAL, spawnX, spawnY, spawnZ, 4, 0.2D, 0.1D, 0.2D, 0.05D);
                    }
                    sl.playSound(null, rainTargetPos.x, rainTargetPos.y + 6.0D, rainTargetPos.z,
                            SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.2F, 0.75F + this.random.nextFloat() * 0.3F);
                }
            }
            return;
        }

        if (target == null || !target.isAlive()) return;

        double distSq = this.distanceToSqr(target);
        double dist = Math.sqrt(distSq);
        boolean canSee = this.getSensing().hasLineOfSight(target);

        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 2. Movimiento táctico de tirador
        if (dist <= 5.0D && backdashCooldown <= 0 && this.onGround()) {
            Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            this.setDeltaMovement(away.x * 0.95D, 0.35D, away.z * 0.95D);
            this.hurtMarked = true;
            this.backdashCooldown = 50;
            this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.5F);
        } else if (dist > 17.0D || !canSee) {
            this.getNavigation().moveTo(target, 1.15D);
        } else {
            this.getNavigation().stop();
            if (--this.strafeTime <= 0) {
                this.strafeTime = 20 + this.random.nextInt(25);
                this.strafeClockwise = !this.strafeClockwise;
            }
            float strafeDir = this.strafeClockwise ? 0.65F : -0.65F;
            float forwardDir = dist > 12.0D ? 0.35F : (dist < 7.5D ? -0.40F : 0.0F);
            this.getMoveControl().strafe(forwardDir, strafeDir);
        }

        if (!canSee) return;

        // 3. Activar Lluvia de Proyectiles sobre el objetivo (cada ~7-9 segundos)
        if (rainCooldown <= 0 && dist >= 4.0D && dist <= 24.0D) {
            this.rainTargetPos = target.position();
            setRainTicks(38);
            rainCooldown = 140 + this.random.nextInt(50);
            this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.5F, 0.75F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 2.8D, this.getZ(), 25, 0.4D, 0.4D, 0.4D, 0.08D);
            }
            return;
        }

        // 4. Disparo directo de proyectil óseo/abismal sin arco
        if (shootCooldown <= 0 && dist <= 22.0D) {
            setShootTicks(10);
            performRangedAttack(target, 1.0F);
            shootCooldown = 28 + this.random.nextInt(14);
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.swing(InteractionHand.MAIN_HAND);
        Arrow spike = new Arrow(sl, this, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
        spike.setPos(this.getX(), this.getEyeY() - 0.25D, this.getZ());
        spike.setBaseDamage(8.5D);
        spike.setCritArrow(true);
        spike.pickup = Arrow.Pickup.DISALLOWED;

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.35D) - spike.getY();
        double dz = target.getZ() - this.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);

        spike.shoot(dx, dy + horiz * 0.16D, dz, 1.9F, 3.0F);
        sl.addFreshEntity(spike);

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getEyeY() - 0.2D, this.getZ(), 8, 0.2D, 0.2D, 0.2D, 0.05D);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.2F, 0.70F);
        this.playSound(SoundEvents.PHANTOM_FLAP, 0.9F, 1.4F);
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
