package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class QuestHudClientEvents {

    private static String toastTitle = "";
    private static String toastProgress = "";
    private static boolean toastCompleted = false;
    private static long toastStartTime = 0L;

    public static void triggerToast(String title, String progressText, boolean isCompleted) {
        toastTitle = title;
        toastProgress = progressText;
        toastCompleted = isCompleted;
        toastStartTime = System.currentTimeMillis();

        if (isCompleted) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F));
        } else {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.2F, 0.6F));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        long elapsed = System.currentTimeMillis() - toastStartTime;
        if (elapsed >= 4000L || toastTitle.isEmpty()) {
            return;
        }

        float alpha = 1.0F;
        if (elapsed < 300L) {
            alpha = elapsed / 300.0F;
        } else if (elapsed > 3200L) {
            alpha = 1.0F - ((elapsed - 3200.0F) / 800.0F);
        }
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        int alphaInt = (int) (alpha * 255.0F);
        if (alphaInt <= 4) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int screenWidth = guiGraphics.guiWidth();
        int width = 196;
        int height = 28;
        int x = screenWidth - width - 10;
        int y = 10;

        // Dark Fantasy Card Background
        int bgAlpha = (int) (alpha * 230.0F) << 24;
        int borderAlpha = (int) (alpha * 255.0F) << 24;

        int bgColor = bgAlpha | 0x12141F;
        int borderColor = borderAlpha | (toastCompleted ? 0x55FF55 : 0xD4AF37);

        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        guiGraphics.renderOutline(x, y, width, height, borderColor);

        // Header Title
        String prefix = toastCompleted ? "§a★ " : "§6📜 ";
        String fullTitle = prefix + "§f" + toastTitle;
        if (font.width(fullTitle) > width - 12) {
            fullTitle = font.plainSubstrByWidth(fullTitle, width - 20) + "...";
        }
        guiGraphics.drawString(font, Component.literal(fullTitle), x + 6, y + 5, borderAlpha | 0xFFFFFF, false);

        // Progress Subtitle
        String subText;
        if (toastCompleted && "¡Completada!".equals(toastProgress)) {
            subText = "§a¡Misión Principal completada!";
        } else if (toastProgress != null && toastProgress.startsWith("§")) {
            subText = toastProgress;
        } else {
            subText = toastCompleted ? "§a¡Misión cumplida! Reclama tu premio" : "§eProgreso: §f" + toastProgress;
        }
        if (font.width(subText) > width - 12) {
            subText = font.plainSubstrByWidth(subText, width - 20) + "...";
        }
        guiGraphics.drawString(font, Component.literal(subText), x + 6, y + 16, borderAlpha | 0xAAAAAA, false);
    }
}
