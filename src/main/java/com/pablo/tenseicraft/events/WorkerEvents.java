package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.items.ModItems;
import com.pablo.tenseicraft.items.OverseerCaneItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class WorkerEvents {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.is(ModItems.OVERSEER_CANE.get()) && event.getEntity() instanceof ServerPlayer player) {
            OverseerCaneItem.handleLeftClick(player, held, event.getPos(), event.getLevel().getBlockState(event.getPos()));
            event.setCanceled(true); // Don't break blocks when configuring with the cane
        }
    }
}

