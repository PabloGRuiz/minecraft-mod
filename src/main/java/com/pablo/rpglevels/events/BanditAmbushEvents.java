package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.entities.BanditEntity;
import com.pablo.rpglevels.mobs.BanditAmbushManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class BanditAmbushEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof BanditEntity bandit) {
            BanditAmbushManager.onBanditDeath(bandit);

            // Registrar muerte atribuida al jugador para el sistema de Venganza Nemesis
            if (event.getSource().getEntity() instanceof ServerPlayer player) {
                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    data.incrementBanditsKilledToday();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            checkDayTransition(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Run checks every 20 ticks (1 second)
        if (player.tickCount % 20 != 0) {
            return;
        }

        // Periodic cleanup check
        if (player.tickCount % 100 == 0) {
            BanditAmbushManager.tickServerAmbushes(player.serverLevel());
        }

        // Solo se gestionan emboscadas en la superficie del Overworld
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return;
        }

        // Elegible si ha asignado puntos o tiene algún rango de aventurero
        boolean isEligible = data.getRank().getIndex() > 0 || data.getAllocatedPointsCount() > 0 || !data.getChosenMilestones().isEmpty();
        if (!isEligible) {
            return;
        }

        // 1. Comprobar cambio de día (24.000 ticks)
        checkDayTransition(player);

        // 2. Si hay una emboscada pendiente para hoy, verificar si el jugador está en la superficie libre
        if (data.isPendingAmbushToday() && !BanditAmbushManager.hasActiveAmbush(player)) {
            // Verificar cada 10 segundos (200 ticks)
            if (player.tickCount % 200 == 0) {
                int surfaceY = player.serverLevel().getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        player.getBlockX(),
                        player.getBlockZ()
                );

                if (Math.abs(player.getBlockY() - surfaceY) <= 12) {
                    int killedYesterday = data.getBanditsKilledYesterday();
                    boolean isRevenge = killedYesterday >= 2;
                    boolean triggered = BanditAmbushManager.triggerAmbush(player, isRevenge, killedYesterday);
                    if (triggered) {
                        data.setPendingAmbushToday(false);
                    }
                }
            }
        }
    }

    private static void checkDayTransition(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || player.level().dimension() != Level.OVERWORLD) {
            return;
        }

        long currentDay = player.serverLevel().getDayTime() / 24000L;
        long lastDay = data.getLastAmbushCheckDay();

        if (lastDay == -1L) {
            data.setLastAmbushCheckDay(currentDay);
            return;
        }

        if (currentDay > lastDay) {
            int killedYesterday = data.getBanditsKilledToday();
            data.advanceBanditDay(currentDay);

            // Probabilidad base: 25%
            // Venganza: +15% de probabilidad por cada bandido eliminado en el día anterior
            // Ej: 3 bandidos = 25% + 45% = 70%. Con 5 o más = 95% (Casi segura!)
            float ambushChance = 0.25f + (killedYesterday * 0.15f);
            ambushChance = Math.min(0.95f, ambushChance);

            if (player.serverLevel().random.nextFloat() < ambushChance) {
                data.setPendingAmbushToday(true);
                if (killedYesterday >= 2) {
                    player.sendSystemMessage(Component.empty());
                    player.sendSystemMessage(
                            Component.literal("§4§l[!] §cLos forajidos claman venganza de sangre por los §e" + killedYesterday + " camaradas §cque eliminaste ayer... Una partida de asalto te está dando caza.")
                                    .withStyle(ChatFormatting.RED)
                    );
                    player.sendSystemMessage(Component.empty());
                }
            } else {
                data.setPendingAmbushToday(false);
            }
        }
    }
}
