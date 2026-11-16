package com.pablo.rpglevels.world.dimension;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceLocation ABYSS_RIFT_ID = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "abyss_rift");
    public static final ResourceKey<Level> ABYSS_RIFT_LEVEL = ResourceKey.create(Registries.DIMENSION, ABYSS_RIFT_ID);
    public static final ResourceKey<DimensionType> ABYSS_RIFT_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ABYSS_RIFT_ID);

    public static final ResourceLocation VOID_TRIAL_ID = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "void_trial");
    public static final ResourceKey<Level> VOID_TRIAL_LEVEL = ResourceKey.create(Registries.DIMENSION, VOID_TRIAL_ID);
    public static final ResourceKey<DimensionType> VOID_TRIAL_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, VOID_TRIAL_ID);

    public static boolean isAbyssRift(Level level) {
        return level != null && level.dimension().equals(ABYSS_RIFT_LEVEL);
    }

    public static boolean isVoidTrial(Level level) {
        return level != null && level.dimension().equals(VOID_TRIAL_LEVEL);
    }
}
