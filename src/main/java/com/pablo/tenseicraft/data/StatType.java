package com.pablo.tenseicraft.data;

public enum StatType {
    FORTITUDE("fortitude", "tenseicraft.stat.fortitude.name", "🛡️"),
    DEXTERITY("dexterity", "tenseicraft.stat.dexterity.name", "🎯"),
    SPIRIT("spirit", "tenseicraft.stat.spirit.name", "✨"),
    LUCK("luck", "tenseicraft.stat.luck.name", "🍀");

    private final String id;
    private final String translationKey;
    private final String icon;

    StatType(String id, String translationKey, String icon) {
        this.id = id;
        this.translationKey = translationKey;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getIcon() {
        return icon;
    }

    public static StatType fromString(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase().trim();
        return switch (lower) {
            case "fortitude", "fortaleza", "vida", "fuerza", "health", "strength" -> FORTITUDE;
            case "dexterity", "destreza", "agilidad", "agility", "velocidad", "speed" -> DEXTERITY;
            case "spirit", "espiritu", "espíritu", "magia", "magic" -> SPIRIT;
            case "luck", "suerte", "lucky", "crit", "critico", "crítico" -> LUCK;
            default -> null;
        };
    }
}
