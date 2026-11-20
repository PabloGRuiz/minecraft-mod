package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.QuestActionPayload;
import com.pablo.rpglevels.quests.DailyQuestManager;
import com.pablo.rpglevels.quests.PlayerQuestData;
import com.pablo.rpglevels.quests.Quest;
import com.pablo.rpglevels.quests.QuestRegistry;
import com.pablo.rpglevels.quests.QuestType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RpgQuestsScreen extends Screen {

    private final Screen parentScreen;
    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 238;
    private static final int PAGE_SIZE = 4;

    private int leftPos;
    private int topPos;
    private int selectedTab = 0; // 0 = Contratos Diarios, 1 = Rango e Historia
    private int currentPage = 0;

    public RpgQuestsScreen(Screen parentScreen) {
        super(Component.translatable("rpglevels.gui.quests_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // 1. Barra superior unificada
        RpgUiHelper.addNavigationBar(this, this::addRenderableWidget, leftPos, topPos, GUI_WIDTH, RpgUiHelper.NavTab.QUESTS);

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        boolean isRegistered = rpgData != null && rpgData.isGuildRegistered();
        AdventurerRank rank = isRegistered ? rpgData.getRank() : AdventurerRank.F;
        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);

        // 2. Sub-pestañas del tablón
        Component tab0Text = Component.literal(selectedTab == 0 ? "§6§l📜 Contratos Diarios" : "§7📜 Contratos Diarios");
        this.addRenderableWidget(Button.builder(tab0Text, b -> {
            selectedTab = 0;
            currentPage = 0;
            this.rebuildWidgets();
        }).bounds(leftPos + 10, topPos + 42, 166, 17)
        .tooltip(Tooltip.create(Component.literal("§6[Tablón Diario del Gremio]\n§eEncargos diarios renovables de cacería y suministros.")))
        .build());

        Component tab1Text = Component.literal(selectedTab == 1 ? "§e§l⭐ Rango e Historia" : "§7⭐ Rango e Historia");
        this.addRenderableWidget(Button.builder(tab1Text, b -> {
            selectedTab = 1;
            currentPage = 0;
            this.rebuildWidgets();
        }).bounds(leftPos + 184, topPos + 42, 166, 17)
        .tooltip(Tooltip.create(Component.literal("§e[Misiones de Rango e Historia]\n§7Pruebas de ascensión, misiones de Alice y jefes.")))
        .build());

        // 3. Misiones filtradas y paginadas
        List<Quest> displayQuests = collectDisplayQuests(player, rpgData, questData, isRegistered, rank);
        int totalPages = Math.max(1, (int) Math.ceil(displayQuests.size() / (double) PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        int startIdx = currentPage * PAGE_SIZE;
        int endIdx = Math.min(displayQuests.size(), startIdx + PAGE_SIZE);
        boolean isAtGuildMaster = (this.parentScreen instanceof NpcDialogueScreen ds && "guild_master".equals(ds.getNpcType()));

        int itemY = topPos + 64;
        for (int i = startIdx; i < endIdx; i++) {
            Quest quest = displayQuests.get(i);
            String qId = quest.id();
            boolean isAliceQuest = "alice_spiritual_trial".equals(qId) || "alice_cultist_hunt".equals(qId);
            boolean isActive = (questData != null && questData.isQuestActive(qId))
                    || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.isAliceQuestActive());
            boolean isClaimed = (questData != null && questData.isQuestClaimed(qId))
                    || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.hasReceivedSpiritualSword());

            int currentProgress = resolveProgress(player, quest, rpgData, questData);
            boolean isDone = currentProgress >= quest.targetCount();

            int btnX = leftPos + GUI_WIDTH - 80;
            int btnY = itemY + 8;

            if (!isClaimed) {
                if (isActive) {
                    if (isDone) {
                        if (isAliceQuest) {
                            this.addRenderableWidget(Button.builder(Component.literal("§d✦ Hablar"), b -> {
                                if (this.minecraft != null && this.minecraft.player != null) {
                                    this.minecraft.player.displayClientMessage(
                                            Component.literal("§d[Segadores Espirituales] ¡Habla con Alice para entregar la misión y recibir tus recompensas!"),
                                            false);
                                }
                            }).bounds(btnX, btnY, 68, 17)
                            .tooltip(Tooltip.create(Component.literal("§dHabla con Alice en cualquier aldea.")))
                            .build());
                        } else if (isAtGuildMaster) {
                            this.addRenderableWidget(Button.builder(Component.literal("§a🎁 Entregar"), b -> {
                                PacketDistributor.sendToServer(new QuestActionPayload(qId, QuestActionPayload.ACTION_CLAIM));
                                b.active = false;
                            }).bounds(btnX, btnY, 68, 17)
                            .tooltip(Tooltip.create(Component.literal("§aReclamar recompensa ante el Maestro del Gremio.")))
                            .build());
                        } else {
                            Button disabledBtn = Button.builder(Component.literal("§7Entregar"), b -> {})
                                    .bounds(btnX, btnY, 68, 17)
                                    .tooltip(Tooltip.create(Component.literal("§6[Entrega en el Gremio]\n§7Acude al Maestro del Gremio en una aldea para reclamar.")))
                                    .build();
                            disabledBtn.active = false;
                            this.addRenderableWidget(disabledBtn);
                        }
                    }
                } else {
                    if (isAliceQuest) {
                        this.addRenderableWidget(Button.builder(Component.literal("§d✦ Alice"), b -> {
                            if (this.minecraft != null && this.minecraft.player != null) {
                                this.minecraft.player.displayClientMessage(
                                        Component.literal("§d[Segadores Espirituales] Habla con Alice en cualquier aldea para aceptar esta misión."),
                                        false);
                            }
                        }).bounds(btnX, btnY, 68, 17)
                        .tooltip(Tooltip.create(Component.literal("§dHabla con Alice para comenzar esta prueba.")))
                        .build());
                    } else {
                        this.addRenderableWidget(Button.builder(Component.literal("§e").append(Component.translatable("rpglevels.gui.quests_start")), b -> {
                            PacketDistributor.sendToServer(new QuestActionPayload(qId, QuestActionPayload.ACTION_ACCEPT));
                            b.active = false;
                        }).bounds(btnX, btnY, 68, 17).build());
                    }
                }
            }

            itemY += 37;
        }

        // 4. Controles de paginación inferiores
        int bottomY = topPos + GUI_HEIGHT - 24;
        Button prevBtn = Button.builder(Component.literal("§e◀"), b -> {
            if (currentPage > 0) {
                currentPage--;
                this.rebuildWidgets();
            }
        }).bounds(leftPos + 10, bottomY, 24, 17).build();
        prevBtn.active = currentPage > 0;
        this.addRenderableWidget(prevBtn);

        Button nextBtn = Button.builder(Component.literal("§e▶"), b -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                this.rebuildWidgets();
            }
        }).bounds(leftPos + 92, bottomY, 24, 17).build();
        nextBtn.active = currentPage < totalPages - 1;
        this.addRenderableWidget(nextBtn);

        if (this.parentScreen != null) {
            this.addRenderableWidget(Button.builder(Component.literal("§cVolver"), b -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }).bounds(leftPos + GUI_WIDTH - 80, bottomY, 70, 17).build());
        }
    }

    private List<Quest> collectDisplayQuests(Player player, PlayerRpgData rpgData, PlayerQuestData questData,
                                             boolean isRegistered, AdventurerRank rank) {
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
        return displayQuests;
    }

    private int resolveProgress(Player player, Quest quest, PlayerRpgData rpgData, PlayerQuestData questData) {
        if (quest.type() == QuestType.COLLECT_ITEMS && quest.requiredItem() != null) {
            return player.getInventory().countItem(quest.requiredItem());
        } else if ("alice_spiritual_trial".equals(quest.id()) && rpgData != null) {
            return rpgData.getAliceDMonstersKilled();
        }
        return (questData != null) ? questData.getProgress(quest.id()) : 0;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.OPEN_QUESTS_KEY.get().matches(keyCode, scanCode)
                || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
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

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        boolean isRegistered = rpgData != null && rpgData.isGuildRegistered();
        AdventurerRank rank = (rpgData != null) ? rpgData.getRank() : AdventurerRank.F;
        PlayerQuestData questData = player.getData(ModAttachments.PLAYER_QUEST_DATA);

        // Sub-cabecera Gremio + Rango
        guiGraphics.drawString(this.font, "📜 §6§lGremio de Aventureros", leftPos + 10, topPos + 30, 0xFFFFAA00, true);
        Component rankBadge = isRegistered ? rank.getBadge() : Component.literal("[Civil No Registrado]").withStyle(ChatFormatting.DARK_GRAY);
        guiGraphics.drawString(this.font, rankBadge, leftPos + GUI_WIDTH - 10 - this.font.width(rankBadge), topPos + 30, 0xFFFFFFFF, false);

        List<Quest> displayQuests = collectDisplayQuests(player, rpgData, questData, isRegistered, rank);
        int totalPages = Math.max(1, (int) Math.ceil(displayQuests.size() / (double) PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        List<Component> hoveredCardTooltip = null;

        if (displayQuests.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "§7No hay encargos disponibles en esta categoría.", leftPos + GUI_WIDTH / 2, topPos + 115, 0xFFAAAAAA);
        } else {
            int startIdx = currentPage * PAGE_SIZE;
            int endIdx = Math.min(displayQuests.size(), startIdx + PAGE_SIZE);
            int itemY = topPos + 64;
            int cardW = GUI_WIDTH - 20;
            int cardH = 34;

            for (int i = startIdx; i < endIdx; i++) {
                Quest quest = displayQuests.get(i);
                String qId = quest.id();
                boolean isActive = (questData != null && questData.isQuestActive(qId))
                        || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.isAliceQuestActive());
                boolean isClaimed = (questData != null && questData.isQuestClaimed(qId))
                        || ("alice_spiritual_trial".equals(qId) && rpgData != null && rpgData.hasReceivedSpiritualSword());

                int currentProgress = resolveProgress(player, quest, rpgData, questData);
                boolean isDone = currentProgress >= quest.targetCount();

                int cardX = leftPos + 10;
                boolean hover = mouseX >= cardX && mouseX <= cardX + cardW - 74 && mouseY >= itemY && mouseY <= itemY + cardH;

                int cardBg = isClaimed ? 0x26FFFFFF : (isDone ? 0x33165026 : (hover ? 0xAA172034 : 0x66101524));
                int outline = isDone ? 0xFF55FF55 : (hover ? 0xFFE5A93C : 0xFF3E445C);
                guiGraphics.fill(cardX, itemY, cardX + cardW, itemY + cardH, cardBg);
                guiGraphics.renderOutline(cardX, itemY, cardW, cardH, outline);

                String repStr = quest.rewardReputation() > 0 ? " §6+" + quest.rewardReputation() + " Rep" : "";
                String rewardStr = "§e+" + quest.rewardXp() + " XP" + repStr;
                String titleLine = "§f" + quest.getTitle().getString() + " §7(" + rewardStr + "§7)";
                guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, titleLine, cardW - 82), cardX + 6, itemY + 5, 0xFFFFFFFF, false);

                if (isClaimed) {
                    guiGraphics.drawString(this.font, "§a✔ Entregada y Reclamada", cardX + 6, itemY + 19, 0xFF88FF88, false);
                } else if (isActive) {
                    float pct = Math.min(1.0f, (float) currentProgress / Math.max(1, quest.targetCount()));
                    int barWidth = 140;
                    int barX = cardX + 6;
                    int barY = itemY + 20;
                    guiGraphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF222530);
                    int fill = (int) (barWidth * pct);
                    if (fill > 0) {
                        guiGraphics.fill(barX, barY, barX + fill, barY + 6, isDone ? 0xFF55FF55 : 0xFF33AAFF);
                    }
                    guiGraphics.renderOutline(barX, barY, barWidth, 6, 0xFF555555);

                    String progressLabel = (quest.type() == QuestType.COLLECT_ITEMS)
                            ? "§eInv: " + currentProgress + "/" + quest.targetCount()
                            : "§e" + currentProgress + "/" + quest.targetCount();
                    guiGraphics.drawString(this.font, progressLabel, barX + barWidth + 8, barY - 1, 0xFFFFFFFF, false);
                } else {
                    guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, quest.getDescription().getString(), cardW - 82),
                            cardX + 6, itemY + 19, 0xFFAAAAAA, false);
                }

                if (hover) {
                    hoveredCardTooltip = List.of(
                            quest.getTitle().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                            quest.getDescription().copy().withStyle(ChatFormatting.GRAY),
                            Component.literal("§eRecompensa: §a+" + quest.rewardXp() + " XP"
                                    + (quest.rewardReputation() > 0 ? " §8| §6+" + quest.rewardReputation() + " Reputación" : ""))
                    );
                }

                itemY += 37;
            }
        }

        String pageLabel = "§7Pág. §f" + (currentPage + 1) + "§7/" + totalPages;
        guiGraphics.drawCenteredString(this.font, pageLabel, leftPos + 63, topPos + GUI_HEIGHT - 19, 0xFFFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (hoveredCardTooltip != null) {
            guiGraphics.renderComponentTooltip(this.font, hoveredCardTooltip, mouseX, mouseY);
        }
    }
}
