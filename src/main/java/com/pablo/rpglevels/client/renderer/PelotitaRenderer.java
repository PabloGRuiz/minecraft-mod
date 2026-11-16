package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.PelotitaModel;
import com.pablo.rpglevels.entities.PelotitaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class PelotitaRenderer extends MobRenderer<PelotitaEntity, PelotitaModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/pelotita.png");

    public PelotitaRenderer(EntityRendererProvider.Context context) {
        super(context, new PelotitaModel(context.bakeLayer(PelotitaModel.LAYER_LOCATION)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(PelotitaEntity entity) {
        return TEXTURE;
    }
}

