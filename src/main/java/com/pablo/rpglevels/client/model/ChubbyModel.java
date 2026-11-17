package com.pablo.rpglevels.client.model;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.ChubbyEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ChubbyModel extends HierarchicalModel<ChubbyEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "chubby"), "main");

    private final ModelPart root;
    private final ModelPart cuerpo;
    private final ModelPart horn;
    private final ModelPart bone;
    private final ModelPart rightArm;
    private final ModelPart nubeCola;
    private final ModelPart leftArm;

    public ChubbyModel(ModelPart root) {
        this.root = root;
        this.cuerpo = root.getChild("cuerpo");
        this.horn = this.cuerpo.getChild("horn_r1");
        this.bone = this.cuerpo.getChild("bone");
        this.rightArm = this.cuerpo.getChild("right_arm");
        this.nubeCola = this.cuerpo.getChild("nube_cola");
        this.leftArm = this.cuerpo.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition cuerpo = partdefinition.addOrReplaceChild("cuerpo", CubeListBuilder.create(), PartPose.offset(0.0F, 15.0F, 0.0F));

        cuerpo.addOrReplaceChild("horn_r1",
                CubeListBuilder.create().texOffs(60, 56).addBox(-1.0F, -8.0F, -2.0F, 0.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -5.0F, -4.0F, 0.6981F, 0.0F, 0.0F));

        cuerpo.addOrReplaceChild("bone",
                CubeListBuilder.create().texOffs(44, 11).addBox(-5.0F, -6.0F, -6.0F, 10.0F, 9.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 23).addBox(5.0F, -6.0F, -5.0F, 1.0F, 9.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-5.0F, -7.0F, -5.0F, 10.0F, 11.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(36, 44).addBox(-5.0F, -6.0F, 5.0F, 10.0F, 9.0F, 3.0F, new CubeDeformation(0.0F))
                        .texOffs(26, 23).addBox(-6.0F, -6.0F, -5.0F, 1.0F, 9.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 0).addBox(-4.0F, 4.0F, -4.0F, 8.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 44).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        cuerpo.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(52, 42).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(62, 47).addBox(-3.0F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-6.0F, -1.0F, 1.0F));

        cuerpo.addOrReplaceChild("nube_cola",
                CubeListBuilder.create().texOffs(52, 23).addBox(-5.0F, -3.0F, 1.0F, 10.0F, 8.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(20, 55).addBox(-3.0F, 0.0F, 5.0F, 6.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 56).addBox(-2.0F, 1.0F, 9.0F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(62, 42).addBox(-1.0F, 0.0F, 11.0F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(36, 56).addBox(-2.0F, 2.0F, 7.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 55).addBox(-4.0F, -1.0F, 3.0F, 8.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(52, 33).addBox(-5.0F, -4.0F, 0.0F, 10.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 8.0F));

        cuerpo.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(62, 50).addBox(1.0F, -1.0F, 0.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(48, 62).addBox(1.0F, -1.0F, -2.0F, 3.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 0.0F, 1.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(ChubbyEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Flotación suave y etérea en el aire
        float hoverBob = Mth.sin(ageInTicks * 0.12F) * 2.2F;
        this.cuerpo.y = 15.0F + hoverBob;

        // Orientación de la nubecita hacia donde mira
        this.cuerpo.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.cuerpo.xRot = headPitch * ((float) Math.PI / 180F);

        // Brazitos que aletean como alas de vapor
        float armFlap = Mth.sin(ageInTicks * 0.22F) * 0.25F;
        this.rightArm.zRot = armFlap;
        this.leftArm.zRot = -armFlap;
        this.rightArm.yRot = Mth.cos(ageInTicks * 0.15F) * 0.15F;
        this.leftArm.yRot = -Mth.cos(ageInTicks * 0.15F) * 0.15F;

        // Cola de nube que ondea como estela de tormenta
        this.nubeCola.yRot = Mth.sin(ageInTicks * 0.16F) * 0.22F;
        this.nubeCola.zRot = Mth.cos(ageInTicks * 0.12F) * 0.12F;
        this.nubeCola.xRot = Mth.sin(limbSwing * 0.5F) * 0.1F * limbSwingAmount;

        // Antena de relámpago con pequeñas vibraciones electrostáticas
        this.horn.zRot = Mth.sin(ageInTicks * 0.35F) * 0.12F;

        // Si está en combate o lanzando rayos: vibración eléctrica
        if (entity.isChargingLightning()) {
            this.cuerpo.x = Mth.sin(ageInTicks * 1.5F) * 0.8F;
            this.rightArm.zRot = -0.6F;
            this.leftArm.zRot = 0.6F;
            this.horn.xRot = 0.6981F + Mth.sin(ageInTicks * 0.8F) * 0.3F;
        }

        // Si fue acariciado: gira alegremente en pirueta de 360° con flotación emocionada
        if (entity.isExcitedSpin()) {
            this.cuerpo.yRot += ageInTicks * 0.95F;
            this.cuerpo.y = 12.0F + Mth.sin(ageInTicks * 0.4F) * 3.5F;
            this.rightArm.zRot = -0.75F + Mth.sin(ageInTicks * 0.6F) * 0.3F;
            this.leftArm.zRot = 0.75F - Mth.sin(ageInTicks * 0.6F) * 0.3F;
            this.nubeCola.yRot = Mth.sin(ageInTicks * 0.8F) * 0.4F;
        }
    }
}
