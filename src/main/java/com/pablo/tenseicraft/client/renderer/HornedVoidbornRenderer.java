package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.model.HornedVoidbornModel;
import com.pablo.tenseicraft.entities.HornedVoidbornEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class HornedVoidbornRenderer extends MobRenderer<HornedVoidbornEntity, HornedVoidbornModel<HornedVoidbornEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/horned_voidborn.png");

    public HornedVoidbornRenderer(EntityRendererProvider.Context context) {
        super(context, new HornedVoidbornModel<>(context.bakeLayer(HornedVoidbornModel.LAYER_LOCATION)), 0.65F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(HornedVoidbornEntity entity) {
        return TEXTURE;
    }
}
