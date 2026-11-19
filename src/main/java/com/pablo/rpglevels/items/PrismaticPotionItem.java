package com.pablo.rpglevels.items;

import com.pablo.rpglevels.init.ModMobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

/**
 * Sistema de Pociones Prismáticas (envase cristalino prismático):
 * - BASE: Poción Prismática (equivalente alquímico prismático de la Poción Rara).
 * - STRENGTH: Fuerza Prismática (+20% / +40% Daño de Ataque multiplicativo total).
 * - SWIFTNESS: Rapidez Prismática (+20% / +40% Velocidad de Movimiento multiplicativa total).
 * - HEALING: Curación Prismática (cura al instante 20% / 40% de la Vida Máxima total).
 * Al consumirse, el frasco vuelve a ser un frasco de cristal vacío común (minecraft:glass_bottle).
 */
public class PrismaticPotionItem extends Item {

    public enum PrismaticType {
        BASE,
        STRENGTH,
        SWIFTNESS,
        HEALING
    }

    private static final String TAG_TIER = "PrismaticTier";
    private static final String TAG_EXTENDED = "PrismaticExtended";

    private static final int BASE_DURATION_TICKS = 3600;     // 3:00
    private static final int EXTENDED_DURATION_TICKS = 9600; // 8:00
    private static final int TIER2_DURATION_TICKS = 1800;    // 1:30

    private final PrismaticType prismaticType;

    public PrismaticPotionItem(PrismaticType prismaticType, Properties properties) {
        super(properties);
        this.prismaticType = prismaticType;
    }

    public PrismaticType getPrismaticType() {
        return prismaticType;
    }

    public static int getTier(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return Math.max(0, Math.min(1, customData.copyTag().getInt(TAG_TIER)));
    }

    public static boolean isExtended(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        return customData.copyTag().getBoolean(TAG_EXTENDED);
    }

    public static ItemStack withTier(ItemStack stack, int tier) {
        ItemStack copy = stack.copy();
        int clamped = Math.max(0, Math.min(1, tier));
        CustomData.update(DataComponents.CUSTOM_DATA, copy, tag -> {
            tag.putInt(TAG_TIER, clamped);
            if (clamped > 0) {
                tag.putBoolean(TAG_EXTENDED, false);
            }
        });
        return copy;
    }

    public static ItemStack withExtended(ItemStack stack, boolean extended) {
        ItemStack copy = stack.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, copy, tag -> {
            tag.putBoolean(TAG_EXTENDED, extended);
            if (extended) {
                tag.putInt(TAG_TIER, 0);
            }
        });
        return copy;
    }

    @Override
    public Component getName(ItemStack stack) {
        Component baseName = super.getName(stack);
        if (prismaticType != PrismaticType.BASE && getTier(stack) > 0) {
            return Component.empty().append(baseName).append(" II");
        }
        return baseName;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            CriteriaTriggers.CONSUME_ITEM.trigger(sp, stack);
        }

        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            int tier = getTier(stack);
            boolean extended = isExtended(stack);
            int duration = tier > 0 ? TIER2_DURATION_TICKS
                    : (extended ? EXTENDED_DURATION_TICKS : BASE_DURATION_TICKS);

            switch (prismaticType) {
                case BASE -> {
                    sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.5F);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                            8, 0.25D, 0.35D, 0.25D, 0.03D);
                    if (entity instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal(
                                "§d✦ Poción Prismática (Sin efecto): úsala en un Soporte para Pociones."), true);
                    }
                }
                case STRENGTH -> {
                    entity.addEffect(new MobEffectInstance(ModMobEffects.PRISMATIC_STRENGTH, duration, tier));
                    sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.2F);
                    sl.sendParticles(ParticleTypes.FLAME,
                            entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                            14, 0.35D, 0.4D, 0.35D, 0.04D);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                            10, 0.3D, 0.4D, 0.3D, 0.05D);
                }
                case SWIFTNESS -> {
                    entity.addEffect(new MobEffectInstance(ModMobEffects.PRISMATIC_SWIFTNESS, duration, tier));
                    sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.6F);
                    sl.sendParticles(ParticleTypes.CLOUD,
                            entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                            14, 0.35D, 0.25D, 0.35D, 0.05D);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                            10, 0.3D, 0.4D, 0.3D, 0.05D);
                }
                case HEALING -> {
                    float pct = tier > 0 ? 0.40F : 0.20F;
                    float healAmount = Math.max(2.0F, entity.getMaxHealth() * pct);
                    entity.heal(healAmount);
                    sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                    sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.5F);
                    sl.sendParticles(ParticleTypes.HEART,
                            entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                            10, 0.4D, 0.4D, 0.4D, 0.06D);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                            15, 0.45D, 0.45D, 0.45D, 0.05D);
                    sl.sendParticles(ParticleTypes.END_ROD,
                            entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                            10, 0.35D, 0.45D, 0.35D, 0.05D);
                    if (entity instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal(
                                "§d✦ ¡Curación Prismática! §a+" + String.format(Locale.ROOT, "%.1f", healAmount)
                                        + " HP §7(" + Math.round(pct * 100.0F) + "% Vida Máx.)"), true);
                    }
                }
            }
        }

        if (entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            stack.consume(1, player);
            if (!player.hasInfiniteMaterials()) {
                if (stack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
                if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                    player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
                }
            }
        } else {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        int tier = getTier(stack);
        boolean extended = isExtended(stack);
        int percentBonus = tier > 0 ? 40 : 20;
        String durationStr = tier > 0 ? "01:30" : (extended ? "08:00" : "03:00");

        switch (prismaticType) {
            case BASE -> {
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.desc1").withStyle(ChatFormatting.LIGHT_PURPLE));
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.desc2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            case STRENGTH -> {
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.effect_strength", percentBonus).withStyle(ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.duration_fmt", durationStr).withStyle(ChatFormatting.AQUA));
            }
            case SWIFTNESS -> {
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.effect_swiftness", percentBonus).withStyle(ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.duration_fmt", durationStr).withStyle(ChatFormatting.BLUE));
            }
            case HEALING -> {
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.effect_healing", percentBonus).withStyle(ChatFormatting.GREEN));
                tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.instant_fmt").withStyle(ChatFormatting.GOLD));
            }
        }
        tooltipComponents.add(Component.translatable("item.rpglevels.prismatic_potion.bottle_return").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
