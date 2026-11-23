package com.pablo.tenseicraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.model.EnergyAttackModel;
import com.pablo.tenseicraft.entities.EnergyAttackEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderizador para el proyectil Danmaku {@link EnergyAttackEntity}.
 * Renderiza el disco Ying-Yang giratorio horizontal a escala configurable con brillo completo.
 */
public class EnergyAttackRenderer extends EntityRenderer<EnergyAttackEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/yingyang.png");

    private final EnergyAttackModel model;

    public EnergyAttackRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new EnergyAttackModel(context.bakeLayer(EnergyAttackModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyAttackEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(EnergyAttackEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float ageInTicks = entity.tickCount + partialTick;
        float scale = entity.getOrbScale();

        poseStack.pushPose();
        // Centrar verticalmente en la hitbox de la entidad (altura total = 1.125F * scale -> centro a 0.5625F * scale)
        poseStack.translate(0.0F, 0.5625F * scale, 0.0F);
        // Escalar según el scale de la entidad (el modelo base mide 16x16 píxeles = 1x1 bloque)
        poseStack.scale(scale, scale, scale);
        // Convertir de coordenadas de EntityModel (Y hacia abajo) a espacio del mundo (Y hacia arriba)
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
