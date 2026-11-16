package com.pablo.rpglevels.quests;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.items.ModItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DailyQuestManager {

    /**
     * Obtiene o genera el tablón de 5 misiones diarias para el día actual y rango del jugador.
     */
    public static List<Quest> getDailyQuests(Level level, AdventurerRank rank) {
        long day = (level != null) ? level.getDayTime() / 24000L : 0L;
        List<Quest> dailies = new ArrayList<>();

        // 1. Cacería de Amenaza según Rango
        dailies.add(generateDailyHuntQuest(day, rank));

        // 2. Recolección de Cristales de Alma para la Forja Humana
        dailies.add(generateDailySoulQuest(day, rank));

        // 3. Suministros y Recursos para la Armería
        dailies.add(generateDailySupplyQuest(day, rank));

        // 4. Contrato de Caza de Bandidos
        dailies.add(generateDailyBanditQuest(day, rank));

        // 5. Patrulla y Protección de Aldeas / Asentamientos
        dailies.add(generateDailyPatrolQuest(day, rank));

        // Registrar todas en el QuestRegistry para que sean accesibles por ID
        for (Quest q : dailies) {
            QuestRegistry.register(q);
        }

        return dailies;
    }

    private static Quest generateDailyHuntQuest(long day, AdventurerRank rank) {
        String id = "daily_hunt_" + day;
        return switch (rank) {
            case F -> new Quest(id,
                    "§c[Diaria] Caza Menor: Monstruos Rango F/E",
                    "Elimina monstruos menores que acechan los caminos en este día.",
                    rank, QuestType.KILL_MOBS_BY_RANK, 6, ThreatLevel.F.ordinal(),
                    250, Items.IRON_INGOT, 4, 15, null, true);
            case E -> new Quest(id,
                    "§c[Diaria] Purga de Bestias: Rango E o Superior",
                    "Da caza a monstruos agresivos de Rango E o superior.",
                    rank, QuestType.KILL_MOBS_BY_RANK, 5, ThreatLevel.E.ordinal(),
                    450, Items.IRON_INGOT, 6, 20, null, true);
            case D -> new Quest(id,
                    "§c[Diaria] Exterminio de Amenazas: Rango D",
                    "Elimina criaturas peligrosas (Endermans, Brujas, Creepers) de Rango D.",
                    rank, QuestType.KILL_MOBS_BY_RANK, 5, ThreatLevel.D.ordinal(),
                    750, Items.GOLD_INGOT, 4, 25, null, true);
            case C -> new Quest(id,
                    "§c[Diaria] Asalto Táctico: Rango C o Superior",
                    "Caza enemigos formidables de Rango C que amenazan los reinos.",
                    rank, QuestType.KILL_MOBS_BY_RANK, 4, ThreatLevel.C.ordinal(),
                    1300, Items.GOLD_INGOT, 8, 30, null, true);
            case B -> new Quest(id,
                    "§c[Diaria] Incursión del Abismo: Nacidos del Vacío",
                    "Elimina Nacidos del Vacío (Voidborn Rango B) o monstruos equivalentes.",
                    rank, QuestType.KILL_MOBS_BY_RANK, 3, ThreatLevel.B.ordinal(),
                    2200, Items.DIAMOND, 2, 40, null, true);
            case A -> new Quest(id,
                    "§c[Diaria] Caza Suprema: Amenazas Rango A / Hazard",
                    "Enfréntate a las criaturas más letales de la región (Rango A o Hazard).",
                    rank, QuestType.KILL_MOBS_BY_RANK, 2, ThreatLevel.A.ordinal(),
                    3800, Items.DIAMOND, 4, 50, null, true);
        };
    }

    private static Quest generateDailySoulQuest(long day, AdventurerRank rank) {
        String id = "daily_soul_" + day;
        if (rank.getIndex() >= AdventurerRank.B.getIndex()) {
            return new Quest(id,
                    "§d[Diaria] Alquimia Mayor: Gema de Alma Radiante",
                    "Entrega 1 Gema de Alma Radiante para las forjas de armaduras maestras del gremio.",
                    rank, QuestType.COLLECT_ITEMS, 1, 0,
                    2800, Items.EMERALD, 12, 45, ModItems.RADIANT_SOUL_GEM.get(), true);
        } else if (rank.getIndex() >= AdventurerRank.D.getIndex()) {
            return new Quest(id,
                    "§d[Diaria] Resonancia Metálica: Cristales Resonantes",
                    "Entrega 2 Cristales de Alma Resonantes a los herreros del gremio.",
                    rank, QuestType.COLLECT_ITEMS, 2, 0,
                    1100, Items.EMERALD, 6, 30, ModItems.RESONANT_SOUL_CRYSTAL.get(), true);
        } else {
            return new Quest(id,
                    "§d[Diaria] Suministro de Almas: Fragmentos Menores",
                    "Entrega 4 Fragmentos de Alma Menor para el templado de armas básicas.",
                    rank, QuestType.COLLECT_ITEMS, 4, 0,
                    400, Items.EMERALD, 4, 20, ModItems.LESSER_SOUL_FRAGMENT.get(), true);
        }
    }

    private static Quest generateDailySupplyQuest(long day, AdventurerRank rank) {
        String id = "daily_supply_" + day;
        int variant = (int) (Math.abs(day * 7 + rank.getIndex()) % 3);

        if (rank.getIndex() >= AdventurerRank.C.getIndex()) {
            if (variant == 0) {
                return new Quest(id,
                        "§e[Diaria] Bóveda del Gremio: Lingotes de Oro",
                        "Entrega 6 Lingotes de Oro para financiar las defensas de la región.",
                        rank, QuestType.COLLECT_ITEMS, 6, 0,
                        800, Items.EMERALD, 6, 25, Items.GOLD_INGOT, true);
            } else if (variant == 1) {
                return new Quest(id,
                        "§e[Diaria] Forja Sagrada: Diamantes Puros",
                        "Entrega 2 Diamantes para el engarce de armaduras de combate.",
                        rank, QuestType.COLLECT_ITEMS, 2, 0,
                        1600, Items.EMERALD, 10, 35, Items.DIAMOND, true);
            } else {
                return new Quest(id,
                        "§e[Diaria] Reserva Estratégica: Lapislázuli Arcano",
                        "Entrega 16 gemas de Lapislázuli para investigaciones del gremio.",
                        rank, QuestType.COLLECT_ITEMS, 16, 0,
                        900, Items.EXPERIENCE_BOTTLE, 4, 25, Items.LAPIS_LAZULI, true);
            }
        } else {
            if (variant == 0) {
                return new Quest(id,
                        "§e[Diaria] Armería Común: Lingotes de Hierro",
                        "Entrega 8 Lingotes de Hierro para reabastecer a la guardia de la aldea.",
                        rank, QuestType.COLLECT_ITEMS, 8, 0,
                        350, Items.BREAD, 6, 15, Items.IRON_INGOT, true);
            } else if (variant == 1) {
                return new Quest(id,
                        "§e[Diaria] Guarnición: Cuero Curtido",
                        "Entrega 6 piezas de Cuero para armaduras de explorador.",
                        rank, QuestType.COLLECT_ITEMS, 6, 0,
                        300, Items.COOKED_BEEF, 5, 15, Items.LEATHER, true);
            } else {
                return new Quest(id,
                        "§e[Diaria] Municiones: Flechas de Cazador",
                        "Entrega 16 Flechas para los arqueros del asentamiento.",
                        rank, QuestType.COLLECT_ITEMS, 16, 0,
                        320, Items.IRON_INGOT, 3, 15, Items.ARROW, true);
            }
        }
    }

    private static Quest generateDailyBanditQuest(long day, AdventurerRank rank) {
        String id = "daily_bandit_" + day;
        return new Quest(id,
                "§6[Diaria] Seguridad de Caminos: Caza de Bandidos",
                "Elimina 3 Bandidos o Forajidos que asedian las rutas comerciales del reino.",
                rank, QuestType.KILL_SPECIFIC_MOB, 3, 0,
                650, Items.IRON_INGOT, 5, 25, null, true);
    }

    private static Quest generateDailyPatrolQuest(long day, AdventurerRank rank) {
        String id = "daily_patrol_" + day;
        return new Quest(id,
                "§b[Diaria] Patrulla de Aldea: Protección Perimetral",
                "Elimina 4 criaturas hostiles en las cercanías de cualquier aldea o puesto del gremio.",
                rank, QuestType.PATROL_VILLAGE, 4, 0,
                550, Items.COOKED_BEEF, 4, 20, null, true);
    }
}
