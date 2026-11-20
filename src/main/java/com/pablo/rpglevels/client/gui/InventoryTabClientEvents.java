package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.ref.WeakReference;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class InventoryTabClientEvents {

    private static WeakReference<Button> activeRpgTabButton = new WeakReference<>(null);

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen inventoryScreen) {
            int left = inventoryScreen.getGuiLeft();
            int top = inventoryScreen.getGuiTop();

            Component keyLabel = ModKeyMappings.OPEN_STATS_KEY.get().getTranslatedKeyMessage();
            // Botón posicionado encima del inventario para no solapar con el libro de recetas vanilla
            Button rpgTabButton = Button.builder(Component.literal("§6★ RPG"), button -> {
                Minecraft.getInstance().setScreen(new RpgStatsScreen(inventoryScreen));
            })
            .bounds(left + 118, top - 20, 58, 18)
            .tooltip(Tooltip.create(Component.translatable("rpglevels.gui.open_stats_tooltip")
                    .append(Component.literal("\n§7Tecla: §e[")).append(keyLabel).append(Component.literal("]"))))
            .build();

            activeRpgTabButton = new WeakReference<>(rpgTabButton);
            event.addListener(rpgTabButton);
        } else {
            activeRpgTabButton = new WeakReference<>(null);
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof InventoryScreen inventoryScreen) {
            Button btn = activeRpgTabButton.get();
            if (btn != null) {
                btn.setX(inventoryScreen.getGuiLeft() + 118);
                btn.setY(inventoryScreen.getGuiTop() - 20);
            }
        }
    }
}
