package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.SyncRpgDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class MobilityProgressionEvents {

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) return;

            boolean unlocked = data.incrementJumpCount(player);
            if (unlocked) {
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);

                if (player.serverLevel() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
                    sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 20, 0.5, 0.1, 0.5, 0.1);
                }

                player.sendSystemMessage(Component.literal("§a§l[¡HABILIDAD DESBLOQUEADA!] §fTus piernas se han fortalecido tras dar §e2,000 saltos§f. ¡Has dominado el §a§lDoble Salto§f! (Presiona Espacio en el aire)").withStyle(ChatFormatting.GREEN));
                PacketDistributor.sendToPlayer(player, new SyncRpgDataPayload(data.serializeNBT(player.registryAccess())));
            } else if (data.getJumpCount() % 500 == 0 && data.getJumpCount() < 2000) {
                player.sendSystemMessage(Component.literal("§7[Entrenamiento de Salto] Progreso: §e" + data.getJumpCount() + "§7 / §e2000 §7saltos.").withStyle(ChatFormatting.GRAY));
            }
        }
    }
}

