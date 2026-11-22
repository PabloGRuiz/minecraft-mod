package com.pablo.tenseicraft.client.model;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.BrambiEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BrambiModel extends HierarchicalModel<BrambiEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "brambi"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart rightLegs;
    private final ModelPart leftLegs;
    private final ModelPart tail;
    private final ModelPart hair;
    private final ModelPart bbMain;

    public BrambiModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightEar = this.head.getChild("right_ear_r1");
        this.leftEar = this.head.getChild("left_ear_r1");
        this.leftHorn = this.head.getChild("left_horn");
        this.rightHorn = this.head.getChild("right_horn");
        this.rightLegs = root.getChild("right_legs");
        this.leftLegs = root.getChild("left_legs");
        this.tail = root.getChild("tail");
        this.hair = root.getChild("hair");
        this.bbMain = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 24).addBox(-6.0F, -9.0F, -7.0F, 10.0F, 9.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 7.0F, -7.0F));

        head.addOrReplaceChild("right_ear_r1",
                CubeListBuilder.create().texOffs(48, 0).addBox(-8.0F, -1.0F, -3.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, -8.0F, -2.0F, 0.0F, 0.0F, -0.829F));

        head.addOrReplaceChild("left_ear_r1",
                CubeListBuilder.create().texOffs(32, 43).addBox(0.0F, -1.0F, -3.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.0F, -8.0F, -2.0F, 0.0F, 0.0F, 0.829F));

        head.addOrReplaceChild("left_horn",
                CubeListBuilder.create().texOffs(16, 62).addBox(1.0F, -14.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(8, 62).addBox(3.0F, -17.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 50).addBox(-1.0F, -19.0F, -4.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.0F, 2.0F));

        head.addOrReplaceChild("right_horn",
                CubeListBuilder.create().texOffs(0, 62).addBox(-5.0F, -14.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 50).addBox(-9.0F, -19.0F, -4.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 61).addBox(-7.0F, -17.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.0F, 2.0F));

        partdefinition.addOrReplaceChild("right_legs",
                CubeListBuilder.create().texOffs(60, 43).addBox(-2.0F, -1.0F, -8.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 61).addBox(-2.0F, -1.0F, 5.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 61).addBox(-2.0F, 3.0F, -8.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 61).addBox(-2.0F, 3.0F, 5.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 15.0F, -2.0F));

        partdefinition.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(48, 7).addBox(-3.0F, -3.0F, 4.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 17).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 9.0F, 6.0F));

        partdefinition.addOrReplaceChild("hair",
                CubeListBuilder.create().texOffs(48, 58).addBox(-4.0F, 1.0F, 9.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 58).addBox(-4.0F, 1.0F, -7.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 43).addBox(-4.0F, 1.0F, -7.0F, 0.0F, 3.0F, 16.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 24).addBox(4.0F, 1.0F, -7.0F, 0.0F, 3.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 11.0F, -3.0F));

        partdefinition.addOrReplaceChild("left_legs",
                CubeListBuilder.create().texOffs(44, 61).mirror().addBox(-1.0F, 3.0F, -8.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
                        .texOffs(60, 43).mirror().addBox(-1.0F, -1.0F, -8.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
                        .texOffs(32, 61).mirror().addBox(-1.0F, -1.0F, 5.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
                        .texOffs(44, 61).mirror().addBox(-1.0F, 3.0F, 5.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false),
                PartPose.offset(1.0F, 15.0F, -2.0F));

        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -17.0F, -10.0F, 8.0F, 8.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(BrambiEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Movimiento suave y natural de orejas
        this.rightEar.zRot = -0.829F - Mth.sin(ageInTicks * 0.15F) * 0.08F;
        this.leftEar.zRot = 0.829F + Mth.sin(ageInTicks * 0.15F) * 0.08F;

        boolean isSitting = entity.isOrderedToSit();
        boolean isHeadbutting = entity.isHeadbutting();
        boolean isForaging = entity.isForaging();

        if (isHeadbutting) {
            // Animación de embestida con cabezazo hacia delante con cuernos de rama
            this.bbMain.y = 22.5F;
            this.bbMain.xRot = -0.2F;
            this.hair.y = 9.5F;
            this.hair.xRot = -0.2F;

            this.head.y = 6.0F;
            this.head.z = -9.0F;
            this.head.xRot = 0.65F;

            this.tail.y = 11.0F;
            this.tail.xRot = 0.4F;
        } else if (isForaging) {
            // Cabeza agachada al suelo hurgando y rascando la tierra
            this.head.y = 12.0F;
            this.head.xRot = 0.65F + Mth.sin(ageInTicks * 0.5F) * 0.15F;
            this.rightLegs.xRot = Mth.sin(ageInTicks * 0.6F) * 0.4F; // Pata derecha raspando la hierba
        } else if (isSitting) {
            // Pose de venado descansando pacíficamente echado en la hierba
            this.bbMain.y = 27.5F;
            this.hair.y = 14.5F;

            this.head.y = 11.0F;
            this.head.xRot = 0.1F;
            this.head.zRot = 0.08F;

            this.rightLegs.y = 21.0F;
            this.rightLegs.xRot = -1.35F;
            this.leftLegs.y = 21.0F;
            this.leftLegs.xRot = -1.35F;

            this.tail.y = 13.0F;
            this.tail.xRot = 0.35F;
            this.tail.yRot = Mth.cos(ageInTicks * 0.2F) * 0.25F;
        } else {
            // Galope / Trote majestuoso
            this.rightLegs.xRot = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
            this.leftLegs.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.1F * limbSwingAmount;

            this.bbMain.y = 24.0F + Mth.abs(Mth.sin(limbSwing * 0.6662F)) * 0.8F * limbSwingAmount;
            this.hair.y = 11.0F + Mth.sin(ageInTicks * 0.08F) * 0.25F + Mth.abs(Mth.sin(limbSwing * 0.6662F)) * 0.8F * limbSwingAmount;
            this.hair.zRot = Mth.sin(limbSwing * 0.6662F) * 0.08F * limbSwingAmount;

            this.head.y = 7.0F + Mth.sin(ageInTicks * 0.08F) * 0.3F;

            this.tail.yRot = Mth.cos(ageInTicks * 0.25F) * 0.3F;
            this.tail.xRot = 0.15F + (limbSwingAmount > 0.1F ? Mth.abs(Mth.sin(limbSwing * 0.6662F)) * 0.25F : 0F);

            // Al embestir en combate baja la cabeza para atravesar con las astas
            if (entity.isAggressive()) {
                this.head.xRot += 0.45F;
            }
        }
    }
}
