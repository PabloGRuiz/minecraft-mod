package com.pablo.tenseicraft.compat;

import com.pablo.tenseicraft.items.WarriorRingItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

public class BetterCombatCompat {

    private static Boolean betterCombatLoaded = null;

    public static boolean isBetterCombatLoaded() {
        if (betterCombatLoaded == null) {
            betterCombatLoaded = ModList.get().isLoaded("bettercombat") || ModList.get().isLoaded("better_combat");
        }
        return betterCombatLoaded;
    }

    public static boolean hasWarriorRingEquipped(Player player) {
        if (player == null) return false;

        var curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isPresent()) {
            var found = curiosOpt.get().findFirstCurio(stack ->
                    stack.getItem() instanceof WarriorRingItem && WarriorRingItem.getTier(stack) >= 1
            );
            return found.isPresent();
        }
        return false;
    }
}
