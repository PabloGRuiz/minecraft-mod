package com.pablo.tenseicraft.client.cinematics;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.ReimuBossEntity;
import com.pablo.tenseicraft.world.dimension.ModDimensions;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Cámara cenital/isométrica estilo Touhou en tercera persona durante el combate activo
 * contra Reimu Hakurei en El Gensokyo (después de cada cinemática de fase).
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class ReimuTouhouCameraClient {

    private static final float CAMERA_YAW = 0.0F;     // Norte abajo / Reimu (+Z) arriba en pantalla
    private static final float CAMERA_PITCH = 65.0F;   // Vista isométrica pronunciada desde arriba
    private static final float CAMERA_DISTANCE = 16.0F;
    private static final int TRANSITION_TICKS = 14;
    private static final int MAX_GRACE_TICKS = 10;

    private static boolean active = false;
    private static CameraType savedCameraType = null;
    private static int transitionTicks = 0;
    private static int graceTicks = 0;

    public static boolean isActive() {
        return active;
    }

    public static boolean hasSavedCamera() {
        return savedCameraType != null;
    }

    private static void deactivate(Minecraft mc, boolean restoreCamera) {
        boolean wasActive = active || savedCameraType != null;
        active = false;
        transitionTicks = 0;
        graceTicks = 0;
        if (restoreCamera && savedCameraType != null) {
            if (mc.player != null) {
                mc.options.setCameraType(savedCameraType);
            }
            savedCameraType = null;
        }
        if (wasActive && restoreCamera) {
            TenseiCraftMod.LOGGER.info("[ReimuTouhouCamera] Cámara isométrica desactivada");
        }
    }

    private static float smoothAlpha(float extraPartial) {
        float t = Mth.clamp((transitionTicks + extraPartial) / (float) TRANSITION_TICKS, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || !ModDimensions.isGensokyo(mc.level)
                || !mc.player.isAlive()
                || mc.player.isSpectator()) {
            deactivate(mc, true);
            return;
        }

        // Ceder el control completo a ReimuIntroCinematicClient durante intros o transiciones de etapa
        if (ReimuIntroCinematicClient.isActive()) {
            active = false;
            transitionTicks = 0;
            return;
        }

        List<ReimuBossEntity> bosses = mc.level.getEntitiesOfClass(
                ReimuBossEntity.class,
                mc.player.getBoundingBox().inflate(96.0),
                b -> b.isAlive() && !(b.getIntroTicks() > 0 && b.getCinematicMode() > 0)
        );

        if (bosses.isEmpty()) {
            if (graceTicks > 0) {
                graceTicks--;
            } else {
                deactivate(mc, true);
            }
            return;
        }

        ReimuBossEntity boss = bosses.stream()
                .min(Comparator.comparingDouble(b -> b.distanceToSqr(mc.player)))
                .orElse(bosses.get(0));

        if (savedCameraType == null) {
            savedCameraType = mc.options.getCameraType();
        }
        if (!active) {
            TenseiCraftMod.LOGGER.info("[ReimuTouhouCamera] Cámara isométrica activada (boss={}, stage={})",
                    boss.getId(), boss.getBossStage());
        }
        active = true;
        graceTicks = MAX_GRACE_TICKS;

        if (mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        if (transitionTicks < TRANSITION_TICKS) {
            transitionTicks++;
        }

        // Auto-orientar suavemente la mirada del jugador hacia Reimu para que ataques y hechizos acierten desde vista cenital
        if (mc.screen == null) {
            Vec3 eye = mc.player.getEyePosition(1.0F);
            Vec3 target = boss.position().add(0.0, boss.getBbHeight() * 0.55, 0.0);
            Vec3 dir = target.subtract(eye);
            double horiz = Math.hypot(dir.x, dir.z);
            if (horiz > 1.0E-4) {
                float targetYaw = (float) (Math.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
                float targetPitch = (float) -(Math.atan2(dir.y, horiz) * (180.0 / Math.PI));
                mc.player.setYRot(Mth.rotLerp(0.35F, mc.player.getYRot(), targetYaw));
                mc.player.setXRot(Mth.lerp(0.35F, mc.player.getXRot(), Mth.clamp(targetPitch, -45.0F, 45.0F)));
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active || ReimuIntroCinematicClient.isActive()) return;

        float alpha = smoothAlpha((float) event.getPartialTick());
        event.setYaw(Mth.rotLerp(alpha, event.getYaw(), CAMERA_YAW));
        event.setPitch(Mth.lerp(alpha, event.getPitch(), CAMERA_PITCH));
        event.setRoll(0.0F);
    }

    @SubscribeEvent
    public static void onCalculateCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (!active || ReimuIntroCinematicClient.isActive()) return;

        float alpha = smoothAlpha(0.0F);
        event.setDistance(Mth.lerp(alpha, event.getDistance(), CAMERA_DISTANCE));
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!active || ReimuIntroCinematicClient.isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || event.getEntity() != mc.player) return;

        var input = event.getInput();
        float screenUp = input.forwardImpulse;   // W (+) / S (-)
        float screenLeft = input.leftImpulse;    // A (+) / D (-)
        if (screenUp == 0.0F && screenLeft == 0.0F) {
            return;
        }

        // Con cámara fija en yaw = 0° (mirando al Sur +Z): Arriba = +Z, Izquierda = +X
        float worldMoveX = screenLeft;
        float worldMoveZ = screenUp;

        float yawRad = (float) Math.toRadians(mc.player.getYRot());
        float sin = Mth.sin(yawRad);
        float cos = Mth.cos(yawRad);

        input.forwardImpulse = -worldMoveX * sin + worldMoveZ * cos;
        input.leftImpulse = worldMoveX * cos + worldMoveZ * sin;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        savedCameraType = null;
        transitionTicks = 0;
        graceTicks = 0;
    }
}
