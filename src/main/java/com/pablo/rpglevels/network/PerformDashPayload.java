package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
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

public record PerformDashPayload(float dirX, float dirY, float dirZ, boolean isAirDash) implements CustomPacketPayload {

    public static final Type<PerformDashPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "perform_dash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PerformDashPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    PerformDashPayload::dirX,
                    ByteBufCodecs.FLOAT,
                    PerformDashPayload::dirY,
                    ByteBufCodecs.FLOAT,
                    PerformDashPayload::dirZ,
                    ByteBufCodecs.BOOL,
                    PerformDashPayload::isAirDash,
                    PerformDashPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PerformDashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (player.isSpectator() || player.isDeadOrDying()) {
                return;
            }

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null || !data.hasSkill("agility_dash")) {
                return;
            }

            long currentTick = player.level().getGameTime();
            if (!data.canDash(currentTick, player)) {
                return;
            }

            // Consume Mana (20 MP)
            if (!data.consumeMana(player, PlayerRpgData.DASH_MANA_COST)) {
                return;
            }
            com.pablo.rpglevels.network.ModNetworking.syncMana(player);

            // Sanitize and normalize client direction vector to prevent speedhack/crash exploits
            float rawX = Float.isFinite(payload.dirX()) ? payload.dirX() : 0.0f;
            float rawY = Float.isFinite(payload.dirY()) ? payload.dirY() : 0.0f;
            float rawZ = Float.isFinite(payload.dirZ()) ? payload.dirZ() : 0.0f;

            // Calculate speed scaling with Dexterity (Level 1 tactical dash)
            int dexterity = data.getTotalStatPoints(player, StatType.DEXTERITY);
            double baseSpeed = 1.05 + Math.min(0.20, dexterity * 0.008);

            Vec3 dirFlat = new Vec3(rawX, 0.0, rawZ);
            if (dirFlat.lengthSqr() < 0.0001) {
                Vec3 look = player.getLookAngle();
                dirFlat = new Vec3(look.x, 0.0, look.z);
            }
            if (dirFlat.lengthSqr() > 0.0001) {
                dirFlat = dirFlat.normalize();
            }
            double vx = dirFlat.x * baseSpeed;
            double vy = 0.16;
            double vz = dirFlat.z * baseSpeed;

            // Apply velocity & activate I-Frames (8 ticks = 0.4s)
            player.setDeltaMovement(new Vec3(vx, vy, vz));
            player.hurtMarked = true;
            data.triggerDash(currentTick, 8);
            ModNetworking.syncToPlayer(player);

            // Server-side audiovisuals for other players to witness
            ServerLevel serverLevel = player.serverLevel();
            serverLevel.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS,
                    0.8f,
                    1.4f
            );
            for (int i = 0; i < 6; i++) {
                serverLevel.sendParticles(
                        ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.2, player.getZ(),
                        1,
                        -dirFlat.x * 0.25, 0.05, -dirFlat.z * 0.25,
                        0.02
                );
            }
        });
    }
}
