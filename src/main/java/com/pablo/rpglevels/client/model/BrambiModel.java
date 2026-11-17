package com.pablo.rpglevels.client.model;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.BrambiEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class BrambiModel extends HierarchicalModel<BrambiEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "brambi"), "main");

    private final ModelPart root;
    private final ModelPart rightLegs;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart leftLegs;
    private final ModelPart hair;
    private final ModelPart bbMain;

    public BrambiModel(ModelPart root) {
        this.root = root;
        this.rightLegs = root.getChild("right_legs");
        this.tail = root.getChild("tail");
        this.head = root.getChild("head");
        this.leftHorn = this.head.getChild("left_horn");
        this.rightHorn = this.head.getChild("right_horn");
        this.leftLegs = root.getChild("left_legs");
        this.hair = root.getChild("hair");
        this.bbMain = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("right_legs",
                CubeListBuilder.create().texOffs(56, 58).addBox(-2.0F, 0.0F, 5.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 58).addBox(-2.0F, 0.0F, -8.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 15.0F, -2.0F));

        partdefinition.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(48, 7).addBox(-3.0F, -3.0F, 4.0F, 6.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 17).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 9.0F, 6.0F));

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 24).addBox(-6.0F, -9.0F, -7.0F, 10.0F, 9.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 43).addBox(4.0F, -8.0F, -5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 0).addBox(-14.0F, -8.0F, -5.0F, 8.0F, 1.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 7.0F, -7.0F));

        head.addOrReplaceChild("left_horn",
                CubeListBuilder.create().texOffs(64, 49).addBox(1.0F, -14.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(64, 17).addBox(3.0F, -17.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 50).addBox(-1.0F, -19.0F, -4.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.0F, 2.0F));

        head.addOrReplaceChild("right_horn",
                CubeListBuilder.create().texOffs(20, 62).addBox(-5.0F, -14.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 50).addBox(-9.0F, -19.0F, -4.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(12, 62).addBox(-7.0F, -17.0F, -5.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 2.0F, 2.0F));

        partdefinition.addOrReplaceChild("left_legs",
                CubeListBuilder.create().texOffs(0, 62).addBox(-2.0F, 0.0F, 5.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(32, 58).addBox(-2.0F, 0.0F, -8.0F, 3.0F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 15.0F, -2.0F));

        partdefinition.addOrReplaceChild("hair",
                CubeListBuilder.create().texOffs(60, 46).addBox(-4.0F, 1.0F, 9.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(60, 43).addBox(-4.0F, 1.0F, -7.0F, 8.0F, 3.0F, 0.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 43).addBox(-4.0F, 1.0F, -7.0F, 0.0F, 3.0F, 16.0F, new CubeDeformation(0.0F))
                        .texOffs(40, 24).addBox(4.0F, 1.0F, -7.0F, 0.0F, 3.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 11.0F, -3.0F));

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

        // Cabeza mira hacia el objetivo o la mirada del jugador
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        boolean isSitting = entity.isOrderedToSit();
        boolean isBlooming = entity.isBlooming();

        if (isBlooming) {
            // Animación majestuosa de invocación del Santuario Natural
            this.head.xRot = -0.32F + Mth.sin(ageInTicks * 0.25F) * 0.1F; // Cabeza erguida con orgullo
            this.bbMain.y = 23.0F - Mth.abs(Mth.sin(ageInTicks * 0.3F)) * 2.0F;
            this.hair.y = 10.0F - Mth.abs(Mth.sin(ageInTicks * 0.3F)) * 2.0F;

            // Astas resonando con la naturaleza
            this.leftHorn.zRot = Mth.sin(ageInTicks * 0.4F) * 0.12F;
            this.rightHorn.zRot = -Mth.sin(ageInTicks * 0.4F) * 0.12F;

            // Cola batiendo alegremente
            this.tail.yRot = Mth.sin(ageInTicks * 0.6F) * 0.55F;
        } else if (isSitting) {
            // Pose de descanso / echado en la hierba como un ciervo pacífico
            this.bbMain.y = 27.5F;
            this.hair.y = 14.5F;

            this.head.y = 11.0F;
            this.head.xRot = 0.1F;
            this.head.zRot = 0.08F; // Ladeado dulce de cabeza

            this.rightLegs.y = 21.0F;
            this.rightLegs.xRot = -1.35F;

            this.leftLegs.y = 21.0F;
            this.leftLegs.xRot = -1.35F;

            this.tail.y = 13.0F;
            this.tail.xRot = 0.35F;
            this.tail.yRot = Mth.cos(ageInTicks * 0.25F) * 0.25F;
        } else {
            // Caminata / trote elegante de venado
            this.rightLegs.xRot = Mth.cos(limbSwing * 0.6662F) * 1.1F * limbSwingAmount;
            this.leftLegs.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.1F * limbSwingAmount;

            // Galope sutil del torso y bamboleo de la melena/follaje
            this.bbMain.y = 24.0F + Mth.abs(Mth.sin(limbSwing * 0.6662F)) * 0.8F * limbSwingAmount;
            this.hair.y = 11.0F + Mth.sin(ageInTicks * 0.08F) * 0.25F + Mth.abs(Mth.sin(limbSwing * 0.6662F)) * 0.8F * limbSwingAmount;
            this.hair.zRot = Mth.sin(limbSwing * 0.6662F) * 0.08F * limbSwingAmount;

            // Respiración natural de la cabeza
            this.head.y = 7.0F + Mth.sin(ageInTicks * 0.08F) * 0.3F;

            // Cola de venado que se menea coquetamente
            this.tail.yRot = Mth.cos(ageInTicks * 0.22F) * 0.28F;
            this.tail.xRot = 0.15F + (limbSwingAmount > 0.1F ? Mth.abs(Mth.sin(limbSwing * 0.6662F)) * 0.25F : 0F);

            // Si está embistiendo o persiguiendo, baja un poco la cabeza para atacar con las astas
            if (entity.isAggressive()) {
                this.head.xRot += 0.35F;
            }
        }
    }
}
