package com.pablo.tenseicraft.client.model;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.PelotitaEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PelotitaModel extends HierarchicalModel<PelotitaEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "pelotita"), "main");

    private final ModelPart root;
    private final ModelPart izquierda;
    private final ModelPart derecha;
    private final ModelPart bb_main;

    public PelotitaModel(ModelPart root) {
        this.root = root;
        this.izquierda = root.getChild("izquierda");
        this.derecha = root.getChild("derecha");
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("izquierda",
                CubeListBuilder.create().texOffs(8, 20)
                        .addBox(3.0F, -4.0F, -3.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 24.0F, 2.0F));

        partdefinition.addOrReplaceChild("derecha",
                CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-5.0F, -4.0F, -3.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 24.0F, 2.0F));

        partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-5.0F, -13.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(PelotitaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Mirada / orientación de la cabecita esférica hacia donde mire el mob
        this.bb_main.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.bb_main.xRot = headPitch * ((float) Math.PI / 180F);

        // Animación de caminata tierna
        this.izquierda.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.derecha.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        // Bamboleo suave del cuerpito al caminar
        this.bb_main.y = 24.0F + Mth.abs(Mth.cos(limbSwing * 0.6662F)) * 0.5F * limbSwingAmount;
        this.bb_main.zRot = Mth.sin(limbSwing * 0.6662F) * 0.12F * limbSwingAmount;

        // Reacción al ser acariciado: sacude alegremente la cabeza de lado a lado y da un saltito mimoso
        if (entity.isPetting()) {
            // Bamboleo rápido y feliz de la cabeza/cuerpo (head shake)
            this.bb_main.zRot += Mth.sin(ageInTicks * 0.6F) * 0.35F;
            this.bb_main.xRot += Mth.cos(ageInTicks * 0.4F) * 0.15F;
            // Pequeño rebotecito mimoso
            this.bb_main.y = 22.5F + Mth.abs(Mth.sin(ageInTicks * 0.5F)) * 1.5F;
        }
    }
}
