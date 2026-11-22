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

import java.util.List;
import java.util.Optional;

/**
 * Habilidad Despertada de la Espada Espiritual: Vórtice de Almas (Soul Vortex)
 * Crea un remolino de atracción y detonación elemental en 6 bloques alrededor del usuario.
 */
public class SoulVortexSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "soul_vortex");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(5)
            .build();

    public SoulVortexSpell() {
        this.baseManaCost = 35;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 8;
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
        return SpellAnimations.ONE_HANDED_VERTICAL_UPSWING_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.GENERIC_EXPLODE.value());
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

        double radius = 6.0;
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.25f);
        serverLevel.playSound(null, entity.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.1f, 1.6f);

        // Anillos concéntricos de partículas
        for (double r = 2.0; r <= radius; r += 2.0) {
            for (int angle = 0; angle < 360; angle += 20) {
                double rad = Math.toRadians(angle);
                double px = entity.getX() + Math.cos(rad) * r;
                double pz = entity.getZ() + Math.sin(rad) * r;
                spawnRingParticles(serverLevel, px, entity.getY() + 0.25, pz, element);
            }
        }

        if (element == SpiritualSwordItem.BladeElement.FULGUR) {
            serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 1, 0, 0, 0, 0);
        }

        float damage = (8.0f + (spirit * 0.60f) + (spellLevel * 1.8f)) * getEntityPowerMultiplier(entity);
        AABB area = entity.getBoundingBox().inflate(radius, 3.0, radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                e -> SpiritualSwordItem.isValidCombatTarget(entity, e));

        for (LivingEntity target : targets) {
            Vec3 toPlayer = entity.position().subtract(target.position()).normalize();
            switch (element) {
                case IGNIS -> {
                    target.igniteForSeconds(8);
                    target.setDeltaMovement(toPlayer.x * 0.35, 0.58, toPlayer.z * 0.35);
                }
                case FULGUR -> {
                    target.setDeltaMovement(toPlayer.x * 0.85, 0.25, toPlayer.z * 0.85);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 45, 3, false, true));
                }
                case GLACIES -> {
                    target.setDeltaMovement(0, 0, 0);
                    target.setTicksFrozen(target.getTicksRequiredToFreeze() + 160);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 90, 3, false, true));
                }
                case UMBRA -> {
                    target.setDeltaMovement(toPlayer.x * 0.70, 0.2, toPlayer.z * 0.70);
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1, false, true));
                    entity.heal(2.0f);
                }
                default -> target.setDeltaMovement(toPlayer.x * 0.6, 0.3, toPlayer.z * 0.6);
            }

            target.hurtMarked = true;
            target.hurt(getDamageSource(entity), damage);
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    private void spawnRingParticles(ServerLevel level, double x, double y, double z, SpiritualSwordItem.BladeElement element) {
        switch (element) {
            case IGNIS -> level.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.1, 0.1, 0.1, 0.02);
            case FULGUR -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 3, 0.15, 0.15, 0.15, 0.05);
            case GLACIES -> level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 3, 0.15, 0.15, 0.15, 0.02);
            case UMBRA -> level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 2, 0.1, 0.1, 0.1, 0.02);
            default -> level.sendParticles(ParticleTypes.ENCHANTED_HIT, x, y, z, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("tenseicraft.spiritual_sword.release.vortex"),
                Component.literal("§7Tipo: §6Habilidad Despertada Innata"),
                Component.literal("§bRadio: §f6 Bloques (Atracción y Detonación)")
        );
    }
}

