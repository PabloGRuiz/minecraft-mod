package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.model.StarryModel;
import com.pablo.tenseicraft.entities.StarryEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class StarryRenderer extends MobRenderer<StarryEntity, StarryModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/starry.png");

    public StarryRenderer(EntityRendererProvider.Context context) {
        super(context, new StarryModel(context.bakeLayer(StarryModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(StarryEntity entity) {
        return TEXTURE;
    }
}
