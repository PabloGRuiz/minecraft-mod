package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.entities.BanditEntity;
import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class ReputationEvents {

    /**
     * Changes player reputation and notifies them with feedback and sound.
     */
    public static void modifyReputation(ServerPlayer player, int amount, String reasonKey, Object... args) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || amount == 0) return;

        // Adventurer Guild Rank Multiplier: High rank adventurers have greater influence and scrutiny!
        if (data.isGuildRegistered()) {
            float mult = data.getRank().getReputationMultiplier();
            amount = Math.round(amount * mult);
            if (amount == 0) amount = (mult > 0 ? 1 : -1);
        }

        int oldScore = data.getReputationScore();
        int changed = data.addReputation(amount);
        if (changed == 0) return;

        ModNetworking.syncToPlayer(player);

        ChatFormatting color = (amount > 0) ? ChatFormatting.GREEN : ChatFormatting.RED;
        String sign = (amount > 0) ? "+" : "";
        Component msg = Component.literal("§8[⚖] ")
                .append(Component.literal(sign + amount + " Reputación ").withStyle(color, ChatFormatting.BOLD))
                .append(Component.literal("(" + data.getReputationScore() + "/100) ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(reasonKey, args).withStyle(ChatFormatting.YELLOW));

        player.displayClientMessage(msg, true);
    }

    /**
     * 1. KILL EVENTS (Monsters, Bosses, Civilians, Adventurers, Golems)
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        LivingEntity victim = event.getEntity();

        // --- NEGATIVE REPUTATION: INJURING / KILLING INNOCENTS ---
        if (victim instanceof Villager || victim instanceof TownswomanNpcEntity) {
            modifyReputation(player, -15, "rpglevels.reputation.reason.killed_civilian");
            return;
        }

        if (victim instanceof AdventurerNpcEntity) {
            modifyReputation(player, -20, "rpglevels.reputation.reason.killed_adventurer");
            return;
        }

        if (victim instanceof IronGolem) {
            modifyReputation(player, -10, "rpglevels.reputation.reason.killed_golem");
            return;
        }

        // --- POSITIVE REPUTATION: DEFEATING FOES, BANDITS AND BOSSES ---
        if (victim instanceof BanditEntity) {
            modifyReputation(player, 2, "rpglevels.reputation.reason.killed_bandit");
            return;
        }

        if (victim instanceof Warden || victim instanceof WitherBoss) {
            modifyReputation(player, 25, "rpglevels.reputation.reason.killed_world_boss");
            return;
        }

        // Check if boss from this mod
        if (victim instanceof com.pablo.rpglevels.entities.UndeadAdventurerBossEntity) {
            modifyReputation(player, 15, "rpglevels.reputation.reason.killed_boss");
            return;
        }

        if (victim instanceof net.minecraft.world.entity.monster.Enemy || victim instanceof Monster) {
            // Check Threat Level of defeated foe
            ThreatLevel threat = ThreatLevel.determineThreatLevel(victim);
            int bonus = switch (threat) {
                case GENESIS -> 30;
                case CATASTROPHE -> 20;
                case GRAND_DISASTER -> 15;
                case DISASTER -> 10;
                case CALAMITY -> 6;
                case HAZARD -> 4;
                case B -> 3;
                case C -> 2;
                default -> 1;
            };

            // Small chance or throttled for regular trash monsters so players don't hit +100 in 1 minute
            long lastKillTick = player.getPersistentData().getLong("RpgLastMonsterRepTick");
            long gameTime = player.level().getGameTime();
            if (gameTime - lastKillTick >= 60 || bonus > 1) { // once every 3s for normal mobs
                player.getPersistentData().putLong("RpgLastMonsterRepTick", gameTime);
                modifyReputation(player, bonus, "rpglevels.reputation.reason.killed_monster");
            }
        }
    }

    /**
     * 2. TRADE WITH VILLAGERS (Positive reputation + apply price modifier based on reputation tier)
     */
    @SubscribeEvent
    public static void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        long lastTradeTick = player.getPersistentData().getLong("RpgLastTradeRepTick");
        long gameTime = player.level().getGameTime();
        // Prevent trade spam exploit (at most +1 every 20 seconds / 400 ticks)
        if (gameTime - lastTradeTick >= 400) {
            player.getPersistentData().putLong("RpgLastTradeRepTick", gameTime);
            modifyReputation(player, 1, "rpglevels.reputation.reason.trade_villager");
        }
    }

    /**
     * Dynamically adjusts merchant trade costs based on Player's Reputation Tier:
     * - Heroes get discounts (up to -40%)
     * - Outlaws/Public Enemies get inflated prices (+25% to +60%)
     */
    public static void applyReputationPriceModifier(Player player, net.minecraft.world.item.trading.MerchantOffers offers) {
        if (offers == null || offers.isEmpty()) return;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        float mod = data.getReputationTier().getPriceModifier();
        if (Math.abs(mod) < 0.001f) return;

        for (MerchantOffer offer : offers) {
            int baseCostA = offer.getBaseCostA().getCount();
            if (baseCostA > 0) {
                int delta = Math.round(baseCostA * mod);
                // SpecialPrice modifier in vanilla MerchantOffer adjusts the cost (negative reduces price, positive raises it)
                offer.addToSpecialPriceDiff(delta);
            }
        }
    }

    /**
     * 3. HERO OF THE VILLAGE ADVANCEMENT (Raid repelled +20 Rep)
     */
    @SubscribeEvent
    public static void onAdvancementEarn(net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getAdvancement() != null && event.getAdvancement().id().getPath().contains("hero_of_the_village")) {
            modifyReputation(player, 20, "rpglevels.reputation.reason.raid_victory");
        }
    }

    /**
     * 4. PIGLIN BARTER (Trading with Piglins decreases reputation in Overworld lore)
     */
    @SubscribeEvent
    public static void onPlayerInteractEntity(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (event.getTarget() instanceof AbstractPiglin piglin) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(event.getHand());
            if (held.is(net.minecraft.world.item.Items.GOLD_INGOT)) {
                modifyReputation(player, -2, "rpglevels.reputation.reason.piglin_trade");
            }
        }
    }

    /**
     * 5. PUBLIC ENEMY HOSTILITY:
     * - Iron Golems attack Public Enemies (-75 to -100 reputation) on sight!
     */
    @SubscribeEvent
    public static void onLivingChangeTarget(net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof IronGolem golem) {
            if (event.getNewAboutToBeSetTarget() == null) {
                // Look for nearby public enemies within 16 blocks
                for (Player p : golem.level().players()) {
                    if (p.distanceToSqr(golem) <= 256.0) {
                        PlayerRpgData data = p.getData(ModAttachments.PLAYER_RPG_DATA);
                        if (data != null && data.getReputationTier() == com.pablo.rpglevels.data.ReputationTier.PUBLIC_ENEMY) {
                            event.setNewAboutToBeSetTarget(p);
                            break;
                        }
                    }
                }
            }
        }
    }
}
