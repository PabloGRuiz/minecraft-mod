package com.pablo.rpglevels.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Pétalos Arcoíris: reliquia floral prismática cosechada de la Flor de la Vida madura.
 * Material base para futuros rituales y mecánicas espirituales.
 */
public class RainbowPetalsItem extends Item {

    public RainbowPetalsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.rpglevels.rainbow_petals.desc1").withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("item.rpglevels.rainbow_petals.desc2").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
    }
}
