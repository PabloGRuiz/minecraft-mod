package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.events.BeamClashCameraClientEvents;
import com.pablo.rpglevels.compat.irons.IronsSpellsCompat;
import com.pablo.rpglevels.magic.beam.BeamClashManager;
import com.pablo.rpglevels.network.BeamClickSpamPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * HUD cinemático y controles interactivos del Choque de Poderes (Beam Clash):
 * - Formato Letterbox 21:9 con barras negras cinemáticas automáticas.
 * - Título dramático con subtítulo motivacional.
 * - Barra de Tira y Afloja (Tug-of-War) con chispa de colisión viva y vibrante.
 * - Feedback táctil con sacudida de cámara e impacto sonoro al spamear clic.
 * - Pantalla de resolución cinemática en cámara lenta (Aftermath de Victoria o Derrota).
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class BeamClashHudClientEvents {

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (BeamClashManager.isPlayerInClash(mc.player.getId())) {
            // Clic Izquierdo presionado durante el forcejeo
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
                PacketDistributor.sendToServer(new BeamClickSpamPayload());

                // Impulso de sacudida cinemática a la cámara
                BeamClashCameraClientEvents.registerClickImpulse();

                // Feedback sonoro táctil inmediato
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_ATTACK_CRIT, 1.7F, 0.45F));
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.9F, 0.35F));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        boolean inClash = BeamClashManager.isPlayerInClash(mc.player.getId());
        boolean inAftermath = BeamClashCameraClientEvents.isAftermathActive();

        if (!inClash && !inAftermath) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        // 1. Barras negras panorámicas 21:9 (Letterbox)
        float maxBarH = screenH * 0.12F;
        float curBarH = maxBarH;
        if (inClash) {
            int duration = BeamClashCameraClientEvents.getClashDurationTicks();
            curBarH = maxBarH * Math.min(1.0F, duration / 8.0F);
        } else if (inAftermath) {
            float rem = BeamClashCameraClientEvents.getAftermathTicks() / (float) BeamClashCameraClientEvents.getAftermathTotalTicks();
            curBarH = maxBarH * Math.min(1.0F, rem * 1.4F);
        }

        int iBarH = Math.round(curBarH);
        if (iBarH > 0) {
            g.fill(0, 0, screenW, iBarH, 0xFF000000);
            g.fill(0, screenH - iBarH, screenW, screenH, 0xFF000000);
        }

        // 2. Pantalla de Desenlace (Aftermath)
        if (inAftermath && !inClash) {
            boolean won = BeamClashCameraClientEvents.isLocalWinner();
            String resTitle = won ? "✦ ¡VICTORIA APLASTANTE! ✦" : "✦ ¡DEFENSA QUEBRADA! ✦";
            String resSub = won ? "« Tu haz de energía ha pulverizado la defensa rival »"
                    : "« Has sido superado por la energía espiritual enemiga... »";

            int titleColor = won ? 0xFFFFD700 : 0xFFFF3333;
            int subColor = won ? 0xFFFFFFA0 : 0xFFB0B0B0;

            int titleW = font.width(resTitle);
            int subW = font.width(resSub);
            int centerY = screenH / 2 - 14;

            // Resplandor translúcido de fondo
            int bannerW = Math.max(titleW, subW) + 40;
            g.fill((screenW - bannerW) / 2, centerY - 8, (screenW + bannerW) / 2, centerY + 30, 0x88000000);

            g.drawString(font, resTitle, (screenW - titleW) / 2, centerY, titleColor, true);
            g.drawString(font, resSub, (screenW - subW) / 2, centerY + 16, subColor, true);
            return;
        }

        // 3. Choque en progreso: HUD interactivo de puja
        var state = BeamClashManager.CLIENT_STATE;
        boolean isA = (mc.player.getId() == state.entityAId);

        float myScore = isA ? state.scoreA : state.scoreB;
        float oppScore = isA ? state.scoreB : state.scoreA;
        int myColor = isA ? state.colorA : state.colorB;
        int oppColor = isA ? state.colorB : state.colorA;

        float playerProgress = isA ? state.progress : (1.0F - state.progress);

        int barW = 270;
        int barH = 14;
        int barX = (screenW - barW) / 2;
        int barY = Math.max(34, iBarH + 14);

        // Título cinematográfico en la parte superior
        String clashTitle = "✦ CHOQUE DE PODERES ESPIRITUALES ✦";
        int titleW = font.width(clashTitle);
        g.drawString(font, clashTitle, (screenW - titleW) / 2, barY - 24, 0xFFFFD700, true);

        // Marco y fondo de la barra
        g.fill(barX - 4, barY - 4, barX + barW + 4, barY + barH + 4, 0xCC05070D);
        g.fill(barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, 0xFF181C26);
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);

        // Segmento Izquierdo: Jugador
        int fillW = (int) (barW * Mth.clamp(playerProgress, 0.02F, 0.98F));
        int r1 = (myColor >> 16) & 0xFF;
        int g1 = (myColor >> 8) & 0xFF;
        int b1 = myColor & 0xFF;
        int packedMyColor = 0xFF000000 | (r1 << 16) | (g1 << 8) | b1;
        g.fill(barX, barY, barX + fillW, barY + barH, packedMyColor);

        // Segmento Derecho: Oponente
        int r2 = (oppColor >> 16) & 0xFF;
        int g2 = (oppColor >> 8) & 0xFF;
        int b2 = oppColor & 0xFF;
        int packedOppColor = 0xFF000000 | (r2 << 16) | (g2 << 8) | b2;
        g.fill(barX + fillW, barY, barX + barW, barY + barH, packedOppColor);

        // Marcador y chispas en el punto de colisión exacta
        long gameTime = mc.level.getGameTime();
        int jitterX = (int) (Math.sin(gameTime * 0.9) * 2.0);
        g.fill(barX + fillW - 2 + jitterX, barY - 3, barX + fillW + 2 + jitterX, barY + barH + 3, 0xFFFFFFFF);
        g.fill(barX + fillW - 4 + jitterX, barY, barX + fillW + 4 + jitterX, barY + barH, 0xFFFFFF55);

        // Nombres y Puntos de Ambos Combatientes
        String leftLabel = "§b§lTÚ: §f" + (int) myScore + " pts";
        g.drawString(font, leftLabel, barX, barY - 11, 0xFFFFFFFF, true);

        Entity opp = mc.level.getEntity(isA ? state.entityBId : state.entityAId);
        String oppName = (opp != null) ? opp.getDisplayName().getString() : "Oponente";
        String rightLabel = "§c§l" + oppName + ": §f" + (int) oppScore + " pts";
        int rightW = font.width(rightLabel);
        g.drawString(font, rightLabel, barX + barW - rightW, barY - 11, 0xFFFFFFFF, true);

        // Temporizador Central
        float secondsLeft = Math.max(0.0F, state.remainingTicks / 20.0F);
        String timeStr = String.format(Locale.ROOT, "⏱ %.1fs", secondsLeft);
        int timeW = font.width(timeStr);
        g.drawString(font, timeStr, (screenW - timeW) / 2, barY - 11, 0xFFFFDD44, true);

        // Multiplicador según Reserva de Maná
        float currentMana = Math.max(0.0F, IronsSpellsCompat.getIronsMana(mc.player));
        float multiplier = 1.0F + (currentMana / 100.0F);
        String manaInfo = String.format(Locale.ROOT, "§7Reserva de Maná: §b%d §7| Multiplicador de Clic: §e§lx%.2f",
                (int) currentMana, multiplier);
        int manaW = font.width(manaInfo);
        g.drawString(font, manaInfo, (screenW - manaW) / 2, barY + barH + 6, 0xFFFFFFFF, true);

        // Mensaje Parpadeante de Acción
        float pulse = (Mth.sin(gameTime * 0.5F) + 1.0F) * 0.5F;
        int promptColor = (pulse > 0.5F) ? 0xFFFF3333 : 0xFFFFCC00;
        String prompt = "⚡ ¡SPAMEA CLIC IZQUIERDO! ⚡";
        int promptW = font.width(prompt);
        g.drawString(font, prompt, (screenW - promptW) / 2, barY + barH + 20, promptColor, true);
    }
}
