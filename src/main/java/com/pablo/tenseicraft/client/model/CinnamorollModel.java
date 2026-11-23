package com.pablo.tenseicraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.CinnamorollEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Modelo 3D animado de Cinnamoroll (exportado de Blockbench con pivotes anatómicos calibrados):
 * - Flotación suave en el aire estilo nubecita.
 * - Aleteo adorable de sus enormes orejas blancas (más rápido al volar o celebrar).
 * - Ladeo tierno de cabeza, bracitos saludando, patitas balanceándose y colita en espiral moviéndose feliz.
 * - Pose acurrucada al sentarse a dormir la siesta y giro alegre al recibir dulces o caricias.
 */
public class CinnamorollModel extends EntityModel<CinnamorollEntity> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "cinnamoroll"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart left_ear;
    private final ModelPart right_ear;
    private final ModelPart right_arm;
    private final ModelPart left_arm;
    private final ModelPart tail;
    private final ModelPart right_leg;
    private final ModelPart left_leg;

    public CinnamorollModel(ModelPart root) {
        this.root = root.getChild("root");
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");
        this.left_ear = this.head.getChild("left_ear");
        this.right_ear = this.head.getChild("right_ear");
        ModelPart brazos = this.body.getChild("brazos");
        this.right_arm = brazos.getChild("derecha_r1");
        this.left_arm = brazos.getChild("izquierda_r1");
        this.tail = this.body.getChild("tail");
        this.right_leg = this.body.getChild("right_leg");
        this.left_leg = this.body.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Nodo raíz para flotación suave, inclinación al volar y giros felices
        PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, -4.0F));

        // Cabeza pivoteada exactamente en el cuello (0.5, -17.0, 4.5) para mirar y ladearse con ternura
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 10).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(0, 67).addBox(5.5F, -7.0F, -4.5F, 1.0F, 6.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(44, 26).addBox(-5.5F, -9.0F, -3.5F, 11.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(32, 53).addBox(4.5F, -8.0F, -4.5F, 1.0F, 7.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(52, 53).addBox(-5.5F, -8.0F, -4.5F, 1.0F, 7.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(68, 34).addBox(-6.5F, -7.0F, -4.5F, 1.0F, 6.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(46, 75).addBox(6.5F, -5.0F, -4.5F, 1.0F, 3.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(66, 75).addBox(-7.5F, -5.0F, -4.5F, 1.0F, 3.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-8.5F, -5.0F, -3.5F, 17.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 28).addBox(-7.5F, -7.0F, -3.5F, 15.0F, 2.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(48, 0).addBox(-5.5F, -1.0F, -3.5F, 11.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(36, 18).addBox(-6.5F, -8.0F, -3.5F, 13.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(36, 10).addBox(-7.5F, -2.0F, -3.5F, 15.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 51).addBox(-4.5F, -10.0F, -3.5F, 9.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(44, 34).addBox(-2.5F, -10.0F, -4.5F, 5.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(20, 87).addBox(-2.5F, -10.0F, 0.5F, 5.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 59).addBox(-3.5F, -11.0F, -3.5F, 7.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.5F, -17.0F, 4.5F));

        // Oreja izquierda pivoteada en la base superior de la cabeza (4.5, -9.0, 0.0)
        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(80, 14).addBox(1.0F, 0.0F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 90).addBox(2.0F, 1.0F, -2.5F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(80, 30).addBox(4.0F, 1.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(98, 61).addBox(5.0F, 2.0F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(68, 97).addBox(0.0F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(14, 92).addBox(3.0F, 2.0F, -2.5F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(20, 69).addBox(3.0F, 3.0F, -2.5F, 9.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(38, 87).addBox(0.0F, 0.0F, -2.5F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(76, 18).addBox(5.0F, 6.0F, -2.5F, 7.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(80, 24).addBox(6.0F, 7.0F, -2.5F, 6.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(48, 69).addBox(3.0F, 4.0F, -2.5F, 9.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(72, 61).addBox(4.0F, 5.0F, -2.5F, 8.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(84, 0).addBox(8.0F, 8.0F, -2.5F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(88, 40).addBox(12.0F, 4.0F, -2.5F, 1.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(38, 99).addBox(12.0F, 3.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(12, 98).addBox(13.0F, 4.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(46, 99).addBox(12.0F, 8.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(100, 18).addBox(2.0F, 2.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(100, 30).addBox(1.0F, 1.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(56, 93).addBox(-1.0F, -2.0F, -2.5F, 1.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(4.5F, -9.0F, 0.0F));

        // Oreja derecha pivoteada en la base superior opuesta (-4.5, -9.0, 0.0)
        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(92, 14).addBox(-4.0F, 0.0F, -1.5F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(28, 93).addBox(-4.0F, 1.0F, -2.5F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(54, 87).addBox(-3.0F, 0.0F, -2.5F, 3.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(80, 97).addBox(-3.0F, -1.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(42, 93).addBox(-5.0F, 2.0F, -2.5F, 2.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(100, 34).addBox(-5.0F, 1.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(28, 99).addBox(-7.0F, 2.0F, -1.5F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(72, 49).addBox(-12.0F, 3.0F, -2.5F, 9.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(72, 55).addBox(-12.0F, 4.0F, -2.5F, 9.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(20, 75).addBox(-12.0F, 5.0F, -2.5F, 8.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(20, 81).addBox(-12.0F, 7.0F, -2.5F, 6.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(76, 67).addBox(-12.0F, 6.0F, -2.5F, 7.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(86, 82).addBox(-12.0F, 8.0F, -2.5F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(82, 88).addBox(-13.0F, 4.0F, -2.5F, 1.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(100, 38).addBox(-13.0F, 3.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(20, 98).addBox(-14.0F, 4.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(100, 42).addBox(-13.0F, 8.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(100, 46).addBox(-3.0F, 2.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(100, 50).addBox(-2.0F, 1.0F, -1.5F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(94, 88).addBox(0.0F, -2.0F, -2.5F, 1.0F, 3.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-4.5F, -9.0F, 0.0F));

        // Bracitos (ya pivoteados en los hombros)
        PartDefinition brazos = body.addOrReplaceChild("brazos", CubeListBuilder.create(), PartPose.ZERO);

        brazos.addOrReplaceChild("derecha_r1",
                CubeListBuilder.create().texOffs(88, 30).addBox(0.0F, 0.0F, -2.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-5.0F, -15.0F, 5.0F, 0.0F, 0.0F, 0.7418F));

        brazos.addOrReplaceChild("izquierda_r1",
                CubeListBuilder.create().texOffs(70, 87).addBox(-3.0F, 0.0F, -2.0F, 3.0F, 7.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(6.0F, -15.0F, 5.0F, 0.0F, 0.0F, -0.7418F));

        // Torso
        body.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(36, 37).addBox(-4.0F, -16.0F, 1.0F, 9.0F, 9.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 82).addBox(-4.0F, -15.0F, 8.0F, 9.0F, 7.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 37).addBox(-5.0F, -15.0F, 1.0F, 11.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(80, 8).addBox(-3.0F, -17.0F, 2.0F, 7.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.ZERO);

        // Colita en rollo de canela separada para menearse con felicidad
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(86, 73).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.5F, -10.5F, 9.0F));

        // Patitas separadas para patalear dulcemente al flotar y caminar
        body.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 96).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.5F, -8.0F, 4.5F));

        body.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(94, 96).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.5F, -8.0F, 4.5F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(CinnamorollEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        boolean sitting = entity.isInSittingPose();
        boolean happy = entity.isHappySpinning();
        boolean gliding = !sitting && !entity.onGround();
        float speed = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);

        // Reset base (28.0F apoya exactamente sus patitas en el piso ya que terminan en y = -4.0F)
        this.root.y = 28.0F;
        this.root.xRot = 0.0F;
        this.root.yRot = 0.0F;
        this.root.zRot = 0.0F;

        if (sitting) {
            // Pose de siesta acurrucada en el piso
            this.root.y = 29.5F + Mth.sin(ageInTicks * 0.08F) * 0.15F;
            this.head.xRot = 0.18F + Mth.sin(ageInTicks * 0.08F) * 0.04F;
            this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.4F;
            this.head.zRot = 0.12F;

            // Orejitas relajadas y caídas suavemente
            float sleepBreath = Mth.sin(ageInTicks * 0.08F) * 0.04F;
            this.left_ear.zRot = 0.25F + sleepBreath;
            this.right_ear.zRot = -0.25F - sleepBreath;
            this.left_ear.xRot = 0.0F;
            this.right_ear.xRot = 0.0F;

            // Patitas hacia adelante como perrito sentado
            this.right_leg.xRot = -1.15F;
            this.left_leg.xRot = -1.15F;
            this.right_arm.zRot = 0.55F;
            this.left_arm.zRot = -0.55F;
            this.right_arm.xRot = -0.25F;
            this.left_arm.xRot = -0.25F;
            this.tail.yRot = Mth.sin(ageInTicks * 0.1F) * 0.12F;
            return;
        }

        // Mirada y ladeo tierno de cabeza
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) * 0.75F;
        this.head.xRot = headPitch * ((float) Math.PI / 180.0F) * 0.5F;
        this.head.zRot = Mth.sin(ageInTicks * 0.09F) * 0.07F;

        if (happy) {
            // Levita un poquito y hace un giro feliz al recibir dulces o caricias
            this.root.y = 24.5F + Mth.sin(ageInTicks * 0.45F) * 1.4F;
            this.root.yRot = (ageInTicks * 0.42F) % ((float) Math.PI * 2.0F);
            float flap = Mth.sin(ageInTicks * 0.55F) * 0.48F;
            this.left_ear.zRot = -0.10F + flap;
            this.right_ear.zRot = 0.10F - flap;
            this.left_ear.xRot = 0.0F;
            this.right_ear.xRot = 0.0F;

            float armWave = Mth.sin(ageInTicks * 0.5F) * 0.45F;
            this.right_arm.zRot = 0.7418F + armWave;
            this.left_arm.zRot = -0.7418F - armWave;
            this.right_arm.xRot = -0.2F;
            this.left_arm.xRot = -0.2F;

            float legKick = Mth.sin(ageInTicks * 0.45F) * 0.35F;
            this.right_leg.xRot = legKick;
            this.left_leg.xRot = -legKick;
            this.tail.yRot = Mth.sin(ageInTicks * 0.75F) * 0.45F;
            return;
        }

        if (gliding) {
            // Planeo suave en el aire batiendo rápido sus orejitas
            this.root.y = 26.5F + Mth.sin(ageInTicks * 0.3F) * 0.6F;
            this.root.xRot = 0.14F;
            float flap = Mth.sin(ageInTicks * 0.58F) * 0.45F;
            this.left_ear.zRot = -0.12F + flap;
            this.right_ear.zRot = 0.12F - flap;
            this.left_ear.xRot = 0.12F;
            this.right_ear.xRot = 0.12F;

            this.right_arm.zRot = 0.7418F + 0.15F;
            this.left_arm.zRot = -0.7418F - 0.15F;
            this.right_arm.xRot = -0.25F;
            this.left_arm.xRot = -0.25F;

            float kick = Mth.sin(ageInTicks * 0.35F) * 0.28F;
            this.right_leg.xRot = 0.22F + kick;
            this.left_leg.xRot = 0.22F - kick;
            this.tail.yRot = Mth.sin(ageInTicks * 0.45F) * 0.28F;
            return;
        }

        // Caminata tierna en el suelo: pasitos cortos, ligero rebote y orejas acompañando el paso
        float walkCycle = Mth.cos(limbSwing * 0.9F);
        float walkBounce = Math.abs(Mth.sin(limbSwing * 0.9F)) * speed * 1.1F;
        this.root.y = 28.0F - walkBounce;
        this.root.zRot = walkCycle * speed * 0.08F;

        // Patitas dando pasitos alternados
        this.right_leg.xRot = walkCycle * 0.85F * speed;
        this.left_leg.xRot = -walkCycle * 0.85F * speed;

        // Bracitos balanceándose al caminar (y respiración sutil al estar quieto)
        float idleBreath = Mth.sin(ageInTicks * 0.12F) * 0.05F;
        this.right_arm.xRot = -walkCycle * 0.65F * speed;
        this.left_arm.xRot = walkCycle * 0.65F * speed;
        this.right_arm.zRot = 0.7418F + idleBreath;
        this.left_arm.zRot = -0.7418F - idleBreath;

        // Orejitas rebotando dulcemente con cada paso (y suave vaivén cuando está quieto)
        float earBounce = Mth.sin(ageInTicks * 0.14F) * 0.08F + Math.abs(Mth.sin(limbSwing * 0.9F)) * speed * 0.22F;
        this.left_ear.zRot = 0.06F - earBounce;
        this.right_ear.zRot = -0.06F + earBounce;
        this.left_ear.xRot = speed * 0.10F;
        this.right_ear.xRot = speed * 0.10F;

        // Colita en espiral meneándose feliz
        this.tail.yRot = Mth.sin(ageInTicks * 0.26F) * (0.18F + speed * 0.22F);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
