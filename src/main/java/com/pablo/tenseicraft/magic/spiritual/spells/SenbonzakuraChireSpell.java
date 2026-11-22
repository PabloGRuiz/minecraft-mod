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
 * Ataque Característico de Byakuya Kuchiki (Bleach):
 * Senbonzakura: Chire (散れ, 千本桜 - Dispersa, Senbonzakura)
 * Desintegra la hoja en un torrente masivo de miles de pétalos cortantes que barren a todos los enemigos en área.
 */
public class SenbonzakuraChireSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "senbonzakura_chire");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.EPIC)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(4)
            .build();

    public SenbonzakuraChireSpell() {
        this.baseManaCost = 30;
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

        int spirit = 5;
        int dex = 5;
        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
                dex = data.getTotalStatPoints(player, StatType.DEXTERITY);
            }
            player.displayClientMessage(Component.literal("§d✦ « Dispersa... Senbonzakura » ✦"), true);
        }

        Vec3 look = entity.getLookAngle().normalize();
        Vec3 start = entity.getEyePosition();
        double maxDist = 14.0 + (spellLevel * 1.5);
        float damage = (float) (getSpellPower(spellLevel, entity) + (spirit * 0.45) + (dex * 0.25));

        // Sonidos icónicos del despertar de la hoja en pétalos
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 1.6f, 1.2f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.4f);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.6f, 1.3f);

        // Despliegue masivo del enjambre de pétalos cortantes en cono frontal
        for (double d = 1.5; d <= maxDist; d += 0.7) {
            Vec3 center = start.add(look.scale(d));
            double spread = d * 0.45; // Amplio arco frontal de pétalos

            for (int p = 0; p < 12; p++) {
                double rx = (serverLevel.random.nextDouble() - 0.5) * spread * 2;
                double ry = (serverLevel.random.nextDouble() - 0.5) * spread * 1.6;
                double rz = (serverLevel.random.nextDouble() - 0.5) * spread * 2;
                Vec3 pPos = center.add(rx, ry, rz);

                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, pPos.x, pPos.y, pPos.z, 2, 0.08, 0.08, 0.08, 0.03);
                serverLevel.sendParticles(ParticleTypes.CRIT, pPos.x, pPos.y, pPos.z, 1, 0.04, 0.04, 0.04, 0.06);
            }
        }

        // Búsqueda y daño a todos los enemigos en el área cónica
        AABB coneBox = entity.getBoundingBox().inflate(maxDist);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, coneBox,
                e -> e != entity && SpiritualSwordItem.isValidCombatTarget(entity, e));

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().subtract(entity.position()).normalize();
            double dot = look.dot(toTarget);
            double dist = entity.distanceTo(target);

            // Cobertura angular generosa (~70 grados)
            if (dist <= maxDist && dot > 0.35) {
                target.hurt(level.damageSources().indirectMagic(entity, entity), damage);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true));

                // Retroceso por la tremenda presión espiritual de los pétalos
                Vec3 push = look.scale(0.40).add(0, 0.12, 0);
                target.push(push.x, push.y, push.z);

                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 1, 0, 0, 0, 0);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = (float) getSpellPower(spellLevel, caster);
        return List.of(
                Component.literal("§d✦ Daño de Pétalos: §f" + String.format("%.1f", power)),
                Component.literal("§d✦ Alcance Frontal: §f" + String.format("%.1f", 14.0 + (spellLevel * 1.5)) + " bloques §7(Cono en Área)")
        );
    }
}
