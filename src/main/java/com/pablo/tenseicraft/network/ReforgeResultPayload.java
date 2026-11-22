package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ReforgeResultPayload(
        boolean success,
        float winChancePct,
        float finalAngleDeg,
        String resultSummary
) implements CustomPacketPayload {

    public static final Type<ReforgeResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "reforge_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ReforgeResultPayload::success,
                    ByteBufCodecs.FLOAT, ReforgeResultPayload::winChancePct,
                    ByteBufCodecs.FLOAT, ReforgeResultPayload::finalAngleDeg,
                    ByteBufCodecs.STRING_UTF8, ReforgeResultPayload::resultSummary,
                    ReforgeResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReforgeResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.pablo.tenseicraft.client.gui.ReforgeClientHandler.onReforgeResult(
                        payload.success(),
                        payload.winChancePct(),
                        payload.finalAngleDeg(),
                        payload.resultSummary()
                );
            }
        });
    }
}
