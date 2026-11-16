package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import net.minecraft.core.Holder;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Nacido del Vacío (Voidborn):
 * Monstruo equivalente a un Hollow en el universo de RPG Levels.
 * Posee estadísticas aleatorias al nacer:
 * - Normal: Rango B (95-115 HP, 13.5-16.5 daño).
 * - Élite (~20%): Rango A Mayor (125-150 HP, 18-21 daño, escala 1.1x).
 * - Variante Ejecutor (~10%): "Voidborn Executor" de Rango HAZARD (175-210 HP, 22-26 daño, escala 1.22x),
 *   porta su propia Espada Espiritual forjada aleatoriamente y desata sus poderes y liberaciones libremente en combate.
 */
public class VoidbornEntity extends Monster {

    private static final EntityDataAccessor<Boolean> IS_EXECUTOR =
            SynchedEntityData.defineId(VoidbornEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> IS_RANK_A =
            SynchedEntityData.defineId(VoidbornEntity.class, EntityDataSerializers.BOOLEAN);

    private int specialAbilityCooldown = 80;
    private int beamCooldown = 60;

    public VoidbornEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 85;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_EXECUTOR, false);
        builder.define(IS_RANK_A, false);
    }

    public boolean isExecutor() {
        return this.entityData.get(IS_EXECUTOR);
    }

    public void setExecutor(boolean executor) {
        this.entityData.set(IS_EXECUTOR, executor);
        updateNameAndThreatTitle();
    }

    public boolean isRankA() {
        return this.entityData.get(IS_RANK_A);
    }

    public void setRankA(boolean rankA) {
        this.entityData.set(IS_RANK_A, rankA);
        updateNameAndThreatTitle();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 110.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 3.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.SCALE, 1.0D);
    }

    public void updateNameAndThreatTitle() {
        MutableComponent name;
        if (isExecutor()) {
            name = Component.empty()
                    .append(ThreatLevel.HAZARD.getBadge())
                    .append(" §6§lVoidborn Executor");
        } else if (isRankA()) {
            name = Component.empty()
                    .append(ThreatLevel.A.getBadge())
                    .append(" §d§lNacido del Vacío Mayor");
        } else {
            name = Component.empty()
                    .append(ThreatLevel.B.getBadge())
                    .append(" §5Nacido del Vacío");
        }
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        // Los Voidborn naturales nacen como Rango B (~75%) o variante Rango A Mayor (~25%).
        // El Voidborn Executor ahora es una entidad separada con su propio huevo de generación.
        if (this.isExecutor()) {
            applyExecutorTraits();
        } else {
            float roll = this.random.nextFloat();
            if (roll < 0.25f) {
                applyRankATraits();
            } else {
                applyRankBTraits();
            }
        }

        this.setHealth(this.getMaxHealth());
        updateNameAndThreatTitle();
        return data;
    }

    public void applyExecutorTraits() {
        this.entityData.set(IS_EXECUTOR, true);
        this.entityData.set(IS_RANK_A, false);

        double maxHp = 175.0D + this.random.nextDouble() * 35.0D; // 175 - 210 HP
        double dmg = 22.0D + this.random.nextDouble() * 4.0D;     // 22 - 26 daño
        double speed = 0.29D + this.random.nextDouble() * 0.03D;

        setAttrBase(Attributes.MAX_HEALTH, maxHp);
        setAttrBase(Attributes.ATTACK_DAMAGE, dmg);
        setAttrBase(Attributes.MOVEMENT_SPEED, speed);
        setAttrBase(Attributes.ARMOR, 14.0D);
        setAttrBase(Attributes.ARMOR_TOUGHNESS, 6.0D);
        setAttrBase(Attributes.KNOCKBACK_RESISTANCE, 0.65D);
        setAttrBase(Attributes.SCALE, 1.22D);
        this.xpReward = 280;

        // Generar Espada Espiritual aleatoria como la de los jugadores
        ItemStack sword = SpiritualSwordItem.createRandomExecutorSword(this.random, "Voidborn Executor");
        this.setItemSlot(EquipmentSlot.MAINHAND, sword);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05F); // 5% chance de drop del arma
    }

    public void applyRankATraits() {
        this.entityData.set(IS_EXECUTOR, false);
        this.entityData.set(IS_RANK_A, true);

        double maxHp = 125.0D + this.random.nextDouble() * 25.0D; // 125 - 150 HP
        double dmg = 18.0D + this.random.nextDouble() * 3.0D;     // 18 - 21 daño
        double speed = 0.27D + this.random.nextDouble() * 0.03D;

        setAttrBase(Attributes.MAX_HEALTH, maxHp);
        setAttrBase(Attributes.ATTACK_DAMAGE, dmg);
        setAttrBase(Attributes.MOVEMENT_SPEED, speed);
        setAttrBase(Attributes.ARMOR, 10.0D);
        setAttrBase(Attributes.ARMOR_TOUGHNESS, 4.0D);
        setAttrBase(Attributes.KNOCKBACK_RESISTANCE, 0.50D);
        setAttrBase(Attributes.SCALE, 1.10D);
        this.xpReward = 140;
    }

    public void applyRankBTraits() {
        this.entityData.set(IS_EXECUTOR, false);
        this.entityData.set(IS_RANK_A, false);

        double maxHp = 95.0D + this.random.nextDouble() * 20.0D;  // 95 - 115 HP
        double dmg = 13.5D + this.random.nextDouble() * 3.0D;     // 13.5 - 16.5 daño
        double speed = 0.25D + this.random.nextDouble() * 0.03D;

        setAttrBase(Attributes.MAX_HEALTH, maxHp);
        setAttrBase(Attributes.ATTACK_DAMAGE, dmg);
        setAttrBase(Attributes.MOVEMENT_SPEED, speed);
        setAttrBase(Attributes.ARMOR, 8.0D);
        setAttrBase(Attributes.ARMOR_TOUGHNESS, 3.0D);
        setAttrBase(Attributes.KNOCKBACK_RESISTANCE, 0.45D);
        setAttrBase(Attributes.SCALE, 1.0D);
        this.xpReward = 85;
    }

    private void setAttrBase(Holder<Attribute> attr, double value) {
        AttributeInstance inst = this.getAttribute(attr);
        if (inst != null) {
            inst.setBaseValue(value);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsExecutor", isExecutor());
        tag.putBoolean("IsRankA", isRankA());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("IsExecutor")) {
            this.entityData.set(IS_EXECUTOR, tag.getBoolean("IsExecutor"));
        }
        if (tag.contains("IsRankA")) {
            this.entityData.set(IS_RANK_A, tag.getBoolean("IsRankA"));
        }
        updateNameAndThreatTitle();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Comportamiento agresivo: ataca a jugadores, aldeanos, golems y miembros de aventureros
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, VoidbornEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AdventurerNpcEntity.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, TownswomanNpcEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();

        // Inteligencia de combate del Voidborn Executor: utiliza libremente su Espada Espiritual
        if (this.isAlive() && isExecutor() && this.getTarget() != null && this.getTarget().isAlive()) {
            LivingEntity target = this.getTarget();
            double distSqr = this.distanceToSqr(target);

            if (beamCooldown > 0) {
                beamCooldown--;
            } else if (distSqr <= 38.0 * 38.0 && this.hasLineOfSight(target)) {
                // Contraataque inmediato si el objetivo nos está disparando un haz espiritual
                boolean targetFiringBeam = false;
                for (SpiritualBeamEntity b : this.level().getEntitiesOfClass(SpiritualBeamEntity.class, target.getBoundingBox().inflate(3.5))) {
                    if (b.getCaster() == target && !b.isClashing()) {
                        targetFiringBeam = true;
                        break;
                    }
                }

                if (targetFiringBeam || (distSqr >= 8.0 * 8.0 && this.random.nextFloat() < 0.05F)) {
                    fireSpiritualBeam(target);
                    beamCooldown = 220 + this.random.nextInt(80);
                    return;
                }
            }

            if (specialAbilityCooldown > 0) {
                specialAbilityCooldown--;
            } else if (distSqr <= 16.0 * 16.0 && this.hasLineOfSight(target)) {
                triggerExecutorSpecialAbility(target);
                specialAbilityCooldown = 160 + this.random.nextInt(80); // 8 a 12 segundos de recarga
            }
        }
    }

    public void fireSpiritualBeam(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        this.getLookControl().setLookAt(target, 180.0F, 180.0F);
        Vec3 dir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        this.setYRot((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        this.setXRot((float) Math.toDegrees(Math.atan2(-dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z))));
        this.swing(InteractionHand.MAIN_HAND);

        ItemStack sword = this.getMainHandItem();
        SpiritualSwordItem.BladeElement element = (sword.getItem() instanceof SpiritualSwordItem)
                ? SpiritualSwordItem.getBladeElement(sword)
                : SpiritualSwordItem.BladeElement.UMBRA;

        int colorRgb = switch (element) {
            case IGNIS -> 0xFF8811;
            case FULGUR -> 0xFFEE22;
            case GLACIES -> 0x33DDFF;
            case UMBRA -> 0xAA22FF;
            default -> 0xFFAA11; // Dorado oscuro Hazard
        };

        SpiritualBeamEntity beam = new SpiritualBeamEntity(sl, this, colorRgb, 26.0F, 140);
        sl.addFreshEntity(beam);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 0.85F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.2F, 0.65F);
    }

    protected void triggerExecutorSpecialAbility(LivingEntity target) {
        ItemStack sword = this.getMainHandItem();
        SpiritualSwordItem.BladeRelease release = SpiritualSwordItem.BladeRelease.SLASH;
        SpiritualSwordItem.BladeElement element = SpiritualSwordItem.BladeElement.UMBRA;

        if (sword.getItem() instanceof SpiritualSwordItem) {
            release = SpiritualSwordItem.getBladeRelease(sword);
            element = SpiritualSwordItem.getBladeElement(sword);
        }

        ServerLevel sl = (ServerLevel) this.level();
        this.swing(InteractionHand.MAIN_HAND);

        switch (release) {
            case SLASH -> {
                // 1. CORTE ESPIRITUAL (Lanza una onda cortante elemental a distancia)
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.7F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WIND_CHARGE_BURST, SoundSource.HOSTILE, 1.2F, 0.8F);

                Vec3 dir = target.position().subtract(this.position()).normalize();
                for (double d = 1.0; d <= 12.0; d += 1.0) {
                    Vec3 p = this.position().add(0, 1.2, 0).add(dir.scale(d));
                    spawnElementParticle(sl, element, p.x, p.y, p.z, 5);
                }
                target.hurt(this.damageSources().mobAttack(this), 16.0F);
                target.setDeltaMovement(target.getDeltaMovement().add(dir.x * 0.8, 0.35, dir.z * 0.8));
                target.hurtMarked = true;
                applyElementEffectOnHit(target, element);
            }
            case VORTEX -> {
                // 2. VÓRTICE DE ALMAS (Atrae y absorbe a todos los objetivos en 8m)
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.6F, 0.6F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.2F, 1.4F);

                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 80, 2.5, 1.0, 2.5, 0.15);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 40, 2.0, 0.8, 2.0, 0.08);

                AABB pullBox = this.getBoundingBox().inflate(8.0);
                for (LivingEntity nearby : sl.getEntitiesOfClass(LivingEntity.class, pullBox, e -> e != this && !(e instanceof VoidbornEntity) && e.isAlive())) {
                    Vec3 pull = this.position().subtract(nearby.position()).normalize().scale(0.85);
                    nearby.setDeltaMovement(pull.x, 0.25, pull.z);
                    nearby.hurtMarked = true;
                    nearby.hurt(this.damageSources().indirectMagic(this, this), 12.0F);
                    nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
                }
            }
            case STEP -> {
                // 3. PASO SOMBRÍO (Teletransporte instantáneo tras la espalda del objetivo con golpe sorpresa)
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4F, 0.8F);
                sl.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 45, 0.5, 0.8, 0.5, 0.2);

                Vec3 targetLook = target.getLookAngle().normalize();
                Vec3 behind = target.position().subtract(targetLook.scale(2.2));
                this.teleportTo(behind.x, target.getY(), behind.z);

                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4F, 0.8F);
                sl.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 35, 0.4, 0.7, 0.4, 0.05);

                // Ataque sorpresa inmediato
                this.doHurtTarget(target);
            }
            case OVERDRIVE -> {
                // 4. TRANCE ESPIRITUAL (Erupción masiva de estadísticas y aura de combate por 8s)
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 1.3F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5F, 1.2F);

                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 60, 0.8, 1.2, 0.8, 0.15);
                sl.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 0.5, this.getZ(), 40, 0.6, 0.8, 0.6, 0.08);

                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1, false, true));
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, true));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0, false, true));
                this.setAbsorptionAmount(Math.max(this.getAbsorptionAmount(), 20.0F));
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Partículas místicas del Vacío / Alma que emanan de su cuerpo
        if (this.level().isClientSide() && this.isAlive() && this.random.nextFloat() < 0.25F) {
            double px = this.getX() + (this.random.nextDouble() - 0.5D) * 0.7D;
            double py = this.getY() + this.random.nextDouble() * 1.8D;
            double pz = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.7D;
            this.level().addParticle(ParticleTypes.SCULK_SOUL, px, py, pz, 0.0D, 0.03D, 0.0D);
            if (this.random.nextFloat() < 0.35F) {
                this.level().addParticle(ParticleTypes.PORTAL, px, py, pz, (this.random.nextDouble() - 0.5D) * 0.2D, -0.1D, (this.random.nextDouble() - 0.5D) * 0.2D);
            }
            if (isExecutor() && this.random.nextFloat() < 0.30F) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz, 0.0D, 0.04D, 0.0D);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            // Golpe corrupto común del Vacío: aplica Ceguera/Oscuridad breve y Ralentización
            living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 45, 0, false, true));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));

            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SCULK_SOUL, living.getX(), living.getY() + 1.0, living.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                sl.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 0.8F, 1.5F);

                // Si es un Executor con Espada Espiritual, desatar el poder pasivo del Elemento:
                if (isExecutor()) {
                    ItemStack sword = this.getMainHandItem();
                    SpiritualSwordItem.BladeElement element = (sword.getItem() instanceof SpiritualSwordItem)
                            ? SpiritualSwordItem.getBladeElement(sword)
                            : SpiritualSwordItem.BladeElement.UMBRA;
                    applyElementEffectOnHit(living, element);
                }
            }
        }
        return hurt;
    }

    private void applyElementEffectOnHit(LivingEntity target, SpiritualSwordItem.BladeElement element) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        switch (element) {
            case IGNIS -> {
                target.igniteForSeconds(5.0F);
                sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.4, 0.3, 0.05);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.0F, 1.0F);
            }
            case FULGUR -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.4, 0.5, 0.4, 0.1);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.8F, 1.8F);
            }
            case GLACIES -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false));
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 60, 200));
                sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(), 14, 0.3, 0.4, 0.3, 0.05);
            }
            case UMBRA -> {
                // Robo de Vida del 50%
                this.heal(6.0F);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 14, 0.3, 0.4, 0.3, 0.05);
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.8, this.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            }
            default -> {}
        }
    }

    private void spawnElementParticle(ServerLevel sl, SpiritualSwordItem.BladeElement element, double x, double y, double z, int count) {
        switch (element) {
            case IGNIS -> sl.sendParticles(ParticleTypes.FLAME, x, y, z, count, 0.2, 0.2, 0.2, 0.03);
            case FULGUR -> sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, count, 0.2, 0.2, 0.2, 0.05);
            case GLACIES -> sl.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, count, 0.2, 0.2, 0.2, 0.02);
            default -> sl.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, count, 0.2, 0.2, 0.2, 0.03);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return isExecutor() ? 0.40F : (isRankA() ? 0.48F : 0.55F);
    }
}
