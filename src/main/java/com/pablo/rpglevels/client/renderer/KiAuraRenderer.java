package com.pablo.rpglevels.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import com.pablo.rpglevels.mobs.MobRankHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderizador de Auras de Ki estilo Dragon Ball:
 * - Geometría esférica-cónica multicapa (abultada en el torso como una esfera de energía y cónica en la punta).
 * - Alta translucidez etérea y brillo radiante emisivo (glow).
 * - Crecimiento dinámico y exponencial según el stat de Aura (Poder del Alma).
 * - Visión del Alma: Al activar Afinidad Mágica, el lanzador percibe las auras de todos los enemigos según su rango.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class KiAuraRenderer {

    public static final ResourceLocation AURA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/misc/ki_aura.png");

    public record ActiveAura(long expireTick, float auraPower, float r, float g, float b) {}
    private static final Map<Integer, ActiveAura> ACTIVE_AURAS = new ConcurrentHashMap<>();

    public record ActiveOppressiveAura(long startTick, long expireTick, float maxRadius, float auraPower, float r, float g, float b) {}
    private static final Map<Integer, ActiveOppressiveAura> ACTIVE_OPPRESSIVE_AURAS = new ConcurrentHashMap<>();

    private record CachedSenseAura(long tick, float auraPower, float r, float g, float b) {}
    private static final Map<Integer, CachedSenseAura> SENSE_AURA_CACHE = new ConcurrentHashMap<>();

    public static void setEntityAura(int entityId, long expireTick, float auraPower, float r, float g, float b) {
        ACTIVE_AURAS.put(entityId, new ActiveAura(expireTick, auraPower, r, g, b));
    }

    public static void setOppressiveAura(int entityId, long startTick, long expireTick, float maxRadius, float auraPower, float r, float g, float b) {
        ACTIVE_OPPRESSIVE_AURAS.put(entityId, new ActiveOppressiveAura(startTick, expireTick, maxRadius, auraPower, r, g, b));
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || !entity.isAlive()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long currentTick = mc.level.getGameTime();
        float partialTick = event.getPartialTick();

        ActiveAura auraInfo = ACTIVE_AURAS.get(entity.getId());
        boolean shouldRender = false;
        float auraPower = 5.0f;
        float r = 1.0f, g = 0.85f, b = 0.2f;

        if (auraInfo != null) {
            if (auraInfo.expireTick() > currentTick) {
                shouldRender = true;
                auraPower = auraInfo.auraPower();
                r = auraInfo.r();
                g = auraInfo.g();
                b = auraInfo.b();
            } else {
                ACTIVE_AURAS.remove(entity.getId());
            }
        }

        // Si es un jugador, verificar si su aura está activa y leer su stat en tiempo real
        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                if (data.isAuraVisible(currentTick) || shouldRender) {
                    shouldRender = true;
                    // Consultar siempre las estadísticas en vivo del jugador
                    auraPower = Math.max(auraPower, data.getAuraStat(player));
                    float[] col = getPlayerAuraRgb(player);
                    r = col[0]; g = col[1]; b = col[2];
                }
            }
        }

        // --- SENTIDO DE KI / VISIÓN DEL ALMA (AFINIDAD MÁGICA) ---
        if (!shouldRender) {
            PlayerRpgData clientData = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (clientData != null && clientData.hasAuraSenseActive(currentTick)) {
                if (entity != mc.player && entity.distanceToSqr(mc.player) <= 36.0 * 36.0) {
                    shouldRender = true;
                    CachedSenseAura cached = SENSE_AURA_CACHE.get(entity.getId());
                    if (cached != null && currentTick - cached.tick() < 20L) {
                        auraPower = cached.auraPower();
                        r = cached.r();
                        g = cached.g();
                        b = cached.b();
                    } else {
                        auraPower = com.pablo.rpglevels.data.AuraStatHelper.calculateEntityAura(entity);
                        if (entity instanceof Enemy || entity instanceof Monster) {
                            ThreatLevel threat = MobRankHandler.getThreatLevel(entity);
                            float[] tCol = getThreatAuraRgb(threat);
                            r = tCol[0]; g = tCol[1]; b = tCol[2];
                        } else if (entity instanceof Player otherPlayer) {
                            float[] pCol = getPlayerAuraRgb(otherPlayer);
                            r = pCol[0]; g = pCol[1]; b = pCol[2];
                        } else {
                            // Criaturas pacíficas / animales / aldeanos
                            r = 0.35f; g = 0.90f; b = 0.45f;
                        }
                        if (SENSE_AURA_CACHE.size() > 200) {
                            SENSE_AURA_CACHE.entrySet().removeIf(e -> currentTick - e.getValue().tick() >= 60L);
                        }
                        SENSE_AURA_CACHE.put(entity.getId(), new CachedSenseAura(currentTick, auraPower, r, g, b));
                    }
                }
            }
        }

        if (shouldRender) {
            renderKiAura(event.getPoseStack(), event.getMultiBufferSource(), entity, currentTick, partialTick, auraPower, r, g, b);
        }

        // --- AURA OPRESIVA (BARRERA CILÍNDRICA EN EXPANSIÓN) ---
        ActiveOppressiveAura oppAura = ACTIVE_OPPRESSIVE_AURAS.get(entity.getId());
        if (oppAura == null && entity instanceof Player player) {
            PlayerRpgData pData = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (pData != null && pData.isOppressiveAuraActive(currentTick)) {
                float aStat = pData.getAuraStat(player);
                float rad = (float) Math.min(24.0, 6.0 + (aStat * 0.10));
                float[] rgb = getPlayerAuraRgb(player);
                if (rgb[0] == 1.0f && rgb[1] == 0.85f && rgb[2] == 0.20f) {
                    rgb = new float[]{0.68f, 0.15f, 1.0f}; // Púrpura espiritual profundo
                }
                oppAura = new ActiveOppressiveAura(currentTick - 10, pData.getOppressiveAuraUntilTick(), rad, aStat, rgb[0], rgb[1], rgb[2]);
            }
        }

        if (oppAura != null) {
            if (oppAura.expireTick() > currentTick) {
                renderOppressiveAuraCylinder(event.getPoseStack(), event.getMultiBufferSource(), entity, currentTick, partialTick, oppAura);
            } else {
                ACTIVE_OPPRESSIVE_AURAS.remove(entity.getId());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) {
            return; // En 3ª persona ya lo dibuja RenderLivingEvent
        }

        long currentTick = mc.level.getGameTime();
        ActiveOppressiveAura oppAura = ACTIVE_OPPRESSIVE_AURAS.get(mc.player.getId());
        if (oppAura == null) {
            PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null && data.isOppressiveAuraActive(currentTick)) {
                float auraStat = data.getAuraStat(mc.player);
                float radius = (float) Math.min(24.0, 6.0 + (auraStat * 0.10));
                float[] rgb = getPlayerAuraRgb(mc.player);
                if (rgb[0] == 1.0f && rgb[1] == 0.85f && rgb[2] == 0.20f) {
                    rgb = new float[]{0.68f, 0.15f, 1.0f};
                }
                oppAura = new ActiveOppressiveAura(currentTick - 10, data.getOppressiveAuraUntilTick(), radius, auraStat, rgb[0], rgb[1], rgb[2]);
            }
        }

        if (oppAura != null && oppAura.expireTick() > currentTick) {
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            net.minecraft.world.phys.Vec3 camPos = event.getCamera().getPosition();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            double px = Mth.lerp(partialTick, mc.player.xOld, mc.player.getX()) - camPos.x;
            double py = Mth.lerp(partialTick, mc.player.yOld, mc.player.getY()) - camPos.y;
            double pz = Mth.lerp(partialTick, mc.player.zOld, mc.player.getZ()) - camPos.z;
            poseStack.translate(px, py, pz);

            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            renderOppressiveAuraCylinder(poseStack, bufferSource, mc.player, currentTick, partialTick, oppAura);
            bufferSource.endBatch(RenderType.entityTranslucentEmissive(AURA_TEXTURE));

            poseStack.popPose();
        }
    }

    private static void renderKiAura(PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity,
                                     long gameTime, float partialTick, float auraPower, float r, float g, float b) {
        poseStack.pushPose();

        float baseWidth = Math.max(0.6f, entity.getBbWidth() * 0.85f);
        float baseHeight = Math.max(1.5f, entity.getBbHeight());

        // Escala exponencial visible con el stat de Aura (crecimiento sustancial al subir stats)
        float powerRatio = (float) Math.pow(Math.max(1.0f, auraPower), 0.55);
        float auraRadius = baseWidth * (0.80f + powerRatio * 0.28f);
        float auraHeight = baseHeight * (0.95f + powerRatio * 0.32f);

        // Pulsación sutil y viva del aura de ki
        float pulse = 1.0f + Mth.sin((gameTime + partialTick) * 0.35F) * 0.05F;
        auraRadius *= pulse;

        // Si la Resonancia Espiritual está activa, el aura se exalta de forma colosal
        boolean isResonance = false;
        if (entity instanceof Player p) {
            PlayerRpgData pData = p.getData(ModAttachments.PLAYER_RPG_DATA);
            if (pData != null && pData.isSpiritualResonanceActive(gameTime)) {
                isResonance = true;
            }
        }
        if (isResonance) {
            auraRadius *= 1.45f;
            auraHeight *= 1.55f;
            float extraPulse = 1.0f + Mth.sin((gameTime + partialTick) * 0.65F) * 0.08F;
            auraRadius *= extraPulse;
        }

        // Desplazamiento de textura vertical continuo rápido (flujo ascendente)
        float vOffset = -((gameTime + partialTick) * 0.11F) % 1.0F;

        // Rotación continua del aura en el eje Y
        float rotAngle = (gameTime + partialTick) * 2.8F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotAngle));

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(AURA_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();

        // 1. Capa Exterior: Malla Esférica-Cónica translúcida y envolvente (Alpha 0.32)
        renderSphericalConicalMesh(consumer, matrix, auraRadius, auraHeight, vOffset, r, g, b, 0.32f);

        // 2. Capa Interior: Núcleo esférico más denso y concentrado (Alpha 0.42)
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotAngle * 2.2F));
        Matrix4f coreMatrix = poseStack.last().pose();
        float coreV = -((gameTime + partialTick) * 0.17F) % 1.0F;
        float coreR = Math.min(1.0f, r * 1.25f);
        float coreG = Math.min(1.0f, g * 1.25f);
        float coreB = Math.min(1.0f, b * 1.25f);
        renderSphericalConicalMesh(consumer, coreMatrix, auraRadius * 0.75f, auraHeight * 0.90f, coreV, coreR, coreG, coreB, 0.42f);

        poseStack.popPose();
    }

    /**
     * Construye una malla 3D de 5 anillos latitudinales que recrea la forma icónica de Dragon Ball:
     * - Anillo 0 (Suelo): ajustado a los pies (radio 0.60x)
     * - Anillo 1 (Rodillas/Piernas): expansión rápida (radio 1.15x)
     * - Anillo 2 (Cintura/Torso): abultamiento esférico máximo de energía (radio 1.40x)
     * - Anillo 3 (Cabeza/Hombros): curvatura cónica hacia adentro (radio 0.95x)
     * - Anillo 4 (Punta de la llama): corona cónica en punta (radio 0.20x)
     */
    private static void renderSphericalConicalMesh(VertexConsumer consumer, Matrix4f matrix, float radius, float height,
                                                  float vOffset, float r, float g, float b, float alpha) {
        int segments = 14;
        float angleStep = (float) (2 * Math.PI / segments);

        // Alturas relativas y radios de los 5 anillos de deformación
        float[] ringY = {
                0.02f * height,
                0.24f * height,
                0.52f * height,
                0.82f * height,
                1.18f * height
        };
        float[] ringR = {
                radius * 0.58f,
                radius * 1.15f,
                radius * 1.42f, // Máxima esfera en el centro
                radius * 0.92f,
                radius * 0.22f  // Cúpula en punta cónica
        };

        for (int ring = 0; ring < ringY.length - 1; ring++) {
            float yBot = ringY[ring];
            float yTop = ringY[ring + 1];
            float rBot = ringR[ring];
            float rTop = ringR[ring + 1];

            float vBot = vOffset + (1.0f - (yBot / (height * 1.18f)));
            float vTop = vOffset + (1.0f - (yTop / (height * 1.18f)));

            for (int i = 0; i < segments; i++) {
                float a1 = i * angleStep;
                float a2 = (i + 1) * angleStep;

                float x1Bot = Mth.sin(a1) * rBot;
                float z1Bot = Mth.cos(a1) * rBot;
                float x2Bot = Mth.sin(a2) * rBot;
                float z2Bot = Mth.cos(a2) * rBot;

                float x1Top = Mth.sin(a1) * rTop;
                float z1Top = Mth.cos(a1) * rTop;
                float x2Top = Mth.sin(a2) * rTop;
                float z2Top = Mth.cos(a2) * rTop;

                float u1 = (float) i / segments;
                float u2 = (float) (i + 1) / segments;

                // Cara exterior
                addVertex(consumer, matrix, x1Bot, yBot, z1Bot, r, g, b, alpha, u1, vBot);
                addVertex(consumer, matrix, x2Bot, yBot, z2Bot, r, g, b, alpha, u2, vBot);
                addVertex(consumer, matrix, x2Top, yTop, z2Top, r, g, b, alpha, u2, vTop);
                addVertex(consumer, matrix, x1Top, yTop, z1Top, r, g, b, alpha, u1, vTop);

                // Cara interior (visible desde dentro del aura)
                addVertex(consumer, matrix, x1Top, yTop, z1Top, r, g, b, alpha, u1, vTop);
                addVertex(consumer, matrix, x2Top, yTop, z2Top, r, g, b, alpha, u2, vTop);
                addVertex(consumer, matrix, x2Bot, yBot, z2Bot, r, g, b, alpha, u2, vBot);
                addVertex(consumer, matrix, x1Bot, yBot, z1Bot, r, g, b, alpha, u1, vBot);
            }
        }
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

    public static float[] getPlayerAuraRgb(Player player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            held = player.getOffhandItem();
        }
        if (held.getItem() instanceof SpiritualSwordItem) {
            if (SpiritualSwordItem.isSenbonzakura(held)) {
                return new float[]{1.0f, 0.65f, 0.85f}; // Rosa pétalo de cerezo (Sakura)
            }
            SpiritualSwordItem.BladeElement elem = SpiritualSwordItem.getBladeElement(held);
            return switch (elem) {
                case IGNIS -> new float[]{1.0f, 0.50f, 0.08f};   // Fuego dorado/naranja
                case FULGUR -> new float[]{1.0f, 0.95f, 0.18f};  // Rayo amarillo
                case GLACIES -> new float[]{0.25f, 0.88f, 1.0f}; // Hielo celeste
                case UMBRA -> new float[]{0.68f, 0.15f, 1.0f};   // Sombra púrpura
                default -> new float[]{0.35f, 0.82f, 1.0f};      // Azul espiritual
            };
        }
        return new float[]{1.0f, 0.85f, 0.20f}; // Dorado Super Saiyajin clásico
    }

    public static float[] getThreatAuraRgb(ThreatLevel threat) {
        if (threat == null) return new float[]{0.7f, 0.7f, 0.7f};
        return switch (threat) {
            case F -> new float[]{0.75f, 0.75f, 0.75f};
            case E -> new float[]{0.25f, 0.90f, 0.35f};
            case D -> new float[]{0.15f, 0.85f, 0.95f};
            case C -> new float[]{0.20f, 0.55f, 1.0f};
            case B -> new float[]{0.70f, 0.15f, 0.95f}; // Púrpura Hollow / Voidborn
            case A -> new float[]{0.95f, 0.20f, 0.85f};
            case HAZARD -> new float[]{1.0f, 0.75f, 0.05f};
            case CALAMITY -> new float[]{0.95f, 0.15f, 0.15f};
            case DISASTER, GRAND_DISASTER -> new float[]{0.65f, 0.0f, 0.0f};
            default -> new float[]{1.0f, 0.95f, 0.40f};
        };
    }

    public static float getThreatAuraPower(ThreatLevel threat) {
        if (threat == null) return 5.0f;
        return switch (threat) {
            case F -> 5.0f;
            case E -> 12.0f;
            case D -> 22.0f;
            case C -> 40.0f;
            case B -> 75.0f;
            case A -> 110.0f;
            case HAZARD -> 150.0f;
            case CALAMITY, DISASTER, GRAND_DISASTER -> 220.0f;
            default -> 300.0f;
        };
    }

    public static void renderOppressiveAuraCylinder(PoseStack poseStack, MultiBufferSource bufferSource, LivingEntity entity,
                                                    long gameTime, float partialTick, ActiveOppressiveAura oppAura) {
        poseStack.pushPose();

        float elapsed = Math.max(0.0f, (float) (gameTime - oppAura.startTick()) + partialTick);
        float progress = Math.min(1.0f, elapsed / 22.0f); // Se expande horizontalmente en ~1.1s
        float easeOut = (float) (1.0 - Math.pow(1.0 - progress, 3));
        float currentRadius = 1.0f + (oppAura.maxRadius() - 1.0f) * easeOut;

        // Pulsación constante de gravedad / presión espiritual
        float pulse = 1.0f + Mth.sin((gameTime + partialTick) * 0.45f) * 0.035f;
        float r = currentRadius * pulse;
        float height = Math.max(6.5f, entity.getBbHeight() + 5.0f);

        // Desplazamiento vertical de energía hacia el cielo
        float vOffset = -((gameTime + partialTick) * 0.07f) % 1.0f;
        float rotAngle = (gameTime + partialTick) * 1.6f;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(AURA_TEXTURE));

        // 1. Capa Exterior del Cilindro (Gran barrera translúcida)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotAngle));
        Matrix4f matOuter = poseStack.last().pose();
        renderCylinderWall(consumer, matOuter, r, height, vOffset, oppAura.r(), oppAura.g(), oppAura.b(), 0.32f);
        poseStack.popPose();

        // 2. Capa Interior Contrarrotante (Gradiente de densidad)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotAngle * 1.4f));
        Matrix4f matInner = poseStack.last().pose();
        renderCylinderWall(consumer, matInner, r * 0.94f, height * 0.95f, -vOffset * 1.3f,
                Math.min(1.0f, oppAura.r() * 1.15f), Math.min(1.0f, oppAura.g() * 1.15f), Math.min(1.0f, oppAura.b() * 1.15f), 0.20f);
        poseStack.popPose();

        // 3. Anillo de Presión en el Suelo (Expansión de onda de choque)
        Matrix4f matFloor = poseStack.last().pose();
        renderFloorPressureRing(consumer, matFloor, r, oppAura.r(), oppAura.g(), oppAura.b(), 0.40f);

        poseStack.popPose();
    }

    private static void renderCylinderWall(VertexConsumer consumer, Matrix4f matrix, float radius, float height,
                                           float vOffset, float r, float g, float b, float alpha) {
        int segments = 24;
        float angleStep = (float) (2 * Math.PI / segments);
        float yBot = -0.1f;
        float yTop = height;

        float vBot = vOffset + 1.0f;
        float vTop = vOffset;

        for (int i = 0; i < segments; i++) {
            float a1 = i * angleStep;
            float a2 = (i + 1) * angleStep;

            float x1 = Mth.sin(a1) * radius;
            float z1 = Mth.cos(a1) * radius;
            float x2 = Mth.sin(a2) * radius;
            float z2 = Mth.cos(a2) * radius;

            float u1 = (float) i / segments;
            float u2 = (float) (i + 1) / segments;

            // Cara exterior
            addVertex(consumer, matrix, x1, yBot, z1, r, g, b, alpha, u1, vBot);
            addVertex(consumer, matrix, x2, yBot, z2, r, g, b, alpha, u2, vBot);
            addVertex(consumer, matrix, x2, yTop, z2, r, g, b, alpha * 0.4f, u2, vTop);
            addVertex(consumer, matrix, x1, yTop, z1, r, g, b, alpha * 0.4f, u1, vTop);

            // Cara interior (visible desde dentro del radio cilíndrico)
            addVertex(consumer, matrix, x1, yTop, z1, r, g, b, alpha * 0.4f, u1, vTop);
            addVertex(consumer, matrix, x2, yTop, z2, r, g, b, alpha * 0.4f, u2, vTop);
            addVertex(consumer, matrix, x2, yBot, z2, r, g, b, alpha, u2, vBot);
            addVertex(consumer, matrix, x1, yBot, z1, r, g, b, alpha, u1, vBot);
        }
    }

    private static void renderFloorPressureRing(VertexConsumer consumer, Matrix4f matrix, float radius,
                                                float r, float g, float b, float alpha) {
        int segments = 24;
        float angleStep = (float) (2 * Math.PI / segments);
        float rInner = Math.max(0.1f, radius * 0.85f);
        float rOuter = radius * 1.02f;
        float y = 0.05f;

        for (int i = 0; i < segments; i++) {
            float a1 = i * angleStep;
            float a2 = (i + 1) * angleStep;

            float x1In = Mth.sin(a1) * rInner;
            float z1In = Mth.cos(a1) * rInner;
            float x2In = Mth.sin(a2) * rInner;
            float z2In = Mth.cos(a2) * rInner;

            float x1Out = Mth.sin(a1) * rOuter;
            float z1Out = Mth.cos(a1) * rOuter;
            float x2Out = Mth.sin(a2) * rOuter;
            float z2Out = Mth.cos(a2) * rOuter;

            // Quad horizontal en el suelo
            addVertex(consumer, matrix, x1In, y, z1In, r, g, b, alpha, 0.0f, 0.0f);
            addVertex(consumer, matrix, x2In, y, z2In, r, g, b, alpha, 1.0f, 0.0f);
            addVertex(consumer, matrix, x2Out, y, z2Out, r, g, b, 0.0f, 1.0f, 1.0f);
            addVertex(consumer, matrix, x1Out, y, z1Out, r, g, b, 0.0f, 0.0f, 1.0f);
        }
    }
}
