package com.pablo.tenseicraft.quests;

import com.pablo.tenseicraft.data.AdventurerRank;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public record Quest(
        String id,
        String titleKey,
        String descKey,
        @Nullable String objectiveKey,
        AdventurerRank requiredRank,
        QuestType type,
        int targetCount,
        int targetRankIndex,
        int rewardXp,
        @Nullable Item rewardItem,
        int rewardItemCount,
        int rewardReputation,
        @Nullable Item requiredItem,
        boolean isDaily,
        boolean isMain,
        @Nullable String prerequisiteId) {
    // Constructor de compatibilidad para contratos diarios y secundarios
    public Quest(String id, String titleKey, String descKey, AdventurerRank requiredRank,
            QuestType type, int targetCount, int targetRankIndex, int rewardXp,
            @Nullable Item rewardItem, int rewardItemCount, int rewardReputation,
            @Nullable Item requiredItem, boolean isDaily) {
        this(id, titleKey, descKey, null, requiredRank, type, targetCount, targetRankIndex,
                rewardXp, rewardItem, rewardItemCount, rewardReputation, requiredItem, isDaily, false, null);
    }

    // Constructor de compatibilidad para misiones estáticas existentes
    public Quest(String id, String titleKey, String descKey, AdventurerRank requiredRank,
            QuestType type, int targetCount, int targetRankIndex, int rewardXp,
            @Nullable Item rewardItem, int rewardItemCount) {
        this(id, titleKey, descKey, null, requiredRank, type, targetCount, targetRankIndex,
                rewardXp, rewardItem, rewardItemCount, 15, null, false, false, null);
    }

    public static Quest mainQuest(String id, String titleKey, String descKey, String objectiveKey,
            int targetCount, @Nullable String prerequisiteId) {
        return new Quest(id, titleKey, descKey, objectiveKey, AdventurerRank.F, QuestType.MAIN_MILESTONE,
                targetCount, 0, 0, null, 0, 0, null,
                false, true, prerequisiteId);
    }

    public Component getTitle() {
        if (titleKey != null && (titleKey.startsWith("tenseicraft.") || titleKey.startsWith("quest."))) {
            return Component.translatable(titleKey);
        }
        return Component.literal(titleKey != null ? titleKey : id);
    }

    public Component getDescription() {
        if (descKey != null && (descKey.startsWith("tenseicraft.") || descKey.startsWith("quest."))) {
            return Component.translatable(descKey);
        }
        return Component.literal(descKey != null ? descKey : "");
    }

    public Component getObjective() {
        if (objectiveKey != null && !objectiveKey.isEmpty()) {
            if (objectiveKey.startsWith("tenseicraft.") || objectiveKey.startsWith("quest.")) {
                return Component.translatable(objectiveKey);
            }
            return Component.literal(objectiveKey);
        }
        return getDescription();
    }

    public ItemStack createRewardItemStack() {
        if (rewardItem != null && rewardItem != Items.AIR && rewardItemCount > 0) {
            return new ItemStack(rewardItem, rewardItemCount);
        }
        return ItemStack.EMPTY;
    }
}
