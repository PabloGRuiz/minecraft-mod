package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.client.ModKeyMappings;
import com.pablo.tenseicraft.data.GreekGod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.magic.SpiritualTechnique;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla de recordatorio de todas las técnicas aprendidas y artes desbloqueadas
 * (Técnicas Espirituales, Espada Espiritual, Movilidad, Guardián y Bendición).
 */
public class RpgPhysicalSkillsScreen extends Screen {

    private record LearnedEntry(
            String title,
            String controlBadge,
            String subline,
            int borderColor,
            List<Component> tooltip
    ) {}

    private final Screen parentScreen;
    private static final int GUI_WIDTH = 380;
    private static final int GUI_HEIGHT = 248;
    private static final int PAGE_SIZE = 4;

    private int leftPos;
    private int topPos;
    private int activeSubTab = 0; // 0 = Técnicas Espirituales, 1 = Artes y Habilidades
    private int currentPage = 0;

    public RpgPhysicalSkillsScreen(Screen parentScreen) {
        super(Component.translatable("tenseicraft.gui.skills_title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // 1. Barra superior unificada
        RpgUiHelper.addNavigationBar(this, this::addRenderableWidget, leftPos, topPos, GUI_WIDTH, RpgUiHelper.NavTab.SKILLS);

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;

        List<SpiritualTechnique> learnedSpiritual = collectLearnedSpiritual(data);
        List<LearnedEntry> learnedOther = collectLearnedOther(player, data);

        // 2. Sub-pestañas
        int tabW = 176;
        Component tab0Text = Component.literal((activeSubTab == 0 ? "§d§l✦ " : "§7✦ ")
                + "Técnicas Espirituales (" + learnedSpiritual.size() + ")");
        this.addRenderableWidget(Button.builder(tab0Text, b -> {
            activeSubTab = 0;
            currentPage = 0;
            this.rebuildWidgets();
        }).bounds(leftPos + 10, topPos + 43, tabW, 17)
        .tooltip(Tooltip.create(Component.literal("§d[Técnicas Espirituales]\n§7Cánticos en Lengua Antigua aprendidos con Alice.")))
        .build());

        Component tab1Text = Component.literal((activeSubTab == 1 ? "§e§l⚔ " : "§7⚔ ")
                + "Artes y Habilidades (" + learnedOther.size() + ")");
        this.addRenderableWidget(Button.builder(tab1Text, b -> {
            activeSubTab = 1;
            currentPage = 0;
            this.rebuildWidgets();
        }).bounds(leftPos + 194, topPos + 43, tabW, 17)
        .tooltip(Tooltip.create(Component.literal("§e[Artes y Habilidades]\n§7Dash, Doble Salto, Espada Espiritual, Guardián y Bendición.")))
        .build());

        // 3. Controles inferiores de paginación
        int totalItems = (activeSubTab == 0) ? learnedSpiritual.size() : learnedOther.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

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

    private List<SpiritualTechnique> collectLearnedSpiritual(PlayerRpgData data) {
        List<SpiritualTechnique> list = new ArrayList<>();
        if (data == null) return list;
        for (SpiritualTechnique tech : SpiritualTechnique.values()) {
            if (data.hasSkill(tech.getId())) {
                list.add(tech);
            }
        }
        return list;
    }

    private ItemStack findSpiritualSword(Player player) {
        if (player == null) return ItemStack.EMPTY;
        if (player.getMainHandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof SpiritualSwordItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private List<LearnedEntry> collectLearnedOther(Player player, PlayerRpgData data) {
        List<LearnedEntry> entries = new ArrayList<>();
        if (player == null || data == null) return entries;

        ItemStack swordStack = findSpiritualSword(player);
        String dashKey = ModKeyMappings.DASH_KEY.get().getTranslatedKeyMessage().getString();
        String magicKey = ModKeyMappings.MAGIC_MODIFIER_KEY.get().getTranslatedKeyMessage().getString();
        String swordKey = ModKeyMappings.RELEASE_SWORD_KEY.get().getTranslatedKeyMessage().getString();
        String summonKey = ModKeyMappings.SUMMON_GUARDIAN_KEY.get().getTranslatedKeyMessage().getString();

        // 1. Dash 360°
        if (data.hasSkill("agility_dash")) {
            float dashCd = data.getDashCooldownTicks(player) / 20.0f;
            List<Component> tt = new ArrayList<>();
            tt.add(Component.literal("§b§l💨 Dash 360°"));
            tt.add(Component.literal("§7Tecla: §e[" + dashKey + "] §8| §3Enfriamiento: §f" + String.format(Locale.ROOT, "%.1fs", dashCd)));
            wrapLines(tt, "Desplazamiento rápido en la dirección de movimiento con breve evasión.", ChatFormatting.GRAY, 220);
            entries.add(new LearnedEntry(
                    "§b💨 Dash 360°",
                    "§e[" + dashKey + "]",
                    "§7Esquive direccional instantáneo §8| §3CD: " + String.format(Locale.ROOT, "%.1fs", dashCd),
                    0xFF3A8FB7,
                    tt
            ));
        }

        // 2. Doble Salto Aéreo
        if (data.hasSkill("agility_double_jump")) {
            List<Component> tt = new ArrayList<>();
            tt.add(Component.literal("§a§l⚡ Doble Salto Aéreo"));
            tt.add(Component.literal("§7Control: §e[Espacio en el aire]"));
            wrapLines(tt, "Permite ejecutar un segundo impulso mientras estás en el aire.", ChatFormatting.GRAY, 220);
            entries.add(new LearnedEntry(
                    "§a⚡ Doble Salto Aéreo",
                    "§e[Espacio en aire]",
                    "§7Impulso adicional en pleno salto §8| §aDesbloqueado por agilidad",
                    0xFF45A860,
                    tt
            ));
        }

        // 3. Afinidad Espiritual
        if (data.hasSkill("magic_affinity")) {
            List<Component> tt = new ArrayList<>();
            tt.add(Component.literal("§d§l✦ Afinidad Espiritual"));
            tt.add(Component.literal("§7Cánticos: Mantén §e[" + magicKey + "] §7y pulsa §f1 / 2 / 3"));
            wrapLines(tt, "Desbloquea los Cánticos en Lengua Antigua y permite dominar objetos encantados sin penalización.", ChatFormatting.GRAY, 220);
            entries.add(new LearnedEntry(
                    "§d✦ Afinidad Espiritual",
                    "§e[" + magicKey + "] + 1/2/3",
                    "§7Habilita Cánticos Espirituales y uso de equipo encantado",
                    0xFF9C55CC,
                    tt
            ));
        }

        // 4. Espada Espiritual — Liberación
        if (data.hasReceivedSpiritualSword() || !swordStack.isEmpty()) {
            List<Component> tt = new ArrayList<>();
            tt.add(Component.literal("§5§l🗡 Espada Espiritual — Liberación"));
            tt.add(Component.literal("§7Tecla de Liberación: §e[" + swordKey + "]"));
            String subline;
            if (!swordStack.isEmpty()) {
                SpiritualSwordItem.BladeStyle style = SpiritualSwordItem.getBladeStyle(swordStack);
                SpiritualSwordItem.BladeElement elem = SpiritualSwordItem.getBladeElement(swordStack);
                SpiritualSwordItem.BladeResonance res = SpiritualSwordItem.getBladeResonance(swordStack);
                int aff = SpiritualSwordItem.getAffinity(swordStack);
                int maxAff = SpiritualSwordItem.getMaxAffinity(swordStack);
                String styleName = Component.translatable(style.getTranslationKey()).getString();
                String elemName = Component.translatable(elem.getTranslationKey()).getString();
                String resName = Component.translatable(res.getTranslationKey()).getString();
                subline = "§7Estilo: " + style.getColor() + styleName + " §8| §7Elem.: " + elem.getColor() + elemName;
                tt.add(Component.literal("§7Estilo: " + style.getColor() + styleName));
                tt.add(Component.literal("§7Elemento: " + elem.getColor() + elemName));
                tt.add(Component.literal("§7Resonancia: " + res.getColor() + resName));
                tt.add(Component.literal("§7Afinidad: §d" + aff + "/" + maxAff));
            } else {
                subline = "§7Empuña tu Espada Espiritual para despertar su forma";
            }
            tt.add(Component.literal("§aMientras esté liberada, tus técnicas cuestan 0 MP."));
            entries.add(new LearnedEntry(
                    "§5🗡 Espada Espiritual — Liberación",
                    "§e[" + swordKey + "]",
                    subline,
                    0xFFAA44CC,
                    tt
            ));
        }

        // 5. 1ª Liberación Activa
        if (data.isFirstReleaseUnlocked() || (!swordStack.isEmpty() && SpiritualSwordItem.isFirstReleaseUnlocked(swordStack))) {
            List<Component> tt = new ArrayList<>();
            String title;
            String subline;
            if (!swordStack.isEmpty()) {
                SpiritualSwordItem.BladeRelease rel = SpiritualSwordItem.getBladeRelease(swordStack);
                String relName = Component.translatable(rel.getTranslationKey()).getString();
                String relDesc = Component.translatable(rel.getDescriptionKey()).getString();
                title = "§6⚡ 1ª Liberación: " + rel.getColor() + relName;
                String costLine = "§7Coste: §d" + (int) rel.getManaCost() + " MP §8| §3CD: "
                        + String.format(Locale.ROOT, "%.1fs", rel.getCooldownTicks() / 20.0f);
                subline = costLine + " §7(con espada liberada)";
                tt.add(Component.literal(title));
                tt.add(Component.literal("§7Uso: §e[Agacharse + Clic Der.] §7(con espada liberada)"));
                tt.add(Component.literal(costLine));
                wrapLines(tt, relDesc, ChatFormatting.GRAY, 220);
            } else {
                title = "§6⚡ 1ª Liberación Activa";
                subline = "§7Habilidad única de tu Espada Espiritual despertada";
                tt.add(Component.literal(title));
                tt.add(Component.literal("§7Uso: §e[Agacharse + Clic Der.]"));
                wrapLines(tt, "Empuña y libera tu Espada Espiritual para ver y usar su habilidad activa.", ChatFormatting.GRAY, 220);
            }
            entries.add(new LearnedEntry(
                    title,
                    "§e[Agachar + Clic Der.]",
                    subline,
                    0xFFD4AF37,
                    tt
            ));
        }

        // 6. Guardián Espiritual
        if (data.hasChosenGuardian()) {
            String gId = data.getChosenGuardian();
            String gName = switch (gId) {
                case "starry" -> "Starry (Espíritu Celestial)";
                case "chubby" -> "Chubby (Nubecita del Trueno)";
                default -> "Brambi (Venado de Brasa)";
            };
            int vital = data.getGuardianVitalEnergy();
            int maxVital = PlayerRpgData.MAX_GUARDIAN_VITAL_ENERGY;
            List<Component> tt = new ArrayList<>();
            tt.add(Component.literal("§d§l✦ Guardián: §f" + gName));
            tt.add(Component.literal("§7Invocar: §e[" + summonKey + "] §8| §7Coste: §d50 MP"));
            tt.add(Component.literal("§aEnergía Vital: §e" + vital + "§7/" + maxVital));
            wrapLines(tt, "Al combatir con tu guardián invocado, reúne Energía Vital para entregarte Semillas de Vida.", ChatFormatting.GRAY, 220);
            entries.add(new LearnedEntry(
                    "§d✦ Guardián: §f" + gName,
                    "§e[" + summonKey + "]",
                    "§7Coste: §d50 MP §8| §aEnergía Vital: §e" + vital + "§7/" + maxVital,
                    0xFF55A065,
                    tt
            ));
        }

        // 7. Bendición Olímpica
        if (data.hasDivineBlessingUnlocked() && data.getPatronGod() != null) {
            GreekGod god = data.getPatronGod();
            int tier = GreekGod.getBlessingTier(data.getRank());
            String roman = tier == 1 ? "I" : (tier == 2 ? "II" : "III");
            List<Component> tt = new ArrayList<>();
            tt.add(Component.literal(god.getBadge().getString() + " §8— §6§l" + god.getBlessingName() + " §e(Nv." + roman + ")"));
            for (String detail : god.getBlessingDetails(tier)) {
                wrapLines(tt, detail, ChatFormatting.GRAY, 220);
            }
            entries.add(new LearnedEntry(
                    god.getBadge().getString() + " §8— §6" + god.getBlessingName(),
                    "§a[Pasiva Nv." + roman + "]",
                    "§7" + god.getBlessingShortSummary(),
                    0xFFD4AF37,
                    tt
            ));
        }

        return entries;
    }

    private void wrapLines(List<Component> out, String text, ChatFormatting color, int maxWidth) {
        if (text == null || text.isEmpty()) return;
        for (FormattedText ft : this.font.getSplitter().splitLines(text, maxWidth, Style.EMPTY)) {
            out.add(Component.literal(ft.getString()).withStyle(color));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.OPEN_SKILLS_KEY.get().matches(keyCode, scanCode)
                || (this.minecraft != null && this.minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB8080A12);
        RpgUiHelper.drawPanelFrame(graphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT);

        Player player = Minecraft.getInstance().player;
        PlayerRpgData data = (player != null) ? player.getData(ModAttachments.PLAYER_RPG_DATA) : null;

        List<SpiritualTechnique> learnedSpiritual = collectLearnedSpiritual(data);
        List<LearnedEntry> learnedOther = collectLearnedOther(player, data);

        // Sub-cabecera
        graphics.drawString(this.font, "§d§l✦ Técnicas Aprendidas", leftPos + 10, topPos + 30, 0xFFFFFFFF, true);
        String summary = "§7Espirituales: §e" + learnedSpiritual.size() + "/" + SpiritualTechnique.values().length
                + " §8| §7Artes: §b" + learnedOther.size();
        graphics.drawString(this.font, summary, leftPos + GUI_WIDTH - 10 - this.font.width(summary), topPos + 30, 0xFFFFFFFF, false);

        int totalItems = (activeSubTab == 0) ? learnedSpiritual.size() : learnedOther.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        List<Component> hoveredTooltip = null;
        int cardX = leftPos + 10;
        int cardW = GUI_WIDTH - 20;
        int cardH = 34;

        if (activeSubTab == 0) {
            if (learnedSpiritual.isEmpty()) {
                graphics.drawCenteredString(this.font, "§7Aún no has aprendido Técnicas Espirituales.", leftPos + GUI_WIDTH / 2, topPos + 114, 0xFFAAAAAA);
                graphics.drawCenteredString(this.font, "§8Habla con Alice en cualquier aldea para despertar tu Afinidad y aprenderlas.", leftPos + GUI_WIDTH / 2, topPos + 128, 0xFF888888);
            } else {
                String magicKey = ModKeyMappings.MAGIC_MODIFIER_KEY.get().getTranslatedKeyMessage().getString();
                boolean freeMana = player != null && PlayerRpgData.isHoldingReleasedSpiritualSword(player);
                int startIdx = currentPage * PAGE_SIZE;
                int endIdx = Math.min(learnedSpiritual.size(), startIdx + PAGE_SIZE);
                int itemY = topPos + 64;

                for (int i = startIdx; i < endIdx; i++) {
                    SpiritualTechnique tech = learnedSpiritual.get(i);
                    boolean hover = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= itemY && mouseY <= itemY + cardH;

                    int cardBg = hover ? 0xAA172034 : 0x66101524;
                    int outline = hover ? 0xFFE5A93C : 0xFF5D3F88;
                    graphics.fill(cardX, itemY, cardX + cardW, itemY + cardH, cardBg);
                    graphics.renderOutline(cardX, itemY, cardW, cardH, outline);

                    String tierTag = switch (tech.getTier()) {
                        case "Única" -> "§c[Única]";
                        case "Avanzado" -> "§5[Avanz.]";
                        case "Intermedio" -> "§6[Interm.]";
                        default -> "§b[Princ.]";
                    };
                    String rawName = (tech == SpiritualTechnique.ELEMENTAL_BALL && player != null)
                            ? SpiritualTechnique.getDynamicElementalBallName(player)
                            : tech.getDisplayName();
                    String compactName = rawName.replace("Técnica Espiritual Nro. ", "Nº ");
                    String comboStr = "§e[" + magicKey + "] + " + tech.getSequenceDisplay();
                    int comboW = this.font.width(comboStr);

                    String line1Left = tierTag + " §f" + compactName;
                    graphics.drawString(this.font, RpgUiHelper.clip(this.font, line1Left, cardW - comboW - 18), cardX + 6, itemY + 5, 0xFFFFFFFF, false);
                    graphics.drawString(this.font, comboStr, cardX + cardW - 6 - comboW, itemY + 5, 0xFFFFFFFF, false);

                    float cdSec = (data != null && player != null)
                            ? data.getTechniqueCooldownTicks(player, tech) / 20.0f
                            : tech.getCooldownSeconds();
                    String statsStr = (freeMana ? "§a0 MP" : "§b" + (int) tech.getManaCost() + " MP")
                            + " §8| §3" + String.format(Locale.ROOT, "%.1fs", cdSec);
                    int statsW = this.font.width(statsStr);
                    String chantLine = "§d" + tech.getChant();
                    graphics.drawString(this.font, RpgUiHelper.clip(this.font, chantLine, cardW - statsW - 18), cardX + 6, itemY + 19, 0xFFDDDDDD, false);
                    graphics.drawString(this.font, statsStr, cardX + cardW - 6 - statsW, itemY + 19, 0xFFFFFFFF, false);

                    if (hover) {
                        List<Component> tt = new ArrayList<>();
                        tt.add(Component.literal(tech.getColor() + "" + ChatFormatting.BOLD + rawName));
                        tt.add(Component.literal("§7Rango: " + tierTag + " §8| §dCántico: §f" + tech.getChant()));
                        tt.add(Component.literal("§eCombinación: §f[" + magicKey + "] + §e" + tech.getSequenceDisplay()));
                        tt.add(Component.literal("§bCoste: §d" + (int) tech.getManaCost() + " MP"
                                + (freeMana ? " §a(0 MP ahora por Liberación)" : " §8(0 MP con Espada liberada)")
                                + " §8| §3CD: §f" + String.format(Locale.ROOT, "%.1fs", cdSec)));
                        wrapLines(tt, tech.getDescription(), ChatFormatting.GRAY, 220);
                        hoveredTooltip = tt;
                    }

                    itemY += 37;
                }
            }
        } else {
            if (learnedOther.isEmpty()) {
                graphics.drawCenteredString(this.font, "§7Aún no has desbloqueado artes ni habilidades especiales.", leftPos + GUI_WIDTH / 2, topPos + 110, 0xFFAAAAAA);
                graphics.drawCenteredString(this.font, "§8Explora para encontrar a Kaelen (Dash), a Alice (Espada) o duerme tras cazar.", leftPos + GUI_WIDTH / 2, topPos + 124, 0xFF888888);
            } else {
                int startIdx = currentPage * PAGE_SIZE;
                int endIdx = Math.min(learnedOther.size(), startIdx + PAGE_SIZE);
                int itemY = topPos + 64;

                for (int i = startIdx; i < endIdx; i++) {
                    LearnedEntry entry = learnedOther.get(i);
                    boolean hover = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= itemY && mouseY <= itemY + cardH;

                    int cardBg = hover ? 0xAA172034 : 0x66101524;
                    int outline = hover ? 0xFFE5A93C : entry.borderColor();
                    graphics.fill(cardX, itemY, cardX + cardW, itemY + cardH, cardBg);
                    graphics.renderOutline(cardX, itemY, cardW, cardH, outline);

                    int ctrlW = this.font.width(entry.controlBadge());
                    graphics.drawString(this.font, RpgUiHelper.clip(this.font, entry.title(), cardW - ctrlW - 18), cardX + 6, itemY + 5, 0xFFFFFFFF, false);
                    graphics.drawString(this.font, entry.controlBadge(), cardX + cardW - 6 - ctrlW, itemY + 5, 0xFFFFFFFF, false);
                    graphics.drawString(this.font, RpgUiHelper.clip(this.font, entry.subline(), cardW - 12), cardX + 6, itemY + 19, 0xFFBBBBBB, false);

                    if (hover) {
                        hoveredTooltip = entry.tooltip();
                    }

                    itemY += 37;
                }
            }
        }

        // Barra inferior
        String pageLabel = "§7Pág. §f" + (currentPage + 1) + "§7/" + totalPages;
        graphics.drawCenteredString(this.font, pageLabel, leftPos + 63, topPos + GUI_HEIGHT - 19, 0xFFFFFFFF);

        String hint = "§8Pasa el cursor sobre cada técnica para ver detalles";
        int hintX = leftPos + 122;
        int maxHintW = (this.parentScreen != null) ? (GUI_WIDTH - 208) : (GUI_WIDTH - 132);
        graphics.drawString(this.font, RpgUiHelper.clip(this.font, hint, maxHintW), hintX, topPos + GUI_HEIGHT - 19, 0xFF999999, false);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (hoveredTooltip != null && !hoveredTooltip.isEmpty()) {
            graphics.renderComponentTooltip(this.font, hoveredTooltip, mouseX, mouseY);
        }
    }
}
