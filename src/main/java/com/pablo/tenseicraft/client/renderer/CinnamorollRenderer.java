package com.pablo.tenseicraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.model.CinnamorollModel;
import com.pablo.tenseicraft.entities.CinnamorollEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CinnamorollRenderer extends MobRenderer<CinnamorollEntity, CinnamorollModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/cinnamoroll.png");

    public CinnamorollRenderer(EntityRendererProvider.Context context) {
        super(context, new CinnamorollModel(context.bakeLayer(CinnamorollModel.LAYER_LOCATION)), 0.35F);
    }

    @Override
    protected void scale(CinnamorollEntity entity, PoseStack poseStack, float partialTickTime) {
        // Tamaño adorable y abrazable de cachorrito nuboso
        poseStack.scale(0.72F, 0.72F, 0.72F);
    }

    @Override
    public ResourceLocation getTextureLocation(CinnamorollEntity entity) {
        return TEXTURE;
    }
}
