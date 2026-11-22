package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenReforgeScreenPayload(boolean isMagicAnvil, int meguEntityId, BlockPos anvilPos)
        implements CustomPacketPayload {

    public static final Type<OpenReforgeScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "open_reforge_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenReforgeScreenPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, OpenReforgeScreenPayload::isMagicAnvil,
                    ByteBufCodecs.VAR_INT, OpenReforgeScreenPayload::meguEntityId,
                    BlockPos.STREAM_CODEC, OpenReforgeScreenPayload::anvilPos,
                    OpenReforgeScreenPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenReforgeScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.pablo.tenseicraft.client.gui.ReforgeClientHandler.openReforgeScreen(
                        payload.isMagicAnvil(),
                        payload.meguEntityId(),
                        payload.anvilPos()
                );
            }
        });
    }
}
