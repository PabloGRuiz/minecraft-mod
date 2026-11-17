package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.StarryModel;
import com.pablo.rpglevels.entities.StarryEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StarryRenderer extends MobRenderer<StarryEntity, StarryModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/starry.png");

    public StarryRenderer(EntityRendererProvider.Context context) {
        super(context, new StarryModel(context.bakeLayer(StarryModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(StarryEntity entity) {
        return TEXTURE;
    }
}
