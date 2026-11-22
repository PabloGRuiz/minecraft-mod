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
 * Centra y escala el modelo 3D acorde a {@link EnergyAttackEntity#getOrbScale()} con brillo completo.
 */
public class EnergyAttackRenderer extends EntityRenderer<EnergyAttackEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/energy_attack.png");

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
        float visualScale = entity.getOrbScale() * 2.25F;

        poseStack.pushPose();
        // Centrar exactamente en la hitbox (1.125 * scale de alto -> centro en 0.5625 * scale = 0.25F * visualScale)
        poseStack.translate(0.0F, 0.25F * visualScale, 0.0F);
        poseStack.scale(visualScale, visualScale, visualScale);
        // Convertir ejes de EntityModel (Y abajo) a mundo y trasladar el centro (0, 18px, 0) = (0, 1.125F, 0) al origen
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.125F, 0.0F);

        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
