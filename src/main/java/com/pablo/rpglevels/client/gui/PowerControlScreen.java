package com.pablo.rpglevels.client.gui;

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
 * Permite al jugador regular con deslizadores (sliders) su poder total / aura,
 * velocidad de movimiento, velocidad de ataque y altura de salto.
 */
public class PowerControlScreen extends Screen {

    private final Screen parentScreen;

    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 235;
    private int leftPos;
    private int topPos;

    // Valores normalizados de los 4 sliders
    private float currentPower = 1.0F;   // 0.01f a 1.0f
    private float currentSpeed = 1.0F;   // 0.10f a 1.0f
    private float currentAttack = 1.0F;  // 0.10f a 1.0f
    private float currentJump = 1.0F;    // 0.00f a 1.0f

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

        int sliderW = 280;
        int sliderH = 18;
        int startX = leftPos + 20;
        int curY = topPos + 48;

        // 1. Slider Poder Total / Supresión de Aura (1% a 100%)
        powerSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Poder Total / Aura", currentPower, 0.01F, 1.0F, "%", val -> this.currentPower = val);
        this.addRenderableWidget(powerSlider);

        // 2. Slider Velocidad de Movimiento (10% a 100%)
        curY += 34;
        speedSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Velocidad de Movimiento", currentSpeed, 0.10F, 1.0F, "%", val -> this.currentSpeed = val);
        this.addRenderableWidget(speedSlider);

        // 3. Slider Velocidad de Ataque (10% a 100%)
        curY += 34;
        attackSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Velocidad de Ataque", currentAttack, 0.10F, 1.0F, "%", val -> this.currentAttack = val);
        this.addRenderableWidget(attackSlider);

        // 4. Slider Altura de Salto (0% a 100%)
        curY += 34;
        jumpSlider = new PowerSlider(startX, curY, sliderW, sliderH,
                "Altura de Salto", currentJump, 0.00F, 1.0F, "%", val -> this.currentJump = val);
        this.addRenderableWidget(jumpSlider);

        // Botones de presets rápidos
        int presetY = topPos + 180;
        this.addRenderableWidget(Button.builder(Component.literal("§a⚡ 100% Máximo"), b -> {
            applyPreset(1.0F, 1.0F, 1.0F, 1.0F);
        }).bounds(leftPos + 20, presetY, 135, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("§b🏙 Modo Ciudad / Sigilo"), b -> {
            applyPreset(0.15F, 0.55F, 0.50F, 0.20F);
        }).bounds(leftPos + 165, presetY, 135, 18).build());

        // Botones de Guardar y Volver
        int bottomY = topPos + GUI_HEIGHT - 24;
        this.addRenderableWidget(Button.builder(Component.literal("§a✔ Guardar y Aplicar"), b -> {
            saveAndApply();
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(leftPos + 20, bottomY, 155, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("§cVolver"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(leftPos + 185, bottomY, 115, 18).build());
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

        // Aplicar localmente de inmediato para respuesta fluida
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.setPowerControl(currentPower, currentSpeed, currentAttack, currentJump);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Sin blur
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fondo oscurecido
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        // Marco estilizado
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF00D111A);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFD4AF37);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0x60D4AF37);

        // Cabecera
        guiGraphics.drawString(font, "§6⚡ CONTROL Y REGULACIÓN DE PODER ⚡", leftPos + 18, topPos + 10, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§7Suprime o libera tu energía física y espiritual a voluntad.", leftPos + 18, topPos + 22, 0xFFAAAAAA, false);

        // Línea dorada separadora
        guiGraphics.fill(leftPos + 12, topPos + 34, leftPos + GUI_WIDTH - 12, topPos + 35, 0x80D4AF37);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * Componente deslizador interactivo con porcentaje dinámico.
     */
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
