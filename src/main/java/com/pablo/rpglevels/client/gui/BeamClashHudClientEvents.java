package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.compat.irons.IronsSpellsCompat;
import com.pablo.rpglevels.magic.beam.BeamClashManager;
import com.pablo.rpglevels.network.BeamClickSpamPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
 * HUD y controles interactivos del Choque de Poderes (Beam Clash).
 * Dibuja la barra de tira y afloja (Tug-of-War), el temporizador, el multiplicador según reserva de maná
 * y el cartel parpadeante para spamear Clic Izquierdo.
 * Intercepta el clic izquierdo para enviar la puja al servidor.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class BeamClashHudClientEvents {

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (BeamClashManager.isPlayerInClash(mc.player.getId())) {
            // Clic Izquierdo presionado
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.getAction() == GLFW.GLFW_PRESS) {
                PacketDistributor.sendToServer(new BeamClickSpamPayload());

                // Feedback sonoro táctil inmediato en cliente
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.8F, 0.4F));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        if (!BeamClashManager.isPlayerInClash(mc.player.getId())) return;

        var state = BeamClashManager.CLIENT_STATE;
        boolean isA = (mc.player.getId() == state.entityAId);

        float myScore = isA ? state.scoreA : state.scoreB;
        float oppScore = isA ? state.scoreB : state.scoreA;
        int myColor = isA ? state.colorA : state.colorB;
        int oppColor = isA ? state.colorB : state.colorA;

        // Progreso relativo al jugador local: >0.5 significa que vamos ganando (empujando hacia la derecha)
        float playerProgress = isA ? state.progress : (1.0F - state.progress);

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = g.guiWidth();

        int barW = 260;
        int barH = 14;
        int barX = (screenW - barW) / 2;
        int barY = 28;

        // 1. Fondo estilizado con marco
        g.fill(barX - 4, barY - 4, barX + barW + 4, barY + barH + 4, 0xCC05070D);
        g.fill(barX - 2, barY - 2, barX + barW + 2, barY + barH + 2, 0xFF181C26);
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);

        // 2. Segmento de barra de Poder del Jugador (Izquierda)
        int fillW = (int) (barW * Mth.clamp(playerProgress, 0.02F, 0.98F));
        int r1 = (myColor >> 16) & 0xFF;
        int g1 = (myColor >> 8) & 0xFF;
        int b1 = myColor & 0xFF;
        int packedMyColor = 0xFF000000 | (r1 << 16) | (g1 << 8) | b1;
        g.fill(barX, barY, barX + fillW, barY + barH, packedMyColor);

        // 3. Segmento de barra de Poder del Oponente (Derecha)
        int r2 = (oppColor >> 16) & 0xFF;
        int g2 = (oppColor >> 8) & 0xFF;
        int b2 = oppColor & 0xFF;
        int packedOppColor = 0xFF000000 | (r2 << 16) | (g2 << 8) | b2;
        g.fill(barX + fillW, barY, barX + barW, barY + barH, packedOppColor);

        // 4. Marcador de Choque de Energía (Chispas en la colisión)
        g.fill(barX + fillW - 2, barY - 2, barX + fillW + 2, barY + barH + 2, 0xFFFFFFFF);
        g.fill(barX + fillW - 4, barY + 1, barX + fillW + 4, barY + barH - 1, 0xFFFFFF66);

        // 5. Nombre y Puntos de Ambos Combatientes
        String leftLabel = "§b§lTÚ: §f" + (int) myScore + " pts";
        g.drawString(font, leftLabel, barX, barY - 12, 0xFFFFFFFF, true);

        Entity opp = mc.level.getEntity(isA ? state.entityBId : state.entityAId);
        String oppName = (opp != null) ? opp.getDisplayName().getString() : "Oponente";
        String rightLabel = "§c§l" + oppName + ": §f" + (int) oppScore + " pts";
        int rightW = font.width(rightLabel);
        g.drawString(font, rightLabel, barX + barW - rightW, barY - 12, 0xFFFFFFFF, true);

        // 6. Temporizador Central en la barra
        float secondsLeft = Math.max(0.0F, state.remainingTicks / 20.0F);
        String timeStr = String.format(Locale.ROOT, "⏱ %.1fs", secondsLeft);
        int timeW = font.width(timeStr);
        g.drawString(font, timeStr, (screenW - timeW) / 2, barY - 12, 0xFFFFDD44, true);

        // 7. Multiplicador según Reserva de Maná
        float currentMana = Math.max(0.0F, IronsSpellsCompat.getIronsMana(mc.player));
        float multiplier = 1.0F + (currentMana / 100.0F);
        String manaInfo = String.format(Locale.ROOT, "§7Reserva de Maná: §b%d §7| Multiplicador de Clic: §e§lx%.2f",
                (int) currentMana, multiplier);
        int manaW = font.width(manaInfo);
        g.drawString(font, manaInfo, (screenW - manaW) / 2, barY + barH + 6, 0xFFFFFFFF, true);

        // 8. Mensaje Parpadeante de Acción (SPAMEA CLIC IZQUIERDO)
        long gameTime = mc.level.getGameTime();
        float pulse = (Mth.sin(gameTime * 0.45F) + 1.0F) * 0.5F;
        int promptColor = (pulse > 0.5F) ? 0xFFFF3333 : 0xFFFFCC00;
        String prompt = "⚡ ¡SPAMEA CLIC IZQUIERDO! ⚡";
        int promptW = font.width(prompt);
        g.drawString(font, prompt, (screenW - promptW) / 2, barY + barH + 20, promptColor, true);
    }
}
