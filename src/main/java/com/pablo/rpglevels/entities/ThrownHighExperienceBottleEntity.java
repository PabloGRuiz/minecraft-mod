package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.items.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * Proyectil arrojadizo de la Botella de Gran Experiencia (High Experience Bottle).
 * Al impactar, otorga 10 veces más experiencia que una botella común (30 a 110 XP).
 */
public class ThrownHighExperienceBottleEntity extends ThrowableItemProjectile {

    public ThrownHighExperienceBottleEntity(EntityType<? extends ThrownHighExperienceBottleEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ThrownHighExperienceBottleEntity(Level level, LivingEntity shooter) {
        super(ModEntities.THROWN_HIGH_EXPERIENCE_BOTTLE.get(), shooter, level);
    }

    public ThrownHighExperienceBottleEntity(Level level, double x, double y, double z) {
        super(ModEntities.THROWN_HIGH_EXPERIENCE_BOTTLE.get(), x, y, z, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.HIGH_EXPERIENCE_BOTTLE.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.07;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel serverLevel) {
            // Efecto visual de poción salpicando (agua/experiencia)
            serverLevel.levelEvent(2002, this.blockPosition(), 0x385dc6);

            // 10 veces más experiencia que la botella común:
            // Vanilla otorga (3 + random(5) + random(5)) -> entre 3 y 11 puntos (promedio ~7)
            // 10x otorga -> entre 30 y 110 puntos de experiencia pura!
            int roll = 3 + serverLevel.random.nextInt(5) + serverLevel.random.nextInt(5);
            int expAmount = roll * 10;
            ExperienceOrb.award(serverLevel, this.position(), expAmount);

            // Partículas y sonidos festivos de alto nivel
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.2, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.2, 0.2, 0.2, 0.08);

            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.9f);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.8f);

            this.discard();
        }
    }
}

