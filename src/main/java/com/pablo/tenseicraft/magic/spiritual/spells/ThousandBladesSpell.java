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
 * Habilidad Despertada de la Espada Espiritual: Danza de Mil Hojas (Thousand Blades / Senbonzakura)
 * Desintegra la hoja en miles de pétalos cortantes de reiryoku que barren en un cono al frente,
 * despedazando a los enemigos y desgarrando sus defensas.
 */
public class ThousandBladesSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "thousand_blades");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(3)
            .build();

    public ThousandBladesSpell() {
        this.baseManaCost = 30;
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
        return SpellAnimations.SLASH_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.CHERRY_WOOD_BREAK);
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
        Vec3 start = entity.getEyePosition();
        double maxDist = 10.0 + (spellLevel * 1.0);
        float damage = (float) (getSpellPower(spellLevel, entity) + (spirit * 0.30));

        // Efectos de sonido de torbellino de cuchillas
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.4f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.2f);

        // Generar el cono de mil hojas
        for (double d = 1.5; d <= maxDist; d += 0.8) {
            Vec3 center = start.add(look.scale(d));
            double spread = d * 0.35; // ensanchamiento cónico

            for (int p = 0; p < 8; p++) {
                double rx = (serverLevel.random.nextDouble() - 0.5) * spread * 2;
                double ry = (serverLevel.random.nextDouble() - 0.5) * spread * 1.5;
                double rz = (serverLevel.random.nextDouble() - 0.5) * spread * 2;
                Vec3 pPos = center.add(rx, ry, rz);

                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, pPos.x, pPos.y, pPos.z, 1, 0.05, 0.05, 0.05, 0.02);
                serverLevel.sendParticles(ParticleTypes.CRIT, pPos.x, pPos.y, pPos.z, 1, 0.05, 0.05, 0.05, 0.08);

                // Imbuir partículas del elemento si corresponde
                if (element == SpiritualSwordItem.BladeElement.IGNIS) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, pPos.x, pPos.y, pPos.z, 1, 0.02, 0.02, 0.02, 0.01);
                } else if (element == SpiritualSwordItem.BladeElement.FULGUR) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pPos.x, pPos.y, pPos.z, 1, 0.02, 0.02, 0.02, 0.05);
                } else if (element == SpiritualSwordItem.BladeElement.GLACIES) {
                    serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, pPos.x, pPos.y, pPos.z, 1, 0.02, 0.02, 0.02, 0.02);
                } else if (element == SpiritualSwordItem.BladeElement.UMBRA) {
                    serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, pPos.x, pPos.y, pPos.z, 1, 0.02, 0.02, 0.02, 0.02);
                }
            }
        }

        // Buscar entidades dentro del cono
        AABB coneBox = entity.getBoundingBox().inflate(maxDist);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, coneBox,
                e -> e != entity && SpiritualSwordItem.isValidCombatTarget(entity, e));

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(entity.position()).normalize();
            double dot = look.dot(toTarget);
            double dist = entity.distanceTo(target);

            // Filtro angular del cono (~60 grados)
            if (dist <= maxDist && dot > 0.5) {
                target.hurt(level.damageSources().indirectMagic(entity, entity), damage);
                // Desgaste de defensas
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));

                // Retroceso cortante
                Vec3 push = look.scale(0.35).add(0, 0.1, 0);
                target.push(push.x, push.y, push.z);

                SpiritualSwordItem.applyElementalStatusEffect(serverLevel, entity, target, element, damage);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = (float) getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.tenseicraft.spell_info.damage", String.format("%.1f", power)),
                Component.translatable("ui.tenseicraft.spell_info.cone_range", String.format("%.1f", 10.0 + (spellLevel * 1.0)))
        );
    }
}
