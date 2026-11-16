package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.quests.PlayerQuestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record QuestActionPayload(String questId, int actionType) implements CustomPacketPayload {

    public static final int ACTION_ACCEPT = 0;
    public static final int ACTION_CLAIM = 1;

    public static final Type<QuestActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "quest_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    QuestActionPayload::questId,
                    ByteBufCodecs.VAR_INT,
                    QuestActionPayload::actionType,
                    QuestActionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerQuestData data = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                if (data != null) {
                    if (payload.actionType() == ACTION_ACCEPT) {
                        data.acceptQuest(payload.questId());
                        if ("alice_spiritual_trial".equals(payload.questId())) {
                            com.pablo.rpglevels.data.PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
                            if (rpgData != null) {
                                rpgData.setAliceQuestActive(true);
                                ModNetworking.syncToPlayer(player);
                            }
                        }
                    } else if (payload.actionType() == ACTION_CLAIM) {
                        if (!"alice_spiritual_trial".equals(payload.questId()) && !"alice_cultist_hunt".equals(payload.questId())) {
                            var nearbyMasters = player.serverLevel().getEntitiesOfClass(
                                    com.pablo.rpglevels.entities.GuildMasterNpcEntity.class,
                                    player.getBoundingBox().inflate(8.0)
                            );
                            if (nearbyMasters.isEmpty()) {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c¡Debes hablar en persona con un Maestro del Gremio en cualquier aldea para entregar esta misión!"));
                                return;
                            }
                        }
                        boolean success = data.claimReward(payload.questId(), player);
                        if (!success) {
                            var q = com.pablo.rpglevels.quests.QuestRegistry.getQuest(payload.questId());
                            if (q != null && q.requiredItem() != null) {
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c¡No tienes suficientes objetos requeridos en tu inventario para entregar este encargo!"));
                            }
                        }
                    }
                    ModNetworking.syncQuestsToPlayer(player);
                }
            }
        });
    }
}
