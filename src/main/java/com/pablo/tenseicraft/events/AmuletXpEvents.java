package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.items.AncientRingItem;
import com.pablo.tenseicraft.items.ExperienceAmuletItem;
import com.pablo.tenseicraft.items.WarriorRingItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class AmuletXpEvents {

    public static List<ItemStack> findAllEvolvableAccessories(Player player) {
        List<ItemStack> list = new ArrayList<>();

        // Buscar exclusivamente en las ranuras de Curios equipadas
        var curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isPresent()) {
            var curiosInv = curiosOpt.get();
            curiosInv.findCurios(stack -> 
                stack.getItem() instanceof ExperienceAmuletItem
                || stack.getItem() instanceof WarriorRingItem
                || stack.getItem() instanceof AncientRingItem
            ).forEach(slotResult -> list.add(slotResult.stack()));
        }

        return list;
    }

    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        List<ItemStack> accessories = findAllEvolvableAccessories(player);
        if (accessories.isEmpty()) {
            return;
        }

        int originalXp = event.getOrb().getValue();
        if (originalXp <= 0) {
            return;
        }

        // Calculate Spirit Stat Bonus
        com.pablo.tenseicraft.data.PlayerRpgData data = player.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_RPG_DATA);
        int spiritPoints = (data != null) ? data.getTotalStatPoints(player, com.pablo.tenseicraft.data.StatType.SPIRIT) : 0;
        float spiritBonusMultiplier = 1.0f + (spiritPoints * 0.001f); // +0.1% per spirit point

        boolean multipliedOnce = false;

        for (ItemStack accessory : accessories) {
            // 1. Experience Amulet logic
            if (accessory.getItem() instanceof ExperienceAmuletItem) {
                int tier = ExperienceAmuletItem.getTier(accessory);
                float baseAmuletMultiplier = ExperienceAmuletItem.getXpMultiplier(tier);
                float totalMultiplier = baseAmuletMultiplier * spiritBonusMultiplier;

                if (!multipliedOnce && totalMultiplier > 1.0f) {
                    int bonusXp = Math.round(originalXp * (totalMultiplier - 1.0f));
                    if (bonusXp > 0) {
                        player.giveExperiencePoints(bonusXp);
                        multipliedOnce = true;
                    }
                }

                if (tier < 3 && !ExperienceAmuletItem.isReadyToEvolve(accessory)) {
                    ExperienceAmuletItem.addStoredXp(accessory, originalXp);
                    int current = ExperienceAmuletItem.getStoredXp(accessory);
                    int req = ExperienceAmuletItem.getRequiredXpForCurrentTier(tier);

                    if (current >= req) {
                        ExperienceAmuletItem.setReadyToEvolve(accessory, true);
                        notifyEvolutionReady(player, accessory);
                    }
                }
            }

            // 2. Warrior Ring logic
            if (accessory.getItem() instanceof WarriorRingItem) {
                int tier = WarriorRingItem.getTier(accessory);
                if (tier < 3 && !WarriorRingItem.isReadyToEvolve(accessory)) {
                    WarriorRingItem.addStoredXp(accessory, originalXp);
                    int current = WarriorRingItem.getStoredXp(accessory);
                    int req = WarriorRingItem.getRequiredXpForCurrentTier(tier);

                    if (current >= req) {
                        WarriorRingItem.setReadyToEvolve(accessory, true);
                        notifyEvolutionReady(player, accessory);
                    }
                }
            }

            // 3. Ancient Ring logic
            if (accessory.getItem() instanceof AncientRingItem) {
                int tier = AncientRingItem.getTier(accessory);
                if (tier < 3 && !AncientRingItem.isReadyToEvolve(accessory)) {
                    AncientRingItem.addStoredXp(accessory, originalXp);
                    int current = AncientRingItem.getStoredXp(accessory);
                    int req = AncientRingItem.getRequiredXpForCurrentTier(tier);

                    if (current >= req) {
                        AncientRingItem.setReadyToEvolve(accessory, true);
                        notifyEvolutionReady(player, accessory);
                    }
                }
            }
        }
    }

    private static void notifyEvolutionReady(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.empty());
            serverPlayer.sendSystemMessage(Component.translatable("tenseicraft.accessory.evolution_ready.title", stack.getHoverName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            serverPlayer.sendSystemMessage(Component.translatable("tenseicraft.amulet.evolution_ready.desc").withStyle(ChatFormatting.YELLOW));
            serverPlayer.sendSystemMessage(Component.empty());
            serverPlayer.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.9f);
        }
    }

    @SubscribeEvent
    public static void onItemTossed(ItemTossEvent event) {
        ItemEntity itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItem();

        boolean isAmuletReady = stack.getItem() instanceof ExperienceAmuletItem && ExperienceAmuletItem.isReadyToEvolve(stack);
        boolean isRingReady = stack.getItem() instanceof WarriorRingItem && WarriorRingItem.isReadyToEvolve(stack);
        boolean isAncientReady = stack.getItem() instanceof AncientRingItem && AncientRingItem.isReadyToEvolve(stack);

        if (isAmuletReady || isRingReady || isAncientReady) {
            if (itemEntity.level() instanceof ServerLevel serverLevel) {
                triggerEvolutionRitual(serverLevel, itemEntity, event.getPlayer(), stack);
            }
        }
    }

    public static void triggerEvolutionRitual(ServerLevel level, ItemEntity itemEntity, Player player, ItemStack stack) {
        int oldTier;
        if (stack.getItem() instanceof ExperienceAmuletItem) {
            oldTier = ExperienceAmuletItem.getTier(stack);
        } else if (stack.getItem() instanceof WarriorRingItem) {
            oldTier = WarriorRingItem.getTier(stack);
        } else {
            oldTier = AncientRingItem.getTier(stack);
        }
        int newTier = oldTier + 1;

        // Visual and thunder strike (visualOnly prevents destroying the item on ground)
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning != null) {
            lightning.moveTo(itemEntity.position());
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }

        // Advance tier and reset ready flag
        if (stack.getItem() instanceof ExperienceAmuletItem) {
            ExperienceAmuletItem.setTier(stack, newTier);
            ExperienceAmuletItem.setReadyToEvolve(stack, false);
        } else if (stack.getItem() instanceof WarriorRingItem) {
            WarriorRingItem.setTier(stack, newTier);
            WarriorRingItem.setReadyToEvolve(stack, false);
        } else if (stack.getItem() instanceof AncientRingItem) {
            AncientRingItem.setTier(stack, newTier);
            AncientRingItem.setReadyToEvolve(stack, false);
        }
        itemEntity.setItem(stack);

        // Sound & Particles
        level.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                SoundEvents.TRIDENT_THUNDER.value(), SoundSource.WEATHER, 1.0f, 1.0f);

        serverParticleBurst(level, itemEntity);

        if (player instanceof ServerPlayer serverPlayer) {
            Component tierLabel = switch (newTier) {
                case 1 -> Component.translatable("tenseicraft.tier.common").withStyle(ChatFormatting.GREEN);
                case 2 -> Component.translatable("tenseicraft.tier.rare").withStyle(ChatFormatting.BLUE);
                case 3 -> Component.translatable("tenseicraft.tier.legendary").withStyle(ChatFormatting.GOLD);
                default -> Component.literal("Tier " + newTier);
            };

            serverPlayer.sendSystemMessage(Component.translatable("tenseicraft.amulet.evolution_success", tierLabel).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            QuestEvents.onAccessoryEvolved(serverPlayer);
        }
    }

    private static void serverParticleBurst(ServerLevel level, ItemEntity itemEntity) {
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, itemEntity.getX(), itemEntity.getY() + 0.3, itemEntity.getZ(),
                30, 0.4, 0.4, 0.4, 0.15);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, itemEntity.getX(), itemEntity.getY() + 0.3, itemEntity.getZ(),
                20, 0.3, 0.3, 0.3, 0.1);
    }
}
