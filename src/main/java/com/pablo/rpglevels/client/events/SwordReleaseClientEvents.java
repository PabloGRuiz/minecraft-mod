package com.pablo.rpglevels.client.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import com.pablo.rpglevels.network.ToggleSwordReleasePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class SwordReleaseClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }

        while (ModKeyMappings.RELEASE_SWORD_KEY.get().consumeClick()) {
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof SpiritualSwordItem)) {
                held = player.getOffhandItem();
            }

            if (!(held.getItem() instanceof SpiritualSwordItem)) {
                player.displayClientMessage(Component.literal("§7Debes empuñar tu Espada Espiritual para canalizar su liberación."), true);
                continue;
            }

            PacketDistributor.sendToServer(new ToggleSwordReleasePayload(true));
        }
    }
}

