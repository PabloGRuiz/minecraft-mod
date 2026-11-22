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
 * Habilidad Despertada de la Espada Espiritual: Lanza Celestial (Celestial Piercer / Getsuga Beam)
 * Dispara un rayo colimado hiperdenso de energía espiritual que perfora a todos los
 * objetivos en una línea de 25 bloques.
 */
public class CelestialPiercerSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "celestial_piercer");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public CelestialPiercerSpell() {
        this.baseManaCost = 35;
        this.manaCostPerLevel = 5;
        this.baseSpellPower = 14;
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
        return SpellAnimations.ONE_HANDED_RAY_SHOOT;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.TRIDENT_RIPTIDE_3.value());
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

        Vec3 start = entity.getEyePosition();
        Vec3 dir = entity.getLookAngle().normalize();
        double maxDist = 24.0 + (spellLevel * 2.0);
        float damage = (float) (getSpellPower(spellLevel, entity) + (spirit * 0.45));

        // Sonido de disparo de cañón espiritual
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 1.8f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.4f);

        Set<LivingEntity> hitEntities = new HashSet<>();

        // Trazado del haz perforante paso a paso
        for (double d = 0.5; d <= maxDist; d += 0.5) {
            Vec3 pos = start.add(dir.scale(d));

            // Partículas centrales del haz
            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.02);
            serverLevel.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 1, 0.1, 0.1, 0.1, 0.01);

            if (element == SpiritualSwordItem.BladeElement.IGNIS) {
                serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.02);
            } else if (element == SpiritualSwordItem.BladeElement.FULGUR) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.05);
            } else if (element == SpiritualSwordItem.BladeElement.GLACIES) {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.02);
            } else if (element == SpiritualSwordItem.BladeElement.UMBRA) {
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.02);
            }

            // Cada 3 bloques, una pequeña onda expansiva
            if (Math.floor(d) % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }

            // Detección de entidades a lo largo del haz (radio 1.5 bloques)
            AABB beamBox = new AABB(pos.x - 1.2, pos.y - 1.2, pos.z - 1.2, pos.x + 1.2, pos.y + 1.2, pos.z + 1.2);
            List<LivingEntity> inStep = serverLevel.getEntitiesOfClass(LivingEntity.class, beamBox,
                    e -> e != entity && !hitEntities.contains(e) && SpiritualSwordItem.isValidCombatTarget(entity, e));

            for (LivingEntity target : inStep) {
                hitEntities.add(target);
                target.hurt(level.damageSources().indirectMagic(entity, entity), damage);

                // Empuje directo en la dirección del disparo
                Vec3 push = dir.scale(0.6).add(0, 0.15, 0);
                target.push(push.x, push.y, push.z);

                serverLevel.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.8f);

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
                Component.translatable("ui.tenseicraft.spell_info.beam_range", String.format("%.1f", 24.0 + (spellLevel * 2.0)))
        );
    }
}
