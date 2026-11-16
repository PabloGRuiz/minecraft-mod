package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncRpgDataPayload(CompoundTag tag) implements CustomPacketPayload {
    public static final Type<SyncRpgDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_rpg_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRpgDataPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            SyncRpgDataPayload::tag,
            SyncRpgDataPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncRpgDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    data.deserializeNBT(player.registryAccess(), payload.tag());
                }
            }
        });
    }
}
