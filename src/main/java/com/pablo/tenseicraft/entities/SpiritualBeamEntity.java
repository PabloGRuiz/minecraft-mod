package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.magic.beam.BeamClashManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Entidad que representa el haz continuo de energía de la técnica "Spiritual Beam"
 * (Estilo Kamehameha de Dragon Ball).
 * Proyecta un cilindro horizontal de energía espiritual continua desde las manos del lanzador.
 * Capaz de colisionar con otros Spiritual Beams para iniciar un Choque de Poderes (Beam Clash).
 */
public class SpiritualBeamEntity extends Entity {

    private static final EntityDataAccessor<Integer> CASTER_ID =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> COLOR_RGB =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> BEAM_LENGTH =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> IS_CLASHING =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Float> CLASH_X =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> CLASH_Y =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> CLASH_Z =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> BEAM_RADIUS =
            SynchedEntityData.defineId(SpiritualBeamEntity.class, EntityDataSerializers.FLOAT);

    public static final float MAX_BEAM_RANGE = 42.0F;

    private int maxLifeTicks = 140; // 7 segundos duración base
    private int lifeTicks = 0;
    private float damage = 22.0F;
    @Nullable
    private UUID clashId = null;

    public SpiritualBeamEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SpiritualBeamEntity(Level level, LivingEntity caster, int colorRgb, float damage, int maxLifeTicks) {
        this(ModEntities.SPIRITUAL_BEAM.get(), level);
        this.setCaster(caster);
        this.setColorRgb(colorRgb);
        this.damage = damage;
        this.maxLifeTicks = maxLifeTicks;

        Vec3 eyePos = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        this.setPos(eyePos.x + look.x * 0.4, eyePos.y + look.y * 0.4 - 0.15, eyePos.z + look.z * 0.4);
        this.setYRot(caster.getYRot());
        this.setXRot(caster.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CASTER_ID, -1);
        builder.define(COLOR_RGB, 0x33A6FF); // Cyan espiritual por defecto
        builder.define(BEAM_LENGTH, 10.0F);
        builder.define(IS_CLASHING, false);
        builder.define(CLASH_X, 0.0F);
        builder.define(CLASH_Y, 0.0F);
        builder.define(CLASH_Z, 0.0F);
        builder.define(BEAM_RADIUS, 0.85F);
    }

    public void refreshChanneling(float newDamage) {
        this.damage = newDamage;
        this.lifeTicks = 0;
    }

    public int getCasterId() {
        return this.entityData.get(CASTER_ID);
    }

    public void setCaster(@Nullable LivingEntity caster) {
        this.entityData.set(CASTER_ID, caster != null ? caster.getId() : -1);
    }

    @Nullable
    public LivingEntity getCaster() {
        int id = getCasterId();
        if (id < 0) return null;
        Entity e = this.level().getEntity(id);
        return (e instanceof LivingEntity living) ? living : null;
    }

    public int getColorRgb() {
        return this.entityData.get(COLOR_RGB);
    }

    public void setColorRgb(int rgb) {
        this.entityData.set(COLOR_RGB, rgb);
    }

    public float getBeamLength() {
        return this.entityData.get(BEAM_LENGTH);
    }

    public void setBeamLength(float length) {
        this.entityData.set(BEAM_LENGTH, length);
    }

    public boolean isClashing() {
        return this.entityData.get(IS_CLASHING);
    }

    public void setClashing(boolean clashing) {
        this.entityData.set(IS_CLASHING, clashing);
    }

    public Vec3 getClashPos() {
        return new Vec3(this.entityData.get(CLASH_X), this.entityData.get(CLASH_Y), this.entityData.get(CLASH_Z));
    }

    public void setClashPos(Vec3 pos) {
        this.entityData.set(CLASH_X, (float) pos.x);
        this.entityData.set(CLASH_Y, (float) pos.y);
        this.entityData.set(CLASH_Z, (float) pos.z);
    }

    public float getBeamRadius() {
        return this.entityData.get(BEAM_RADIUS);
    }

    public void setBeamRadius(float radius) {
        this.entityData.set(BEAM_RADIUS, radius);
    }

    @Nullable
    public UUID getClashId() {
        return clashId;
    }

    public void setClashId(@Nullable UUID clashId) {
        this.clashId = clashId;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void tick() {
        super.tick();

        // 1. Validar que el lanzador siga vivo y activo (solo el servidor descarta la entidad para evitar desincronía cliente)
        LivingEntity caster = getCaster();
        if (!this.level().isClientSide()) {
            if (caster == null || !caster.isAlive() || caster.isRemoved()) {
                this.discard();
                return;
            }

            // 2. Si el lanzador es un jugador, cortar si se interrumpe, se cancela o se queda sin maná
            if (caster instanceof Player player) {
                if (com.pablo.tenseicraft.compat.irons.IronsSpellsCompat.isLoaded()) {
                    float mana = com.pablo.tenseicraft.compat.irons.IronsSpellsCompat.getIronsMana(player);
                    if (mana <= 0.0F) {
                        this.discard();
                        return;
                    }

                    try {
                        var magicData = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
                        if (magicData == null || !magicData.isCasting()
                                || !"tenseicraft:spiritual_beam".equals(magicData.getCastingSpellId())) {
                            this.discard();
                            return;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } else if (caster == null || !caster.isAlive() || caster.isRemoved()) {
            return;
        }

        lifeTicks++;

        // Actualizar posición al pecho/manos del lanzador
        Vec3 eyePos = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 origin = eyePos.add(look.scale(0.45)).add(0, -0.15, 0);
        this.setPos(origin.x, origin.y, origin.z);

        if (isClashing()) {
            // Durante un choque: la orientación y longitud terminan estrictamente en el punto de colisión
            Vec3 clashPoint = getClashPos();
            Vec3 toClash = clashPoint.subtract(origin);
            float currentDist = (float) toClash.length();
            this.setBeamLength(currentDist);

            double dx = toClash.x;
            double dy = toClash.y;
            double dz = toClash.z;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            float desiredYaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
            float desiredPitch = (float) (Math.toDegrees(Math.atan2(-dy, horizDist)));
            this.setYRot(desiredYaw);
            this.setXRot(desiredPitch);

            // Mantener al lanzador mirando fijamente hacia el choque
            caster.setYRot(desiredYaw);
            caster.setXRot(desiredPitch);
            caster.setDeltaMovement(0, caster.getDeltaMovement().y > 0 ? 0 : caster.getDeltaMovement().y, 0);

            // Partículas de choque en el centro de energía
            if (this.level().isClientSide() && this.random.nextFloat() < 0.65F) {
                spawnClashParticlesClient(clashPoint);
            }
        } else {
            // Fuera de choque: sigue la mirada libre del lanzador
            this.setYRot(caster.getYRot());
            this.setXRot(caster.getXRot());

            // Raycast hacia adelante: corte estricto contra bloques sólidos
            Vec3 rayEnd = origin.add(look.scale(MAX_BEAM_RANGE));
            BlockHitResult blockHit = this.level().clip(new ClipContext(
                    origin, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this
            ));

            float hitDist = (blockHit.getType() != HitResult.Type.MISS)
                    ? (float) blockHit.getLocation().distanceTo(origin)
                    : MAX_BEAM_RANGE;
            hitDist = Math.max(1.2F, hitDist);

            // Servidor: comprobar colisión de haces y aplicar daño
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                // Comprobar colisión con otro rayo ANTES de aplicar daño
                boolean clashStarted = checkForBeamClash(serverLevel, caster, origin, look, hitDist);
                if (clashStarted) {
                    return;
                }

                this.setBeamLength(hitDist);

                // Sonido continuo de zumbido de haz cada 15 ticks
                if (lifeTicks % 15 == 1) {
                    serverLevel.playSound(null, origin.x, origin.y, origin.z,
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.8F, 1.5F);
                }

                // Daño por perforación a entidades dentro del cilindro cada 4 ticks
                if (lifeTicks % 4 == 0) {
                    applyBeamPiercingDamage(serverLevel, caster, origin, look, hitDist);
                }

                // Fin de duración normal si no está en choque
                if (lifeTicks >= maxLifeTicks) {
                    this.discard();
                }
            } else {
                this.setBeamLength(hitDist);
            }
        }
    }

    private void applyBeamPiercingDamage(ServerLevel serverLevel, LivingEntity caster, Vec3 origin, Vec3 look, float length) {
        Vec3 end = origin.add(look.scale(length));
        AABB box = new AABB(origin, end).inflate(1.6);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != caster && e.isAlive() && !e.isAlliedTo(caster));

        float radius = getBeamRadius();
        DamageSource ds = (caster instanceof Player player)
                ? serverLevel.damageSources().playerAttack(player)
                : serverLevel.damageSources().mobAttack(caster);

        for (LivingEntity target : targets) {
            // Si el objetivo está en choque con este lanzador, nunca recibe daño de perforación
            if (BeamClashManager.isPlayerInClash(target.getId()) && BeamClashManager.isPlayerInClash(caster.getId())) {
                continue;
            }

            Vec3 targetPos = target.getBoundingBox().getCenter();
            Vec3 v = targetPos.subtract(origin);
            double proj = v.dot(look);
            if (proj >= 0.0 && proj <= length) {
                Vec3 closestPoint = origin.add(look.scale(proj));
                double distToRay = closestPoint.distanceTo(targetPos);
                if (distToRay <= radius + target.getBbWidth() * 0.5F) {
                    // Verificar que no haya un bloque sólido entre el haz y el objetivo
                    BlockHitResult blockCheck = serverLevel.clip(new ClipContext(
                            closestPoint, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster
                    ));
                    if (blockCheck.getType() == HitResult.Type.MISS) {
                        target.hurt(ds, this.damage);
                        target.setDeltaMovement(target.getDeltaMovement().add(look.scale(0.35)));
                        target.hurtMarked = true;
                    }
                }
            }
        }
    }

    private boolean checkForBeamClash(ServerLevel serverLevel, LivingEntity caster, Vec3 origin, Vec3 look, float length) {
        AABB searchBox = this.getBoundingBox().inflate(MAX_BEAM_RANGE);
        List<SpiritualBeamEntity> candidates = serverLevel.getEntitiesOfClass(SpiritualBeamEntity.class, searchBox,
                b -> b != this && !b.isClashing() && b.isAlive());

        for (SpiritualBeamEntity other : candidates) {
            LivingEntity otherCaster = other.getCaster();
            if (otherCaster == null || otherCaster == caster || !otherCaster.isAlive()) continue;

            Vec3 otherOrigin = other.position();
            Vec3 otherLook = other.getLookAngle().normalize();

            // 1. Los haces deben ir en direcciones opuestas (encarados)
            double facingDot = look.dot(otherLook);
            if (facingDot > -0.30) continue;

            // 2. Vector relativo de lanzador a lanzador
            Vec3 between = otherOrigin.subtract(origin);
            double dist = between.length();
            if (dist > MAX_BEAM_RANGE * 1.5) continue;

            Vec3 normBetween = between.normalize();
            if (look.dot(normBetween) < 0.25) continue;

            // Colisión confirmada: Se inicia el Choque de Poderes y ambos haces se cortan en el centro
            BeamClashManager.startClash(serverLevel, this, other);
            return true;
        }
        return false;
    }

    private void spawnClashParticlesClient(Vec3 clashPoint) {
        int color = getColorRgb();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        for (int i = 0; i < 4; i++) {
            double rx = (this.random.nextDouble() - 0.5) * 1.6;
            double ry = (this.random.nextDouble() - 0.5) * 1.6;
            double rz = (this.random.nextDouble() - 0.5) * 1.6;
            this.level().addParticle(new DustParticleOptions(new Vector3f(r, g, b), 1.5F),
                    clashPoint.x + rx, clashPoint.y + ry, clashPoint.z + rz,
                    (this.random.nextDouble() - 0.5) * 0.4,
                    (this.random.nextDouble() - 0.5) * 0.4,
                    (this.random.nextDouble() - 0.5) * 0.4);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.damage = compound.getFloat("Damage");
        this.lifeTicks = compound.getInt("LifeTicks");
        this.maxLifeTicks = compound.getInt("MaxLifeTicks");
        setColorRgb(compound.getInt("ColorRgb"));
        setBeamLength(compound.getFloat("BeamLength"));
        setClashing(compound.getBoolean("IsClashing"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Damage", this.damage);
        compound.putInt("LifeTicks", this.lifeTicks);
        compound.putInt("MaxLifeTicks", this.maxLifeTicks);
        compound.putInt("ColorRgb", getColorRgb());
        compound.putFloat("BeamLength", getBeamLength());
        compound.putBoolean("IsClashing", isClashing());
    }
}
