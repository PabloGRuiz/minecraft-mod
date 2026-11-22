package com.pablo.tenseicraft.items;

import com.pablo.tenseicraft.entities.ThrownHighExperienceBottleEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Botella de Gran Experiencia (High Experience Bottle).
 * Al lanzarse, se rompe y desata 10 veces más orbes de experiencia que una botella normal.
 * Se craftea combinando 9 botellas de experiencia en una mesa de crafteo.
 */
public class HighExperienceBottleItem extends Item {

    public HighExperienceBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Efecto de encantamiento brillante
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EXPERIENCE_BOTTLE_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            ThrownHighExperienceBottleEntity bottle = new ThrownHighExperienceBottleEntity(level, player);
            bottle.setItem(itemstack);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.7F, 1.0F);
            level.addFreshEntity(bottle);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        itemstack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.tenseicraft.high_experience_bottle.desc").withStyle(ChatFormatting.GREEN));
        tooltipComponents.add(Component.translatable("item.tenseicraft.high_experience_bottle.recipe_hint").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}

