package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.network.ModNetworking;
import com.pablo.rpglevels.quests.*;
import com.pablo.rpglevels.world.kingdom.KingdomVillageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class QuestEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initPlayerDailyQuests(player);
            ModNetworking.syncQuestsToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initPlayerDailyQuests(player);
            ModNetworking.syncQuestsToPlayer(player);
        }
    }

    private static void initPlayerDailyQuests(ServerPlayer player) {
        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (questData != null && player.level() != null) {
            long day = player.level().getDayTime() / 24000L;
            questData.checkDailyReset(day);
            AdventurerRank rank = (rpgData != null && rpgData.isGuildRegistered()) ? rpgData.getRank() : AdventurerRank.F;
            DailyQuestManager.getDailyQuests(player.level(), rank);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Escalonar la verificación de ubicación y reseteo diario entre jugadores cada 60 ticks (3 segundos)
            if ((player.tickCount + player.getId()) % 60 == 0) {
                KingdomVillageManager.checkPlayerLocation(player);
                PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
                if (questData != null && player.level() != null) {
                    long day = player.level().getDayTime() / 24000L;
                    questData.checkDailyReset(day);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
        if (questData == null) {
            return;
        }

        LivingEntity victim = event.getEntity();
        ThreatLevel threat = ThreatLevel.determineThreatLevel(victim);
        int victimRankIndex = threat.ordinal();
        boolean isEnemy = victim instanceof Enemy || victim instanceof Monster;
        boolean isAnimal = (victim instanceof Animal) && !(victim instanceof Enemy);
        boolean nearSettlement = KingdomVillageManager.isNearSettlement(victim.level(), victim.blockPosition(), 64.0);

        // Auto-sincronizar misión de Alice en PlayerQuestData si se inició en PlayerRpgData
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (rpgData != null && rpgData.isAliceQuestActive() && !rpgData.isAliceQuestCompleted()) {
            if (!questData.isQuestActive("alice_spiritual_trial") && !questData.isQuestClaimed("alice_spiritual_trial")) {
                questData.acceptQuest("alice_spiritual_trial");
                int currentKills = rpgData.getAliceDMonstersKilled();
                if (currentKills > 0) {
                    questData.setProgress("alice_spiritual_trial", currentKills);
                }
                ModNetworking.syncQuestsToPlayer(player);
            }
        }

        for (Map.Entry<String, Integer> entry : questData.getActiveQuests().entrySet()) {
            String questId = entry.getKey();
            Quest quest = QuestRegistry.getQuest(questId);
            if (quest == null || questData.isCompleted(questId, player)) {
                continue;
            }

            boolean matched = false;

            if (quest.type() == QuestType.KILL_MOBS_BY_RANK && isEnemy) {
                if (victimRankIndex >= quest.targetRankIndex()) {
                    matched = true;
                }
            } else if (quest.type() == QuestType.HUNT_ANIMALS && isAnimal) {
                matched = true;
            } else if (quest.type() == QuestType.PATROL_VILLAGE && isEnemy && nearSettlement) {
                matched = true;
            } else if (quest.type() == QuestType.KILL_SPECIFIC_MOB) {
                if ("alice_cultist_hunt".equals(questId) && victim instanceof com.pablo.rpglevels.entities.CultistBossEntity) {
                    matched = true;
                } else if (questId.startsWith("daily_bandit_") && (victim instanceof com.pablo.rpglevels.entities.BanditEntity || victim instanceof AbstractIllager)) {
                    matched = true;
                }
            }

            if (matched) {
                boolean progressed = questData.addProgress(questId, 1);
                if (progressed) {
                    int cur = questData.getProgress(questId);
                    boolean completed = questData.isCompleted(questId);
                    ModNetworking.sendQuestToast(player, quest.getTitle().getString(), cur + "/" + quest.targetCount(), completed);
                    ModNetworking.syncQuestsToPlayer(player);

                    // Sincronizar específicamente la misión de Alice con PlayerRpgData
                    if ("alice_spiritual_trial".equals(questId) && rpgData != null) {
                        rpgData.setAliceDMonstersKilled(cur);
                        player.sendSystemMessage(Component.literal("§e[Segadores Espirituales] Monstruos Rango D eliminados: §a" + cur + "/5"));
                        if (completed) {
                            rpgData.setAliceQuestCompleted(true);
                            player.sendSystemMessage(Component.empty());
                            player.sendSystemMessage(Component.literal("§6« ¡ENCARGO DE LOS SEGADORES COMPLETADO! »").withStyle(ChatFormatting.BOLD));
                            player.sendSystemMessage(Component.literal("§aHas demostrado tu temple eliminando a los 5 monstruos de Rango D. Regresa con Alice para reclamar tu Espada Espiritual."));
                            player.sendSystemMessage(Component.empty());
                            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                        }
                        ModNetworking.syncToPlayer(player);
                    }
                }
            }
        }
    }

    public static void onAccessoryEvolved(ServerPlayer player) {
        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
        if (questData == null) return;

        for (Map.Entry<String, Integer> entry : questData.getActiveQuests().entrySet()) {
            String questId = entry.getKey();
            Quest quest = QuestRegistry.getQuest(questId);
            if (quest == null || questData.isCompleted(questId)) continue;

            if (quest.type() == QuestType.EVOLVE_ACCESSORY) {
                boolean progressed = questData.addProgress(questId, 1);
                if (progressed) {
                    int cur = questData.getProgress(questId);
                    boolean completed = questData.isCompleted(questId);
                    ModNetworking.sendQuestToast(player, quest.getTitle().getString(), cur + "/" + quest.targetCount(), completed);
                    ModNetworking.syncQuestsToPlayer(player);
                }
            }
        }
    }
}
