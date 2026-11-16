package com.pablo.rpglevels.magic;

import com.pablo.rpglevels.items.SpiritualSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public enum SpiritualTechnique {
    PHYSICAL_BOOST(
            "physical_boost",
            "Técnica Espiritual Nro. 1: Physical Boost",
            "Principiante",
            List.of(1, 2, 3),
            "1 - 2 - 3",
            75.0f,
            500, // 25s cooldown
            5,
            "«Corpus Vigor Eruptio!»",
            ChatFormatting.GOLD,
            "Aumenta drásticamente las capacidades físicas del lanzador durante 30s: Fuerza, Resistencia, Velocidad y Absorción. Su elevado coste exige temple."
    ),
    LOW_HEALING(
            "low_healing",
            "Técnica Espiritual Nro. 2: Low Healing",
            "Principiante",
            List.of(1, 3, 2),
            "1 - 3 - 2",
            25.0f,
            120, // 6s cooldown
            5,
            "«Corpus Vitae Proiectio!»",
            ChatFormatting.GREEN,
            "Dispara un proyectil sagrado en línea recta que sana a la primera entidad que impacte. No puede curar al propio usuario."
    ),
    LOW_PARALIZE(
            "low_paralize",
            "Técnica Espiritual Nro. 3: Low Paralize",
            "Principiante",
            List.of(2, 1, 3),
            "2 - 1 - 3",
            60.0f,
            360, // 18s cooldown
            5,
            "«Fulgur Vinculum Stasis!»",
            ChatFormatting.YELLOW,
            "Dispara un rayo restrictivo que paraliza por completo e inmoviliza al objetivo impactado durante unos breves segundos."
    ),
    LOW_PURIFICATION(
            "low_purification",
            "Técnica Espiritual Nro. 4: Low Purification",
            "Principiante",
            List.of(3, 2, 1),
            "3 - 2 - 1",
            20.0f,
            100, // 5s cooldown
            5,
            "«Anima Purgatio Dubia!»",
            ChatFormatting.AQUA,
            "Hechizo básico de purificación. Intenta disipar un efecto negativo del usuario, aunque al ser rudimentario existe riesgo de disipar un efecto beneficioso por error."
    ),
    ELEMENTAL_BALL(
            "elemental_ball",
            "Técnica Espiritual Nro. 5: Elemental Ball",
            "Principiante",
            List.of(3, 1, 2),
            "3 - 1 - 2",
            25.0f,
            120, // 6s cooldown
            5,
            "«Elementum Sphaera Iactus!»",
            ChatFormatting.RED,
            "Condensa energía espiritual en una esfera elemental básica. Muta su nombre, aspecto y efecto según el elemento del usuario (Fuego, Hielo, Rayo, Sombra o Energía). Daño leve y bajo coste."
    ),
    MAGIC_AFFINITY(
            "channel_magic",
            "Técnica Espiritual Nro. 6: Afinidad Mágica",
            "Principiante",
            List.of(2, 3, 1),
            "2 - 3 - 1",
            50.0f,
            300, // 15s cooldown
            5,
            "«Anima Aether Fluctuo!»",
            ChatFormatting.LIGHT_PURPLE,
            "Estabiliza el flujo de maná espiritual, forja el vínculo con la espada y otorga un aumento temporal de +5 de Espíritu durante 15s para lanzar técnicas con soltura."
    ),
    SUMMON_LESSER_SOUL(
            "summon_lesser_soul",
            "Técnica Espiritual Nro. 7: Convocar Alma Menor",
            "Principiante",
            List.of(1, 3, 1),
            "1 - 3 - 1",
            50.0f,
            600, // 30s cooldown
            10,
            "«Anima Minor Evocatio!»",
            ChatFormatting.LIGHT_PURPLE,
            "Técnica de invocación espiritual. Manifiesta permanentemente a una pequeña criatura leal nacida de tu alma (Pelotita). Te seguirá fielmente, se integrará a tu grupo y lanzará Low Healing cada 30s para sanar a quien esté herido. Su salud y poder curativo escalan con tu Espíritu."
    ),
    FAST_HEALING(
            "fast_healing",
            "Técnica Espiritual Nro. 10: Fast Healing",
            "Intermedio",
            List.of(3, 2, 1, 2),
            "3 - 2 - 1 - 2",
            80.0f,
            600, // 30s cooldown
            5,
            "«Anima Vitae Restitutio!»",
            ChatFormatting.GREEN,
            "Técnica intermedia de restauración automática. Detecta en un radio de 24 metros al aliado o compañero de grupo con menor porcentaje de salud y le restaura instantáneamente una cantidad masiva de vida."
    ),
    CONNECT_REALITIES(
            "connect_realities",
            "Técnica Espiritual Nro. 20: Conectar Realidades",
            "Avanzado",
            List.of(2, 1, 3, 2),
            "2 - 1 - 3 - 2",
            500.0f,
            600, // 30s cooldown
            25,
            "«Spatium Ruptura Coniunctio!»",
            ChatFormatting.DARK_PURPLE,
            "Técnica espiritual avanzada. Desata una onda expansiva que repele entidades hostiles y rasga el tejido dimensional. Si impacta contra un marco de Obsidiana Llorosa, despierta el Portal al Abismo."
    ),
    OPPRESSIVE_AURA(
            "oppressive_aura",
            "Técnica Espiritual Nro. 25: Aura Opresiva",
            "Avanzado",
            List.of(1, 2, 1, 3),
            "1 - 2 - 1 - 3",
            120.0f,
            600, // 30s cooldown
            25,
            "«Anima Gravitas Dominatio!»",
            ChatFormatting.DARK_PURPLE,
            "Técnica espiritual avanzada. Desata una imponente columna cilíndrica de presión espiritual que se expande a lo ancho. Toda entidad atrapada sufre la aplastante gravedad de tu alma: si tu aura supera a la suya, será ralentizada y desgastada continuamente por daño mágico; ante una brecha abrumadora de poder, sucumbirá al colapso sensorial (Ceguera)."
    );

    private final String id;
    private final String displayName;
    private final String tier;
    private final List<Integer> sequence;
    private final String sequenceDisplay;
    private final float manaCost;
    private final int cooldownTicks;
    private final int emeraldCost;
    private final String chant;
    private final ChatFormatting color;
    private final String description;

    SpiritualTechnique(String id, String displayName, String tier, List<Integer> sequence, String sequenceDisplay,
                       float manaCost, int cooldownTicks, int emeraldCost, String chant, ChatFormatting color, String description) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.sequence = sequence;
        this.sequenceDisplay = sequenceDisplay;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.emeraldCost = emeraldCost;
        this.chant = chant;
        this.color = color;
        this.description = description;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getTier() { return tier; }
    public List<Integer> getSequence() { return sequence; }
    public String getSequenceDisplay() { return sequenceDisplay; }
    public float getManaCost() { return manaCost; }
    public int getCooldownTicks() { return cooldownTicks; }
    public float getCooldownSeconds() { return cooldownTicks / 20.0f; }
    public int getEmeraldCost() { return emeraldCost; }
    public String getChant() { return chant; }
    public ChatFormatting getColor() { return color; }
    public String getDescription() { return description; }

    public static SpiritualTechnique byId(String id) {
        if (id == null) return null;
        for (SpiritualTechnique tech : values()) {
            if (tech.id.equalsIgnoreCase(id)) return tech;
        }
        return null;
    }

    /**
     * Resuelve el nombre dinámico de Elemental Ball según el elemento de la espada sostenida o vinculada.
     */
    public static String getDynamicElementalBallName(Player player) {
        SpiritualSwordItem.BladeElement elem = getPlayerElement(player);
        return switch (elem) {
            case IGNIS -> "Técnica Espiritual Nro. 5: Fire Ball";
            case GLACIES -> "Técnica Espiritual Nro. 5: Ice Ball";
            case FULGUR -> "Técnica Espiritual Nro. 5: Lightning Ball";
            case UMBRA -> "Técnica Espiritual Nro. 5: Shadow Ball";
            default -> "Técnica Espiritual Nro. 5: Energy Ball";
        };
    }

    public static SpiritualSwordItem.BladeElement getPlayerElement(Player player) {
        if (player == null) return SpiritualSwordItem.BladeElement.NONE;

        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SpiritualSwordItem) {
            return SpiritualSwordItem.getBladeElement(main);
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof SpiritualSwordItem) {
            return SpiritualSwordItem.getBladeElement(off);
        }

        // Buscar en el inventario si tiene su espada vinculada
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof SpiritualSwordItem) {
                return SpiritualSwordItem.getBladeElement(item);
            }
        }
        return SpiritualSwordItem.BladeElement.NONE;
    }
}

