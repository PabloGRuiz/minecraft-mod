package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.quests.PlayerQuestData;
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
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "quest_action"));

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
                if (data == null) {
                    return;
                }

                String qId = payload.questId();
                boolean isAliceQuest = "alice_spiritual_trial".equals(qId) || "alice_cultist_hunt".equals(qId);
                if (isAliceQuest) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§d[Segadores Espirituales] ¡Debes hablar directamente con Alice para gestionar esta misión!"));
                    return;
                }

                com.pablo.tenseicraft.quests.Quest targetQuest = com.pablo.tenseicraft.quests.QuestRegistry.getQuest(qId);
                boolean isMainQuest = (targetQuest != null && targetQuest.isMain());

                if (payload.actionType() == ACTION_ACCEPT) {
                    if (isMainQuest) {
                        String prereq = targetQuest.prerequisiteId();
                        if (prereq == null || data.isQuestClaimed(prereq)) {
                            data.acceptQuest(qId);
                            com.pablo.tenseicraft.events.QuestEvents.syncMainQuests(player, false);
                        }
                    } else {
                        com.pablo.tenseicraft.data.PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
                        if (rpgData == null || !rpgData.isGuildRegistered()) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c¡Debes inscribirte primero con el Maestro del Gremio!"));
                            return;
                        }

                        var nearbyMasters = player.serverLevel().getEntitiesOfClass(
                                com.pablo.tenseicraft.entities.GuildMasterNpcEntity.class,
                                player.getBoundingBox().inflate(8.0)
                        );
                        if (nearbyMasters.isEmpty()) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c¡Debes hablar en persona con un Maestro del Gremio en cualquier aldea para aceptar esta misión!"));
                            return;
                        }

                        // Asegura que los contratos diarios de hoy estén registrados en el servidor antes de aceptar
                        com.pablo.tenseicraft.quests.DailyQuestManager.getDailyQuests(player.serverLevel(), rpgData.getRank());
                        data.acceptQuest(qId);
                    }
                } else if (payload.actionType() == ACTION_CLAIM) {
                    if (!isMainQuest) {
                        var nearbyMasters = player.serverLevel().getEntitiesOfClass(
                                com.pablo.tenseicraft.entities.GuildMasterNpcEntity.class,
                                player.getBoundingBox().inflate(8.0)
                        );
                        if (nearbyMasters.isEmpty()) {
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c¡Debes hablar en persona con un Maestro del Gremio en cualquier aldea para entregar esta misión!"));
                            return;
                        }
                    }

                    boolean success = data.claimReward(qId, player);
                    if (!success && targetQuest != null && targetQuest.requiredItem() != null) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§c¡No tienes suficientes objetos requeridos en tu inventario para entregar este encargo!"));
                    }
                }
                ModNetworking.syncQuestsToPlayer(player);
            }
        });
    }
}
