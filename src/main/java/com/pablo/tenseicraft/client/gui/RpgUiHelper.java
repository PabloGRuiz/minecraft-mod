package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.client.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Utilidades compartidas de interfaz para unificar el diseño, la barra superior
 * de navegación y el recorte seguro de textos en todas las pantallas de TenseiCraft.
 */
public final class RpgUiHelper {

    public enum NavTab {
        PROFILE("Perfil"),
        QUESTS("Misiones"),
        SKILLS("Técnicas"),
        POWER("Poder"),
        PARTY("Grupo");

        private final String label;

        NavTab(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private RpgUiHelper() {}

    public static void drawPanelFrame(GuiGraphics g, int leftPos, int topPos, int width, int height) {
        g.fillGradient(leftPos, topPos, leftPos + width, topPos + height, 0xF2101320, 0xF2181D2E);
        g.renderOutline(leftPos, topPos, width, height, 0xFFD4AF37);
        g.renderOutline(leftPos + 2, topPos + 2, width - 4, height - 4, 0xFF393F56);
        g.hLine(leftPos + 8, leftPos + width - 8, topPos + 26, 0xFF8C7233);
    }

    public static void addNavigationBar(Screen screen, Consumer<Button> addWidget,
                                        int leftPos, int topPos, int guiWidth, NavTab activeTab) {
        int startX = leftPos + 8;
        int endX = leftPos + guiWidth - 26;
        int gap = 3;
        NavTab[] tabs = NavTab.values();
        int btnW = (endX - startX - gap * (tabs.length - 1)) / tabs.length;
        int y = topPos + 6;

        for (int i = 0; i < tabs.length; i++) {
            NavTab tab = tabs[i];
            int bx = startX + i * (btnW + gap);
            boolean isActive = (tab == activeTab);
            Component label = Component.literal((isActive ? "§6§l" : "§7") + tab.getLabel());
            String keyName = getKeyLabelForTab(tab);

            Button btn = Button.builder(label, b -> {
                if (!isActive) {
                    openTabScreen(tab);
                }
            })
            .bounds(bx, y, btnW, 16)
            .tooltip(Tooltip.create(Component.literal("§e" + tab.getLabel() + " §8[" + keyName + "]")))
            .build();

            btn.active = !isActive;
            addWidget.accept(btn);
        }

        Button closeBtn = Button.builder(Component.literal("§c✕"), b -> screen.onClose())
                .bounds(leftPos + guiWidth - 22, y, 16, 16)
                .tooltip(Tooltip.create(Component.literal("§cCerrar §7[ESC]")))
                .build();
        addWidget.accept(closeBtn);
    }

    private static String getKeyLabelForTab(NavTab tab) {
        return switch (tab) {
            case PROFILE -> ModKeyMappings.OPEN_STATS_KEY.get().getTranslatedKeyMessage().getString();
            case QUESTS -> ModKeyMappings.OPEN_QUESTS_KEY.get().getTranslatedKeyMessage().getString();
            case SKILLS -> ModKeyMappings.OPEN_SKILLS_KEY.get().getTranslatedKeyMessage().getString();
            case POWER -> ModKeyMappings.OPEN_POWER_CONTROL_KEY.get().getTranslatedKeyMessage().getString();
            case PARTY -> ModKeyMappings.PARTY_KEY.get().getTranslatedKeyMessage().getString();
        };
    }

    private static void openTabScreen(NavTab tab) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        switch (tab) {
            case PROFILE -> mc.setScreen(new RpgStatsScreen(null));
            case QUESTS -> mc.setScreen(new RpgQuestsScreen(null));
            case SKILLS -> mc.setScreen(new RpgPhysicalSkillsScreen(null));
            case POWER -> mc.setScreen(new PowerControlScreen(null));
            case PARTY -> mc.setScreen(new PartyScreen());
        }
    }

    public static String clip(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 8) return "";
        if (font.width(text) <= maxWidth) return text;
        int ellipsisW = font.width("…");
        String sub = font.plainSubstrByWidth(text, Math.max(4, maxWidth - ellipsisW));
        return sub + "…";
    }
}
