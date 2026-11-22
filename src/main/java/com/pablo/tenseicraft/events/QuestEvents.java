package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.quests.*;
import com.pablo.tenseicraft.world.kingdom.KingdomVillageManager;
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

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class QuestEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initPlayerDailyQuests(player);
            syncMainQuests(player, true);
            ModNetworking.syncQuestsToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initPlayerDailyQuests(player);
            syncMainQuests(player, true);
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

    public static void syncMainQuests(ServerPlayer player, boolean notifyNewChapter) {
        if (player == null) return;
        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (questData == null || rpgData == null) return;

        var mainQuests = QuestRegistry.getMainQuests();
        boolean changed = false;
        boolean shouldNotify = notifyNewChapter;

        for (int pass = 0; pass < mainQuests.size(); pass++) {
            boolean chained = false;
            for (Quest q : mainQuests) {
                if (questData.isQuestClaimed(q.id())) {
                    continue;
                }
                String prereq = q.prerequisiteId();
                if (prereq != null && !questData.isQuestClaimed(prereq)) {
                    break;
                }
                if (!isMainQuestUnlocked(rpgData, q.id())) {
                    break;
                }

                if (!questData.isQuestActive(q.id())) {
                    questData.acceptQuest(q.id());
                    changed = true;
                    if (shouldNotify) {
                        player.sendSystemMessage(Component.literal("§d★ §l[NUEVA MISIÓN PRINCIPAL] §e" + q.getTitle().getString()));
                        player.sendSystemMessage(Component.literal("§7  Objetivo: §f" + q.getObjective().getString() + " §8(Pulsa [J] para leer la historia completa)"));
                        ModNetworking.sendQuestToast(player, q.getTitle().getString(), "§dObjetivo: §f" + q.getObjective().getString(), false);
                    }
                }

                int target = Math.max(1, q.targetCount());
                int computed = Math.min(target, computeMainQuestProgress(rpgData, q.id()));
                int previous = questData.getProgress(q.id());

                if (computed >= target) {
                    if (questData.completeMainQuestAuto(q.id(), player)) {
                        changed = true;
                        shouldNotify = true;
                        chained = true;
                    }
                } else if (computed > previous) {
                    questData.setProgress(q.id(), computed);
                    changed = true;
                    ModNetworking.sendQuestToast(player, q.getTitle().getString(), computed + "/" + target, false);
                }
                break;
            }
            if (!chained) {
                break;
            }
        }

        if (changed) {
            ModNetworking.syncQuestsToPlayer(player);
        }
    }

    private static boolean isMainQuestUnlocked(PlayerRpgData rpgData, String questId) {
        return switch (questId) {
            case "main_01_welcome" -> true;
            case "main_02_ascend" -> rpgData.hasDivineBlessingUnlocked();
            case "main_03_adventurer" -> true;
            default -> true;
        };
    }

    private static int computeMainQuestProgress(PlayerRpgData rpgData, String questId) {
        return switch (questId) {
            case "main_01_welcome" -> rpgData.hasKilledFirstMonster() ? 1 : 0;
            case "main_02_ascend" -> rpgData.getRank().getIndex() >= AdventurerRank.E.getIndex() ? 1 : 0;
            case "main_03_adventurer" -> rpgData.isGuildRegistered() ? 1 : 0;
            default -> 0;
        };
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Sincronizar misiones principales cada 40 ticks (2 segundos)
            if ((player.tickCount + player.getId()) % 40 == 0) {
                syncMainQuests(player, true);
            }
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
        if (victim instanceof com.pablo.tenseicraft.entities.WindMasterBossEntity) {
            player.getPersistentData().putBoolean("DefeatedWindMasterBoss", true);
        } else if (victim instanceof com.pablo.tenseicraft.entities.UndeadAdventurerBossEntity) {
            player.getPersistentData().putBoolean("DefeatedUndeadBoss", true);
        } else if (victim instanceof com.pablo.tenseicraft.entities.CultistBossEntity) {
            player.getPersistentData().putBoolean("RpgKilledCultistBoss", true);
        }
        syncMainQuests(player, true);

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
                if ("alice_cultist_hunt".equals(questId) && victim instanceof com.pablo.tenseicraft.entities.CultistBossEntity) {
                    matched = true;
                } else if (questId.startsWith("daily_bandit_") && (victim instanceof com.pablo.tenseicraft.entities.BanditEntity || victim instanceof AbstractIllager)) {
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
