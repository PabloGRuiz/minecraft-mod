package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.network.ModNetworking;
import com.pablo.rpglevels.network.PartyActionPayload;
import com.pablo.rpglevels.party.ClientPartyData;
import com.pablo.rpglevels.party.PartyMemberInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PartyScreen extends Screen {

    private static final int GUI_WIDTH = 340;
    private static final int GUI_HEIGHT = 236;

    private int leftPos;
    private int topPos;
    private EditBox inviteInput;

    public PartyScreen() {
        super(Component.literal("Grupo de Aventura"));
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Barra superior unificada
        RpgUiHelper.addNavigationBar(this, this::addRenderableWidget, leftPos, topPos, GUI_WIDTH, RpgUiHelper.NavTab.PARTY);

        boolean inParty = ClientPartyData.isInParty();
        boolean isLeader = ClientPartyData.isLeader();

        if (!inParty) {
            this.addRenderableWidget(Button.builder(Component.literal("§6⚔ Crear Grupo de Aventura"), b -> {
                ModNetworking.sendPartyAction(PartyActionPayload.ACTION_CREATE, "");
                this.onClose();
            }).bounds(leftPos + 60, topPos + 86, 220, 22)
            .tooltip(Tooltip.create(Component.literal("§6Funda un nuevo equipo de expedición (máx 4 miembros) y asume el rol de líder.")))
            .build());

            this.addRenderableWidget(Button.builder(Component.literal("§a✔ Aceptar Invitación"), b -> {
                ModNetworking.sendPartyAction(PartyActionPayload.ACTION_ACCEPT, "");
                this.onClose();
            }).bounds(leftPos + 60, topPos + 120, 220, 22)
            .tooltip(Tooltip.create(Component.literal("§aÚnete al grupo de un aventurero que te haya invitado recientemente.")))
            .build());
        } else {
            if (isLeader) {
                this.inviteInput = new EditBox(this.font, leftPos + 14, topPos + 166, 212, 18, Component.literal("Jugador"));
                this.inviteInput.setMaxLength(16);
                this.inviteInput.setHint(Component.literal("§8Nombre del jugador..."));
                this.addRenderableWidget(this.inviteInput);

                this.addRenderableWidget(Button.builder(Component.literal("§b+ Invitar"), b -> {
                    String name = inviteInput.getValue().trim();
                    if (!name.isEmpty()) {
                        ModNetworking.sendPartyAction(PartyActionPayload.ACTION_INVITE, name);
                        inviteInput.setValue("");
                    }
                }).bounds(leftPos + 232, topPos + 165, 94, 20)
                .tooltip(Tooltip.create(Component.literal("§bInvita a un jugador en línea a tu grupo (máx 4 aventureros).")))
                .build());

                this.addRenderableWidget(Button.builder(Component.literal("§cDisolver Grupo"), b -> {
                    ModNetworking.sendPartyAction(PartyActionPayload.ACTION_DISBAND, "");
                    this.onClose();
                }).bounds(leftPos + 14, topPos + 196, 150, 20)
                .tooltip(Tooltip.create(Component.literal("§cDisuelve todo el grupo de aventura.")))
                .build());

                this.addRenderableWidget(Button.builder(Component.literal("§eAbandonar"), b -> {
                    ModNetworking.sendPartyAction(PartyActionPayload.ACTION_LEAVE, "");
                    this.onClose();
                }).bounds(leftPos + 176, topPos + 196, 150, 20)
                .tooltip(Tooltip.create(Component.literal("§eSal del grupo actual.")))
                .build());
            } else {
                this.addRenderableWidget(Button.builder(Component.literal("§eAbandonar Grupo"), b -> {
                    ModNetworking.sendPartyAction(PartyActionPayload.ACTION_LEAVE, "");
                    this.onClose();
                }).bounds(leftPos + 85, topPos + 196, 170, 20)
                .tooltip(Tooltip.create(Component.literal("§eSal del grupo actual.")))
                .build());
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((this.inviteInput == null || !this.inviteInput.isFocused())
                && ModKeyMappings.PARTY_KEY.get().matches(keyCode, scanCode)) {
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
        guiGraphics.fill(0, 0, this.width, this.height, 0xC8080A12);
        RpgUiHelper.drawPanelFrame(guiGraphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        boolean inParty = ClientPartyData.isInParty();

        if (!inParty) {
            guiGraphics.drawCenteredString(this.font, "§6✦ GRUPO DE AVENTURA ✦", leftPos + GUI_WIDTH / 2, topPos + 42, 0xFFFFAA00);
            guiGraphics.drawCenteredString(this.font, "§7No formas parte de ningún grupo actualmente.", leftPos + GUI_WIDTH / 2, topPos + 58, 0xFFBBBBBB);
        } else {
            List<PartyMemberInfo> members = ClientPartyData.getMembers();
            String leaderTitle = "§eLíder: §6" + ClientPartyData.getLeaderName() + " §7(" + members.size() + "/4)";
            guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, leaderTitle, 170), leftPos + 14, topPos + 31, 0xFFFFAA00, true);

            var pRank = ClientPartyData.getPartyRank();
            String rankHeader = "§7Grupo: " + pRank.getBadge().getString();
            rankHeader = RpgUiHelper.clip(this.font, rankHeader, 145);
            guiGraphics.drawString(this.font, rankHeader, leftPos + GUI_WIDTH - 14 - this.font.width(rankHeader), topPos + 31, 0xFFFFFFFF, true);

            int listY = topPos + 46;
            int boxW = GUI_WIDTH - 28;

            for (PartyMemberInfo m : members) {
                boolean hasMana = m.maxMana() > 0;
                int rowH = hasMana ? 25 : 19;

                guiGraphics.fill(leftPos + 14, listY, leftPos + 14 + boxW, listY + rowH, 0xCC0E111C);
                guiGraphics.renderOutline(leftPos + 14, listY, boxW, rowH, 0xFF3D435C);

                String nameStr = (m.isNpc() ? "§b[NPC] " : "§f") + m.name();
                guiGraphics.drawString(this.font, RpgUiHelper.clip(this.font, nameStr, 130), leftPos + 19, listY + 5, 0xFFFFFFFF, true);

                int barX = leftPos + 156;
                int barY = listY + 5;
                int barW = 108;
                int barH = 5;
                float pct = Math.max(0.0f, Math.min(1.0f, m.getHealthPercentage()));
                int fill = (int) (barW * pct);
                int col = pct > 0.5f ? 0xFF2ECC71 : (pct > 0.25f ? 0xFFF1C40F : 0xFFE74C3C);

                guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF180A0A);
                if (fill > 0) {
                    guiGraphics.fill(barX, barY, barX + fill, barY + barH, col);
                }

                String hp = String.format("%d%%", (int) (pct * 100));
                guiGraphics.drawString(this.font, "§7" + hp, barX + barW + 6, listY + 4, 0xFFAAAAAA, false);

                if (hasMana) {
                    int manaY = listY + 14;
                    int manaH = 4;
                    float manaPct = Math.max(0.0f, Math.min(1.0f, m.getManaPercentage()));
                    int manaFill = (int) (barW * manaPct);

                    guiGraphics.fill(barX, manaY, barX + barW, manaY + manaH, 0xFF081420);
                    if (manaFill > 0) {
                        guiGraphics.fill(barX, manaY, barX + manaFill, manaY + manaH, 0xFF2A82E4);
                    }
                    String mp = String.format("%d MP", (int) m.mana());
                    guiGraphics.drawString(this.font, "§b" + mp, barX + barW + 6, manaY - 2, 0xFF55FFFF, false);
                }

                listY += rowH + 4;
                if (listY > topPos + 155) break;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
