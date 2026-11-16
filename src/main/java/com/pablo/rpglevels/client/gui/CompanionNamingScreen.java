package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.network.RenameCompanionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class CompanionNamingScreen extends Screen {

    private final int entityId;
    private final String currentName;
    private final Screen parentScreen;

    private EditBox nameInput;
    private final int GUI_WIDTH = 260;
    private final int GUI_HEIGHT = 140;
    private int leftPos;
    private int topPos;

    public CompanionNamingScreen(int entityId, String currentName, Screen parentScreen) {
        super(Component.literal("Asignar Nombre"));
        this.entityId = entityId;
        this.currentName = currentName != null && !currentName.equals("Aventurero") ? currentName : "";
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        this.nameInput = new EditBox(this.font, leftPos + 30, topPos + 60, 200, 20, Component.literal("Nombre"));
        this.nameInput.setMaxLength(20);
        this.nameInput.setValue(this.currentName);
        this.addRenderableWidget(this.nameInput);
        this.setInitialFocus(this.nameInput);

        // Confirm button
        this.addRenderableWidget(Button.builder(Component.literal("§aConfirmar"), b -> {
            String val = this.nameInput.getValue().trim();
            if (!val.isEmpty()) {
                PacketDistributor.sendToServer(new RenameCompanionPayload(this.entityId, val));
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }
        }).bounds(leftPos + 30, topPos + 95, 95, 20).build());

        // Cancel button
        this.addRenderableWidget(Button.builder(Component.literal("§cCancelar"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(leftPos + 135, topPos + 95, 95, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Sin shader
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF212141F);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFD4AF37);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0xFF35394A);

        guiGraphics.drawCenteredString(font, "§6§l✦ ASIGNAR NOMBRE AL CAMARADA ✦", leftPos + GUI_WIDTH / 2, topPos + 12, 0xFFFFAA00);
        guiGraphics.drawCenteredString(font, "§7En estas tierras, los plebeyos no portan nombre.", leftPos + GUI_WIDTH / 2, topPos + 28, 0xFFAAAAAA);
        guiGraphics.drawCenteredString(font, "§e¡Bautízalo y dale un lugar de honor en tu linaje!", leftPos + GUI_WIDTH / 2, topPos + 40, 0xFFFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

