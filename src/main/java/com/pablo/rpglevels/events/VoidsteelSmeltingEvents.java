package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.init.ModBlocks;
import com.pablo.rpglevels.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla la fundición de Voidsteel:
 * 1. Solo se puede fundir en un Alto Horno (Blast Furnace).
 * 2. Solo avanza la cocción si el combustible activo proviene de un Balde de Lava (litDuration == 20000).
 * 3. Si se intenta fundir con carbón u otro combustible, la temperatura se declara insuficiente,
 *    se emite humo y la barra de progreso se reinicia a 0.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class VoidsteelSmeltingEvents {

    private static final Map<BlockPos, Long> TRACKED_BLAST_FURNACES = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        BlockPos pos = event.getPos();
        if (event.getLevel().getBlockState(pos).is(Blocks.BLAST_FURNACE)) {
            TRACKED_BLAST_FURNACES.put(pos, event.getLevel().getGameTime());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (player.containerMenu instanceof BlastFurnaceMenu furnaceMenu) {
            ItemStack input = furnaceMenu.getSlot(0).getItem();
            if (isVoidsteelInput(input)) {
                Container container = furnaceMenu.getSlot(0).container;
                if (container instanceof AbstractFurnaceBlockEntity furnace) {
                    validateFurnaceHeat(player.serverLevel(), furnace.getBlockPos(), furnace, player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();
        if (gameTime % 10 != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            TRACKED_BLAST_FURNACES.keySet().removeIf(pos -> {
                if (!level.hasChunkAt(pos)) return false;
                if (!level.getBlockState(pos).is(Blocks.BLAST_FURNACE)) return true;

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof AbstractFurnaceBlockEntity furnace) {
                    ItemStack input = furnace.getItem(0);
                    if (isVoidsteelInput(input)) {
                        validateFurnaceHeat(level, pos, furnace, null);
                    }
                    return false;
                }
                return true;
            });
        }
    }

    private static boolean isVoidsteelInput(ItemStack stack) {
        return stack.is(ModItems.RAW_VOIDSTEEL.get())
                || stack.is(ModBlocks.VOIDSTEEL_ORE.asItem());
    }

    private static java.lang.reflect.Field DATA_ACCESS_FIELD;

    static {
        try {
            DATA_ACCESS_FIELD = AbstractFurnaceBlockEntity.class.getDeclaredField("dataAccess");
            DATA_ACCESS_FIELD.setAccessible(true);
        } catch (Throwable t) {
            for (java.lang.reflect.Field f : AbstractFurnaceBlockEntity.class.getDeclaredFields()) {
                if (net.minecraft.world.inventory.ContainerData.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    DATA_ACCESS_FIELD = f;
                    break;
                }
            }
        }
    }

    private static net.minecraft.world.inventory.ContainerData getFurnaceData(AbstractFurnaceBlockEntity furnace) {
        if (DATA_ACCESS_FIELD != null) {
            try {
                return (net.minecraft.world.inventory.ContainerData) DATA_ACCESS_FIELD.get(furnace);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void validateFurnaceHeat(ServerLevel level, BlockPos pos, AbstractFurnaceBlockEntity furnace, ServerPlayer player) {
        // En AbstractFurnaceBlockEntity, los datos de cocción están en dataAccess:
        // Slot 0: litTime
        // Slot 1: litDuration (Lava Bucket = 20000)
        // Slot 2: cookingProgress
        // Slot 3: cookingTotalTime
        try {
            net.minecraft.world.inventory.ContainerData data = getFurnaceData(furnace);
            if (data == null) return;

            int litTime = data.get(0);
            int litDuration = data.get(1);

            // Si el horno está encendido pero con combustible de baja temperatura (< 20000 ticks)
            if (litTime > 0 && litDuration < 20000) {
                // Reiniciar progreso de fundición a 0
                data.set(2, 0);

                // Efectos de enfriamiento / humo
                if (level.getGameTime() % 20 == 0) {
                    level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 4, 0.1, 0.1, 0.1, 0.02);
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.4f);

                    if (player != null && level.getGameTime() % 60 == 0) {
                        player.displayClientMessage(
                                Component.literal("§c⚠️ ¡Temperatura insuficiente! El Voidsteel requiere el calor de un Balde de Lava en un Alto Horno."),
                                true
                        );
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
