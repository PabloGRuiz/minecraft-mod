package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.magic.beam.BeamClashManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Servidor -> Cliente: Notifica el desenlace y explosión de un Choque de Poderes (Beam Clash).
 */
public record EndBeamClashPayload(
        UUID clashId,
        int winnerId,
        int loserId,
        double clashX,
        double clashY,
        double clashZ,
        boolean wasTimeout
) implements CustomPacketPayload {

    public static final Type<EndBeamClashPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "end_beam_clash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EndBeamClashPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, val) -> {
                        buf.writeUUID(val.clashId);
                        buf.writeInt(val.winnerId);
                        buf.writeInt(val.loserId);
                        buf.writeDouble(val.clashX);
                        buf.writeDouble(val.clashY);
                        buf.writeDouble(val.clashZ);
                        buf.writeBoolean(val.wasTimeout);
                    },
                    buf -> new EndBeamClashPayload(
                            buf.readUUID(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EndBeamClashPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.pablo.tenseicraft.client.events.BeamClashCameraClientEvents.triggerAftermath(payload);
            }
            BeamClashManager.handleClientEnd(payload);
        });
    }
}
