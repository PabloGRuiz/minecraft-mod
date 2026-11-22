package com.pablo.tenseicraft.entities;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Peñasco / Roca gigante arrojada por el Hollow Grande.
 */
public class HollowRockProjectileEntity extends ThrowableProjectile implements ItemSupplier {

    private float damage = 16.0F;
    private int ticksInAir = 0;

    public HollowRockProjectileEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public HollowRockProjectileEntity(Level level, LivingEntity shooter, float damage) {
        super(ModEntities.HOLLOW_ROCK.get(), shooter, level);
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.COBBLESTONE);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    public void tick() {
        super.tick();
        ticksInAir++;

        if (this.level().isClientSide()) {
            BlockParticleOption stoneDust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(stoneDust,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D,
                        this.getY() + (this.random.nextDouble() - 0.5D) * 0.5D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D,
                        0.0D, -0.05D, 0.0D);
            }
            this.level().addParticle(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
        } else if (ticksInAir > 100) {
            shatterRock();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hit = result.getEntity();
        Entity owner = this.getOwner();
        if (hit == owner) return;

        if (!this.level().isClientSide() && hit instanceof LivingEntity living) {
            living.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null), this.damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            shatterRock();
        }
    }

    private void shatterRock() {
        if (this.level() instanceof ServerLevel sl) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            sl.playSound(null, x, y, z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.65F);
            sl.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1F, 1.25F);

            BlockParticleOption stoneBurst = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
            sl.sendParticles(stoneBurst, x, y + 0.3D, z, 45, 0.6D, 0.5D, 0.6D, 0.15D);
            sl.sendParticles(ParticleTypes.POOF, x, y + 0.3D, z, 15, 0.5D, 0.4D, 0.5D, 0.05D);
            sl.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.3D, z, 1, 0.1D, 0.1D, 0.1D, 0.0D);

            Entity owner = this.getOwner();
            AABB impactBox = this.getBoundingBox().inflate(2.4D);
            List<LivingEntity> victims = sl.getEntitiesOfClass(LivingEntity.class, impactBox,
                    e -> e != owner && !(e instanceof ColossalVoidbornEntity) && e.isAlive());

            for (LivingEntity victim : victims) {
                victim.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity le ? le : null), this.damage * 0.75F);
                Vec3 push = victim.position().subtract(this.position()).normalize();
                victim.setDeltaMovement(victim.getDeltaMovement().add(push.x * 0.8D, 0.36D, push.z * 0.8D));
                victim.hurtMarked = true;
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
            }
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("RockDamage", this.damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("RockDamage")) {
            this.damage = compound.getFloat("RockDamage");
        }
    }
}
