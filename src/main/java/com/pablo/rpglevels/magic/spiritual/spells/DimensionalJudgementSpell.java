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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Habilidad Despertada de la Espada Espiritual: Juicio Dimensional (Dimensional Judgement)
 * Abre una fractura en el espacio en el objetivo, atrapando y rebanando a los enemigos
 * con una tempestad de cortes multidimensionales.
 */
public class DimensionalJudgementSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "dimensional_judgement");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public DimensionalJudgementSpell() {
        this.baseManaCost = 40;
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 11;
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
        return SpellAnimations.SLASH_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ENDER_DRAGON_FLAP);
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

        // Determinar el punto de impacto dimensional (hasta 16 bloques al frente o bloque/entidad objetivo)
        Vec3 eyePos = entity.getEyePosition();
        Vec3 look = entity.getLookAngle();
        Vec3 targetPos = eyePos.add(look.scale(12.0));

        HitResult hit = level.clip(new ClipContext(eyePos, eyePos.add(look.scale(16.0)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        if (hit.getType() != HitResult.Type.MISS) {
            targetPos = hit.getLocation();
        }

        // Sonido inicial de corte espacial
        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.4f, 1.6f);
        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.6f);

        double radius = 4.5 + (spellLevel * 0.3);
        AABB sphereBox = new AABB(
                targetPos.x - radius, targetPos.y - radius, targetPos.z - radius,
                targetPos.x + radius, targetPos.y + radius, targetPos.z + radius
        );

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, sphereBox,
                e -> e != entity && SpiritualSwordItem.isValidCombatTarget(entity, e));

        float totalDamage = (float) (getSpellPower(spellLevel, entity) + (spirit * 0.35));

        // Partículas de cúpula dimensional
        for (int i = 0; i < 40; i++) {
            double theta = serverLevel.random.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * serverLevel.random.nextDouble() - 1);
            double r = radius * 0.9;
            double px = targetPos.x + r * Math.sin(phi) * Math.cos(theta);
            double py = targetPos.y + r * Math.sin(phi) * Math.sin(theta);
            double pz = targetPos.z + r * Math.cos(phi);

            serverLevel.sendParticles(ParticleTypes.PORTAL, px, py, pz, 1, 0, 0, 0, -0.2);
            if (serverLevel.random.nextFloat() < 0.3f) {
                serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0, 0, 0.05);
            }
        }

        // Cortar a cada enemigo dentro de la cúpula
        for (LivingEntity target : targets) {
            // Breve suspensión / gravedad dimensional
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0, false, false, false));
            target.hurt(level.damageSources().indirectMagic(entity, entity), totalDamage);

            // Múltiples cortes visuales
            for (int s = 0; s < 4; s++) {
                double ox = (serverLevel.random.nextDouble() - 0.5) * 1.5;
                double oy = (serverLevel.random.nextDouble() - 0.5) * 1.5 + target.getBbHeight() * 0.5;
                double oz = (serverLevel.random.nextDouble() - 0.5) * 1.5;
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX() + ox, target.getY() + oy, target.getZ() + oz,
                        1, 0, 0, 0, 0);
            }

            SpiritualSwordItem.applyElementalStatusEffect(serverLevel, entity, target, element, totalDamage);
        }

        // Detonación dimensional final al cerrar la fractura
        serverLevel.sendParticles(ParticleTypes.FLASH, targetPos.x, targetPos.y, targetPos.z, 1, 0, 0, 0, 0);
        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8f, 1.8f);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = (float) getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.rpglevels.spell_info.damage", String.format("%.1f", power)),
                Component.translatable("ui.rpglevels.spell_info.dimensional_radius", String.format("%.1f", 4.5 + (spellLevel * 0.3)))
        );
    }
}

