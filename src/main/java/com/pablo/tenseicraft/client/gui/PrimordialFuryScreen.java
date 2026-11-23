package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.client.ModKeyMappings;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Pantalla de la solapa desbloqueable "Furia" (Furia Primordial).
 * Muestra las habilidades y combos destructivos con sus controles y descripciones completas.
 */
public class PrimordialFuryScreen extends Screen {

    private static final int GUI_WIDTH = 340;
    private static final int GUI_HEIGHT = 246;

    private final Screen parent;
    private int leftPos;
    private int topPos;

    public PrimordialFuryScreen(Screen parent) {
        super(Component.literal("Furia Primordial"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        RpgUiHelper.addNavigationBar(this, this::addRenderableWidget, leftPos, topPos, GUI_WIDTH, RpgUiHelper.NavTab.FURY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Sin desenfoque de fondo
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xA8070910);
        RpgUiHelper.drawPanelFrame(g, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        Minecraft mc = Minecraft.getInstance();
        PlayerRpgData data = mc.player != null ? mc.player.getData(ModAttachments.PLAYER_RPG_DATA) : null;
        boolean trained = data != null && data.isPrimordialFuryTrained();
        String releaseKey = ModKeyMappings.RELEASE_SWORD_KEY.get().getTranslatedKeyMessage().getString().toUpperCase();

        int x = leftPos + 12;
        int w = GUI_WIDTH - 24;
        int y = topPos + 32;

        // Encabezado principal
        g.drawCenteredString(this.font, Component.literal("§f§l✦ FURIA PRIMORDIAL ✦"), leftPos + GUI_WIDTH / 2, y, 0xFFFFFFFF);
        y += 11;
        g.drawCenteredString(this.font, Component.literal("§7Destino Celestial"), leftPos + GUI_WIDTH / 2, y, 0xFFAAAAAA);
        y += 14;

        int cardH = 42;
        int gap = 5;

        // Tarjeta 1: Modo Furia Primordial
        drawAbilityCard(
                g, x, y, w, cardH, trained,
                "§f§lModo Furia Primordial",
                "§e[" + releaseKey + "] al 100%",
                "§7Desata el Manto (~22s) con buffs grosos y convierte tus golpes normales en Ondas Celestiales de 4.5m."
        );
        y += cardH + gap;

        // Tarjeta 2: Golpe Sísmico
        drawAbilityCard(
                g, x, y, w, cardH, trained,
                "§6✦ Golpe Sísmico",
                "§fCorrer + Click Izq",
                "§7Estampas la espada sellada contra el suelo, creando una onda expansiva de 5.5m que daña y empuja enemigos."
        );
        y += cardH + gap;

        // Tarjeta 3: Caída Destructiva
        drawAbilityCard(
                g, x, y, w, cardH, trained,
                "§6✦ Caída Destructiva",
                "§fSaltar + Click Izq",
                "§7Te lanzas en picada meteórica desde el aire; al caer detonas una onda sísmica de 6.5m con gran empuje."
        );
        y += cardH + gap;

        // Tarjeta 4: Tajo Ascendente
        drawAbilityCard(
                g, x, y, w, cardH, trained,
                "§6✦ Tajo Ascendente",
                "§fMantener Click Izq",
                "§7Corte vertical explosivo que eleva a los enemigos y te impulsa al aire para rematarlos con Caída Destructiva."
        );

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawAbilityCard(GuiGraphics g, int x, int y, int w, int h, boolean unlocked,
                                 String title, String controls, String desc) {
        int bg = unlocked ? 0xCC161C2E : 0xAA10131E;
        int outline = unlocked ? 0xFF5A658A : 0xFF31364A;
        g.fill(x, y, x + w, y + h, bg);
        g.renderOutline(x, y, w, h, outline);

        String prefix = unlocked ? "" : "§8🔒 ";
        String titleText = unlocked ? title : "§8" + title.replaceAll("§.", "");
        g.drawString(this.font, Component.literal(prefix + titleText), x + 8, y + 5, 0xFFFFFFFF, false);

        String ctrlText = unlocked ? controls : "§8" + controls.replaceAll("§.", "");
        int ctrlW = this.font.width(ctrlText);
        g.drawString(this.font, Component.literal(ctrlText), x + w - ctrlW - 8, y + 5, 0xFFE0E0E0, false);

        String bodyText = unlocked ? desc : "§8Entrena con Redfox para desbloquear esta técnica.";
        List<FormattedCharSequence> lines = this.font.split(Component.literal(bodyText), w - 16);
        int lineY = y + 18;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.drawString(this.font, lines.get(i), x + 8, lineY + (i * 10), 0xFFCCCCCC, false);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
