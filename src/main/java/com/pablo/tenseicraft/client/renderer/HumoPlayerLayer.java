package com.pablo.tenseicraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pablo.tenseicraft.client.model.HumoModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class HumoPlayerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final HumoModel<AbstractClientPlayer> model;

    public HumoPlayerLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new HumoModel<>(modelSet.bakeLayer(HumoModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) return;
        // Solo renderizar si la transformación / habilidad de humo está activa
        if (!HumoEffectManager.isSmokeActive(player)) return;

        poseStack.pushPose();

        // Anclar al torso/hombros del jugador (así no se mueve como una peluca con la cabeza)
        this.getParentModel().body.translateAndRotate(poseStack);

        // Posicionar un pelín más arriba (-Y) y más adelante (+Z moderado) según la guía visual
        poseStack.translate(0.0D, -0.27D, 0.07D);

        // Escalar para que sea amplio y envuelva los hombros
        poseStack.scale(1.75F, 1.75F, 1.75F);

        // Configurar animación de flotación y respiración mística
        this.model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Renderizado translúcido con culling deshabilitado
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityTranslucent(HumoModel.TEXTURE));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();
    }
}
