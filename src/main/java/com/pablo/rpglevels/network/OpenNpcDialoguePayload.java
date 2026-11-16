package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenNpcDialoguePayload(
        int entityId,
        String npcName,
        String npcType,
        int skinVariant,
        int rankIndex,
        int affinity,
        boolean isHired,
        boolean isHiredByPlayer,
        long hireRemainingTicks,
        boolean isStaying,
        int workerStatus,
        boolean isDevoted,
        int vassalRank,
        boolean isEternalComrade,
        int stance,
        boolean companionReadyToAscend,
        String dialogueText
) implements CustomPacketPayload {

    public static final Type<OpenNpcDialoguePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "open_npc_dialogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNpcDialoguePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public OpenNpcDialoguePayload decode(RegistryFriendlyByteBuf buf) {
            int entityId = buf.readVarInt();
            String npcName = buf.readUtf();
            String npcType = buf.readUtf();
            int skinVariant = buf.readVarInt();
            int rankIndex = buf.readVarInt();
            int affinity = buf.readVarInt();
            boolean isHired = buf.readBoolean();
            boolean isHiredByPlayer = buf.readBoolean();
            long hireRemainingTicks = buf.readVarLong();
            boolean isStaying = buf.readBoolean();
            int workerStatus = buf.readVarInt();
            boolean isDevoted = buf.readBoolean();
            int vassalRank = buf.readVarInt();
            boolean isEternalComrade = buf.readBoolean();
            int stance = buf.readVarInt();
            boolean companionReadyToAscend = buf.readBoolean();
            String dialogueText = buf.readUtf();

            return new OpenNpcDialoguePayload(
                    entityId, npcName, npcType, skinVariant, rankIndex, affinity,
                    isHired, isHiredByPlayer, hireRemainingTicks, isStaying, workerStatus,
                    isDevoted, vassalRank, isEternalComrade, stance, companionReadyToAscend, dialogueText
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, OpenNpcDialoguePayload payload) {
            buf.writeVarInt(payload.entityId());
            buf.writeUtf(payload.npcName());
            buf.writeUtf(payload.npcType());
            buf.writeVarInt(payload.skinVariant());
            buf.writeVarInt(payload.rankIndex());
            buf.writeVarInt(payload.affinity());
            buf.writeBoolean(payload.isHired());
            buf.writeBoolean(payload.isHiredByPlayer());
            buf.writeVarLong(payload.hireRemainingTicks());
            buf.writeBoolean(payload.isStaying());
            buf.writeVarInt(payload.workerStatus());
            buf.writeBoolean(payload.isDevoted());
            buf.writeVarInt(payload.vassalRank());
            buf.writeBoolean(payload.isEternalComrade());
            buf.writeVarInt(payload.stance());
            buf.writeBoolean(payload.companionReadyToAscend());
            buf.writeUtf(payload.dialogueText());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenNpcDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.pablo.rpglevels.client.gui.NpcDialogueScreen screen && screen.getEntityId() == payload.entityId()) {
                screen.updateState(payload);
            } else {
                mc.setScreen(new com.pablo.rpglevels.client.gui.NpcDialogueScreen(payload));
            }
        });
    }
}
