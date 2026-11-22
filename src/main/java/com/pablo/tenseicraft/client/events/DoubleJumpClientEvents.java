package com.pablo.tenseicraft.client.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.network.PerformDoubleJumpPayload;
import com.pablo.tenseicraft.utils.DoubleJumpHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class DoubleJumpClientEvents {

    private static boolean canDoubleJump = false;
    private static boolean jumpKeyReleasedSinceAir = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null
                || com.pablo.tenseicraft.client.cinematics.ReimuIntroCinematicClient.isActive()) {
            return;
        }

        // 1. Reset availability when grounded, climbing, or in fluids
        if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            canDoubleJump = true;
            jumpKeyReleasedSinceAir = false;
            return;
        }

        // 2. CRITICAL: Elytra protection. If wearing any elytra or fall flying, disable double jump completely
        // so spacebar remains 100% dedicated to vanilla elytra deployment and gliding!
        if (DoubleJumpHelper.hasElytraEquippedOrFlying(player)) {
            canDoubleJump = false;
            jumpKeyReleasedSinceAir = false;
            return;
        }

        // 3. Other disqualifiers
        if (player.isSpectator() || player.getAbilities().flying || player.isPassenger() || player.isDeadOrDying()) {
            return;
        }

        // 4. Track spacebar state in mid-air
        boolean jumpKeyDown = mc.options.keyJump.isDown();
        if (!jumpKeyDown) {
            // Player let go of space after jumping or stepping off ledge
            jumpKeyReleasedSinceAir = true;
        } else if (jumpKeyReleasedSinceAir && canDoubleJump) {
            // Player pressed space again in mid-air -> execute Double Jump!
            tryPerformDoubleJump(player);
        }
    }

    private static void tryPerformDoubleJump(LocalPlayer player) {
        canDoubleJump = false;
        jumpKeyReleasedSinceAir = false;

        float forward = player.input.forwardImpulse;
        float left = player.input.leftImpulse;
        boolean hasInput = (forward != 0.0f || left != 0.0f);

        float dirX = 0.0f;
        float dirZ = 0.0f;

        Vec3 currentVel = player.getDeltaMovement();
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasSkill("agility_double_jump")) {
            return;
        }

        int dexterity = data.getTotalStatPoints(player, StatType.DEXTERITY);
        double horizontalBoost = 0.28 + Math.min(0.12, dexterity * 0.005);

        double vx;
        double vz;

        if (hasInput) {
            // Directional calculation relative to player camera yaw (matching 360 dash logic)
            double moveAngleRad = Math.atan2(-left, forward);
            float jumpYaw = player.getYRot() + (float) Math.toDegrees(moveAngleRad);
            double rad = Math.toRadians(jumpYaw);
            dirX = (float) -Math.sin(rad);
            dirZ = (float) Math.cos(rad);

            // Residual momentum is preserved (0.85x) + crisp directional boost in input direction
            vx = (currentVel.x * 0.85) + (dirX * horizontalBoost);
            vz = (currentVel.z * 0.85) + (dirZ * horizontalBoost);
        } else {
            // No movement keys pressed: preserve 100% of current residual horizontal momentum
            vx = currentVel.x;
            vz = currentVel.z;
        }

        // Upward leap impulse
        double vy = 0.50 + Math.min(0.08, dexterity * 0.003);

        // Apply instant client-side velocity (0ms latency feel)
        player.setDeltaMovement(new Vec3(vx, vy, vz));
        player.hasImpulse = true;
        player.resetFallDistance();

        // Local audiovisual effects
        player.playSound(SoundEvents.WIND_CHARGE_BURST.value(), 0.8f, 1.6f);

        // Ring of cloud particles under feet
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * Math.PI * 2;
            double px = Math.cos(angle) * 0.35;
            double pz = Math.sin(angle) * 0.35;
            player.level().addParticle(
                    ParticleTypes.CLOUD,
                    player.getX() + px, player.getY(), player.getZ() + pz,
                    px * 0.15, -0.05, pz * 0.15
            );
        }
        player.level().addParticle(
                ParticleTypes.GUST,
                player.getX(), player.getY(), player.getZ(),
                0, 0, 0
        );

        // Send to server
        PacketDistributor.sendToServer(new PerformDoubleJumpPayload(dirX, dirZ, hasInput));
    }
}
