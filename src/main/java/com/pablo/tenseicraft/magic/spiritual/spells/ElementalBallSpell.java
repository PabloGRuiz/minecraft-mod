package com.pablo.tenseicraft.magic.spiritual.spells;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.entities.MagicSpellProjectileEntity;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.magic.spiritual.SpiritualSchool;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Técnica Espiritual Nº 5: Elemental Ball / Esfera Espiritual (Grado Principiante - Híbrido)
 * Cántico: «Elementum Sphaera Iactus!»
 * Condensa energía espiritual concentrada en un proyectil esférico visible que viaja a gran velocidad
 * y detona al impactar contra un enemigo o superficie, infligiendo daño mágico en área.
 */
public class ElementalBallSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "elemental_ball");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(4)
            .build();

    public ElementalBallSpell() {
        this.baseManaCost = 25;
        this.manaCostPerLevel = 3;
        this.baseSpellPower = 8;
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
        return SpellAnimations.ANIMATION_INSTANT_CAST;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.FIRECHARGE_USE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        if (!level.isClientSide()) {
            String elem = "none";
            ItemStack held = caster.getMainHandItem();
            if (!(held.getItem() instanceof SpiritualSwordItem)) {
                held = caster.getOffhandItem();
            }
            if (held.getItem() instanceof SpiritualSwordItem) {
                if (SpiritualSwordItem.isSenbonzakura(held)) {
                    elem = "senbonzakura";
                } else {
                    elem = SpiritualSwordItem.getBladeElement(held).getId();
                }
            }

            int spirit = 0;
            if (caster instanceof Player player) {
                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
                }
            }

            float power = (float) (getSpellPower(spellLevel, caster) + (spirit * 0.40));

            MagicSpellProjectileEntity proj = new MagicSpellProjectileEntity(
                    level,
                    caster,
                    MagicSpellProjectileEntity.SpellType.ELEMENTAL_BALL,
                    elem,
                    power
            );

            Vec3 look = caster.getLookAngle();
            proj.setPos(caster.getX(), caster.getEyeY() - 0.15, caster.getZ());
            proj.shoot(look.x, look.y, look.z, 1.4f, 0.4f);
            level.addFreshEntity(proj);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.1f, 1.1f);
            }
        }

        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float power = (float) getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.tenseicraft.technique_chant", "«Elementum Sphaera Iactus!»"),
                Component.translatable("ui.tenseicraft.tier_info", "Principiante - Híbrido"),
                Component.literal("§cDaño de Impacto: " + String.format(java.util.Locale.ROOT, "%.1f", power)),
                Component.literal("§6Radio de Explosión: 3.5m")
        );
    }
}
