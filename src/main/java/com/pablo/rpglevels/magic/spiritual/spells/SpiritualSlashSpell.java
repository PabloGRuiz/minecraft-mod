package com.pablo.rpglevels.magic.spiritual.spells;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Habilidad Despertada de la Espada Espiritual: Corte Espiritual (Spiritual Slash)
 * Lanza una media luna cortante de energía espiritual imbuida con el elemento del alma.
 */
public class SpiritualSlashSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "spiritual_slash");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(3)
            .build();

    public SpiritualSlashSpell() {
        this.baseManaCost = 30;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 9;
        this.spellPowerPerLevel = 2;
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
        return SpellAnimations.SLASH_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.PLAYER_ATTACK_SWEEP);
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

        int spirit = 0;
        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
            }
        }

        Vec3 look = entity.getLookAngle().normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1, 0, 0);
        else right = right.normalize();
        Vec3 start = entity.getEyePosition().add(0, -0.25, 0);

        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.75f);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 0.9f, 1.35f);

        float spellMultiplier = getEntityPowerMultiplier(entity);
        float baseDamage = (9.0f + (spirit * 0.65f) + (spellLevel * 2.0f)) * spellMultiplier;
        Set<Integer> hitIds = new HashSet<>();

        for (double d = 1.2; d <= 10.0; d += 0.75) {
            Vec3 center = start.add(look.scale(d));
            // Traza la media luna elemental de energía espiritual
            for (int w = -2; w <= 2; w++) {
                Vec3 arcPt = center.add(right.scale(w * 0.45)).subtract(look.scale(Math.abs(w) * 0.12));
                spawnWaveParticles(serverLevel, arcPt.x, arcPt.y, arcPt.z, element);
            }

            AABB hitBox = new AABB(center.x - 1.5, center.y - 1.2, center.z - 1.5, center.x + 1.5, center.y + 1.2, center.z + 1.5);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> SpiritualSwordItem.isValidCombatTarget(entity, e) && !hitIds.contains(e.getId()));

            for (LivingEntity target : targets) {
                hitIds.add(target.getId());
                float finalDmg = baseDamage;

                if (element == SpiritualSwordItem.BladeElement.IGNIS) {
                    finalDmg *= 1.25f;
                    target.igniteForSeconds(8);
                } else if (element == SpiritualSwordItem.BladeElement.GLACIES && (target.isFullyFrozen() || target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))) {
                    finalDmg *= 1.30f;
                }

                target.hurt(getDamageSource(entity), finalDmg);

                if (element == SpiritualSwordItem.BladeElement.FULGUR) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 3, false, true));
                    if (playerMagicData != null) playerMagicData.addMana(2.0f);
                } else if (element == SpiritualSwordItem.BladeElement.GLACIES) {
                    target.setTicksFrozen(target.getTicksRequiredToFreeze() + 120);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, true));
                } else if (element == SpiritualSwordItem.BladeElement.UMBRA) {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, false, true));
                    entity.heal(Math.min(6.0f, finalDmg * 0.20f));
                }
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnWaveParticles(ServerLevel level, double x, double y, double z, SpiritualSwordItem.BladeElement element) {
        switch (element) {
            case IGNIS -> {
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 5, 0.2, 0.2, 0.2, 0.03);
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.15, 0.15, 0.15, 0.01);
            }
            case FULGUR -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 6, 0.25, 0.25, 0.25, 0.06);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 2, 0.1, 0.1, 0.1, 0.02);
            }
            case GLACIES -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 6, 0.2, 0.2, 0.2, 0.03);
                level.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 3, 0.2, 0.2, 0.2, 0.02);
            }
            case UMBRA -> {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 5, 0.2, 0.2, 0.2, 0.03);
                level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 2, 0.15, 0.15, 0.15, 0.02);
            }
            default -> {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 4, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("rpglevels.spiritual_sword.release.slash"),
                Component.literal("§7Tipo: §6Habilidad Despertada Innata"),
                Component.literal("§bAlcance: §f10 Bloques (Media Luna Elemental)")
        );
    }
}

