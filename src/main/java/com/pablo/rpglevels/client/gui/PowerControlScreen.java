package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.UpdatePowerControlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Pantalla de Regulación y Control de Poder (Power Control & Supresión).
 */
public class PowerControlScreen extends Screen {

    private final Screen parentScreen;

    private static final int GUI_WIDTH = 350;
    private static final int GUI_HEIGHT = 246;
    private int leftPos;
    private int topPos;

    private float currentPower = 1.0F;
    private float currentSpeed = 1.0F;
    private float currentAttack = 1.0F;
    private float currentJump = 1.0F;

    private PowerSlider powerSlider;
    private PowerSlider speedSlider;
    private PowerSlider attackSlider;
    private PowerSlider jumpSlider;

    public PowerControlScreen(Screen parentScreen) {
        super(Component.literal("Regulación de Poder"));
        this.parentScreen = parentScreen;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                this.currentPower = data.getPowerOutputRatio();
                this.currentSpeed = data.getMoveSpeedRatio();
                this.currentAttack = data.getAttackSpeedRatio();
                this.currentJump = data.getJumpHeightRatio();
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Barra superior unificada
        RpgUiHelper.addNavigationBar(this, this::addRenderableWidget, leftPos, topPos, GUI_WIDTH, RpgUiHelper.NavTab.POWER);

        int sliderW = 310;
        int sliderH = 18;
        int startX = leftPos + 20;
        int curY = topPos + 54;

        powerSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Poder Total / Aura", currentPower, 0.01F, 1.0F, "%", val -> this.currentPower = val);
        this.addRenderableWidget(powerSlider);

        curY += 30;
        speedSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Velocidad de Movimiento", currentSpeed, 0.10F, 1.0F, "%", val -> this.currentSpeed = val);
        this.addRenderableWidget(speedSlider);

        curY += 30;
        attackSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Velocidad de Ataque", currentAttack, 0.10F, 1.0F, "%", val -> this.currentAttack = val);
        this.addRenderableWidget(attackSlider);

        curY += 30;
        jumpSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Altura de Salto", currentJump, 0.00F, 1.0F, "%", val -> this.currentJump = val);
        this.addRenderableWidget(jumpSlider);

        int presetY = topPos + 178;
        this.addRenderableWidget(Button.builder(Component.literal("§a⚡ 100% Máximo"), b -> {
            applyPreset(1.0F, 1.0F, 1.0F, 1.0F);
        }).bounds(leftPos + 20, presetY, 150, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("§b🏙 Modo Ciudad / Sigilo"), b -> {
            applyPreset(0.15F, 0.55F, 0.50F, 0.20F);
        }).bounds(leftPos + 180, presetY, 150, 18).build());

        int bottomY = topPos + GUI_HEIGHT - 28;
        int saveW = (this.parentScreen != null) ? 200 : 310;
        this.addRenderableWidget(Button.builder(Component.literal("§a✔ Guardar y Aplicar"), b -> {
            saveAndApply();
            if (this.parentScreen != null && this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            } else {
                this.onClose();
            }
        }).bounds(leftPos + 20, bottomY, saveW, 18).build());

        if (this.parentScreen != null) {
            this.addRenderableWidget(Button.builder(Component.literal("§cVolver"), b -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }).bounds(leftPos + 228, bottomY, 102, 18).build());
        }
    }

    private void applyPreset(float power, float speed, float attack, float jump) {
        this.currentPower = power;
        this.currentSpeed = speed;
        this.currentAttack = attack;
        this.currentJump = jump;

        if (powerSlider != null) powerSlider.setSliderValue(power);
        if (speedSlider != null) speedSlider.setSliderValue(speed);
        if (attackSlider != null) attackSlider.setSliderValue(attack);
        if (jumpSlider != null) jumpSlider.setSliderValue(jump);
    }

    private void saveAndApply() {
        PacketDistributor.sendToServer(new UpdatePowerControlPayload(
                currentPower, currentSpeed, currentAttack, currentJump
        ));

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.setPowerControl(currentPower, currentSpeed, currentAttack, currentJump);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.OPEN_POWER_CONTROL_KEY.get().matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB8080A12);
        RpgUiHelper.drawPanelFrame(guiGraphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        guiGraphics.drawString(font, "§6⚡ Control y Regulación de Poder", leftPos + 14, topPos + 31, 0xFFFFFFFF, true);
        guiGraphics.drawString(font, "§7Suprime o libera tu energía física y aura.", leftPos + 14, topPos + 42, 0xFFAAAAAA, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static class PowerSlider extends AbstractSliderButton {
        private final String prefix;
        private final float min;
        private final float max;
        private final String unit;
        private final java.util.function.Consumer<Float> onChange;

        public PowerSlider(int x, int y, int width, int height, String prefix,
                           float initialValue, float min, float max, String unit,
                           java.util.function.Consumer<Float> onChange) {
            super(x, y, width, height, Component.empty(), (initialValue - min) / (max - min));
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.onChange = onChange;
            this.updateMessage();
        }

        public void setSliderValue(float val) {
            this.value = Mth.clamp((val - min) / (max - min), 0.0, 1.0);
            this.updateMessage();
        }

        public float getActualValue() {
            return (float) (min + this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            int percent = Math.round(getActualValue() * 100.0F);
            this.setMessage(Component.literal("§e" + prefix + ": §f" + percent + unit));
        }

        @Override
        protected void applyValue() {
            float actual = getActualValue();
            if (onChange != null) {
                onChange.accept(actual);
            }
        }
    }
}
