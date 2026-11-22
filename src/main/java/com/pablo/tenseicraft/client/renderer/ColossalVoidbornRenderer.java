package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.model.ColossalVoidbornModel;
import com.pablo.tenseicraft.entities.ColossalVoidbornEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class ColossalVoidbornRenderer extends MobRenderer<ColossalVoidbornEntity, ColossalVoidbornModel<ColossalVoidbornEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/colossal_voidborn.png");

    public ColossalVoidbornRenderer(EntityRendererProvider.Context context) {
        super(context, new ColossalVoidbornModel<>(context.bakeLayer(ColossalVoidbornModel.LAYER_LOCATION)), 0.85F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ColossalVoidbornEntity entity) {
        return TEXTURE;
    }
}
