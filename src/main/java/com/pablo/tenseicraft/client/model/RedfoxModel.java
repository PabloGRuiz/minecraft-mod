package com.pablo.tenseicraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.RedfoxEntity;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class RedfoxModel<T extends RedfoxEntity> extends HierarchicalModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "redfox"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart falda;
    private final ModelPart left_arm;
    private final ModelPart right_arm;
    private final ModelPart right_leg;
    private final ModelPart left_leg;
    private final ModelPart cola;
    private final ModelPart head;
    private final ModelPart left_ear;
    private final ModelPart right_ear;

    public RedfoxModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.falda = this.body.getChild("falda");
        this.left_arm = this.body.getChild("left_arm");
        this.right_arm = this.body.getChild("right_arm");
        this.right_leg = this.body.getChild("right_leg");
        this.left_leg = this.body.getChild("left_leg");
        this.cola = this.body.getChild("cola");
        this.head = this.body.getChild("head");
        this.left_ear = this.head.getChild("left_ear");
        this.right_ear = this.head.getChild("right_ear");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(36, 19).addBox(9.0F, -9.0F, -1.0F, 10.0F, 13.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-14.0F, 10.0F, 0.0F));

        PartDefinition falda = body.addOrReplaceChild("falda",
                CubeListBuilder.create(),
                PartPose.offset(14.0F, 3.0F, 0.0F));

        falda.addOrReplaceChild("izquierda_r1",
                CubeListBuilder.create().texOffs(64, 62).addBox(0.0F, 0.0F, -2.0F, 0.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(5.0F, 0.0F, 1.0F, 0.0F, 0.0F, -0.3491F));

        falda.addOrReplaceChild("derecha_r1",
                CubeListBuilder.create().texOffs(64, 62).mirror().addBox(0.0F, 0.0F, -3.0F, 0.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offsetAndRotation(-5.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.3054F));

        PartDefinition frente = falda.addOrReplaceChild("frente",
                CubeListBuilder.create().texOffs(16, 53).addBox(-5.0F, 0.0F, 0.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -0.2182F, 0.0F, 0.0F));

        frente.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(16, 53).addBox(2.0F, 0.0F, -1.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 1.0F, 0.0436F, 0.0F, 0.0F));

        frente.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(10, 66).addBox(-5.0F, 0.0F, 0.0F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, 0.0F, 0.0F, -0.0873F, 0.0F, 0.0F));

        PartDefinition atras = falda.addOrReplaceChild("atras",
                CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.2618F, 0.0F, 0.0F));

        atras.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(16, 53).addBox(-2.0F, 0.0F, 0.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 53).addBox(-9.0F, 0.0F, 0.0F, 3.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, -0.0873F, 0.0F, 0.0F));

        atras.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(10, 66).addBox(-2.0F, 0.0F, 0.0F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0436F, 0.0F, 0.0F));

        body.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(22, 45).addBox(0.0F, -1.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(54, 45).addBox(0.0F, 6.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(19.0F, -8.0F, 1.0F));

        body.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0, 47).addBox(-4.0F, 6.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 45).addBox(-4.0F, -1.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(9.0F, -8.0F, 1.0F));

        body.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(32, 57).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(58, 9).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 38).addBox(-2.0F, 8.0F, -5.0F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(11.0F, 4.0F, 1.0F));

        body.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(48, 57).addBox(-2.0F, 4.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(36, 36).addBox(-2.0F, 8.0F, -5.0F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 57).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(17.0F, 4.0F, 1.0F));

        body.addOrReplaceChild("cola",
                CubeListBuilder.create().texOffs(0, 19).addBox(-8.0F, -4.0F, 3.0F, 8.0F, 9.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 66).addBox(-5.0F, -2.0F, 15.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(68, 5).addBox(-6.0F, 3.0F, 15.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 66).addBox(-1.0F, -1.0F, 14.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 9).addBox(-8.0F, -4.0F, 13.0F, 8.0F, 9.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 22).addBox(-6.0F, -1.0F, 15.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(58, 36).addBox(-7.0F, -3.0F, 14.0F, 6.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 47).addBox(-8.0F, -2.0F, 14.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 0).addBox(-6.0F, -1.0F, -1.0F, 4.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(18.0F, 0.0F, 3.0F));

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(22, 38).addBox(-2.0F, -4.0F, -8.0F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 65).addBox(-9.0F, -5.0F, -5.0F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 65).addBox(5.0F, -5.0F, -5.0F, 4.0F, 5.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(14.0F, -9.0F, 1.0F));

        PartDefinition left_ear = head.addOrReplaceChild("left_ear",
                CubeListBuilder.create(),
                PartPose.offset(4.0F, -8.0F, 1.0F));

        left_ear.addOrReplaceChild("cube_r5",
                CubeListBuilder.create().texOffs(0, 59).addBox(0.0F, -5.0F, -6.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 64).addBox(-1.0F, -4.0F, -6.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 64).addBox(1.0F, -6.0F, -7.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 66).addBox(3.0F, -7.0F, -7.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(60, 5).addBox(3.0F, -7.0F, -6.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(60, 0).addBox(1.0F, -6.0F, -6.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 68).addBox(0.0F, -5.0F, -7.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 59).addBox(-1.0F, -4.0F, -7.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 1.0F, 5.0F, 0.0F, 0.0F, 0.3054F));

        PartDefinition right_ear = head.addOrReplaceChild("right_ear",
                CubeListBuilder.create(),
                PartPose.offset(-4.0F, -8.0F, 0.0F));

        right_ear.addOrReplaceChild("cube_r6",
                CubeListBuilder.create().texOffs(64, 32).addBox(-6.0F, -7.0F, -6.0F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 57).addBox(-5.0F, -6.0F, -6.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 27).addBox(-4.0F, -5.0F, -6.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(34, 66).addBox(0.0F, -4.0F, -7.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 17).addBox(-3.0F, -4.0F, -6.0F, 4.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 66).addBox(-6.0F, -7.0F, -7.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(58, 17).addBox(-3.0F, -6.0F, -7.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(68, 7).addBox(-1.0F, -5.0F, -7.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 1.0F, 6.0F, 0.0F, 0.0F, -0.3054F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Mirada / giro de cabeza
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Movimiento de piernas al caminar
        this.left_leg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.right_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        // Movimiento de brazos al caminar
        this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.2F * limbSwingAmount;
        if (entity.getMainArm() == HumanoidArm.RIGHT && !entity.getMainHandItem().isEmpty()) {
            // Brazo ligeramente inclinado al empuñar la espada, balanceándose con suavidad al caminar
            this.right_arm.xRot = -0.25F + Mth.cos(limbSwing * 0.6662F) * 0.2F * limbSwingAmount;
            this.right_arm.yRot = 0.0F;
        } else {
            this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
            this.right_arm.yRot = 0.0F;
        }

        // Animación suave y natural de la cola de zorro (oscilación pendular suave en reposo y caminata)
        this.cola.yRot = Mth.sin(ageInTicks * 0.07F) * 0.18F;
        this.cola.zRot = Mth.cos(ageInTicks * 0.05F) * 0.06F;
        if (limbSwingAmount > 0.05F) {
            this.cola.yRot += Mth.sin(limbSwing * 0.6662F) * 0.25F * limbSwingAmount;
        }

        // Animación orgánica de orejas kitsune (twitch / aleteo de orejas cada ciertos segundos)
        float earCycle = (ageInTicks + entity.getId() * 11) % 120.0F; // Ciclo de 6 segundos por entidad
        float rightEarTwitch = 0.0F;
        float leftEarTwitch = 0.0F;

        // Contracciones de orejas en distintos momentos del ciclo
        if (earCycle > 20.0F && earCycle < 35.0F) {
            // Oreja derecha se agacha y sube (doble espasmo rápido)
            float t = (earCycle - 20.0F) * (float) Math.PI / 15.0F;
            rightEarTwitch = Mth.sin(t * 2.0F) * 0.22F;
        } else if (earCycle > 70.0F && earCycle < 85.0F) {
            // Oreja izquierda se agacha y sube
            float t = (earCycle - 70.0F) * (float) Math.PI / 15.0F;
            leftEarTwitch = Mth.sin(t * 2.0F) * 0.22F;
        } else if (earCycle > 105.0F && earCycle < 118.0F) {
            // Ambas orejas reaccionan sutilmente juntas
            float t = (earCycle - 105.0F) * (float) Math.PI / 13.0F;
            float sync = Mth.sin(t) * 0.15F;
            rightEarTwitch = sync;
            leftEarTwitch = sync;
        }

        // Micro-respiración continua de las puntas
        float breathingEar = Mth.sin(ageInTicks * 0.04F) * 0.03F;

        this.right_ear.xRot = rightEarTwitch + breathingEar;
        this.right_ear.zRot = rightEarTwitch * 0.5F;
        this.left_ear.xRot = leftEarTwitch + breathingEar;
        this.left_ear.zRot = -leftEarTwitch * 0.5F;
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);

        if (arm == HumanoidArm.RIGHT) {
            this.right_arm.translateAndRotate(poseStack);
            // El modelo humanoide estándar de Minecraft sitúa la mano a X = -1.0, Y = 10.0 en el brazo derecho.
            // En este modelo, el brazo derecho tiene centro X = -2.0 y el puño en Y = 12.5.
            // Ajustamos exactamente ese delta (-0.0625F en X, +0.156F en Y) para que ItemInHandLayer
            // coloque la empuñadura exactamente en la mano sin desfasarla en Z hacia adelante.
            poseStack.translate(-0.0625F, 0.156F, 0.0F);
        } else {
            this.left_arm.translateAndRotate(poseStack);
            poseStack.translate(0.0625F, 0.156F, 0.0F);
        }
    }
}
