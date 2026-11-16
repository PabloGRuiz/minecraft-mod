package com.pablo.rpglevels.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class AdventurerCardClientHandler {

    public static void openCardScreen(LivingEntity entity, ItemStack cardStack) {
        Minecraft.getInstance().setScreen(new AdventurerCardScreen(entity, cardStack, null));
    }

    public static void openCardFromSnapshot(CompoundTag cardTag) {
        Minecraft.getInstance().setScreen(new AdventurerCardScreen(cardTag, null));
    }
}

