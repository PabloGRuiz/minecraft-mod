package com.pablo.rpglevels.client.events;

import com.mojang.blaze3d.shaders.FogShape;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.world.dimension.ModDimensions;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class AbyssFogClientEvents {

    /**
     * Atmospheric 5-chunk fog limiter:
     * When inside the Abyss Rift, restricts visibility to 5 chunks (~75 blocks)
     * with dense impenetrable shadow beyond.
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        Entity entity = camera.getEntity();
        if (entity == null) return;
        Level level = entity.level();
        if (!ModDimensions.isAbyssRift(level)) return;

        // Restrict fog so you can see up to ~5 chunks (75-80 blocks), but beyond that is shrouded in pure abyssal dark mist
        event.setNearPlaneDistance(15.0f);
        event.setFarPlaneDistance(75.0f);
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    /**
     * Atmospheric fog coloring based on abyssal biome:
     * - Basalt Deltas / Magma: dark volcanic charcoal with deep crimson ember tint
     * - Frozen Peaks / Ice: deep frozen midnight navy cyan
     * - Dripstone Caves: cold dark slate charcoal
     */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Camera camera = event.getCamera();
        Entity entity = camera.getEntity();
        if (entity == null) return;
        Level level = entity.level();
        if (!ModDimensions.isAbyssRift(level)) return;

        BlockPos pos = camera.getBlockPosition();
        Holder<Biome> biome = level.getBiome(pos);

        if (biome.is(com.pablo.rpglevels.init.ModBiomes.MAGMA_CHASM) || biome.is(Biomes.BASALT_DELTAS) || biome.is(Biomes.NETHER_WASTES)) {
            // Dark volcanic crimson tint
            event.setRed(0.06f);
            event.setGreen(0.015f);
            event.setBlue(0.015f);
        } else if (biome.is(com.pablo.rpglevels.init.ModBiomes.FROSTED_HOLLOW) || biome.is(Biomes.FROZEN_PEAKS) || biome.is(Biomes.JAGGED_PEAKS) || biome.is(Biomes.ICE_SPIKES)) {
            // Deep frosted navy cyan tint
            event.setRed(0.015f);
            event.setGreen(0.04f);
            event.setBlue(0.07f);
        } else {
            // Abyssal slate dark mist
            event.setRed(0.02f);
            event.setGreen(0.02f);
            event.setBlue(0.03f);
        }
    }
}
