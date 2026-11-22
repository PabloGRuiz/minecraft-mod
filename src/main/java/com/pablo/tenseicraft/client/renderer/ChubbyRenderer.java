package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.model.ChubbyModel;
import com.pablo.tenseicraft.entities.ChubbyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ChubbyRenderer extends MobRenderer<ChubbyEntity, ChubbyModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/chubby.png");

    public ChubbyRenderer(EntityRendererProvider.Context context) {
        super(context, new ChubbyModel(context.bakeLayer(ChubbyModel.LAYER_LOCATION)), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(ChubbyEntity entity) {
        return TEXTURE;
    }
}
