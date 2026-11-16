package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.gui.QuestHudClientEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuestToastPayload(String title, String progressText, boolean isCompleted) implements CustomPacketPayload {

    public static final Type<QuestToastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "quest_toast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestToastPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    QuestToastPayload::title,
                    ByteBufCodecs.STRING_UTF8,
                    QuestToastPayload::progressText,
                    ByteBufCodecs.BOOL,
                    QuestToastPayload::isCompleted,
                    QuestToastPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestToastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> QuestHudClientEvents.triggerToast(payload.title(), payload.progressText(), payload.isCompleted()));
    }
}
