package com.pablo.tenseicraft.init;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class ModBiomes {

    public static final ResourceKey<Biome> DRIPSTONE_ABYSS = register("dripstone_abyss");
    public static final ResourceKey<Biome> MAGMA_CHASM = register("magma_chasm");
    public static final ResourceKey<Biome> FROSTED_HOLLOW = register("frosted_hollow");

    private static ResourceKey<Biome> register(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, name));
    }
}
