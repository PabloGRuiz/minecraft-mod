package com.pablo.tenseicraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class AncientRingItem extends Item implements ICurioItem {

    public static final int XP_REQ_TIER_1 = 160;
    public static final int XP_REQ_TIER_2 = 550;
    public static final int XP_REQ_TIER_3 = 2920;

    public AncientRingItem(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("deprecation")
    public static int getTier(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.getUnsafe().getInt("AncientRingTier");
        }
        return 0;
    }

    public static void setTier(ItemStack stack, int tier) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt("AncientRingTier", Math.min(3, Math.max(0, tier)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @SuppressWarnings("deprecation")
    public static int getStoredXp(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.getUnsafe().getInt("StoredXp");
        }
        return 0;
    }

    public static void setStoredXp(ItemStack stack, int xp) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt("StoredXp", Math.max(0, xp));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void addStoredXp(ItemStack stack, int amount) {
        setStoredXp(stack, getStoredXp(stack) + amount);
    }

    public static boolean isReadyToEvolve(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            return customData.copyTag().getBoolean("ReadyToEvolve");
        }
        return false;
    }

    public static void setReadyToEvolve(ItemStack stack, boolean ready) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putBoolean("ReadyToEvolve", ready);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getRequiredXpForCurrentTier(int tier) {
        return switch (tier) {
            case 0 -> XP_REQ_TIER_1;
            case 1 -> XP_REQ_TIER_2;
            case 2 -> XP_REQ_TIER_3;
            default -> Integer.MAX_VALUE;
        };
    }

    public static int getDexterityAndLuckBonus(ItemStack stack) {
        int tier = getTier(stack);
        return switch (tier) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 0;
        };
    }

    public static float getLifestealFraction(ItemStack stack) {
        int tier = getTier(stack);
        return switch (tier) {
            case 1 -> 0.05f;
            case 2 -> 0.08f;
            case 3 -> 0.12f;
            default -> 0.03f;
        };
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getTier(stack) >= 1 || isReadyToEvolve(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        int tier = getTier(stack);
        MutableComponent baseName = Component.translatable("item.tenseicraft.ancient_ring");

        return switch (tier) {
            case 1 -> Component.translatable("tenseicraft.tier.common").append(" ").append(baseName).withStyle(ChatFormatting.GREEN);
            case 2 -> Component.translatable("tenseicraft.tier.rare").append(" ").append(baseName).withStyle(ChatFormatting.BLUE);
            case 3 -> Component.translatable("tenseicraft.tier.legendary").append(" ").append(baseName).withStyle(ChatFormatting.GOLD);
            default -> baseName.withStyle(ChatFormatting.GRAY);
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        int tier = getTier(stack);
        int stored = getStoredXp(stack);
        int req = getRequiredXpForCurrentTier(tier);
        int statBonus = getDexterityAndLuckBonus(stack);
        int lifestealPct = Math.round(getLifestealFraction(stack) * 100.0f);

        tooltipComponents.add(Component.translatable("item.tenseicraft.ancient_ring.desc").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tenseicraft.ancient_ring.lifesteal", lifestealPct).withStyle(ChatFormatting.RED));

        if (statBonus > 0) {
            tooltipComponents.add(Component.translatable("tenseicraft.ancient_ring.stat_boost", statBonus, statBonus).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }

        if (isReadyToEvolve(stack)) {
            tooltipComponents.add(Component.translatable("tenseicraft.amulet.ready_to_evolve").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (tier < 3) {
            tooltipComponents.add(Component.translatable("tenseicraft.amulet.progress", stored, req).withStyle(ChatFormatting.AQUA));
        } else {
            tooltipComponents.add(Component.translatable("tenseicraft.amulet.max_tier").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        tooltipComponents.add(Component.translatable("item.tenseicraft.curio.slot_ring").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
