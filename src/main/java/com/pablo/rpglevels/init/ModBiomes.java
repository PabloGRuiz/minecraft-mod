package com.pablo.rpglevels.init;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class ModBiomes {

    public static final ResourceKey<Biome> DRIPSTONE_ABYSS = register("dripstone_abyss");
    public static final ResourceKey<Biome> MAGMA_CHASM = register("magma_chasm");
    public static final ResourceKey<Biome> FROSTED_HOLLOW = register("frosted_hollow");

    private static ResourceKey<Biome> register(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, name));
    }
}
