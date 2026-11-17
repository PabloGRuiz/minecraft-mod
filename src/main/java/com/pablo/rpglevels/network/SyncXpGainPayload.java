package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncXpGainPayload(int gainedAmount, int currentXp, int maxXp, int currentLevel, boolean atCap) implements CustomPacketPayload {
    public static final Type<SyncXpGainPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_xp_gain"));

    public static final StreamCodec<ByteBuf, SyncXpGainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncXpGainPayload::gainedAmount,
            ByteBufCodecs.VAR_INT, SyncXpGainPayload::currentXp,
            ByteBufCodecs.VAR_INT, SyncXpGainPayload::maxXp,
            ByteBufCodecs.VAR_INT, SyncXpGainPayload::currentLevel,
            ByteBufCodecs.BOOL, SyncXpGainPayload::atCap,
            SyncXpGainPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncXpGainPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            com.pablo.rpglevels.client.gui.RpgXpHudClientEvents.onXpGainReceived(
                    payload.gainedAmount(),
                    payload.currentXp(),
                    payload.maxXp(),
                    payload.currentLevel(),
                    payload.atCap()
            );
        });
    }
}
