package com.pablo.tenseicraft.client.cinematics;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Cinemática de Audiencia Divina en el Espacio Astral:
 * - Se activa al entrar al Plano Astral (tanto en primera bendición como en ascensiones de hito).
 * - Muestra varios planos cinematográficos rotatorios y contemplativos alrededor del jugador mientras el Dios habla.
 * - Da tiempo generoso al jugador para leer tranquilamente los diálogos divinos y admirar el entorno cósmico.
 * - En la ascensión a Rango B, libera la cámara una vez concluidas las palabras del dios para permitir la interacción
 *   con las 3 estatuas guardianas (Starry, Chubby, Brambi).
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class AstralCinematicClient {

    private static boolean active = false;
    private static int casterEntityId = -1;
    private static int currentTick = 0;
    private static int totalTicks = 320;
    private static boolean isAscendingToB = false;
    private static CameraType savedCameraType = CameraType.FIRST_PERSON;

    public static void startCinematic(int entityId, int duration, boolean ascendingToB) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != entityId) return;

        active = true;
        casterEntityId = entityId;
        currentTick = 0;
        totalTicks = duration;
        isAscendingToB = ascendingToB;

        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    }

    public static void stopCinematic() {
        if (!active) return;
        active = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.options.setCameraType(savedCameraType);
        }
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopCinematic();
            return;
        }

        currentTick++;

        // Transición de planos de cámara
        if (currentTick == 85) {
            // Plano 2: Cambiar a tercera persona trasera orbital
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (currentTick == 190) {
            // Plano 3: Perspectiva lateral
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        // Si es ascensión a B, liberar cámara en tick 250 para que el jugador elija a su guardián
        if (isAscendingToB && currentTick >= 250) {
            stopCinematic();
            return;
        }

        if (currentTick >= totalTicks) {
            stopCinematic();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != casterEntityId) return;

        float progress = (float) (currentTick + (double) event.getPartialTick());

        if (progress < 85.0f) {
            // PLANO 1: Contrapicado frontal contemplativo mirando hacia arriba al jugador en el cosmos
            float floatPitch = -12.0f + (float) Math.sin(progress * 0.05f) * 2.5f;
            float floatYaw = (float) Math.sin(progress * 0.04f) * 5.0f;
            event.setPitch(event.getPitch() + floatPitch);
            event.setYaw(event.getYaw() + floatYaw);
            event.setRoll((float) Math.sin(progress * 0.035f) * 1.5f);
        } else if (progress < 190.0f) {
            // PLANO 2: Paneo orbital panorámico circular alrededor del jugador a 360°
            float elapsed = progress - 85.0f;
            float orbitYaw = elapsed * 1.6f;
            float orbitPitch = 12.0f + (float) Math.cos(progress * 0.04f) * 3.5f;
            event.setYaw(event.getYaw() + orbitYaw);
            event.setPitch(orbitPitch);
            event.setRoll((float) Math.sin(progress * 0.04f) * 1.8f);
        } else {
            // PLANO 3: Vista lateral dramática en tres cuartos enfocando hacia el horizonte y las estatuas
            float sideYaw = 32.0f + (float) Math.sin(progress * 0.03f) * 4.0f;
            float sidePitch = 8.0f + (float) Math.cos(progress * 0.03f) * 2.0f;
            event.setYaw(event.getYaw() + sideYaw);
            event.setPitch(event.getPitch() + sidePitch);
            event.setRoll(2.0f);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != casterEntityId) return;

        // FOV atmosférico ligeramente amplio para transmitir la inmensidad del espacio astral
        event.setFOV(event.getFOV() + 6.0);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!active) return;
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != casterEntityId || mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        // Barras negras panorámicas 21:9
        float maxBarH = screenH * 0.12f;
        float barH;
        if (currentTick < 15) {
            barH = (currentTick / 15.0f) * maxBarH;
        } else if (currentTick > totalTicks - 15) {
            barH = ((totalTicks - currentTick) / 15.0f) * maxBarH;
        } else {
            barH = maxBarH;
        }

        int iBarH = Math.round(barH);
        if (iBarH > 0) {
            g.fill(0, 0, screenW, iBarH, 0xFF000000);
            g.fill(0, screenH - iBarH, screenW, screenH, 0xFF000000);
        }

        // Título etéreo en la barra superior
        if (currentTick >= 20 && currentTick <= totalTicks - 5) {
            String titleText = "✦ ESPACIO ASTRAL: AUDIENCIA DIVINA ✦";
            String subText = isAscendingToB
                    ? "« Elige a tu Guardián Espiritual interactuando con su estatua sagrada »"
                    : "« El tiempo se detiene en presencia de las deidades ancestrales »";

            int titleW = font.width(titleText);
            int subW = font.width(subText);

            int titleX = (screenW - titleW) / 2;
            int subX = (screenW - subW) / 2;

            int titleY = Math.max(6, (iBarH - 24) / 2);
            int subY = titleY + 13;

            g.drawString(font, Component.literal(titleText), titleX, titleY, 0xFFE5C0FF, true);
            g.drawString(font, Component.literal(subText), subX, subY, 0xFFD0D0D0, true);
        }
    }
}
