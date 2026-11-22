package com.pablo.tenseicraft.client.gui;

import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.GreekGod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import com.pablo.tenseicraft.items.AdventurerCardItem;
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

    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 236;
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

        LivingEntity target = resolveRenderEntity();
        if (target == Minecraft.getInstance().player) {
            this.addRenderableWidget(Button.builder(Component.literal("§e⚡ Control de Poder"), b -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new PowerControlScreen(this));
                }
            }).bounds(leftPos + GUI_WIDTH - 235, topPos + GUI_HEIGHT - 25, 138, 18).build());
        }

        Component closeText = (this.parentScreen != null) ? Component.literal("§cVolver") : Component.literal("§cCerrar");
        this.addRenderableWidget(Button.builder(closeText, b -> {
            if (this.minecraft != null) {
                if (this.parentScreen != null) {
                    this.minecraft.setScreen(this.parentScreen);
                } else {
                    this.onClose();
                }
            }
        }).bounds(leftPos + GUI_WIDTH - 90, topPos + GUI_HEIGHT - 25, 78, 18).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB8080A12);

        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF20D111A);
        guiGraphics.renderOutline(leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFD4AF37);
        guiGraphics.renderOutline(leftPos + 2, topPos + 2, GUI_WIDTH - 4, GUI_HEIGHT - 4, 0x60D4AF37);

        guiGraphics.drawString(font, "§6✦ TARJETA OFICIAL DEL GREMIO DE AVENTUREROS ✦", leftPos + 14, topPos + 9, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§7Orden de Aventureros • Registro Oficial", leftPos + 14, topPos + 20, 0xFFAAAAAA, false);
        guiGraphics.fill(leftPos + 10, topPos + 32, leftPos + GUI_WIDTH - 10, topPos + 33, 0x80D4AF37);

        int pX1 = leftPos + 14;
        int pY1 = topPos + 40;
        int pX2 = leftPos + 104;
        int pY2 = topPos + 174;

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

        if (this.ownerCardTag != null) {
            if (this.ownerCardTag.contains(AdventurerCardItem.TAG_XP_LEVEL)) {
                guiGraphics.drawCenteredString(font, "§eNivel RPG: §f" + this.ownerCardTag.getInt(AdventurerCardItem.TAG_XP_LEVEL), (pX1 + pX2) / 2, pY2 + 6, 0xFFFFFFFF);
            }
        } else if (renderEntity instanceof Player p) {
            PlayerRpgData pData = p.getData(ModAttachments.PLAYER_RPG_DATA);
            int rpgLvl = (pData != null) ? pData.getRpgLevel() : 1;
            guiGraphics.drawCenteredString(font, "§eNivel RPG: §f" + rpgLvl, (pX1 + pX2) / 2, pY2 + 6, 0xFFFFFFFF);
        } else if (renderEntity instanceof AdventurerNpcEntity npc) {
            guiGraphics.drawCenteredString(font, "§eNivel: §f" + npc.getAdventurerLevel(), (pX1 + pX2) / 2, pY2 + 6, 0xFFFFFFFF);
        }

        int infoX = leftPos + 116;
        int curY = topPos + 40;
        int maxInfoW = leftPos + GUI_WIDTH - 14 - infoX;

        if (this.ownerCardTag != null) {
            renderPlayerSnapshotDetails(guiGraphics, this.ownerCardTag, infoX, curY, maxInfoW);
        } else if (renderEntity instanceof Player player) {
            renderPlayerDetails(guiGraphics, player, infoX, curY, maxInfoW);
        } else if (renderEntity instanceof AdventurerNpcEntity npc) {
            renderNpcDetails(guiGraphics, npc, infoX, curY, maxInfoW);
        } else {
            String ownerName = !cardStack.isEmpty() ? AdventurerCardItem.getOwnerName(cardStack) : "Aventurero";
            String rankName = !cardStack.isEmpty() ? AdventurerCardItem.getRankName(cardStack) : "F";
            guiGraphics.drawString(font, RpgUiHelper.clip(font, "§eTitular: §f" + ownerName, maxInfoW), infoX, curY, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, RpgUiHelper.clip(font, "§6Rango Registrado: §eRango " + rankName, maxInfoW), infoX, curY + 14, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, "§7Credencial válida expedida por el Gremio.", infoX, curY + 28, 0xFFAAAAAA, false);
        }

        guiGraphics.fill(leftPos + 10, topPos + GUI_HEIGHT - 33, leftPos + GUI_WIDTH - 10, topPos + GUI_HEIGHT - 32, 0x40D4AF37);
        guiGraphics.drawString(font, "§8Expedido: §6Maestro del Gremio §7| §a✔ Válido", leftPos + 14, topPos + GUI_HEIGHT - 20, 0xFFAAAAAA, false);

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

    private void renderPlayerSnapshotDetails(GuiGraphics guiGraphics, CompoundTag tag, int x, int y, int maxW) {
        String ownerName = tag.contains(AdventurerCardItem.TAG_OWNER_NAME)
                ? tag.getString(AdventurerCardItem.TAG_OWNER_NAME)
                : "Aventurero Anónimo";
        boolean hasSnapshot = tag.getBoolean(AdventurerCardItem.TAG_HAS_SNAPSHOT);
        boolean online = tag.getBoolean(AdventurerCardItem.TAG_IS_ONLINE);

        String statusSuffix = (hasSnapshot && !online) ? " §7(Offline)" : "";
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§eTitular: §f" + ownerName + statusSuffix, maxW), x, y, 0xFFFFFFFF, false);

        AdventurerRank rank = tag.contains(AdventurerCardItem.TAG_RANK_INDEX)
                ? AdventurerRank.fromIndex(tag.getInt(AdventurerCardItem.TAG_RANK_INDEX))
                : resolveRankByName(tag.getString(AdventurerCardItem.TAG_RANK));
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§6Rango: " + rank.getBadge().getString(), maxW), x, y + 13, 0xFFFFFFFF, false);

        if (!hasSnapshot) {
            guiGraphics.drawString(font, "§7Ficha registrada antes de la sincronización.", x, y + 32, 0xFFAAAAAA, false);
            return;
        }

        int repScore = tag.getInt(AdventurerCardItem.TAG_REP_SCORE);
        String repScoreStr = (repScore > 0 ? "+" : "") + repScore;
        String repDisplay = tag.getString(AdventurerCardItem.TAG_REP_DISPLAY);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§eMoral: " + repDisplay + " §7(" + repScoreStr + "/100)", maxW), x, y + 26, 0xFFFFFFFF, false);

        guiGraphics.fill(x, y + 38, leftPos + GUI_WIDTH - 14, y + 39, 0x30FFFFFF);

        int statY = y + 44;
        int fortitude = tag.getInt(AdventurerCardItem.TAG_FORTITUDE);
        int dexterity = tag.getInt(AdventurerCardItem.TAG_DEXTERITY);
        int spirit = tag.getInt(AdventurerCardItem.TAG_SPIRIT);
        int luck = tag.getInt(AdventurerCardItem.TAG_LUCK);
        int maxMana = tag.getInt(AdventurerCardItem.TAG_MAX_MANA);
        float auraStat = Math.max(1.0f, (fortitude + dexterity + spirit + luck) / 4.0f);

        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§c🛡 Fortaleza: §f" + fortitude + " pts §7(+" + Math.round(fortitude * 1.5) + " HP)", maxW), x, statY, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§b🎯 Destreza: §f" + dexterity + " pts §7(+" + String.format(Locale.US, "%.1f", dexterity * 1.2) + "% Vel)", maxW), x, statY + 13, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§d✨ Espíritu: §f" + spirit + " pts §7(+" + maxMana + " PM)", maxW), x, statY + 26, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§a🍀 Suerte: §f" + luck + " pts §7(+" + (luck * 2) + "% Crít)", maxW), x, statY + 39, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§5🌀 Aura Espiritual: §f" + String.format(Locale.US, "%.1f", auraStat) + " pts", maxW), x, statY + 52, 0xFFFFFFFF, false);

        int hp = tag.getInt(AdventurerCardItem.TAG_HEALTH);
        int maxHp = tag.getInt(AdventurerCardItem.TAG_MAX_HEALTH);
        int mana = tag.getInt(AdventurerCardItem.TAG_MANA);

        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§c❤ Salud: §f" + hp + "/" + maxHp + " HP   §b🌀 Maná: §f" + mana + "/" + maxMana + " MP", maxW), x, statY + 67, 0xFFFFFFFF, false);
    }

    private AdventurerRank resolveRankByName(String rankName) {
        if (rankName == null || rankName.isEmpty()) return AdventurerRank.F;
        for (AdventurerRank r : AdventurerRank.values()) {
            if (r.getName().equalsIgnoreCase(rankName)) return r;
        }
        return AdventurerRank.F;
    }

    private void renderPlayerDetails(GuiGraphics guiGraphics, Player player, int x, int y, int maxW) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        AdventurerRank rank = data.getRank();
        String playerName = player.getName().getString();
        GreekGod god = data.hasDivineBlessingUnlocked() ? data.getPatronGod() : null;

        String rankLine = "§6Rango: " + rank.getBadge().getString()
                + (data.hasReceivedSpiritualSword() ? " " + data.getSpiritualTier().getBadge(data.isSpiritualEngineer()).getString() : "");
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§eTitular: §f" + playerName, maxW), x, y, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, rankLine, maxW), x, y + 12, 0xFFFFFFFF, false);

        String repScoreStr = (data.getReputationScore() > 0 ? "+" : "") + data.getReputationScore();
        String moralLine = "§eMoral: " + data.getReputationTier().getDisplayName().getString() + " §7(" + repScoreStr + ")";
        String godSuffix = (god != null) ? (" §8| " + god.getBadge().getString()) : "";
        guiGraphics.drawString(font, RpgUiHelper.clip(font, moralLine + godSuffix, maxW), x, y + 24, 0xFFFFFFFF, false);

        guiGraphics.fill(x, y + 36, leftPos + GUI_WIDTH - 14, y + 37, 0x30FFFFFF);

        int statY = y + 42;
        int fortitude = data.getTotalStatPoints(player, StatType.FORTITUDE);
        int dexterity = data.getTotalStatPoints(player, StatType.DEXTERITY);
        int spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
        int luck = data.getTotalStatPoints(player, StatType.LUCK);
        float auraStat = data.getAuraStat(player);

        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§c🛡 Fortaleza: §f" + fortitude + " pts §7(+" + Math.round(fortitude * 1.5) + " HP)", maxW), x, statY, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§b🎯 Destreza: §f" + dexterity + " pts §7(+" + String.format(Locale.US, "%.1f", dexterity * 1.2) + "% Vel)", maxW), x, statY + 12, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§d✨ Espíritu: §f" + spirit + " pts §7(+" + (int) data.getMaxMana(player) + " PM)", maxW), x, statY + 24, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§a🍀 Suerte: §f" + luck + " pts §7(+" + (luck * 2) + "% Crít)", maxW), x, statY + 36, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§5🌀 Aura Espiritual: §f" + String.format(Locale.US, "%.1f", auraStat) + " pts", maxW), x, statY + 48, 0xFFFFFFFF, false);

        int hp = (int) player.getHealth();
        int maxHp = (int) player.getMaxHealth();
        int mana = (int) data.getCurrentMana(player);
        int maxMana = (int) data.getMaxMana(player);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§c❤ Salud: §f" + hp + "/" + maxHp + " HP   §b🌀 Maná: §f" + mana + "/" + maxMana + " MP", maxW), x, statY + 62, 0xFFFFFFFF, false);

        String loc = data.getCurrentLocationName();
        if (loc == null || loc.isEmpty()) {
            loc = "Tierras del Reino";
        }
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§7📍 Ubicación: §f" + loc, maxW), x, statY + 75, 0xFFFFFFFF, false);
    }

    private void renderNpcDetails(GuiGraphics guiGraphics, AdventurerNpcEntity npc, int x, int y, int maxW) {
        AdventurerRank rank = npc.getRank();
        String npcName = npc.hasCustomName() ? npc.getCustomName().getString() : "Aventurero del Gremio";

        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§eTitular: §f" + npcName, maxW), x, y, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, RpgUiHelper.clip(font, "§6Rango Oficial: " + rank.getBadge().getString(), maxW), x, y + 12, 0xFFFFFFFF, false);

        int level = npc.getAdventurerLevel();
        int xp = npc.getAdventurerXp();
        int reqXp = npc.getRequiredXpForNextLevel();
        boolean ready = npc.isReadyToAscend();

        guiGraphics.drawString(font, "§eNivel: §f" + level + " §7| §aXP: §f" + xp + "/" + reqXp, x, y + 24, 0xFFFFFFFF, false);
        int barW = Math.min(160, maxW);
        int barH = 3;
        float xpRatio = Math.min(1.0f, (float) xp / Math.max(1, reqXp));
        guiGraphics.fill(x, y + 34, x + barW, y + 34 + barH, 0xFF222635);
        guiGraphics.fill(x, y + 34, x + (int) (barW * xpRatio), y + 34 + barH, 0xFF55FF55);

        int nextY = y + 41;
        if (ready) {
            guiGraphics.drawString(font, RpgUiHelper.clip(font, "§6⚡ ¡Listo para ascender a Rango " + rank.getNextRank().getName() + "!", maxW), x, nextY, 0xFFFFAA00, false);
            guiGraphics.drawString(font, "§7Acude al Maestro del Gremio.", x, nextY + 10, 0xFFAAAAAA, false);
            nextY += 22;
        } else {
            Player clientPlayer = Minecraft.getInstance().player;
            int affinity = (clientPlayer != null) ? npc.getAffinity(clientPlayer) : 0;
            String statusStr = npc.isEternalComrade()
                    ? "§6Camarada Eterno"
                    : (npc.isHired() ? "§aContratado" : "§eDisponible");
            guiGraphics.drawString(font, RpgUiHelper.clip(font, "§dEstado: " + statusStr + " §7| Afinidad: §e" + affinity + "/100", maxW), x, nextY, 0xFFFFFFFF, false);
            nextY += 13;
        }

        guiGraphics.fill(x, nextY, leftPos + GUI_WIDTH - 14, nextY + 1, 0x30FFFFFF);

        int statY = nextY + 6;
        int hp = (int) npc.getHealth();
        int maxHp = (int) npc.getMaxHealth();
        double armor = npc.getArmorValue();
        double dmg = npc.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double speed = npc.getAttributeValue(Attributes.MOVEMENT_SPEED);
        String combatRole = (npc.getMainHandItem().getItem() instanceof BowItem) ? "Arquero" : "Duelista";

        guiGraphics.drawString(font, "§c❤ Salud: §f" + hp + " / " + maxHp + " HP", x, statY, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§b🛡 Armadura: §f+" + String.format(Locale.ROOT, "%.1f", armor), x, statY + 12, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§6⚔ Daño: §f+" + String.format(Locale.ROOT, "%.1f", dmg), x, statY + 24, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§e⚡ Velocidad: §f" + String.format(Locale.ROOT, "%.2f", speed), x, statY + 36, 0xFFFFFFFF, false);
        guiGraphics.drawString(font, "§7Rol: §f" + combatRole, x, statY + 48, 0xFFAAAAAA, false);
    }
}
