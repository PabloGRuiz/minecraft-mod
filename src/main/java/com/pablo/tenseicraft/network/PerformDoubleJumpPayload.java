package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.utils.DoubleJumpHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PerformDoubleJumpPayload(float dirX, float dirZ, boolean hasInput) implements CustomPacketPayload {

    public static final Type<PerformDoubleJumpPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "perform_double_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PerformDoubleJumpPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    PerformDoubleJumpPayload::dirX,
                    ByteBufCodecs.FLOAT,
                    PerformDoubleJumpPayload::dirZ,
                    ByteBufCodecs.BOOL,
                    PerformDoubleJumpPayload::hasInput,
                    PerformDoubleJumpPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PerformDoubleJumpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (player.isSpectator() || player.isDeadOrDying() || player.getAbilities().flying || player.isPassenger()) {
                return;
            }

            // CRITICAL: Prevent double jump if wearing or flying with any Elytra
            if (DoubleJumpHelper.hasElytraEquippedOrFlying(player)) {
                return;
            }

            if (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
                return;
            }

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null || !data.canDoubleJumpAirborne()) {
                return;
            }

            long currentTick = player.level().getGameTime();
            data.triggerDoubleJump(currentTick);

            Vec3 currentVel = player.getDeltaMovement();
            int dexterity = data.getTotalStatPoints(player, StatType.DEXTERITY);
            float jumpRatio = data.getJumpHeightRatio();
            double horizontalBoost = (0.28 + Math.min(0.12, dexterity * 0.005)) * (0.35 + 0.65 * jumpRatio);

            float rawX = Float.isFinite(payload.dirX()) ? payload.dirX() : 0.0f;
            float rawZ = Float.isFinite(payload.dirZ()) ? payload.dirZ() : 0.0f;
            Vec3 inputDir = new Vec3(rawX, 0.0, rawZ);
            if (inputDir.lengthSqr() > 0.0001) {
                inputDir = inputDir.normalize();
            }

            double vx;
            double vz;
            if (payload.hasInput() && inputDir.lengthSqr() > 0.0001) {
                // Residual momentum preserved + normalized directional action boost
                vx = (currentVel.x * 0.85) + (inputDir.x * horizontalBoost);
                vz = (currentVel.z * 0.85) + (inputDir.z * horizontalBoost);
            } else {
                // No movement keys: preserve 100% of current horizontal residual velocity
                vx = currentVel.x;
                vz = currentVel.z;
            }

            // Upward vertical impulse regulable con el slider de Altura de Salto
            double baseVy = 0.50 + Math.min(0.08, dexterity * 0.003);
            double vy = 0.38 + (baseVy - 0.38) * jumpRatio;

            player.setDeltaMovement(new Vec3(vx, vy, vz));
            player.hasImpulse = true;
            player.hurtMarked = true;
            player.resetFallDistance();

            // Broadcast sound and air step particles to nearby players
            ServerLevel serverLevel = player.serverLevel();
            serverLevel.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WIND_CHARGE_BURST,
                    SoundSource.PLAYERS,
                    0.8f,
                    1.6f
            );

            // Ring of clouds at feet simulating air platform jump
            for (int i = 0; i < 8; i++) {
                double angle = (i / 8.0) * Math.PI * 2;
                double px = Math.cos(angle) * 0.35;
                double pz = Math.sin(angle) * 0.35;
                serverLevel.sendParticles(
                        ParticleTypes.CLOUD,
                        player.getX() + px, player.getY(), player.getZ() + pz,
                        1, px * 0.15, -0.05, pz * 0.15, 0.04
                );
            }
            serverLevel.sendParticles(
                    ParticleTypes.GUST,
                    player.getX(), player.getY(), player.getZ(),
                    1, 0, 0, 0, 0.05
            );
        });
    }
}
