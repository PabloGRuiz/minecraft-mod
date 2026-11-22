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
public class SkillUnlockedToastClient {

    private static String currentSkillId = "";
    private static String currentSkillName = "";
    private static String currentSkillCategory = "";
    private static String currentDescription = "";
    private static long toastStartTime = 0L;

    public static void triggerSkillToast(String id, String name, String category, String description) {
        currentSkillId = id;
        currentSkillName = name;
        currentSkillCategory = category;
        currentDescription = description;
        toastStartTime = System.currentTimeMillis();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Majestic anime voice-of-the-world sequence sounds
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.0F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 0.7F, 1.2F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        long elapsed = System.currentTimeMillis() - toastStartTime;
        if (elapsed >= 5500L || currentSkillName.isEmpty()) {
            return;
        }

        float alpha = 1.0F;
        if (elapsed < 400L) {
            alpha = elapsed / 400.0F;
        } else if (elapsed > 4500L) {
            alpha = 1.0F - ((elapsed - 4500.0F) / 1000.0F);
        }
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        int alphaInt = (int) (alpha * 255.0F);
        if (alphaInt <= 5) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int screenWidth = guiGraphics.guiWidth();
        int width = 220;
        int height = 44;
        int x = (screenWidth - width) / 2;
        int y = 18;

        // Tensura Voice of the World Styling (Royal Gold & Deep Indigo)
        int bgAlpha = (int) (alpha * 240.0F) << 24;
        int borderAlpha = (int) (alpha * 255.0F) << 24;

        int bgColor = bgAlpha | 0x0D1022; // Deep astral indigo
        int borderColor = borderAlpha | 0xFFD700; // Royal gold
        int innerBorder = borderAlpha | 0x384277;

        // Background banner
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        guiGraphics.renderOutline(x, y, width, height, borderColor);
        guiGraphics.renderOutline(x + 2, y + 2, width - 4, height - 4, innerBorder);

        // Header: "« VOZ DEL MUNDO »"
        Component headerComp = Component.literal("« ").append(Component.translatable("tenseicraft.toast.voice_of_world")).append(" »");
        int headerWidth = font.width(headerComp);
        guiGraphics.drawString(font, headerComp, x + (width - headerWidth) / 2, y + 5, borderAlpha | 0xFFDF00, true);

        // Skill Name line: "✦ Habilidad: [Percepción de Rango] ✦"
        String skillLine = "✦ " + currentSkillCategory + ": " + currentSkillName + " ✦";
        int skillLineWidth = font.width(skillLine);
        guiGraphics.drawString(font, Component.literal(skillLine), x + (width - skillLineWidth) / 2, y + 17, borderAlpha | 0x55FFFF, false);

        // Description line
        String desc = currentDescription;
        if (font.width(desc) > width - 12) {
            desc = font.plainSubstrByWidth(desc, width - 16) + "...";
        }
        int descWidth = font.width(desc);
        guiGraphics.drawString(font, Component.literal(desc), x + (width - descWidth) / 2, y + 29, borderAlpha | 0xCCCCCC, false);
    }
}
