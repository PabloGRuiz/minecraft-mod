package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.quests.PlayerQuestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncQuestDataPayload(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SyncQuestDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_quest_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncQuestDataPayload> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG,
                    SyncQuestDataPayload::tag,
                    SyncQuestDataPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncQuestDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                PlayerQuestData data = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                if (data != null) {
                    data.deserializeNBT(player.registryAccess(), payload.tag());
                }
            }
        });
    }
}
