package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.VoidbornModel;
import com.pablo.rpglevels.entities.VoidbornEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class VoidbornRenderer<T extends VoidbornEntity> extends MobRenderer<T, VoidbornModel<T>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/voidborn.png");

    public VoidbornRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidbornModel<>(context.bakeLayer(VoidbornModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
