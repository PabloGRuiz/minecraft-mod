package com.pablo.tenseicraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.EnergyAttackEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo 3D animado del orbe de energía Danmaku ("EnergyAttack").
 * - {@code Core}: cubo interior que levita suavemente de arriba a abajo.
 * - {@code frame_pivot} / {@code frame}: jaula exterior que gira de forma errática y rápida en los 3 ejes.
 */
public class EnergyAttackModel extends EntityModel<EnergyAttackEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "energy_attack"), "main");

    private final ModelPart core;
    private final ModelPart framePivot;

    public EnergyAttackModel(ModelPart root) {
        this.core = root.getChild("Core");
        this.framePivot = root.getChild("frame_pivot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("Core",
                CubeListBuilder.create()
                        .texOffs(30, 22).addBox(-1.0F, -1.5F, -1.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 18.5F, 0.0F));

        // Nodo pivote centrado en (0.0F, 18.0F, 0.0F) para que la jaula exterior gire concéntrica al Core
        PartDefinition framePivot = partdefinition.addOrReplaceChild("frame_pivot",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        PartDefinition frame = framePivot.addOrReplaceChild("frame",
                CubeListBuilder.create()
                        .texOffs(16, 8).addBox(7.0F, 3.0F, -6.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 15).addBox(0.0F, 3.0F, -6.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(14, 29).addBox(0.0F, -4.0F, -6.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(30, 8).addBox(7.0F, -4.0F, -6.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-4.0F, 0.0F, 3.0F));

        frame.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(15, -1).addBox(0.0F, -1.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 1.5708F));

        frame.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(-1, 15).addBox(0.0F, -1.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 1.5708F));

        frame.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(-1, 7).addBox(0.0F, -1.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, -7.0F, 0.0F, -1.5708F, 1.5708F));

        frame.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(-1, -1).addBox(0.0F, -1.0F, -4.0F, 1.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(7.0F, 0.0F, -7.0F, 0.0F, -1.5708F, 1.5708F));

        frame.addOrReplaceChild("cube_r5",
                CubeListBuilder.create()
                        .texOffs(30, 15).addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 24).addBox(0.0F, 6.0F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -3.0F, -7.0F, 0.0F, -1.5708F, 0.0F));

        frame.addOrReplaceChild("cube_r6",
                CubeListBuilder.create()
                        .texOffs(28, 29).addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 22).addBox(0.0F, 6.0F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -3.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(EnergyAttackEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Desfase por ID para que múltiples orbes disparados en patrón no oscilen idénticos al unísono
        float phase = (entity != null ? entity.getId() * 0.73F : 0.0F);
        float t = ageInTicks + phase;

        // Núcleo interior: levita suavemente de arriba abajo con rotación pausada
        this.core.y = 18.5F + Mth.sin(t * 0.08F) * 0.75F;
        this.core.yRot = t * 0.03F;
        this.core.xRot = Mth.cos(t * 0.05F) * 0.15F;

        // Marco exterior: rotación continua, fluida y lenta en los 3 ejes alrededor del centro del orbe
        this.framePivot.xRot = t * 0.045F + Mth.sin(t * 0.035F) * 0.25F;
        this.framePivot.yRot = t * 0.065F + Mth.cos(t * 0.028F) * 0.30F;
        this.framePivot.zRot = t * 0.052F + Mth.sin(t * 0.04F + 1.2F) * 0.25F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.core.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.framePivot.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
