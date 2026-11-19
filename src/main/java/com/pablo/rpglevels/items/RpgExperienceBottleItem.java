package com.pablo.rpglevels.items;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.events.CombatEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Frasco de Experiencia RPG universal:
 * Al beberlo, otorga Experiencia RPG equivalente a derrotar a una criatura
 * de un rango de amenaza superior al rango actual del aventurero
 * (F -> E, E -> D, D -> C, C -> B, B -> A, A -> Hazard).
 * Al consumirse devuelve un frasco de cristal vacío.
 */
public class RpgExperienceBottleItem extends Item {

    public RpgExperienceBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return stack;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasDivineBlessingUnlocked()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.literal("§cDebes recibir primero tu Bendición Divina en el Espacio Astral para absorber XP RPG."),
                        true);
                serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.9F, 1.0F);
            }
            return stack;
        }

        if (data.isAtMilestoneCap()) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.literal("§eYa alcanzaste el Hito de Nivel " + data.getCurrentMilestoneCap()
                                + ". Duerme en una cama para ascender en el Espacio Astral."),
                        true);
                serverPlayer.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.9F, 1.1F);
            }
            return stack;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            AdventurerRank playerRank = data.getRank();
            ThreatLevel[] threats = ThreatLevel.values();
            ThreatLevel higherThreat = threats[Math.min(threats.length - 1, playerRank.getIndex() + 1)];

            int baseReward = CombatEvents.getBaseXpForThreat(higherThreat);
            int xpGrant = CombatEvents.calculateRankScaledXp(baseReward, playerRank, higherThreat);

            data.addRpgXp(serverPlayer, xpGrant);

            ServerLevel sl = serverPlayer.serverLevel();
            sl.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.1F);
            sl.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.4F);

            sl.sendParticles(ParticleTypes.ENCHANT,
                    serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                    25, 0.45D, 0.5D, 0.45D, 0.3D);
            sl.sendParticles(ParticleTypes.END_ROD,
                    serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                    10, 0.3D, 0.4D, 0.3D, 0.05D);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                    12, 0.3D, 0.4D, 0.3D, 0.08D);

            serverPlayer.displayClientMessage(
                    Component.literal("§b✦ +" + xpGrant + " XP RPG §7(Equivalente a Amenaza §e" + higherThreat.getId() + "§7)"),
                    true);

            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);
        if (!player.hasInfiniteMaterials()) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            if (!player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.rpglevels.rpg_experience_bottle.desc1").withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable("item.rpglevels.rpg_experience_bottle.desc2").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable("item.rpglevels.rpg_experience_bottle.desc3").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
