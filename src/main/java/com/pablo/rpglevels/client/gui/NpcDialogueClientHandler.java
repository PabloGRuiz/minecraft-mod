package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.network.OpenNpcDialoguePayload;
import net.minecraft.client.Minecraft;

public class NpcDialogueClientHandler {

    public static void handleOpenDialogue(OpenNpcDialoguePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof NpcDialogueScreen currentScreen && currentScreen.getEntityId() == payload.entityId()) {
            currentScreen.updateState(payload);
        } else {
            mc.setScreen(new NpcDialogueScreen(payload));
        }
    }
}
