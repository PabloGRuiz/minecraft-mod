package com.pablo.rpglevels.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.ColossalVoidbornEntity;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class ColossalVoidbornModel<T extends ColossalVoidbornEntity> extends HierarchicalModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "colossal_voidborn"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart mask;
    private final ModelPart torso;
    private final ModelPart hueco;
    private final ModelPart right_leg;
    private final ModelPart right_foot;
    private final ModelPart left_leg;
    private final ModelPart left_foot;
    private final ModelPart right_arm;
    private final ModelPart right_forearm;
    private final ModelPart right_hand;
    private final ModelPart left_arm;
    private final ModelPart left_forearm;
    private final ModelPart left_hand;

    public ColossalVoidbornModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.mask = this.head.getChild("mask");
        this.torso = root.getChild("torso");
        this.hueco = this.torso.getChild("hueco");
        this.right_leg = root.getChild("right_leg");
        this.right_foot = this.right_leg.getChild("right_foot");
        this.left_leg = root.getChild("left_leg");
        this.left_foot = this.left_leg.getChild("left_foot");
        this.right_arm = root.getChild("right_arm");
        this.right_forearm = this.right_arm.getChild("right_forearm");
        this.right_hand = this.right_forearm.getChild("right_hand");
        this.left_arm = root.getChild("left_arm");
        this.left_forearm = this.left_arm.getChild("left_forearm");
        this.left_hand = this.left_forearm.getChild("left_hand");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Offsets ajustados +6.0F en Y para que la planta de los pies descanse exactamente en Y=24.0F (suelo)
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-4.0F, -8.0F, -5.0F, 8.0F, 8.0F, 9.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, -2.0F, 2.0F));

        PartDefinition mask = head.addOrReplaceChild("mask", CubeListBuilder.create()
                .texOffs(62, 49).addBox(-9.0F, -10.0F, 3.0F, 10.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 63).addBox(-8.0F, -9.0F, 2.0F, 8.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(34, 28).addBox(-8.0F, -1.0F, 2.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(66, 8).addBox(-7.0F, -8.0F, 1.0F, 6.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(76, 30).addBox(-7.0F, -1.0F, 1.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(76, 36).addBox(0.0F, -4.0F, 2.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(18, 78).addBox(-8.0F, -4.0F, 2.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(50, 76).addBox(-7.0F, -4.0F, 1.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(46, 76).addBox(-1.0F, -4.0F, 1.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(30, 78).addBox(0.0F, -2.0F, 2.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(78, 77).addBox(-8.0F, -2.0F, 2.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(26, 78).addBox(-7.0F, -2.0F, 1.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(22, 78).addBox(-1.0F, -2.0F, 1.0F, 0.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, 1.0F, -9.0F));

        mask.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(68, 77).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -9.0F, 3.0F, 0.3503F, 0.082F, 0.0299F));
        mask.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(76, 32).addBox(-3.0F, -1.0F, -1.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -9.0F, 3.0F, 0.0F, -0.5672F, -0.1309F));
        mask.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(68, 70).addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -11.0F, 7.0F, 0.3927F, 0.0F, 0.0F));
        mask.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(62, 23).addBox(0.0F, -1.0F, -1.0F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.0F, 7.0F, 0.0F, 0.0F, -0.48F));
        mask.addOrReplaceChild("dientesFrenteAbajo_r1", CubeListBuilder.create().texOffs(76, 23).addBox(1.0F, -1.0F, -1.0F, 0.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(34, 73).addBox(1.0F, -3.0F, -1.0F, 0.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, -1.0F, 2.0F, 0.0F, 1.5708F, 0.0F));
        mask.addOrReplaceChild("fondoDerechaArriba_r1", CubeListBuilder.create().texOffs(0, 73).addBox(-8.0F, -10.0F, -1.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(54, 67).addBox(-9.0F, -1.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-9.0F, 0.0F, 12.0F, 0.0F, -1.5708F, 0.0F));
        mask.addOrReplaceChild("fondoIzquierdaArriba_r1", CubeListBuilder.create().texOffs(20, 72).addBox(-8.0F, -10.0F, -1.0F, 6.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(54, 65).addBox(-9.0F, -1.0F, -1.0F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 12.0F, 0.0F, -1.5708F, 0.0F));
        mask.addOrReplaceChild("fondoArriba_r1", CubeListBuilder.create().texOffs(20, 65).addBox(-8.0F, -7.0F, -1.0F, 8.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.0F, 3.0F, -1.5708F, 0.0F, 0.0F));

        PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-4.0F, 6.0F, 0.0F, 8.0F, 8.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-6.0F, 0.0F, -1.0F, 12.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-2.0F, -2.0F, 0.0F));

        torso.addOrReplaceChild("hueco", CubeListBuilder.create()
                .texOffs(62, 56).addBox(-4.0F, -30.0F, -1.0F, 4.0F, 0.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(54, 70).addBox(-4.0F, -30.0F, -1.0F, 0.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(54, 70).addBox(0.0F, -30.0F, -1.0F, 0.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(62, 56).addBox(-4.0F, -27.0F, -1.0F, 4.0F, 0.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 32.0F, 0.0F));

        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(26, 48).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 12.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 11.0F, 1.0F));

        right_leg.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(62, 14).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 11.0F, 0.0F));

        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(44, 48).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 12.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 11.0F, 1.0F));

        left_leg.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(62, 40).addBox(-2.0F, 0.0F, -3.0F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 11.0F, 0.0F));

        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(34, 14).addBox(-7.0F, -3.0F, -4.0F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 61).addBox(-6.0F, 2.0F, -3.0F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, 1.0F, 3.0F));

        PartDefinition right_forearm = right_arm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(28, 31).addBox(-4.0F, 0.0F, -4.0F, 7.0F, 10.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 7.0F, 0.0F));

        PartDefinition right_hand = right_forearm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(46, 73).addBox(-1.0F, 5.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(50, 73).addBox(-3.0F, 5.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(66, 0).addBox(-5.0F, 0.0F, -1.0F, 5.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(14, 76).addBox(-5.0F, 5.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, 9.0F, -3.0F, 0.0F, 1.5708F, 0.0F));

        right_hand.addOrReplaceChild("pulgar_r1", CubeListBuilder.create().texOffs(56, 40).addBox(-0.8434F, -0.4627F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.5672F));

        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(38, 0).addBox(0.0F, -3.0F, -4.0F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(56, 28).addBox(1.0F, 2.0F, -3.0F, 5.0F, 7.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, 1.0F, 3.0F));

        PartDefinition left_forearm = left_arm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(0, 31).addBox(-3.0F, 0.0F, -4.0F, 7.0F, 10.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.0F, 7.0F, 0.0F));

        PartDefinition left_hand = left_forearm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(56, 44).addBox(0.0F, 5.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(20, 61).addBox(2.0F, 5.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(38, 65).addBox(0.0F, 0.0F, -1.0F, 5.0F, 5.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(14, 73).addBox(4.0F, 5.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, 9.0F, -3.0F, 0.0F, -1.5708F, 0.0F));

        left_hand.addOrReplaceChild("pulgar_r2", CubeListBuilder.create().texOffs(56, 42).addBox(-1.1566F, -0.4627F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, -0.5672F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        this.right_leg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
        this.left_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.1F * limbSwingAmount;

        this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.85F * limbSwingAmount - 0.15F;
        this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * 0.85F * limbSwingAmount - 0.15F;
        this.right_forearm.xRot = -0.35F + Mth.sin(ageInTicks * 0.08F) * 0.05F;
        this.left_forearm.xRot = -0.35F + Mth.cos(ageInTicks * 0.08F) * 0.05F;

        this.right_arm.zRot = 0.10F + Mth.cos(ageInTicks * 0.07F) * 0.04F;
        this.left_arm.zRot = -0.10F - Mth.cos(ageInTicks * 0.07F) * 0.04F;
        this.torso.xRot = Mth.sin(ageInTicks * 0.06F) * 0.03F;

        // 1. Animación de Golpe al Suelo (Ground Slam)
        int slamTicks = entity.getSlamTicks();
        if (slamTicks > 0) {
            if (slamTicks > 10) {
                float prep = Mth.clamp((24.0F - slamTicks) / 14.0F, 0.0F, 1.0F);
                this.right_arm.xRot = Mth.lerp(prep, -0.2F, -2.5F);
                this.left_arm.xRot = Mth.lerp(prep, -0.2F, -2.5F);
                this.right_forearm.xRot = -0.45F;
                this.left_forearm.xRot = -0.45F;
                this.torso.xRot = -0.22F * prep;
                this.head.xRot -= 0.25F * prep;
            } else {
                this.right_arm.xRot = 0.50F;
                this.left_arm.xRot = 0.50F;
                this.right_forearm.xRot = -0.15F;
                this.left_forearm.xRot = -0.15F;
                this.torso.xRot = 0.48F;
                this.head.xRot += 0.30F;
            }
            return;
        }

        // 2. Animación de Arrojar Piedra (Boulder Throw)
        int throwTicks = entity.getThrowTicks();
        if (throwTicks > 0) {
            if (throwTicks > 10) {
                float prep = Mth.clamp((22.0F - throwTicks) / 12.0F, 0.0F, 1.0F);
                this.right_arm.xRot = Mth.lerp(prep, 0.45F, -2.2F);
                this.left_arm.xRot = Mth.lerp(prep, 0.45F, -2.2F);
                this.right_arm.yRot = -0.25F;
                this.left_arm.yRot = 0.25F;
                this.torso.xRot = Mth.lerp(prep, 0.35F, -0.18F);
            } else {
                this.right_arm.xRot = -0.85F;
                this.left_arm.xRot = -0.85F;
                this.torso.xRot = 0.32F;
            }
            return;
        }

        // 3. Golpe cuerpo a cuerpo normal
        if (this.attackTime > 0.0F) {
            float swing = Mth.sin(this.attackTime * (float) Math.PI);
            this.right_arm.xRot = -1.75F * swing;
            this.left_arm.xRot = -1.45F * swing;
            this.right_forearm.xRot = -0.60F * swing;
            this.left_forearm.xRot = -0.60F * swing;
            this.torso.xRot = 0.25F * swing;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        if (arm == HumanoidArm.RIGHT) {
            this.right_arm.translateAndRotate(poseStack);
            this.right_forearm.translateAndRotate(poseStack);
            this.right_hand.translateAndRotate(poseStack);
        } else {
            this.left_arm.translateAndRotate(poseStack);
            this.left_forearm.translateAndRotate(poseStack);
            this.left_hand.translateAndRotate(poseStack);
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
    }
}
