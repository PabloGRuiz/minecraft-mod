package com.pablo.rpglevels.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Semillas de Vida: destilación pura de Energía Vital reunida por un Espíritu Protector.
 * Pueden plantarse en tierra de cultivo para hacer crecer la Flor de la Vida.
 */
public class LifeSeedsItem extends ItemNameBlockItem {

    public LifeSeedsItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.rpglevels.life_seeds.desc1").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable("item.rpglevels.life_seeds.desc2").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
