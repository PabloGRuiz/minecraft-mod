package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.party.ClientPartyData;
import com.pablo.rpglevels.party.PartyMemberInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SyncPartyPayload(
        boolean inParty,
        boolean isLeader,
        String leaderName,
        int partyRankIndex,
        List<PartyMemberInfo> members
) implements CustomPacketPayload {

    public static final Type<SyncPartyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "sync_party"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPartyPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    SyncPartyPayload::inParty,
                    ByteBufCodecs.BOOL,
                    SyncPartyPayload::isLeader,
                    ByteBufCodecs.STRING_UTF8,
                    SyncPartyPayload::leaderName,
                    ByteBufCodecs.VAR_INT,
                    SyncPartyPayload::partyRankIndex,
                    PartyMemberInfo.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncPartyPayload::members,
                    SyncPartyPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPartyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientPartyData.update(payload.inParty(), payload.isLeader(), payload.leaderName(), payload.partyRankIndex(), payload.members());
        });
    }
}
