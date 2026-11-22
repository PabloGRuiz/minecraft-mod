package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.party.ClientPartyData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class RpgXpHudClientEvents {

    private static final int MAX_DISPLAY_TICKS = 90; // ~4.5 seconds
    private static int displayTicks = 0;
    private static int gainedXp = 0;
    private static int currentXp = 0;
    private static int maxXp = 100;
    private static int currentLevel = 1;
    private static boolean isAtCap = false;

    private static float visualProgress = 0.0f;
    private static float targetProgress = 0.0f;

    public static void onXpGainReceived(int amount, int curXp, int reqXp, int level, boolean atCap) {
        if (displayTicks > 0) {
            gainedXp += amount;
        } else {
            gainedXp = amount;
            float prevFraction = (reqXp > 0) ? Math.max(0.0f, (float)(curXp - amount) / (float) reqXp) : 0.0f;
            visualProgress = Math.min(1.0f, Math.max(0.0f, prevFraction));
        }

        currentXp = curXp;
        maxXp = Math.max(1, reqXp);
        currentLevel = level;
        isAtCap = atCap;
        targetProgress = isAtCap ? 1.0f : Math.min(1.0f, (float) currentXp / (float) maxXp);
        displayTicks = MAX_DISPLAY_TICKS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (displayTicks > 0) {
            displayTicks--;
            if (displayTicks == 0) {
                gainedXp = 0;
            }
        }

        // Smooth visual bar progression
        visualProgress += (targetProgress - visualProgress) * 0.18f;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        if (displayTicks <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        // Alpha calculation: smooth ease-in (15 ticks) and ease-out (20 ticks)
        float alpha;
        if (displayTicks > (MAX_DISPLAY_TICKS - 15)) {
            alpha = (float) (MAX_DISPLAY_TICKS - displayTicks) / 15.0f;
        } else if (displayTicks < 20) {
            alpha = (float) displayTicks / 20.0f;
        } else {
            alpha = 1.0f;
        }
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        if (alpha < 0.04f) {
            return;
        }

        // Smart dynamic placement: Below Party HUD if in a party, or top-left corner if solo
        int startX = 10;
        int startY = 10;

        int partyBottom = PartyHudClientEvents.getPartyHudBottom();
        if (partyBottom > 0) {
            startY = partyBottom + 6;
        } else if (ClientPartyData.isInParty() && !ClientPartyData.getMembers().isEmpty()) {
            float hudScale = (mc.getWindow().getGuiScaledHeight() < 360 || mc.getWindow().getGuiScaledWidth() < 640) ? 0.65f : 0.80f;
            int membersCount = ClientPartyData.getMembers().size();
            startY = (int) ((44 + 14 + (membersCount * 33)) * hudScale) + 6;
        }

        // Screen boundary protection
        int screenH = mc.getWindow().getGuiScaledHeight();
        if (startY + 32 > screenH - 25) {
            startY = Math.max(10, screenH - 55);
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;

        int frameWidth = 140;
        int frameHeight = 26;

        // Background box
        guiGraphics.fill(startX, startY, startX + frameWidth, startY + frameHeight, withAlpha(0xEE0E101A, alpha));

        // Border outline
        int borderColor = isAtCap ? 0xFFDA70D6 : 0xFFE5A93C;
        guiGraphics.renderOutline(startX, startY, frameWidth, frameHeight, withAlpha(borderColor, alpha));

        // 1. Header Row: Level + XP gained pop
        String levelTitle = isAtCap ? "§d⚡ Nivel " + currentLevel + " [HITO]" : "§6✦ Nivel " + currentLevel;
        guiGraphics.drawString(font, Component.literal(levelTitle), startX + 4, startY + 2, withAlpha(0xFFFFAA00, alpha), false);

        String gainText = "§a+" + gainedXp + " XP";
        int gainW = font.width(gainText);
        guiGraphics.drawString(font, Component.literal(gainText), startX + frameWidth - 4 - gainW, startY + 2, withAlpha(0xFF55FF55, alpha), false);

        // 2. Middle Row: Progress Bar Track
        int barX = startX + 4;
        int barY = startY + 12;
        int barW = frameWidth - 8;
        int barH = 3;

        guiGraphics.fill(barX, barY, barX + barW, barY + barH, withAlpha(0xFF141724, alpha));
        guiGraphics.renderOutline(barX, barY, barW, barH, withAlpha(0xFF2B3248, alpha));

        // Progress Bar Fill
        float clampedProgress = Math.max(0.0f, Math.min(1.0f, visualProgress));
        int fillW = (int) (barW * clampedProgress);
        if (fillW > 0) {
            int barColor = isAtCap ? 0xFFDA70D6 : (clampedProgress >= 0.75f ? 0xFF2ECC71 : 0xFF00D2D3);
            guiGraphics.fill(barX, barY, barX + fillW, barY + barH, withAlpha(barColor, alpha));
            guiGraphics.fill(barX, barY, barX + fillW, barY + 1, withAlpha(0xFFE8FFE8, alpha * 0.75f));
        }

        // 3. Bottom Row: Numerical Progression or Milestone Ascension prompt
        String bottomText = isAtCap
                ? "§d⚡ Duerme para Ascender"
                : String.format("§7%d / %d XP §8(§e%d%%§8)", currentXp, maxXp, (int) (clampedProgress * 100));
        int bottomW = font.width(bottomText);
        int bottomX = startX + (frameWidth - bottomW) / 2;
        guiGraphics.drawString(font, Component.literal(bottomText), bottomX, startY + 16, withAlpha(0xFFAAAAAA, alpha), false);
    }

    private static int withAlpha(int argb, float alpha) {
        int a = (int) (((argb >> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0x00FFFFFF);
    }
}
