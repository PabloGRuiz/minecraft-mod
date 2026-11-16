package com.pablo.rpglevels.world.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RiftMonolithBlock extends Block {

    public RiftMonolithBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (ModDimensions.isAbyssRift(level)) {
                AbyssTeleportHelper.exitAbyssRift(serverPlayer);
            } else {
                AbyssTeleportHelper.enterAbyssRift(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Mysterious purple and abyssal particles rising from the monolith
        for (int i = 0; i < 3; i++) {
            double px = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
            double py = pos.getY() + 0.8 + random.nextDouble() * 0.5;
            double pz = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;

            level.addParticle(ParticleTypes.PORTAL, px, py, pz,
                    (random.nextDouble() - 0.5) * 0.2, 0.1, (random.nextDouble() - 0.5) * 0.2);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz,
                    0, 0.05, 0);
        }

        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.4f, 1.2f, false);
        }
    }
}
