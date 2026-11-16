package com.pablo.rpglevels.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class RpgClientConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue MANA_BAR_ENABLED;
    public static final ModConfigSpec.IntValue MANA_BAR_X_OFFSET;
    public static final ModConfigSpec.IntValue MANA_BAR_Y_OFFSET;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("mana_hud");
        MANA_BAR_ENABLED = builder
                .comment("Habilita o deshabilita el renderizado de la barra de maná en pantalla.")
                .define("enabled", true);

        MANA_BAR_X_OFFSET = builder
                .comment("Desplazamiento horizontal (X) de la barra de maná (en píxeles). Útil para ajustar o evitar colisiones con otros mods de interfaz.")
                .defineInRange("x_offset", 0, -500, 500);

        MANA_BAR_Y_OFFSET = builder
                .comment("Desplazamiento vertical (Y) de la barra de maná (en píxeles). Útil si tienes mods como AppleSkin o Iron's Spells encima de la comida.")
                .defineInRange("y_offset", 0, -500, 500);
        builder.pop();

        SPEC = builder.build();
    }
}
