package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.ChubbyModel;
import com.pablo.rpglevels.entities.ChubbyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ChubbyRenderer extends MobRenderer<ChubbyEntity, ChubbyModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/chubby.png");

    public ChubbyRenderer(EntityRendererProvider.Context context) {
        super(context, new ChubbyModel(context.bakeLayer(ChubbyModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(ChubbyEntity entity) {
        return TEXTURE;
    }
}
