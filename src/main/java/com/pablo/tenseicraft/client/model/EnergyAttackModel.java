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

/**
 * Modelo 3D del proyectil Danmaku Ying-Yang de Reimu Hakurei ("EnergyAttack").
 * Orientado de forma completamente horizontal (paralelo al piso) y con giro continuo.
 */
public class EnergyAttackModel extends EntityModel<EnergyAttackEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "energy_attack"), "main");

    private final ModelPart root;
    private final ModelPart disc;
    private final ModelPart bb_main;

    public EnergyAttackModel(ModelPart root) {
        this.root = root;
        this.disc = root.getChild("disc");
        this.bb_main = this.disc.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Nodo 'disc': rota 90° alrededor del eje Z (Math.PI / 2) para orientar el disco Ying-Yang
        // de forma totalmente HORIZONTAL (paralelo al piso / plano X-Z).
        PartDefinition disc = partdefinition.addOrReplaceChild("disc",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2.0)));

        // bb_main con offset (-7.5F, 11.0F, 0.0F) para que el centro del disco quede exactamente en (0, 0, 0)
        disc.addOrReplaceChild("bb_main",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(7.0F, -14.0F, -8.0F, 1.0F, 6.0F, 16.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 22).addBox(7.0F, -16.0F, -7.0F, 1.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(30, 22).addBox(7.0F, -8.0F, -7.0F, 1.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(34, 0).addBox(7.0F, -6.0F, -6.0F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 38).addBox(7.0F, -17.0F, -6.0F, 1.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 38).addBox(7.0F, -5.0F, -5.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 38).addBox(7.0F, -18.0F, -5.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(34, 13).addBox(7.0F, -4.0F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 13).addBox(7.0F, -19.0F, -3.0F, 1.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-7.5F, 11.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(EnergyAttackEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Giro horizontal continuo del disco Ying-Yang alrededor del eje vertical Y
        float phase = (entity != null ? entity.getId() * 17.0F : 0.0F);
        float t = ageInTicks + phase;
        this.root.yRot = t * 0.18F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
