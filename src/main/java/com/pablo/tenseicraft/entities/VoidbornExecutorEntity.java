package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Voidborn Executor: Entidad independiente de Rango HAZARD.
 * Monstruo de élite abisal portador de una Espada Espiritual despierta con habilidades
 * elementales y capaz de desatar y responder con el Spiritual Beam.
 * Cuenta con su propio huevo generador de spawn.
 */
public class VoidbornExecutorEntity extends VoidbornEntity {

    private int specialAbilityCooldown = 80;
    private int beamCooldown = 50;

    public VoidbornExecutorEntity(EntityType<? extends VoidbornEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 160;
        this.setExecutor(true);
        this.setRankA(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 195.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 24.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.SCALE, 1.22D);
    }

    @Override
    public void updateNameAndThreatTitle() {
        MutableComponent name = Component.empty()
                .append(ThreatLevel.HAZARD.getBadge())
                .append(" §6§lVoidborn Executor");
        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        this.setExecutor(true);
        this.setRankA(false);

        // Generar y equipar espada espiritual propia
        ItemStack sword = SpiritualSwordItem.createRandomExecutorSword(this.random, "Voidborn Executor");
        this.setItemSlot(EquipmentSlot.MAINHAND, sword);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.08F);

        updateNameAndThreatTitle();
        return data;
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();

        if (this.isAlive() && this.getTarget() != null && this.getTarget().isAlive()) {
            LivingEntity target = this.getTarget();
            double distSqr = this.distanceToSqr(target);

            // 1. Detección de Contraataque con Spiritual Beam si el objetivo dispara su propio haz
            if (beamCooldown > 0) {
                beamCooldown--;
            } else if (distSqr <= 38.0 * 38.0 && this.hasLineOfSight(target)) {
                boolean targetFiringBeam = false;
                for (SpiritualBeamEntity b : this.level().getEntitiesOfClass(SpiritualBeamEntity.class, target.getBoundingBox().inflate(3.5))) {
                    if (b.getCaster() == target && !b.isClashing()) {
                        targetFiringBeam = true;
                        break;
                    }
                }

                if (targetFiringBeam || (distSqr >= 8.0 * 8.0 && this.random.nextFloat() < 0.06F)) {
                    fireSpiritualBeam(target);
                    beamCooldown = 200 + this.random.nextInt(60);
                    return;
                }
            }

            // 2. Habilidad Especial con la Espada cada 8-12s
            if (specialAbilityCooldown > 0) {
                specialAbilityCooldown--;
            } else if (distSqr <= 16.0 * 16.0 && this.hasLineOfSight(target)) {
                triggerExecutorSpecialAbility(target);
                specialAbilityCooldown = 160 + this.random.nextInt(80);
            }
        }
    }

    @Override
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

        SpiritualBeamEntity beam = new SpiritualBeamEntity(sl, this, colorRgb, 28.0F, 140);
        sl.addFreshEntity(beam);

        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 0.85F);
        sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.2F, 0.65F);
    }
}
