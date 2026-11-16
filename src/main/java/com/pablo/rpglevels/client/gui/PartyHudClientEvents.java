package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.party.ClientPartyData;
import com.pablo.rpglevels.party.PartyMemberInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class PartyHudClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }

        while (ModKeyMappings.PARTY_KEY.get().consumeClick()) {
            mc.setScreen(new PartyScreen());
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        if (!ClientPartyData.isInParty()) {
            return;
        }

        List<PartyMemberInfo> members = ClientPartyData.getMembers();
        if (members.isEmpty()) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;

        float hudScale = 0.80f;
        if (mc.getWindow().getGuiScaledHeight() < 360 || mc.getWindow().getGuiScaledWidth() < 640) {
            hudScale = 0.65f;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(hudScale, hudScale, 1.0f);

        int startX = 10;
        int startY = 44;

        // Título del grupo con rango promedio
        com.pablo.rpglevels.data.AdventurerRank pRank = ClientPartyData.getPartyRank();
        String leaderTitle = "§6✦ " + ClientPartyData.getLeaderName() + " " + pRank.getBadge().getString() + " §8(" + members.size() + "/4)";
        int textW = font.width(leaderTitle);
        int frameWidth = Math.max(145, textW + 8);

        guiGraphics.fill(startX, startY - 14, startX + frameWidth, startY - 2, 0xEE0E101A);
        guiGraphics.renderOutline(startX, startY - 14, frameWidth, 12, 0xFFE5A93C);
        guiGraphics.drawString(font, Component.literal(leaderTitle), startX + 4, startY - 12, 0xFFFFAA00, false);

        int currentY = startY;

        for (PartyMemberInfo info : members) {
            boolean isSelf = info.uuid().equals(mc.player.getUUID());

            float curHp = isSelf ? mc.player.getHealth() : info.health();
            float maxHp = isSelf ? mc.player.getMaxHealth() : info.maxHealth();

            float curMana;
            float maxMana;
            if (isSelf) {
                com.pablo.rpglevels.data.PlayerRpgData myData = mc.player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
                curMana = (myData != null) ? myData.getCurrentMana(mc.player) : 0.0f;
                maxMana = (myData != null) ? myData.getMaxMana(mc.player) : 0.0f;
            } else {
                curMana = info.mana();
                maxMana = info.maxMana();
            }

            boolean hasMana = maxMana > 0.0f;
            int boxH = hasMana ? 33 : 20;

            // Fondo de miembro
            guiGraphics.fill(startX, currentY, startX + frameWidth, currentY + boxH, 0xEE141724);
            guiGraphics.renderOutline(startX, currentY, frameWidth, boxH, isSelf ? 0xFF5A78A5 : 0xFF363B50);

            // Nombre y Rol
            String namePrefix = info.isNpc() ? "§b[NPC] " : (info.roleOrRank().contains("Líder") ? "§6★ " : (isSelf ? "§e● " : "§f"));
            String displayName = namePrefix + info.name();

            String hpText = String.format("§a%d/%d", (int) Math.ceil(curHp), (int) Math.ceil(maxHp));
            int hpTextW = font.width(hpText);

            int maxNameW = frameWidth - 8 - hpTextW;
            if (font.width(displayName) > maxNameW) {
                displayName = font.plainSubstrByWidth(displayName, maxNameW - 6) + "..";
            }
            guiGraphics.drawString(font, Component.literal(displayName), startX + 4, currentY + 2, 0xFFFFFFFF, false);
            guiGraphics.drawString(font, Component.literal(hpText), startX + frameWidth - 4 - hpTextW, currentY + 2, 0xFFAAAAAA, false);

            // Barra de vida
            int barX = startX + 4;
            int barY = currentY + 12;
            int barW = frameWidth - 8;
            int barH = 4;

            float pct = maxHp > 0.0f ? Math.max(0.0f, Math.min(1.0f, curHp / maxHp)) : 0.0f;
            int fillW = (int) (barW * pct);
            int barColor = (pct > 0.5f) ? 0xFF2ECC71 : (pct > 0.25f ? 0xFFF1C40F : 0xFFE74C3C);

            guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF180A0A);
            if (fillW > 0) {
                guiGraphics.fill(barX, barY, barX + fillW, barY + barH, barColor);
            }

            // Barra de maná (si aplica)
            if (hasMana) {
                int manaBarY = currentY + 18;
                int manaBarH = 3;
                float manaPct = Math.max(0.0f, Math.min(1.0f, curMana / maxMana));
                int manaFillW = (int) (barW * manaPct);

                guiGraphics.fill(barX, manaBarY, barX + barW, manaBarY + manaBarH, 0xFF081420);
                if (manaFillW > 0) {
                    guiGraphics.fill(barX, manaBarY, barX + manaFillW, manaBarY + manaBarH, 0xFF2B88E8);
                }

                String mpText = String.format("§b%d/%d MP", (int) Math.ceil(curMana), (int) Math.ceil(maxMana));
                guiGraphics.drawString(font, Component.literal(mpText), startX + frameWidth - 4 - font.width(mpText), currentY + 22, 0xFF70C0FF, false);
            }

            currentY += boxH + 2;
        }

        guiGraphics.pose().popPose();
    }
}
