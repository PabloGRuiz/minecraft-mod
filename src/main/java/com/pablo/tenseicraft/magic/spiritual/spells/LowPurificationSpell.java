package com.pablo.tenseicraft.magic.spiritual.spells;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.magic.spiritual.SpiritualSchool;
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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Técnica Espiritual Nº 4: Low Purification (Grado Principiante - Rama Ingeniero)
 * Cántico: «Anima Purgatio Dubia!»
 * Disipa efectos negativos y venenos del lanzador o de un aliado apuntado.
 */
public class LowPurificationSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "low_purification");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(5)
            .build();

    public LowPurificationSpell() {
        this.baseManaCost = 20;
        this.manaCostPerLevel = 2;
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
        return Optional.of(SoundEvents.AMETHYST_CLUSTER_STEP);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        HitResult hit = Utils.raycastForEntity(level, caster, 16.0f, true);
        LivingEntity target = caster;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            target = living;
        }

        List<net.minecraft.core.Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance instance : target.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                toRemove.add(instance.getEffect());
            }
        }

        for (net.minecraft.core.Holder<MobEffect> effect : toRemove) {
            target.removeEffect(effect);
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + 0.5, target.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.tenseicraft.technique_chant", "«Anima Purgatio Dubia!»"),
                Component.translatable("ui.tenseicraft.tier_info", "Principiante - Ingeniero"),
                Component.literal("§bPurifica: Efectos nocivos y venenos")
        );
    }
}

