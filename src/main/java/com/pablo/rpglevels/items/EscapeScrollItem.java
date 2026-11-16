package com.pablo.rpglevels.items;

import com.pablo.rpglevels.world.dimension.AbyssTeleportHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class EscapeScrollItem extends Item {

    public EscapeScrollItem() {
        super(new Properties().stacksTo(16).rarity(Rarity.RARE));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60; // 3 seconds channeling
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.6f, 1.4f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide()) {
            for (int i = 0; i < 2; i++) {
                double angle = Math.random() * Math.PI * 2;
                double px = entity.getX() + Math.cos(angle) * 0.7;
                double pz = entity.getZ() + Math.sin(angle) * 0.7;
                level.addParticle(ParticleTypes.REVERSE_PORTAL, px, entity.getY() + 0.1, pz, 0, 0.1, 0);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            AbyssTeleportHelper.exitAbyssRift(player);
            player.sendSystemMessage(Component.literal("§d✦ ¡Pergamino de Escape activado! Has regresado a salvo con tus tesoros."));
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return stack;
    }
}
