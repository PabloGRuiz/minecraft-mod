package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.HornlessVoidbornModel;
import com.pablo.rpglevels.entities.HornlessVoidbornEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class HornlessVoidbornRenderer extends MobRenderer<HornlessVoidbornEntity, HornlessVoidbornModel<HornlessVoidbornEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/hornless_voidborn.png");

    public HornlessVoidbornRenderer(EntityRendererProvider.Context context) {
        super(context, new HornlessVoidbornModel<>(context.bakeLayer(HornlessVoidbornModel.LAYER_LOCATION)), 0.6F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(HornlessVoidbornEntity entity) {
        return TEXTURE;
    }
}
