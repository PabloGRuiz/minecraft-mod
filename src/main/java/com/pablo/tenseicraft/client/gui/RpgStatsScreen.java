package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.client.ModKeyMappings;
import com.pablo.tenseicraft.data.GreekGod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.data.ThreatLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RpgStatsScreen extends Screen {

    private final Screen parentScreen;
    private static final int GUI_WIDTH = 390;
    private static final int GUI_HEIGHT = 248;
    private int leftPos;
    private int topPos;
    private int activeSubTab = 0; // 0 = Atributos, 1 = Bendición, 2 = Equipo y Técnicas

    public RpgStatsScreen(Screen parentScreen) {
        super(Component.translatable("tenseicraft.gui.stats_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // 1. Barra superior unificada
        RpgUiHelper.addNavigationBar(this, this::addRenderableWidget, leftPos, topPos, GUI_WIDTH, RpgUiHelper.NavTab.PROFILE);

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        int level = (data != null) ? data.getRpgLevel() : 1;

        // 2. Sub-pestañas del panel derecho (Atributos | Bendición | Equipo/Téc.)
        int rightX = leftPos + 122;
        int subY = topPos + 44;
        int subW = 84;
        String[] subLabels = {"Atributos", "Bendición", "Equipo/Téc."};
        for (int i = 0; i < subLabels.length; i++) {
            final int tabIdx = i;
            boolean active = (this.activeSubTab == tabIdx);
            Button subBtn = Button.builder(Component.literal((active ? "§e§l" : "§7") + subLabels[i]), b -> {
                this.activeSubTab = tabIdx;
                this.rebuildWidgets();
            }).bounds(rightX + i * (subW + 3), subY, subW, 16).build();
            subBtn.active = !active;
            this.addRenderableWidget(subBtn);
        }

        // 3. Botón inferior izquierdo (Bendición / Ascender / Volver al Inventario)
        int leftBtnX = leftPos + 10;
        int leftBtnY = topPos + GUI_HEIGHT - 26;
        int leftBtnW = 104;
        if (data != null && !data.hasDivineBlessingUnlocked()) {
            this.addRenderableWidget(Button.builder(Component.literal("§5✦ Sin Bendición"), b -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                            Component.literal("§d[Plano Astral] §eDerrota a tu primer enemigo y duerme en una cama para recibir tu Bendición Olímpica."),
                            false
                    );
                }
            }).bounds(leftBtnX, leftBtnY, leftBtnW, 18)
            .tooltip(Tooltip.create(Component.literal("§5[Progresión Bloqueada]\n§7Abate un enemigo y duerme en una cama para acudir al Plano Astral.")))
            .build());
        } else if (data != null && data.isAtMilestoneCap()) {
            this.addRenderableWidget(Button.builder(Component.literal("§d⚡ Ascender"), b -> {
                if (this.minecraft != null && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(
                            Component.literal("§d[Espacio Astral] §eAlcanzaste el Nivel " + data.getCurrentMilestoneCap() + ". Duerme en una cama para recibir tu Ascensión."),
                            false
                    );
                }
            }).bounds(leftBtnX, leftBtnY, leftBtnW, 18)
            .tooltip(Tooltip.create(Component.literal("§d[Hito Alcanzado]\n§eDuerme en una cama para ascender de rango.")))
            .build());
        } else {
            this.addRenderableWidget(Button.builder(Component.translatable("tenseicraft.gui.back_inventory"), b -> {
                if (this.minecraft != null) {
                    if (this.parentScreen instanceof InventoryScreen) {
                        this.minecraft.setScreen(this.parentScreen);
                    } else if (this.minecraft.player != null) {
                        this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
                    }
                }
            }).bounds(leftBtnX, leftBtnY, leftBtnW, 18).build());
        }

        // 4. Botones de asignación (+1) solo en la sub-pestaña de Atributos
        if (this.activeSubTab == 0 && data != null) {
            int pending = data.getPendingPointsCount(level);
            if (pending > 0) {
                int btnX = leftPos + GUI_WIDTH - 28;
                int cardStartY = topPos + 78;
                int cardStep = 39;

                addStatUpgradeButton(btnX, cardStartY + 8, "fortitude", "§c+", pending);
                addStatUpgradeButton(btnX, cardStartY + cardStep + 8, "dexterity", "§b+", pending);
                addStatUpgradeButton(btnX, cardStartY + cardStep * 2 + 8, "spirit", "§d+", pending);
                addStatUpgradeButton(btnX, cardStartY + cardStep * 3 + 8, "luck", "§a+", pending);
            }
        }
    }

    private void addStatUpgradeButton(int x, int y, String statId, String label, int pending) {
        Button btn = Button.builder(Component.literal(label), b -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.connection.sendCommand("tenseicraft choose " + statId);
                this.rebuildWidgets();
            }
        }).bounds(x, y, 18, 18)
        .tooltip(Tooltip.create(Component.literal(label + "1 ").append(Component.translatable("tenseicraft.stat." + statId + ".name"))
                .append("\n").append(Component.translatable("tenseicraft.stat." + statId + ".hover"))
                .append("\n§a").append(Component.translatable("tenseicraft.gui.stat_points_pending", pending))))
        .build();
        this.addRenderableWidget(btn);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.OPEN_STATS_KEY.get().matches(keyCode, scanCode)
                || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Sin blur shader de 1.21
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        guiGraphics.fill(0, 0, this.width, this.height, 0xB8080A12);
        RpgUiHelper.drawPanelFrame(guiGraphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        int currentLevel = (data != null) ? data.getRpgLevel() : 1;

        // 1. Sub-cabecera de Identidad (sin colisiones)
        Component rankOrThreatBadge;
        if (data != null && data.getReputationScore() < 0) {
            rankOrThreatBadge = ThreatLevel.getThreatForPlayer(player, data).getBadge();
        } else if (data != null && data.isGuildRegistered()) {
            rankOrThreatBadge = data.getRank().getBadge();
        } else {
            rankOrThreatBadge = Component.literal("[Civil]").withStyle(ChatFormatting.DARK_GRAY);
        }

        String leftIdentity = rankOrThreatBadge.getString() + " §e" + player.getName().getString() + " §7(Nv. §f" + currentLevel + "§7)";
        guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, leftIdentity, 225), leftPos + 10, topPos + 31, 0xFFFFFFFF, false);

        GreekGod patronGod = (data != null && data.hasDivineBlessingUnlocked()) ? data.getPatronGod() : null;
        String godRight = (patronGod != null)
                ? patronGod.getBadge().getString()
                : "§8✦ Sin Bendición";
        godRight = RpgUiHelper.clip(this.font, godRight, 138);
        guiGraphics.drawString(this.font, godRight, leftPos + GUI_WIDTH - 10 - this.font.width(godRight), topPos + 31, 0xFFFFFFFF, false);

        guiGraphics.hLine(leftPos + 8, leftPos + GUI_WIDTH - 8, topPos + 41, 0xFF34394C);

        // 2. Barra Lateral Izquierda (Modelo 3D + Hito XP + Moral)
        int sideX = leftPos + 10;
        int sideW = 104;
        int pY1 = topPos + 44;
        int pY2 = pY1 + 94;
        guiGraphics.fillGradient(sideX, pY1, sideX + sideW, pY2, 0x55000000, 0x88000000);
        guiGraphics.renderOutline(sideX, pY1, sideW, pY2 - pY1, 0xFF4A4D60);
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, sideX, pY1, sideX + sideW, pY2, 40, 0.0625f, mouseX, mouseY, player);

        renderMilestoneBar(guiGraphics, data, sideX, pY2 + 5, sideW);
        List<Component> repTooltip = renderReputationBar(guiGraphics, data, sideX, pY2 + 34, sideW, mouseX, mouseY);

        // Separador vertical entre columnas
        guiGraphics.vLine(leftPos + 118, topPos + 43, topPos + GUI_HEIGHT - 8, 0xFF34394C);

        // 3. Contenido Principal Derecho según sub-pestaña activa
        int rightX = leftPos + 122;
        int rightW = 258;
        List<Component> hoveredTooltip = null;

        if (activeSubTab == 0) {
            hoveredTooltip = renderStatsTab(guiGraphics, player, data, currentLevel, rightX, topPos + 64, rightW, mouseX, mouseY);
        } else if (activeSubTab == 1) {
            renderBlessingTab(guiGraphics, data, rightX, topPos + 64, rightW);
        } else {
            renderGearAndTechTab(guiGraphics, player, data, rightX, topPos + 64, rightW);
        }

        // 4. Widgets interactivos
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 5. Tooltips en la capa más alta
        if (hoveredTooltip != null && !hoveredTooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, hoveredTooltip, mouseX, mouseY);
        } else if (repTooltip != null && !repTooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, repTooltip, mouseX, mouseY);
        }
    }

    private void renderMilestoneBar(GuiGraphics g, PlayerRpgData data, int x, int y, int width) {
        if (data == null || !data.hasDivineBlessingUnlocked()) {
            g.drawString(this.font, RpgUiHelper.clip(this.font, "§5🔒 Sueña tras cazar", width), x, y, 0xFFAA88FF, false);
            g.fill(x, y + 10, x + width, y + 14, 0xFF222530);
            g.renderOutline(x, y + 10, width, 4, 0xFF555555);
            g.drawString(this.font, "§8Bloqueado", x, y + 16, 0xFF777777, false);
            return;
        }

        if (data.isAtMilestoneCap()) {
            g.drawString(this.font, "§d⚡ ¡Hito Nv." + data.getCurrentMilestoneCap() + "!", x, y, 0xFFFFAAFF, false);
            g.fillGradient(x, y + 10, x + width, y + 14, 0xFFFF55FF, 0xFF55FFFF);
            g.renderOutline(x, y + 10, width, 4, 0xFFFFDD33);
            g.drawString(this.font, "§eDuerme para ascender", x, y + 16, 0xFFFFDD66, false);
            return;
        }

        int curXp = data.getRpgXp();
        int reqXp = Math.max(1, data.getXpForNextLevel());
        float progress = Math.min(1.0f, (float) curXp / reqXp);
        int cap = data.getCurrentMilestoneCap();

        g.drawString(this.font, "§7Hito: §fNv." + cap + " §8(" + (int) (progress * 100) + "%)", x, y, 0xFFCCCCCC, false);
        int barY = y + 10;
        g.fill(x, barY, x + width, barY + 4, 0xFF222530);
        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            g.fillGradient(x, barY, x + fillWidth, barY + 4, 0xFF55FF55, 0xFF00AA00);
        }
        g.renderOutline(x, barY, width, 4, 0xFF555555);
        g.drawString(this.font, RpgUiHelper.clip(this.font, "§7XP: §f" + curXp + "§8/" + reqXp, width), x, y + 16, 0xFFAAAAAA, false);
    }

    private List<Component> renderReputationBar(GuiGraphics g, PlayerRpgData data, int x, int y, int width, int mouseX, int mouseY) {
        int score = (data != null) ? data.getReputationScore() : 0;
        com.pablo.tenseicraft.data.ReputationTier tier = (data != null) ? data.getReputationTier() : com.pablo.tenseicraft.data.ReputationTier.NEUTRAL;
        String scoreStr = (score > 0 ? "+" : "") + score;

        Component repName = Component.translatable(tier.getTranslationKey()).withStyle(tier.getColor());
        String labelText = "§7Moral: " + tier.getIcon() + " " + repName.getString();
        g.drawString(this.font, RpgUiHelper.clip(this.font, labelText, width), x, y, 0xFFD4D4D4, false);

        int barY = y + 10;
        int barH = 5;
        int midX = x + (width / 2);
        g.fill(x, barY, x + width, barY + barH, 0xFF181A22);
        g.renderOutline(x, barY, width, barH, 0xFF4A4E62);
        g.vLine(midX, barY - 1, barY + barH, 0xFFFFAA00);

        float normalized = Math.min(1.0f, Math.abs(score) / 100.0f);
        int fillLen = (int) ((width / 2.0f) * normalized);
        if (score > 0 && fillLen > 0) {
            g.fillGradient(midX, barY + 1, Math.min(x + width - 1, midX + fillLen), barY + barH - 1, 0xFF55FF55, 0xFF00AA00);
        } else if (score < 0 && fillLen > 0) {
            g.fillGradient(Math.max(x + 1, midX - fillLen), barY + 1, midX, barY + barH - 1, 0xFFFF3333, 0xFFAA0000);
        }
        g.drawString(this.font, "§8Puntuación: §f" + scoreStr + "§8/100", x, barY + 7, 0xFF888888, false);

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= barY + barH + 8) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("tenseicraft.gui.reputation_tooltip_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltip.add(Component.translatable("tenseicraft.gui.reputation_tooltip_score", scoreStr).withStyle(tier.getColor()));
            tooltip.add(Component.translatable("tenseicraft.gui.reputation_tooltip_desc." + tier.getId()).withStyle(ChatFormatting.GRAY));
            if (tier.getPriceModifier() < 0) {
                int discount = Math.round(Math.abs(tier.getPriceModifier()) * 100);
                tooltip.add(Component.literal("§a• Descuento comercial en aldeas: -" + discount + "%"));
            } else if (tier.getPriceModifier() > 0) {
                int penalty = Math.round(tier.getPriceModifier() * 100);
                tooltip.add(Component.literal("§c• Inflación por infamia: +" + penalty + "%"));
            }
            return tooltip;
        }
        return null;
    }

    private List<Component> renderStatsTab(GuiGraphics g, Player player, PlayerRpgData data, int currentLevel,
                                           int startX, int startY, int totalW, int mouseX, int mouseY) {
        int pendingPoints = (data != null) ? data.getPendingPointsCount(currentLevel) : 0;

        if (pendingPoints > 0) {
            g.drawString(this.font, Component.translatable("tenseicraft.gui.unassigned_points", pendingPoints), startX, startY + 2, 0xFFFFFF55, true);
        } else {
            g.drawString(this.font, "§7Atributos Principales §8(Pasa el cursor sobre cada ficha)", startX, startY + 2, 0xFFAAAAAA, false);
        }

        int fortPerm = (data != null) ? data.getPermanentPoints(StatType.FORTITUDE) : 0;
        int fortActive = (data != null) ? data.getActiveMilestonePoints(StatType.FORTITUDE, currentLevel) : 0;
        int fortCurios = (data != null) ? data.getCuriosBonus(player, StatType.FORTITUDE) : 0;
        int fortTotal = (data != null) ? data.getTotalStatPoints(player, StatType.FORTITUDE) : 0;

        int dexPerm = (data != null) ? data.getPermanentPoints(StatType.DEXTERITY) : 0;
        int dexActive = (data != null) ? data.getActiveMilestonePoints(StatType.DEXTERITY, currentLevel) : 0;
        int dexCurios = (data != null) ? data.getCuriosBonus(player, StatType.DEXTERITY) : 0;
        int dexTotal = (data != null) ? data.getTotalStatPoints(player, StatType.DEXTERITY) : 0;

        int spiPerm = (data != null) ? data.getPermanentPoints(StatType.SPIRIT) : 0;
        int spiActive = (data != null) ? data.getActiveMilestonePoints(StatType.SPIRIT, currentLevel) : 0;
        int spiCurios = (data != null) ? data.getCuriosBonus(player, StatType.SPIRIT) : 0;
        int spiTotal = (data != null) ? data.getTotalStatPoints(player, StatType.SPIRIT) : 0;

        int luckPerm = (data != null) ? data.getPermanentPoints(StatType.LUCK) : 0;
        int luckActive = (data != null) ? data.getActiveMilestonePoints(StatType.LUCK, currentLevel) : 0;
        int luckCurios = (data != null) ? data.getCuriosBonus(player, StatType.LUCK) : 0;
        int luckTotal = (data != null) ? data.getTotalStatPoints(player, StatType.LUCK) : 0;

        double critChance = (data != null) ? data.getCritChance(player) : 5.0;
        double critMult = (data != null) ? data.getCritDamageMultiplier(critChance) : 1.5;

        AttributeInstance hpAttr = player.getAttribute(Attributes.MAX_HEALTH);
        double liveHealth = (hpAttr != null) ? hpAttr.getValue() : 20.0;
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        double liveArmor = (armorAttr != null) ? armorAttr.getValue() : 0.0;
        double deflectChance = Math.min(70.0, fortTotal * 0.7);
        float maxMana = (data != null) ? data.getMaxMana(player) : 100.0f;
        float curMana = (data != null) ? data.getCurrentMana(player) : maxMana;
        double relDmgBonus = (data != null) ? data.getPhysicalRelationDamageBonus(player) : 0.0;
        double spiritSwordBonus = (data != null) ? data.getSpiritualSwordSpiritBonus(player) : 0.0;
        double hpDelta = (fortTotal >= 5) ? ((fortTotal - 5) * 2.0) : ((fortTotal - 5) * 1.5);

        int cardW = (pendingPoints > 0) ? (totalW - 22) : totalW;
        int cardH = 35;
        int cardStartY = startY + 14;
        int cardStep = 39;
        List<Component> hovered = null;

        // Card 1: Fortaleza
        int y0 = cardStartY;
        boolean h0 = isHover(mouseX, mouseY, startX, y0, cardW, cardH);
        drawStatCard(g, startX, y0, cardW, cardH, h0,
                "🛡️ §c§l" + Component.translatable("tenseicraft.stat.fortitude.name").getString() + ": §f" + fortTotal + " pts",
                "§8★" + fortPerm + " +Nv" + fortActive + (fortCurios > 0 ? " +" + fortCurios : ""),
                "§7Vida: §c" + (int) liveHealth + " HP §8| §7Armadura: §b" + String.format(Locale.ROOT, "%.1f", liveArmor),
                "§7Desvío Proy.: §e" + String.format(Locale.ROOT, "%.1f", deflectChance) + "% §8| §7Mitigación: §a" + String.format(Locale.ROOT, "%.1f", Math.min(35.0, fortTotal * 0.45)) + "%");
        if (h0) {
            hovered = List.of(
                    Component.literal("🛡️ §c§lFortaleza §7(" + fortTotal + " pts | Base = 5)"),
                    Component.literal("§8Nacimiento: " + fortPerm + " | Nivel: +" + fortActive + (fortCurios > 0 ? " | Accesorios: +" + fortCurios : "")),
                    Component.literal("§f• Vida Máxima: §c" + (hpDelta >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.1f", hpDelta) + " HP (§e" + (int) (liveHealth / 2.0) + "♥§f)"),
                    Component.literal("§f• Armadura Total: §b" + String.format(Locale.ROOT, "%.1f", liveArmor) + " §8| §fDaño (F+D): §c" + (relDmgBonus >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.2f", relDmgBonus)),
                    Component.literal("§f• Desvío Proyectiles: §e" + String.format(Locale.ROOT, "%.1f", deflectChance) + "% §8| §fReducción Plana: §a" + String.format(Locale.ROOT, "%.1f", Math.min(35.0, fortTotal * 0.45)) + "%")
            );
        }

        // Card 2: Destreza
        int y1 = cardStartY + cardStep;
        boolean h1 = isHover(mouseX, mouseY, startX, y1, cardW, cardH);
        double dexPctAtk = (dexTotal - 5) * 2.5;
        float dexCdrPct = Math.min(60.0f, dexTotal * 1.2f);
        drawStatCard(g, startX, y1, cardW, cardH, h1,
                "🎯 §b§l" + Component.translatable("tenseicraft.stat.dexterity.name").getString() + ": §f" + dexTotal + " pts",
                "§8★" + dexPerm + " +Nv" + dexActive + (dexCurios > 0 ? " +" + dexCurios : ""),
                "§7Vel. Atq.: §e" + (dexPctAtk >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.1f", dexPctAtk) + "% §8| §7Daño (F+D): §c" + (relDmgBonus >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.1f", relDmgBonus),
                "§7Parry: §e" + String.format(Locale.ROOT, "%.0f", Math.min(80.0, 15.0 + dexTotal * 1.2)) + "% §8| §7Red. Enfr.: §b-" + String.format(Locale.ROOT, "%.0f", dexCdrPct) + "%");
        if (h1 && hovered == null) {
            hovered = List.of(
                    Component.literal("🎯 §b§lDestreza §7(" + dexTotal + " pts | Base = 5)"),
                    Component.literal("§8Nacimiento: " + dexPerm + " | Nivel: +" + dexActive + (dexCurios > 0 ? " | Accesorios: +" + dexCurios : "")),
                    Component.literal("§f• Vel. Ataque: §e" + (dexPctAtk >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.1f", dexPctAtk) + "% §8| §fVelocidad: §a" + String.format(Locale.ROOT, "%.1f", (dexTotal - 5) * 1.2) + "%"),
                    Component.literal("§f• Sinergia Daño (F+D): §c" + (relDmgBonus >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.2f", relDmgBonus) + " §8| §fSalto: §d" + (dexTotal - 5) + "%"),
                    Component.literal("§f• Parry Base: §e" + String.format(Locale.ROOT, "%.1f", Math.min(80.0, 15.0 + dexTotal * 1.2)) + "% §8| §fCDR Técnicas: §b-" + String.format(Locale.ROOT, "%.1f", dexCdrPct) + "%")
            );
        }

        // Card 3: Espíritu
        int y2 = cardStartY + cardStep * 2;
        boolean h2 = isHover(mouseX, mouseY, startX, y2, cardW, cardH);
        drawStatCard(g, startX, y2, cardW, cardH, h2,
                "✨ §d§l" + Component.translatable("tenseicraft.stat.spirit.name").getString() + ": §f" + spiTotal + " pts",
                "§8★" + spiPerm + " +Nv" + spiActive + (spiCurios > 0 ? " +" + spiCurios : ""),
                "§7Maná: §b" + (int) curMana + "/" + (int) maxMana + " PM §8| §7Bono Esp. Espiritual: §d+" + String.format(Locale.ROOT, "%.1f", spiritSwordBonus),
                "§7Regen. Maná: §3+" + String.format(Locale.ROOT, "%.2f", Math.max(0.0f, (spiTotal - 5) * 0.05f)) + "/s §8| §7Bono XP RPG: §a+" + String.format(Locale.ROOT, "%.0f", Math.max(0.0, (spiTotal - 5) * 2.0)) + "%");
        if (h2 && hovered == null) {
            hovered = List.of(
                    Component.literal("✨ §d§lEspíritu §7(" + spiTotal + " pts | Base = 5)"),
                    Component.literal("§8Nacimiento: " + spiPerm + " | Nivel: +" + spiActive + (spiCurios > 0 ? " | Accesorios: +" + spiCurios : "")),
                    Component.literal("§f• Maná Máximo: §b" + (int) maxMana + " PM §8| §fRegen.: §3+" + String.format(Locale.ROOT, "%.2f", Math.max(0.0f, (spiTotal - 5) * 0.05f)) + " PM/s"),
                    Component.literal("§f• Bono Espada Espiritual: §d+" + String.format(Locale.ROOT, "%.2f", spiritSwordBonus) + " daño"),
                    Component.literal("§f• Multiplicador XP RPG: §a+" + String.format(Locale.ROOT, "%.0f", Math.max(0.0, (spiTotal - 5) * 2.0)) + "%")
            );
        }

        // Card 4: Suerte
        int y3 = cardStartY + cardStep * 3;
        boolean h3 = isHover(mouseX, mouseY, startX, y3, cardW, cardH);
        drawStatCard(g, startX, y3, cardW, cardH, h3,
                "🍀 §a§l" + Component.translatable("tenseicraft.stat.luck.name").getString() + ": §f" + luckTotal + " pts",
                "§8★" + luckPerm + " +Nv" + luckActive + (luckCurios > 0 ? " +" + luckCurios : ""),
                "§7Prob. Crítico: §e" + String.format(Locale.ROOT, "%.1f", critChance) + "% §8(§c" + String.format(Locale.ROOT, "%.2f", critMult) + "x§8)",
                "§7Drops Raros: §a+" + (luckTotal * 3) + "% §8| §7Suerte Vanilla: §b" + String.format(Locale.ROOT, "%.1f", (luckTotal - 5) * 0.2));
        if (h3 && hovered == null) {
            hovered = List.of(
                    Component.literal("🍀 §a§lSuerte §7(" + luckTotal + " pts | Base = 5)"),
                    Component.literal("§8Nacimiento: " + luckPerm + " | Nivel: +" + luckActive + (luckCurios > 0 ? " | Accesorios: +" + luckCurios : "")),
                    Component.literal("§f• Crítico: §e" + String.format(Locale.ROOT, "%.1f", critChance) + "% §8| §fDaño Crítico: §c" + String.format(Locale.ROOT, "%.2f", critMult) + "x"),
                    Component.literal("§f• Botín y Recompensas: §a+" + (luckTotal * 3) + "% §8| §fLuck Vanilla: §b" + String.format(Locale.ROOT, "%.1f", (luckTotal - 5) * 0.2))
            );
        }

        return hovered;
    }

    private void drawStatCard(GuiGraphics g, int x, int y, int w, int h, boolean hover,
                              String titleLeft, String badgeRight, String line1, String line2) {
        g.fill(x, y, x + w, y + h, hover ? 0xDD1C2338 : 0x90101423);
        g.renderOutline(x, y, w, h, hover ? 0xFFE5A93C : 0xFF2D334A);

        int badgeW = this.font.width(badgeRight);
        int maxTitleW = Math.max(40, w - badgeW - 14);
        g.drawString(this.font, RpgUiHelper.clip(this.font, titleLeft, maxTitleW), x + 5, y + 4, 0xFFFFFFFF, true);
        g.drawString(this.font, badgeRight, x + w - 5 - badgeW, y + 4, 0xFF888888, false);
        g.drawString(this.font, RpgUiHelper.clip(this.font, line1, w - 10), x + 5, y + 15, 0xFFCCCCCC, false);
        g.drawString(this.font, RpgUiHelper.clip(this.font, line2, w - 10), x + 5, y + 24, 0xFF99A0B8, false);
    }

    private void renderBlessingTab(GuiGraphics g, PlayerRpgData data, int x, int y, int w) {
        // Card Superior: Bendición Única del Dios Olímpico
        int godCardH = 104;
        g.fill(x, y, x + w, y + godCardH, 0x99121626);
        g.renderOutline(x, y, w, godCardH, 0xFFD4AF37);

        if (data == null || !data.hasDivineBlessingUnlocked() || data.getPatronGod() == null) {
            g.drawString(this.font, "§5✦ Bendición Divina Bloqueada", x + 7, y + 7, 0xFFFFFFFF, true);
            List<FormattedCharSequence> lines = this.font.split(
                    Component.literal("§7Abate a tu primer enemigo y duerme en una cama para acudir al Espacio Astral. Un Dios Olímpico te otorgará su Bendición Única."),
                    w - 14);
            int ly = y + 23;
            for (FormattedCharSequence line : lines) {
                g.drawString(this.font, line, x + 7, ly, 0xFFBBBBBB, false);
                ly += 10;
            }
        } else {
            GreekGod god = data.getPatronGod();
            int tier = GreekGod.getBlessingTier(data.getRank());
            String roman = tier == 1 ? "I" : (tier == 2 ? "II" : "III");

            g.drawString(this.font, god.getBadge(), x + 7, y + 6, 0xFFFFFFFF, true);
            String tierBadge = "§eNivel " + roman;
            g.drawString(this.font, tierBadge, x + w - 7 - this.font.width(tierBadge), y + 6, 0xFFFFFFFF, false);

            g.drawString(this.font, "§6✦ " + god.getBlessingName(), x + 7, y + 18, 0xFFFFDD66, true);
            g.hLine(x + 6, x + w - 6, y + 29, 0xFF394058);

            int textY = y + 33;
            for (String detail : god.getBlessingDetails(tier)) {
                for (FormattedCharSequence line : this.font.split(Component.literal("§f" + detail), w - 14)) {
                    if (textY > y + godCardH - 18) break;
                    g.drawString(this.font, line, x + 7, textY, 0xFFD8DEEE, false);
                    textY += 9;
                }
            }

            String scaleHint = (tier < 3)
                    ? "§8Escala automáticamente al ascender a Rango D/C (II) y B/A (III)."
                    : "§a✦ Poder divino en su máximo esplendor (Nivel III).";
            g.drawString(this.font, RpgUiHelper.clip(this.font, scaleHint, w - 14), x + 7, y + godCardH - 12, 0xFF999999, false);
        }

        // Card Inferior: Guardián Espiritual & Energía Vital
        int guardY = y + godCardH + 6;
        int guardH = 66;
        g.fill(x, guardY, x + w, guardY + guardH, 0x99121626);
        g.renderOutline(x, guardY, w, guardH, 0xFF55A065);

        String summonKey = ModKeyMappings.SUMMON_GUARDIAN_KEY.get().getTranslatedKeyMessage().getString();
        if (data == null || !data.hasChosenGuardian()) {
            g.drawString(this.font, "§d✦ Guardián Espiritual", x + 7, guardY + 7, 0xFFFFFFFF, true);
            g.drawString(this.font, "§7Se desbloquea al ascender a Rango B en el Plano Astral.", x + 7, guardY + 21, 0xFFAAAAAA, false);
            g.drawString(this.font, "§8Al invocarlo [" + summonKey + "] reunirá Energía Vital para ti.", x + 7, guardY + 34, 0xFF888888, false);
        } else {
            String gId = data.getChosenGuardian();
            String gName = switch (gId) {
                case "starry" -> "Starry (Espíritu Celestial)";
                case "chubby" -> "Chubby (Nubecita del Trueno)";
                default -> "Brambi (Venado de Brasa)";
            };
            g.drawString(this.font, RpgUiHelper.clip(this.font, "§d✦ Guardián: §f" + gName, w - 14), x + 7, guardY + 6, 0xFFFFFFFF, true);
            g.drawString(this.font, "§7Invocar: §e[" + summonKey + "] §8(50 PM) | Escala con Nivel RPG", x + 7, guardY + 18, 0xFFCCCCCC, false);

            int vital = data.getGuardianVitalEnergy();
            int maxVital = PlayerRpgData.MAX_GUARDIAN_VITAL_ENERGY;
            float pct = Math.min(1.0f, Math.max(0.0f, (float) vital / Math.max(1, maxVital)));

            g.drawString(this.font, "§aEnergía Vital: §e" + vital + "§7/" + maxVital, x + 7, guardY + 31, 0xFFFFFFFF, false);
            String dropHint = "§7→ Semillas de Vida";
            g.drawString(this.font, dropHint, x + w - 7 - this.font.width(dropHint), guardY + 31, 0xFF88FF88, false);

            int barX = x + 7;
            int barY = guardY + 42;
            int barW = w - 14;
            g.fill(barX, barY, barX + barW, barY + 5, 0xFF18221A);
            int fillW = (int) (barW * pct);
            if (fillW > 0) {
                g.fillGradient(barX, barY, barX + fillW, barY + 5, 0xFF55FF88, 0xFFFFD740);
            }
            g.renderOutline(barX, barY, barW, 5, 0xFF3E6648);
            g.drawString(this.font, "§8Absorbida al derrotar entidades cerca de tu guardián invocado.", barX, guardY + 51, 0xFF888888, false);
        }
    }

    private void renderGearAndTechTab(GuiGraphics g, Player player, PlayerRpgData data, int x, int y, int w) {
        // Card Superior: Accesorios Curios
        int accH = 60;
        g.fill(x, y, x + w, y + accH, 0x99121626);
        g.renderOutline(x, y, w, accH, 0xFF394058);
        g.drawString(this.font, Component.translatable("tenseicraft.gui.accessories_title"), x + 7, y + 6, 0xFFFFAA00, true);

        List<Component> equipped = new ArrayList<>();
        var curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isPresent()) {
            curiosOpt.get().findCurios(s -> !s.isEmpty()).forEach(res -> equipped.add(res.stack().getHoverName()));
        }

        if (equipped.isEmpty()) {
            g.drawString(this.font, Component.translatable("tenseicraft.gui.no_accessories"), x + 7, y + 20, 0xFF888888, false);
        } else {
            int rowY = y + 19;
            for (int i = 0; i < Math.min(3, equipped.size()); i++) {
                String itemLine = "• " + equipped.get(i).getString();
                g.drawString(this.font, RpgUiHelper.clip(this.font, itemLine, w - 14), x + 7, rowY, 0xFFFFFFFF, false);
                rowY += 12;
            }
            if (equipped.size() > 3) {
                g.drawString(this.font, "§7... y " + (equipped.size() - 3) + " más.", x + 7, rowY, 0xFFAAAAAA, false);
            }
        }

        // Card Inferior: Movilidad, Espada Espiritual y Técnicas
        int skY = y + accH + 6;
        int skH = 110;
        g.fill(x, skY, x + w, skY + skH, 0x99121626);
        g.renderOutline(x, skY, w, skH, 0xFF394058);
        g.drawString(this.font, "§b✦ Técnicas y Habilidades Aprendidas", x + 7, skY + 6, 0xFFFFFFFF, true);

        if (data == null) return;

        String dashKey = ModKeyMappings.DASH_KEY.get().getTranslatedKeyMessage().getString();
        String swordKey = ModKeyMappings.RELEASE_SWORD_KEY.get().getTranslatedKeyMessage().getString();
        String magicKey = ModKeyMappings.MAGIC_MODIFIER_KEY.get().getTranslatedKeyMessage().getString();
        String skillsKey = ModKeyMappings.OPEN_SKILLS_KEY.get().getTranslatedKeyMessage().getString();

        int learnedTechs = 0;
        for (com.pablo.tenseicraft.magic.SpiritualTechnique tech : com.pablo.tenseicraft.magic.SpiritualTechnique.values()) {
            if (data.hasSkill(tech.getId())) learnedTechs++;
        }

        List<String> badges = new ArrayList<>();
        badges.add(data.hasSkill("agility_double_jump")
                ? "§a⚡ Doble Salto"
                : "§7🔒 Doble Salto (" + data.getJumpCount() + "/2000)");
        badges.add(data.hasSkill("agility_dash")
                ? "§b💨 Dash [" + dashKey + "] (" + String.format(Locale.ROOT, "%.1f", data.getDashCooldownTicks(player) / 20.0f) + "s)"
                : "§7🔒 Dash (Aprende c/ Kaelen)");
        badges.add(data.hasReceivedSpiritualSword()
                ? "§5🗡 Espada Espiritual [" + swordKey + "]"
                : "§7🔒 Espada Espiritual (Alice)");
        badges.add(data.hasSkill("magic_affinity")
                ? "§d✦ Afinidad Espiritual"
                : "§7🔒 Afinidad Espiritual (Alice)");
        badges.add("§d✦ Técnicas Esp.: §e" + learnedTechs + "/" + com.pablo.tenseicraft.magic.SpiritualTechnique.values().length);
        badges.add(data.hasDivineBlessingUnlocked()
                ? "§6✦ Bendición Divina Activa"
                : "§7🔒 Bendición (Sueña tras cazar)");

        int colW = (w - 18) / 2;
        int cy = skY + 20;
        for (int i = 0; i < badges.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            int itemY = cy + row * 14;
            if (itemY > skY + skH - 22) break;
            int bx = x + 7 + col * (colW + 4);
            g.drawString(this.font, RpgUiHelper.clip(this.font, badges.get(i), colW), bx, itemY, 0xFFFFFFFF, false);
        }

        g.drawString(this.font, RpgUiHelper.clip(this.font,
                "§7Cánticos: §e[" + magicKey + "]§7+1/2/3 §8| §7Recordatorio: §e[" + skillsKey + "]", w - 14),
                x + 7, skY + skH - 13, 0xFFAAAAAA, false);
    }

    private boolean isHover(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}
