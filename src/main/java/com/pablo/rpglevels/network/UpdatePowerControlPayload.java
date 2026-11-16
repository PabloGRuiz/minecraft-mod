package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.events.ModAttributesHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Cliente -> Servidor: Actualiza la regulación y supresión de estadísticas del jugador (Power Control).
 */
public record UpdatePowerControlPayload(
        float powerOutput,
        float moveSpeed,
        float attackSpeed,
        float jumpHeight
) implements CustomPacketPayload {

    public static final Type<UpdatePowerControlPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "update_power_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePowerControlPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,
                    UpdatePowerControlPayload::powerOutput,
                    ByteBufCodecs.FLOAT,
                    UpdatePowerControlPayload::moveSpeed,
                    ByteBufCodecs.FLOAT,
                    UpdatePowerControlPayload::attackSpeed,
                    ByteBufCodecs.FLOAT,
                    UpdatePowerControlPayload::jumpHeight,
                    UpdatePowerControlPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdatePowerControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) return;

            data.setPowerControl(
                    payload.powerOutput(),
                    payload.moveSpeed(),
                    payload.attackSpeed(),
                    payload.jumpHeight()
            );

            // Reaplicar atributos y sincronizar con cliente
            ModAttributesHandler.updatePlayerAttributes(player);
            ModNetworking.syncToPlayer(player);

            player.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.2F);
        });
    }
}
