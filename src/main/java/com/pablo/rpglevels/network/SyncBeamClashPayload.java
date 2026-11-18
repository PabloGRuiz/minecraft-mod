package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.magic.beam.BeamClashManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Servidor -> Cliente: Sincroniza el estado del Choque de Poderes (Beam Clash)
 * para los participantes y observadores cercanos, incluyendo el retardo de preparación (warmup).
 */
public record SyncBeamClashPayload(
        UUID clashId,
        int entityAId,
        int entityBId,
        float scoreA,
        float scoreB,
        float progress,
        double clashX,
        double clashY,
        double clashZ,
        int remainingTicks,
        int warmupRemainingTicks,
        float manaA,
        float manaB,
        int colorA,
        int colorB
) implements CustomPacketPayload {

    public static final Type<SyncBeamClashPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_beam_clash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBeamClashPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, val) -> {
                        buf.writeUUID(val.clashId);
                        buf.writeInt(val.entityAId);
                        buf.writeInt(val.entityBId);
                        buf.writeFloat(val.scoreA);
                        buf.writeFloat(val.scoreB);
                        buf.writeFloat(val.progress);
                        buf.writeDouble(val.clashX);
                        buf.writeDouble(val.clashY);
                        buf.writeDouble(val.clashZ);
                        buf.writeInt(val.remainingTicks);
                        buf.writeInt(val.warmupRemainingTicks);
                        buf.writeFloat(val.manaA);
                        buf.writeFloat(val.manaB);
                        buf.writeInt(val.colorA);
                        buf.writeInt(val.colorB);
                    },
                    buf -> new SyncBeamClashPayload(
                            buf.readUUID(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readFloat(),
                            buf.readFloat(),
                            buf.readInt(),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncBeamClashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BeamClashManager.handleClientSync(payload);
        });
    }
}
