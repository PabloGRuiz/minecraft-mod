package com.pablo.rpglevels.compat.irons;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.world.entity.player.Player;

/**
 * Helper seguro para acceder a datos de Iron's Spells en el lado del cliente (evita ClassNotFound en servidor dedicado).
 */
public class IronsSpellsClientCompat {

    public static float getClientMana() {
        try {
            return (float) ClientMagicData.getPlayerMana();
        } catch (Throwable t) {
            return -1.0f;
        }
    }

    public static float getClientMaxMana(Player player) {
        if (player == null) return -1.0f;
        try {
            if (player.getAttributes().hasAttribute(AttributeRegistry.MAX_MANA)) {
                return (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
            }
        } catch (Throwable t) {
        }
        return -1.0f;
    }
}

