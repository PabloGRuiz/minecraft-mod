package com.pablo.rpglevels.client.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.compat.irons.IronsSpellsCompat;
import com.pablo.rpglevels.entities.SpiritualBeamEntity;
import com.pablo.rpglevels.magic.beam.BeamClashManager;
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
 * Control cinemático de cámara para el lanzamiento del "Spiritual Beam" y durante los Choques de Poderes (Beam Clash).
 * - Al disparar el Spiritual Beam: Transiciona a tercera persona con vista dinámica sobre el hombro,
 *   expansión de FOV y vibración de energía para apreciar el Kamehameha en todo su esplendor.
 * - En un Beam Clash: Transiciona a vista lateral rotatoria orbital encuadrando épicamente a ambos combatientes.
 * - Al finalizar: Restaura de forma limpia la perspectiva previa que tenía el jugador.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class BeamClashCameraClientEvents {

    private static boolean wasCinematicActive = false;
    private static CameraType previousCameraType = CameraType.FIRST_PERSON;

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
        boolean firingBeam = isLocalPlayerFiringBeam();
        boolean shouldBeCinematic = inClash || firingBeam;

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
        boolean firingBeam = isLocalPlayerFiringBeam();
        float time = (float) (mc.level.getGameTime() + event.getPartialTick());

        if (inClash) {
            // --- CÁMARA DE CHOQUE DE PODERES: Vista lateral orbital de los 2 combatientes ---
            Vec3 clashPos = BeamClashManager.CLIENT_STATE.clashPos;
            if (clashPos != null && !clashPos.equals(Vec3.ZERO)) {
                Vec3 playerPos = mc.player.getEyePosition();
                double dx = clashPos.x - playerPos.x;
                double dz = clashPos.z - playerPos.z;

                float baseAngle = (float) Math.toDegrees(Math.atan2(dz, dx));
                float orbitOffset = 80.0F + Mth.sin(time * 0.045F) * 20.0F;

                float cinematicYaw = baseAngle + orbitOffset;
                float cinematicPitch = 12.0F + Mth.cos(time * 0.035F) * 5.0F;

                event.setYaw(cinematicYaw);
                event.setPitch(cinematicPitch);

                // Inclinación dramática holandesa
                event.setRoll(Mth.sin(time * 0.12F) * 2.0F);
            }
        } else if (firingBeam) {
            // --- CÁMARA CINEMÁTICA DE DISPARO DEL HAZ (KAMEHAMEHA) ---
            // Encuadre sobre el hombro: ángulo ligeramente desplazado para ver al personaje
            // con las manos extendidas y el haz masivo disparándose hacia adelante.
            float yawOffset = 18.0F + Mth.sin(time * 0.08F) * 2.5F;
            float pitchOffset = -2.5F + Mth.sin(time * 0.10F) * 1.5F;

            // Vibración espiritual continua por la colosal potencia del haz
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
        boolean firingBeam = isLocalPlayerFiringBeam();

        if (inClash || firingBeam) {
            // Expansión de FOV para mayor dinamismo y sensación de energía destructiva
            event.setFOV(event.getFOV() + 7.5);
        }
    }
}
