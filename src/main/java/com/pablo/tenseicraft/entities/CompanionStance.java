package com.pablo.tenseicraft.entities;

public enum CompanionStance {
    FOLLOW(0, "Seguir", "§a🚶 Seguir"),
    WANDER_BASE(1, "Esperar en Casa", "§b🏡 En Casa"),
    STAY_GUARD(2, "Quedarse Quieto", "§e🛡 En Guardia");

    private final int id;
    private final String displayName;
    private final String buttonLabel;

    CompanionStance(int id, String displayName, String buttonLabel) {
        this.id = id;
        this.displayName = displayName;
        this.buttonLabel = buttonLabel;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public static CompanionStance fromId(int id) {
        for (CompanionStance stance : values()) {
            if (stance.id == id) {
                return stance;
            }
        }
        return FOLLOW;
    }
}

