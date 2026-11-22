package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenAdventurerCardPayload(CompoundTag cardTag) implements CustomPacketPayload {

    public static final Type<OpenAdventurerCardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "open_adventurer_card"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAdventurerCardPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG, OpenAdventurerCardPayload::cardTag,
                    OpenAdventurerCardPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenAdventurerCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null && context.player().level().isClientSide()) {
                com.pablo.tenseicraft.client.gui.AdventurerCardClientHandler.openCardFromSnapshot(payload.cardTag());
            }
        });
    }
}

