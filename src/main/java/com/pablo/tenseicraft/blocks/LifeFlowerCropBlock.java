package com.pablo.tenseicraft.blocks;

import com.pablo.tenseicraft.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Cultivo mágico de la Flor de la Vida (Flor Arcoíris).
 * Se planta con Semillas de Vida sobre tierra de cultivo (farmland),
 * posee 8 etapas de crecimiento (0-7) acelerables con polvo de hueso o magia floral,
 * y al madurar florece con una paleta arcoíris que al cosecharse otorga Pétalos Arcoíris.
 */
public class LifeFlowerCropBlock extends CropBlock {

    public static final int MAX_AGE = 7;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;

    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(4.0D, 0.0D, 4.0D, 12.0D, 3.0D, 12.0D),
            Block.box(3.5D, 0.0D, 3.5D, 12.5D, 5.0D, 12.5D),
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 7.0D, 13.0D),
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 9.0D, 13.0D),
            Block.box(2.5D, 0.0D, 2.5D, 13.5D, 11.0D, 13.5D),
            Block.box(2.5D, 0.0D, 2.5D, 13.5D, 12.0D, 13.5D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.5D, 14.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 15.0D, 14.0D)
    };

    private static final Vector3f[] RAINBOW_COLORS = new Vector3f[]{
            new Vector3f(1.0F, 0.25F, 0.35F), // Rojo rubí
            new Vector3f(1.0F, 0.62F, 0.18F), // Naranja coral
            new Vector3f(1.0F, 0.92F, 0.28F), // Amarillo dorado
            new Vector3f(0.28F, 0.95F, 0.55F), // Verde esmeralda
            new Vector3f(0.25F, 0.85F, 1.0F), // Celeste arcoíris
            new Vector3f(0.76F, 0.35F, 1.0F)  // Violeta magenta
    };

    public LifeFlowerCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.LIFE_SEEDS.get();
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[this.getAge(state)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (this.isMaxAge(state) && random.nextInt(5) == 0) {
            Vector3f color = RAINBOW_COLORS[random.nextInt(RAINBOW_COLORS.length)];
            double px = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
            double py = pos.getY() + 0.55D + random.nextDouble() * 0.4D;
            double pz = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.55D;
            level.addParticle(new DustParticleOptions(color, 0.85F), px, py, pz, 0.0D, 0.03D, 0.0D);

            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.END_ROD, px, py + 0.1D, pz, 0.0D, 0.025D, 0.0D);
            }
        }
    }
}
