package com.pablo.rpglevels.magic.spiritual.spells;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.magic.spiritual.SpiritualSchool;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

/**
 * Técnica Espiritual Nº 3: Low Paralize (Grado Principiante - Rama Ingeniero)
 * Cántico: «Fulgur Vinculum Stasis!»
 * Dispara un rayo restrictivo que inmoviliza y ancla al enemigo.
 */
public class LowParalizeSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "low_paralize");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(18)
            .build();

    public LowParalizeSpell() {
        this.baseManaCost = 50;
        this.manaCostPerLevel = 4;
        this.baseSpellPower = 3;
        this.spellPowerPerLevel = 1;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_RAY_SHOOT;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.LIGHTNING_BOLT_THUNDER);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        HitResult hit = Utils.raycastForEntity(level, caster, 20.0f, true);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && target != caster) {
            int durationTicks = (int) ((60 + (spellLevel * 10)) * getEntityPowerMultiplier(caster)); // 3.5s to 8s
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 4, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 2, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 3, false, true, true));

            target.setDeltaMovement(0, 0, 0);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 40, 0.4, 0.6, 0.4, 0.15);
                serverLevel.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 0.5, target.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            }
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float sec = (60 + (spellLevel * 10)) / 20.0f;
        return List.of(
                Component.translatable("ui.rpglevels.technique_chant", "«Fulgur Vinculum Stasis!»"),
                Component.translatable("ui.rpglevels.tier_info", "Principiante - Ingeniero"),
                Component.literal("§eParálisis: " + String.format(java.util.Locale.ROOT, "%.1f", sec) + "s")
        );
    }
}

