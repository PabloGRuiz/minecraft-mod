package com.pablo.rpglevels.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ReputationTier {
    HERO_LEGENDARY("hero_legendary", 75, 100, ChatFormatting.GOLD, "🌟", -0.40f, "rpglevels.reputation.hero_legendary"),
    CITIZEN_EXEMPLARY("citizen_exemplary", 25, 74, ChatFormatting.GREEN, "🟢", -0.20f, "rpglevels.reputation.citizen_exemplary"),
    NEUTRAL("neutral", -24, 24, ChatFormatting.YELLOW, "🟡", 0.0f, "rpglevels.reputation.neutral"),
    OUTLAW_SUSPECT("outlaw_suspect", -74, -25, ChatFormatting.GOLD, "🟠", 0.25f, "rpglevels.reputation.outlaw_suspect"),
    PUBLIC_ENEMY("public_enemy", -100, -75, ChatFormatting.DARK_RED, "☠️", 0.60f, "rpglevels.reputation.public_enemy");

    private final String id;
    private final int minPoints;
    private final int maxPoints;
    private final ChatFormatting color;
    private final String icon;
    private final float priceModifier; // e.g. -0.40f = 40% discount, +0.25f = 25% inflation
    private final String translationKey;

    ReputationTier(String id, int minPoints, int maxPoints, ChatFormatting color, String icon, float priceModifier, String translationKey) {
        this.id = id;
        this.minPoints = minPoints;
        this.maxPoints = maxPoints;
        this.color = color;
        this.icon = icon;
        this.priceModifier = priceModifier;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public int getMinPoints() {
        return minPoints;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public float getPriceModifier() {
        return priceModifier;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public Component getDisplayName() {
        return Component.literal(icon + " ").append(Component.translatable(translationKey).withStyle(color, ChatFormatting.BOLD));
    }

    public static ReputationTier fromScore(int score) {
        int clamped = Math.max(-100, Math.min(100, score));
        if (clamped >= 75) return HERO_LEGENDARY;
        if (clamped >= 25) return CITIZEN_EXEMPLARY;
        if (clamped >= -24) return NEUTRAL;
        if (clamped >= -74) return OUTLAW_SUSPECT;
        return PUBLIC_ENEMY;
    }
}

