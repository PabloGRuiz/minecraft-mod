package com.pablo.tenseicraft.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagicFireballEntity extends ThrowableProjectile implements ItemSupplier {

    private float spellDamage = 12.0f;
    private float blastRadius = 3.8f;
    private int burnSeconds = 5;
    private int ticksInAir = 0;

    public MagicFireballEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public MagicFireballEntity(Level level, LivingEntity shooter, float damage, float blastRadius, int burnSeconds) {
        super(ModEntities.MAGIC_FIREBALL.get(), shooter, level);
        this.spellDamage = damage;
        this.blastRadius = blastRadius;
        this.burnSeconds = burnSeconds;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No custom synched data needed
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    public void tick() {
        super.tick();
        ticksInAir++;

        // Client-side dense flame & smoke particle trail
        if (this.level().isClientSide()) {
            Vec3 pos = this.position();
            for (int i = 0; i < 3; i++) {
                double ox = (this.random.nextDouble() - 0.5) * 0.3;
                double oy = (this.random.nextDouble() - 0.5) * 0.3;
                double oz = (this.random.nextDouble() - 0.5) * 0.3;
                this.level().addParticle(ParticleTypes.FLAME, pos.x + ox, pos.y + oy, pos.z + oz, 0, 0, 0);
                this.level().addParticle(ParticleTypes.SMOKE, pos.x + ox, pos.y + oy, pos.z + oz, 0, 0.01, 0);
            }
            if (this.random.nextFloat() < 0.3f) {
                this.level().addParticle(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }

        // Auto explode after 4 seconds (80 ticks) in air if no impact
        if (!this.level().isClientSide() && ticksInAir > 80) {
            explodeAndDiscard();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            explodeAndDiscard();
        }
    }

    private void explodeAndDiscard() {
        if (this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            // Sound effects
            serverLevel.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 1.1f);
            serverLevel.playSound(null, x, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.6f, 0.9f);

            // Blast visual particles (massive fiery burst)
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 3, 0.3, 0.3, 0.3, 0.0);
            serverLevel.sendParticles(ParticleTypes.FLAME, x, y, z, 45, 0.9, 0.9, 0.9, 0.18);
            serverLevel.sendParticles(ParticleTypes.LAVA, x, y, z, 15, 0.5, 0.5, 0.5, 0.1);

            // Area of Effect damage without terrain destruction
            AABB blastBox = new AABB(x - blastRadius, y - blastRadius, z - blastRadius,
                    x + blastRadius, y + blastRadius, z + blastRadius);
            List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, blastBox);

            for (LivingEntity victim : victims) {
                if (victim == this.getOwner()) {
                    continue; // Do not self-harm caster
                }

                // Indirect magic fire damage
                victim.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.spellDamage);
                victim.igniteForSeconds(this.burnSeconds);

                // Knockback away from fireball center
                Vec3 pushDir = victim.position().subtract(this.position()).normalize().scale(0.75);
                victim.push(pushDir.x, 0.35, pushDir.z);
            }
        }
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("SpellDamage", spellDamage);
        compound.putFloat("BlastRadius", blastRadius);
        compound.putInt("BurnSeconds", burnSeconds);
        compound.putInt("TicksInAir", ticksInAir);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SpellDamage")) spellDamage = compound.getFloat("SpellDamage");
        if (compound.contains("BlastRadius")) blastRadius = compound.getFloat("BlastRadius");
        if (compound.contains("BurnSeconds")) burnSeconds = compound.getInt("BurnSeconds");
        if (compound.contains("TicksInAir")) ticksInAir = compound.getInt("TicksInAir");
    }
}
