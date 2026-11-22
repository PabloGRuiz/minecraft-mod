package com.pablo.tenseicraft.client;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.compat.BetterCombatCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class AutoAttackClientEvents {

    private static boolean wasAttackKeyDown = false;
    private static boolean allowNextClick = true;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.isPaused()
                || com.pablo.tenseicraft.client.cinematics.ReimuIntroCinematicClient.isActive()) {
            wasAttackKeyDown = false;
            allowNextClick = true;
            return;
        }

        boolean isKeyDown = mc.options.keyAttack.isDown();

        // Never interrupt or suppress keyAttack when mining or looking at a block
        boolean isMiningOrTargetingBlock = (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK)
                || (mc.gameMode != null && mc.gameMode.isDestroying());

        if (isMiningOrTargetingBlock) {
            wasAttackKeyDown = isKeyDown;
            return;
        }

        if (!isKeyDown) {
            wasAttackKeyDown = false;
            return;
        }

        boolean hasRing = BetterCombatCompat.hasWarriorRingEquipped(mc.player);

        // 1. Better Combat Integration Mode
        if (BetterCombatCompat.isBetterCombatLoaded()) {
            if (!hasRing) {
                // Without ring: block holding attack loop (only first click press registers for combat)
                if (wasAttackKeyDown) {
                    mc.options.keyAttack.setDown(false);
                }
            }
            wasAttackKeyDown = true;
            return;
        }

        // 2. Standalone Vanilla Auto-Attack Mode (when Better Combat is not installed)
        if (isKeyDown && hasRing) {
            if (mc.player.getAttackStrengthScale(0.0f) >= 1.0f) {
                if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult entityHit = (EntityHitResult) mc.hitResult;
                    if (mc.gameMode != null) {
                        mc.gameMode.attack(mc.player, entityHit.getEntity());
                        mc.player.swing(InteractionHand.MAIN_HAND);
                        mc.player.resetAttackStrengthTicker();
                    }
                }
            }
        }
    }
}
