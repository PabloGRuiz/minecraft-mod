package com.pablo.rpglevels.quests;

import com.pablo.rpglevels.data.AdventurerRank;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public record Quest(
        String id,
        String titleKey,
        String descKey,
        AdventurerRank requiredRank,
        QuestType type,
        int targetCount,
        int targetRankIndex,
        int rewardXp,
        @Nullable Item rewardItem,
        int rewardItemCount,
        int rewardReputation,
        @Nullable Item requiredItem,
        boolean isDaily
) {
    // Constructor de compatibilidad para misiones estáticas existentes
    public Quest(String id, String titleKey, String descKey, AdventurerRank requiredRank,
                 QuestType type, int targetCount, int targetRankIndex, int rewardXp,
                 @Nullable Item rewardItem, int rewardItemCount) {
        this(id, titleKey, descKey, requiredRank, type, targetCount, targetRankIndex,
                rewardXp, rewardItem, rewardItemCount, 15, null, false);
    }

    public Component getTitle() {
        if (titleKey != null && (titleKey.startsWith("rpglevels.") || titleKey.startsWith("quest."))) {
            return Component.translatable(titleKey);
        }
        return Component.literal(titleKey != null ? titleKey : id);
    }

    public Component getDescription() {
        if (descKey != null && (descKey.startsWith("rpglevels.") || descKey.startsWith("quest."))) {
            return Component.translatable(descKey);
        }
        return Component.literal(descKey != null ? descKey : "");
    }

    public ItemStack createRewardItemStack() {
        if (rewardItem != null && rewardItem != Items.AIR && rewardItemCount > 0) {
            return new ItemStack(rewardItem, rewardItemCount);
        }
        return ItemStack.EMPTY;
    }
}
