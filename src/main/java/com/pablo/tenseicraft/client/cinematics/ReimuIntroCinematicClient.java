package com.pablo.tenseicraft.client.cinematics;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.ReimuBossEntity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * Cinemática de introducción y transición entre fases para Reimu Hakurei en El Gensokyo:
 * - Enfoca la cámara suavemente hacia Reimu mientras pronuncia sus diálogos.
 * - Bloquea el movimiento del jugador (caminar, saltar, agacharse, Dash y Doble Salto) durante la cinemática.
 * - Aplica zoom progresivo, barras panorámicas 21:9 y subtítulos sincronizados por etapa.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class ReimuIntroCinematicClient {

    private static boolean active = false;
    private static ReimuBossEntity targetBoss = null;
    private static CameraType savedCameraType = CameraType.FIRST_PERSON;
    private static int activeMode = 0; // 1 = intro, 2 = transición etapa 2, 3 = transición etapa 3
    private static int introDuration = 180;

    private static void stopCinematic(Minecraft mc) {
        if (!active) return;
        active = false;
        activeMode = 0;
        targetBoss = null;
        if (mc.player != null) {
            mc.options.setCameraType(savedCameraType);
        }
    }

    public static boolean isActive() {
        return active;
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!active) return;

        var input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!active) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopCinematic(mc);
            return;
        }

        List<ReimuBossEntity> bosses = mc.level.getEntitiesOfClass(
                ReimuBossEntity.class,
                mc.player.getBoundingBox().inflate(96.0),
                b -> b.isAlive() && b.getIntroTicks() > 0 && b.getCinematicMode() > 0
        );

        if (bosses.isEmpty()) {
            stopCinematic(mc);
            return;
        }

        ReimuBossEntity boss = bosses.get(0);
        int mode = boss.getCinematicMode();
        if (!active || activeMode != mode) {
            if (!active) {
                savedCameraType = mc.options.getCameraType();
                if (savedCameraType != CameraType.FIRST_PERSON) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                }
            }
            active = true;
            activeMode = mode;
            int defaultDur = switch (mode) {
                case 2 -> 85;
                case 3 -> 105;
                default -> 180;
            };
            introDuration = Math.max(boss.getIntroTicks(), defaultDur);
        }
        targetBoss = boss;

        // Mantener orientada la mirada del jugador hacia Reimu para una transición fluida al terminar
        Vec3 eye = mc.player.getEyePosition(1.0F);
        Vec3 focus = boss.position().add(0.0, 1.38, 0.0);
        Vec3 dir = focus.subtract(eye);
        double horiz = Math.hypot(dir.x, dir.z);
        if (horiz > 1.0E-4) {
            float yaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }
    }

    private static float computeZoomFactor(int remainingTicks) {
        int easeIn = (activeMode == 1) ? 20 : 14;
        int easeOut = 15;
        int elapsed = Math.max(0, introDuration - remainingTicks);
        float factor;
        if (elapsed < easeIn) {
            factor = elapsed / (float) easeIn;
        } else if (remainingTicks < easeOut) {
            factor = remainingTicks / (float) easeOut;
        } else {
            factor = 1.0F;
        }
        factor = Mth.clamp(factor, 0.0F, 1.0F);
        return factor * factor * (3.0F - 2.0F * factor);
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active || targetBoss == null || !targetBoss.isAlive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        float partial = (float) event.getPartialTick();
        Vec3 camPos = mc.player.getEyePosition(partial);
        Vec3 bossFocus = targetBoss.getPosition(partial).add(0.0, 1.38, 0.0);
        Vec3 dir = bossFocus.subtract(camPos);
        double horiz = Math.hypot(dir.x, dir.z);
        if (horiz < 1.0E-4) return;

        float targetYaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));

        float time = mc.level.getGameTime() + partial;
        int rem = targetBoss.getIntroTicks();
        float zoom = computeZoomFactor(rem);
        float swayYaw = Mth.sin(time * 0.04F) * 1.1F * zoom;
        float swayPitch = Mth.cos(time * 0.035F) * 0.7F * zoom;

        if (activeMode == 3 && rem <= 55) {
            swayYaw += Mth.sin(time * 2.1F) * 0.65F * zoom;
            swayPitch += Mth.cos(time * 2.4F) * 0.45F * zoom;
        }

        event.setYaw(targetYaw + swayYaw);
        event.setPitch(targetPitch + swayPitch);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!active || targetBoss == null || !targetBoss.isAlive()) return;

        float zoom = computeZoomFactor(targetBoss.getIntroTicks());
        double baseFov = event.getFOV();
        double maxReduce = (activeMode == 1) ? 38.0 : 32.0;
        double zoomedFov = Math.max(24.0, baseFov - maxReduce * zoom);
        event.setFOV(zoomedFov);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!active || targetBoss == null || !targetBoss.isAlive()) return;
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        int rem = targetBoss.getIntroTicks();
        float zoom = computeZoomFactor(rem);
        int iBarH = Math.round(screenH * 0.12F * zoom);

        if (iBarH > 0) {
            g.fill(0, 0, screenW, iBarH, 0xFF000000);
            g.fill(0, screenH - iBarH, screenW, screenH, 0xFF000000);
        }

        if (zoom > 0.35F) {
            String header;
            int headerColor;
            String subtitle;

            if (activeMode == 2) {
                header = "§b§l⛩ ETAPA 2 — DANZA DE CEREZOS ⛩";
                headerColor = 0xFF66DDFF;
                if (rem > 35) {
                    subtitle = "«Nada mal... Consigues leer mis patrones entre los pétalos.»";
                } else {
                    subtitle = "§b«Tomemos un ritmo más sereno... ¡Danza Carmesí bajo los Cerezos!»";
                }
            } else if (activeMode == 3) {
                header = "§5§l⛩ ETAPA FINAL — SPELL CARD SUPREMA ⛩";
                headerColor = 0xFFFF55AA;
                if (rem > 45) {
                    subtitle = "«¡Se acabó contenerme! Has demostrado ser una amenaza real para el Santuario.»";
                } else {
                    subtitle = "§6«¡Musou Fuuin — Fantasía del Paraíso!»";
                }
            } else {
                header = "§c§l⛩ EL GENSOKYO — SANTUARIO HAKUREI ⛩";
                headerColor = 0xFFFF6666;
                if (rem > 115) {
                    subtitle = "«¿Una fisura en la Gran Barrera Hakurei...? ¡Tú no perteneces a este mundo!»";
                } else if (rem > 65) {
                    subtitle = "«Los invasores de otra dimensión amenazan el equilibrio del Gensokyo.»";
                } else if (rem > 20) {
                    subtitle = "«¡Te expulsaré de aquí usando las Reglas de las Spell Cards!»";
                } else {
                    subtitle = "§6«¡Sello Espiritual — Danza Carmesí del Santuario Hakurei!»";
                }
            }

            int topY = Math.max(6, (iBarH - 10) / 2);
            g.drawCenteredString(font, Component.literal(header), screenW / 2, topY, headerColor);

            int subY = screenH - Math.max(18, (iBarH + 8) / 2);
            g.drawCenteredString(font, Component.literal("§f" + subtitle), screenW / 2, subY, 0xFFFFFFFF);
        }
    }
}
