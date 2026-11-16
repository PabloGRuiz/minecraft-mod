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

import java.util.List;
import java.util.Optional;

/**
 * Habilidad Despertada de la Espada Espiritual: Dominio Gravitacional (Spirit Dominion)
 * Libera una aplastante presión espiritual en 8 bloques a la redonda, estrellando a los
 * enemigos contra el suelo con gravedad forzada e inmovilizándolos.
 */
public class SpiritDominionSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "spirit_dominion");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(6)
            .build();

    public SpiritDominionSpell() {
        this.baseManaCost = 45;
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 12;
        this.spellPowerPerLevel = 3;
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
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ANVIL_LAND);
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
        int fortitude = 0;
        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
                fortitude = data.getTotalStatPoints(player, StatType.FORTITUDE);
            }
        }

        double radius = 7.0 + (spellLevel * 0.5);
        float damage = (float) (getSpellPower(spellLevel, entity) + (spirit * 0.25) + (fortitude * 0.25));

        // Sonidos colosales de impacto gravitacional
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.6f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 0.9f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.9f, 0.5f);

        // Ondas concéntricas de choque gravitacional
        for (double r = 1.0; r <= radius; r += 1.2) {
            int points = (int) (r * 12);
            for (int p = 0; p < points; p++) {
                double angle = (2 * Math.PI / points) * p;
                double px = entity.getX() + r * Math.cos(angle);
                double pz = entity.getZ() + r * Math.sin(angle);
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, px, entity.getY() + 0.1, pz, 1, 0, 0.05, 0, 0.02);
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, px, entity.getY() + 0.2, pz, 1, 0, 0.1, 0, 0.03);
            }
        }

        // El usuario obtiene Barrera de Resistencia por su dominio espiritual
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80 + spellLevel * 20, 1, false, true));

        // Aplastar a todos los enemigos en el área
        AABB area = entity.getBoundingBox().inflate(radius, 3.5, radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != entity && SpiritualSwordItem.isValidCombatTarget(entity, e));

        for (LivingEntity target : targets) {
            if (entity.distanceTo(target) <= radius) {
                target.hurt(level.damageSources().indirectMagic(entity, entity), damage);

                // Aplastamiento gravitacional hacia abajo
                target.setDeltaMovement(target.getDeltaMovement().x * 0.2, -1.2, target.getDeltaMovement().z * 0.2);
                target.hurtMarked = true;

                // Inmovilización y debilitamiento masivo
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70 + spellLevel * 10, 3, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70 + spellLevel * 10, 1, false, true));

                // Partículas de caída aplastante
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.2, target.getZ(), 1, 0, 0, 0, 0);

                SpiritualSwordItem.applyElementalStatusEffect(serverLevel, entity, target, element, damage);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = (float) getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.rpglevels.spell_info.damage", String.format("%.1f", power)),
                Component.translatable("ui.rpglevels.spell_info.radius", String.format("%.1f", 7.0 + (spellLevel * 0.5)))
        );
    }
}
