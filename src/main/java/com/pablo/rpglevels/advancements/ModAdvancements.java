package com.pablo.rpglevels.advancements;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ModAdvancements {

    public static final String ROOT = "root";
    public static final String FIND_GUILD = "find_guild";
    public static final String HIRE_ADVENTURER = "hire_adventurer";
    public static final String HIGH_AFFINITY = "high_affinity";
    public static final String RENAME_COMRADE = "rename_comrade";
    public static final String FIRST_ASCENSION = "first_ascension";
    public static final String RANK_B = "rank_b";

    public static final String MEET_ALICE = "meet_alice";
    public static final String OBTAIN_SPIRITUAL_SWORD = "obtain_spiritual_sword";
    public static final String FORGE_SOUL_BOND = "forge_soul_bond";
    public static final String FIRST_RELEASE = "first_release";
    public static final String MASTER_SWORD = "master_sword";

    public static final String DEFEAT_WIND_MASTER = "defeat_wind_master";
    public static final String DEFEAT_UNDEAD_ADVENTURER = "defeat_undead_adventurer";
    public static final String ENTER_ABYSS = "enter_abyss";
    public static final String DEFEAT_CULTIST = "defeat_cultist";

    public static void award(ServerPlayer player, String advancementName) {
        if (player == null || player.server == null) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, advancementName);
        AdvancementHolder holder = player.server.getAdvancements().get(id);
        if (holder != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(holder, criterion);
                }
            }
        }
    }
}

