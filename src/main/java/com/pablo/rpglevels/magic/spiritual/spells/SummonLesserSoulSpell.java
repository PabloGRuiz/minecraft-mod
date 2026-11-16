package com.pablo.rpglevels.magic.spiritual.spells;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.ModEntities;
import com.pablo.rpglevels.entities.PelotitaEntity;
import com.pablo.rpglevels.magic.spiritual.SpiritualSchool;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Técnica Espiritual Nº 7: Convocar Alma Menor (Summon Lesser Soul).
 * Manifiesta a la Pelotita como guardiana leal y permanente del jugador.
 */
public class SummonLesserSoulSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "summon_lesser_soul");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .build();

    public SummonLesserSoulSpell() {
        this.baseManaCost = 50;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 5;
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
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.EVOKER_CAST_SPELL);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        ServerLevel serverLevel = (ServerLevel) level;
        spawnOrTeleportPelotita(player, serverLevel);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    public static void spawnOrTeleportPelotita(Player player, ServerLevel serverLevel) {
        UUID existingUUID = player.getPersistentData().hasUUID("RpgSummonedPelotitaUUID")
                ? player.getPersistentData().getUUID("RpgSummonedPelotitaUUID")
                : null;

        PelotitaEntity existing = null;
        if (existingUUID != null) {
            Entity e = serverLevel.getEntity(existingUUID);
            if (e instanceof PelotitaEntity p && p.isAlive()) {
                existing = p;
            } else if (player.getServer() != null) {
                for (ServerLevel sl : player.getServer().getAllLevels()) {
                    Entity oe = sl.getEntity(existingUUID);
                    if (oe instanceof PelotitaEntity op && op.isAlive()) {
                        existing = op;
                        break;
                    }
                }
            }
        }

        // Si ya tiene una invocación viva: la teletransporta a su lado, la sana y actualiza sus stats
        if (existing != null) {
            existing.teleportTo(serverLevel, player.getX(), player.getY(), player.getZ(), Set.of(), player.getYRot(), 0.0F);
            existing.updateStatsFromOwner(player);
            existing.setHealth(existing.getMaxHealth());
            existing.setOrderedToSit(false);

            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.5, player.getZ(), 30, 0.4, 0.4, 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 0.8, player.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.5F);

            player.displayClientMessage(Component.literal("§a[✦] ¡Tu Pelotita acudió a tu llamado y fue sanada por completo!"), true);
            player.sendSystemMessage(Component.literal("§5✦ [Convocar Alma Menor] §aTu Pelotita ha respondido a tu llamado y acudió a tu lado."));
        } else {
            // Invocar una nueva criatura
            PelotitaEntity pelotita = ModEntities.PELOTITA.get().create(serverLevel);
            if (pelotita != null) {
                pelotita.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
                pelotita.tame(player);
                pelotita.updateStatsFromOwner(player);
                pelotita.setHealth(pelotita.getMaxHealth());

                serverLevel.addFreshEntity(pelotita);
                player.getPersistentData().putUUID("RpgSummonedPelotitaUUID", pelotita.getUUID());

                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 0.6, player.getZ(), 35, 0.4, 0.5, 0.4, 0.15);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.3, 0.4, 0.3, 0.08);
                serverLevel.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 0.9, player.getZ(), 8, 0.3, 0.3, 0.3, 0.1);

                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.4F);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.2F, 1.8F);

                player.displayClientMessage(Component.literal("§d§l¡ALMA MENOR MANIFESTADA! §e(Pelotita convocada)"), true);
                player.sendSystemMessage(Component.literal("§5✦ [Convocar Alma Menor] §d¡Has manifestado un fragmento de tu alma como una pequeña guardiana leal!"));
            }
        }
    }
}
