package com.pablo.rpglevels.client.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.client.gui.PartyScreen;
import com.pablo.rpglevels.client.gui.PowerControlScreen;
import com.pablo.rpglevels.client.gui.RpgPhysicalSkillsScreen;
import com.pablo.rpglevels.client.gui.RpgQuestsScreen;
import com.pablo.rpglevels.client.gui.RpgStatsScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Maneja las teclas configurables para abrir directamente todas las interfaces del mod RPG Levels.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class MenuKeyClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        while (ModKeyMappings.OPEN_STATS_KEY.get().consumeClick()) {
            mc.setScreen(new RpgStatsScreen(null));
        }

        while (ModKeyMappings.OPEN_QUESTS_KEY.get().consumeClick()) {
            mc.setScreen(new RpgQuestsScreen(null));
        }

        while (ModKeyMappings.OPEN_SKILLS_KEY.get().consumeClick()) {
            mc.setScreen(new RpgPhysicalSkillsScreen(null));
        }

        while (ModKeyMappings.OPEN_POWER_CONTROL_KEY.get().consumeClick()) {
            mc.setScreen(new PowerControlScreen(null));
        }

        while (ModKeyMappings.PARTY_KEY.get().consumeClick()) {
            mc.setScreen(new PartyScreen());
        }
    }
}
