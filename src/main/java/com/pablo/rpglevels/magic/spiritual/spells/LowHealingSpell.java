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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Optional;

/**
 * Técnica Espiritual Nº 2: Low Healing (Grado Principiante - Rama Ingeniero)
 * Cántico: «Corpus Vitae Proiectio!»
 * Proyecta un pulso sanador hacia el aliado enfocado, restaurándole salud al instante.
 */
public class LowHealingSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "low_healing");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(6)
            .build();

    public LowHealingSpell() {
        this.baseManaCost = 25;
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 4;
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
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        float healAmount = (float) (4.0 + (spellLevel * 1.5) * getEntityPowerMultiplier(caster));

        if (!level.isClientSide()) {
            com.pablo.rpglevels.entities.MagicSpellProjectileEntity projectile =
                    new com.pablo.rpglevels.entities.MagicSpellProjectileEntity(
                            level, caster, com.pablo.rpglevels.entities.MagicSpellProjectileEntity.SpellType.LOW_HEALING, "none", healAmount
                    );
            projectile.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0F, 1.6F, 1.0F);
            level.addFreshEntity(projectile);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
            }
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float amount = (float) (4.0 + (spellLevel * 1.5) * getEntityPowerMultiplier(caster));
        return List.of(
                Component.translatable("ui.rpglevels.technique_chant", "«Corpus Vitae Proiectio!»"),
                Component.translatable("ui.rpglevels.tier_info", "Principiante - Ingeniero"),
                Component.literal("§aSanación: +" + String.format(java.util.Locale.ROOT, "%.1f", amount) + " HP")
        );
    }
}

