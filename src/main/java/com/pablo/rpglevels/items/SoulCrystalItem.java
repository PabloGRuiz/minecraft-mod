package com.pablo.rpglevels.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SoulCrystalItem extends Item {

    public enum Tier {
        LESSER(1, "lesser", ChatFormatting.AQUA, Rarity.COMMON, false),
        RESONANT(2, "resonant", ChatFormatting.LIGHT_PURPLE, Rarity.UNCOMMON, false),
        RADIANT(3, "radiant", ChatFormatting.GOLD, Rarity.RARE, false),
        PRIMORDIAL(4, "primordial", ChatFormatting.RED, Rarity.EPIC, true);

        private final int level;
        private final String key;
        private final ChatFormatting color;
        private final Rarity rarity;
        private final boolean foil;

        Tier(int level, String key, ChatFormatting color, Rarity rarity, boolean foil) {
            this.level = level;
            this.key = key;
            this.color = color;
            this.rarity = rarity;
            this.foil = foil;
        }

        public int getLevel() {
            return level;
        }

        public String getKey() {
            return key;
        }

        public ChatFormatting getColor() {
            return color;
        }

        public Rarity getRarity() {
            return rarity;
        }

        public boolean isFoil() {
            return foil;
        }
    }

    private final Tier tier;

    public SoulCrystalItem(Tier tier) {
        super(new Properties().stacksTo(64).rarity(tier.getRarity()));
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this.tier.isFoil() || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Insignia de Grado
        Component tierBadge = switch (tier) {
            case LESSER -> Component.literal("[Grado I: Alma Menor]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
            case RESONANT -> Component.literal("[Grado II: Alma Resonante]").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
            case RADIANT -> Component.literal("[Grado III: Alma Radiante]").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            case PRIMORDIAL -> Component.literal("[Grado IV: Alma Primordial]").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        };
        tooltipComponents.add(tierBadge);

        // Descripción de Lore
        String descKey = "item.rpglevels." + tier.getKey() + "_soul_crystal.desc";
        tooltipComponents.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));

        tooltipComponents.add(Component.empty());

        // Propósito / Lore del Gremio
        tooltipComponents.add(Component.literal("✦ Material de Reforja del Gremio").withStyle(ChatFormatting.DARK_AQUA));
        tooltipComponents.add(Component.literal("  Permite al ingenio humano forjar equipo").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal("  capaz de rivalizar con entidades espirituales.").withStyle(ChatFormatting.DARK_GRAY));

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
