package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.network.OpenNpcDialoguePayload;
import com.pablo.rpglevels.social.NpcSocialManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class VillagerDialogueEvents {

    private static final String[] MALE_VILLAGER_NAMES = {
            "Mateo", "Lucas", "Tomás", "Gabriel", "Bruno",
            "Nicolás", "Martín", "Diego", "Adrián", "Julián",
            "Sebastián", "Felipe", "Marcos", "Darío", "Hugo", "Elías"
    };

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }

        VillagerProfession profession = villager.getVillagerData().getProfession();
        boolean isUnemployed = (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT);
        boolean isSneaking = event.getEntity().isShiftKeyDown();

        // On Client: Don't access offers (causes IllegalStateException). Only handle cancel for dialogue.
        if (event.getLevel().isClientSide()) {
            if (isUnemployed || isSneaking) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        // --- SERVER ONLY LOGIC ---
        // 1. Unemployed / Nitwit villagers: Regular right-click opens the interactive dialogue menu!
        // 2. Employed villagers (Farmers, Librarians, etc.): Shift + Right-click opens the dialogue menu (preserving normal trade on regular right-click).
        if (!isUnemployed && !isSneaking) {
            // Check if player is a PUBLIC ENEMY: Villager refuses to trade with enemies of the village!
            if (event.getEntity() instanceof ServerPlayer sp) {
                com.pablo.rpglevels.data.PlayerRpgData rpgData = sp.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
                if (rpgData != null && rpgData.getReputationTier() == com.pablo.rpglevels.data.ReputationTier.PUBLIC_ENEMY) {
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                    sp.displayClientMessage(Component.translatable("rpglevels.reputation.refusal_public_enemy").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.BOLD), true);
                    sp.serverLevel().playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                            SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    return;
                }
            }

            // Apply reputation price adjustments (Discounts for heroes, Inflation for outlaws)
            ReputationEvents.applyReputationPriceModifier(event.getEntity(), villager.getOffers());
            return; // Allow standard vanilla trading
        }

        // Cancel vanilla action so no head-shake or default interaction happens
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (event.getEntity() instanceof ServerPlayer player) {
            // Assign a persistent name if none exists
            if (!villager.hasCustomName()) {
                String assignedName = MALE_VILLAGER_NAMES[Math.abs(villager.getUUID().hashCode()) % MALE_VILLAGER_NAMES.length];
                villager.setCustomName(Component.literal("§e" + assignedName));
                villager.setCustomNameVisible(true);
            }

            String villagerName = villager.getCustomName() != null ? villager.getCustomName().getString() : "Aldeano";
            String playerName = player.getName().getString();

            String affKey = "RpgAffinity_" + player.getUUID();
            int currentAffinity = villager.getPersistentData().getInt(affKey);

            String greeting = NpcSocialManager.getGreeting(
                    "villager",
                    currentAffinity,
                    AdventurerRank.F,
                    villagerName,
                    playerName,
                    false,
                    false
            );

            // Friendly villager voice
            player.serverLevel().playSound(null, villager.getX(), villager.getY(), villager.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);

            PacketDistributor.sendToPlayer(player, new OpenNpcDialoguePayload(
                    villager.getId(),
                    villagerName,
                    "villager",
                    0,
                    0,
                    currentAffinity,
                    false,
                    false,
                    0,
                    false,
                    0,
                    false,
                    0,
                    false,
                    0,
                    false,
                    greeting
            ));
        }
    }
}
