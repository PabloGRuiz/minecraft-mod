package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.party.PartyManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record PartyActionPayload(int actionType, String targetData) implements CustomPacketPayload {

    public static final int ACTION_CREATE = 1;
    public static final int ACTION_INVITE = 2;
    public static final int ACTION_ACCEPT = 3;
    public static final int ACTION_LEAVE = 4;
    public static final int ACTION_DISBAND = 5;
    public static final int ACTION_KICK = 6;

    public static final Type<PartyActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "party_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PartyActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    PartyActionPayload::actionType,
                    ByteBufCodecs.STRING_UTF8,
                    PartyActionPayload::targetData,
                    PartyActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartyActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            switch (payload.actionType()) {
                case ACTION_CREATE -> PartyManager.createParty(player);
                case ACTION_INVITE -> {
                    ServerPlayer target = player.server.getPlayerList().getPlayerByName(payload.targetData());
                    if (target != null) {
                        PartyManager.invitePlayer(player, target);
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cJugador '" + payload.targetData() + "' no encontrado o desconectado."));
                    }
                }
                case ACTION_ACCEPT -> PartyManager.acceptInvite(player);
                case ACTION_LEAVE -> PartyManager.leaveParty(player);
                case ACTION_DISBAND -> PartyManager.disbandParty(player);
                case ACTION_KICK -> {
                    try {
                        UUID targetUUID = UUID.fromString(payload.targetData());
                        PartyManager.kickMember(player, targetUUID);
                    } catch (IllegalArgumentException e) {
                        ServerPlayer target = player.server.getPlayerList().getPlayerByName(payload.targetData());
                        if (target != null) {
                            PartyManager.kickMember(player, target.getUUID());
                        }
                    }
                }
            }
        });
    }
}

