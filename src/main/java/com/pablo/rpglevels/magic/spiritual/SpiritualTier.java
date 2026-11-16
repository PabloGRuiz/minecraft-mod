package com.pablo.rpglevels.magic.spiritual;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Sistema de Rangos y Títulos para los Segadores Espirituales inspirado en Mushoku Tensei.
 * Se divide en dos ramas: Combatientes (físico/espada) e Ingenieros (técnicas/sellos).
 */
public enum SpiritualTier {
    PRINCIPIANTE(1, "Principiante", "Novicio Segador", "Aprendiz de Artífice", ChatFormatting.WHITE, 0),
    INTERMEDIO(2, "Intermedio", "Segador Espiritual", "Ingeniero Espiritual", ChatFormatting.GREEN, 10),
    AVANZADO(3, "Avanzado", "Segador de Élite", "Maestro Ingeniero", ChatFormatting.AQUA, 25),
    SANTO(4, "Santo", "Santo de la Espada Espiritual", "Santo Ingeniero Espiritual", ChatFormatting.YELLOW, 50),
    REAL(5, "Real", "Rey Segador Espiritual", "Rey Ingeniero Espiritual", ChatFormatting.GOLD, 75),
    IMPERIAL(6, "Imperial", "Emperador Segador", "Emperador Ingeniero", ChatFormatting.DARK_PURPLE, 100),
    DIVINO(7, "Divino", "Dios Segador Espiritual", "Dios Ingeniero Espiritual", ChatFormatting.LIGHT_PURPLE, 150);

    private final int level;
    private final String tierName;
    private final String combatantTitle;
    private final String engineerTitle;
    private final ChatFormatting color;
    private final int requiredAffinity;

    SpiritualTier(int level, String tierName, String combatantTitle, String engineerTitle, ChatFormatting color, int requiredAffinity) {
        this.level = level;
        this.tierName = tierName;
        this.combatantTitle = combatantTitle;
        this.engineerTitle = engineerTitle;
        this.color = color;
        this.requiredAffinity = requiredAffinity;
    }

    public int getLevel() {
        return level;
    }

    public String getTierName() {
        return tierName;
    }

    public String getCombatantTitle() {
        return combatantTitle;
    }

    public String getEngineerTitle() {
        return engineerTitle;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public int getRequiredAffinity() {
        return requiredAffinity;
    }

    public String getTitle(boolean isEngineer) {
        return isEngineer ? engineerTitle : combatantTitle;
    }

    public MutableComponent getBadge(boolean isEngineer) {
        return Component.literal("[" + getTitle(isEngineer) + "]").withStyle(color, ChatFormatting.BOLD);
    }

    public MutableComponent getTierBadge() {
        return Component.literal("Grado " + tierName).withStyle(color, ChatFormatting.BOLD);
    }

    public static SpiritualTier fromLevel(int lvl) {
        for (SpiritualTier tier : values()) {
            if (tier.level == lvl) return tier;
        }
        return PRINCIPIANTE;
    }
}

