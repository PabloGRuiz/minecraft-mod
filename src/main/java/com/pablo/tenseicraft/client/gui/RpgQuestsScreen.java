package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.client.ModKeyMappings;
import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.network.QuestActionPayload;
import com.pablo.tenseicraft.quests.DailyQuestManager;
import com.pablo.tenseicraft.quests.PlayerQuestData;
import com.pablo.tenseicraft.quests.Quest;
import com.pablo.tenseicraft.quests.QuestRegistry;
import com.pablo.tenseicraft.quests.QuestType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RpgQuestsScreen extends Screen {

    private final Screen parentScreen;
    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 238;
    private static final int PAGE_SIZE = 4;
    private static final int MAIN_PILLS_PER_PAGE = 3;

    private int leftPos;
    private int topPos;
    private int selectedTab; // 0 = Principales, 1 = Diarias, 2 = Gremio e Historia
    private int currentPage = 0;
    private int selectedMainIndex = -1;
    private int mainSelectorPage = 0;
    private int lastQuestSyncHash = -1;

    public RpgQuestsScreen(Screen parentScreen) {
        super(Component.translatable("tenseicraft.gui.quests_title"));
        this.parentScreen = parentScreen;
        boolean atGuildMaster = (parentScreen instanceof NpcDialogueScreen ds && "guild_master".equals(ds.getNpcType()));
        this.selectedTab = atGuildMaster ? 1 : 0;
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
        this.lastQuestSyncHash = computeQuestSyncHash(questData);

        // 2. Tres sub-pestañas del tablón (110px cada una, separación 5px -> total 340px)
        int tabY = topPos + 42;
        int tabW = 110;
        int tabGap = 5;

        Component tab0Text = Component.literal(selectedTab == 0 ? "§d§l★ Principales" : "§7★ Principales");
        this.addRenderableWidget(Button.builder(tab0Text, b -> {
            selectedTab = 0;
            selectedMainIndex = -1;
            this.rebuildWidgets();
        }).bounds(leftPos + 10, tabY, tabW, 17)
        .tooltip(Tooltip.create(Component.literal("§d[Misiones Principales de TenseiCraft]\n§eGuían tu aventura y se completan automáticamente al cumplir su objetivo, otorgando gran experiencia escalada a tu rango.")))
        .build());

        Component tab1Text = Component.literal(selectedTab == 1 ? "§6§l📜 Diarias" : "§7📜 Diarias");
        this.addRenderableWidget(Button.builder(tab1Text, b -> {
            selectedTab = 1;
            currentPage = 0;
            this.rebuildWidgets();
        }).bounds(leftPos + 10 + tabW + tabGap, tabY, tabW, 17)
        .tooltip(Tooltip.create(Component.literal("§6[Contratos Diarios del Gremio]\n§eEncargos diarios renovables de cacería y suministros.")))
        .build());

        Component tab2Text = Component.literal(selectedTab == 2 ? "§e§l⚔ Gremio" : "§7⚔ Gremio");
        this.addRenderableWidget(Button.builder(tab2Text, b -> {
            selectedTab = 2;
            currentPage = 0;
            this.rebuildWidgets();
        }).bounds(leftPos + 10 + (tabW + tabGap) * 2, tabY, tabW, 17)
        .tooltip(Tooltip.create(Component.literal("§e[Encargos de Rango y Segadores]\n§7Pruebas de ascensión, misiones de Alice y jefes.")))
        .build());

        if (selectedTab == 0) {
            initMainStoryTab(questData);
            return;
        }

        // 3. Misiones filtradas y paginadas (Diarias / Gremio)
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
                            Button disabledBtn = Button.builder(Component.literal("§8🔒 Entregar"), b -> {})
                                    .bounds(btnX, btnY, 68, 17)
                                    .tooltip(Tooltip.create(Component.literal("§6[Entrega en el Gremio]\n§7Acude al Maestro del Gremio en una aldea para reclamar tu recompensa.")))
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
                    } else if (isAtGuildMaster) {
                        this.addRenderableWidget(Button.builder(Component.literal("§e").append(Component.translatable("tenseicraft.gui.quests_start")), b -> {
                            PacketDistributor.sendToServer(new QuestActionPayload(qId, QuestActionPayload.ACTION_ACCEPT));
                            b.active = false;
                        }).bounds(btnX, btnY, 68, 17)
                        .tooltip(Tooltip.create(Component.literal("§eAceptar este contrato del Gremio.")))
                        .build());
                    } else {
                        Button disabledBtn = Button.builder(Component.literal("§8🔒 Gremio"), b -> {})
                                .bounds(btnX, btnY, 68, 17)
                                .tooltip(Tooltip.create(Component.literal("§6[Solo en el Gremio]\n§7Habla con el Maestro del Gremio en una aldea para aceptar este encargo.")))
                                .build();
                        disabledBtn.active = false;
                        this.addRenderableWidget(disabledBtn);
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

    private void initMainStoryTab(PlayerQuestData questData) {
        List<Quest> mainQuests = QuestRegistry.getMainQuests();
        int totalMain = mainQuests.size();
        if (selectedMainIndex < 0 || selectedMainIndex >= totalMain) {
            selectedMainIndex = findDefaultMainQuestIndex(mainQuests, questData);
            mainSelectorPage = selectedMainIndex / MAIN_PILLS_PER_PAGE;
        }

        int totalSelectorPages = Math.max(1, (int) Math.ceil(totalMain / (double) MAIN_PILLS_PER_PAGE));
        mainSelectorPage = Math.max(0, Math.min(mainSelectorPage, totalSelectorPages - 1));

        // Fila selectora de capítulos en la parte inferior de la tarjeta narrativa
        int selectorY = topPos + 178;
        int start = mainSelectorPage * MAIN_PILLS_PER_PAGE;
        int end = Math.min(totalMain, start + MAIN_PILLS_PER_PAGE);
        int pillW = 108;
        int pillGap = 8;

        for (int idx = start; idx < end; idx++) {
            final int chapterIdx = idx;
            Quest q = mainQuests.get(idx);
            boolean isClaimed = questData != null && questData.isQuestClaimed(q.id());
            boolean isActive = questData != null && questData.isQuestActive(q.id());
            boolean isSelected = (chapterIdx == selectedMainIndex);
            int slot = idx - start;
            int px = leftPos + 10 + slot * (pillW + pillGap);

            if (isClaimed || isActive) {
                String icon = isClaimed ? "§a✔ " : "§d★ ";
                String color = isSelected ? "§l" : "";
                String label = icon + color + "Parte " + (chapterIdx + 1);
                Button chBtn = Button.builder(Component.literal(label), b -> {
                    selectedMainIndex = chapterIdx;
                    this.rebuildWidgets();
                }).bounds(px, selectorY, pillW, 18)
                .tooltip(Tooltip.create(q.getTitle().copy().withStyle(isClaimed ? ChatFormatting.GREEN : ChatFormatting.LIGHT_PURPLE)))
                .build();
                chBtn.active = !isSelected;
                this.addRenderableWidget(chBtn);
            } else {
                Button lockedBtn = Button.builder(Component.literal("§8🔒 Parte " + (chapterIdx + 1)), b -> {})
                        .bounds(px, selectorY, pillW, 18)
                        .tooltip(Tooltip.create(Component.literal("§8[Capítulo Bloqueado]\n§7Completa los pasos actuales para descubrir la continuación.")))
                        .build();
                lockedBtn.active = false;
                this.addRenderableWidget(lockedBtn);
            }
        }

        // Navegación inferior si hay más de 3 capítulos
        int bottomY = topPos + GUI_HEIGHT - 24;
        Button prevBtn = Button.builder(Component.literal("§e◀"), b -> {
            if (mainSelectorPage > 0) {
                mainSelectorPage--;
                this.rebuildWidgets();
            }
        }).bounds(leftPos + 10, bottomY, 24, 17).build();
        prevBtn.active = mainSelectorPage > 0;
        this.addRenderableWidget(prevBtn);

        Button nextBtn = Button.builder(Component.literal("§e▶"), b -> {
            if (mainSelectorPage < totalSelectorPages - 1) {
                mainSelectorPage++;
                this.rebuildWidgets();
            }
        }).bounds(leftPos + 92, bottomY, 24, 17).build();
        nextBtn.active = mainSelectorPage < totalSelectorPages - 1;
        this.addRenderableWidget(nextBtn);

        if (this.parentScreen != null) {
            this.addRenderableWidget(Button.builder(Component.literal("§cVolver"), b -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(this.parentScreen);
                }
            }).bounds(leftPos + GUI_WIDTH - 80, bottomY, 70, 17).build());
        }
    }

    private int findDefaultMainQuestIndex(List<Quest> mainQuests, PlayerQuestData questData) {
        if (mainQuests.isEmpty() || questData == null) return 0;
        for (int i = 0; i < mainQuests.size(); i++) {
            if (questData.isQuestActive(mainQuests.get(i).id())) {
                return i;
            }
        }
        int lastClaimed = 0;
        for (int i = 0; i < mainQuests.size(); i++) {
            if (questData.isQuestClaimed(mainQuests.get(i).id())) {
                lastClaimed = i;
            }
        }
        return lastClaimed;
    }

    private List<Quest> collectDisplayQuests(Player player, PlayerRpgData rpgData, PlayerQuestData questData,
                                             boolean isRegistered, AdventurerRank rank) {
        List<Quest> displayQuests = new ArrayList<>();
        if (selectedTab == 1) {
            displayQuests.addAll(DailyQuestManager.getDailyQuests(player.level(), rank));
        } else {
            if (isRegistered) {
                displayQuests.addAll(QuestRegistry.getQuestsForRank(rank));
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
                    if (q != null && !q.isDaily() && !q.isMain() && !displayQuests.contains(q)) {
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

    private int computeQuestSyncHash(PlayerQuestData questData) {
        if (questData == null) return 0;
        int progSum = 0;
        for (int v : questData.getActiveQuests().values()) {
            progSum = progSum * 31 + v;
        }
        return questData.getActiveQuests().size() * 97 + questData.getClaimedQuests().size() * 31 + progSum;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft != null && this.minecraft.player != null) {
            PlayerQuestData questData = this.minecraft.player.getData(ModAttachments.PLAYER_QUEST_DATA);
            int hash = computeQuestSyncHash(questData);
            if (hash != this.lastQuestSyncHash) {
                this.lastQuestSyncHash = hash;
                if (selectedTab == 0) {
                    selectedMainIndex = -1;
                }
                this.rebuildWidgets();
            }
        }
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

        // Sub-cabecera
        String subHeader = (selectedTab == 0) ? "§d★ Historia Principal de TenseiCraft" : "📜 §6§lGremio de Aventureros";
        guiGraphics.drawString(this.font, subHeader, leftPos + 10, topPos + 30, 0xFFFFAA00, true);
        Component rankBadge = isRegistered ? rank.getBadge() : Component.literal("[Civil No Registrado]").withStyle(ChatFormatting.DARK_GRAY);
        guiGraphics.drawString(this.font, rankBadge, leftPos + GUI_WIDTH - 10 - this.font.width(rankBadge), topPos + 30, 0xFFFFFFFF, false);

        if (selectedTab == 0) {
            renderMainStoryTab(guiGraphics, rpgData, questData, rank);
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

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
                    List<Component> tip = new ArrayList<>();
                    tip.add(quest.getTitle().copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    tip.addAll(this.font.split(quest.getDescription().copy().withStyle(ChatFormatting.GRAY), 210)
                            .stream().map(seq -> Component.literal("").append(convertSeq(seq))).toList());
                    String itemNote = "";
                    ItemStack rewardItem = quest.createRewardItemStack();
                    if (!rewardItem.isEmpty()) {
                        itemNote = " §8| §b" + rewardItem.getCount() + "x " + rewardItem.getHoverName().getString();
                    }
                    tip.add(Component.literal("§eRecompensa: §a+" + quest.rewardXp() + " XP"
                            + (quest.rewardReputation() > 0 ? " §8| §6+" + quest.rewardReputation() + " Rep" : "")
                            + itemNote));
                    hoveredCardTooltip = tip;
                }

                itemY += 37;
            }
        }

        boolean isAtGuildMaster = (this.parentScreen instanceof NpcDialogueScreen ds && "guild_master".equals(ds.getNpcType()));
        String pageLabel = "§7Pág. §f" + (currentPage + 1) + "§7/" + totalPages;
        guiGraphics.drawCenteredString(this.font, pageLabel, leftPos + 63, topPos + GUI_HEIGHT - 19, 0xFFFFFFFF);

        String modeHint = isAtGuildMaster
                ? "§a✔ Tablón del Maestro del Gremio"
                : "§8Solo lectura • Acepta y entrega con el Maestro del Gremio";
        int maxHintW = (this.parentScreen != null) ? (GUI_WIDTH - 208) : (GUI_WIDTH - 132);
        guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, modeHint, maxHintW),
                leftPos + 122, topPos + GUI_HEIGHT - 19, 0xFFAAAAAA, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (hoveredCardTooltip != null) {
            guiGraphics.renderComponentTooltip(this.font, hoveredCardTooltip, mouseX, mouseY);
        }
    }

    private void renderMainStoryTab(GuiGraphics guiGraphics, PlayerRpgData rpgData, PlayerQuestData questData, AdventurerRank rank) {
        List<Quest> mainQuests = QuestRegistry.getMainQuests();
        int totalMain = mainQuests.size();
        int idx = Math.max(0, Math.min(totalMain - 1, selectedMainIndex));
        int boxX = leftPos + 10;
        int boxY = topPos + 64;
        int boxW = GUI_WIDTH - 20;
        int boxH = 108;

        guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x77161226);

        if (mainQuests.isEmpty()) {
            guiGraphics.renderOutline(boxX, boxY, boxW, boxH, 0xFF3E445C);
            guiGraphics.drawCenteredString(this.font, "§7No hay misiones principales registradas aún.", leftPos + GUI_WIDTH / 2, boxY + 46, 0xFFAAAAAA);
            return;
        }

        Quest q = mainQuests.get(idx);
        boolean isClaimed = questData != null && questData.isQuestClaimed(q.id());
        boolean isActive = questData != null && questData.isQuestActive(q.id());

        int border = isClaimed ? 0xFF55FF55 : (isActive ? 0xFFCC66FF : 0xFF3E445C);
        guiGraphics.renderOutline(boxX, boxY, boxW, boxH, border);

        if (!isClaimed && !isActive) {
            guiGraphics.drawCenteredString(this.font, "§8🔒 Capítulo bloqueado", leftPos + GUI_WIDTH / 2, boxY + 40, 0xFF888888);
            guiGraphics.drawCenteredString(this.font, "§7Avanza en los capítulos anteriores para desbloquearlo.", leftPos + GUI_WIDTH / 2, boxY + 54, 0xFF777777);
        } else {
            // Título del capítulo y estado a la derecha
            String statusBadge = isClaimed ? "§a✔ Completada" : "§d★ En Curso";
            int statusW = this.font.width(statusBadge);
            guiGraphics.drawString(this.font, statusBadge, boxX + boxW - 8 - statusW, boxY + 7, 0xFFFFFFFF, false);

            String titleStr = "§d§l" + q.getTitle().getString();
            guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, titleStr, boxW - statusW - 22), boxX + 8, boxY + 7, 0xFFFFFFFF, false);
            guiGraphics.hLine(boxX + 6, boxX + boxW - 6, boxY + 18, 0xFF393F56);

            // Descripción narrativa completa en varias líneas
            List<FormattedCharSequence> wrappedDesc = this.font.split(q.getDescription().copy().withStyle(ChatFormatting.GRAY), boxW - 16);
            int lineY = boxY + 23;
            for (int l = 0; l < wrappedDesc.size() && lineY <= boxY + 68; l++) {
                guiGraphics.drawString(this.font, wrappedDesc.get(l), boxX + 8, lineY, 0xFFCCCCCC, false);
                lineY += 9;
            }

            // Separador y bloque de Objetivo + Recompensa relativa al rango
            guiGraphics.hLine(boxX + 6, boxX + boxW - 6, boxY + 77, 0xFF2D334A);
            String objLine = "§eObjetivo: §f" + q.getObjective().getString();
            guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, objLine, boxW - 16), boxX + 8, boxY + 82, 0xFFFFFFFF, false);

            boolean blessed = rpgData != null && rpgData.hasDivineBlessingUnlocked();
            int curLvl = (rpgData != null) ? rpgData.getRpgLevel() : 1;
            int vanillaXp = QuestRegistry.computeMainQuestVanillaXp(rank);
            int rpgXp = QuestRegistry.computeMainQuestRpgXp(rank, curLvl);
            String rewardPreview = blessed
                    ? "§bRecompensa: §e+" + rpgXp + " XP RPG §7(Rango " + rank.getName() + ") §8+ §a" + vanillaXp + " XP §8| §dAuto-completa"
                    : "§bRecompensa: §a+" + vanillaXp + " XP §8| §dSe completa automáticamente";
            guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, rewardPreview, boxW - 16), boxX + 8, boxY + 94, 0xFFAAAAAA, false);
        }

        // Pie de página e indicación si está esperando sorpresa/desbloqueo tras completar el capítulo activo
        int totalSelectorPages = Math.max(1, (int) Math.ceil(totalMain / (double) MAIN_PILLS_PER_PAGE));
        String pageLabel = "§7Pág. §f" + (mainSelectorPage + 1) + "§7/" + totalSelectorPages;
        guiGraphics.drawCenteredString(this.font, pageLabel, leftPos + 63, topPos + GUI_HEIGHT - 19, 0xFFFFFFFF);

        boolean hasActiveMain = mainQuests.stream().anyMatch(mq -> questData != null && questData.isQuestActive(mq.id()));
        String hint = (!hasActiveMain && questData != null && questData.isQuestClaimed("main_01_welcome") && !questData.isQuestClaimed("main_02_ascend"))
                ? "§e✦ Duerme para descubrir la sorpresa pendiente..."
                : "§d★ Historia Principal • Auto-completado por hitos";
        int maxHintW = (this.parentScreen != null) ? (GUI_WIDTH - 208) : (GUI_WIDTH - 132);
        guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, hint, maxHintW),
                leftPos + 122, topPos + GUI_HEIGHT - 19, 0xFFAAAAAA, false);
    }

    private Component convertSeq(FormattedCharSequence seq) {
        StringBuilder sb = new StringBuilder();
        seq.accept((idx, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return Component.literal(sb.toString()).withStyle(ChatFormatting.GRAY);
    }
}
