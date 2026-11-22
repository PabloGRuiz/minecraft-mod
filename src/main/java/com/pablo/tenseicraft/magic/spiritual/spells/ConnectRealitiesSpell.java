package com.pablo.tenseicraft.magic.spiritual.spells;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.magic.spiritual.SpiritualSchool;
import com.pablo.tenseicraft.world.dimension.AbyssPortalBlock;
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
 * Técnica Espiritual Nº 20: Conectar Realidades (Connect Realities).
 * Costo: 500 Maná.
 * Desata una onda expansiva que empuja a enemigos cercanos y enciende el Portal del Abismo
 * si impacta contra un marco de Obsidiana Llorosa.
 */
public class ConnectRealitiesSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "connect_realities");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30)
            .build();

    public ConnectRealitiesSpell() {
        this.baseManaCost = 500;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 15;
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
        if (level.isClientSide()) return;

        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 pos = entity.position();

        // 1. Efectos visuales de onda expansiva abisal (radio 5-6m)
        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 0.8, pos.z, 2, 0.2, 0.2, 0.2, 0.0);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1.0, pos.z, 80, 2.5, 1.2, 2.5, 0.2);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.8, pos.z, 40, 2.0, 0.5, 2.0, 0.1);
        serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 0.5, pos.z, 50, 3.0, 0.8, 3.0, 0.05);

        serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.6F);
        serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0F, 0.9F);

        // 2. Empuje y aturdimiento de combate a hostiles cercanos (radio 6m)
        AABB waveBox = entity.getBoundingBox().inflate(6.0);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, waveBox,
                e -> e != entity && e.isAlive() && !(e instanceof Player));
        for (LivingEntity target : targets) {
            Vec3 delta = target.position().subtract(pos).normalize();
            target.setDeltaMovement(delta.x * 1.5, 0.45, delta.z * 1.5);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }

        // 3. Detección y activación del marco de Obsidiana Llorosa
        boolean portalLit = AbyssPortalBlock.attemptPortalIgnition(serverLevel, entity.blockPosition(), 6);

        if (entity instanceof Player player) {
            if (portalLit) {
                serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4F, 0.7F);
                player.sendSystemMessage(Component.literal("§5✦ ¡Técnica Espiritual Nº 20: Conectar Realidades! ✦"));
                player.sendSystemMessage(Component.literal("§dEl tejido dimensional se rasga ante tu poder espiritual. El portal de Obsidiana Llorosa ha despertado."));
                player.displayClientMessage(Component.literal("§d§l¡PORTAL DEL ABISMO ACTIVADO!"), true);
            } else if (entity instanceof net.minecraft.server.level.ServerPlayer sp
                    && com.pablo.tenseicraft.events.GensokyoDimensionEvents.tryHandleConnectRealitiesTeleport(sp)) {
                serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4F, 1.1F);
            } else {
                player.displayClientMessage(Component.literal("§5§l¡CONECTAR REALIDADES! §7(Onda expansiva desatada)"), true);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}

