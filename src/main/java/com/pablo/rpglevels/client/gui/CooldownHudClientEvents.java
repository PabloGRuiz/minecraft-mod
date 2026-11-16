package com.pablo.rpglevels.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.events.DashClientEvents;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import com.pablo.rpglevels.magic.SpiritualTechnique;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Muestra iconos estilo Efecto de Estado en la esquina superior derecha para todos los cooldowns activos:
 * - Dash (icono del efecto de Velocidad tintado de naranja dorado brillante para diferenciarlo).
 * - Técnicas Espirituales (icono temático por detrás + número de técnica + segundos restantes).
 * - Liberación de la Espada Espiritual (icono de la espada + temporizador).
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class CooldownHudClientEvents {

    private static final ResourceLocation EFFECT_BACKGROUND_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/effect_background");

    private static final List<CooldownEntry> CACHED_ACTIVE_COOLDOWNS = new ArrayList<>(16);

    private record CooldownEntry(
            String badgeNumber,
            Holder<MobEffect> spriteEffect,
            ItemStack itemIcon,
            float r, float g, float b,
            int accentColor,
            long remainingTicks,
            int totalTicks
    ) {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return;
        }

        long currentTick = mc.level.getGameTime();
        List<CooldownEntry> activeCooldowns = CACHED_ACTIVE_COOLDOWNS;
        activeCooldowns.clear();

        // 1. Dash Cooldown (usa icono de Velocidad tintado de Ámbar/Naranja brillante)
        if (data.hasSkill("agility_dash")) {
            long dashRem = Math.max(
                    DashClientEvents.getRemainingCooldownTicks(currentTick),
                    data.getDashCooldownRemainingTicks(currentTick, mc.player)
            );
            if (dashRem > 0) {
                int dashTotal = Math.max(1, data.getDashCooldownTicks(mc.player));
                activeCooldowns.add(new CooldownEntry(
                        "D",
                        MobEffects.MOVEMENT_SPEED,
                        ItemStack.EMPTY,
                        1.0f, 0.58f, 0.12f,
                        0xFFFF9620,
                        dashRem,
                        dashTotal
                ));
            }
        }

        // 2. Técnicas Espirituales (Números 1-6, 10)
        for (SpiritualTechnique tech : SpiritualTechnique.values()) {
            if (data.isSkillOnCooldown(tech.getId(), currentTick)) {
                long rem = data.getSkillCooldownRemainingTicks(tech.getId(), currentTick);
                int total = Math.max(1, data.getSkillCooldownTotalTicks(tech.getId()));
                activeCooldowns.add(buildTechniqueEntry(mc, tech, rem, total));
            }
        }

        // 3. Liberación de Espada Espiritual (Cooldown nativo del ítem)
        ItemStack swordStack = ItemStack.EMPTY;
        if (mc.player.getMainHandItem().getItem() instanceof SpiritualSwordItem) {
            swordStack = mc.player.getMainHandItem();
        } else if (mc.player.getOffhandItem().getItem() instanceof SpiritualSwordItem) {
            swordStack = mc.player.getOffhandItem();
        }
        if (!swordStack.isEmpty() && mc.player.getCooldowns().isOnCooldown(swordStack.getItem())) {
            float pct = mc.player.getCooldowns().getCooldownPercent(swordStack.getItem(), 0.0f);
            if (pct > 0.01f) {
                SpiritualSwordItem.BladeRelease release = SpiritualSwordItem.getBladeRelease(swordStack);
                int total = Math.max(1, data.getGenericCooldownTicks(mc.player, release.getCooldownTicks()));
                long rem = Math.max(1L, Math.round(pct * total));
                activeCooldowns.add(new CooldownEntry(
                        "★",
                        null,
                        swordStack,
                        1.0f, 1.0f, 1.0f,
                        0xFF55FFFF,
                        rem,
                        total
                ));
            }
        }

        if (activeCooldowns.isEmpty()) {
            return;
        }

        // Calcular fila inicial debajo de los efectos vanilla activos para nunca superponerse
        boolean hasBeneficial = false;
        boolean hasHarmful = false;
        for (MobEffectInstance eff : mc.player.getActiveEffects()) {
            if (eff.showIcon()) {
                if (eff.getEffect().value().isBeneficial()) {
                    hasBeneficial = true;
                } else {
                    hasHarmful = true;
                }
            }
        }

        int baseStartY = 2;
        if (hasHarmful) {
            baseStartY = 54;
        } else if (hasBeneficial) {
            baseStartY = 28;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = guiGraphics.guiWidth();

        RenderSystem.enableBlend();
        int maxPerRow = 6;

        for (int idx = 0; idx < activeCooldowns.size(); idx++) {
            CooldownEntry entry = activeCooldowns.get(idx);
            int col = idx % maxPerRow;
            int row = idx / maxPerRow;

            int x = screenWidth - 2 - (26 * (col + 1));
            int y = baseStartY + (26 * row);

            // Fondo estilo casilla de efecto oficial de Minecraft
            guiGraphics.setColor(0.85f, 0.85f, 0.92f, 0.95f);
            guiGraphics.blitSprite(EFFECT_BACKGROUND_SPRITE, x, y, 24, 24);
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

            // Borde sutil del color de la técnica
            guiGraphics.renderOutline(x, y, 24, 24, (entry.accentColor & 0x00FFFFFF) | 0x88000000);

            // Sombra de progreso vertical (oscurece la proporción restante)
            float remRatio = Math.max(0.0f, Math.min(1.0f, (float) entry.remainingTicks / Math.max(1, entry.totalTicks)));
            int darkH = Math.round(20 * remRatio);
            if (darkH > 0) {
                guiGraphics.fill(x + 2, y + 2, x + 22, y + 2 + darkH, 0x55000000);
            }

            // Dibujo/Icono por detrás
            if (entry.spriteEffect != null) {
                TextureAtlasSprite sprite = mc.getMobEffectTextures().get(entry.spriteEffect);
                guiGraphics.setColor(entry.r, entry.g, entry.b, 0.95f);
                guiGraphics.blit(x + 3, y + 3, 0, 18, 18, sprite);
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else if (!entry.itemIcon.isEmpty()) {
                guiGraphics.renderFakeItem(entry.itemIcon, x + 4, y + 4);
            }

            // Barra inferior fina de progreso hacia completado
            int readyW = Math.round(20 * (1.0f - remRatio));
            guiGraphics.fill(x + 2, y + 21, x + 22, y + 22, 0xAA101018);
            if (readyW > 0) {
                guiGraphics.fill(x + 2, y + 21, x + 2 + readyW, y + 22, entry.accentColor);
            }

            // Número de técnica en la esquina superior izquierda (ej. 1, 2, 3, D)
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + 2.5f, y + 2.0f, 200.0f);
            guiGraphics.pose().scale(0.80f, 0.80f, 1.0f);
            guiGraphics.drawString(font, entry.badgeNumber, 0, 0, entry.accentColor, true);
            guiGraphics.pose().popPose();

            // Tiempo restante en segundos abajo al centro (ej. 14s o 3.2s)
            float remSec = entry.remainingTicks / 20.0f;
            String timeText;
            if (remSec >= 5.0f) {
                timeText = ((int) Math.ceil(remSec)) + "s";
            } else {
                int tenths = Math.max(0, (int) (remSec * 10.0f));
                timeText = (tenths / 10) + "." + (tenths % 10) + "s";
            }
            int timeW = font.width(timeText);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + 12.0f - (timeW * 0.72f) / 2.0f, y + 14.5f, 200.0f);
            guiGraphics.pose().scale(0.72f, 0.72f, 1.0f);
            guiGraphics.drawString(font, timeText, 0, 0, 0xFFFFFFFF, true);
            guiGraphics.pose().popPose();
        }

        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        activeCooldowns.clear();
    }

    private static CooldownEntry buildTechniqueEntry(Minecraft mc, SpiritualTechnique tech, long remTicks, int totalTicks) {
        return switch (tech) {
            case PHYSICAL_BOOST -> new CooldownEntry(
                    "1", MobEffects.DAMAGE_BOOST, ItemStack.EMPTY,
                    1.0f, 0.85f, 0.20f, 0xFFFFD733, remTicks, totalTicks
            );
            case LOW_HEALING -> new CooldownEntry(
                    "2", MobEffects.REGENERATION, ItemStack.EMPTY,
                    0.30f, 1.0f, 0.45f, 0xFF4DFF73, remTicks, totalTicks
            );
            case LOW_PARALIZE -> new CooldownEntry(
                    "3", MobEffects.MOVEMENT_SLOWDOWN, ItemStack.EMPTY,
                    1.0f, 0.92f, 0.22f, 0xFFFFEB38, remTicks, totalTicks
            );
            case LOW_PURIFICATION -> new CooldownEntry(
                    "4", MobEffects.GLOWING, ItemStack.EMPTY,
                    0.25f, 0.95f, 1.0f, 0xFF40F2FF, remTicks, totalTicks
            );
            case ELEMENTAL_BALL -> {
                SpiritualSwordItem.BladeElement elem = SpiritualTechnique.getPlayerElement(mc.player);
                float r = 1.0f, g = 0.35f, b = 0.25f;
                int hex = 0xFFFF5940;
                switch (elem) {
                    case GLACIES -> { r = 0.35f; g = 0.90f; b = 1.0f; hex = 0xFF59E6FF; }
                    case FULGUR -> { r = 1.0f; g = 0.92f; b = 0.25f; hex = 0xFFFFEB40; }
                    case UMBRA -> { r = 0.72f; g = 0.30f; b = 1.0f; hex = 0xFFB84DFF; }
                    case NONE -> { r = 0.45f; g = 0.75f; b = 1.0f; hex = 0xFF73BFFF; }
                    default -> {}
                }
                yield new CooldownEntry(
                        "5", MobEffects.FIRE_RESISTANCE, ItemStack.EMPTY,
                        r, g, b, hex, remTicks, totalTicks
                );
            }
            case MAGIC_AFFINITY -> new CooldownEntry(
                    "6", MobEffects.NIGHT_VISION, ItemStack.EMPTY,
                    0.85f, 0.40f, 1.0f, 0xFFD966FF, remTicks, totalTicks
            );
            case SUMMON_LESSER_SOUL -> new CooldownEntry(
                    "7", MobEffects.LUCK, ItemStack.EMPTY,
                    0.95f, 0.70f, 1.0f, 0xFFF2B3FF, remTicks, totalTicks
            );
            case FAST_HEALING -> new CooldownEntry(
                    "10", MobEffects.HEALTH_BOOST, ItemStack.EMPTY,
                    0.35f, 1.0f, 0.60f, 0xFF59FF99, remTicks, totalTicks
            );
            case CONNECT_REALITIES -> new CooldownEntry(
                    "20", MobEffects.DARKNESS, ItemStack.EMPTY,
                    0.65f, 0.20f, 0.95f, 0xFFA633F2, remTicks, totalTicks
            );
            case OPPRESSIVE_AURA -> new CooldownEntry(
                    "25", MobEffects.MOVEMENT_SLOWDOWN, ItemStack.EMPTY,
                    0.75f, 0.15f, 0.95f, 0xFFBF26F2, remTicks, totalTicks
            );
        };
    }
}

