package com.pablo.rpglevels.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.VoidbornEntity;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class VoidbornModel<T extends VoidbornEntity> extends HierarchicalModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "voidborn"), "main");

    private final ModelPart root;
    private final ModelPart brazos;
    private final ModelPart cuerpo;
    private final ModelPart cabeza;
    private final ModelPart piernas;
    private final ModelPart izquierda;
    private final ModelPart derecha;

    public VoidbornModel(ModelPart root) {
        this.root = root;
        this.brazos = root.getChild("brazos");
        this.cuerpo = root.getChild("cuerpo");
        this.cabeza = this.cuerpo.getChild("cabeza");
        this.piernas = root.getChild("piernas");
        this.izquierda = this.piernas.getChild("izquierda");
        this.derecha = this.piernas.getChild("derecha");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Brazos estirados al frente estilo Hollow / Zombie
        PartDefinition brazos = partdefinition.addOrReplaceChild("brazos",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(9.0F, -3.0F, -7.0F, 4.0F, 4.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-7.0F, 5.0F, -3.0F));

        brazos.addOrReplaceChild("izquierdo", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

        brazos.addOrReplaceChild("derecho",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -3.0F, -7.0F, 4.0F, 4.0F, 12.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Torso
        PartDefinition cuerpo = partdefinition.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(32, 0)
                        .addBox(-6.0F, -22.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        // Hueco de Hollow en el pecho
        cuerpo.addOrReplaceChild("hueco",
                CubeListBuilder.create().texOffs(44, 44).addBox(2.0F, 1.0F, -2.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 44).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 44).addBox(-2.0F, 1.0F, -2.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 44).addBox(-1.0F, 4.0F, -2.0F, 2.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48).addBox(1.0F, 1.0F, -2.0F, 1.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48).addBox(-2.0F, 1.0F, -2.0F, 1.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48).addBox(-2.0F, 3.0F, -2.0F, 1.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 48).addBox(1.0F, 3.0F, -2.0F, 1.0F, 0.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 48).addBox(1.0F, 3.0F, -2.0F, 0.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 48).addBox(1.0F, 0.0F, -2.0F, 0.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 48).addBox(-1.0F, 0.0F, -2.0F, 0.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 48).addBox(-1.0F, 3.0F, -2.0F, 0.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, -18.0F, 0.0F));

        // Cabeza con máscara de Hollow separada para rotar al mirar al jugador
        cuerpo.addOrReplaceChild("cabeza",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, -22.0F, 0.0F));

        // Piernas con pivote articulado en la cadera para caminata natural
        PartDefinition piernas = partdefinition.addOrReplaceChild("piernas",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        piernas.addOrReplaceChild("izquierda",
                CubeListBuilder.create().texOffs(32, 16)
                        .addBox(-6.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -10.0F, 0.0F));

        piernas.addOrReplaceChild("derecha",
                CubeListBuilder.create().texOffs(32, 30)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -10.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Mirada de la cabeza / máscara hacia el jugador
        this.cabeza.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.cabeza.xRot = headPitch * ((float) Math.PI / 180F);

        // Movimiento de piernas al caminar
        this.izquierda.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.derecha.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        // Respiración / balanceo siniestro de los brazos extendidos
        this.brazos.xRot = Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.brazos.yRot = Mth.cos(ageInTicks * 0.09F) * 0.05F;

        if (limbSwingAmount > 0.01F) {
            this.brazos.xRot += Mth.cos(limbSwing * 0.6662F) * 0.25F * limbSwingAmount;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.brazos.translateAndRotate(poseStack);

        // 1. Posicionar la empuñadura exactamente en la mano al frente del brazo
        if (arm == HumanoidArm.RIGHT) {
            poseStack.translate(-0.0625F, -0.0625F, -0.40625F);
        } else {
            poseStack.translate(0.6875F, -0.0625F, -0.40625F);
        }

        // 2. Orientación de combate: hoja apuntando hacia adelante al enemigo
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));

        // 3. Cancelar las transformaciones fijas de ItemInHandLayer para que nuestra orientación
        // se aplique de forma limpia y directa al ítem sin voltearlo al revés
        boolean isLeft = arm == HumanoidArm.LEFT;
        poseStack.translate(isLeft ? 0.0625F : -0.0625F, -0.125F, 0.625F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }
}
