package com.pablo.rpglevels.mobs;

import com.pablo.rpglevels.data.ThreatLevel;
import net.minecraft.world.entity.LivingEntity;

public class MobRankHandler {

    /**
     * Obtains the Threat Level for any entity based on its base stats and lore.
     */
    public static ThreatLevel getThreatLevel(LivingEntity entity) {
        return ThreatLevel.determineThreatLevel(entity);
    }

    public static boolean isBossOrBlacklisted(LivingEntity entity) {
        if (entity == null) return true;
        if (BossScalingHandler.isBoss(entity)) return true;
        if (entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || entity instanceof com.pablo.rpglevels.entities.WindMasterBossEntity
                || entity instanceof com.pablo.rpglevels.entities.UndeadAdventurerBossEntity
                || entity instanceof com.pablo.rpglevels.entities.GuildMasterNpcEntity
                || entity instanceof com.pablo.rpglevels.entities.WindMasterNpcEntity
                || entity instanceof com.pablo.rpglevels.entities.AdventurerNpcEntity
                || entity instanceof com.pablo.rpglevels.entities.TownswomanNpcEntity
                || entity instanceof com.pablo.rpglevels.entities.VoidbornEntity
                || entity instanceof com.pablo.rpglevels.entities.HornlessVoidbornEntity
                || entity instanceof com.pablo.rpglevels.entities.HornedVoidbornEntity
                || entity instanceof com.pablo.rpglevels.entities.ColossalVoidbornEntity) {
            return true;
        }
        return false;
    }

    /**
     * Drop loot multiplier based on Threat Level.
     */
    public static float getLootMultiplier(ThreatLevel threat) {
        if (threat == null) return 1.0f;
        return switch (threat) {
            case GENESIS -> 5.0f;
            case CATASTROPHE -> 4.0f;
            case GRAND_DISASTER -> 3.0f;
            case DISASTER -> 2.5f;
            case CALAMITY -> 2.0f;
            case HAZARD -> 1.75f;
            case A -> 1.6f;
            case B -> 1.5f;
            case C -> 1.25f;
            case D -> 1.15f;
            case E -> 1.05f;
            default -> 1.0f;
        };
    }
}
