package com.pablo.rpglevels.magic.spiritual.spells;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Habilidad Despertada de la Espada Espiritual: Trance Espiritual (Spiritual Overdrive)
 * Desata la sobrecarga del alma durante 12 segundos, potenciando velocidad, daño y reduciendo costes de maná.
 */
public class SpiritualOverdriveSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "spiritual_overdrive");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(12)
            .build();

    public SpiritualOverdriveSpell() {
        this.baseManaCost = 45;
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
        return SpellAnimations.SELF_CAST_TWO_HANDS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.TOTEM_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(level instanceof ServerLevel serverLevel)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        ItemStack held = entity.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            held = entity.getOffhandItem();
        }

        SpiritualSwordItem.BladeElement element = (held.getItem() instanceof SpiritualSwordItem)
                ? SpiritualSwordItem.getBladeElement(held)
                : SpiritualSwordItem.BladeElement.NONE;

        int duration = 240 + (spellLevel * 20); // 12s to 17s
        long until = level.getGameTime() + duration;

        if (entity instanceof Player player) {
            player.getPersistentData().putLong("RpgSwordOverdriveUntil", until);
        }

        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false, true));

        switch (element) {
            case IGNIS -> entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false, true));
            case FULGUR -> {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 2, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 1, false, false, true));
            }
            case GLACIES -> entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false, true));
            case UMBRA -> {
                entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, false, false, true));
                entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1, false, false, true));
            }
            default -> {}
        }

        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0, entity.getZ(), 45, 0.4, 0.7, 0.4, 0.15);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, entity.getX(), entity.getY() + 0.8, entity.getZ(), 30, 0.5, 0.6, 0.5, 0.08);

        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9f, 1.45f);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.1f, 1.3f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("rpglevels.spiritual_sword.release.overdrive"),
                Component.literal("§7Tipo: §6Habilidad Despertada Innata"),
                Component.literal("§eSobrecarga: §fReduce coste de MP un 50% y potencia capacidades")
        );
    }
}

