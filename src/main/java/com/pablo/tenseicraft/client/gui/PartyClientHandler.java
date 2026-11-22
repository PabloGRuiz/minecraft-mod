package com.pablo.tenseicraft.client.gui;

import net.minecraft.client.Minecraft;

public class PartyClientHandler {

    public static void openPartyScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new PartyScreen());
        }
    }
}

