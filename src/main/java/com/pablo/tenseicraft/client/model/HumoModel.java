package com.pablo.tenseicraft.client.model;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Modelo 3D de Humo místico estilo Gear 5 de Luffy.
 * Flota y ondea alrededor de la cabeza del jugador.
 */
public class HumoModel<T extends LivingEntity> extends HierarchicalModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "humo"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/humo.png");

    private final ModelPart root;
    private final ModelPart bloques;
    private final ModelPart bloques_internos;
    private final ModelPart bloques_externos;
    private final ModelPart planos;

    public HumoModel(ModelPart root) {
        this.root = root.getChild("root");
        this.bloques = this.root.getChild("bloques");
        this.bloques_internos = this.bloques.getChild("bloques_internos");
        this.bloques_externos = this.bloques.getChild("bloques_externos");
        this.planos = this.root.getChild("planos");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);

        // Se desplaza en Y +12.0F para centrar verticalmente el modelo en su origen (hombros/cuello)
        PartDefinition bloques = root.addOrReplaceChild("bloques", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition bloques_internos = bloques.addOrReplaceChild("bloques_internos", CubeListBuilder.create()
                .texOffs(0, 30).addBox(5.0F, -11.0F, -4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(20, 11).addBox(-5.0F, -11.0F, -4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(26, 22).addBox(-1.0F, -13.0F, 5.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 0).addBox(5.0F, -12.0F, -3.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-5.0F, -12.0F, -3.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        bloques_internos.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(20, 6).addBox(0.3827F, -1.0F, -1.9239F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, -11.0F, 3.0F, 0.0F, 0.3054F, 0.0F));

        bloques_internos.addOrReplaceChild("cube_r2",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.3827F, -1.0F, -1.9239F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.0F, -11.0F, 3.0F, 0.0F, -0.3054F, 0.0F));

        bloques_internos.addOrReplaceChild("cube_r3",
                CubeListBuilder.create().texOffs(24, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -12.0F, 6.0F, 0.0F, 0.5672F, 0.0F));

        bloques_internos.addOrReplaceChild("cube_r4",
                CubeListBuilder.create().texOffs(24, 18).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, -12.0F, 6.0F, 0.0F, -0.5672F, 0.0F));

        PartDefinition bloques_externos = bloques.addOrReplaceChild("bloques_externos", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-6.0F, -12.0F, -3.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(4, 30).addBox(-6.0F, -11.0F, -4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(8, 27).addBox(-1.0F, -13.0F, 6.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(12, 14).addBox(6.0F, -12.0F, -3.0F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(30, 5).addBox(6.0F, -11.0F, -4.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        bloques_externos.addOrReplaceChild("cube_r5",
                CubeListBuilder.create().texOffs(24, 11).addBox(-0.3827F, -1.0F, -1.9239F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.0F, -11.0F, 3.0F, 0.0F, -0.3054F, 0.0F));

        bloques_externos.addOrReplaceChild("cube_r6",
                CubeListBuilder.create().texOffs(26, 20).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -12.0F, 6.0F, 0.0F, 0.5672F, 0.0F));

        bloques_externos.addOrReplaceChild("cube_r7",
                CubeListBuilder.create().texOffs(0, 25).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, -12.0F, 6.0F, 0.0F, -0.5672F, 0.0F));

        bloques_externos.addOrReplaceChild("cube_r8",
                CubeListBuilder.create().texOffs(0, 20).addBox(-0.6173F, -1.0F, -1.9239F, 1.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, -11.0F, 3.0F, 0.0F, 0.3054F, 0.0F));

        PartDefinition planos = root.addOrReplaceChild("planos", CubeListBuilder.create()
                .texOffs(22, 27).addBox(6.0F, -11.0F, -5.0F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(24, 27).addBox(6.0F, -12.0F, -4.0F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 6).addBox(6.0F, -13.0F, -3.0F, 0.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(16, 27).addBox(-1.0F, -14.0F, 6.0F, 3.0F, 4.0F, 0.0F, new CubeDeformation(0.0F))
                .texOffs(10, 6).addBox(-5.0F, -13.0F, -3.0F, 0.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(26, 28).addBox(-5.0F, -12.0F, -4.0F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(28, 28).addBox(-5.0F, -11.0F, -5.0F, 0.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, 0.0F));

        planos.addOrReplaceChild("cube_r9",
                CubeListBuilder.create().texOffs(18, 20).addBox(0.3827F, -1.0F, -1.9239F, 0.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, -12.0F, 3.0F, 0.0F, 0.3054F, 0.0F));

        planos.addOrReplaceChild("cube_r10",
                CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.0F, -11.0F, 6.0F, 0.0F, -0.5236F, 0.0F));

        planos.addOrReplaceChild("cube_r11",
                CubeListBuilder.create().texOffs(26, 25).addBox(-2.0F, -2.0F, -1.0F, 4.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -11.0F, 6.0F, 0.0F, 0.48F, 0.0F));

        planos.addOrReplaceChild("cube_r12",
                CubeListBuilder.create().texOffs(10, 20).addBox(-0.3827F, -1.0F, -1.9239F, 0.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.0F, -12.0F, 3.0F, 0.0F, -0.3054F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Suave flotación y oleaje del humo místico de Gear 5
        float bob = Mth.sin(ageInTicks * 0.08F) * 0.4F;
        float subtleRot = Mth.sin(ageInTicks * 0.04F) * 0.04F;

        this.root.y = bob;
        this.root.yRot = subtleRot;
        this.root.zRot = Mth.cos(ageInTicks * 0.05F) * 0.02F;
    }
}
