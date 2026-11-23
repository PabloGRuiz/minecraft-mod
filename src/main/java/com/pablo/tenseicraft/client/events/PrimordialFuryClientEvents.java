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
        if (data == null || !data.isPrimordialFuryTrained() || !data.isPrimordialFuryActive()
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
        if (data == null || !data.isPrimordialFuryTrained())
            return;

        GuiGraphics g = event.getGuiGraphics();
        int screenH = g.guiHeight();

        float charge = Mth.clamp(data.getPrimordialFuryCharge(), 0.0f, 100.0f);
        boolean active = data.isPrimordialFuryActive();
        boolean ready = !active && charge >= 100.0f;

        int barW = 128;
        int barH = 8;
        int x = 12;
        int y = screenH - 36;

        // Fondo translúcido estilo HUD RPG
        g.fill(x - 4, y - 14, x + barW + 4, y + barH + 14, 0xA80D101B);
        int borderColor = active ? 0xFFFFFFFF : (ready ? 0xFFFFD700 : 0xFF6B728E);
        g.renderOutline(x - 4, y - 14, barW + 8, barH + 28, borderColor);

        // Título superior
        String title = active
                ? "§f§l✦ FURIA PRIMORDIAL §e[ACTIVA]"
                : (ready ? "§6§l✦ FURIA PRIMORDIAL §f[LISTA]" : "§f✦ Furia Primordial");
        g.drawString(mc.font, Component.literal(title), x, y - 11, 0xFFFFFFFF, true);

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

        // Subtítulo de estado / pista de teclas
        String sub;
        if (active) {
            if (leftClickHoldTicks > 2) {
                int pct = Math.min(100, (leftClickHoldTicks * 100) / HOLD_TICKS_FOR_ASCENDING_SLASH);
                sub = "§eCargando Tajo Ascendente... §f" + pct + "%";
            } else {
                sub = "§f" + Math.round(charge) + "% §7Correr/Saltar/Mant. §fLMB";
            }
        } else if (ready) {
            sub = "§ePresiona §f§l[Z] §econ tu espada sellada";
        } else {
            sub = "§7Carga: §f" + Math.round(charge) + "% §8(Pelea con espada sellada)";
        }
        g.drawString(mc.font, Component.literal(sub), x, y + barH + 3, 0xFFCCCCCC, false);
    }
}
