package com.pablo.rpglevels.utils;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;

public class DoubleJumpHelper {

    /**
     * Checks if the entity is currently flying with an elytra, or has an elytra-capable
     * item equipped in the chest slot or Curios slots.
     */
    public static boolean hasElytraEquippedOrFlying(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        if (entity.isFallFlying()) {
            return true;
        }

        // Check chest slot
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            if (chest.is(Items.ELYTRA) || chest.canElytraFly(entity)) {
                return true;
            }
        }

        // Check Curios slots if present
        try {
            var curiosOpt = CuriosApi.getCuriosInventory(entity);
            if (curiosOpt.isPresent()) {
                var handler = curiosOpt.get();
                var stacks = handler.getEquippedCurios();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStackInSlot(i);
                    if (!stack.isEmpty() && (stack.is(Items.ELYTRA) || stack.canElytraFly(entity))) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Curios not installed or class not found
        }

        return false;
    }
}
