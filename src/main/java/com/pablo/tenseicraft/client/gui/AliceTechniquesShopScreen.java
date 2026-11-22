package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.magic.SpiritualTechnique;
import com.pablo.tenseicraft.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AliceTechniquesShopScreen extends Screen {

    private final Screen parentScreen;
    private final int GUI_WIDTH = 345;
    private final int GUI_HEIGHT = 285;
    private int leftPos;
    private int topPos;

    public AliceTechniquesShopScreen(Screen parentScreen) {
        super(Component.literal("Tienda de Técnicas Espirituales"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        int emeralds = getPlayerEmeralds(player);

        SpiritualTechnique[] techniques = SpiritualTechnique.getShopTechniques();
        int startY = topPos + 36;
        int rowHeight = 22;

        for (int i = 0; i < techniques.length; i++) {
            SpiritualTechnique tech = techniques[i];
            int currentY = startY + (i * rowHeight);
            boolean alreadyLearned = data != null && data.hasSkill(tech.getId());
            boolean canAfford = emeralds >= tech.getEmeraldCost();

            Component btnText;
            if (alreadyLearned) {
                btnText = Component.literal("§a✔ Dominado");
            } else {
                btnText = Component.literal(canAfford ? ("§aAprender §e(" + tech.getEmeraldCost() + " ❇)") : ("§7Faltan ❇ §e(" + tech.getEmeraldCost() + ")"));
            }

            // Rich Tooltip with full description, sequence, and mana cost
            String techName = tech == SpiritualTechnique.ELEMENTAL_BALL && player != null
                    ? SpiritualTechnique.getDynamicElementalBallName(player)
                    : tech.getDisplayName();

            String tierDisplay = switch (tech.getTier()) {
                case "Avanzado" -> "§5[Avanzado]";
                case "Intermedio" -> "§6[Intermedio]";
                default -> "§b[Principiante]";
            };

            String tooltipContent = "§6" + techName + "\n" +
                    "§eRango: " + tierDisplay + "\n" +
                    "§dCántico: §f" + tech.getChant() + "\n" +
                    "§bSecuencia Sagrada: §f[ALT IZQ] + §e" + tech.getSequenceDisplay() + "\n" +
                    "§bCoste de Maná: §d" + (int) tech.getManaCost() + " MP\n" +
                    "§3Enfriamiento: §f" + String.format(java.util.Locale.ROOT, "%.1f", tech.getCooldownSeconds()) + "s\n" +
                    "§aPrecio de Aprendizaje: §f" + tech.getEmeraldCost() + " Esmeraldas\n\n" +
                    "§7" + tech.getDescription();

            Button buyBtn = Button.builder(btnText, b -> {
                ModNetworking.sendBuySpiritualSkill(tech.getId());
                b.active = false;
                // Schedule GUI refresh
                Minecraft.getInstance().tell(this::rebuildWidgets);
            })
            .bounds(leftPos + GUI_WIDTH - 105, currentY + 1, 95, 18)
            .tooltip(Tooltip.create(Component.literal(tooltipContent)))
            .build();

            buyBtn.active = !alreadyLearned && canAfford;
            this.addRenderableWidget(buyBtn);
        }

        // Botón Reroll Espada (25 Esmeraldas)
        if (data != null && data.hasReceivedSpiritualSword()) {
            int parentEntityId = (this.parentScreen instanceof NpcDialogueScreen npcScreen) ? npcScreen.getEntityId() : -1;
            if (parentEntityId != -1) {
                this.addRenderableWidget(Button.builder(Component.literal("§d🎲 Reroll Espada §e(25 ❇)"), b -> {
                    ModNetworking.sendNpcDialogueAction(parentEntityId, com.pablo.tenseicraft.network.NpcDialogueActionPayload.ACTION_ALICE_REROLL_SOUL_BLADE);
                    Minecraft.getInstance().tell(this::rebuildWidgets);
                })
                .bounds(leftPos + 12, topPos + GUI_HEIGHT - 23, 140, 18)
                .tooltip(Tooltip.create(Component.literal("§d[Reroll Espiritual de Hoja (25 Esmeraldas)]\n§eAlice rearmonizará tu alma para forjar un nuevo destino para tu espada:\n§7- Reroll completo: Estilo, Elemento, 1ª Liberación, Resonancia y Skin.\n§a- ¡Conservas el despertar! Al presionar [Z] mutará a su nueva forma sin repetir la prueba.")))
                .build());
            }
        }

        // Back / Close Button
        this.addRenderableWidget(Button.builder(Component.literal("§cVolver"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parentScreen);
            }
        }).bounds(leftPos + GUI_WIDTH - 95, topPos + GUI_HEIGHT - 23, 85, 18).build());
    }

    public static int getPlayerEmeralds(Player player) {
        if (player == null) return 0;
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.EMERALD)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Sin shader de desenfoque
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Oscurecer fondo
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        // Ventana principal
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF010121C);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFF8A55C8);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0x408A55C8);

        // Header
        guiGraphics.drawString(font, "§5✦ ARTES ESPIRITUALES ✦", leftPos + 12, topPos + 10, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§7Alice te enseñará las técnicas sagradas a cambio de esmeraldas", leftPos + 12, topPos + 22, 0xFFAAAAAA, false);

        Player player = Minecraft.getInstance().player;
        int emeralds = getPlayerEmeralds(player);
        String emeraldsStr = "§aEsmeraldas: §e" + emeralds + " §a❇";
        guiGraphics.drawString(font, emeraldsStr, leftPos + GUI_WIDTH - 14 - font.width(emeraldsStr), topPos + 10, 0xFF55FF55, false);

        // Divisor
        guiGraphics.fill(leftPos + 10, topPos + 34, leftPos + GUI_WIDTH - 10, topPos + 35, 0x806A35A8);

        // Listar técnicas
        SpiritualTechnique[] techniques = SpiritualTechnique.getShopTechniques();
        int startY = topPos + 36;
        int rowHeight = 22;

        for (int i = 0; i < techniques.length; i++) {
            SpiritualTechnique tech = techniques[i];
            int currentY = startY + (i * rowHeight);

            // Row background highlight
            int rowBg = (i % 2 == 0) ? 0x251F1835 : 0x151F1835;
            guiGraphics.fill(leftPos + 10, currentY, leftPos + GUI_WIDTH - 10, currentY + 20, rowBg);

            // Tier badge
            String tierBadge = switch (tech.getTier()) {
                case "Avanzado" -> "§5[Avanz.]";
                case "Intermedio" -> "§6[Interm.]";
                default -> "§b[Princ.]";
            };
            guiGraphics.drawString(font, tierBadge, leftPos + 14, currentY + 3, 0xFFFFFFFF, false);

            // Skill display name
            String techName = tech == SpiritualTechnique.ELEMENTAL_BALL && player != null
                    ? SpiritualTechnique.getDynamicElementalBallName(player)
                    : tech.getDisplayName();

            // Truncate if long for label
            String shortName = techName.replace("Técnica Espiritual Nro. ", "Nº");
            guiGraphics.drawString(font, "§f" + shortName, leftPos + 60, currentY + 3, 0xFFFFFFFF, false);

            // Sequence & Mana subline
            String subline = "§7Seq: §e" + tech.getSequenceDisplay() + " §7| Coste: §d" + (int) tech.getManaCost() + " MP";
            guiGraphics.drawString(font, subline, leftPos + 60, currentY + 13, 0xFFAAAAAA, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}

