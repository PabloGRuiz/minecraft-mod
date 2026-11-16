package com.pablo.rpglevels.quests;

import com.pablo.rpglevels.data.AdventurerRank;
import net.minecraft.world.item.Items;

import java.util.*;

public class QuestRegistry {

    private static final Map<String, Quest> QUESTS = new LinkedHashMap<>();

    public static final Quest ALICE_SPIRITUAL_TRIAL = register(new Quest(
            "alice_spiritual_trial",
            "rpglevels.quest.alice_trial.title",
            "rpglevels.quest.alice_trial.desc",
            AdventurerRank.F,
            QuestType.KILL_MOBS_BY_RANK,
            5,
            2, // Rank D or higher (Brujas, Hoglins, Ahogados con tridente, etc.)
            500,
            com.pablo.rpglevels.items.ModItems.SPIRITUAL_SWORD.get(),
            1
    ));

    public static final Quest NOVICE_HUNT = register(new Quest(
            "novice_hunt",
            "rpglevels.quest.novice_hunt.title",
            "rpglevels.quest.novice_hunt.desc",
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
            "rpglevels.quest.beast_trapper.title",
            "rpglevels.quest.beast_trapper.desc",
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
            "rpglevels.quest.cave_purge.title",
            "rpglevels.quest.cave_purge.desc",
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
            "rpglevels.quest.arcane_awakening.title",
            "rpglevels.quest.arcane_awakening.desc",
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
            "rpglevels.quest.abyssal_slayer.title",
            "rpglevels.quest.abyssal_slayer.desc",
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
            "rpglevels.quest.cultist_hunt.title",
            "rpglevels.quest.cultist_hunt.desc",
            AdventurerRank.B,
            QuestType.KILL_SPECIFIC_MOB,
            1,
            0,
            2500,
            com.pablo.rpglevels.items.ModItems.CULTIST_SPIRITUAL_SWORD.get(),
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

    public static List<Quest> getQuestsForRank(AdventurerRank rank) {
        List<Quest> result = new ArrayList<>();
        for (Quest q : QUESTS.values()) {
            if (rank.getIndex() >= q.requiredRank().getIndex()) {
                result.add(q);
            }
        }
        return result;
    }
}
