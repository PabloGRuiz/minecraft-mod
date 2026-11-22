package com.pablo.tenseicraft.magic.spiritual.spells;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Habilidad Despertada de la Espada Espiritual: Paso Sombrío (Shadow Step / Shunpo)
 * Teletransporte táctico a la espalda del enemigo con golpe crítico y señuelo elemental.
 */
public class ShadowStepSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "shadow_step");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(3)
            .build();

    public ShadowStepSpell() {
        this.baseManaCost = 25;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 10;
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
        return SpellAnimations.ONE_HANDED_RAY_SHOOT;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDERMAN_TELEPORT);
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

        Vec3 originPos = entity.position();
        Vec3 eye = entity.getEyePosition();
        Vec3 look = entity.getLookAngle().normalize();
        double maxRange = (element == SpiritualSwordItem.BladeElement.FULGUR) ? 14.0 : 12.0;
        AABB searchBox = entity.getBoundingBox().expandTowards(look.scale(maxRange)).inflate(2.5);

        LivingEntity bestTarget = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                        e -> SpiritualSwordItem.isValidCombatTarget(entity, e))
                .stream()
                .filter(e -> look.dot(e.getBoundingBox().getCenter().subtract(eye).normalize()) > 0.70)
                .min(Comparator.comparingDouble(entity::distanceToSqr))
                .orElse(null);

        // Estela elemental en el punto de partida (señuelo)
        serverLevel.sendParticles(ParticleTypes.POOF, originPos.x, originPos.y + 1.0, originPos.z, 15, 0.3, 0.5, 0.3, 0.05);
        if (element == SpiritualSwordItem.BladeElement.IGNIS) {
            serverLevel.sendParticles(ParticleTypes.FLAME, originPos.x, originPos.y + 1.0, originPos.z, 25, 0.5, 0.5, 0.5, 0.08);
            for (LivingEntity near : serverLevel.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(3.0),
                    e -> SpiritualSwordItem.isValidCombatTarget(entity, e))) {
                near.igniteForSeconds(6);
                near.hurt(getDamageSource(entity), 5.0f);
            }
        } else if (element == SpiritualSwordItem.BladeElement.GLACIES) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, originPos.x, originPos.y + 1.0, originPos.z, 25, 0.5, 0.5, 0.5, 0.05);
            for (LivingEntity near : serverLevel.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(3.0),
                    e -> SpiritualSwordItem.isValidCombatTarget(entity, e))) {
                near.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
            }
        } else if (element == SpiritualSwordItem.BladeElement.UMBRA) {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
        }

        if (bestTarget != null) {
            Vec3 targetLook = bestTarget.getLookAngle().normalize();
            Vec3 behindPos = bestTarget.position().subtract(targetLook.scale(1.5));
            entity.teleportTo(behindPos.x, bestTarget.getY(), behindPos.z);

            float damage = (9.5f + (spirit * 0.65f) + (spellLevel * 2.0f)) * getEntityPowerMultiplier(entity);
            if (element == SpiritualSwordItem.BladeElement.IGNIS) {
                damage *= 1.25f;
                bestTarget.igniteForSeconds(7);
            }

            bestTarget.hurt(getDamageSource(entity), damage);
            serverLevel.sendParticles(ParticleTypes.CRIT, bestTarget.getX(), bestTarget.getY() + 1.0, bestTarget.getZ(), 20, 0.3, 0.4, 0.3, 0.15);

            if (element == SpiritualSwordItem.BladeElement.FULGUR) {
                bestTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, true));
                if (playerMagicData != null) playerMagicData.addMana(3.0f);
            } else if (element == SpiritualSwordItem.BladeElement.UMBRA) {
                entity.heal(Math.min(6.0f, damage * 0.25f));
            }
        } else {
            Vec3 dest = entity.position().add(look.scale(7.5));
            entity.teleportTo(dest.x, dest.y, dest.z);
        }

        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.4f);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1f, 1.2f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("tenseicraft.spiritual_sword.release.step"),
                Component.literal("§7Tipo: §6Habilidad Despertada Innata"),
                Component.literal("§bAlcance: §f12-14 Bloques (Parpadeo Tras Objetivo)")
        );
    }
}

