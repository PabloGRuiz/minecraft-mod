package com.pablo.tenseicraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ReforgeClientHandler {

    private ReforgeClientHandler() {}

    public static void openReforgeScreen(boolean isMagicAnvil, int meguEntityId, BlockPos anvilPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new ReforgeScreen(isMagicAnvil, meguEntityId, anvilPos));
        }
    }

    public static void onReforgeResult(boolean success, float winChancePct, float finalAngleDeg, String resultSummary) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ReforgeScreen screen) {
            screen.startSpinResult(success, winChancePct, finalAngleDeg, resultSummary);
        }
    }
}
