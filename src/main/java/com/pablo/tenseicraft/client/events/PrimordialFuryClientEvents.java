package com.pablo.tenseicraft.client.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.network.PrimordialFuryComboPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cliente para Furia Primordial:
 * 1. Detección 100% con Click Izquierdo (sin conflicto con escudo ni Better Combat):
 *    - Saltar / En el aire + Click Izq -> Caída Destructiva
 *    - Correr + Click Izq -> Golpe Sísmico
 *    - Mantener Click Izq (~0.5s en suelo) -> Tajo Ascendente
 *    - Click Izq normal -> Onda Celestial (corte de presión en media luna)
 * 2. Barra de carga estilo "Modo Tachy" en el HUD.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class PrimordialFuryClientEvents {

    private static final int HOLD_TICKS_FOR_ASCENDING_SLASH = 10; // ~0.5 segundos manteniendo Click Izq
    private static boolean wasAttackDown = false;
    private static int leftClickHoldTicks = 0;
    private static long lastComboSentTick = 0L;
    private static long lastSlashSentTick = 0L;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            wasAttackDown = false;
            leftClickHoldTicks = 0;
            return;
        }

        PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.isDenseSoul() || !data.isPrimordialFuryTrained() || !data.isPrimordialFuryActive()
                || !(mc.player.getMainHandItem().getItem() instanceof SpiritualSwordItem)) {
            wasAttackDown = false;
            leftClickHoldTicks = 0;
            return;
        }

        boolean attackDown = mc.options.keyAttack.isDown();
        boolean justPressed = attackDown && !wasAttackDown;
        wasAttackDown = attackDown;

        long now = mc.level.getGameTime();

        if (justPressed) {
            // 1. En el aire / Saltar + Click Izq -> Caída Destructiva
            if (!mc.player.onGround()) {
                if (now - lastComboSentTick >= 5L) {
                    lastComboSentTick = now;
                    leftClickHoldTicks = 0;
                    PacketDistributor.sendToServer(
                            new PrimordialFuryComboPayload(PrimordialFuryComboPayload.COMBO_AERIAL_DROP));
                }
                return;
            }

            // 2. Correr + Click Izq -> Golpe Sísmico
            if (mc.player.isSprinting()) {
                if (now - lastComboSentTick >= 10L) {
                    lastComboSentTick = now;
                    leftClickHoldTicks = 0;
                    PacketDistributor.sendToServer(
                            new PrimordialFuryComboPayload(PrimordialFuryComboPayload.COMBO_SEISMIC_SLAM));
                }
                return;
            }

            // 3. Click Izq normal en el suelo -> Onda Celestial (golpe normal potenciado)
            if (now - lastSlashSentTick >= 6L) {
                lastSlashSentTick = now;
                PacketDistributor.sendToServer(
                        new PrimordialFuryComboPayload(PrimordialFuryComboPayload.COMBO_CELESTIAL_SLASH));
            }
        }

        // 4. Mantener Click Izq en el suelo (sin correr) -> Tajo Ascendente
        if (attackDown && mc.player.onGround() && !mc.player.isSprinting()) {
            if (now - lastComboSentTick >= 14L) {
                leftClickHoldTicks++;
                if (leftClickHoldTicks >= HOLD_TICKS_FOR_ASCENDING_SLASH) {
                    leftClickHoldTicks = 0;
                    lastComboSentTick = now;
                    PacketDistributor.sendToServer(
                            new PrimordialFuryComboPayload(PrimordialFuryComboPayload.COMBO_ASCENDING_SLASH));
                }
            }
        } else {
            leftClickHoldTicks = 0;
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CHAT))
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui)
            return;

        PlayerRpgData data = mc.player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.isDenseSoul() || !data.isPrimordialFuryTrained())
            return;

        GuiGraphics g = event.getGuiGraphics();
        int screenW = g.guiWidth();
        int screenH = g.guiHeight();

        float charge = Mth.clamp(data.getPrimordialFuryCharge(), 0.0f, 100.0f);
        boolean active = data.isPrimordialFuryActive();
        boolean ready = !active && charge >= 100.0f;

        int barW = 92;
        int barH = 6;
        int x = screenW - barW - 10;
        int y = screenH - 16;

        // Fondo translúcido compacto en la esquina inferior derecha
        g.fill(x - 4, y - 13, x + barW + 4, y + barH + 4, 0xA80D101B);
        int borderColor = active ? 0xFFFFFFFF : (ready ? 0xFFFFD700 : 0xFF6B728E);
        g.renderOutline(x - 4, y - 13, barW + 8, barH + 17, borderColor);

        // Encabezado compacto de una línea (sin teclas en pantalla)
        String leftLabel = active ? "§f§l✦ Furia" : (ready ? "§6§l✦ Furia" : "§f✦ Furia");
        String rightLabel = (active && leftClickHoldTicks > 2)
                ? "§eTajo " + Math.min(100, (leftClickHoldTicks * 100) / HOLD_TICKS_FOR_ASCENDING_SLASH) + "%"
                : (active ? "§e" : (ready ? "§6§l" : "§7")) + Math.round(charge) + "%";

        g.drawString(mc.font, Component.literal(leftLabel), x, y - 10, 0xFFFFFFFF, true);
        int rightW = mc.font.width(rightLabel);
        g.drawString(mc.font, Component.literal(rightLabel), x + barW - rightW, y - 10, 0xFFFFFFFF, true);

        // Barra vacía
        g.fill(x, y, x + barW, y + barH, 0xFF1A1F30);

        // Relleno de barra con brillo
        int fillW = Math.round((charge / 100.0f) * barW);
        if (fillW > 0) {
            int colorTop = active ? 0xFFFFFFFF : (ready ? 0xFFFFE57F : 0xFFD9E6FF);
            int colorBot = active ? 0xFFFFC107 : (ready ? 0xFFFFA000 : 0xFF7B93C4);
            g.fillGradient(x, y, x + fillW, y + barH, colorTop, colorBot);
        }
        g.renderOutline(x, y, barW, barH, 0xFF3B425C);
    }
}
