package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.QuestActionPayload;
import com.pablo.rpglevels.quests.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RpgQuestsScreen extends Screen {

    private final Screen parentScreen;
    private final int GUI_WIDTH = 300;
    private final int GUI_HEIGHT = 215;
    private int leftPos;
    private int topPos;
    private int selectedTab = 0; // 0 = Contratos Diarios, 1 = Misiones de Rango e Historia

    public RpgQuestsScreen(Screen parentScreen) {
        super(Component.translatable("rpglevels.gui.quests_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        boolean isRegistered = rpgData != null && rpgData.isGuildRegistered();
        AdventurerRank rank = isRegistered ? rpgData.getRank() : AdventurerRank.F;
        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);

        // 1. Tab buttons
        Component tab0Text = Component.literal(selectedTab == 0 ? "§6§l📜 Contratos Diarios" : "§7📜 Contratos Diarios");
        this.addRenderableWidget(Button.builder(tab0Text, b -> {
            selectedTab = 0;
            this.rebuildWidgets();
        }).bounds(leftPos + 10, topPos + 22, 136, 16)
        .tooltip(Tooltip.create(Component.literal("§6[Tablón Diario del Gremio]\n§eEncargos diarios autogenerados de cacería, recolección de cristales de alma y suministros que se renuevan cada día.")))
        .build());

        Component tab1Text = Component.literal(selectedTab == 1 ? "§e§l⭐ Rango e Historia" : "§7⭐ Rango e Historia");
        this.addRenderableWidget(Button.builder(tab1Text, b -> {
            selectedTab = 1;
            this.rebuildWidgets();
        }).bounds(leftPos + 150, topPos + 22, 140, 16)
        .tooltip(Tooltip.create(Component.literal("§e[Misiones de Rango e Historia]\n§7Misiones permanentes de ascensión, pruebas de Alice y encargos especiales de jefes.")))
        .build());

        // 2. Back button
        this.addRenderableWidget(Button.builder(Component.literal("§cVolver"), button -> {
            if (this.minecraft != null) {
                if (this.parentScreen != null) {
                    this.minecraft.setScreen(this.parentScreen);
                } else {
                    this.minecraft.setScreen(new RpgStatsScreen(new InventoryScreen(player)));
                }
            }
        }).bounds(leftPos + GUI_WIDTH - 85, topPos + GUI_HEIGHT - 20, 75, 16).build());

        // 3. Resolve quests for current tab
        List<Quest> displayQuests = new ArrayList<>();
        if (selectedTab == 0) {
            // Contratos Diarios
            displayQuests.addAll(DailyQuestManager.getDailyQuests(player.level(), rank));
        } else {
            // Misiones de Rango e Historia
            if (isRegistered) {
                for (Quest q : QuestRegistry.getQuestsForRank(rank)) {
                    if (!q.isDaily()) displayQuests.add(q);
                }
            }
            if (rpgData != null && rpgData.isAliceQuestActive()) {
                Quest q = QuestRegistry.getQuest("alice_spiritual_trial");
                if (q != null && !displayQuests.contains(q)) {
                    displayQuests.add(q);
                }
            }
            if (questData != null) {
                for (String activeId : questData.getActiveQuests().keySet()) {
                    Quest q = QuestRegistry.getQuest(activeId);
                    if (q != null && !q.isDaily() && !displayQuests.contains(q)) {
                        displayQuests.add(q);
                    }
                }
            }
        }

        int itemY = topPos + 44;
        boolean isAtGuildMaster = (this.parentScreen instanceof NpcDialogueScreen ds && "guild_master".equals(ds.getNpcType()));

        for (Quest quest : displayQuests) {
            String qId = quest.id();
            boolean isAliceQuest = "alice_spiritual_trial".equals(qId) || "alice_cultist_hunt".equals(qId);
            boolean isActive = (questData != null && questData.isQuestActive(qId)) || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.isAliceQuestActive());
            boolean isClaimed = (questData != null && questData.isQuestClaimed(qId)) || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.hasReceivedSpiritualSword());

            int currentProgress;
            if (quest.type() == QuestType.COLLECT_ITEMS && quest.requiredItem() != null) {
                currentProgress = player.getInventory().countItem(quest.requiredItem());
            } else if ("alice_spiritual_trial".equals(qId) && rpgData != null) {
                currentProgress = rpgData.getAliceDMonstersKilled();
            } else {
                currentProgress = (questData != null) ? questData.getProgress(qId) : 0;
            }
            boolean isDone = currentProgress >= quest.targetCount();

            int btnX = leftPos + GUI_WIDTH - 76;
            int btnY = itemY + 3;

            if (isClaimed) {
                // Ya completada y entregada
            } else if (isActive) {
                if (isDone) {
                    if (isAliceQuest) {
                        Button aliceBtn = Button.builder(Component.literal("§d✦ Hablar"), b -> {
                            if (this.minecraft != null && this.minecraft.player != null) {
                                this.minecraft.player.displayClientMessage(
                                        Component.literal("§d[Segadores Espirituales] ¡Habla con Alice para entregar la misión y recibir tus recompensas!"),
                                        false
                                );
                            }
                        }).bounds(btnX, btnY, 66, 16)
                        .tooltip(Tooltip.create(Component.literal("§dHabla con Alice en cualquier aldea para entregar esta misión.")))
                        .build();
                        this.addRenderableWidget(aliceBtn);
                    } else {
                        if (isAtGuildMaster) {
                            Button claimBtn = Button.builder(Component.literal("§a🎁 Entregar"), b -> {
                                PacketDistributor.sendToServer(new QuestActionPayload(qId, QuestActionPayload.ACTION_CLAIM));
                                b.active = false;
                            }).bounds(btnX, btnY, 66, 16)
                            .tooltip(Tooltip.create(Component.literal("§a[Entregar Encargo]\n§eEntrega los informes u objetos al Maestro del Gremio para recibir tu recompensa y reputación.")))
                            .build();
                            this.addRenderableWidget(claimBtn);
                        } else {
                            Button disabledBtn = Button.builder(Component.literal("§7Entregar"), b -> {})
                                    .bounds(btnX, btnY, 66, 16)
                                    .tooltip(Tooltip.create(Component.literal("§6[Entrega en el Gremio]\n§7Debes presentarte en persona ante un §eMaestro del Gremio §7en cualquier aldea para entregar tu informe y reclamar tus recompensas.")))
                                    .build();
                            disabledBtn.active = false;
                            this.addRenderableWidget(disabledBtn);
                        }
                    }
                }
            } else {
                if (isAliceQuest) {
                    Button aliceBtn = Button.builder(Component.literal("§d✦ Alice"), b -> {
                        if (this.minecraft != null && this.minecraft.player != null) {
                            this.minecraft.player.displayClientMessage(
                                    Component.literal("§d[Segadores Espirituales] Habla directamente con Alice en cualquier aldea para aceptar esta misión especial."),
                                    false
                                );
                        }
                    }).bounds(btnX, btnY, 66, 16)
                    .tooltip(Tooltip.create(Component.literal("§dHabla con Alice para aceptar esta misión especial.")))
                    .build();
                    this.addRenderableWidget(aliceBtn);
                } else {
                    Button acceptBtn = Button.builder(Component.literal("§e").append(Component.translatable("rpglevels.gui.quests_start")), b -> {
                        PacketDistributor.sendToServer(new QuestActionPayload(qId, QuestActionPayload.ACTION_ACCEPT));
                        b.active = false;
                    }).bounds(btnX, btnY, 66, 16).build();
                    this.addRenderableWidget(acceptBtn);
                }
            }

            itemY += 31;
            if (itemY > topPos + GUI_HEIGHT - 30) break;
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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        boolean isRegistered = rpgData != null && rpgData.isGuildRegistered();
        AdventurerRank rank = (rpgData != null) ? rpgData.getRank() : AdventurerRank.F;
        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);

        // Frame
        guiGraphics.fillGradient(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF012141F, 0xF01C2030);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFD4AF37);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0xFF35394A);

        // Header
        guiGraphics.drawString(this.font, Component.literal("📜 §6§lGremio de Aventureros"), leftPos + 12, topPos + 8, 0xFFFFAA00, true);
        Component rankBadge = isRegistered ? rank.getBadge() : Component.literal("[Civil No Registrado]").withStyle(ChatFormatting.DARK_GRAY);
        guiGraphics.drawString(this.font, rankBadge, leftPos + GUI_WIDTH - 12 - this.font.width(rankBadge), topPos + 8, 0xFFFFFFFF, false);

        // Quests List
        List<Quest> displayQuests = new ArrayList<>();
        if (selectedTab == 0) {
            displayQuests.addAll(DailyQuestManager.getDailyQuests(player.level(), rank));
        } else {
            if (isRegistered) {
                for (Quest q : QuestRegistry.getQuestsForRank(rank)) {
                    if (!q.isDaily()) displayQuests.add(q);
                }
            }
            if (rpgData != null && rpgData.isAliceQuestActive()) {
                Quest q = QuestRegistry.getQuest("alice_spiritual_trial");
                if (q != null && !displayQuests.contains(q)) {
                    displayQuests.add(q);
                }
            }
            if (questData != null) {
                for (String activeId : questData.getActiveQuests().keySet()) {
                    Quest q = QuestRegistry.getQuest(activeId);
                    if (q != null && !q.isDaily() && !displayQuests.contains(q)) {
                        displayQuests.add(q);
                    }
                }
            }
        }

        int itemY = topPos + 44;

        for (Quest quest : displayQuests) {
            String qId = quest.id();
            boolean isAliceQuest = "alice_spiritual_trial".equals(qId) || "alice_cultist_hunt".equals(qId);
            boolean isActive = (questData != null && questData.isQuestActive(qId)) || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.isAliceQuestActive());
            boolean isClaimed = (questData != null && questData.isQuestClaimed(qId)) || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.hasReceivedSpiritualSword());

            int currentProgress;
            if (quest.type() == QuestType.COLLECT_ITEMS && quest.requiredItem() != null) {
                currentProgress = player.getInventory().countItem(quest.requiredItem());
            } else if ("alice_spiritual_trial".equals(qId) && rpgData != null) {
                currentProgress = rpgData.getAliceDMonstersKilled();
            } else {
                currentProgress = (questData != null) ? questData.getProgress(qId) : 0;
            }
            boolean isDone = currentProgress >= quest.targetCount();

            // Card background
            int cardColor = isClaimed ? 0x22FFFFFF : (isDone ? 0x3300FF00 : 0x22112233);
            guiGraphics.fill(leftPos + 8, itemY, leftPos + GUI_WIDTH - 8, itemY + 28, cardColor);
            guiGraphics.renderOutline(leftPos + 8, itemY, GUI_WIDTH - 16, 28, isDone ? 0xFF55FF55 : 0xFF4A4D60);

            // Title & Reward info
            Component titleComp = quest.getTitle();
            String repStr = quest.rewardReputation() > 0 ? " §6+" + quest.rewardReputation() + " Rep" : "";
            String rewardStr = " §e+" + quest.rewardXp() + " XP" + repStr;
            guiGraphics.drawString(this.font, Component.literal("§f").append(titleComp).append(Component.literal("§7(" + rewardStr + "§7)")), leftPos + 12, itemY + 4, 0xFFFFFFFF, false);

            // Description / Progress
            if (isClaimed) {
                guiGraphics.drawString(this.font, Component.literal("§a✔ Entregada y Reclamada"), leftPos + 12, itemY + 15, 0xFF88FF88, false);
            } else if (isActive) {
                float pct = Math.min(1.0f, (float) currentProgress / (float) quest.targetCount());
                int barWidth = 110;
                int barX = leftPos + 12;
                int barY = itemY + 16;
                guiGraphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF222530);
                guiGraphics.fill(barX, barY, barX + (int)(barWidth * pct), barY + 6, isDone ? 0xFF55FF55 : 0xFF33AAFF);
                guiGraphics.renderOutline(barX, barY, barWidth, 6, 0xFF555555);

                String progressLabel = (quest.type() == QuestType.COLLECT_ITEMS)
                        ? "§eEn inventario: " + currentProgress + "/" + quest.targetCount()
                        : "§e" + currentProgress + "/" + quest.targetCount();
                guiGraphics.drawString(this.font, Component.literal(progressLabel), barX + barWidth + 6, barY - 1, 0xFFFFFFFF, false);
            } else {
                guiGraphics.drawString(this.font, quest.getDescription(), leftPos + 12, itemY + 15, 0xFFAAAAAA, false);
            }

            itemY += 31;
            if (itemY > topPos + GUI_HEIGHT - 30) break;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
