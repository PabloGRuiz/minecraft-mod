package com.pablo.tenseicraft.entities.ai;

import net.minecraft.world.entity.PathfinderMob;

/**
 * Interface implemented by humanoid combatants (Adventurers, Bandits)
 * who participate in the fluid RPG duel combat system.
 */
public interface RpgCombatDuelist {
    int getCombatCombo();
    void setCombatCombo(int combo);

    boolean isBlockingOrParrying();
    void setBlockingOrParrying(boolean parrying);

    long getLastAttackGameTime();
    void setLastAttackGameTime(long time);

    PathfinderMob getDuelistMob();

    default boolean isCombatSuspended() {
        return false;
    }
}
