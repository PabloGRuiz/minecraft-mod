package com.pablo.rpglevels.init;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.items.ModItems;
import com.pablo.rpglevels.world.dimension.RiftMonolithBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RpgLevelsMod.MOD_ID);

    public static final DeferredBlock<RiftMonolithBlock> RIFT_MONOLITH = registerBlock("rift_monolith",
            () -> new RiftMonolithBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.DEEPSLATE)
                    .strength(4.0f, 1200.0f)
                    .lightLevel(state -> 12)
                    .requiresCorrectToolForDrops()),
            block -> new BlockItem(block, new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredBlock<com.pablo.rpglevels.world.dimension.AbyssPortalBlock> ABYSS_PORTAL =
            BLOCKS.register("abyss_portal", com.pablo.rpglevels.world.dimension.AbyssPortalBlock::new);

    // Piedra del Abismo: Roca única de escala de grises/ceniza que compone todo el terreno del Abismo
    public static final DeferredBlock<Block> ABYSSAL_STONE = registerBlock("abyssal_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .sound(SoundType.STONE)
                    .strength(2.2f, 6.0f)
                    .requiresCorrectToolForDrops()),
            block -> new BlockItem(block, new Item.Properties()));

    // Mineral de Voidsteel: Vetas luminosas violeta fluorescente incrustadas en la roca del Abismo
    public static final DeferredBlock<Block> VOIDSTEEL_ORE = registerBlock("voidsteel_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(
                    net.minecraft.util.valueproviders.UniformInt.of(3, 7),
                    BlockBehaviour.Properties.of()
                            .sound(SoundType.DEEPSLATE)
                            .strength(4.5f, 9.0f)
                            .lightLevel(state -> 11) // Bloque luminoso
                            .requiresCorrectToolForDrops()),
            block -> new BlockItem(block, new Item.Properties().rarity(Rarity.RARE)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> blockSupplier, Function<T, BlockItem> itemFactory) {
        DeferredBlock<T> block = BLOCKS.register(name, blockSupplier);
        ModItems.ITEMS.register(name, () -> itemFactory.apply(block.get()));
        return block;
    }
}
