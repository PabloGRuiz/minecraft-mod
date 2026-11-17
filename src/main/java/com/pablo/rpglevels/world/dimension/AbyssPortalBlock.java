package com.pablo.rpglevels.world.dimension;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.init.ModBlocks;
import com.pablo.rpglevels.init.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

/**
 * Portal del Abismo con marco de Obsidiana Llorosa (Crying Obsidian).
 * Despertado mediante la Técnica Espiritual Nº 20: Conectar Realidades.
 */
public class AbyssPortalBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public AbyssPortalBlock() {
        super(BlockBehaviour.Properties.of()
                .noCollission()
                .strength(-1.0F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 10)
                .noOcclusion()
                .mapColor(MapColor.COLOR_BLACK)
                .pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_AABB : X_AXIS_AABB;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }

        long gameTime = level.getGameTime();
        long lastTouch = player.getPersistentData().getLong("RpgAbyssPortalLastTouch");
        int ticks = player.getPersistentData().getInt("RpgAbyssPortalTicks");
        long cooldownEnd = player.getPersistentData().getLong("RpgAbyssPortalCooldownEnd");

        // Si aún está en enfriamiento tras un teletransporte o rechazo previo
        if (gameTime < cooldownEnd) {
            return;
        }

        // Si pasó más de 5 ticks desde el último toque, reiniciamos el contador
        if (gameTime - lastTouch > 5) {
            ticks = 0;
        }
        ticks++;
        player.getPersistentData().putLong("RpgAbyssPortalLastTouch", gameTime);
        player.getPersistentData().putInt("RpgAbyssPortalTicks", ticks);

        // Requiere estar parado ~1.2 segundos (24 ticks), o instantáneo en creativo
        if (player.isCreative() || ticks >= 24) {
            player.getPersistentData().putInt("RpgAbyssPortalTicks", 0);
            player.getPersistentData().putLong("RpgAbyssPortalCooldownEnd", gameTime + 60L); // 3 segundos de cooldown

            boolean isInAbyss = level.dimension().equals(ModDimensions.ABYSS_RIFT_LEVEL);

            if (isInAbyss) {
                // Volviendo del Abismo al Overworld
                AbyssTeleportHelper.exitAbyssRift(player);
            } else {
                // Entrando al Abismo desde el Overworld: Comprobar Rechazo del Vacío
                boolean hasWard = player.hasEffect(ModMobEffects.ABYSSAL_WARD);

                if (!player.isCreative() && !player.isSpectator() && !hasWard) {
                    // ¡RECHAZO DEL VACÍO!
                    executeVoidRejection(player);
                    return;
                }

                // Permiso concedido: teletransporte seguro al Abismo
                AbyssTeleportHelper.enterAbyssRift(player);
            }
        }
    }

    /**
     * Expulsa y repele al jugador con un impacto del Vacío si intenta entrar sin la preparación requerida.
     */
    public static void executeVoidRejection(ServerPlayer player) {
        ServerLevel serverLevel = player.serverLevel();

        // Empuje violento hacia atrás
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(-look.x * 1.5, 0.4, -look.z * 1.5);
        player.hurtMarked = true;

        // Efectos sónicos y de distorsión abisal
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 1.8F);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 1.0F, 0.6F);

        serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 0.8, 0.6, 0.15);

        // Títulos de alerta en pantalla
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§l✦ ¡EL VACÍO TE RECHAZA! ✦")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§7Requiere el efecto de Inmunidad Abisal de Alice.")));

        player.sendSystemMessage(Component.literal("§c[Abismo] §7La fuerza de repulsión del Vacío te ha expulsado violentamente."));
        player.sendSystemMessage(Component.literal("§7Habla con §dAlice §7en el Gremio para que te otorgue su §bInmunidad al Rechazo Abisal (20 min)§7."));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.4F + random.nextFloat() * 0.3F, false);
        }

        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            double speedX = (random.nextDouble() - 0.5D) * 0.2D;
            double speedY = (random.nextDouble() - 0.5D) * 0.2D;
            double speedZ = (random.nextDouble() - 0.5D) * 0.2D;

            if (random.nextBoolean()) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, speedX, speedY, speedZ);
            } else if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, speedX * 0.5, speedY * 0.5, speedZ * 0.5);
            } else {
                level.addParticle(ParticleTypes.PORTAL, x, y, z, speedX, speedY, speedZ);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        // Si el marco circundante o soporte fue roto, se disuelve el portal
        if (direction == Direction.DOWN || direction == Direction.UP) {
            if (!isValidNeighbor(neighborState)) {
                return Blocks.AIR.defaultBlockState();
            }
        } else if ((axis == Direction.Axis.X && (direction == Direction.WEST || direction == Direction.EAST))
                || (axis == Direction.Axis.Z && (direction == Direction.NORTH || direction == Direction.SOUTH))) {
            if (!isValidNeighbor(neighborState)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    private static boolean isValidNeighbor(BlockState state) {
        return state.is(Blocks.CRYING_OBSIDIAN) || state.is(ModBlocks.ABYSS_PORTAL.get());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return switch (rot) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                default -> state;
            };
            default -> state;
        };
    }

    // =========================================================================
    // DETECCIÓN E IGNICIÓN DEL MARCO DE OBSIDIANA LLOROSA (TÉCNICA Nº 20)
    // =========================================================================

    /**
     * Busca marcos válidos de Obsidiana Llorosa alrededor de centerPos (dentro del radio)
     * e intenta encender el portal. Devuelve true si al menos un portal fue encendido.
     */
    public static boolean attemptPortalIgnition(Level level, BlockPos centerPos, int radius) {
        boolean ignited = false;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(centerPos.getX() + dx, centerPos.getY() + dy, centerPos.getZ() + dz);
                    BlockState bs = level.getBlockState(cursor);

                    // Solo intentamos crear en espacios vacíos de aire
                    if (bs.isAir() || bs.canBeReplaced()) {
                        // Comprobar si está justo encima de Obsidiana Llorosa
                        if (level.getBlockState(cursor.below()).is(Blocks.CRYING_OBSIDIAN)) {
                            // Probar a encender en Eje X
                            if (tryCreatePortalInFrame(level, cursor.immutable(), Direction.Axis.X)) {
                                ignited = true;
                            }
                            // Probar a encender en Eje Z
                            else if (tryCreatePortalInFrame(level, cursor.immutable(), Direction.Axis.Z)) {
                                ignited = true;
                            }
                        }
                    }
                }
            }
        }
        return ignited;
    }

    /**
     * Analiza si pos es el borde interior inferior de un marco de Obsidiana Llorosa válido (min 2x3, max 21x21).
     * Si es válido, rellena el interior con bloques de portal.
     */
    public static boolean tryCreatePortalInFrame(Level level, BlockPos bottomInteriorPos, Direction.Axis axis) {
        Direction widthDir = (axis == Direction.Axis.X) ? Direction.EAST : Direction.SOUTH;
        Direction negWidthDir = widthDir.getOpposite();

        // 1. Encontrar el extremo izquierdo del interior
        BlockPos left = bottomInteriorPos;
        while (level.getBlockState(left).isAir() && level.getBlockState(left.below()).is(Blocks.CRYING_OBSIDIAN)) {
            BlockPos next = left.relative(negWidthDir);
            if (level.getBlockState(next).is(Blocks.CRYING_OBSIDIAN)) {
                break; // Marco izquierdo alcanzado
            }
            if (!level.getBlockState(next).isAir()) {
                return false; // Obstáculo no válido
            }
            left = next;
        }

        // El bloque a la izquierda de 'left' debe ser Obsidiana Llorosa
        if (!level.getBlockState(left.relative(negWidthDir)).is(Blocks.CRYING_OBSIDIAN)) {
            return false;
        }

        // 2. Medir el ancho interior hacia la derecha
        int width = 0;
        BlockPos rightCursor = left;
        while (level.getBlockState(rightCursor).isAir() && level.getBlockState(rightCursor.below()).is(Blocks.CRYING_OBSIDIAN)) {
            width++;
            if (width > 21) return false;
            rightCursor = rightCursor.relative(widthDir);
        }

        // Ancho mínimo: 2 bloques de aire
        if (width < 2 || !level.getBlockState(rightCursor).is(Blocks.CRYING_OBSIDIAN)) {
            return false;
        }

        // 3. Medir la altura interior
        int height = 0;
        for (int dy = 0; dy <= 22; dy++) {
            boolean allAir = true;
            for (int w = 0; w < width; w++) {
                BlockPos check = left.relative(widthDir, w).above(dy);
                if (!level.getBlockState(check).isAir()) {
                    allAir = false;
                    break;
                }
            }

            if (allAir) {
                // Comprobar postes laterales
                BlockPos leftWall = left.relative(negWidthDir).above(dy);
                BlockPos rightWall = left.relative(widthDir, width).above(dy);
                if (!level.getBlockState(leftWall).is(Blocks.CRYING_OBSIDIAN) || !level.getBlockState(rightWall).is(Blocks.CRYING_OBSIDIAN)) {
                    return false;
                }
                height++;
            } else {
                // Debe ser el techo superior de Obsidiana Llorosa
                if (dy < 3) return false; // Altura mínima: 3 bloques de aire
                for (int w = 0; w < width; w++) {
                    BlockPos topCheck = left.relative(widthDir, w).above(dy);
                    if (!level.getBlockState(topCheck).is(Blocks.CRYING_OBSIDIAN)) {
                        return false;
                    }
                }
                break;
            }
        }

        if (height < 3 || height > 21) {
            return false;
        }

        // 4. ¡Marco Válido! Rellenar el interior con bloques de Portal del Abismo
        BlockState portalState = ModBlocks.ABYSS_PORTAL.get().defaultBlockState().setValue(AXIS, axis);
        for (int w = 0; w < width; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos p = left.relative(widthDir, w).above(h);
                level.setBlock(p, portalState, 2);
            }
        }

        // Sonido y partículas del despertar del portal
        level.playSound(null, left.getX() + 0.5, left.getY() + 1.5, left.getZ() + 0.5,
                SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.2F, 0.8F);

        return true;
    }
}

