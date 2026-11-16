package com.pablo.rpglevels.client.gui;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renders player health stacked into a single row of 10 hearts with color-coded tiers
 * (Scaling Health style), along with a multiplier counter badge (e.g. 2x, 3x) and numerical HP.
 * Extra temporary hearts like Absorption render cleanly on an extra row above.
 * Uses official Minecraft GUI Atlas sprites via blitSprite for 100% compatibility with Sodium & Iris.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class CustomHealthBarClientEvents {

    // Native vanilla GUI sprites in the GUI Atlas
    private static final ResourceLocation SPRITE_CONTAINER =
            ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation SPRITE_CONTAINER_HARDCORE =
            ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore");
    private static final ResourceLocation SPRITE_VANILLA_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation SPRITE_VANILLA_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final ResourceLocation SPRITE_ABSORBING_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full");
    private static final ResourceLocation SPRITE_ABSORBING_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half");
    private static final ResourceLocation SPRITE_POISONED_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_full");
    private static final ResourceLocation SPRITE_POISONED_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/poisoned_half");
    private static final ResourceLocation SPRITE_WITHERED_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/withered_full");
    private static final ResourceLocation SPRITE_WITHERED_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/withered_half");
    private static final ResourceLocation SPRITE_FROZEN_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_full");
    private static final ResourceLocation SPRITE_FROZEN_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/frozen_half");

    // Grayscale heart sprites stitched into the mod's GUI atlas for multi-tier color tinting
    private static final ResourceLocation SPRITE_MOD_TINTED_FULL =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "hud/heart/full");
    private static final ResourceLocation SPRITE_MOD_TINTED_HALF =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "hud/heart/half");

    // Color Tiers (10 colors, cycling if higher)
    public record HeartColor(float r, float g, float b, int rgbHex) {}

    private static final HeartColor[] COLOR_TIERS = new HeartColor[] {
            new HeartColor(1.0f, 0.15f, 0.15f, 0xFFFF2222),  // 0: Red (1x, 0-20 HP)
            new HeartColor(1.0f, 0.55f, 0.0f, 0xFFFF8C00),   // 1: Orange (2x, 20-40 HP)
            new HeartColor(1.0f, 0.88f, 0.0f, 0xFFFFE000),   // 2: Yellow (3x, 40-60 HP)
            new HeartColor(0.15f, 0.88f, 0.15f, 0xFF26E026),  // 3: Green (4x, 60-80 HP)
            new HeartColor(0.0f, 0.85f, 1.0f, 0xFF00D9FF),   // 4: Cyan (5x, 80-100 HP)
            new HeartColor(0.25f, 0.45f, 1.0f, 0xFF4073FF),  // 5: Blue (6x, 100-120 HP)
            new HeartColor(0.65f, 0.25f, 1.0f, 0xFFA640FF),  // 6: Purple (7x, 120-140 HP)
            new HeartColor(1.0f, 0.35f, 0.75f, 0xFFFF59BF),  // 7: Pink (8x, 140-160 HP)
            new HeartColor(0.9f, 0.95f, 1.0f, 0xFFE6F2FF),   // 8: Platinum (9x, 160-180 HP)
            new HeartColor(1.0f, 0.75f, 0.15f, 0xFFFFC026)   // 9: Golden Crimson (10x, 180-200 HP)
    };

    private static final RandomSource random = RandomSource.create();

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) {
            return;
        }

        // Cancel vanilla player health rendering
        event.setCanceled(true);

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;

        float maxHp = player.getMaxHealth();
        float currentHp = player.getHealth();
        float absorption = player.getAbsorptionAmount();

        boolean isHardcore = player.level().getLevelData().isHardcore();
        boolean isPoisoned = player.hasEffect(MobEffects.POISON);
        boolean isWithered = player.hasEffect(MobEffects.WITHER);
        boolean isFrozen = player.isFullyFrozen();

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();

        // Calculate position based on NeoForge's leftHeight (starts at 39)
        int left = screenWidth / 2 - 91;
        int top = screenHeight - mc.gui.leftHeight;

        // Advance leftHeight so ARMOR renders on the row above (Row 2, screenHeight - 49)!
        mc.gui.leftHeight += 10;
        if (absorption > 0.0f) {
            mc.gui.leftHeight += 10;
        }

        // Calculate layer counts (each layer represents 20 HP = 10 hearts)
        int totalLayers = Math.max(1, (int) Math.ceil(maxHp / 20.0f));
        int activeLayer = (int) Math.floor((currentHp - 0.0001f) / 20.0f);
        if (activeLayer < 0) activeLayer = 0;
        if (activeLayer >= totalLayers) activeLayer = totalLayers - 1;

        int currentBars = Math.max(1, (int) Math.ceil(currentHp / 20.0f));
        HeartColor activeColor = getColorForLayer(activeLayer);

        // 1. Draw Multiplier Badge on the Left (e.g. "4x", "3x", "2x", "1x")
        if (totalLayers > 1) {
            String badge = currentBars + "x";
            int badgeW = font.width(badge);
            int badgeX = left - badgeW - 3;
            int badgeY = top + 1;
            guiGraphics.drawString(font, badge, badgeX, badgeY, activeColor.rgbHex(), true);
        }

        // 2. Numerical HP Indicator on the Left (e.g. "§c30/90")
        int curHpInt = Math.round(currentHp);
        int maxHpInt = Math.round(maxHp);
        String hpText = "§c" + curHpInt + "§7/§c" + maxHpInt;
        int hpTextW = font.width(hpText);
        int hpTextX = (totalLayers > 1) ? (left - font.width(currentBars + "x") - hpTextW - 7) : (left - hpTextW - 4);
        guiGraphics.drawString(font, hpText, hpTextX, top + 1, 0xFFFFFFFF, true);

        // 3. Render Single Row of 10 Hearts
        float layerHealth = Math.max(0.0f, currentHp - (activeLayer * 20.0f));
        int displayHealth = Mth.ceil(layerHealth);

        HeartColor currentColor = getColorForLayer(activeLayer);
        HeartColor previousColor = (activeLayer > 0) ? getColorForLayer(activeLayer - 1) : null;

        // Low health shake
        boolean isShaking = currentHp <= 4.0f;

        ResourceLocation containerSprite = isHardcore ? SPRITE_CONTAINER_HARDCORE : SPRITE_CONTAINER;

        for (int i = 0; i < 10; i++) {
            int heartX = left + (i * 8);
            int heartY = top;
            if (isShaking) {
                heartY += random.nextInt(2);
            }

            int heartValue = (i + 1) * 2;

            // Step A: Draw Heart Container
            guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            guiGraphics.blitSprite(containerSprite, heartX, heartY, 9, 9);

            // Step B: Previous layer background for missing health
            if (previousColor != null && displayHealth < heartValue) {
                if (activeLayer == 1) {
                    // Previous is layer 0 (vanilla red)
                    guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                    guiGraphics.blitSprite(SPRITE_VANILLA_FULL, heartX, heartY, 9, 9);
                } else {
                    guiGraphics.setColor(previousColor.r(), previousColor.g(), previousColor.b(), 1.0f);
                    guiGraphics.blitSprite(SPRITE_MOD_TINTED_FULL, heartX, heartY, 9, 9);
                }
            }

            // Step C: Render active layer health on top
            if (isPoisoned) {
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                if (displayHealth >= heartValue) {
                    guiGraphics.blitSprite(SPRITE_POISONED_FULL, heartX, heartY, 9, 9);
                } else if (displayHealth == heartValue - 1) {
                    guiGraphics.blitSprite(SPRITE_POISONED_HALF, heartX, heartY, 9, 9);
                }
            } else if (isWithered) {
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                if (displayHealth >= heartValue) {
                    guiGraphics.blitSprite(SPRITE_WITHERED_FULL, heartX, heartY, 9, 9);
                } else if (displayHealth == heartValue - 1) {
                    guiGraphics.blitSprite(SPRITE_WITHERED_HALF, heartX, heartY, 9, 9);
                }
            } else if (isFrozen) {
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                if (displayHealth >= heartValue) {
                    guiGraphics.blitSprite(SPRITE_FROZEN_FULL, heartX, heartY, 9, 9);
                } else if (displayHealth == heartValue - 1) {
                    guiGraphics.blitSprite(SPRITE_FROZEN_HALF, heartX, heartY, 9, 9);
                }
            } else if (activeLayer == 0) {
                // Tier 0: Vanilla native red sprites
                guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                if (displayHealth >= heartValue) {
                    guiGraphics.blitSprite(SPRITE_VANILLA_FULL, heartX, heartY, 9, 9);
                } else if (displayHealth == heartValue - 1) {
                    guiGraphics.blitSprite(SPRITE_VANILLA_HALF, heartX, heartY, 9, 9);
                }
            } else {
                // Tier 1+: Modded tinted sprites
                guiGraphics.setColor(currentColor.r(), currentColor.g(), currentColor.b(), 1.0f);
                if (displayHealth >= heartValue) {
                    guiGraphics.blitSprite(SPRITE_MOD_TINTED_FULL, heartX, heartY, 9, 9);
                } else if (displayHealth == heartValue - 1) {
                    guiGraphics.blitSprite(SPRITE_MOD_TINTED_HALF, heartX, heartY, 9, 9);
                }
            }
        }

        // Reset color multiplier
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 4. Render Absorption Hearts on an Extra Row Above (top - 10)
        if (absorption > 0.0f) {
            int absorbRowY = top - 10;
            int absorbHearts = Mth.ceil(absorption / 2.0f);
            int cappedHearts = Math.min(10, absorbHearts);

            for (int i = 0; i < cappedHearts; i++) {
                int absorbX = left + (i * 8);
                int absorbVal = (i + 1) * 2;

                // Container
                guiGraphics.blitSprite(containerSprite, absorbX, absorbRowY, 9, 9);

                // Golden Absorption Heart
                if (absorption >= absorbVal) {
                    guiGraphics.blitSprite(SPRITE_ABSORBING_FULL, absorbX, absorbRowY, 9, 9);
                } else {
                    guiGraphics.blitSprite(SPRITE_ABSORBING_HALF, absorbX, absorbRowY, 9, 9);
                }
            }
        }
    }

    private static HeartColor getColorForLayer(int layerIndex) {
        if (layerIndex < 0) layerIndex = 0;
        return COLOR_TIERS[layerIndex % COLOR_TIERS.length];
    }
}
