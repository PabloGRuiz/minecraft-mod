package com.pablo.rpglevels.compat.patchouli;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import vazkii.patchouli.api.PatchouliAPI;

public class PatchouliCompat {

    public static final ResourceLocation BOOK_ID = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "rpg_guide");

    public static boolean isLoaded() {
        return ModList.get().isLoaded("patchouli");
    }

    public static ItemStack getGuideBookStack() {
        if (!isLoaded()) {
            return ItemStack.EMPTY;
        }
        try {
            return PatchouliAPI.get().getBookStack(BOOK_ID);
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
    }

    public static void openBook(ServerPlayer player) {
        if (isLoaded() && player != null) {
            try {
                PatchouliAPI.get().openBookGUI(player, BOOK_ID);
            } catch (Throwable ignored) {}
        }
    }
}

