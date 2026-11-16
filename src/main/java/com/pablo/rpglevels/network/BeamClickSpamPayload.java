package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.magic.beam.BeamClashManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Enviado por el cliente cuando el jugador spamea Clic Izquierdo durante un Choque de Poderes (Beam Clash).
 */
public record BeamClickSpamPayload() implements CustomPacketPayload {

    public static final Type<BeamClickSpamPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "beam_click_spam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeamClickSpamPayload> STREAM_CODEC =
            StreamCodec.unit(new BeamClickSpamPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeamClickSpamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BeamClashManager.handlePlayerClick(player);
            }
        });
    }
}
