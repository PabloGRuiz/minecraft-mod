package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.model.RedfoxModel;
import com.pablo.rpglevels.entities.RedfoxEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class RedfoxRenderer extends MobRenderer<RedfoxEntity, RedfoxModel<RedfoxEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/redfox.png");

    public RedfoxRenderer(EntityRendererProvider.Context context) {
        super(context, new RedfoxModel<>(context.bakeLayer(RedfoxModel.LAYER_LOCATION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(RedfoxEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(RedfoxEntity entity) {
        return super.shouldShowName(entity);
    }
}
