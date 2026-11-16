package com.pablo.rpglevels.magic.spiritual.spells;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import com.pablo.rpglevels.magic.spiritual.SpiritualSchool;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Técnica Espiritual Nº 6: Afinidad Mágica (Grado Principiante - Rama Ingeniero)
 * Cántico: «Anima Aether Fluctuo!»
 * Estabiliza el flujo de maná, potencia la regeneración mágica y resuena con la espada espiritual.
 */
public class MagicAffinitySpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "magic_affinity");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(15)
            .build();

    public MagicAffinitySpell() {
        this.baseManaCost = 40;
        this.manaCostPerLevel = 4;
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
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (entity instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof SpiritualSwordItem && !SpiritualSwordItem.isSoulBound(mainHand)) {
                // Si la espada es virgen y tiene nombre, forjar vínculo
                if (mainHand.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                    SpiritualSwordItem.bindSoulVassal(mainHand, player);
                }
            } else if (mainHand.getItem() instanceof SpiritualSwordItem && SpiritualSwordItem.isSoulBound(mainHand)) {
                int currentAff = SpiritualSwordItem.getAffinity(mainHand);
                SpiritualSwordItem.setAffinity(mainHand, currentAff + 2 + spellLevel);
            }
        }

        // Restaura maná instantáneo
        if (playerMagicData != null) {
            playerMagicData.addMana(15.0f + (spellLevel * 5.0f));
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + 1.2, entity.getZ(), 40, 0.5, 0.6, 0.5, 0.2);
            serverLevel.sendParticles(ParticleTypes.GLOW, entity.getX(), entity.getY() + 0.8, entity.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float manaBonus = 15.0f + (spellLevel * 5.0f);
        return List.of(
                Component.translatable("ui.rpglevels.technique_chant", "«Anima Aether Fluctuo!»"),
                Component.translatable("ui.rpglevels.tier_info", "Principiante - Ingeniero"),
                Component.literal("§bRecupera: +" + (int)manaBonus + " Maná"),
                Component.literal("§dResonancia: +Afinidad de espada")
        );
    }
}
