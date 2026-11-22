package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.items.ModItems;
import com.pablo.tenseicraft.network.PerformReforgePayload;
import com.pablo.tenseicraft.reforge.ReforgeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Interfaz de Reforja y Gambling con Cristales de Alma y ruleta circular estilo KeyDrop.
 * Soporta tanto la forja de Megu (hasta Rango B) como el Yunque Mágico (hasta Rango Génesis).
 */
public class ReforgeScreen extends Screen {

    private static final int GUI_WIDTH = 380;
    private static final int GUI_HEIGHT = 248;
    private static final long SPIN_DURATION_MS = 2800L;

    private final boolean isMagicAnvil;
    private final int meguEntityId;
    private final BlockPos anvilPos;

    private int leftPos;
    private int topPos;

    private final List<Integer> eligibleSlots = new ArrayList<>();
    private int selectedIdx = 0;

    private int selLesser = 0;
    private int selResonant = 0;
    private int selRadiant = 0;
    private int selPrimordial = 0;

    private boolean isWaitingServer = false;
    private boolean isSpinning = false;
    private long spinStartMs = 0L;
    private float spinTargetAngleDeg = 0.0f;
    private float spinWinChancePct = 0.0f;
    private boolean pendingSuccess = false;
    private String pendingSummary = "";
    private int lastClickTickStep = 0;

    private String resultBannerText = "";
    private boolean resultBannerSuccess = false;
    private long resultBannerUntilMs = 0L;

    public ReforgeScreen(boolean isMagicAnvil, int meguEntityId, BlockPos anvilPos) {
        super(Component.literal(isMagicAnvil ? "Yunque Mágico" : "Forja de Megu"));
        this.isMagicAnvil = isMagicAnvil;
        this.meguEntityId = meguEntityId;
        this.anvilPos = (anvilPos != null) ? anvilPos : BlockPos.ZERO;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        scanEligibleItems(getCurrentSlot());
        clampSelectedCrystals();

        boolean controlsActive = !isSpinning && !isWaitingServer;

        // Botón cerrar (top right)
        this.addRenderableWidget(Button.builder(Component.literal("§c✕"), b -> this.onClose())
                .bounds(leftPos + GUI_WIDTH - 22, topPos + 6, 16, 16)
                .tooltip(Tooltip.create(Component.literal("§cCerrar §7[ESC]")))
                .build());

        // Selectores de equipamiento en inventario
        Button prevItemBtn = Button.builder(Component.literal("§e◀"), b -> {
            if (!eligibleSlots.isEmpty()) {
                selectedIdx = (selectedIdx - 1 + eligibleSlots.size()) % eligibleSlots.size();
                this.rebuildWidgets();
            }
        }).bounds(leftPos + 15, topPos + 56, 18, 18).build();
        prevItemBtn.active = controlsActive && eligibleSlots.size() > 1;
        this.addRenderableWidget(prevItemBtn);

        Button nextItemBtn = Button.builder(Component.literal("§e▶"), b -> {
            if (!eligibleSlots.isEmpty()) {
                selectedIdx = (selectedIdx + 1) % eligibleSlots.size();
                this.rebuildWidgets();
            }
        }).bounds(leftPos + 180, topPos + 56, 18, 18).build();
        nextItemBtn.active = controlsActive && eligibleSlots.size() > 1;
        this.addRenderableWidget(nextItemBtn);

        // 4 Filas de Cristales de Alma (- / +)
        int crystalY = topPos + 111;
        int rowStep = 28;
        Player player = Minecraft.getInstance().player;

        addCrystalControls(0, crystalY, controlsActive, selLesser,
                countCrystal(player, ModItems.LESSER_SOUL_FRAGMENT.get()));
        addCrystalControls(1, crystalY + rowStep, controlsActive, selResonant,
                countCrystal(player, ModItems.RESONANT_SOUL_CRYSTAL.get()));
        addCrystalControls(2, crystalY + rowStep * 2, controlsActive, selRadiant,
                countCrystal(player, ModItems.RADIANT_SOUL_GEM.get()));
        addCrystalControls(3, crystalY + rowStep * 3, controlsActive, selPrimordial,
                countCrystal(player, ModItems.PRIMORDIAL_SOUL_CORE.get()));

        // Botón principal ¡REFORJAR!
        ItemStack current = getSelectedStack();
        ThreatLevel nextRank = ReforgeManager.getNextTargetRank(current);
        boolean meguBlocked = (!isMagicAnvil && nextRank != null && nextRank.ordinal() > ReforgeManager.MEGU_MAX_RANK.ordinal());
        float chance = computeLiveChance(nextRank);
        int totalSelected = selLesser + selResonant + selRadiant + selPrimordial;

        String btnLabel = isSpinning
                ? "§e✦ GIRANDO... ✦"
                : (meguBlocked ? "§c🔒 Límite de Megu" : "§a§l🎲 ¡REFORJAR!");
        Tooltip rollTip;
        if (current.isEmpty()) {
            rollTip = Tooltip.create(Component.literal("§7Lleva un arma, herramienta o armadura compatible en tu inventario.\n§cNota: La Espada Espiritual no puede reforjarse aquí."));
        } else if (nextRank == null) {
            rollTip = Tooltip.create(Component.literal("§e¡Este ítem ya alcanzó el rango supremo [Génesis]!"));
        } else if (meguBlocked) {
            rollTip = Tooltip.create(Component.literal("§6[Límite Mortal de Megu]\n§7Megu solo puede reforjar hasta Rango B.\n§eUsa un §dYunque Mágico §epara mejorar a Rango A → Génesis."));
        } else {
            rollTip = Tooltip.create(Component.literal(
                    "§e[Tirada de Reforja → " + nextRank.getShortBadge().getString() + "§e]\n"
                            + "§a• Éxito (" + String.format(Locale.ROOT, "%.1f%%", chance) + "): §7Aumenta el rango del ítem y añade atributos/efectos aleatorios.\n"
                            + "§c• Fallo (" + String.format(Locale.ROOT, "%.1f%%", Math.max(0.0f, 100.0f - chance)) + "): §c§l¡PIERDES EL ARMA!"));
        }

        Button reforgeBtn = Button.builder(Component.literal(btnLabel), b -> {
            int slot = getCurrentSlot();
            if (slot >= 0 && !isSpinning && !isWaitingServer) {
                isWaitingServer = true;
                resultBannerText = "";
                PacketDistributor.sendToServer(new PerformReforgePayload(
                        slot, selLesser, selResonant, selRadiant, selPrimordial,
                        isMagicAnvil, meguEntityId, anvilPos
                ));
                this.rebuildWidgets();
            }
        }).bounds(leftPos + 216, topPos + 190, 146, 20)
        .tooltip(rollTip)
        .build();

        reforgeBtn.active = controlsActive && !current.isEmpty() && nextRank != null && !meguBlocked && totalSelected > 0;
        this.addRenderableWidget(reforgeBtn);
    }

    private void addCrystalControls(int idx, int rowY, boolean active, int selected, int owned) {
        Button minusBtn = Button.builder(Component.literal("§c-"), b -> {
            int step = Screen.hasShiftDown() ? 5 : 1;
            adjustCrystal(idx, -step);
        }).bounds(leftPos + 154, rowY + 4, 18, 17)
        .tooltip(Tooltip.create(Component.literal("§7Restar §e1 §8(Shift: -5)")))
        .build();
        minusBtn.active = active && selected > 0;
        this.addRenderableWidget(minusBtn);

        Button plusBtn = Button.builder(Component.literal("§a+"), b -> {
            int step = Screen.hasShiftDown() ? 5 : 1;
            adjustCrystal(idx, step);
        }).bounds(leftPos + 175, rowY + 4, 18, 17)
        .tooltip(Tooltip.create(Component.literal("§7Añadir §e1 §8(Shift: +5)")))
        .build();
        plusBtn.active = active && selected < owned;
        this.addRenderableWidget(plusBtn);
    }

    private void adjustCrystal(int idx, int delta) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        switch (idx) {
            case 0 -> selLesser = clamp(selLesser + delta, countCrystal(player, ModItems.LESSER_SOUL_FRAGMENT.get()));
            case 1 -> selResonant = clamp(selResonant + delta, countCrystal(player, ModItems.RESONANT_SOUL_CRYSTAL.get()));
            case 2 -> selRadiant = clamp(selRadiant + delta, countCrystal(player, ModItems.RADIANT_SOUL_GEM.get()));
            case 3 -> selPrimordial = clamp(selPrimordial + delta, countCrystal(player, ModItems.PRIMORDIAL_SOUL_CORE.get()));
        }
        this.rebuildWidgets();
    }

    private int clamp(int val, int max) {
        return Math.max(0, Math.min(max, val));
    }

    private void clampSelectedCrystals() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        selLesser = clamp(selLesser, countCrystal(player, ModItems.LESSER_SOUL_FRAGMENT.get()));
        selResonant = clamp(selResonant, countCrystal(player, ModItems.RESONANT_SOUL_CRYSTAL.get()));
        selRadiant = clamp(selRadiant, countCrystal(player, ModItems.RADIANT_SOUL_GEM.get()));
        selPrimordial = clamp(selPrimordial, countCrystal(player, ModItems.PRIMORDIAL_SOUL_CORE.get()));
    }

    private void scanEligibleItems(int preferredSlot) {
        eligibleSlots.clear();
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack s = player.getInventory().items.get(i);
            if (ReforgeManager.canReforge(s)) {
                eligibleSlots.add(i);
            }
        }
        if (eligibleSlots.isEmpty()) {
            selectedIdx = 0;
        } else if (preferredSlot >= 0) {
            int found = eligibleSlots.indexOf(preferredSlot);
            selectedIdx = (found >= 0) ? found : Math.min(selectedIdx, eligibleSlots.size() - 1);
        } else {
            selectedIdx = Math.min(selectedIdx, eligibleSlots.size() - 1);
        }
    }

    private int getCurrentSlot() {
        if (eligibleSlots.isEmpty() || selectedIdx < 0 || selectedIdx >= eligibleSlots.size()) {
            return -1;
        }
        return eligibleSlots.get(selectedIdx);
    }

    private ItemStack getSelectedStack() {
        Player player = Minecraft.getInstance().player;
        int slot = getCurrentSlot();
        if (player == null || slot < 0 || slot >= player.getInventory().items.size()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().items.get(slot);
    }

    private int countCrystal(Player player, Item item) {
        if (player == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private float computeLiveChance(ThreatLevel targetRank) {
        if (targetRank == null) return 0.0f;
        Player player = Minecraft.getInstance().player;
        int luck = 5;
        if (player != null) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                luck = data.getTotalStatPoints(player, StatType.LUCK);
            }
        }
        return ReforgeManager.calculateSuccessChance(targetRank, selLesser, selResonant, selRadiant, selPrimordial, luck);
    }

    public void startSpinResult(boolean success, float winChancePct, float finalAngleDeg, String summary) {
        this.isWaitingServer = false;
        this.isSpinning = true;
        this.spinStartMs = System.currentTimeMillis();
        this.spinWinChancePct = winChancePct;
        this.spinTargetAngleDeg = 4.0f * 360.0f + finalAngleDeg;
        this.pendingSuccess = success;
        this.pendingSummary = summary != null ? summary : "";
        this.lastClickTickStep = 0;
        this.rebuildWidgets();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB8080A12);
        RpgUiHelper.drawPanelFrame(graphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Cabecera
        String title = isMagicAnvil ? "§d§l⚒ Yunque Mágico" : "§6§l⚒ Forja de Megu";
        graphics.drawString(this.font, title, leftPos + 12, topPos + 10, 0xFFFFFFFF, true);

        String capBadge = isMagicAnvil ? "§5Rango F → §eGénesis" : "§eTope de Megu: §dRango B";
        graphics.drawString(this.font, capBadge, leftPos + GUI_WIDTH - 28 - this.font.width(capBadge), topPos + 10, 0xFFFFFFFF, false);

        // Separador vertical central
        graphics.vLine(leftPos + 206, topPos + 30, topPos + GUI_HEIGHT - 10, 0xFF34394C);

        // === COLUMNA IZQUIERDA ===
        ItemStack selected = getSelectedStack();
        ThreatLevel curRank = ReforgeManager.getReforgeRank(selected);
        ThreatLevel nextRank = ReforgeManager.getNextTargetRank(selected);
        boolean meguBlocked = (!isMagicAnvil && nextRank != null && nextRank.ordinal() > ReforgeManager.MEGU_MAX_RANK.ordinal());

        // Tarjeta del equipamiento seleccionado
        int cardX = leftPos + 10;
        int cardY = topPos + 34;
        int cardW = 192;
        int cardH = 72;
        boolean itemHover = mouseX >= cardX + 34 && mouseX <= cardX + 168 && mouseY >= cardY && mouseY <= cardY + 50;
        graphics.fill(cardX, cardY, cardX + cardW, cardY + cardH, itemHover ? 0xAA172034 : 0x66101524);
        graphics.renderOutline(cardX, cardY, cardW, cardH, meguBlocked ? 0xFFCC4444 : 0xFF555D7A);

        if (selected.isEmpty()) {
            boolean hasSpiritualSword = (player.getMainHandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem)
                    || (player.getOffhandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem);
            graphics.drawCenteredString(this.font, "§7Sin equipamiento elegible", cardX + cardW / 2, cardY + 18, 0xFFAAAAAA);
            graphics.drawCenteredString(this.font,
                    hasSpiritualSword ? "§c⚠ La Espada Espiritual está excluida" : "§8Lleva armas/armaduras en tu inventario",
                    cardX + cardW / 2, cardY + 34, 0xFF996666);
        } else {
            graphics.renderItem(selected, cardX + 28, cardY + 23);
            graphics.renderItemDecorations(this.font, selected, cardX + 28, cardY + 23);

            String itemName = selected.getHoverName().getString();
            graphics.drawString(this.font, RpgUiHelper.clip(this.font, "§f" + itemName, 122), cardX + 50, cardY + 10, 0xFFFFFFFF, true);

            String curStr = (curRank != null) ? curRank.getShortBadge().getString() : "§7[Sin Rango]";
            String nextStr = (nextRank != null) ? nextRank.getShortBadge().getString() : "§e[MÁXIMO]";
            graphics.drawString(this.font, RpgUiHelper.clip(this.font, curStr + " §7→ " + nextStr, 122), cardX + 50, cardY + 24, 0xFFFFFFFF, false);

            String statusNote;
            if (nextRank == null) {
                statusNote = "§e✦ Rango Génesis Supremo alcanzado";
            } else if (meguBlocked) {
                statusNote = "§c⚠ Requiere Yunque Mágico (Rango A+)";
            } else if (player.getMainHandItem().getItem() instanceof com.pablo.tenseicraft.items.SpiritualSwordItem) {
                statusNote = "§8(Espada Espiritual protegida / excluida)";
            } else {
                statusNote = "§7Pieza " + (selectedIdx + 1) + "/" + eligibleSlots.size() + " §8| Pasa cursor para ver stats";
            }
            graphics.drawString(this.font, RpgUiHelper.clip(this.font, statusNote, cardW - 10), cardX + 6, cardY + 56, 0xFFBBBBBB, false);
        }

        // 4 filas de Cristales de Alma
        int crystalY = topPos + 111;
        int rowStep = 28;
        drawCrystalRow(graphics, cardX, crystalY, ModItems.LESSER_SOUL_FRAGMENT.get(),
                "§bAlma Menor §8(x1)", selLesser, countCrystal(player, ModItems.LESSER_SOUL_FRAGMENT.get()), 0xFF3388AA);
        drawCrystalRow(graphics, cardX, crystalY + rowStep, ModItems.RESONANT_SOUL_CRYSTAL.get(),
                "§dResonante §8(x5)", selResonant, countCrystal(player, ModItems.RESONANT_SOUL_CRYSTAL.get()), 0xFF8844AA);
        drawCrystalRow(graphics, cardX, crystalY + rowStep * 2, ModItems.RADIANT_SOUL_GEM.get(),
                "§6Radiante §8(x25)", selRadiant, countCrystal(player, ModItems.RADIANT_SOUL_GEM.get()), 0xFFBB8822);
        drawCrystalRow(graphics, cardX, crystalY + rowStep * 3, ModItems.PRIMORDIAL_SOUL_CORE.get(),
                "§cPrimordial §8(x125)", selPrimordial, countCrystal(player, ModItems.PRIMORDIAL_SOUL_CORE.get()), 0xFFAA3344);

        // Poder anímico total en pie izquierdo
        int totalPower = ReforgeManager.computeSoulPower(selLesser, selResonant, selRadiant, selPrimordial);
        int reqPower = (nextRank != null) ? ReforgeManager.getRequiredSoulPowerForMax(nextRank) : 0;
        String powerSummary = (nextRank != null)
                ? "§7Poder Anímico: §d" + totalPower + " §8/ §f" + reqPower + " §8(Shift ±5)"
                : "§7Poder Anímico: §eMÁXIMO";
        graphics.drawString(this.font, RpgUiHelper.clip(this.font, powerSummary, cardW), cardX, topPos + 227, 0xFFCCCCCC, false);

        // === COLUMNA DERECHA: RULETA CIRCULAR ===
        long now = System.currentTimeMillis();
        float liveChance = computeLiveChance(nextRank);
        float displayChance = isSpinning ? spinWinChancePct : liveChance;
        float pointerAngle = 0.0f;

        if (isSpinning) {
            double t = Math.min(1.0, Math.max(0.0, (now - spinStartMs) / (double) SPIN_DURATION_MS));
            double eased = 1.0 - Math.pow(1.0 - t, 3.0);
            double traveled = spinTargetAngleDeg * eased;
            pointerAngle = (float) (traveled % 360.0);

            int clickStep = (int) (traveled / 18.0);
            if (clickStep > lastClickTickStep && player != null) {
                lastClickTickStep = clickStep;
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.32f, 1.55f);
            }

            if (t >= 1.0) {
                finishSpin();
            }
        }

        int wheelCx = leftPos + 289;
        int wheelCy = topPos + 98;
        renderRouletteWheel(graphics, wheelCx, wheelCy, displayChance, pointerAngle);

        // Banner de resultado debajo de la ruleta
        if (!resultBannerText.isEmpty() && now < resultBannerUntilMs) {
            int bx = leftPos + 212;
            int by = topPos + 158;
            int bw = 156;
            int bh = 26;
            graphics.fill(bx, by, bx + bw, by + bh, resultBannerSuccess ? 0x66124420 : 0x664A1218);
            graphics.renderOutline(bx, by, bw, bh, resultBannerSuccess ? 0xFF44FF77 : 0xFFFF4455);

            List<FormattedText> lines = this.font.getSplitter().splitLines(resultBannerText, bw - 8, Style.EMPTY);
            int ly = by + 4;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                graphics.drawString(this.font, lines.get(i).getString(), bx + 4, ly, 0xFFFFFFFF, false);
                ly += 10;
            }
        } else {
            String warn = (meguBlocked)
                    ? "§eUsa un Yunque Mágico para continuar"
                    : "§c⚠ Si la tirada falla, pierdes el ítem";
            graphics.drawCenteredString(this.font, warn, leftPos + 289, topPos + 168, 0xFFCC7777);
        }

        // Pie derecho
        graphics.drawCenteredString(this.font,
                "§8Prob. Máx Base: 95% §7(+Suerte)",
                leftPos + 289, topPos + 216, 0xFF888888);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (itemHover && !selected.isEmpty() && this.minecraft != null) {
            List<Component> tt = Screen.getTooltipFromItem(this.minecraft, selected);
            graphics.renderComponentTooltip(this.font, tt, mouseX, mouseY);
        }
    }

    private void drawCrystalRow(GuiGraphics graphics, int x, int y, Item crystalItem, String title,
                                int selected, int owned, int borderColor) {
        int w = 192;
        int h = 25;
        graphics.fill(x, y, x + w, y + h, 0x66101524);
        graphics.renderOutline(x, y, w, h, selected > 0 ? borderColor : 0xFF2D334A);

        graphics.renderItem(new ItemStack(crystalItem), x + 4, y + 4);
        graphics.drawString(this.font, RpgUiHelper.clip(this.font, title, 120), x + 24, y + 4, 0xFFFFFFFF, false);
        String countStr = "§7Usar: " + (selected > 0 ? "§a" : "§e") + selected + " §8/ §7" + owned;
        graphics.drawString(this.font, countStr, x + 24, y + 14, 0xFFAAAAAA, false);
    }

    private void renderRouletteWheel(GuiGraphics graphics, int cx, int cy, float winChancePct, float pointerAngleDeg) {
        // 1. Marcas radiales exteriores cada 10°
        for (int deg = 0; deg < 360; deg += 10) {
            double rad = Math.toRadians(deg - 90.0);
            int x1 = cx + (int) Math.round(Math.cos(rad) * 49.0);
            int y1 = cy + (int) Math.round(Math.sin(rad) * 49.0);
            int x2 = cx + (int) Math.round(Math.cos(rad) * 53.0);
            int y2 = cy + (int) Math.round(Math.sin(rad) * 53.0);
            drawPixelPoint(graphics, x1, y1, 0xFF4A5068);
            drawPixelPoint(graphics, (x1 + x2) / 2, (y1 + y2) / 2, 0xFF4A5068);
            drawPixelPoint(graphics, x2, y2, 0xFF4A5068);
        }

        // 2. Anillo principal (verde para zona de éxito, gris oscuro para fallo)
        double winArcDeg = Math.min(360.0, Math.max(0.0, winChancePct * 3.6));
        for (double deg = 0.0; deg < 360.0; deg += 0.6) {
            boolean isWin = deg <= winArcDeg && winChancePct > 0.0f;
            int colOuter = isWin ? 0xFF15D845 : 0xFF282D3B;
            int colInner = isWin ? 0xFF44FF70 : 0xFF33394A;
            double rad = Math.toRadians(deg - 90.0);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            for (double r = 41.0; r <= 46.0; r += 1.0) {
                int px = cx + (int) Math.round(cos * r);
                int py = cy + (int) Math.round(sin * r);
                graphics.fill(px, py, px + 2, py + 2, (r < 43.0) ? colInner : colOuter);
            }
        }

        // 3. Flecha dorada giratoria apuntando al anillo desde el interior
        double tipRad = Math.toRadians(pointerAngleDeg - 90.0);
        double leftRad = Math.toRadians(pointerAngleDeg - 90.0 - 8.5);
        double rightRad = Math.toRadians(pointerAngleDeg - 90.0 + 8.5);

        double tipX = cx + Math.cos(tipRad) * 39.5;
        double tipY = cy + Math.sin(tipRad) * 39.5;
        double leftX = cx + Math.cos(leftRad) * 31.0;
        double leftY = cy + Math.sin(leftRad) * 31.0;
        double rightX = cx + Math.cos(rightRad) * 31.0;
        double rightY = cy + Math.sin(rightRad) * 31.0;

        for (double s = 0.0; s <= 1.0; s += 0.1) {
            double bx = leftX + (rightX - leftX) * s;
            double by = leftY + (rightY - leftY) * s;
            for (double t = 0.0; t <= 1.0; t += 0.15) {
                int px = (int) Math.round(bx + (tipX - bx) * t);
                int py = (int) Math.round(by + (tipY - by) * t);
                graphics.fill(px - 1, py - 1, px + 1, py + 1, 0xFFFFC815);
            }
        }

        // 4. Porcentaje central grande y etiqueta de probabilidad
        String pctStr = String.format(Locale.ROOT, "%.1f%%", winChancePct);
        int pctColor = (winChancePct >= 55.0f) ? 0xFF22FF55
                : (winChancePct >= 25.0f ? 0xFFFFCC33 : (winChancePct > 0.0f ? 0xFFFF5555 : 0xFF888888));

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy - 8, 0);
        graphics.pose().scale(1.45f, 1.45f, 1.0f);
        graphics.drawCenteredString(this.font, pctStr, 0, 0, pctColor);
        graphics.pose().popPose();

        String label;
        if (winChancePct <= 0.0f) label = "Sin Cristales";
        else if (winChancePct >= 75.0f) label = "Muy Alta";
        else if (winChancePct >= 50.0f) label = "High chance";
        else if (winChancePct >= 25.0f) label = "Media";
        else label = "Arriesgada";
        graphics.drawCenteredString(this.font, label, cx, cy + 10, 0xFFCCCCCC);
    }

    private void drawPixelPoint(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 1, y + 1, color);
    }

    private void finishSpin() {
        this.isSpinning = false;
        this.resultBannerText = this.pendingSummary;
        this.resultBannerSuccess = this.pendingSuccess;
        this.resultBannerUntilMs = System.currentTimeMillis() + 5500L;

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            if (pendingSuccess) {
                player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.2f);
                player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
            } else {
                player.playSound(SoundEvents.ITEM_BREAK, 1.0f, 0.8f);
                player.playSound(SoundEvents.ANVIL_DESTROY, 0.7f, 0.9f);
            }
        }

        int preferredSlot = pendingSuccess ? getCurrentSlot() : -1;
        scanEligibleItems(preferredSlot);
        clampSelectedCrystals();
        this.rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
