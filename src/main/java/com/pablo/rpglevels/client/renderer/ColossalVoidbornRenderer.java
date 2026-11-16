package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.ColossalVoidbornModel;
import com.pablo.rpglevels.entities.ColossalVoidbornEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class ColossalVoidbornRenderer extends MobRenderer<ColossalVoidbornEntity, ColossalVoidbornModel<ColossalVoidbornEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/colossal_voidborn.png");

    public ColossalVoidbornRenderer(EntityRendererProvider.Context context) {
        super(context, new ColossalVoidbornModel<>(context.bakeLayer(ColossalVoidbornModel.LAYER_LOCATION)), 0.85F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ColossalVoidbornEntity entity) {
        return TEXTURE;
    }
}
