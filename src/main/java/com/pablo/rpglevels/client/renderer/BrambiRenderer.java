package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.BrambiModel;
import com.pablo.rpglevels.entities.BrambiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BrambiRenderer extends MobRenderer<BrambiEntity, BrambiModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/brambi.png");

    public BrambiRenderer(EntityRendererProvider.Context context) {
        super(context, new BrambiModel(context.bakeLayer(BrambiModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(BrambiEntity entity) {
        return TEXTURE;
    }
}
