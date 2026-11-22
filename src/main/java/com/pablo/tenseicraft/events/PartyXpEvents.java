package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import com.pablo.tenseicraft.party.PartyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;

import java.util.List;

/**
 * Handles equitable party experience sharing across all party members,
 * including both human players and hired companion Adventurer NPCs.
 *
 * Ensures that if a player earns XP of any type (orbs, combat, ambushes, mining, quests),
 * it is automatically and fairly split among all nearby companions and party members.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class PartyXpEvents {

    private static final ThreadLocal<Boolean> IS_DISTRIBUTING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onPlayerXpChange(PlayerXpEvent.XpChange event) {
        if (IS_DISTRIBUTING.get()) {
            return;
        }

        Player player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        int totalXp = event.getAmount();
        if (totalXp <= 0) {
            return;
        }

        // Obtener miembros del grupo en un radio de 48 bloques (jugadores y compañeros NPC)
        List<LivingEntity> recipients = PartyManager.getPartyXpRecipients(serverPlayer, 48.0);
        if (recipients.size() <= 1) {
            return; // Jugador en solitario sin compañeros cercanos
        }

        int memberCount = recipients.size();
        int share = Math.max(1, totalXp / memberCount);

        IS_DISTRIBUTING.set(true);
        try {
            // El jugador actual conserva su parte o al menos 1
            event.setAmount(Math.max(1, totalXp - (share * (memberCount - 1))));

            for (LivingEntity recipient : recipients) {
                if (recipient == serverPlayer) {
                    continue; // Ya asignado mediante event.setAmount
                }
                if (recipient instanceof ServerPlayer otherPlayer) {
                    otherPlayer.giveExperiencePoints(share);
                } else if (recipient instanceof AdventurerNpcEntity adv) {
                    adv.addExperience(share);
                }
            }
        } finally {
            IS_DISTRIBUTING.set(false);
        }
    }
}

