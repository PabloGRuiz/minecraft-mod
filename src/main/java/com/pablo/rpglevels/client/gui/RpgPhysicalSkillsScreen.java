package com.pablo.rpglevels.client.gui;

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
    private final int GUI_WIDTH = 325;
    private final int GUI_HEIGHT = 225;
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

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        int points = (data != null) ? data.getPhysicalSkillPoints() : 0;

        // Column 1: Agilidad
        addSkillNode("agility_dash", null, leftPos + 18, topPos + 55, data, points);
        addSkillNode("agility_double_jump", "agility_dash", leftPos + 18, topPos + 105, data, points);
        addSkillNode("agility_air_dash", "agility_double_jump", leftPos + 18, topPos + 155, data, points);

        // Column 2: Fuerza
        addSkillNode("strength_1", null, leftPos + 120, topPos + 55, data, points);
        addSkillNode("strength_2", "strength_1", leftPos + 120, topPos + 105, data, points);
        addSkillNode("strength_3", "strength_2", leftPos + 120, topPos + 155, data, points);

        // Column 3: Fortaleza
        addSkillNode("fortitude_1", null, leftPos + 222, topPos + 55, data, points);
        addSkillNode("fortitude_2", "fortitude_1", leftPos + 222, topPos + 105, data, points);
        addSkillNode("fortitude_3", "fortitude_2", leftPos + 222, topPos + 155, data, points);

        // Back to Stats Button
        this.addRenderableWidget(Button.builder(Component.translatable("rpglevels.gui.back_stats"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(leftPos + 18, topPos + GUI_HEIGHT - 24, 110, 18).build());

        // Close Button
        this.addRenderableWidget(Button.builder(Component.translatable("rpglevels.gui.close"), button -> {
            this.onClose();
        }).bounds(leftPos + GUI_WIDTH - 85, topPos + GUI_HEIGHT - 24, 68, 18).build());
    }

    private void addSkillNode(String skillId, String prereqId,
                              int x, int y, PlayerRpgData data, int points) {
        boolean unlocked = (data != null && data.hasSkill(skillId));
        boolean prereqMet = (prereqId == null || (data != null && data.hasSkill(prereqId)));
        boolean canUnlock = (!unlocked && prereqMet && points >= 1);

        int btnWidth = 85;
        int btnHeight = 18;

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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intencionalmente vacío para evitar processBlurEffect de 1.21
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Darken backdrop without blur
        graphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        // Dark RPG Backdrop Container
        graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF0101216);
        graphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFC5A059);
        graphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0x55C5A059);

        // Header Title
        Component titleComp = Component.literal("§6§l⚔ ").append(Component.translatable("rpglevels.gui.skills_title")).append(Component.literal(" ⚔"));
        graphics.drawCenteredString(this.font, titleComp, leftPos + GUI_WIDTH / 2, topPos + 10, 0xFFFFFFFF);

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        int points = (data != null) ? data.getPhysicalSkillPoints() : 0;

        // Subtitle: Available Points
        Component pointsComp = Component.translatable("rpglevels.gui.points_available", points);
        graphics.drawCenteredString(this.font, pointsComp, leftPos + GUI_WIDTH / 2, topPos + 24, 0xFFFFFFFF);

        // Column Titles
        graphics.drawCenteredString(this.font, Component.translatable("rpglevels.gui.branch_agility"), leftPos + 60, topPos + 40, 0xFF55FFFF);
        graphics.drawCenteredString(this.font, Component.translatable("rpglevels.gui.branch_strength"), leftPos + 162, topPos + 40, 0xFFFF5555);
        graphics.drawCenteredString(this.font, Component.translatable("rpglevels.gui.branch_fortitude"), leftPos + 264, topPos + 40, 0xFFFFAA00);

        // Connecting flow arrows between node tiers
        graphics.drawCenteredString(this.font, "§8↓", leftPos + 60, topPos + 82, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, "§8↓", leftPos + 60, topPos + 132, 0xFFAAAAAA);

        graphics.drawCenteredString(this.font, "§8↓", leftPos + 162, topPos + 82, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, "§8↓", leftPos + 162, topPos + 132, 0xFFAAAAAA);

        graphics.drawCenteredString(this.font, "§8↓", leftPos + 264, topPos + 82, 0xFFAAAAAA);
        graphics.drawCenteredString(this.font, "§8↓", leftPos + 264, topPos + 132, 0xFFAAAAAA);

        // Render all buttons & tooltips
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
