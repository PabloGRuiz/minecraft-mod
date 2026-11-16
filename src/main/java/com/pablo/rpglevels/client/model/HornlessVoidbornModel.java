package com.pablo.rpglevels.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.HornlessVoidbornEntity;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class HornlessVoidbornModel<T extends HornlessVoidbornEntity> extends HierarchicalModel<T> implements ArmedModel {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "hornless_voidborn"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart hueco;
    private final ModelPart right_leg;
    private final ModelPart right_foot;
    private final ModelPart left_leg;
    private final ModelPart left_foot;
    private final ModelPart left_arm;
    private final ModelPart left_hand;
    private final ModelPart right_arm;
    private final ModelPart right_hand;
    private final ModelPart garras;

    public HornlessVoidbornModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.torso = root.getChild("torso");
        this.hueco = this.torso.getChild("hueco");
        this.right_leg = root.getChild("right_leg");
        this.right_foot = this.right_leg.getChild("right_foot");
        this.left_leg = root.getChild("left_leg");
        this.left_foot = this.left_leg.getChild("left_foot");
        this.left_arm = root.getChild("left_arm");
        this.left_hand = this.left_arm.getChild("left_hand");
        this.right_arm = root.getChild("right_arm");
        this.right_hand = this.right_arm.getChild("right_hand");
        this.garras = this.right_hand.getChild("garras");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(64, 51).addBox(-1.0F, -6.0F, -2.0F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 14).addBox(-4.0F, -13.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(54, 20).addBox(-4.0F, -12.0F, -5.0F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 42).addBox(-3.0F, -11.0F, -6.0F, 6.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(62, 35).addBox(-2.0F, -11.0F, -8.0F, 4.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(64, 43).addBox(-2.0F, -11.0F, -10.0F, 4.0F, 6.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(48, 68).addBox(-1.0F, -10.0F, -12.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(72, 21).addBox(-1.0F, -9.0F, -13.0F, 2.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -16.0F, 0.0F));

        PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(32, 14).addBox(-5.0F, 9.0F, -2.0F, 10.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-6.0F, -9.0F, -3.0F, 12.0F, 9.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -8.0F, 0.0F));

        PartDefinition hueco = torso.addOrReplaceChild("hueco", CubeListBuilder.create()
                .texOffs(68, 0).addBox(-3.0F, -1.0F, -3.0F, 0.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(34, 8).addBox(-2.0F, -2.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(34, 8).addBox(-2.0F, 2.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(34, 8).mirror().addBox(2.0F, 2.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(34, 8).mirror().addBox(2.0F, -2.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
                .texOffs(68, 0).mirror().addBox(3.0F, -1.0F, -3.0F, 0.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(0.0F, -5.0F, 0.0F));

        hueco.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(64, 60).addBox(1.0F, -3.0F, -3.0F, 0.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 4.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        hueco.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(34, 8).mirror().addBox(-1.0F, -3.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 1.5708F));
        hueco.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(34, 8).mirror().addBox(-1.0F, -3.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.5708F));
        hueco.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(64, 60).addBox(1.0F, -3.0F, -3.0F, 0.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, -1.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        hueco.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(34, 8).addBox(1.0F, -3.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.5708F));
        hueco.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(34, 8).addBox(1.0F, -3.0F, -3.0F, 0.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, -1.5708F));

        PartDefinition right_leg = partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(56, 0).addBox(-1.0F, 10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(12, 59).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 3.0F, -1.0F));

        right_leg.addOrReplaceChild("right_foot", CubeListBuilder.create()
                .texOffs(64, 69).addBox(-2.0F, 1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(56, 68).addBox(2.0F, 1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(32, 20).addBox(-2.0F, 0.0F, -1.0F, 5.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(8, 72).addBox(0.0F, 1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 19.0F, -2.0F));

        PartDefinition left_leg = partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(24, 59).addBox(-1.0F, 10.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(36, 59).addBox(-1.0F, 0.0F, -1.0F, 3.0F, 10.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.0F, 3.0F, -1.0F));

        left_leg.addOrReplaceChild("left_foot", CubeListBuilder.create()
                .texOffs(72, 13).addBox(-3.0F, 1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(16, 72).addBox(1.0F, 1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(34, 0).addBox(-3.0F, 0.0F, -1.0F, 5.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(72, 17).addBox(-1.0F, 1.0F, -3.0F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 19.0F, -2.0F));

        PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(0, 42).addBox(0.0F, 12.0F, -1.0F, 3.0F, 15.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(50, 28).addBox(0.0F, -1.0F, -1.0F, 3.0F, 13.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(6.0F, -15.0F, -1.0F));

        left_arm.addOrReplaceChild("left_hand", CubeListBuilder.create()
                .texOffs(48, 60).addBox(-2.0F, 0.0F, -3.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(8, 67).addBox(0.0F, 2.0F, 1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(72, 72).addBox(0.0F, 2.0F, -1.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 73).addBox(0.0F, 2.0F, -3.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(4, 73).addBox(-2.0F, 2.0F, -2.0F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 26.0F, 1.0F));

        PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(30, 30).addBox(-4.0F, 12.0F, -3.0F, 5.0F, 14.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(52, 44).addBox(-3.0F, -1.0F, -2.0F, 3.0F, 13.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-6.0F, -15.0F, 0.0F));

        PartDefinition right_hand = right_arm.addOrReplaceChild("right_hand", CubeListBuilder.create()
                .texOffs(0, 30).addBox(-4.0F, -1.0F, -4.0F, 7.0F, 4.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 26.0F, -1.0F));

        right_hand.addOrReplaceChild("garras", CubeListBuilder.create()
                .texOffs(36, 72).addBox(2.0F, 0.0F, -2.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(40, 72).addBox(1.0F, 0.0F, 3.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(44, 72).addBox(2.0F, 0.0F, 0.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(32, 72).addBox(2.0F, 0.0F, -4.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(28, 72).addBox(-2.0F, 0.0F, -4.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 72).addBox(-4.0F, 0.0F, -2.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(8, 60).addBox(-4.0F, 0.0F, 1.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(26, 42).addBox(-3.0F, 0.0F, 3.0F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 3.0F, 0.0F));

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

        this.right_leg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.25F * limbSwingAmount;
        this.left_leg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.25F * limbSwingAmount;

        this.right_arm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 0.9F * limbSwingAmount - 0.18F;
        this.left_arm.xRot = Mth.cos(limbSwing * 0.6662F) * 0.9F * limbSwingAmount - 0.12F;

        this.right_arm.zRot = 0.08F + Mth.cos(ageInTicks * 0.09F) * 0.04F;
        this.left_arm.zRot = -0.08F - Mth.cos(ageInTicks * 0.09F) * 0.04F;
        this.torso.xRot = Mth.sin(ageInTicks * 0.07F) * 0.03F;

        // 1. Invocación de Lluvia de Proyectiles (alza ambos brazos hacia el cielo)
        int rainTicks = entity.getRainTicks();
        if (rainTicks > 0) {
            float pulse = Mth.sin(ageInTicks * 0.45F) * 0.12F;
            this.right_arm.xRot = -2.65F + pulse;
            this.left_arm.xRot = -2.65F - pulse;
            this.right_arm.zRot = 0.25F;
            this.left_arm.zRot = -0.25F;
            this.head.xRot = -0.45F;
            this.torso.xRot = -0.15F;
            return;
        }

        // 2. Disparo directo de proyectil desde el brazo/garra
        int shootTicks = entity.getShootTicks();
        if (shootTicks > 0) {
            float progress = shootTicks / 10.0F;
            this.right_arm.xRot = -1.55F + this.head.xRot - (0.35F * progress);
            this.right_arm.yRot = -0.10F + this.head.yRot;
            this.right_hand.xRot = 0.30F * progress;
            this.torso.yRot = -0.20F * progress;
            return;
        }

        // 3. Animación de zarpazo si golpea de cerca
        if (this.attackTime > 0.0F) {
            float swing = Mth.sin(this.attackTime * (float) Math.PI);
            this.right_arm.xRot = -1.65F * swing;
            this.right_arm.yRot = -0.35F * swing;
            this.right_hand.xRot = 0.50F * swing;
            this.torso.yRot = -0.25F * swing;
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        this.root.translateAndRotate(poseStack);
        if (arm == HumanoidArm.RIGHT) {
            this.right_arm.translateAndRotate(poseStack);
            this.right_hand.translateAndRotate(poseStack);
            poseStack.translate(0.0F, -0.55F, -0.10F);
        } else {
            this.left_arm.translateAndRotate(poseStack);
            this.left_hand.translateAndRotate(poseStack);
            poseStack.translate(0.0F, -0.55F, -0.10F);
        }
    }
}
