package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncManaPayload(float currentMana, float maxMana) implements CustomPacketPayload {

    public static final Type<SyncManaPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_mana"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncManaPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, SyncManaPayload::currentMana,
                    ByteBufCodecs.FLOAT, SyncManaPayload::maxMana,
                    SyncManaPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncManaPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            com.pablo.rpglevels.client.gui.ManaHudClientEvents.updateMana(payload.currentMana(), payload.maxMana());
        });
    }
}
