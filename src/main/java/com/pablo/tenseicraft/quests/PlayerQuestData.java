package com.pablo.tenseicraft.quests;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.*;

public class PlayerQuestData implements INBTSerializable<CompoundTag> {

    private final Map<String, Integer> activeQuests = new LinkedHashMap<>();
    private final Set<String> claimedQuests = new HashSet<>();
    private long lastDailyDay = -1;

    public PlayerQuestData() {
    }

    public long getLastDailyDay() {
        return lastDailyDay;
    }

    public void checkDailyReset(long currentDay) {
        if (this.lastDailyDay != -1 && currentDay > this.lastDailyDay) {
            // Remover misiones diarias expiradas de días anteriores
            this.activeQuests.keySet().removeIf(id -> id.startsWith("daily_"));
            this.claimedQuests.removeIf(id -> id.startsWith("daily_"));
        }
        this.lastDailyDay = currentDay;
    }

    public Map<String, Integer> getActiveQuests() {
        return Collections.unmodifiableMap(activeQuests);
    }

    public Set<String> getClaimedQuests() {
        return Collections.unmodifiableSet(claimedQuests);
    }

    public boolean isQuestActive(String questId) {
        return activeQuests.containsKey(questId);
    }

    public boolean isQuestClaimed(String questId) {
        return claimedQuests.contains(questId);
    }

    public int getProgress(String questId) {
        return activeQuests.getOrDefault(questId, 0);
    }

    public void resetAllQuests() {
        this.activeQuests.clear();
        this.claimedQuests.clear();
    }

    public boolean acceptQuest(String questId) {
        if (isQuestActive(questId) || isQuestClaimed(questId)) {
            return false;
        }
        Quest quest = QuestRegistry.getQuest(questId);
        if (quest == null) {
            return false;
        }
        activeQuests.put(questId, 0);
        return true;
    }

    public boolean addProgress(String questId, int amount) {
        if (!isQuestActive(questId)) {
            return false;
        }
        Quest quest = QuestRegistry.getQuest(questId);
        if (quest == null) {
            return false;
        }

        int current = getProgress(questId);
        if (current >= quest.targetCount()) {
            return false;
        }

        int updated = Math.min(quest.targetCount(), current + amount);
        activeQuests.put(questId, updated);
        return true;
    }

    public void setProgress(String questId, int progress) {
        if (isQuestActive(questId)) {
            Quest quest = QuestRegistry.getQuest(questId);
            int max = (quest != null) ? quest.targetCount() : progress;
            activeQuests.put(questId, Math.min(max, Math.max(0, progress)));
        }
    }

    public boolean isCompleted(String questId) {
        return isCompleted(questId, null);
    }

    public boolean isCompleted(String questId, net.minecraft.world.entity.player.Player player) {
        Quest quest = QuestRegistry.getQuest(questId);
        if (quest == null || !isQuestActive(questId)) {
            return false;
        }
        if (quest.type() == QuestType.COLLECT_ITEMS && quest.requiredItem() != null) {
            if (player != null) {
                return player.getInventory().countItem(quest.requiredItem()) >= quest.targetCount();
            }
            return false;
        }
        return getProgress(questId) >= quest.targetCount();
    }

    public boolean claimReward(String questId, ServerPlayer player) {
        if (!isCompleted(questId, player) || isQuestClaimed(questId)) {
            return false;
        }

        Quest quest = QuestRegistry.getQuest(questId);
        if (quest == null) {
            return false;
        }

        // Si es misión de recolección/entrega de ítems, consumir del inventario
        if (quest.type() == QuestType.COLLECT_ITEMS && quest.requiredItem() != null) {
            if (player.getInventory().countItem(quest.requiredItem()) < quest.targetCount()) {
                return false;
            }
            int remaining = quest.targetCount();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack itemStack = player.getInventory().getItem(i);
                if (!itemStack.isEmpty() && itemStack.getItem() == quest.requiredItem()) {
                    int toTake = Math.min(remaining, itemStack.getCount());
                    itemStack.shrink(toTake);
                    remaining -= toTake;
                    if (remaining <= 0)
                        break;
                }
            }
        }

        // Move to claimed
        activeQuests.remove(questId);
        claimedQuests.add(questId);

        // Give XP (both vanilla XP and RPG XP if blessed by a God)
        if (quest.rewardXp() > 0) {
            player.giveExperiencePoints(quest.rewardXp());
            com.pablo.tenseicraft.data.PlayerRpgData rpgData = player
                    .getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA);
            if (rpgData != null && rpgData.hasDivineBlessingUnlocked()) {
                int rpgXpGain = Math.max(40, quest.rewardXp() / 3);
                rpgData.addRpgXp(player, rpgXpGain);
                String sourceLabel = quest.isMain() ? "la Misión Principal!" : "la misión del Gremio!";
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .literal("§b✦ +" + rpgXpGain + " XP RPG por completar " + sourceLabel));
            }
        }

        // Give items
        ItemStack rewardStack = quest.createRewardItemStack();
        if (!rewardStack.isEmpty()) {
            boolean added = player.getInventory().add(rewardStack);
            if (!added && !rewardStack.isEmpty()) {
                ItemEntity itemEntity = new ItemEntity(player.level(), player.getX(), player.getY() + 0.5,
                        player.getZ(), rewardStack);
                player.level().addFreshEntity(itemEntity);
            }
        }

        // Otorgar Reputación de Gremio
        if (quest.rewardReputation() > 0) {
            com.pablo.tenseicraft.data.PlayerRpgData rpgData = player
                    .getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA);
            if (rpgData != null) {
                rpgData.addReputation(quest.rewardReputation());
                player.sendSystemMessage(net.minecraft.network.chat.Component
                        .literal("§6✦ +" + quest.rewardReputation() + " Puntos de Reputación con el Gremio!")
                        .withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
                com.pablo.tenseicraft.network.ModNetworking.syncToPlayer(player);
            }
        }

        // Sound effect
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (quest.isMain()) {
            com.pablo.tenseicraft.events.QuestEvents.syncMainQuests(player, true);
        }
        return true;
    }

    public boolean completeMainQuestAuto(String questId, ServerPlayer player) {
        if (isQuestClaimed(questId)) {
            return false;
        }
        Quest quest = QuestRegistry.getQuest(questId);
        if (quest == null || !quest.isMain()) {
            return false;
        }

        activeQuests.remove(questId);
        claimedQuests.add(questId);

        com.pablo.tenseicraft.data.PlayerRpgData rpgData = player
                .getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA);
        com.pablo.tenseicraft.data.AdventurerRank rank = (rpgData != null)
                ? rpgData.getRank()
                : com.pablo.tenseicraft.data.AdventurerRank.F;
        int lvl = (rpgData != null) ? rpgData.getRpgLevel() : 1;

        int vanillaXp = QuestRegistry.computeMainQuestVanillaXp(rank);
        int rpgXp = QuestRegistry.computeMainQuestRpgXp(rank, lvl);

        player.giveExperiencePoints(vanillaXp);
        player.sendSystemMessage(net.minecraft.network.chat.Component.empty());
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§d§l✦ [MISIÓN PRINCIPAL COMPLETADA] §f" + quest.getTitle().getString()));

        if (rpgData != null && rpgData.hasDivineBlessingUnlocked()) {
            if (rpgData.isAtMilestoneCap()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a  +" + vanillaXp + " XP Vanilla §8| §7(Nivel RPG en el hito máximo de rango — duerme para ascender y seguir subiendo)"));
            } else {
                rpgData.addRpgXp(player, rpgXp);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§b  ✦ +" + rpgXp + " XP RPG §7(escalada a Rango " + rank.getName() + ") §8| §a+" + vanillaXp + " XP"));
            }
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§a  ✦ +" + vanillaXp + " XP §7(¡Has completado tu primer paso!)"));
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.empty());

        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        com.pablo.tenseicraft.network.ModNetworking.sendQuestToast(player, quest.getTitle().getString(), "¡Completada!", true);
        return true;
    }

    public void copyFrom(PlayerQuestData other) {
        this.activeQuests.clear();
        this.activeQuests.putAll(other.activeQuests);
        this.claimedQuests.clear();
        this.claimedQuests.addAll(other.claimedQuests);
        this.lastDailyDay = other.lastDailyDay;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LastDailyDay", lastDailyDay);

        ListTag activeList = new ListTag();
        for (Map.Entry<String, Integer> entry : activeQuests.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Id", entry.getKey());
            entryTag.putInt("Progress", entry.getValue());
            activeList.add(entryTag);
        }
        tag.put("ActiveQuests", activeList);

        ListTag claimedList = new ListTag();
        for (String id : claimedQuests) {
            claimedList.add(StringTag.valueOf(id));
        }
        tag.put("ClaimedQuests", claimedList);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        activeQuests.clear();
        claimedQuests.clear();

        if (tag.contains("LastDailyDay", Tag.TAG_LONG)) {
            this.lastDailyDay = tag.getLong("LastDailyDay");
        }

        if (tag.contains("ActiveQuests", Tag.TAG_LIST)) {
            ListTag activeList = tag.getList("ActiveQuests", Tag.TAG_COMPOUND);
            for (int i = 0; i < activeList.size(); i++) {
                CompoundTag entryTag = activeList.getCompound(i);
                String id = entryTag.getString("Id");
                int progress = entryTag.getInt("Progress");
                activeQuests.put(id, progress);
            }
        }

        if (tag.contains("ClaimedQuests", Tag.TAG_LIST)) {
            ListTag claimedList = tag.getList("ClaimedQuests", Tag.TAG_STRING);
            for (int i = 0; i < claimedList.size(); i++) {
                claimedQuests.add(claimedList.getString(i));
            }
        }
    }
}
