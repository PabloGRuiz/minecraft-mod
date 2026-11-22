package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.reforge.ReforgeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Aplica los atributos, efectos pasivos, efectos al impactar y tooltips de los ítems reforjados.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class ReforgeEvents {

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModAttributesHandler.updatePlayerAttributes(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 != 0) {
            return;
        }

        List<ItemStack> activeGear = ReforgeManager.getActiveEquippedStacks(player);
        if (activeGear.isEmpty()) {
            return;
        }

        int regenTier = 0;
        int speedTier = 0;
        int strengthTier = 0;
        int resistanceTier = 0;

        for (ItemStack stack : activeGear) {
            Map<ReforgeManager.EffectType, Integer> effects = ReforgeManager.getEffects(stack);
            regenTier = Math.max(regenTier, effects.getOrDefault(ReforgeManager.EffectType.REGENERATION, 0));
            speedTier = Math.max(speedTier, effects.getOrDefault(ReforgeManager.EffectType.SPEED, 0));
            strengthTier = Math.max(strengthTier, effects.getOrDefault(ReforgeManager.EffectType.STRENGTH, 0));
            resistanceTier = Math.max(resistanceTier, effects.getOrDefault(ReforgeManager.EffectType.RESISTANCE, 0));
        }

        if (regenTier > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, regenTier - 1, true, false, true));
        }
        if (speedTier > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, speedTier - 1, true, false, true));
        }
        if (strengthTier > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, strengthTier - 1, true, false, true));
        }
        if (resistanceTier > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, resistanceTier - 1, true, false, true));
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        boolean isWeaponHit = (event.getSource().getDirectEntity() == attacker)
                || (event.getSource().getDirectEntity() instanceof Projectile);
        if (!isWeaponHit) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty() || ReforgeManager.getReforgeRankOrdinal(weapon) < 0) {
            return;
        }

        Map<ReforgeManager.EffectType, Integer> effects = ReforgeManager.getEffects(weapon);
        if (effects.isEmpty()) {
            return;
        }

        LivingEntity target = event.getEntity();
        ServerLevel sl = attacker.serverLevel();

        int fireTier = effects.getOrDefault(ReforgeManager.EffectType.FIRE_ASPECT, 0);
        if (fireTier > 0) {
            target.igniteForSeconds(3.0f * fireTier);
            event.setNewDamage(event.getNewDamage() + 1.0f * fireTier);
            sl.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 1.0, target.getZ(),
                    6 * fireTier, 0.25, 0.35, 0.25, 0.03);
        }

        int frostTier = effects.getOrDefault(ReforgeManager.EffectType.FROSTBITE, 0);
        if (frostTier > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 * frostTier, frostTier - 1));
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 50 * frostTier, 240));
            sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1.0, target.getZ(),
                    6 * frostTier, 0.25, 0.35, 0.25, 0.03);
        }

        int lightningTier = effects.getOrDefault(ReforgeManager.EffectType.LIGHTNING, 0);
        if (lightningTier > 0) {
            float procChance = 0.20f + 0.10f * lightningTier;
            if (attacker.getRandom().nextFloat() < procChance) {
                event.setNewDamage(event.getNewDamage() + 2.5f * lightningTier);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
                sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7f, 1.5f);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(),
                        12, 0.35, 0.4, 0.35, 0.08);
            }
        }

        int venomTier = effects.getOrDefault(ReforgeManager.EffectType.VENOM, 0);
        if (venomTier > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 60 * venomTier, venomTier - 1));
            sl.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1.0, target.getZ(),
                    6 * venomTier, 0.25, 0.35, 0.25, 0.03);
        }

        int lifestealTier = effects.getOrDefault(ReforgeManager.EffectType.LIFESTEAL, 0);
        if (lifestealTier > 0) {
            float heal = Math.max(0.5f, event.getNewDamage() * (0.06f * lifestealTier));
            attacker.heal(heal);
            sl.sendParticles(ParticleTypes.HEART, attacker.getX(), attacker.getY() + 1.6, attacker.getZ(),
                    2, 0.25, 0.25, 0.25, 0.02);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ThreatLevel rank = ReforgeManager.getReforgeRank(stack);
        if (rank == null) {
            return;
        }

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("§6⚒ Grado de Reforja: ").append(rank.getBadge()));

        double dmg = ReforgeManager.getStat(stack, ReforgeManager.TAG_DMG);
        double atkSpd = ReforgeManager.getStat(stack, ReforgeManager.TAG_ATK_SPD);
        double hp = ReforgeManager.getStat(stack, ReforgeManager.TAG_HP);
        double armor = ReforgeManager.getStat(stack, ReforgeManager.TAG_ARMOR);
        double speed = ReforgeManager.getStat(stack, ReforgeManager.TAG_SPEED);
        double luck = ReforgeManager.getStat(stack, ReforgeManager.TAG_LUCK);

        if (dmg > 0.0) tooltip.add(Component.literal("§c  • +" + fmt(dmg) + " Daño de Ataque"));
        if (atkSpd > 0.0) tooltip.add(Component.literal("§e  • +" + fmt(atkSpd * 100.0) + "% Velocidad de Ataque"));
        if (hp > 0.0) tooltip.add(Component.literal("§d  • +" + fmt(hp) + " Vida Máxima"));
        if (armor > 0.0) tooltip.add(Component.literal("§b  • +" + fmt(armor) + " Armadura"));
        if (speed > 0.0) tooltip.add(Component.literal("§a  • +" + fmt(speed * 100.0) + "% Velocidad de Movimiento"));
        if (luck > 0.0) tooltip.add(Component.literal("§a  • +" + fmt(luck) + " Suerte"));

        Map<ReforgeManager.EffectType, Integer> effects = ReforgeManager.getEffects(stack);
        if (!effects.isEmpty()) {
            tooltip.add(Component.literal("§5✦ Efectos de Alma:").withStyle(ChatFormatting.BOLD));
            for (Map.Entry<ReforgeManager.EffectType, Integer> entry : effects.entrySet()) {
                ReforgeManager.EffectType type = entry.getKey();
                tooltip.add(Component.literal("  • " + type.getColor() + type.getDisplayName()
                        + " " + ReforgeManager.roman(entry.getValue())));
            }
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
