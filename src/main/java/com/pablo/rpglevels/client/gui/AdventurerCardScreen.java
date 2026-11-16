package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.items.AdventurerCardItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.UUID;

public class AdventurerCardScreen extends Screen {

    private final LivingEntity targetEntity;
    private final ItemStack cardStack;
    private final CompoundTag ownerCardTag;
    private final Screen parentScreen;

    private final int GUI_WIDTH = 340;
    private final int GUI_HEIGHT = 225;
    private int leftPos;
    private int topPos;

    public AdventurerCardScreen(LivingEntity entity, ItemStack cardStack, Screen parentScreen) {
        super(Component.literal("Tarjeta de Aventurero"));
        this.targetEntity = entity;
        this.cardStack = cardStack;
        this.ownerCardTag = null;
        this.parentScreen = parentScreen;
    }

    public AdventurerCardScreen(LivingEntity entity, Screen parentScreen) {
        this(entity, ItemStack.EMPTY, parentScreen);
    }

    public AdventurerCardScreen(CompoundTag ownerCardTag, Screen parentScreen) {
        super(Component.literal("Tarjeta de Aventurero"));
        this.targetEntity = null;
        this.cardStack = ItemStack.EMPTY;
        this.ownerCardTag = ownerCardTag;
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Power Control button (visible al ver la propia tarjeta de aventurero)
        LivingEntity target = resolveRenderEntity();
        if (target == Minecraft.getInstance().player) {
            this.addRenderableWidget(Button.builder(Component.literal("§e⚡ Control de Poder"), b -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new PowerControlScreen(this));
                }
            }).bounds(leftPos + GUI_WIDTH - 225, topPos + GUI_HEIGHT - 24, 135, 18).build());
        }

        // Close / Back button
        Component closeText = (this.parentScreen != null) ? Component.literal("§cVolver") : Component.literal("§cCerrar");
        this.addRenderableWidget(Button.builder(closeText, b -> {
            if (this.minecraft != null) {
                if (this.parentScreen != null) {
                    this.minecraft.setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        }).bounds(leftPos + GUI_WIDTH - 85, topPos + GUI_HEIGHT - 24, 75, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Sin shader de desenfoque
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fondo oscurecido
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0080A12);

        // Marco de Tarjeta Oficial (estilo credencial dorada / azul noche)
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF00D111A);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFD4AF37);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0x60D4AF37);

        // Cabecera Oficial del Gremio
        guiGraphics.drawString(font, "§6✦ TARJETA OFICIAL DEL GREMIO DE AVENTUREROS ✦", leftPos + 14, topPos + 9, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§7Orden de Aventureros • Reino de las Llanuras", leftPos + 14, topPos + 20, 0xFFAAAAAA, false);

        // Línea dorada separadora
        guiGraphics.fill(leftPos + 10, topPos + 32, leftPos + GUI_WIDTH - 10, topPos + 33, 0x80D4AF37);

        // 1. Recuadro para la miniatura 3D del Aventurero
        int pX1 = leftPos + 14;
        int pY1 = topPos + 40;
        int pX2 = leftPos + 98;
        int pY2 = topPos + 172;

        guiGraphics.fillGradient(pX1, pY1, pX2, pY2, 0x40000000, 0x70000000);
        guiGraphics.renderOutline(pX1, pY1, pX2 - pX1, pY2 - pY1, 0xFF4A4D60);

        LivingEntity renderEntity = resolveRenderEntity();
        if (renderEntity != null) {
            Component oldName = renderEntity.getCustomName();
            boolean oldVisible = renderEntity.isCustomNameVisible();
            try {
                renderEntity.setCustomName(null);
                renderEntity.setCustomNameVisible(false);
                InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, pX1, pY1, pX2, pY2, 42, 0.0625f, mouseX, mouseY, renderEntity);
            } finally {
                renderEntity.setCustomName(oldName);
                renderEntity.setCustomNameVisible(oldVisible);
            }
        } else if (this.ownerCardTag != null) {
            boolean online = this.ownerCardTag.getBoolean(AdventurerCardItem.TAG_IS_ONLINE);
            int centerX = (pX1 + pX2) / 2;
            int centerY = (pY1 + pY2) / 2;
            guiGraphics.drawCenteredString(font, "§6✦", centerX, centerY - 20, 0xFFFFFFFF);
            guiGraphics.drawCenteredString(font, "§7Sin modelo", centerX, centerY - 4, 0xFFAAAAAA);
            guiGraphics.drawCenteredString(font, online ? "§8(Fuera de rango)" : "§8(Desconectado)", centerX, centerY + 8, 0xFF888888);
        }

        // Subtexto bajo la miniatura
        if (this.ownerCardTag != null) {
            if (this.ownerCardTag.contains(AdventurerCardItem.TAG_XP_LEVEL)) {
                guiGraphics.drawCenteredString(font, "§eNivel XP: §f" + this.ownerCardTag.getInt(AdventurerCardItem.TAG_XP_LEVEL), (pX1 + pX2) / 2, pY2 + 5, 0xFFFFFFFF);
            }
        } else if (renderEntity instanceof Player p) {
            guiGraphics.drawCenteredString(font, "§eNivel XP: §f" + p.experienceLevel, (pX1 + pX2) / 2, pY2 + 5, 0xFFFFFFFF);
        } else if (renderEntity instanceof AdventurerNpcEntity npc) {
            guiGraphics.drawCenteredString(font, "§eNivel: §f" + npc.getAdventurerLevel(), (pX1 + pX2) / 2, pY2 + 5, 0xFFFFFFFF);
        }

        // 2. Columna de Datos y Estadísticas
        int infoX = leftPos + 110;
        int curY = topPos + 40;

        if (this.ownerCardTag != null) {
            renderPlayerSnapshotDetails(guiGraphics, this.ownerCardTag, infoX, curY);
        } else if (renderEntity instanceof Player player) {
            renderPlayerDetails(guiGraphics, player, infoX, curY);
        } else if (renderEntity instanceof AdventurerNpcEntity npc) {
            renderNpcDetails(guiGraphics, npc, infoX, curY);
        } else {
            // Tarjeta genérica con datos del stack
            String ownerName = !cardStack.isEmpty() ? AdventurerCardItem.getOwnerName(cardStack) : "Aventurero";
            String rankName = !cardStack.isEmpty() ? AdventurerCardItem.getRankName(cardStack) : "F";

            guiGraphics.drawString(font, "§eTitular: §f" + ownerName, infoX, curY, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, "§6Rango Registrado: §eRango " + rankName, infoX, curY + 14, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, "§7Credencial válida expedida por el Gremio.", infoX, curY + 28, 0xFFAAAAAA, false);
        }

        // 3. Pie de Tarjeta / Sello Oficial
        guiGraphics.fill(leftPos + 10, topPos + GUI_HEIGHT - 32, leftPos + GUI_WIDTH - 10, topPos + GUI_HEIGHT - 31, 0x40D4AF37);
        guiGraphics.drawString(font, "§8Expedido por: §6Maestro del Gremio §7| Sello Real: §a✔ Válido", leftPos + 14, topPos + GUI_HEIGHT - 20, 0xFFAAAAAA, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private LivingEntity resolveRenderEntity() {
        if (this.ownerCardTag != null) {
            if (this.ownerCardTag.contains(AdventurerCardItem.TAG_OWNER_UUID) && Minecraft.getInstance().level != null) {
                try {
                    UUID ownerUuid = UUID.fromString(this.ownerCardTag.getString(AdventurerCardItem.TAG_OWNER_UUID));
                    return Minecraft.getInstance().level.getPlayerByUUID(ownerUuid);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        }
        return this.targetEntity != null ? this.targetEntity : Minecraft.getInstance().player;
    }

    private void renderPlayerSnapshotDetails(GuiGraphics guiGraphics, CompoundTag tag, int x, int y) {
        String ownerName = tag.contains(AdventurerCardItem.TAG_OWNER_NAME)
                ? tag.getString(AdventurerCardItem.TAG_OWNER_NAME)
                : "Aventurero Anónimo";
        boolean hasSnapshot = tag.getBoolean(AdventurerCardItem.TAG_HAS_SNAPSHOT);
        boolean online = tag.getBoolean(AdventurerCardItem.TAG_IS_ONLINE);

        String statusSuffix = (hasSnapshot && !online) ? " §7(Offline)" : "";
        guiGraphics.drawString(font, "§eTitular: §f" + ownerName + statusSuffix, x, y, 0xFFFFFFFF, false);

        AdventurerRank rank = tag.contains(AdventurerCardItem.TAG_RANK_INDEX)
                ? AdventurerRank.fromIndex(tag.getInt(AdventurerCardItem.TAG_RANK_INDEX))
                : resolveRankByName(tag.getString(AdventurerCardItem.TAG_RANK));
        guiGraphics.drawString(font, "§6Rango Oficial: ", x, y + 13, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, rank.getBadge(), x + 72, y + 13, 0xFFFFFFFF, false);

        if (!hasSnapshot) {
            guiGraphics.drawString(font, "§7Ficha registrada antes de la sincronización.", x, y + 32, 0xFFAAAAAA, false);
            guiGraphics.drawString(font, "§7Los datos completos se actualizarán al", x, y + 44, 0xFFAAAAAA, false);
            guiGraphics.drawString(font, "§7interactuar mientras el titular esté activo.", x, y + 56, 0xFFAAAAAA, false);
            return;
        }

        int repScore = tag.getInt(AdventurerCardItem.TAG_REP_SCORE);
        String repScoreStr = (repScore > 0 ? "+" : "") + repScore;
        String repDisplay = tag.getString(AdventurerCardItem.TAG_REP_DISPLAY);
        guiGraphics.drawString(font, "§eMoral: " + repDisplay + " §7(" + repScoreStr + "/100)", x, y + 26, 0xFFFFFFFF, false);

        guiGraphics.fill(x, y + 38, leftPos + GUI_WIDTH - 14, y + 39, 0x30FFFFFF);

        int statY = y + 43;
        int fortitude = tag.getInt(AdventurerCardItem.TAG_FORTITUDE);
        int dexterity = tag.getInt(AdventurerCardItem.TAG_DEXTERITY);
        int spirit = tag.getInt(AdventurerCardItem.TAG_SPIRIT);
        int luck = tag.getInt(AdventurerCardItem.TAG_LUCK);
        int maxMana = tag.getInt(AdventurerCardItem.TAG_MAX_MANA);
        float auraStat = Math.max(1.0f, (fortitude + dexterity + spirit + luck) / 4.0f);

        guiGraphics.drawString(font, "§c🛡 Fortaleza: §f" + fortitude + " pts §7(+" + Math.round(fortitude * 1.5) + " HP)", x, statY, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§b🎯 Destreza: §f" + dexterity + " pts §7(+" + String.format(Locale.US, "%.1f", dexterity * 1.2) + "% Vel)", x, statY + 12, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§d✨ Espíritu: §f" + spirit + " pts §7(+" + maxMana + " PM)", x, statY + 24, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§a🍀 Suerte: §f" + luck + " pts §7(+" + (luck * 2) + "% Crít)", x, statY + 36, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§5🌀 Aura Espiritual: §f" + String.format(Locale.US, "%.1f", auraStat) + " pts §7(Poder del Alma)", x, statY + 48, 0xFFFFFFFF, false);

        int hp = tag.getInt(AdventurerCardItem.TAG_HEALTH);
        int maxHp = tag.getInt(AdventurerCardItem.TAG_MAX_HEALTH);
        int mana = tag.getInt(AdventurerCardItem.TAG_MANA);

        guiGraphics.drawString(font, "§c❤ Salud: §f" + hp + "/" + maxHp + " HP   §b🌀 Maná: §f" + mana + "/" + maxMana + " MP", x, statY + 62, 0xFFFFFFFF, false);
    }

    private AdventurerRank resolveRankByName(String rankName) {
        if (rankName == null || rankName.isEmpty()) return AdventurerRank.F;
        for (AdventurerRank r : AdventurerRank.values()) {
            if (r.getName().equalsIgnoreCase(rankName)) return r;
        }
        return AdventurerRank.F;
    }

    private void renderPlayerDetails(GuiGraphics guiGraphics, Player player, int x, int y) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        AdventurerRank rank = data.getRank();
        String playerName = player.getName().getString();

        guiGraphics.drawString(font, "§eTitular: §f" + playerName, x, y, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§6Rango Oficial: ", x, y + 13, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, rank.getBadge(), x + 72, y + 13, 0xFFFFFFFF, false);
        if (data.hasReceivedSpiritualSword()) {
            guiGraphics.drawString(font, " " + data.getSpiritualTier().getBadge(data.isSpiritualEngineer()).getString(), x + 72 + font.width(rank.getBadge()), y + 13, 0xFFFFFFFF, false);
        }

        String repScoreStr = (data.getReputationScore() > 0 ? "+" : "") + data.getReputationScore();
        guiGraphics.drawString(font, "§eMoral: " + data.getReputationTier().getDisplayName().getString() + " §7(" + repScoreStr + "/100)", x, y + 26, 0xFFFFFFFF, false);

        // Separador de atributos
        guiGraphics.fill(x, y + 38, leftPos + GUI_WIDTH - 14, y + 39, 0x30FFFFFF);

        int statY = y + 43;
        int fortitude = data.getTotalStatPoints(player, StatType.FORTITUDE);
        int dexterity = data.getTotalStatPoints(player, StatType.DEXTERITY);
        int spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
        int luck = data.getTotalStatPoints(player, StatType.LUCK);
        float auraStat = data.getAuraStat(player);

        guiGraphics.drawString(font, "§c🛡 Fortaleza: §f" + fortitude + " pts §7(+" + Math.round(fortitude * 1.5) + " HP)", x, statY, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§b🎯 Destreza: §f" + dexterity + " pts §7(+" + String.format(Locale.US, "%.1f", dexterity * 1.2) + "% Vel)", x, statY + 12, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§d✨ Espíritu: §f" + spirit + " pts §7(+" + (int)data.getMaxMana(player) + " PM)", x, statY + 24, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§a🍀 Suerte: §f" + luck + " pts §7(+" + (luck * 2) + "% Crít)", x, statY + 36, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§5🌀 Aura Espiritual: §f" + String.format(Locale.US, "%.1f", auraStat) + " pts §7(Poder del Alma)", x, statY + 48, 0xFFFFFFFF, false);

        // Salud y Maná
        int hp = (int) player.getHealth();
        int maxHp = (int) player.getMaxHealth();
        int mana = (int) data.getCurrentMana(player);
        int maxMana = (int) data.getMaxMana(player);
        guiGraphics.drawString(font, "§c❤ Salud: §f" + hp + "/" + maxHp + " HP   §b🌀 Maná: §f" + mana + "/" + maxMana + " MP", x, statY + 60, 0xFFFFFFFF, false);

        String loc = data.getCurrentLocationName();
        if (loc == null || loc.isEmpty()) {
            loc = "Tierras del Reino";
        }
        guiGraphics.drawString(font, "§7📍 Ubicación: §f" + loc, x, statY + 73, 0xFFFFFFFF, false);
    }

    private void renderNpcDetails(GuiGraphics guiGraphics, AdventurerNpcEntity npc, int x, int y) {
        AdventurerRank rank = npc.getRank();
        String npcName = npc.hasCustomName() ? npc.getCustomName().getString() : "Aventurero del Gremio";

        guiGraphics.drawString(font, "§eTitular: §f" + npcName, x, y, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§6Rango Oficial: ", x, y + 12, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, rank.getBadge(), x + 72, y + 12, 0xFFFFFFFF, false);

        int level = npc.getAdventurerLevel();
        int xp = npc.getAdventurerXp();
        int reqXp = npc.getRequiredXpForNextLevel();
        boolean ready = npc.isReadyToAscend();

        // Nivel y barra de XP
        guiGraphics.drawString(font, "§eNivel: §f" + level + " §7| §aXP: §f" + xp + "/" + reqXp, x, y + 24, 0xFFFFFFFF, false);
        int barW = 120;
        int barH = 3;
        float xpRatio = Math.min(1.0f, (float) xp / reqXp);
        guiGraphics.fill(x, y + 34, x + barW, y + 34 + barH, 0xFF222635);
        guiGraphics.fill(x, y + 34, x + (int) (barW * xpRatio), y + 34 + barH, 0xFF55FF55);

        int nextY = y + 40;
        if (ready) {
            guiGraphics.drawString(font, "§6⚡ ¡Listo para ascender al Rango " + rank.getNextRank().getName() + "!", x, nextY, 0xFFFFAA00, false);
            guiGraphics.drawString(font, "§7Acude al Maestro del Gremio.", x, nextY + 9, 0xFFAAAAAA, false);
            nextY += 20;
        } else {
            Player clientPlayer = Minecraft.getInstance().player;
            int affinity = (clientPlayer != null) ? npc.getAffinity(clientPlayer) : 0;
            String statusStr = npc.isEternalComrade()
                    ? "§6Camarada Eterno (Gratis)"
                    : (npc.isHired() ? "§aContratado" : "§eDisponible");
            guiGraphics.drawString(font, "§dEstado: " + statusStr + " §7| Afinidad: §e" + affinity + "/100", x, nextY, 0xFFFFFFFF, false);
            nextY += 12;
        }

        // Separador
        guiGraphics.fill(x, nextY, leftPos + GUI_WIDTH - 14, nextY + 1, 0x30FFFFFF);

        int statY = nextY + 5;
        int hp = (int) npc.getHealth();
        int maxHp = (int) npc.getMaxHealth();
        double armor = npc.getArmorValue();
        double dmg = npc.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed = npc.getAttributeValue(Attributes.MOVEMENT_SPEED);
        String combatRole = (npc.getMainHandItem().getItem() instanceof BowItem) ? "Arquero" : "Duelista";

        guiGraphics.drawString(font, "§c❤ Salud: §f" + hp + " / " + maxHp + " HP", x, statY, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§b🛡 Armadura: §f+" + String.format(Locale.ROOT, "%.1f", armor), x, statY + 11, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§6⚔ Daño: §f+" + String.format(Locale.ROOT, "%.1f", dmg), x, statY + 22, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§e⚡ Velocidad: §f" + String.format(Locale.ROOT, "%.2f", speed), x, statY + 33, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§7Rol: §f" + combatRole, x, statY + 44, 0xFFAAAAAA, false);
    }
}

