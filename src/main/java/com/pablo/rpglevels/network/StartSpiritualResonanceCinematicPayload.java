package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.cinematics.SpiritualResonanceCinematicClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartSpiritualResonanceCinematicPayload(int entityId, int durationTicks, int shockwaveTick) implements CustomPacketPayload {

    public static final Type<StartSpiritualResonanceCinematicPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "start_spiritual_resonance_cinematic"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartSpiritualResonanceCinematicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StartSpiritualResonanceCinematicPayload::entityId,
            ByteBufCodecs.VAR_INT, StartSpiritualResonanceCinematicPayload::durationTicks,
            ByteBufCodecs.VAR_INT, StartSpiritualResonanceCinematicPayload::shockwaveTick,
            StartSpiritualResonanceCinematicPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartSpiritualResonanceCinematicPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            SpiritualResonanceCinematicClient.startCinematic(payload.entityId(), payload.durationTicks(), payload.shockwaveTick());
        });
    }
}
