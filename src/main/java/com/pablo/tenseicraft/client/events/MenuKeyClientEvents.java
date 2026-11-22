package com.pablo.tenseicraft.client.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.ModKeyMappings;
import com.pablo.tenseicraft.client.gui.PartyScreen;
import com.pablo.tenseicraft.client.gui.PowerControlScreen;
import com.pablo.tenseicraft.client.gui.RpgPhysicalSkillsScreen;
import com.pablo.tenseicraft.client.gui.RpgQuestsScreen;
import com.pablo.tenseicraft.client.gui.RpgStatsScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Maneja las teclas configurables para abrir directamente todas las interfaces del mod TenseiCraft.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
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
