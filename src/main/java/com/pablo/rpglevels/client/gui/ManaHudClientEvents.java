package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.pablo.rpglevels.config.RpgClientConfig;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class ManaHudClientEvents {

    private static final ResourceLocation MANA_ICONS =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/gui/mana_icons.png");

    private static float cachedCurrentMana = 20.0f;
    private static float cachedMaxMana = 20.0f;

    public static void updateMana(float current, float max) {
        cachedCurrentMana = current;
        cachedMaxMana = max;
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        if (!RpgClientConfig.MANA_BAR_ENABLED.get()) {
            return;
        }

        // Si Iron's Spells está instalado, se unifica utilizando exclusivamente su barra nativa de maná en el HUD
        if (com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        // Mostrar barra de gemas de maná solo si el jugador tiene Afinidad Espiritual
        PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasSkill("magic_affinity")) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        int offsetX = RpgClientConfig.MANA_BAR_X_OFFSET.get();
        int offsetY = RpgClientConfig.MANA_BAR_Y_OFFSET.get();

        int right = screenWidth / 2 + 91 + offsetX;

        boolean isSurvival = mc.gameMode != null && mc.gameMode.canHurtPlayer();
        int air = mc.player.getAirSupply();
        int maxAir = mc.player.getMaxAirSupply();
        boolean isAirShowing = isSurvival && air < maxAir;

        int manaTop;
        if (isSurvival) {
            manaTop = (isAirShowing ? (screenHeight - 59) : (screenHeight - 49)) + offsetY;
        } else {
            manaTop = (screenHeight - 40) + offsetY;
        }

        float maxMana = (data != null) ? data.getMaxMana(mc.player) : 100.0f;
        float currentMana = (data != null) ? data.getCurrentMana(mc.player) : maxMana;

        if (com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            float ironsMana = com.pablo.rpglevels.compat.irons.IronsSpellsCompat.getIronsMana(mc.player);
            if (ironsMana >= 0.0f) {
                currentMana = ironsMana;
            }
            float ironsMax = com.pablo.rpglevels.compat.irons.IronsSpellsCompat.getIronsMaxMana(mc.player);
            if (ironsMax > 0.0f) {
                maxMana = ironsMax;
            }
        }

        // Draw 10 Lapis Lazuli Gem Icons (Right to Left, matching Food drumsticks)
        float manaPerGem = maxMana / 10.0f;
        float halfGemValue = manaPerGem / 2.0f;

        for (int i = 0; i < 10; i++) {
            int x = right - (i * 8) - 9;

            // 1. Draw empty container
            guiGraphics.blit(MANA_ICONS, x, manaTop, 0, 0, 9, 9, 32, 32);

            // 2. Draw full or half gem
            float iconThreshold = (i + 1) * manaPerGem;
            float halfThreshold = (i * manaPerGem) + halfGemValue;

            if (currentMana >= iconThreshold - 0.05f) {
                // Full Lapis Lazuli Gem
                guiGraphics.blit(MANA_ICONS, x, manaTop, 18, 0, 9, 9, 32, 32);
            } else if (currentMana >= halfThreshold - 0.05f) {
                // Half Lapis Lazuli Gem
                guiGraphics.blit(MANA_ICONS, x, manaTop, 9, 0, 9, 9, 32, 32);
            }
        }

        // 3. Draw numerical text indicator to the left of the 10th icon
        int curInt = Math.round(currentMana);
        int maxInt = Math.round(maxMana);
        String text = "§9" + curInt + "§7/§9" + maxInt + " MP";
        int textX = right - (10 * 8) - 9 - mc.font.width(text) - 4;

        guiGraphics.drawString(mc.font, text, textX, manaTop + 1, 0xFF5C8CF5, true);
    }
}
