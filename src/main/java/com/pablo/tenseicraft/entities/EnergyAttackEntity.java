package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.party.PartyManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Orbe de Energía Danmaku (inspiración Touhou) pensado para patrones de disparo,
 * rotaciones orbitales y distintos tamaños configurables mediante {@link #DATA_SCALE}.
 *
 * Soporta además:
 * - {@code lethalInstakill}: usado por Reimu Hakurei (mata instantáneamente sin importar rango/armadura).
 * - {@code remainingBounces}: usado por Técnica Espiritual Nro. 890: Ha-ku-rei (rebota hasta 5 veces).
 * - Modo inspección al invocarse con {@code /summon tenseicraft:energy_attack}.
 */
public class EnergyAttackEntity extends Projectile {

    private static final EntityDataAccessor<Float> DATA_SCALE =
            SynchedEntityData.defineId(EnergyAttackEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE =
            SynchedEntityData.defineId(EnergyAttackEntity.class, EntityDataSerializers.FLOAT);

    // Tiempo máximo de vida (0 = infinito para modo inspección /summon)
    private int maxLifeTicks = 0;

    // Instakill de Reimu & rebotes de Ha-ku-rei
    private boolean lethalInstakill = false;
    private int remainingBounces = 0;
    private final List<Integer> hitEntityIds = new ArrayList<>();

    // Estado de patrón orbital previo al disparo (para formaciones Danmaku en anillo/espiral)
    private boolean orbiting = false;
    private double orbitCenterX = 0.0;
    private double orbitCenterY = 0.0;
    private double orbitCenterZ = 0.0;
    private float orbitRadius = 2.0F;
    private float orbitAngle = 0.0F;
    private float orbitAngularSpeed = 0.1F;
    private int orbitTicksRemaining = 0;
    private float launchSpeed = 0.65F;
    private double launchDirX = 0.0;
    private double launchDirY = 0.0;
    private double launchDirZ = 0.0;
    private boolean hasCustomLaunchDir = false;

    public EnergyAttackEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public EnergyAttackEntity(Level level, LivingEntity shooter, float scale, float damage) {
        super(ModEntities.ENERGY_ATTACK.get(), level);
        this.setOwner(shooter);
        this.setNoGravity(true);
        this.setOrbScale(scale);
        this.setDamage(damage);
        this.maxLifeTicks = 200;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SCALE, 1.0F);
        builder.define(DATA_DAMAGE, 8.0F);
    }

    public float getOrbScale() {
        return Mth.clamp(this.entityData.get(DATA_SCALE), 0.2F, 8.0F);
    }

    public void setOrbScale(float scale) {
        this.entityData.set(DATA_SCALE, Mth.clamp(scale, 0.2F, 8.0F));
        this.refreshDimensions();
    }

    public float getDamage() {
        return this.entityData.get(DATA_DAMAGE);
    }

    public void setDamage(float damage) {
        this.entityData.set(DATA_DAMAGE, Math.max(0.0F, damage));
    }

    public void setMaxLifeTicks(int maxLifeTicks) {
        this.maxLifeTicks = Math.max(0, maxLifeTicks);
    }

    public boolean isLethalInstakill() {
        return this.lethalInstakill;
    }

    public void setLethalInstakill(boolean lethalInstakill) {
        this.lethalInstakill = lethalInstakill;
    }

    public int getRemainingBounces() {
        return this.remainingBounces;
    }

    public void setRemainingBounces(int remainingBounces) {
        this.remainingBounces = Math.max(0, remainingBounces);
    }

    /**
     * Configura una fase orbital previa antes de lanzarse en línea recta en patrones Danmaku.
     */
    public void configureOrbit(Vec3 center, float radius, float initialAngleRad, float angularSpeedRadPerTick,
                               int orbitTicks, float launchSpeed, Vec3 customLaunchDir) {
        this.orbiting = true;
        this.orbitCenterX = center.x;
        this.orbitCenterY = center.y;
        this.orbitCenterZ = center.z;
        this.orbitRadius = Math.max(0.2F, radius);
        this.orbitAngle = initialAngleRad;
        this.orbitAngularSpeed = angularSpeedRadPerTick;
        this.orbitTicksRemaining = Math.max(1, orbitTicks);
        this.launchSpeed = Math.max(0.05F, launchSpeed);

        if (customLaunchDir != null && customLaunchDir.lengthSqr() > 1.0E-5) {
            Vec3 normalized = customLaunchDir.normalize();
            this.launchDirX = normalized.x;
            this.launchDirY = normalized.y;
            this.launchDirZ = normalized.z;
            this.hasCustomLaunchDir = true;
        } else {
            this.hasCustomLaunchDir = false;
        }

        Vec3 startPos = computeOrbitPosition(this.orbitAngle);
        this.setPos(startPos.x, startPos.y, startPos.z);
    }

    private Vec3 computeOrbitPosition(float angle) {
        return new Vec3(
                this.orbitCenterX + Math.cos(angle) * this.orbitRadius,
                this.orbitCenterY,
                this.orbitCenterZ + Math.sin(angle) * this.orbitRadius
        );
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_SCALE.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(getOrbScale());
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (this.maxLifeTicks > 0 && this.tickCount >= this.maxLifeTicks) {
                dissipate(false);
                return;
            }

            if (this.orbiting) {
                this.orbitAngle += this.orbitAngularSpeed;
                Vec3 nextPos = computeOrbitPosition(this.orbitAngle);
                this.setDeltaMovement(nextPos.subtract(this.position()));
                this.setPos(nextPos.x, nextPos.y, nextPos.z);

                this.orbitTicksRemaining--;
                if (this.orbitTicksRemaining <= 0) {
                    launchFromOrbit();
                }
            }

            // Comprobación cilíndrica justa centrada en el disco visible (evita golpes fantasma por encima/debajo o esquinas del cubo)
            if (this.lethalInstakill) {
                float orbScale = getOrbScale();
                double discCenterY = this.getY() + this.getBbHeight() * 0.5;
                double hitRadius = 0.34 * orbScale;
                double halfHeight = 0.22 * orbScale;
                AABB broadBox = new AABB(
                        this.getX() - hitRadius - 0.6, discCenterY - halfHeight - 1.0, this.getZ() - hitRadius - 0.6,
                        this.getX() + hitRadius + 0.6, discCenterY + halfHeight + 1.0, this.getZ() + hitRadius + 0.6
                );
                List<LivingEntity> candidates = this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        broadBox,
                        this::canHitEntity
                );
                for (LivingEntity target : candidates) {
                    if (discCenterY + halfHeight < target.getY()
                            || discCenterY - halfHeight > target.getY() + target.getBbHeight()) {
                        continue;
                    }
                    double targetRadius = (target instanceof Player) ? 0.24 : (target.getBbWidth() * 0.45);
                    double maxDist = hitRadius + targetRadius;
                    double dx = target.getX() - this.getX();
                    double dz = target.getZ() - this.getZ();
                    if (dx * dx + dz * dz <= maxDist * maxDist) {
                        onHitEntity(new EntityHitResult(target));
                        if (this.isRemoved()) {
                            return;
                        }
                    }
                }
            }
        }

        Vec3 velocity = this.getDeltaMovement();
        if (!this.orbiting && velocity.lengthSqr() > 1.0E-6) {
            if (!this.level().isClientSide()) {
                // Para orbes letales, ProjectileUtil sólo comprueba bloques; el impacto con entidades lo gestiona el disco cilíndrico justo arriba
                HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(
                        this,
                        this.lethalInstakill ? (e -> false) : this::canHitEntity
                );
                if (hitResult.getType() != HitResult.Type.MISS) {
                    this.onHit(hitResult);
                    if (this.isRemoved()) {
                        return;
                    }
                }
            }

            Vec3 nextPos = this.position().add(velocity);
            this.setPos(nextPos.x, nextPos.y, nextPos.z);
        }

        if (this.level().isClientSide()) {
            spawnAmbientParticles();
        }
    }

    private void launchFromOrbit() {
        this.orbiting = false;
        Vec3 launchVec;
        if (this.hasCustomLaunchDir) {
            launchVec = new Vec3(this.launchDirX, this.launchDirY, this.launchDirZ).scale(this.launchSpeed);
        } else {
            // Disparo radial hacia afuera desde el centro de la órbita
            Vec3 radial = new Vec3(this.getX() - this.orbitCenterX, 0.0, this.getZ() - this.orbitCenterZ);
            if (radial.lengthSqr() < 1.0E-5) {
                radial = new Vec3(1.0, 0.0, 0.0);
            }
            launchVec = radial.normalize().scale(this.launchSpeed);
        }
        this.setDeltaMovement(launchVec);
        this.hurtMarked = true;
    }

    private void spawnAmbientParticles() {
        float scale = getOrbScale();
        double spread = 0.24 * scale;
        double cy = this.getY() + this.getBbHeight() * 0.5;

        if (this.random.nextFloat() < 0.50F) {
            this.level().addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + (this.random.nextDouble() - 0.5) * spread,
                    cy + (this.random.nextDouble() - 0.5) * 0.08,
                    this.getZ() + (this.random.nextDouble() - 0.5) * spread,
                    0.0, 0.01, 0.0
            );
        }
        if (this.random.nextFloat() < (this.lethalInstakill ? 0.35F : 0.20F)) {
            this.level().addParticle(
                    ParticleTypes.END_ROD,
                    this.getX() + (this.random.nextDouble() - 0.5) * spread * 0.6,
                    cy + (this.random.nextDouble() - 0.5) * 0.06,
                    this.getZ() + (this.random.nextDouble() - 0.5) * spread * 0.6,
                    0.0, 0.005, 0.0
            );
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) {
            return false;
        }
        Entity owner = this.getOwner();
        if (owner != null && target.is(owner)) {
            return false;
        }
        if (this.lethalInstakill) {
            if (target instanceof ReimuBossEntity || target instanceof ReimuNpcEntity) {
                return false;
            }
            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
                return false;
            }
            return target instanceof LivingEntity;
        }
        // Proyectiles de jugador (Ha-ku-rei) no hieren aliados ni al último objetivo recién golpeado
        if (owner instanceof Player && PartyManager.isAllied(owner, target)) {
            return false;
        }
        if (this.hitEntityIds.contains(target.getId()) && this.remainingBounces > 0 && target instanceof LivingEntity) {
            // Permitir re-golpe sólo si estamos apuntando explícitamente en ping-pong; evitar doble colisión en el mismo tick
            if (this.hitEntityIds.get(this.hitEntityIds.size() - 1) == target.getId()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) return;

        Entity hit = result.getEntity();
        Entity owner = this.getOwner();

        if (this.lethalInstakill && hit instanceof LivingEntity living) {
            if (living instanceof Player p && (p.isCreative() || p.isSpectator())) {
                return;
            }
            if (this.level() instanceof ServerLevel sl) {
                double cy = living.getY() + living.getBbHeight() * 0.5;
                sl.sendParticles(ParticleTypes.EXPLOSION, living.getX(), cy, living.getZ(), 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.END_ROD, living.getX(), cy, living.getZ(), 24, 0.3, 0.5, 0.3, 0.15);
                sl.playSound(null, living.getX(), living.getY(), living.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9F, 1.5F);
            }
            DamageSource lethalSource = this.damageSources().genericKill();
            living.setAbsorptionAmount(0.0F);
            living.hurt(lethalSource, Float.MAX_VALUE);
            if (living.isAlive()) {
                living.setHealth(0.0F);
                living.die(lethalSource);
            }
            dissipate(false);
            return;
        }

        if (hit instanceof LivingEntity living) {
            if (owner instanceof Player) {
                living.hurt(this.damageSources().indirectMagic(this, owner), getDamage());
            } else {
                LivingEntity attacker = owner instanceof LivingEntity le ? le : null;
                living.hurt(this.damageSources().mobProjectile(this, attacker), getDamage());
            }

            if (this.remainingBounces > 0) {
                this.hitEntityIds.add(living.getId());
                if (tryBounceToNextTarget(living)) {
                    return;
                }
            }
        }

        dissipate(true);
    }

    /**
     * Busca el siguiente objetivo hostil para Técnica Espiritual Nro. 890: Ha-ku-rei
     * y redirige el orbe en cadena hasta agotar los 5 rebotes.
     */
    private boolean tryBounceToNextTarget(LivingEntity lastVictim) {
        if (!(this.level() instanceof ServerLevel sl)) return false;

        Entity owner = this.getOwner();
        List<LivingEntity> candidates = sl.getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(18.0),
                e -> e.isAlive()
                        && e.getId() != lastVictim.getId()
                        && e != owner
                        && !(e instanceof ReimuNpcEntity)
                        && (e instanceof Enemy || e instanceof Monster || e instanceof ReimuBossEntity)
                        && !(owner != null && PartyManager.isAllied(owner, e))
        );

        if (candidates.isEmpty()) {
            return false;
        }

        // Priorizar enemigos que aún no han sido golpeados en esta cadena de rebotes
        LivingEntity nextTarget = candidates.stream()
                .filter(e -> !this.hitEntityIds.contains(e.getId()))
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElseGet(() -> candidates.stream()
                        .min(Comparator.comparingDouble(this::distanceToSqr))
                        .orElse(null));

        if (nextTarget == null) {
            return false;
        }

        this.remainingBounces--;
        this.maxLifeTicks = Math.max(this.maxLifeTicks, this.tickCount + 40);

        Vec3 from = this.position().add(0.0, 0.35 * getOrbScale(), 0.0);
        Vec3 to = new Vec3(nextTarget.getX(), nextTarget.getY(0.5), nextTarget.getZ());
        Vec3 dir = to.subtract(from).normalize();

        // Posicionar ligeramente fuera de la hitbox anterior para no colisionar dos veces seguidas
        Vec3 startPos = lastVictim.position().add(0.0, lastVictim.getBbHeight() * 0.5, 0.0).add(dir.scale(0.6));
        this.setPos(startPos.x, startPos.y, startPos.z);
        this.setDeltaMovement(dir.scale(1.25));
        this.hurtMarked = true;

        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, startPos.x, startPos.y, startPos.z, 12, 0.25, 0.25, 0.25, 0.08);
        sl.sendParticles(ParticleTypes.END_ROD, startPos.x, startPos.y, startPos.z, 10, 0.2, 0.2, 0.2, 0.10);
        sl.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.4F + (5 - this.remainingBounces) * 0.1F);

        return true;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide()) {
            dissipate(true);
        }
    }

    public void dissipate(boolean withSound) {
        if (this.level() instanceof ServerLevel sl) {
            float scale = getOrbScale();
            double cy = this.getY() + this.getBbHeight() * 0.5;
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), cy, this.getZ(),
                    Math.max(8, (int) (14 * scale)), 0.15 * scale, 0.15 * scale, 0.15 * scale, 0.08);
            sl.sendParticles(ParticleTypes.END_ROD, this.getX(), cy, this.getZ(),
                    Math.max(4, (int) (6 * scale)), 0.10 * scale, 0.10 * scale, 0.10 * scale, 0.05);
            if (withSound) {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.6F, 1.4F);
            }
        }
        this.discard();
    }

    private boolean isStationaryTestOrb() {
        return !this.lethalInstakill && this.remainingBounces == 0 && !this.orbiting
                && this.maxLifeTicks <= 0 && this.getDeltaMovement().lengthSqr() < 1.0E-4;
    }

    @Override
    public boolean isPickable() {
        return isStationaryTestOrb();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isStationaryTestOrb() && !this.level().isClientSide()) {
            dissipate(true);
            return true;
        }
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Scale", getOrbScale());
        compound.putFloat("Damage", getDamage());
        compound.putInt("MaxLifeTicks", this.maxLifeTicks);
        compound.putBoolean("LethalInstakill", this.lethalInstakill);
        compound.putInt("RemainingBounces", this.remainingBounces);
        compound.putBoolean("Orbiting", this.orbiting);
        if (this.orbiting) {
            compound.putDouble("OrbitCenterX", this.orbitCenterX);
            compound.putDouble("OrbitCenterY", this.orbitCenterY);
            compound.putDouble("OrbitCenterZ", this.orbitCenterZ);
            compound.putFloat("OrbitRadius", this.orbitRadius);
            compound.putFloat("OrbitAngle", this.orbitAngle);
            compound.putFloat("OrbitAngularSpeed", this.orbitAngularSpeed);
            compound.putInt("OrbitTicksRemaining", this.orbitTicksRemaining);
            compound.putFloat("LaunchSpeed", this.launchSpeed);
            compound.putBoolean("HasCustomLaunchDir", this.hasCustomLaunchDir);
            compound.putDouble("LaunchDirX", this.launchDirX);
            compound.putDouble("LaunchDirY", this.launchDirY);
            compound.putDouble("LaunchDirZ", this.launchDirZ);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Scale")) {
            setOrbScale(compound.getFloat("Scale"));
        }
        if (compound.contains("Damage")) {
            setDamage(compound.getFloat("Damage"));
        }
        if (compound.contains("MaxLifeTicks")) {
            this.maxLifeTicks = compound.getInt("MaxLifeTicks");
        }
        this.lethalInstakill = compound.getBoolean("LethalInstakill");
        this.remainingBounces = compound.getInt("RemainingBounces");
        this.orbiting = compound.getBoolean("Orbiting");
        if (this.orbiting) {
            this.orbitCenterX = compound.getDouble("OrbitCenterX");
            this.orbitCenterY = compound.getDouble("OrbitCenterY");
            this.orbitCenterZ = compound.getDouble("OrbitCenterZ");
            this.orbitRadius = compound.getFloat("OrbitRadius");
            this.orbitAngle = compound.getFloat("OrbitAngle");
            this.orbitAngularSpeed = compound.getFloat("OrbitAngularSpeed");
            this.orbitTicksRemaining = compound.getInt("OrbitTicksRemaining");
            this.launchSpeed = compound.contains("LaunchSpeed") ? compound.getFloat("LaunchSpeed") : 0.65F;
            this.hasCustomLaunchDir = compound.getBoolean("HasCustomLaunchDir");
            this.launchDirX = compound.getDouble("LaunchDirX");
            this.launchDirY = compound.getDouble("LaunchDirY");
            this.launchDirZ = compound.getDouble("LaunchDirZ");
        }
    }
}
