package com.pablo.tenseicraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class ModKeyMappings {

    public static final String CATEGORY_RPG = "key.categories.tenseicraft";

    // Menús e interfaces principales
    public static final Lazy<KeyMapping> OPEN_STATS_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.open_stats",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> OPEN_QUESTS_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.open_quests",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> OPEN_SKILLS_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.open_skills",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> OPEN_POWER_CONTROL_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.open_power_control",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> PARTY_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.party",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY_RPG
    ));

    // Habilidades de combate y espiritualidad
    public static final Lazy<KeyMapping> DASH_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.dash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> RELEASE_SWORD_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.release_sword",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> SUMMON_GUARDIAN_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.summon_guardian",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> MAGIC_MODIFIER_KEY = Lazy.of(() -> new KeyMapping(
            "key.tenseicraft.magic_modifier",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY_RPG
    ));

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_STATS_KEY.get());
        event.register(OPEN_QUESTS_KEY.get());
        event.register(OPEN_SKILLS_KEY.get());
        event.register(OPEN_POWER_CONTROL_KEY.get());
        event.register(PARTY_KEY.get());
        event.register(DASH_KEY.get());
        event.register(RELEASE_SWORD_KEY.get());
        event.register(SUMMON_GUARDIAN_KEY.get());
        event.register(MAGIC_MODIFIER_KEY.get());
    }
}
