package com.pablo.rpglevels.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum AdventurerRank {
    F(0, "F", ChatFormatting.GRAY, 1.0F),
    E(1, "E", ChatFormatting.GREEN, 1.25F),
    D(2, "D", ChatFormatting.AQUA, 1.5F),
    C(3, "C", ChatFormatting.BLUE, 2.0F),
    B(4, "B", ChatFormatting.DARK_PURPLE, 2.5F),
    A(5, "A", ChatFormatting.GOLD, 3.0F, true);

    private final int index;
    private final String name;
    private final ChatFormatting color;
    private final boolean bold;
    private final float reputationMultiplier;

    AdventurerRank(int index, String name, ChatFormatting color, float reputationMultiplier) {
        this(index, name, color, reputationMultiplier, false);
    }

    AdventurerRank(int index, String name, ChatFormatting color, float reputationMultiplier, boolean bold) {
        this.index = index;
        this.name = name;
        this.color = color;
        this.reputationMultiplier = reputationMultiplier;
        this.bold = bold;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean isBold() {
        return bold;
    }

    public float getReputationMultiplier() {
        return reputationMultiplier;
    }

    public boolean isMaxRank() {
        return this == A;
    }

    public int getRequiredAscensionLevel() {
        return switch (this) {
            case F -> 20;
            case E -> 35;
            case D -> 55;
            case C -> 75;
            case B -> 100;
            case A -> 125;
        };
    }

    public int getHireCost() {
        return switch (this) {
            case F -> 1;
            case E -> 2;
            case D -> 3;
            case C -> 5;
            case B -> 8;
            case A -> 12;
        };
    }

    public AdventurerRank getNextRank() {
        if (isMaxRank()) {
            return this;
        }
        return values()[index + 1];
    }

    public static AdventurerRank fromIndex(int index) {
        if (index < 0 || index >= values().length) {
            return F;
        }
        return values()[index];
    }

    public static AdventurerRank fromString(String name) {
        if (name == null) return null;
        for (AdventurerRank rank : values()) {
            if (rank.name.equalsIgnoreCase(name) || rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return null;
    }

    public MutableComponent getBadge() {
        MutableComponent comp = Component.literal("[Aventurero Rango " + name + "]").withStyle(color);
        if (bold) {
            comp = comp.withStyle(ChatFormatting.BOLD);
        }
        return comp;
    }

    public MutableComponent getShortBadge() {
        MutableComponent comp = Component.literal("[" + name + "]").withStyle(color);
        if (bold) {
            comp = comp.withStyle(ChatFormatting.BOLD);
        }
        return comp;
    }
}
