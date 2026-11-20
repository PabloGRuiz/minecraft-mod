package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.UnlockSkillPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class RpgPhysicalSkillsScreen extends Screen {

    private final Screen parentScreen;
    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 238;
    private int leftPos;
    private int topPos;

    public RpgPhysicalSkillsScreen(Screen parentScreen) {
        super(Component.translatable("rpglevels.gui.skills_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // 1. Barra superior unificada
        RpgUiHelper.addNavigationBar(this, this::addRenderableWidget, leftPos, topPos, GUI_WIDTH, RpgUiHelper.NavTab.SKILLS);

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        int points = (data != null) ? data.getPhysicalSkillPoints() : 0;

        int col1X = leftPos + 20;
        int col2X = leftPos + 132;
        int col3X = leftPos + 244;
        int row1Y = topPos + 72;
        int row2Y = topPos + 122;
        int row3Y = topPos + 172;

        // Columna 1: Agilidad
        addSkillNode("agility_dash", null, col1X, row1Y, data, points);
        addSkillNode("agility_double_jump", "agility_dash", col1X, row2Y, data, points);
        addSkillNode("agility_air_dash", "agility_double_jump", col1X, row3Y, data, points);

        // Columna 2: Fuerza
        addSkillNode("strength_1", null, col2X, row1Y, data, points);
        addSkillNode("strength_2", "strength_1", col2X, row2Y, data, points);
        addSkillNode("strength_3", "strength_2", col2X, row3Y, data, points);

        // Columna 3: Fortaleza
        addSkillNode("fortitude_1", null, col3X, row1Y, data, points);
        addSkillNode("fortitude_2", "fortitude_1", col3X, row2Y, data, points);
        addSkillNode("fortitude_3", "fortitude_2", col3X, row3Y, data, points);

        if (this.parentScreen != null) {
            this.addRenderableWidget(Button.builder(Component.literal("§cVolver"), b -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }).bounds(leftPos + 14, topPos + GUI_HEIGHT - 24, 72, 17).build());
        }
    }

    private void addSkillNode(String skillId, String prereqId, int x, int y, PlayerRpgData data, int points) {
        boolean unlocked = (data != null && data.hasSkill(skillId));
        boolean prereqMet = (prereqId == null || (data != null && data.hasSkill(prereqId)));
        boolean canUnlock = (!unlocked && prereqMet && points >= 1);

        int btnWidth = 96;
        int btnHeight = 20;

        Component skillName = Component.translatable("rpglevels.skill." + skillId);
        Component skillDesc = Component.translatable("rpglevels.skill." + skillId + ".desc");

        if (unlocked) {
            Button btn = Button.builder(Component.literal("§a✔ ").append(skillName), b -> {})
                    .bounds(x, y, btnWidth, btnHeight)
                    .tooltip(Tooltip.create(Component.translatable("rpglevels.skill.learned")
                            .append(Component.literal("\n§f")).append(skillName)
                            .append(Component.literal("\n§7")).append(skillDesc)))
                    .build();
            btn.active = false;
            this.addRenderableWidget(btn);
        } else if (canUnlock) {
            Button btn = Button.builder(Component.literal("§e★ ").append(skillName), b -> {
                PacketDistributor.sendToServer(new UnlockSkillPayload(skillId));
                this.rebuildWidgets();
            }).bounds(x, y, btnWidth, btnHeight)
            .tooltip(Tooltip.create(Component.translatable("rpglevels.skill.learn_cost")
                    .append(Component.literal("\n§f")).append(skillName)
                    .append(Component.literal("\n§7")).append(skillDesc)))
            .build();
            this.addRenderableWidget(btn);
        } else {
            MutableComponent reason = !prereqMet
                    ? Component.translatable("rpglevels.skill.req_prev")
                    : Component.translatable("rpglevels.skill.no_points");

            Button btn = Button.builder(Component.literal("§7🔒 ").append(skillName), b -> {})
                    .bounds(x, y, btnWidth, btnHeight)
                    .tooltip(Tooltip.create(Component.translatable("rpglevels.skill.locked")
                            .append(Component.literal("\n§f")).append(skillName)
                            .append(Component.literal("\n§7")).append(skillDesc)
                            .append(Component.literal("\n§c")).append(reason)))
                    .build();
            btn.active = false;
            this.addRenderableWidget(btn);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.OPEN_SKILLS_KEY.get().matches(keyCode, scanCode)
                || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB8080A12);
        RpgUiHelper.drawPanelFrame(graphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        int points = (data != null) ? data.getPhysicalSkillPoints() : 0;

        Component titleComp = Component.literal("§6§l⚔ ").append(Component.translatable("rpglevels.gui.skills_title"));
        graphics.drawString(this.font, titleComp, leftPos + 12, topPos + 31, 0xFFFFFFFF, true);
        Component pointsComp = Component.translatable("rpglevels.gui.points_available", points);
        graphics.drawString(this.font, pointsComp, leftPos + GUI_WIDTH - 12 - this.font.width(pointsComp), topPos + 31, 0xFFFFFFFF, false);

        // Encabezados de columnas
        int c1Mid = leftPos + 68;
        int c2Mid = leftPos + 180;
        int c3Mid = leftPos + 292;
        graphics.drawCenteredString(this.font, Component.translatable("rpglevels.gui.branch_agility"), c1Mid, topPos + 54, 0xFF55FFFF);
        graphics.drawCenteredString(this.font, Component.translatable("rpglevels.gui.branch_strength"), c2Mid, topPos + 54, 0xFFFF5555);
        graphics.drawCenteredString(this.font, Component.translatable("rpglevels.gui.branch_fortitude"), c3Mid, topPos + 54, 0xFFFFAA00);

        // Conectores entre niveles
        int[] mids = {c1Mid, c2Mid, c3Mid};
        for (int mid : mids) {
            graphics.drawCenteredString(this.font, "§8↓", mid, topPos + 100, 0xFFAAAAAA);
            graphics.drawCenteredString(this.font, "§8↓", mid, topPos + 150, 0xFFAAAAAA);
        }

        String dashKey = ModKeyMappings.DASH_KEY.get().getTranslatedKeyMessage().getString();
        graphics.drawCenteredString(this.font,
                "§7Tecla Dash: §e[" + dashKey + "] §8| Doble Salto con §e[Espacio] §7(Pasa el cursor sobre cada nodo)",
                leftPos + GUI_WIDTH / 2, topPos + GUI_HEIGHT - 16, 0xFF999999);

        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
