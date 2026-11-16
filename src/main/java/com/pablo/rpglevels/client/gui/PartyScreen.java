package com.pablo.rpglevels.client.gui;

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

    private static final int GUI_WIDTH = 250;
    private static final int GUI_HEIGHT = 195;

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

        boolean inParty = ClientPartyData.isInParty();
        boolean isLeader = ClientPartyData.isLeader();

        if (!inParty) {
            // Botón Crear Grupo
            this.addRenderableWidget(Button.builder(Component.literal("§6⚔ Crear Grupo de Aventura"), b -> {
                ModNetworking.sendPartyAction(PartyActionPayload.ACTION_CREATE, "");
                this.onClose();
            }).bounds(leftPos + 25, topPos + 62, 200, 22)
            .tooltip(Tooltip.create(Component.literal("§6Funda un nuevo equipo de expedición (máx 4 miembros) y asume el rol de líder.")))
            .build());

            // Botón Aceptar Invitación
            this.addRenderableWidget(Button.builder(Component.literal("§a✔ Aceptar Invitación"), b -> {
                ModNetworking.sendPartyAction(PartyActionPayload.ACTION_ACCEPT, "");
                this.onClose();
            }).bounds(leftPos + 25, topPos + 94, 200, 22)
            .tooltip(Tooltip.create(Component.literal("§aÚnete al grupo de un aventurero que te haya invitado recientemente.")))
            .build());
        } else {
            // Si está en el grupo
            if (isLeader) {
                // Caja de texto para invitar
                this.inviteInput = new EditBox(this.font, leftPos + 14, topPos + 138, 140, 18, Component.literal("Jugador"));
                this.inviteInput.setMaxLength(16);
                this.addRenderableWidget(this.inviteInput);

                // Botón Invitar
                this.addRenderableWidget(Button.builder(Component.literal("§b+ Invitar"), b -> {
                    String name = inviteInput.getValue().trim();
                    if (!name.isEmpty()) {
                        ModNetworking.sendPartyAction(PartyActionPayload.ACTION_INVITE, name);
                        inviteInput.setValue("");
                    }
                }).bounds(leftPos + 158, topPos + 137, 78, 20)
                .tooltip(Tooltip.create(Component.literal("§bInvita a un jugador en línea a tu grupo (máximo 4 aventureros).")))
                .build());

                // Botón Disolver
                this.addRenderableWidget(Button.builder(Component.literal("§cDisolver"), b -> {
                    ModNetworking.sendPartyAction(PartyActionPayload.ACTION_DISBAND, "");
                    this.onClose();
                }).bounds(leftPos + 14, topPos + 165, 84, 20)
                .tooltip(Tooltip.create(Component.literal("§cDisuelve todo el grupo de aventura.")))
                .build());
            }

            // Botón Abandonar
            this.addRenderableWidget(Button.builder(Component.literal("§eAbandonar"), b -> {
                ModNetworking.sendPartyAction(PartyActionPayload.ACTION_LEAVE, "");
                this.onClose();
            }).bounds(leftPos + (isLeader ? 112 : 75), topPos + 165, 100, 20)
            .tooltip(Tooltip.create(Component.literal("§eSal del grupo actual.")))
            .build());
        }

        // Botón Cerrar
        this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> this.onClose())
                .bounds(leftPos + GUI_WIDTH - 22, topPos + 6, 16, 16)
                .build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Intencionalmente vacío: anula processBlurEffect de Minecraft 1.21 para evitar difuminar la interfaz
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fondo oscuro y nítido para bloquear por completo el desenfoque del mundo 3D y la lluvia
        guiGraphics.fill(0, 0, this.width, this.height, 0xD0080A12);

        // Ventana principal RPG 100% opaca con marco dorado y sombras profundas
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFF121524);
        guiGraphics.fill(leftPos + 4, topPos + 4, leftPos + GUI_WIDTH - 4, topPos + GUI_HEIGHT - 4, 0xFF181C2E);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFE5A93C);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0xFF4D536E);

        // Línea dorada separadora superior
        guiGraphics.hLine(leftPos + 8, leftPos + GUI_WIDTH - 8, topPos + 26, 0xFFE5A93C);

        // Título con sombra
        guiGraphics.drawCenteredString(this.font, Component.literal("§6§l✦ GRUPO DE AVENTURA ✦"), leftPos + GUI_WIDTH / 2, topPos + 9, 0xFFFFAA00);

        boolean inParty = ClientPartyData.isInParty();

        if (!inParty) {
            // Subtítulo claro y legible
            guiGraphics.drawCenteredString(this.font, Component.literal("§fNo formas parte de ningún grupo actualmente."), leftPos + GUI_WIDTH / 2, topPos + 38, 0xFFFFFFFF);
        } else {
            List<PartyMemberInfo> members = ClientPartyData.getMembers();
            String leaderTitle = "§eLíder: §6" + ClientPartyData.getLeaderName() + " §7(" + members.size() + "/4)";
            guiGraphics.drawString(this.font, Component.literal(leaderTitle), leftPos + 14, topPos + 31, 0xFFFFAA00, true);

            var pRank = ClientPartyData.getPartyRank();
            String rankHeader = "§7Rango Grupo: ";
            int totalRankW = this.font.width(rankHeader) + this.font.width(pRank.getBadge());
            guiGraphics.drawString(this.font, Component.literal(rankHeader).append(pRank.getBadge()), leftPos + GUI_WIDTH - 14 - totalRankW, topPos + 31, 0xFFFFFFFF, true);

            int listY = topPos + 44;

            for (PartyMemberInfo m : members) {
                boolean hasMana = m.maxMana() > 0;
                int rowH = hasMana ? 20 : 16;

                // Caja de miembro
                guiGraphics.fill(leftPos + 12, listY, leftPos + GUI_WIDTH - 12, listY + rowH, 0xFF0E111C);
                guiGraphics.renderOutline(leftPos + 12, listY, GUI_WIDTH - 24, rowH, 0xFF3D435C);

                String nameStr = (m.isNpc() ? "§b[NPC] " : "") + m.name();
                guiGraphics.drawString(this.font, Component.literal(nameStr), leftPos + 16, listY + 4, 0xFFFFFFFF, true);

                // Barra de vida
                int barX = leftPos + 120;
                int barY = listY + 4;
                int barW = 55;
                int barH = 5;
                float pct = Math.max(0.0f, Math.min(1.0f, m.getHealthPercentage()));
                int fill = (int) (barW * pct);
                int col = pct > 0.5f ? 0xFF2ECC71 : (pct > 0.25f ? 0xFFF1C40F : 0xFFE74C3C);

                guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF180A0A);
                if (fill > 0) {
                    guiGraphics.fill(barX, barY, barX + fill, barY + barH, col);
                }

                String hp = String.format("%d%%", (int) (pct * 100));
                guiGraphics.drawString(this.font, Component.literal("§7" + hp), barX + barW + 5, listY + 3, 0xFFAAAAAA, false);

                // Barra de maná (si aplica)
                if (hasMana) {
                    int manaY = listY + 11;
                    int manaH = 4;
                    float manaPct = Math.max(0.0f, Math.min(1.0f, m.getManaPercentage()));
                    int manaFill = (int) (barW * manaPct);

                    guiGraphics.fill(barX, manaY, barX + barW, manaY + manaH, 0xFF081420);
                    if (manaFill > 0) {
                        guiGraphics.fill(barX, manaY, barX + manaFill, manaY + manaH, 0xFF2A82E4);
                    }
                    String mp = String.format("%d MP", (int) m.mana());
                    guiGraphics.drawString(this.font, Component.literal("§b" + mp), barX + barW + 5, manaY - 1, 0xFF55FFFF, false);
                }

                listY += rowH + 3;
                if (listY > topPos + 130) break;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
