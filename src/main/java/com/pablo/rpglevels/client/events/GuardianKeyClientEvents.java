package com.pablo.rpglevels.client.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.ModKeyMappings;
import com.pablo.rpglevels.network.SummonGuardianPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class GuardianKeyClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null) {
            return;
        }

        while (ModKeyMappings.SUMMON_GUARDIAN_KEY.get().consumeClick()) {
            PacketDistributor.sendToServer(new SummonGuardianPayload());
        }
    }
}
