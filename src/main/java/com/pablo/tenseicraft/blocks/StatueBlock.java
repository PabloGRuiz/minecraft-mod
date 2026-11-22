package com.pablo.tenseicraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Bloque decorativo de estatua que cabe exactamente en 1 bloque,
 * se orienta hacia el jugador al colocarse y reacciona con sonido y partículas al acariciarla.
 */
public class StatueBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    protected static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

    private final Supplier<SoundEvent> chimeSound;
    private final Supplier<ParticleOptions> chimeParticle;

    public StatueBlock(Properties properties, Supplier<SoundEvent> chimeSound, Supplier<ParticleOptions> chimeParticle) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        this.chimeSound = chimeSound;
        this.chimeParticle = chimeParticle;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (chimeSound != null && chimeSound.get() != null) {
                level.playSound(null, pos, chimeSound.get(), SoundSource.BLOCKS, 0.8F, 1.5F);
            }
            if (chimeParticle != null && chimeParticle.get() != null && level instanceof ServerLevel sl) {
                sl.sendParticles(chimeParticle.get(),
                        pos.getX() + 0.5D, pos.getY() + 0.8D, pos.getZ() + 0.5D,
                        6, 0.2D, 0.2D, 0.2D, 0.04D);
                sl.sendParticles(ParticleTypes.HEART,
                        pos.getX() + 0.5D, pos.getY() + 0.9D, pos.getZ() + 0.5D,
                        2, 0.15D, 0.15D, 0.15D, 0.02D);
            }
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
