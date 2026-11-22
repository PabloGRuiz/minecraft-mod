package com.pablo.tenseicraft.client.cinematics;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Controlador de la cinemática de la "Resonancia Espiritual":
 * 1. Transición de cámara a primer plano frontal (enfocando al personaje mientras concentra el poder).
 * 2. Barras negras panorámicas (letterbox cinematográfico 21:9).
 * 3. Vibración de cámara (rumble sísmico) creciente conforme la energía se desata.
 * 4. Onda de choque expansiva en el tick 35: impulso de FOV y cambio dinámico a vista en 3ª persona.
 * 5. Cámara en 3ª persona mostrando el aura colosal y exaltada.
 * 6. Transición suave y retorno al POV previo del jugador listo para combatir.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class SpiritualResonanceCinematicClient {

    private static boolean cinematicActive = false;
    private static int casterEntityId = -1;
    private static int currentTick = 0;
    private static int totalTicks = 70;
    private static int shockwaveTick = 35;
    private static CameraType savedCameraType = CameraType.FIRST_PERSON;
    private static boolean isHoldingSenbonzakura = false;

    public static void startCinematic(int entityId, int duration, int shockwave) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.player.getId() != entityId) return;

        cinematicActive = true;
        casterEntityId = entityId;
        currentTick = 0;
        totalTicks = duration;
        shockwaveTick = shockwave;

        // Guardar perspectiva previa del jugador y enfocar en primer plano frontal
        savedCameraType = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);

        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            held = mc.player.getOffhandItem();
        }
        isHoldingSenbonzakura = (held.getItem() instanceof SpiritualSwordItem && SpiritualSwordItem.isSenbonzakura(held));
    }

    public static void stopCinematic() {
        if (!cinematicActive) return;
        cinematicActive = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.options.setCameraType(savedCameraType);
        }
    }

    public static boolean isCinematicActive() {
        return cinematicActive;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!cinematicActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopCinematic();
            return;
        }

        currentTick++;

        // En el tick de la explosión (35), pasar la cámara a tercera persona trasera para contemplar el aura exaltada
        if (currentTick == shockwaveTick) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        if (currentTick >= totalTicks) {
            stopCinematic();
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!cinematicActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != casterEntityId) return;

        float progress = (float) (currentTick + (double) event.getPartialTick());

        if (progress < shockwaveTick) {
            // FASE 1: Primer plano frontal con vibración creciente de energía
            float intensity = 0.25f + (progress / (float) shockwaveTick) * 0.75f;
            float rumbleYaw = (float) ((Math.sin(progress * 1.6f) * 0.6f + (Math.random() - 0.5) * 0.35f) * intensity);
            float rumblePitch = (float) ((Math.cos(progress * 1.9f) * 0.5f + (Math.random() - 0.5) * 0.30f) * intensity);
            float dutchRoll = (float) (Math.sin(progress * 0.40f) * 1.8f * intensity);

            event.setYaw(event.getYaw() + rumbleYaw);
            event.setPitch(event.getPitch() + rumblePitch);
            event.setRoll(dutchRoll);
        } else {
            // FASE 2 y 3: Gran explosión y cámara amplia mostrando el aura inmensa
            float elapsedShock = progress - shockwaveTick;

            // Sacudida de impacto en los primeros 8 ticks tras la explosión
            if (elapsedShock < 8.0f) {
                float impulse = 1.0f - (elapsedShock / 8.0f);
                float kickYaw = (float) ((Math.random() - 0.5) * 2.5f * impulse);
                float kickPitch = (float) ((Math.random() - 0.5) * 2.0f * impulse) - (impulse * 1.5f);
                event.setYaw(event.getYaw() + kickYaw);
                event.setPitch(event.getPitch() + kickPitch);
                event.setRoll((float) (Math.sin(elapsedShock * 1.2f) * 2.5f * impulse));
            }

            // Órbita panorámica suave en tercera persona
            float remaining = (float) (totalTicks - shockwaveTick);
            float orbitProgress = Math.min(1.0f, elapsedShock / remaining);
            float orbitYaw = (float) Math.sin(orbitProgress * Math.PI * 0.5f) * 16.0f;
            float orbitPitch = 10.0f * (1.0f - orbitProgress);

            event.setYaw(event.getYaw() + orbitYaw);
            event.setPitch(event.getPitch() + orbitPitch);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!cinematicActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != casterEntityId) return;

        float progress = (float) (currentTick + (double) event.getPartialTick());

        if (progress < shockwaveTick) {
            // Zoom-in de tensión enfocando la mirada/figura del personaje
            float zoom = (progress / (float) shockwaveTick) * 12.0f;
            event.setFOV(event.getFOV() - zoom);
        } else {
            // Expansión masiva de FOV tras la explosión y retorno suave a la normalidad
            float remaining = (float) (totalTicks - shockwaveTick);
            float recovery = Math.min(1.0f, (progress - shockwaveTick) / remaining);
            float fovBoost = 18.0f * (1.0f - recovery);
            event.setFOV(event.getFOV() + fovBoost);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!cinematicActive) return;
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getId() != casterEntityId || mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        // Cálculo animado de las barras negras (Letterbox 21:9)
        float maxBarH = screenH * 0.125f; // ~1/8 de pantalla arriba y abajo
        float barH;
        if (currentTick < 10) {
            barH = (currentTick / 10.0f) * maxBarH;
        } else if (currentTick > totalTicks - 10) {
            barH = ((totalTicks - currentTick) / 10.0f) * maxBarH;
        } else {
            barH = maxBarH;
        }

        int iBarH = Math.round(barH);
        if (iBarH > 0) {
            // Barra superior
            g.fill(0, 0, screenW, iBarH, 0xFF000000);
            // Barra inferior
            g.fill(0, screenH - iBarH, screenW, screenH, 0xFF000000);
        }

        // Título cinematográfico de la transformación (aparece a partir del tick 20)
        if (currentTick >= 20 && currentTick <= totalTicks - 5) {
            String titleText = isHoldingSenbonzakura
                    ? "✦ RESONANCIA ESPIRITUAL: SENBONZAKURA ✦"
                    : "✦ RESONANCIA ESPIRITUAL ✦";
            String subText = isHoldingSenbonzakura
                    ? "« Dispersa en un millón de filos... Danza de pétalos celestiales »"
                    : "« El alma y la hoja se vuelven uno... ¡Poder desatado al 200%! »";

            int titleColor = isHoldingSenbonzakura ? 0xFFFFB7C5 : 0xFFFFD700;
            int subColor = 0xFFE0E0E0;

            int titleW = font.width(titleText);
            int subW = font.width(subText);

            int titleX = (screenW - titleW) / 2;
            int subX = (screenW - subW) / 2;

            // Renderizar centrado elegantemente en la barra negra superior
            int titleY = Math.max(6, (iBarH - 24) / 2);
            int subY = titleY + 13;

            g.drawString(font, Component.literal(titleText), titleX, titleY, titleColor, true);
            g.drawString(font, Component.literal(subText), subX, subY, subColor, true);
        }
    }
}
