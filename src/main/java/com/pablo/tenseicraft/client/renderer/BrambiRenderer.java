package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.model.BrambiModel;
import com.pablo.tenseicraft.entities.BrambiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BrambiRenderer extends MobRenderer<BrambiEntity, BrambiModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/brambi.png");

    public BrambiRenderer(EntityRendererProvider.Context context) {
        super(context, new BrambiModel(context.bakeLayer(BrambiModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(BrambiEntity entity) {
        return TEXTURE;
    }
}
