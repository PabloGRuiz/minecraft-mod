package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.items.ExperienceAmuletItem;
import com.pablo.rpglevels.items.WarriorRingItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class RpgStatsScreen extends Screen {

    private final Screen parentScreen;
    private final int GUI_WIDTH = 348;
    private final int GUI_HEIGHT = 238;
    private int leftPos;
    private int topPos;

    public RpgStatsScreen(Screen parentScreen) {
        super(Component.translatable("rpglevels.gui.stats_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        int level = (data != null) ? data.getRpgLevel() : 1;

        // 1. Button to switch back to normal Inventory
        this.addRenderableWidget(Button.builder(Component.translatable("rpglevels.gui.back_inventory"), button -> {
            if (this.minecraft != null) {
                if (this.parentScreen instanceof InventoryScreen) {
                    this.minecraft.setScreen(this.parentScreen);
                } else if (this.minecraft.player != null) {
                    this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
                } else {
                    this.onClose();
                }
            }
        }).bounds(leftPos + GUI_WIDTH - 82, topPos + GUI_HEIGHT - 23, 72, 17).build());

        // 1.5 Button to open Quests Journal
        this.addRenderableWidget(Button.builder(Component.translatable("rpglevels.gui.quests_button"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new RpgQuestsScreen(this));
            }
        }).bounds(leftPos + GUI_WIDTH - 158, topPos + GUI_HEIGHT - 23, 72, 17).build());

        // 2. Ascension / Divine Blessing status button
        if (data != null) {
            if (!data.hasDivineBlessingUnlocked()) {
                this.addRenderableWidget(Button.builder(Component.literal("§5✦ Sin Bendición"), button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.displayClientMessage(
                                Component.literal("§d[Plano Astral] §eDerrota a tu primer enemigo y ve a dormir a una cama para que un Dios Olímpico te otorgue su Bendición y desbloquee tu subida de nivel."),
                                false
                        );
                    }
                }).bounds(leftPos + 12, topPos + GUI_HEIGHT - 23, 95, 17)
                .tooltip(Tooltip.create(Component.literal("§5[Progresión Bloqueada]\n§7Hasta tener tu primer encuentro con un Dios en el Plano Astral, solo cuentas con tus estadísticas de nacimiento.\n§eAbate a un enemigo y duerme en una cama.")))
                .build());
            } else if (data.isAtMilestoneCap()) {
                this.addRenderableWidget(Button.builder(Component.literal("§d⚡ Ascender (Dormir)"), button -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.displayClientMessage(
                                Component.literal("§d[Espacio Astral] §eHas alcanzado el Hito de Nivel " + data.getCurrentMilestoneCap() + ". Duerme en una cama para que tu Dios te otorgue la Ascensión."),
                                false
                        );
                    }
                }).bounds(leftPos + 12, topPos + GUI_HEIGHT - 23, 110, 17)
                .tooltip(Tooltip.create(Component.literal("§d[Hito de Rango Alcanzado]\n§eHas llegado al Nivel " + data.getCurrentMilestoneCap() + ".\n§6Duerme en una cama para acudir al Espacio Astral y recibir tu Ascensión de Rango.")))
                .build());
            }
        }

        // 3. Interactive Upgrade Buttons for Pending Attribute Points (1 by 1)
        if (data != null) {
            int pending = data.getPendingPointsCount(level);
            if (pending > 0) {
                int btnX = leftPos + GUI_WIDTH - 24;

                // Upgrade Fortitude button (+1 point)
                Button btnFort = Button.builder(Component.literal("§c+"), b -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.connection.sendCommand("rpgstats choose fortitude");
                        this.rebuildWidgets();
                    }
                }).bounds(btnX, topPos + 41, 16, 14)
                .tooltip(Tooltip.create(Component.literal("§c+1 ").append(Component.translatable("rpglevels.stat.fortitude.name"))
                        .append("\n").append(Component.translatable("rpglevels.stat.fortitude.hover"))
                        .append("\n§a").append(Component.translatable("rpglevels.gui.stat_points_pending", pending))))
                .build();
                this.addRenderableWidget(btnFort);

                // Upgrade Dexterity button (+1 point)
                Button btnDex = Button.builder(Component.literal("§b+"), b -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.connection.sendCommand("rpgstats choose dexterity");
                        this.rebuildWidgets();
                    }
                }).bounds(btnX, topPos + 69, 16, 14)
                .tooltip(Tooltip.create(Component.literal("§b+1 ").append(Component.translatable("rpglevels.stat.dexterity.name"))
                        .append("\n").append(Component.translatable("rpglevels.stat.dexterity.hover"))
                        .append("\n§a").append(Component.translatable("rpglevels.gui.stat_points_pending", pending))))
                .build();
                this.addRenderableWidget(btnDex);

                // Upgrade Spirit button (+1 point)
                Button btnSpi = Button.builder(Component.literal("§d+"), b -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.connection.sendCommand("rpgstats choose spirit");
                        this.rebuildWidgets();
                    }
                }).bounds(btnX, topPos + 97, 16, 14)
                .tooltip(Tooltip.create(Component.literal("§d+1 ").append(Component.translatable("rpglevels.stat.spirit.name"))
                        .append("\n").append(Component.translatable("rpglevels.stat.spirit.hover"))
                        .append("\n§a").append(Component.translatable("rpglevels.gui.stat_points_pending", pending))))
                .build();
                this.addRenderableWidget(btnSpi);

                // Upgrade Luck button (+1 point)
                Button btnLuck = Button.builder(Component.literal("§a+"), b -> {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        this.minecraft.player.connection.sendCommand("rpgstats choose luck");
                        this.rebuildWidgets();
                    }
                }).bounds(btnX, topPos + 125, 16, 14)
                .tooltip(Tooltip.create(Component.literal("§a+1 ").append(Component.translatable("rpglevels.stat.luck.name"))
                        .append("\n").append(Component.translatable("rpglevels.stat.luck.hover"))
                        .append("\n§a").append(Component.translatable("rpglevels.gui.stat_points_pending", pending))))
                .build();
                this.addRenderableWidget(btnLuck);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intencionalmente vacío: anula processBlurEffect de Minecraft 1.21 para que la interfaz no se vea difuminada
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // Fondo oscuro y nítido para atenuar el mundo 3D sin shader de desenfoque
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        Component rankOrThreatBadge;
        if (data != null && data.getReputationScore() < 0) {
            ThreatLevel threat = ThreatLevel.getThreatForPlayer(player, data);
            rankOrThreatBadge = threat.getBadge();
        } else if (data != null && data.isGuildRegistered()) {
            rankOrThreatBadge = data.getRank().getBadge();
        } else {
            rankOrThreatBadge = Component.literal("[Civil No Registrado]").withStyle(ChatFormatting.DARK_GRAY);
        }

        // 1. Dark Fantasy Background Frame
        renderBackgroundFrame(guiGraphics);

        // 2. Title & Header with Rank/Threat Badge & Patron God
        int currentLevel = (data != null) ? data.getRpgLevel() : 1;
        guiGraphics.drawString(this.font, Component.translatable("rpglevels.gui.header_title"), leftPos + 12, topPos + 8, 0xFFFFAA00, true);

        com.pablo.rpglevels.data.GreekGod patronGod = (data != null && data.hasDivineBlessingUnlocked()) ? data.getPatronGod() : null;
        Component godSuffix = (patronGod != null)
                ? Component.literal(" §8| ").append(patronGod.getBadge())
                : Component.literal(" §8| §7Sin Bendición");

        Component playerInfo = rankOrThreatBadge.copy()
                .append(Component.literal(" §e" + player.getName().getString() + " §7(Nv.RPG §f" + currentLevel + "§7)"))
                .append(godSuffix);
        int playerInfoW = this.font.width(playerInfo);
        guiGraphics.drawString(this.font, playerInfo, leftPos + GUI_WIDTH - 12 - playerInfoW, topPos + 8, 0xFFFFFFFF, false);

        // 3. Render 3D Player Entity
        int pX1 = leftPos + 12;
        int pY1 = topPos + 25;
        int pX2 = leftPos + 80;
        int pY2 = topPos + 138;
        guiGraphics.fillGradient(pX1, pY1, pX2, pY2, 0x55000000, 0x88000000);
        guiGraphics.renderOutline(pX1, pY1, pX2 - pX1, pY2 - pY1, 0xFF4A4D60);
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, pX1, pY1, pX2, pY2, 42, 0.0625f, mouseX, mouseY, player);

        // 4. Milestone & RPG XP Progress Bar below player
        renderMilestoneBar(guiGraphics, data, pX1, pY2 + 3, pX2 - pX1);

        // 4b. Moral Reputation & Infamy Bar below Milestone Bar
        List<Component> repTooltip = renderReputationBar(guiGraphics, data, pX1, pY2 + 18, pX2 - pX1, mouseX, mouseY);

        // 5. Stats Breakdown with Interactive Toolinfo Cards
        List<Component> statTooltip = renderStatsBreakdown(guiGraphics, player, data, currentLevel, leftPos + 88, topPos + 25, mouseX, mouseY);

        // 6. Active Curios / Accessories Summary
        renderCuriosSummary(guiGraphics, player, leftPos + 12, topPos + 172);

        // 7. Render interactive button widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 8. Render hovered tooltips on the very top layer
        if (statTooltip != null && !statTooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, statTooltip, mouseX, mouseY);
        } else if (repTooltip != null && !repTooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(this.font, repTooltip, mouseX, mouseY);
        }
    }

    private void renderBackgroundFrame(GuiGraphics guiGraphics) {
        guiGraphics.fillGradient(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF012141F, 0xF01C2030);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFD4AF37);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0xFF35394A);

        // Separator lines
        guiGraphics.hLine(leftPos + 8, leftPos + GUI_WIDTH - 8, topPos + 21, 0xFF5A5F7A);
        guiGraphics.hLine(leftPos + 8, leftPos + GUI_WIDTH - 8, topPos + 168, 0xFF5A5F7A);
        guiGraphics.hLine(leftPos + 8, leftPos + GUI_WIDTH - 8, topPos + 208, 0xFF5A5F7A);
    }

    private void renderMilestoneBar(GuiGraphics guiGraphics, PlayerRpgData data, int x, int y, int width) {
        if (data == null || !data.hasDivineBlessingUnlocked()) {
            guiGraphics.drawString(this.font, Component.literal("§5🔒 Sueña tras cazar"), x, y, 0xFFAA88FF, false);
            int barY = y + 8;
            guiGraphics.fill(x, barY, x + width, barY + 3, 0xFF222530);
            guiGraphics.renderOutline(x, barY, width, 3, 0xFF555555);
            return;
        }

        if (data.isAtMilestoneCap()) {
            guiGraphics.drawString(this.font, Component.literal("§d⚡ ¡Duerme! (Hito)"), x, y, 0xFFFFAAFF, false);
            int barY = y + 8;
            guiGraphics.fillGradient(x, barY, x + width, barY + 4, 0xFFFF55FF, 0xFF55FFFF);
            guiGraphics.renderOutline(x, barY, width, 4, 0xFFFFDD33);
            return;
        }

        int curXp = data.getRpgXp();
        int reqXp = Math.max(1, data.getXpForNextLevel());
        float progress = Math.min(1.0f, (float) curXp / reqXp);
        int cap = data.getCurrentMilestoneCap();

        guiGraphics.drawString(this.font, Component.literal("§7Hito Nv." + cap + " §8(" + curXp + "/" + reqXp + ")"), x, y, 0xFFCCCCCC, false);

        int barY = y + 8;
        int barHeight = 3;
        guiGraphics.fill(x, barY, x + width, barY + barHeight, 0xFF222530);
        int fillWidth = (int) (width * progress);
        if (fillWidth > 0) {
            guiGraphics.fillGradient(x, barY, x + fillWidth, barY + barHeight, 0xFF55FF55, 0xFF00AA00);
        }
        guiGraphics.renderOutline(x, barY, width, barHeight, 0xFF555555);
    }

    private List<Component> renderReputationBar(GuiGraphics guiGraphics, PlayerRpgData data, int x, int y, int width, int mouseX, int mouseY) {
        int score = (data != null) ? data.getReputationScore() : 0;
        com.pablo.rpglevels.data.ReputationTier tier = (data != null) ? data.getReputationTier() : com.pablo.rpglevels.data.ReputationTier.NEUTRAL;

        // Label: "Moralidad: 🌟 Héroe (+75)" or "Moralidad: ☠️ Forajido (-60)"
        String scoreStr = (score > 0 ? "+" : "") + score;
        Component repLabel = Component.translatable("rpglevels.gui.reputation_label")
                .append(Component.literal(": " + tier.getIcon() + " "))
                .append(Component.translatable(tier.getTranslationKey()).withStyle(tier.getColor()))
                .append(Component.literal(" (" + scoreStr + ")").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        guiGraphics.drawString(this.font, repLabel, x, y, 0xFFD4D4D4, false);

        int barY = y + 9;
        int barHeight = 4;
        int midX = x + (width / 2);

        // Dark background with center tick
        guiGraphics.fill(x, barY, x + width, barY + barHeight, 0xFF181A22);
        guiGraphics.renderOutline(x, barY, width, barHeight, 0xFF4A4E62);
        guiGraphics.vLine(midX, barY - 1, barY + barHeight, 0xFFFFAA00); // Yellow 0-line tick

        // If score is positive: fill from center to right with green/gold
        // If score is negative: fill from center to left with red
        float normalized = Math.min(1.0f, Math.abs(score) / 100.0f);
        int halfWidth = width / 2;
        int fillLength = (int) (halfWidth * normalized);

        if (score > 0 && fillLength > 0) {
            int rightEdge = Math.min(x + width - 1, midX + fillLength);
            guiGraphics.fillGradient(midX, barY + 1, rightEdge, barY + barHeight - 1, 0xFF55FF55, 0xFF00AA00);
        } else if (score < 0 && fillLength > 0) {
            int leftEdge = Math.max(x + 1, midX - fillLength);
            guiGraphics.fillGradient(leftEdge, barY + 1, midX, barY + barHeight - 1, 0xFFFF3333, 0xFFAA0000);
        }

        // Hover tooltip over reputation bar
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= barY + barHeight + 2) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("rpglevels.gui.reputation_tooltip_title").withStyle(net.minecraft.ChatFormatting.GOLD, net.minecraft.ChatFormatting.BOLD));
            tooltip.add(Component.translatable("rpglevels.gui.reputation_tooltip_score", scoreStr).withStyle(tier.getColor()));
            tooltip.add(Component.translatable("rpglevels.gui.reputation_tooltip_desc." + tier.getId()).withStyle(net.minecraft.ChatFormatting.GRAY));
            if (tier.getPriceModifier() < 0) {
                int discount = Math.round(Math.abs(tier.getPriceModifier()) * 100);
                tooltip.add(Component.literal("§a• Descuento comercial en aldeas: -" + discount + "%"));
            } else if (tier.getPriceModifier() > 0) {
                int penalty = Math.round(tier.getPriceModifier() * 100);
                tooltip.add(Component.literal("§c• Inflación y sobrecostos por infamia: +" + penalty + "%"));
            }
            return tooltip;
        }
        return null;
    }

    private List<Component> renderStatsBreakdown(GuiGraphics guiGraphics, Player player, PlayerRpgData data, int currentLevel, int startX, int startY, int mouseX, int mouseY) {
        int pendingPoints = (data != null) ? data.getPendingPointsCount(currentLevel) : 0;
        
        // 0. Pending Points Banner if any
        int lineY = startY;
        if (pendingPoints > 0) {
            guiGraphics.drawString(this.font, Component.translatable("rpglevels.gui.unassigned_points", pendingPoints), startX, lineY, 0xFFFFFF55, true);
        } else {
            guiGraphics.drawString(this.font, Component.literal("§7Atributos Principales §8(Pasa el cursor para ver detalles)"), startX, lineY, 0xFFAAAAAA, false);
        }

        // Points
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

        // Live Minecraft Attributes
        AttributeInstance hpAttr = player.getAttribute(Attributes.MAX_HEALTH);
        double liveHealth = (hpAttr != null) ? hpAttr.getValue() : 20.0;
        int totalHearts = (int) (liveHealth / 2.0);

        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        double liveArmor = (armorAttr != null) ? armorAttr.getValue() : 0.0;

        double deflectChance = Math.min(70.0, fortTotal * 0.7);

        float maxMana = (data != null) ? data.getMaxMana(player) : 100.0f;
        float curMana = (data != null) ? data.getCurrentMana(player) : maxMana;

        int cardW = (leftPos + GUI_WIDTH - (pendingPoints > 0 ? 30 : 14)) - startX;
        int cardH = 24;
        List<Component> hoveredTooltip = null;

        double relDmgBonus = (data != null) ? data.getPhysicalRelationDamageBonus(player) : 0.0;
        double spiritSwordBonus = (data != null) ? data.getSpiritualSwordSpiritBonus(player) : 0.0;
        double hpDelta = (fortTotal >= 5) ? ((fortTotal - 5) * 2.0) : ((fortTotal - 5) * 1.5);

        // 1. FORTITUDE CARD
        int fortY = startY + 11;
        boolean fortHover = mouseX >= startX && mouseX <= startX + cardW && mouseY >= fortY && mouseY <= fortY + cardH;
        guiGraphics.fill(startX, fortY, startX + cardW, fortY + cardH, fortHover ? 0xDD1B2236 : 0x88101322);
        guiGraphics.renderOutline(startX, fortY, cardW, cardH, fortHover ? 0xFFE5A93C : 0xFF2D3248);

        String fortName = Component.translatable("rpglevels.stat.fortitude.name").getString();
        guiGraphics.drawString(this.font, Component.literal("🛡️ §c§l" + fortName + ": §f" + fortTotal + " pts"), startX + 5, fortY + 3, 0xFFFFFFFF, true);
        String fortBadge = "§8★" + fortPerm + " +Nv" + fortActive + (fortCurios > 0 ? " +" + fortCurios : "");
        guiGraphics.drawString(this.font, Component.literal(fortBadge), startX + cardW - 5 - this.font.width(fortBadge), fortY + 3, 0xFF888888, false);
        String fortSub = "§7Vida: §c" + (int)liveHealth + " HP §8| §7Armad: §b+" + String.format("%.1f", Math.max(0.0, (fortTotal - 5) * 0.35)) + " §8| §7Desvío: §e" + String.format("%.1f", deflectChance) + "%";
        guiGraphics.drawString(this.font, Component.literal(fortSub), startX + 5, fortY + 13, 0xFFAAAAAA, false);

        if (fortHover) {
            hoveredTooltip = new ArrayList<>();
            hoveredTooltip.add(Component.literal("🛡️ §c§lFortaleza §7(" + fortTotal + " pts | Estándar = 5)").withStyle(net.minecraft.ChatFormatting.BOLD));
            hoveredTooltip.add(Component.literal("§8Puntos: ★" + fortPerm + " nacimiento | +Nv" + fortActive + " por nivel" + (fortCurios > 0 ? " | +" + fortCurios + " accesorios" : "")));
            hoveredTooltip.add(Component.literal("§8──────────────────────────────────"));
            hoveredTooltip.add(Component.literal("§f• Vida Máxima: §c" + (hpDelta >= 0 ? "+" : "") + String.format("%.1f", hpDelta) + " HP §8(§c" + (int)liveHealth + " HP §7/ §e" + totalHearts + "♥§8)"));
            hoveredTooltip.add(Component.literal("§f• Armadura Base: §b+" + String.format("%.1f", Math.max(0.0, (fortTotal - 5) * 0.35)) + " §8(Total: §b" + String.format("%.1f", liveArmor) + "§8)"));
            hoveredTooltip.add(Component.literal("§f• Dureza de Armadura: §9+" + String.format("%.1f", Math.max(0.0, (fortTotal - 5) * 0.15))));
            hoveredTooltip.add(Component.literal("§f• Daño Físico (Relación Fort+Des): §c" + (relDmgBonus >= 0 ? "+" : "") + String.format("%.2f", relDmgBonus) + " §8(Base: " + String.format("%.2f", 1.0 + relDmgBonus) + ")"));
            hoveredTooltip.add(Component.literal("§f• Probabilidad de Desvío: §e" + String.format("%.1f", deflectChance) + "% §7(Anula proyectiles)"));
            hoveredTooltip.add(Component.literal("§f• Reducción de Daño Plano: §a" + String.format("%.1f", Math.min(35.0, fortTotal * 0.45)) + "% §7(Mitigación constante)"));
        }

        // 2. DEXTERITY CARD
        int dexY = startY + 39;
        boolean dexHover = mouseX >= startX && mouseX <= startX + cardW && mouseY >= dexY && mouseY <= dexY + cardH;
        guiGraphics.fill(startX, dexY, startX + cardW, dexY + cardH, dexHover ? 0xDD1B2236 : 0x88101322);
        guiGraphics.renderOutline(startX, dexY, cardW, cardH, dexHover ? 0xFFE5A93C : 0xFF2D3248);

        String dexName = Component.translatable("rpglevels.stat.dexterity.name").getString();
        guiGraphics.drawString(this.font, Component.literal("🎯 §b§l" + dexName + ": §f" + dexTotal + " pts"), startX + 5, dexY + 3, 0xFFFFFFFF, true);
        String dexBadge = "§8★" + dexPerm + " +Nv" + dexActive + (dexCurios > 0 ? " +" + dexCurios : "");
        guiGraphics.drawString(this.font, Component.literal(dexBadge), startX + cardW - 5 - this.font.width(dexBadge), dexY + 3, 0xFF888888, false);
        float dexCdrPct = Math.min(60.0f, dexTotal * 1.2f);
        double dexPctAtk = (dexTotal - 5) * 2.5;
        String dexSub = "§7Vel.Atq: §e" + (dexPctAtk >= 0 ? "+" : "") + String.format("%.1f", dexPctAtk) + "% §8| §7Daño(F+D): §c" + (relDmgBonus >= 0 ? "+" : "") + String.format("%.1f", relDmgBonus) + " §8| §7Parry: §e" + String.format("%.0f", Math.min(80.0, 15.0 + dexTotal * 1.2)) + "%";
        guiGraphics.drawString(this.font, Component.literal(dexSub), startX + 5, dexY + 13, 0xFFAAAAAA, false);

        if (dexHover && hoveredTooltip == null) {
            hoveredTooltip = new ArrayList<>();
            hoveredTooltip.add(Component.literal("🎯 §b§lDestreza §7(" + dexTotal + " pts | Estándar = 5)").withStyle(net.minecraft.ChatFormatting.BOLD));
            hoveredTooltip.add(Component.literal("§8Puntos: ★" + dexPerm + " nacimiento | +Nv" + dexActive + " por nivel" + (dexCurios > 0 ? " | +" + dexCurios + " accesorios" : "")));
            hoveredTooltip.add(Component.literal("§8──────────────────────────────────"));
            hoveredTooltip.add(Component.literal("§f• Sinergia de Daño (Fortitud + Destreza): §c" + (relDmgBonus >= 0 ? "+" : "") + String.format("%.2f", relDmgBonus)));
            hoveredTooltip.add(Component.literal("§f• Velocidad de Ataque: §e" + (dexPctAtk >= 0 ? "+" : "") + String.format("%.1f", dexPctAtk) + "%"));
            hoveredTooltip.add(Component.literal("§f• Velocidad de Movimiento: §a" + ((dexTotal - 5) >= 0 ? "+" : "") + String.format("%.1f", (dexTotal - 5) * 1.2) + "%"));
            hoveredTooltip.add(Component.literal("§f• Altura de Salto: §d" + ((dexTotal - 5) >= 0 ? "+" : "") + (dexTotal - 5) + "%"));
            hoveredTooltip.add(Component.literal("§f• Caída Segura: §b+" + String.format("%.1f", Math.max(0.0, (dexTotal - 5) * 0.4)) + " bloques"));
            hoveredTooltip.add(Component.literal("§f• Probabilidad de Parry (Escudo/Espada): §e" + String.format("%.1f", Math.min(80.0, 15.0 + dexTotal * 1.2)) + "%"));
            hoveredTooltip.add(Component.literal("§f• Reducción de Cooldowns (Técnicas): §b-" + String.format("%.1f", dexCdrPct) + "% §8(Máx 60%)"));
        }

        // 3. SPIRIT CARD
        int spiY = startY + 67;
        boolean spiHover = mouseX >= startX && mouseX <= startX + cardW && mouseY >= spiY && mouseY <= spiY + cardH;
        guiGraphics.fill(startX, spiY, startX + cardW, spiY + cardH, spiHover ? 0xDD1B2236 : 0x88101322);
        guiGraphics.renderOutline(startX, spiY, cardW, cardH, spiHover ? 0xFFE5A93C : 0xFF2D3248);

        String spiName = Component.translatable("rpglevels.stat.spirit.name").getString();
        guiGraphics.drawString(this.font, Component.literal("✨ §d§l" + spiName + ": §f" + spiTotal + " pts"), startX + 5, spiY + 3, 0xFFFFFFFF, true);
        String spiBadge = "§8★" + spiPerm + " +Nv" + spiActive + (spiCurios > 0 ? " +" + spiCurios : "");
        guiGraphics.drawString(this.font, Component.literal(spiBadge), startX + cardW - 5 - this.font.width(spiBadge), spiY + 3, 0xFF888888, false);
        String spiSub = "§7Maná: §b" + (int)curMana + "/" + (int)maxMana + " PM §8| §7Bono Espada Esp.: §d+" + String.format("%.2f", spiritSwordBonus);
        guiGraphics.drawString(this.font, Component.literal(spiSub), startX + 5, spiY + 13, 0xFFAAAAAA, false);

        if (spiHover && hoveredTooltip == null) {
            hoveredTooltip = new ArrayList<>();
            hoveredTooltip.add(Component.literal("✨ §d§lEspíritu §7(" + spiTotal + " pts | Estándar = 5 -> 100 PM)").withStyle(net.minecraft.ChatFormatting.BOLD));
            hoveredTooltip.add(Component.literal("§8Puntos: ★" + spiPerm + " nacimiento | +Nv" + spiActive + " por nivel" + (spiCurios > 0 ? " | +" + spiCurios + " accesorios" : "")));
            hoveredTooltip.add(Component.literal("§8──────────────────────────────────"));
            hoveredTooltip.add(Component.literal("§f• Reserva de Maná Máximo: §b" + (int)maxMana + " PM §8(Base 100 PM en Espíritu 5)"));
            hoveredTooltip.add(Component.literal("§f• Bono de Daño en Espadas Espirituales: §d+" + String.format("%.2f", spiritSwordBonus) + " daño extra"));
            hoveredTooltip.add(Component.literal("§f• Regeneración de Maná: §3+" + String.format("%.2f", Math.max(0.0f, (spiTotal - 5) * 0.05f)) + " PM/s §8(Base +1.0/s)"));
            hoveredTooltip.add(Component.literal("§f• Multiplicador de XP RPG: §a+" + String.format("%.0f", Math.max(0.0, (spiTotal - 5) * 2.0)) + "% extra"));
        }

        // 4. LUCK CARD
        int luckY = startY + 95;
        boolean luckHover = mouseX >= startX && mouseX <= startX + cardW && mouseY >= luckY && mouseY <= luckY + cardH;
        guiGraphics.fill(startX, luckY, startX + cardW, luckY + cardH, luckHover ? 0xDD1B2236 : 0x88101322);
        guiGraphics.renderOutline(startX, luckY, cardW, cardH, luckHover ? 0xFFE5A93C : 0xFF2D3248);

        String luckName = Component.translatable("rpglevels.stat.luck.name").getString();
        guiGraphics.drawString(this.font, Component.literal("🍀 §a§l" + luckName + ": §f" + luckTotal + " pts"), startX + 5, luckY + 3, 0xFFFFFFFF, true);
        String luckBadge = "§8★" + luckPerm + " +Nv" + luckActive + (luckCurios > 0 ? " +" + luckCurios : "");
        guiGraphics.drawString(this.font, Component.literal(luckBadge), startX + cardW - 5 - this.font.width(luckBadge), luckY + 3, 0xFF888888, false);
        String luckSub = "§7Crítico: §e" + String.format("%.1f", critChance) + "% §8(§c" + String.format("%.2f", critMult) + "x§8) §8| §7Drops: §a+" + (luckTotal * 3) + "%";
        guiGraphics.drawString(this.font, Component.literal(luckSub), startX + 5, luckY + 13, 0xFFAAAAAA, false);

        if (luckHover && hoveredTooltip == null) {
            hoveredTooltip = new ArrayList<>();
            hoveredTooltip.add(Component.literal("🍀 §a§lSuerte §7(" + luckTotal + " pts | Estándar = 5)").withStyle(net.minecraft.ChatFormatting.BOLD));
            hoveredTooltip.add(Component.literal("§8Puntos: ★" + luckPerm + " nacimiento | +Nv" + luckActive + " por nivel" + (luckCurios > 0 ? " | +" + luckCurios + " accesorios" : "")));
            hoveredTooltip.add(Component.literal("§8──────────────────────────────────"));
            hoveredTooltip.add(Component.literal("§f• Probabilidad de Crítico: §e" + String.format("%.1f", critChance) + "% " + (critChance > 100.0 ? "§6[SUPER-CRIT!]" : "")));
            hoveredTooltip.add(Component.literal("§f• Multiplicador de Crítico: §c" + String.format("%.2f", critMult) + "x daño"));
            hoveredTooltip.add(Component.literal("§f• Suerte Nativa (Vanilla Luck): §b" + ((luckTotal - 5) >= 0 ? "+" : "") + String.format("%.1f", (luckTotal - 5) * 0.2) + " §7(Mejor botín y pesca)"));
            hoveredTooltip.add(Component.literal("§f• Bonus de Drops y Recompensas: §a+" + (luckTotal * 3) + "% más probabilidad de botín raro"));
        }

        return hoveredTooltip;
    }

    private void renderCuriosSummary(GuiGraphics guiGraphics, Player player, int startX, int startY) {
        guiGraphics.drawString(this.font, Component.translatable("rpglevels.gui.accessories_title"), startX, startY, 0xFFFFAA00, false);

        List<Component> itemsRender = new ArrayList<>();
        var curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isPresent()) {
            curiosOpt.get().findCurios(s -> !s.isEmpty()).forEach(res -> {
                ItemStack stack = res.stack();
                itemsRender.add(stack.getHoverName());
            });
        }

        // Add Skills badges
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            if (data.hasSkill("agility_double_jump")) {
                itemsRender.add(Component.literal("§a⚡ Doble Salto"));
            } else {
                itemsRender.add(Component.literal("§7⚡ Doble Salto (" + data.getJumpCount() + "/2000)"));
            }

            if (data.hasSkill("agility_dash")) {
                int cdSec = data.getDashCooldownTicks(player) / 20;
                itemsRender.add(Component.literal("§b💨 Dash Nivel 1 (" + cdSec + "s)"));
            } else {
                itemsRender.add(Component.literal("§7💨 Dash (Kaelen)"));
            }

            if (data.hasSkill("magic_affinity")) {
                itemsRender.add(Component.literal("§d✦ Afinidad Espiritual"));
            }
            if (data.hasSkill("physical_boost")) {
                itemsRender.add(Component.literal("§c⚔ Refuerzo"));
            }
            if (data.hasSkill("agility_boost")) {
                itemsRender.add(Component.literal("§b💨 Celeridad"));
            }
            if (data.hasSkill("fireball")) {
                itemsRender.add(Component.literal("§6🔥 Fuego"));
            }
            if (data.hasSkill("heal_wounds")) {
                itemsRender.add(Component.literal("§a💚 Curación"));
            }
            if (data.hasReceivedSpiritualSword()) {
                itemsRender.add(Component.literal("§5🗡 Espada Espiritual"));
            }
        }

        if (itemsRender.isEmpty()) {
            guiGraphics.drawString(this.font, Component.translatable("rpglevels.gui.no_accessories"), startX, startY + 10, 0xFF888888, false);
        } else {
            int curX = startX;
            int curY = startY + 10;
            int maxX = leftPos + GUI_WIDTH - 14;

            for (Component item : itemsRender) {
                Component renderedItem = Component.literal("• ").append(item);
                int itemW = this.font.width(renderedItem);
                if (curX + itemW > maxX && curX > startX) {
                    curX = startX;
                    curY += 10;
                    if (curY > topPos + 204) break;
                }
                guiGraphics.drawString(this.font, renderedItem, curX, curY, 0xFFFFFFFF, false);
                curX += itemW + 8;
            }
        }
    }
}
