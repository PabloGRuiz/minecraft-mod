package com.pablo.tenseicraft.data;

import net.minecraft.util.RandomSource;

import java.util.Locale;

/**
 * Atributo oculto que determina la densidad espiritual del alma del jugador,
 * su techo máximo de estadísticas y si su camino de poder sigue la Liberación
 * de la Espada Espiritual + Resonancia (Menor/Moderada) o la Furia Primordial (Alta/Irregular).
 */
public enum SoulDensity {
    MINOR("minor", "Menor", 40, 45, false, 1.0f, 0.0f, 1.0f),
    MODERATE("moderate", "Moderada", 50, 55, false, 1.0f, 0.0f, 1.0f),
    HIGH("high", "Alta", 60, 60, true, 1.50f, 0.15f, 0.80f),
    IRREGULAR("irregular", "Irregular", Integer.MAX_VALUE, Integer.MAX_VALUE, true, 2.00f, 0.25f, 0.65f);

    private final String id;
    private final String displayName;
    private final int minCap;
    private final int maxCap;
    private final boolean denseSoul;
    private final float manaCostMultiplier;
    private final float failChance;
    private final float effectivenessMultiplier;

    SoulDensity(
            String id,
            String displayName,
            int minCap,
            int maxCap,
            boolean denseSoul,
            float manaCostMultiplier,
            float failChance,
            float effectivenessMultiplier
    ) {
        this.id = id;
        this.displayName = displayName;
        this.minCap = minCap;
        this.maxCap = maxCap;
        this.denseSoul = denseSoul;
        this.manaCostMultiplier = manaCostMultiplier;
        this.failChance = failChance;
        this.effectivenessMultiplier = effectivenessMultiplier;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinCap() {
        return minCap;
    }

    public int getMaxCap() {
        return maxCap;
    }

    public boolean isDenseSoul() {
        return denseSoul;
    }

    public boolean isUncapped() {
        return this == IRREGULAR;
    }

    public float getManaCostMultiplier() {
        return manaCostMultiplier;
    }

    public float getFailChance() {
        return failChance;
    }

    public float getEffectivenessMultiplier() {
        return effectivenessMultiplier;
    }

    public int rollStatCap(RandomSource random) {
        if (isUncapped()) {
            return Integer.MAX_VALUE;
        }
        if (minCap >= maxCap) {
            return minCap;
        }
        if (random == null) {
            return (minCap + maxCap) / 2;
        }
        return minCap + random.nextInt((maxCap - minCap) + 1);
    }

    public static SoulDensity rollRandom(RandomSource random) {
        if (random == null) {
            return MINOR;
        }
        int roll = random.nextInt(100);
        if (roll < 50) return MINOR;
        if (roll < 80) return MODERATE;
        if (roll < 95) return HIGH;
        return IRREGULAR;
    }

    public static SoulDensity fromId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String norm = raw.toLowerCase(Locale.ROOT).trim();
        return switch (norm) {
            case "minor", "menor", "low", "baja" -> MINOR;
            case "moderate", "moderada", "moderado", "intermediate", "intermedia", "intermedio", "media", "medium" -> MODERATE;
            case "high", "alta", "alto", "dense", "densa" -> HIGH;
            case "irregular", "irreg" -> IRREGULAR;
            default -> null;
        };
    }
}
