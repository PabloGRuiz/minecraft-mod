package com.pablo.tenseicraft.client.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Genera partículas diminutas estilo Gear 5 (chispitas eléctricas y puntitos de luz)
 * que brotan de vez en cuando (cada ~5 segundos) para no saturar la vista.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
public class HumoParticleClientEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null || !player.isAlive()) return;

        Level level = player.level();
        if (!level.isClientSide()) return;
        if (player.isInvisible() || player.isSpectator()) return;

        // Solo emitir partículas si el humo está activo
        if (!com.pablo.tenseicraft.client.renderer.HumoEffectManager.isSmokeActive(player)) return;

        // Ciclo de 5 segundos (100 ticks)
        int cycle = player.tickCount % 100;

        // Solo se activan brevemente cada 5 segundos (en ticks 0, 3 y 6)
        if (cycle != 0 && cycle != 3 && cycle != 6) {
            return;
        }

        RandomSource random = player.getRandom();

        // Seleccionar un punto a lo largo del aro de humo sobre hombros y espalda
        double angleDeg = random.nextDouble() * 260.0 - 130.0;
        double angleRad = Math.toRadians(angleDeg);

        double radiusX = 0.58 + random.nextDouble() * 0.14;
        double radiusZ = 0.46 + random.nextDouble() * 0.14;

        double lx = Math.sin(angleRad) * radiusX;
        double lz = Math.cos(angleRad) * radiusZ + 0.08;

        float bodyYawRad = (float) Math.toRadians(player.yBodyRot);
        float cos = Mth.cos(bodyYawRad);
        float sin = Mth.sin(bodyYawRad);

        double worldX = player.getX() + (lx * cos - lz * sin);
        double worldZ = player.getZ() + (lx * sin + lz * cos);

        double baseHeight = player.isCrouching() ? 1.22D : 1.48D;
        double worldY = player.getY() + baseHeight + (random.nextDouble() - 0.5) * 0.10D;

        // Solo partículas diminutas: chispitas eléctricas sutiles y puntitos de luz blanca
        if (random.nextBoolean()) {
            level.addParticle(
                    ParticleTypes.ELECTRIC_SPARK,
                    worldX, worldY, worldZ,
                    (random.nextDouble() - 0.5) * 0.02,
                    0.015,
                    (random.nextDouble() - 0.5) * 0.02
            );
        } else {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    worldX, worldY, worldZ,
                    (random.nextDouble() - 0.5) * 0.005,
                    0.012,
                    (random.nextDouble() - 0.5) * 0.005
            );
        }
    }
}
