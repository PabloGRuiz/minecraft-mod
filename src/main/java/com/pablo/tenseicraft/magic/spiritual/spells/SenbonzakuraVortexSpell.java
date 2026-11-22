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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Segundo Ataque Característico de Byakuya Kuchiki (Bleach - Shikai):
 * Senbonzakura: Torbellino de Pétalos (千本桜・花嵐 - Danza del Vórtice de Pétalos)
 * Desata una tormenta esférica de 360° en torno al usuario formada por millones de microláminas
 * en forma de pétalos de cerezo que envuelven, levitan y destrozan a todos los enemigos en el área.
 */
public class SenbonzakuraVortexSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "senbonzakura_vortex");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(7)
            .build();

    public SenbonzakuraVortexSpell() {
        this.baseManaCost = 45;
        this.manaCostPerLevel = 6;
        this.baseSpellPower = 18;
        this.spellPowerPerLevel = 4;
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
        return Optional.of(SoundEvents.CHERRY_LEAVES_BREAK);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(level instanceof ServerLevel serverLevel)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        int spirit = 5;
        int dex = 5;
        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
                dex = data.getTotalStatPoints(player, StatType.DEXTERITY);
            }
            player.displayClientMessage(Component.literal("§d✦ « Torbellino de Pétalos: Senbonzakura » ✦"), true);
        }

        double radius = 8.5 + (spellLevel * 1.0);
        float damage = (float) (getSpellPower(spellLevel, entity) + (spirit * 0.55) + (dex * 0.30));

        // Sonidos orquestales del vórtice
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 2.0f, 0.9f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.8f, 1.1f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.5f, 1.4f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.7f, 1.2f);

        // Despliegue de espirales de 360 grados de pétalos de cerezo a diferentes alturas y radios
        for (double r = 1.5; r <= radius; r += 1.5) {
            int points = (int) (r * 10);
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double px = entity.getX() + Math.cos(angle) * r;
                double pz = entity.getZ() + Math.sin(angle) * r;

                // Múltiples alturas creando una cúpula/torbellino esférico
                for (double yOff = 0.2; yOff <= 3.2; yOff += 1.0) {
                    serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, px, entity.getY() + yOff, pz, 2, 0.12, 0.12, 0.12, 0.04);
                }
            }
        }

        // Anillo exterior de ráfagas cortantes
        for (int i = 0; i < 16; i++) {
            double angle = (2 * Math.PI / 16) * i;
            double px = entity.getX() + Math.cos(angle) * (radius * 0.7);
            double pz = entity.getZ() + Math.sin(angle) * (radius * 0.7);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, px, entity.getY() + 1.2, pz, 1, 0, 0, 0, 0);
        }

        // Búsqueda y daño a todos los enemigos en el radio esférico completo
        AABB vortexBox = entity.getBoundingBox().inflate(radius, 4.0, radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, vortexBox,
                e -> e != entity && SpiritualSwordItem.isValidCombatTarget(entity, e));

        for (LivingEntity target : targets) {
            double dist = entity.distanceTo(target);
            if (dist <= radius) {
                target.hurt(level.damageSources().indirectMagic(entity, entity), damage);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, true));

                // Levantamiento y atracción hacia el torbellino central de pétalos
                Vec3 toPlayer = entity.position().subtract(target.position()).normalize();
                target.setDeltaMovement(toPlayer.x * 0.5, 0.38, toPlayer.z * 0.5);
                target.hurtMarked = true;

                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + 0.8, target.getZ(), 25, 0.5, 0.6, 0.5, 0.12);
                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 0.8, target.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = (float) getSpellPower(spellLevel, caster);
        return List.of(
                Component.literal("§d✦ Daño de Torbellino: §f" + String.format("%.1f", power)),
                Component.literal("§d✦ Radio del Vórtice: §f" + String.format("%.1f", 8.5 + (spellLevel * 1.0)) + " bloques §7(360° Omnidireccional)")
        );
    }
}
