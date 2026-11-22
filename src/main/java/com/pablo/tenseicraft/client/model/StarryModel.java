package com.pablo.tenseicraft.client.model;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.StarryEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class StarryModel extends HierarchicalModel<StarryEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "starry"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart torso;
    private final ModelPart rightLegs;
    private final ModelPart leftLegs;
    private final ModelPart tail;

    public StarryModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftEar = this.head.getChild("left_ear_r1");
        this.rightEar = this.head.getChild("right_ear_r1");
        this.torso = root.getChild("torso");
        this.rightLegs = root.getChild("right_legs");
        this.leftLegs = root.getChild("left_legs");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 50).addBox(1.0F, -8.0F, -3.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 41).addBox(-2.0F, -2.0F, -5.0F, 4.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 27).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 49).addBox(-3.0F, -8.0F, -3.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 12).addBox(-6.0F, -11.0F, 2.0F, 5.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 18).addBox(1.0F, -11.0F, 2.0F, 5.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 50).addBox(4.0F, -3.0F, -1.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 50).addBox(-6.0F, -3.0F, -1.0F, 2.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 15.0F, -3.0F));

        head.addOrReplaceChild("left_ear_r1",
                CubeListBuilder.create().texOffs(26, 49).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -5.0F, 1.0F, 0.0F, 0.0F, 0.3054F));

        head.addOrReplaceChild("right_ear_r1",
                CubeListBuilder.create().texOffs(20, 49).addBox(-0.0463F, -3.6993F, -1.0F, 2.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, -5.0F, 1.0F, 0.0F, 0.0F, -0.3054F));

        partdefinition.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 14).addBox(-4.0F, 0.0F, -1.0F, 6.0F, 5.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 49).addBox(-2.0F, 5.0F, -2.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 0).addBox(-3.0F, 4.0F, -2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 30).addBox(-4.0F, 0.0F, -2.0F, 6.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 18).addBox(-10.0F, -6.0F, 5.0F, 8.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 12).addBox(0.0F, -6.0F, 5.0F, 8.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 39).addBox(0.0F, 0.0F, 5.0F, 8.0F, 6.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 24).addBox(-10.0F, 0.0F, 5.0F, 8.0F, 6.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 15.0F, -3.0F));

        partdefinition.addOrReplaceChild("right_legs",
                CubeListBuilder.create().texOffs(20, 45).addBox(0.0F, 3.0F, 2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 45).addBox(-1.0F, 0.0F, -4.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 2).addBox(0.0F, 3.0F, -4.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 44).addBox(-1.0F, 0.0F, 2.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 19.0F, 0.0F));

        partdefinition.addOrReplaceChild("left_legs",
                CubeListBuilder.create().texOffs(48, 6).addBox(1.0F, -2.0F, 2.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 45).addBox(1.0F, -5.0F, -4.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 45).addBox(1.0F, -5.0F, 2.0F, 3.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 45).addBox(1.0F, -2.0F, -4.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        partdefinition.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -4.0F, 1.0F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 6).addBox(-2.0F, 3.0F, 2.0F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 34).addBox(-4.0F, -3.0F, 2.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 0).addBox(-2.0F, -5.0F, 2.0F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(28, 24).addBox(4.0F, -3.0F, 2.0F, 1.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 39).addBox(-2.0F, -3.0F, 8.0F, 5.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 35).addBox(-2.0F, -3.0F, 0.0F, 5.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 16.0F, 4.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(StarryEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Cabeza mira hacia el objetivo o la dirección de visión
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Movimiento tierno de orejas
        this.leftEar.zRot = 0.3054F + Mth.sin(ageInTicks * 0.12F) * 0.06F;
        this.rightEar.zRot = -0.3054F - Mth.sin(ageInTicks * 0.12F) * 0.06F;

        boolean isSitting = entity.isOrderedToSit();
        boolean isDancing = entity.isDancing();

        if (isDancing) {
            // Saltitos alegres y giros de cola al lanzar magia / curación
            float hop = Mth.abs(Mth.sin(ageInTicks * 0.45F)) * 4.0F;
            this.torso.y = 15.0F - hop;
            this.head.y = 15.0F - hop;
            this.rightLegs.y = 19.0F - hop;
            this.leftLegs.y = 24.0F - hop;
            this.tail.y = 16.0F - hop;

            this.tail.yRot = Mth.sin(ageInTicks * 0.7F) * 0.75F;
            this.head.zRot = Mth.sin(ageInTicks * 0.35F) * 0.22F;
        } else if (isSitting) {
            // Pose de sentado dulce: cola moviéndose alegremente
            this.torso.y = 17.5F;
            this.torso.xRot = -0.25F;

            this.head.y = 16.5F;
            this.head.zRot = 0.12F; // Inclinación tierna de cabeza

            this.rightLegs.y = 21.0F;
            this.rightLegs.xRot = -1.1F;

            this.leftLegs.y = 25.5F;
            this.leftLegs.xRot = -1.1F;

            this.tail.y = 18.0F;
            this.tail.xRot = 0.35F;
            this.tail.yRot = Mth.cos(ageInTicks * 0.3F) * 0.45F;
        } else {
            // Caminata tierna de 4 patitas
            this.rightLegs.xRot = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbSwingAmount;
            this.leftLegs.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.0F * limbSwingAmount;

            // Bamboleo sutil del cuerpito y respiración
            this.torso.y = 15.0F + Mth.sin(ageInTicks * 0.08F) * 0.3F + Mth.abs(Mth.sin(limbSwing * 0.6662F)) * 0.8F * limbSwingAmount;

            // Cola esponjosa que se mueve continuamente
            float tailWagSpeed = limbSwingAmount > 0.1F ? 0.4F : 0.2F;
            float tailWagAmp = limbSwingAmount > 0.1F ? 0.55F : 0.32F;
            this.tail.yRot = Mth.cos(ageInTicks * tailWagSpeed) * tailWagAmp;
            this.tail.zRot = Mth.sin(ageInTicks * 0.15F) * 0.1F;
        }
    }
}
