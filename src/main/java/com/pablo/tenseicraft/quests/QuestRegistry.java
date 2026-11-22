package com.pablo.tenseicraft.quests;

import com.pablo.tenseicraft.data.AdventurerRank;
import net.minecraft.world.item.Items;

import java.util.*;

public class QuestRegistry {

    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();

    // =========================================================================
    // MISIONES PRINCIPALES (HISTORIA OFICIAL DE TENSEICRAFT)
    // =========================================================================

    public static final Quest MAIN_01_WELCOME = register(Quest.mainQuest(
            "main_01_welcome",
            "tenseicraft.quest.main_01_welcome.title",
            "tenseicraft.quest.main_01_welcome.desc",
            "tenseicraft.quest.main_01_welcome.objective",
            1,
            null
    ));

    public static final Quest MAIN_02_ASCEND = register(Quest.mainQuest(
            "main_02_ascend",
            "tenseicraft.quest.main_02_ascend.title",
            "tenseicraft.quest.main_02_ascend.desc",
            "tenseicraft.quest.main_02_ascend.objective",
            1,
            "main_01_welcome"
    ));

    public static final Quest MAIN_03_ADVENTURER = register(Quest.mainQuest(
            "main_03_adventurer",
            "tenseicraft.quest.main_03_adventurer.title",
            "tenseicraft.quest.main_03_adventurer.desc",
            "tenseicraft.quest.main_03_adventurer.objective",
            1,
            "main_02_ascend"
    ));

    public static int computeMainQuestRpgXp(AdventurerRank rank, int currentLevel) {
        AdventurerRank safeRank = (rank != null) ? rank : AdventurerRank.F;
        int baseByRank = switch (safeRank) {
            case F -> 450;
            case E -> 2600;
            case D -> 7000;
            case C -> 14000;
            case B -> 23000;
            case A -> 35000;
        };
        int lvl = Math.max(1, currentLevel);
        int nextLevelXp = 80 + ((lvl - 1) * 35) + ((lvl - 1) * (lvl - 1) * 3);
        return Math.max(baseByRank, Math.round(nextLevelXp * 1.65F));
    }

    public static int computeMainQuestVanillaXp(AdventurerRank rank) {
        AdventurerRank safeRank = (rank != null) ? rank : AdventurerRank.F;
        return switch (safeRank) {
            case F -> 120;
            case E -> 250;
            case D -> 450;
            case C -> 750;
            case B -> 1200;
            case A -> 2000;
        };
    }

    // =========================================================================
    // MISIONES SECUNDARIAS DEL GREMIO Y DE LOS SEGADORES
    // =========================================================================

    public static final Quest ALICE_SPIRITUAL_TRIAL = register(new Quest(
            "alice_spiritual_trial",
            "tenseicraft.quest.alice_trial.title",
            "tenseicraft.quest.alice_trial.desc",
            AdventurerRank.F,
            QuestType.KILL_MOBS_BY_RANK,
            5,
            2, // Rank D or higher (Brujas, Hoglins, Ahogados con tridente, etc.)
            500,
            com.pablo.tenseicraft.items.ModItems.SPIRITUAL_SWORD.get(),
            1
    ));

    public static final Quest NOVICE_HUNT = register(new Quest(
            "novice_hunt",
            "tenseicraft.quest.novice_hunt.title",
            "tenseicraft.quest.novice_hunt.desc",
            AdventurerRank.F,
            QuestType.KILL_MOBS_BY_RANK,
            5,
            0, // Rank F or higher
            150,
            Items.IRON_INGOT,
            5
    ));

    public static final Quest BEAST_TRAPPER = register(new Quest(
            "beast_trapper",
            "tenseicraft.quest.beast_trapper.title",
            "tenseicraft.quest.beast_trapper.desc",
            AdventurerRank.F,
            QuestType.HUNT_ANIMALS,
            6,
            0,
            150,
            Items.LEATHER,
            4
    ));

    public static final Quest CAVE_PURGE = register(new Quest(
            "cave_purge",
            "tenseicraft.quest.cave_purge.title",
            "tenseicraft.quest.cave_purge.desc",
            AdventurerRank.E,
            QuestType.KILL_MOBS_BY_RANK,
            3,
            2, // Rank D or higher
            350,
            Items.GOLD_INGOT,
            3
    ));

    public static final Quest ARCANE_AWAKENING = register(new Quest(
            "arcane_awakening",
            "tenseicraft.quest.arcane_awakening.title",
            "tenseicraft.quest.arcane_awakening.desc",
            AdventurerRank.E,
            QuestType.EVOLVE_ACCESSORY,
            1,
            0,
            500,
            Items.EMERALD,
            3
    ));

    public static final Quest ABYSSAL_SLAYER = register(new Quest(
            "abyssal_slayer",
            "tenseicraft.quest.abyssal_slayer.title",
            "tenseicraft.quest.abyssal_slayer.desc",
            AdventurerRank.D,
            QuestType.KILL_MOBS_BY_RANK,
            3,
            3, // Rank C or higher
            800,
            Items.DIAMOND,
            2
    ));

    public static final Quest ALICE_CULTIST_HUNT = register(new Quest(
            "alice_cultist_hunt",
            "tenseicraft.quest.cultist_hunt.title",
            "tenseicraft.quest.cultist_hunt.desc",
            AdventurerRank.B,
            QuestType.KILL_SPECIFIC_MOB,
            1,
            0,
            2500,
            com.pablo.tenseicraft.items.ModItems.CULTIST_SPIRITUAL_SWORD.get(),
            1
    ));

    public static Quest register(Quest quest) {
        QUESTS.put(quest.id(), quest);
        return quest;
    }

    public static Quest getQuest(String id) {
        return QUESTS.get(id);
    }

    public static Collection<Quest> getAllQuests() {
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static List<Quest> getMainQuests() {
        List<Quest> result = new ArrayList<>();
        for (Quest q : QUESTS.values()) {
            if (q.isMain()) {
                result.add(q);
            }
        }
        return result;
    }

    public static List<Quest> getQuestsForRank(AdventurerRank rank) {
        List<Quest> result = new ArrayList<>();
        for (Quest q : QUESTS.values()) {
            if (!q.isMain() && !q.isDaily() && rank.getIndex() >= q.requiredRank().getIndex()) {
                result.add(q);
            }
        }
        return result;
    }
}
