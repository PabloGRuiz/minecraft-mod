package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenPartyScreenPayload() implements CustomPacketPayload {

    public static final Type<OpenPartyScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "open_party_screen"));

    public static final StreamCodec<ByteBuf, OpenPartyScreenPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenPartyScreenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenPartyScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                com.pablo.rpglevels.client.gui.PartyClientHandler.openPartyScreen();
            }
        });
    }
}

