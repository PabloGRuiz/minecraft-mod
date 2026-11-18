package com.pablo.rpglevels.client.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.compat.irons.IronsSpellsCompat;
import com.pablo.rpglevels.entities.SpiritualBeamEntity;
import com.pablo.rpglevels.magic.beam.BeamClashManager;
import com.pablo.rpglevels.network.EndBeamClashPayload;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Control cinemático de cámara cinematográfica de alto impacto para:
 * 1. Lanzamiento del "Spiritual Beam" (Kamehameha / Cero sobre el hombro).
 * 2. Choques de Poderes (Beam Clash) estilo Anime/Dragon Ball:
 *    - Vista lateral panorámica orbital dinámica que encuadra a ambos combatientes.
 *    - Screen Shake / Rumble sísmico continuo por colisión de gigavatios de energía espiritual.
 *    - Inclinación de cámara (Dutch Angle) que reacciona a la ventaja del forcejeo.
 *    - Impulso cinético táctil de cámara en cada clic del jugador.
 *    - Tensión dinámica de FOV según ventaja y pulsación de energía.
 *    - Secuencia de Desenlace / Aftermath: explosión cataclísmica y retroceso cinemático antes de volver a 1ª persona.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class BeamClashCameraClientEvents {

    private static boolean wasCinematicActive = false;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;

    // Estado del Choque y Aftermath
    private static int clashDurationTicks = 0;
    private static int aftermathTicks = 0;
    private static final int AFTERMATH_TOTAL_TICKS = 35;
    private static boolean isLocalWinner = false;
    private static boolean aftermathTimeout = false;
    private static Vec3 aftermathExplosionPos = Vec3.ZERO;

    // Impulso de sacudida por spam de clics
    private static float clickShakeIntensity = 0.0F;

    public static void registerClickImpulse() {
        clickShakeIntensity = Math.min(3.8F, clickShakeIntensity + 0.9F);
    }

    public static void triggerAftermath(EndBeamClashPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean involved = (mc.player.getId() == payload.winnerId() || mc.player.getId() == payload.loserId());
        if (!involved) return;

        aftermathTicks = AFTERMATH_TOTAL_TICKS;
        isLocalWinner = (mc.player.getId() == payload.winnerId());
        aftermathTimeout = payload.wasTimeout();
        aftermathExplosionPos = new Vec3(payload.clashX(), payload.clashY(), payload.clashZ());
    }

    public static boolean isAftermathActive() {
        return aftermathTicks > 0;
    }

    public static int getAftermathTicks() {
        return aftermathTicks;
    }

    public static int getAftermathTotalTicks() {
        return AFTERMATH_TOTAL_TICKS;
    }

    public static boolean isLocalWinner() {
        return isLocalWinner;
    }

    public static int getClashDurationTicks() {
        return clashDurationTicks;
    }

    public static boolean isLocalPlayerFiringBeam() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        // 1. Verificar si hay un SpiritualBeamEntity activo en el mundo cuyo lanzador sea el jugador local
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof SpiritualBeamEntity beam && beam.getCasterId() == mc.player.getId() && beam.isAlive()) {
                return true;
            }
        }

        // 2. Verificar en MagicData si está canalizando el hechizo spiritual_beam
        if (IronsSpellsCompat.isLoaded()) {
            try {
                var magicData = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(mc.player);
                if (magicData != null && magicData.isCasting() && "rpglevels:spiritual_beam".equals(magicData.getCastingSpellId())) {
                    return true;
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean inClash = BeamClashManager.isPlayerInClash(mc.player.getId());
        boolean inAftermath = aftermathTicks > 0;
        boolean firingBeam = isLocalPlayerFiringBeam();
        boolean shouldBeCinematic = inClash || inAftermath || firingBeam;

        if (inClash) {
            clashDurationTicks++;
        } else {
            clashDurationTicks = 0;
        }

        if (inAftermath) {
            aftermathTicks--;
        }

        // Decadencia suave de la sacudida por clic
        if (clickShakeIntensity > 0.0F) {
            clickShakeIntensity *= 0.86F;
            if (clickShakeIntensity < 0.05F) clickShakeIntensity = 0.0F;
        }

        if (shouldBeCinematic && !wasCinematicActive) {
            // Guardar perspectiva actual del usuario y forzar tercera persona trasera
            previousCameraType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            wasCinematicActive = true;
        } else if (!shouldBeCinematic && wasCinematicActive) {
            // Restaurar perspectiva previa del usuario
            mc.options.setCameraType(previousCameraType);
            wasCinematicActive = false;
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean inClash = BeamClashManager.isPlayerInClash(mc.player.getId());
        boolean inAftermath = aftermathTicks > 0;
        boolean firingBeam = isLocalPlayerFiringBeam();
        float time = (float) (mc.level.getGameTime() + event.getPartialTick());

        if (inClash) {
            // --- CÁMARA DE CHOQUE DE PODERES: Vista lateral orbital con vibración sísmica ---
            Vec3 clashPos = BeamClashManager.CLIENT_STATE.clashPos;
            if (clashPos != null && !clashPos.equals(Vec3.ZERO)) {
                Vec3 playerPos = mc.player.getEyePosition();
                double dx = clashPos.x - playerPos.x;
                double dz = clashPos.z - playerPos.z;

                float baseAngle = (float) Math.toDegrees(Math.atan2(dz, dx));
                float orbitOffset = 78.0F + Mth.sin(time * 0.045F) * 22.0F;

                float cinematicYaw = baseAngle + orbitOffset;
                float cinematicPitch = 9.5F + Mth.cos(time * 0.035F) * 5.0F;

                // Vibración de alta frecuencia por la brutal fricción entre los haces
                float continuousRumble = (float) ((Math.sin(time * 2.2) * 0.40) + ((Math.random() - 0.5) * 0.50));
                float totalShake = continuousRumble + (clickShakeIntensity * (float) ((Math.random() - 0.5) * 1.4));

                event.setYaw(cinematicYaw + totalShake * 0.75F);
                event.setPitch(cinematicPitch + totalShake);

                // Inclinación dramática holandesa reactiva a la ventaja en el choque
                boolean isA = (mc.player.getId() == BeamClashManager.CLIENT_STATE.entityAId);
                float playerProgress = isA ? BeamClashManager.CLIENT_STATE.progress : (1.0F - BeamClashManager.CLIENT_STATE.progress);
                float struggleRoll = (playerProgress - 0.5F) * 7.5F;
                float rollJitter = Mth.sin(time * 0.16F) * 2.6F + (float) ((Math.random() - 0.5) * 0.9);

                event.setRoll(struggleRoll + rollJitter);
            }
        } else if (inAftermath) {
            // --- AFTERMATH: Explosión cataclísmica al resolverse el choque ---
            float intensity = aftermathTicks / (float) AFTERMATH_TOTAL_TICKS;
            float expRumble = intensity * 4.2F * (float) (Math.random() - 0.5);

            event.setYaw(event.getYaw() + expRumble * 0.8F);
            event.setPitch(event.getPitch() + expRumble);
            event.setRoll(Mth.sin(time * 0.28F) * 5.0F * intensity);
        } else if (firingBeam) {
            // --- CÁMARA CINEMÁTICA DE DISPARO DEL HAZ (KAMEHAMEHA) ---
            float yawOffset = 18.0F + Mth.sin(time * 0.08F) * 2.5F;
            float pitchOffset = -2.5F + Mth.sin(time * 0.10F) * 1.5F;

            float rumble = (float) ((Math.sin(time * 1.6) * 0.35) + ((Math.random() - 0.5) * 0.25));

            event.setYaw(mc.player.getYRot() + yawOffset + rumble * 0.4F);
            event.setPitch(mc.player.getXRot() + pitchOffset + rumble);
            event.setRoll(Mth.sin(time * 0.14F) * 1.8F);
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean inClash = BeamClashManager.isPlayerInClash(mc.player.getId());
        boolean inAftermath = aftermathTicks > 0;
        boolean firingBeam = isLocalPlayerFiringBeam();
        float time = (float) (mc.level != null ? mc.level.getGameTime() + event.getPartialTick() : 0.0F);

        if (inClash) {
            // Expansión dinámica de FOV con pulsación de energía y tensión de forcejeo
            boolean isA = (mc.player.getId() == BeamClashManager.CLIENT_STATE.entityAId);
            float playerProgress = isA ? BeamClashManager.CLIENT_STATE.progress : (1.0F - BeamClashManager.CLIENT_STATE.progress);

            double baseFov = 9.0;
            double wave = Mth.sin(time * 0.35F) * 2.5;
            double clickPop = Math.min(3.5, clickShakeIntensity * 1.2);
            double struggleFov = playerProgress > 0.6F ? 3.0 : (playerProgress < 0.4F ? -2.5 : 0.0);

            event.setFOV(event.getFOV() + baseFov + wave + clickPop + struggleFov);
        } else if (inAftermath) {
            // Onda de choque expansiva de la detonación final
            float intensity = aftermathTicks / (float) AFTERMATH_TOTAL_TICKS;
            event.setFOV(event.getFOV() + (intensity * 22.0));
        } else if (firingBeam) {
            event.setFOV(event.getFOV() + 7.5);
        }
    }
}
