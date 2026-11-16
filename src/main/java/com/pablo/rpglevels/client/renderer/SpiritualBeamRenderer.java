package com.pablo.rpglevels.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.SpiritualBeamEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Renderizador de haz de energía continuo (Estilo Kamehameha / Cero / Dragon Ball)
 * para la técnica "Spiritual Beam".
 * Proyecta cilindros concéntricos de energía con texturas fluidas de ki en rotación,
 * núcleo blanco brillante de alta densidad y orbes caóticos de impacto o choque.
 */
public class SpiritualBeamRenderer extends EntityRenderer<SpiritualBeamEntity> {

    public static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/misc/ki_aura.png");

    public SpiritualBeamRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(SpiritualBeamEntity entity) {
        return BEAM_TEXTURE;
    }

    @Override
    public void render(SpiritualBeamEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float length = entity.getBeamLength();
        if (length <= 0.25F) return;

        poseStack.pushPose();

        // Orientar el haz en el espacio 3D según el Yaw y Pitch de la entidad (alineado con la mirada hacia adelante)
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));

        int colorRgb = entity.getColorRgb();
        float r = ((colorRgb >> 16) & 0xFF) / 255.0F;
        float g = ((colorRgb >> 8) & 0xFF) / 255.0F;
        float b = (colorRgb & 0xFF) / 255.0F;

        long gameTime = entity.level().getGameTime();
        float time = gameTime + partialTick;
        float baseRadius = entity.getBeamRadius();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(BEAM_TEXTURE));

        // 1. Núcleo Interno Súper Denso (Blanco / color muy claro de alta energía)
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 6.5F));
        float coreR = baseRadius * 0.35F;
        float coreWhiteR = Math.min(1.0F, r * 0.5F + 0.5F);
        float coreWhiteG = Math.min(1.0F, g * 0.5F + 0.5F);
        float coreWhiteB = Math.min(1.0F, b * 0.5F + 0.5F);
        renderHorizontalCylinder(consumer, poseStack.last().pose(), coreR, length, -time * 0.18F,
                coreWhiteR, coreWhiteG, coreWhiteB, 0.85F);
        poseStack.popPose();

        // 2. Capa Intermedia de Flujo de Aura (Color puro del jugador)
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 4.2F));
        float midR = baseRadius * 0.75F;
        renderHorizontalCylinder(consumer, poseStack.last().pose(), midR, length, -time * 0.12F,
                r, g, b, 0.50F);
        poseStack.popPose();

        // 3. Capa Exterior Translúcida Difusa (Aura pulsante envolvente)
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.8F));
        float pulse = 1.0F + Mth.sin(time * 0.4F) * 0.08F;
        float outerR = baseRadius * 1.15F * pulse;
        renderHorizontalCylinder(consumer, poseStack.last().pose(), outerR, length, -time * 0.08F,
                r, g, b, 0.28F);
        poseStack.popPose();

        // 4. Esfera de Impacto / Choque al final del haz
        poseStack.pushPose();
        poseStack.translate(0, 0, length);
        float sphereRadius = entity.isClashing()
                ? (baseRadius * 2.5F * (1.0F + Mth.sin(time * 0.6F) * 0.14F))
                : (baseRadius * 1.4F);
        renderEnergySphere(consumer, poseStack, sphereRadius, time, r, g, b);
        poseStack.popPose();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderHorizontalCylinder(VertexConsumer consumer, Matrix4f matrix, float radius, float length,
                                                 float vOffset, float r, float g, float b, float alpha) {
        int segments = 16;
        float angleStep = (float) (2 * Math.PI / segments);
        float vRepeat = Math.max(1.0F, length / 4.0F);

        for (int i = 0; i < segments; i++) {
            float a1 = i * angleStep;
            float a2 = (i + 1) * angleStep;

            float x1 = Mth.sin(a1) * radius;
            float y1 = Mth.cos(a1) * radius;
            float x2 = Mth.sin(a2) * radius;
            float y2 = Mth.cos(a2) * radius;

            float u1 = (float) i / segments;
            float u2 = (float) (i + 1) / segments;
            float v1 = vOffset;
            float v2 = vOffset + vRepeat;

            // Cara exterior
            addVertex(consumer, matrix, x1, y1, 0, r, g, b, alpha, u1, v1);
            addVertex(consumer, matrix, x2, y2, 0, r, g, b, alpha, u2, v1);
            addVertex(consumer, matrix, x2, y2, length, r, g, b, alpha, u2, v2);
            addVertex(consumer, matrix, x1, y1, length, r, g, b, alpha, u1, v2);

            // Cara interior (visible para quien esté dentro del haz)
            addVertex(consumer, matrix, x1, y1, length, r, g, b, alpha, u1, v2);
            addVertex(consumer, matrix, x2, y2, length, r, g, b, alpha, u2, v2);
            addVertex(consumer, matrix, x2, y2, 0, r, g, b, alpha, u2, v1);
            addVertex(consumer, matrix, x1, y1, 0, r, g, b, alpha, u1, v1);
        }
    }

    private static void renderEnergySphere(VertexConsumer consumer, PoseStack poseStack, float radius,
                                           float time, float r, float g, float b) {
        int rings = 8;
        int sectors = 12;
        float ringStep = (float) (Math.PI / rings);
        float sectorStep = (float) (2 * Math.PI / sectors);

        // Capa rotatoria 1
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 4.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 3.0F));
        Matrix4f mat1 = poseStack.last().pose();

        for (int i = 0; i < rings; i++) {
            float lat0 = (float) (Math.PI * (-0.5 + (double) i / rings));
            float z0 = Mth.sin(lat0) * radius;
            float zr0 = Mth.cos(lat0) * radius;

            float lat1 = (float) (Math.PI * (-0.5 + (double) (i + 1) / rings));
            float z1 = Mth.sin(lat1) * radius;
            float zr1 = Mth.cos(lat1) * radius;

            for (int j = 0; j < sectors; j++) {
                float lng0 = j * sectorStep;
                float x0 = Mth.sin(lng0);
                float y0 = Mth.cos(lng0);

                float lng1 = (j + 1) * sectorStep;
                float x1 = Mth.sin(lng1);
                float y1 = Mth.cos(lng1);

                addVertex(consumer, mat1, x0 * zr0, y0 * zr0, z0, r, g, b, 0.45F, 0, 0);
                addVertex(consumer, mat1, x1 * zr0, y1 * zr0, z0, r, g, b, 0.45F, 1, 0);
                addVertex(consumer, mat1, x1 * zr1, y1 * zr1, z1, r, g, b, 0.45F, 1, 1);
                addVertex(consumer, mat1, x0 * zr1, y0 * zr1, z1, r, g, b, 0.45F, 0, 1);
            }
        }
        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z,
                                  float r, float g, float b, float a, float u, float v) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
