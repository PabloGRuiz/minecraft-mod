package com.pablo.tenseicraft.magic.spiritual.spells;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.entities.EnergyAttackEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Técnica Espiritual Nro. 890: Ha-ku-rei (Obsequio único de Reimu Hakurei).
 * Dispara el orbe de energía Danmaku ({@link EnergyAttackEntity}) que rebota hasta
 * 5 veces entre objetivos hostiles cercanos infligiendo daño espiritual masivo.
 */
public class HakureiSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "hakurei");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(12)
            .build();

    public HakureiSpell() {
        this.baseManaCost = 90;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 55;
        this.spellPowerPerLevel = 0;
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
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            int spirit = 0;
            if (caster instanceof Player player) {
                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
                }
            }

            float damage = (float) (getSpellPower(spellLevel, caster) + (spirit * 2.2));
            Vec3 look = caster.getLookAngle().normalize();

            EnergyAttackEntity orb = new EnergyAttackEntity(sl, caster, 1.35F, damage);
            orb.setLethalInstakill(false);
            orb.setRemainingBounces(5);
            orb.setMaxLifeTicks(160);
            Vec3 spawnPos = new Vec3(caster.getX(), caster.getEyeY() - 0.35, caster.getZ()).add(look.scale(1.1));
            orb.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            orb.setDeltaMovement(look.scale(1.25));
            sl.addFreshEntity(orb);

            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, spawnPos.x, spawnPos.y + 0.3, spawnPos.z, 20, 0.3, 0.3, 0.3, 0.08);
            sl.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y + 0.3, spawnPos.z, 12, 0.2, 0.2, 0.2, 0.08);
            sl.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2F, 1.3F);
            sl.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.65F, 1.7F);
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = (float) getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.tenseicraft.technique_chant", "«Hakurei Musou Sphaera!»"),
                Component.translatable("ui.tenseicraft.tier_info", "Única (Santuario Hakurei)"),
                Component.literal("§cDaño Espiritual Base: " + String.format(Locale.ROOT, "%.1f", power)),
                Component.literal("§dRebotes en Cadena: Hasta 5 objetivos distintos")
        );
    }
}
