package com.pablo.rpglevels.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class ModKeyMappings {

    public static final String CATEGORY_RPG = "key.categories.rpglevels";

    public static final Lazy<KeyMapping> DASH_KEY = Lazy.of(() -> new KeyMapping(
            "key.rpglevels.dash",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_CONTROL,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> PARTY_KEY = Lazy.of(() -> new KeyMapping(
            "key.rpglevels.party",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> RELEASE_SWORD_KEY = Lazy.of(() -> new KeyMapping(
            "key.rpglevels.release_sword",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY_RPG
    ));

    public static final Lazy<KeyMapping> SUMMON_GUARDIAN_KEY = Lazy.of(() -> new KeyMapping(
            "key.rpglevels.summon_guardian",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY_RPG
    ));

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DASH_KEY.get());
        event.register(PARTY_KEY.get());
        event.register(RELEASE_SWORD_KEY.get());
        event.register(SUMMON_GUARDIAN_KEY.get());
    }
}
