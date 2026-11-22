package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.cinematics.AstralCinematicClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartAstralCinematicPayload(int entityId, int durationTicks, boolean isAscendingToB) implements CustomPacketPayload {

    public static final Type<StartAstralCinematicPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "start_astral_cinematic"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartAstralCinematicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StartAstralCinematicPayload::entityId,
            ByteBufCodecs.VAR_INT, StartAstralCinematicPayload::durationTicks,
            ByteBufCodecs.BOOL, StartAstralCinematicPayload::isAscendingToB,
            StartAstralCinematicPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartAstralCinematicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AstralCinematicClient.startCinematic(payload.entityId(), payload.durationTicks(), payload.isAscendingToB());
        });
    }
}
