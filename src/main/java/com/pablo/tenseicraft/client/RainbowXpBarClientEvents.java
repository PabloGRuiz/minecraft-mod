package com.pablo.tenseicraft.client;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.awt.Color;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class RainbowXpBarClientEvents {

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying()) {
            return;
        }

        int level = mc.player.experienceLevel;
        if (level < 100) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        // Animated rainbow color cycling through HSB
        float timeHue = (float) ((System.currentTimeMillis() % 2500L) / 2500.0);
        int rainbowRgb = Color.HSBtoRGB(timeHue, 0.85f, 1.0f);

        // 1. Rainbow Level Number
        if (event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            String levelStr = String.valueOf(level);
            int x = (screenWidth - mc.font.width(levelStr)) / 2;
            int y = screenHeight - 31 - 4;

            // Draw animated chroma level number
            guiGraphics.drawString(mc.font, levelStr, x, y, rainbowRgb, false);
        }

        // 2. Shimmering Rainbow Experience Bar Overlay
        if (event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)) {
            int barX = (screenWidth - 182) / 2;
            int barY = screenHeight - 29;
            int nextHueRgb = Color.HSBtoRGB((timeHue + 0.25f) % 1.0f, 0.85f, 1.0f);

            int startColor = (rainbowRgb & 0x00FFFFFF) | 0x66000000;
            int endColor = (nextHueRgb & 0x00FFFFFF) | 0x66000000;

            guiGraphics.fillGradient(barX, barY, barX + 182, barY + 5, startColor, endColor);
        }
    }
}
