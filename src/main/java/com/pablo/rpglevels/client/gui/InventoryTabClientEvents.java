package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class InventoryTabClientEvents {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen inventoryScreen) {
            int left = inventoryScreen.getGuiLeft();
            int top = inventoryScreen.getGuiTop();

            // Button positioned as an RPG tab inside the player's inventory
            Button rpgTabButton = Button.builder(Component.literal("§6★ RPG"), button -> {
                Minecraft.getInstance().setScreen(new RpgStatsScreen(inventoryScreen));
            })
            .bounds(left + 118, top + 61, 52, 18)
            .tooltip(Tooltip.create(Component.translatable("rpglevels.gui.open_stats_tooltip")))
            .build();

            event.addListener(rpgTabButton);
        }
    }
}
