package com.pablo.rpglevels.magic.spiritual.spells;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.entities.SpiritualBeamEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Tercera Técnica Universal de la Espada Espiritual (Desbloqueada a los 200 de Afinidad):
 * Spiritual Beam (Haz Espiritual - Estilo Kamehameha).
 * Dispara un torrente masivo y continuo de energía espiritual cilíndrica sintonizada con el aura del alma.
 * Si colisiona con el haz de otro combatiente, desencadena un épico Choque de Poderes (Beam Clash).
 */
public class SpiritualBeamSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "spiritual_beam");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(10)
            .build();

    public SpiritualBeamSpell() {
        this.baseManaCost = 50;
        this.manaCostPerLevel = 10;
        this.baseSpellPower = 20;
        this.spellPowerPerLevel = 4;
        this.castTime = 140; // 7 segundos de canalización continua
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
        return CastType.CONTINUOUS;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
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

        // Color del haz según elemento de espada o aura de jugador
        int colorRgb = switch (element) {
            case IGNIS -> 0xFF8811;   // Fuego dorado brillante
            case FULGUR -> 0xFFEE22;  // Rayo amarillo
            case GLACIES -> 0x33DDFF; // Hielo celeste
            case UMBRA -> 0xAA22FF;   // Sombra púrpura
            default -> 0x33A6FF;      // Azul espiritual cian
        };

        float baseDamage = this.baseSpellPower + (spellLevel * this.spellPowerPerLevel) + (spirit * 0.12F);

        // Crear la entidad del haz continuo
        SpiritualBeamEntity beam = new SpiritualBeamEntity(serverLevel, entity, colorRgb, baseDamage, this.castTime);
        serverLevel.addFreshEntity(beam);

        // Sonido atronador de disparo
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.0F, 1.15F);
        serverLevel.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.4F, 1.8F);

        serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.2, entity.getZ(), 2, 0, 0, 0, 0);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
        if (level instanceof ServerLevel serverLevel) {
            // Descartar inmediatamente cualquier haz activo perteneciente a este lanzador
            List<SpiritualBeamEntity> beams = serverLevel.getEntitiesOfClass(SpiritualBeamEntity.class,
                    entity.getBoundingBox().inflate(SpiritualBeamEntity.MAX_BEAM_RANGE),
                    b -> b.getCaster() == entity && b.isAlive());
            for (SpiritualBeamEntity beam : beams) {
                beam.discard();
            }
        }
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.rpglevels.spiritual_beam.info1", (this.baseSpellPower + (spellLevel * this.spellPowerPerLevel))),
                Component.translatable("ui.rpglevels.spiritual_beam.info2")
        );
    }
}
