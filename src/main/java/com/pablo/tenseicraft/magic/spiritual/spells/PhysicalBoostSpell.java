package com.pablo.tenseicraft.magic.spiritual.spells;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.magic.spiritual.SpiritualSchool;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
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

import java.util.List;
import java.util.Optional;

/**
 * Técnica Espiritual Nº 1: Physical Boost (Grado Principiante - Rama Combatiente)
 * Cántico: «Corpus Vigor Eruptio!»
 * Potencia las capacidades corporales del lanzador: Fuerza, Velocidad, Resistencia y Absorción.
 */
public class PhysicalBoostSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "physical_boost");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(25)
            .build();

    public PhysicalBoostSpell() {
        this.baseManaCost = 50;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 1;
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
        return SpellAnimations.SELF_CAST_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        int durationTicks = 20 * (25 + spellLevel * 5); // 30s to 75s
        int strAmplifier = spellLevel >= 5 ? 1 : 0; // Fuerza I (Lvl 1-4), Fuerza II (Lvl 5+)
        int speedAmplifier = spellLevel >= 6 ? 1 : 0;

        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, durationTicks, strAmplifier, false, true, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 0, false, true, true));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, durationTicks, speedAmplifier, false, true, true));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, Math.min(2, spellLevel / 3), false, true, true));

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, entity.getX(), entity.getY() + 1.0, entity.getZ(), 45, 0.5, 0.8, 0.5, 0.2);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 0.5, entity.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        int durationSec = 25 + spellLevel * 5;
        return List.of(
                Component.translatable("ui.tenseicraft.technique_chant", "«Corpus Vigor Eruptio!»"),
                Component.translatable("ui.tenseicraft.tier_info", "Principiante - Combatiente"),
                Component.translatable("ui.tenseicraft.duration", durationSec)
        );
    }
}

