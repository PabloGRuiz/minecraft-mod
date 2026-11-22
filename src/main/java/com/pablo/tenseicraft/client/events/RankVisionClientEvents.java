package com.pablo.tenseicraft.client.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.ThreatLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;

/**
 * Handles rendering of official Adventurer Rank titles or Threat Level brands
 * above players' heads.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class RankVisionClientEvents {

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            event.setContent(Component.empty());
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return;
        }

        Component original = event.getContent();
        if (original == null) {
            original = player.getDisplayName();
        }

        // Case 1: Negative reputation players are branded with their Threat Level!
        if (data.getReputationScore() < 0) {
            ThreatLevel threat = ThreatLevel.getThreatForPlayer(player, data);
            event.setContent(threat.getBadge().append(" ").append(original));
            return;
        }

        // Case 2: Registered guild members display their official Adventurer Rank!
        if (data.isGuildRegistered()) {
            AdventurerRank rank = data.getRank();
            event.setContent(rank.getBadge().append(" ").append(original));
        }
    }
}
