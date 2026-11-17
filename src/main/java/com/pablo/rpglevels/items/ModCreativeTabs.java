package com.pablo.rpglevels.items;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RpgLevelsMod.MOD_ID);

    public static final Supplier<CreativeModeTab> RPG_TAB = CREATIVE_MODE_TABS.register("rpg_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.EXPERIENCE_AMULET.get()))
                    .title(Component.translatable("itemGroup.rpglevels.rpg_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.EXPERIENCE_AMULET.get());
                        output.accept(ModItems.ANCIENT_RING.get());
                        output.accept(ModItems.WARRIOR_RING.get());
                        output.accept(ModItems.SPIRITUAL_SWORD.get());
                        output.accept(ModItems.ADVENTURER_SPAWN_EGG.get());
                        output.accept(ModItems.TOWNSWOMAN_SPAWN_EGG.get());
                        output.accept(ModItems.BANDIT_SPAWN_EGG.get());
                        output.accept(ModItems.UNDEAD_ADVENTURER_SPAWN_EGG.get());
                        output.accept(ModItems.WIND_MASTER_SPAWN_EGG.get());
                        output.accept(ModItems.GUILD_MASTER_SPAWN_EGG.get());
                        output.accept(ModItems.ALICE_SPAWN_EGG.get());
                        output.accept(ModItems.PELOTITA_SPAWN_EGG.get());
                        output.accept(ModItems.STARRY_SPAWN_EGG.get());
                        output.accept(ModItems.BRAMBI_SPAWN_EGG.get());
                        output.accept(ModItems.VOIDBORN_SPAWN_EGG.get());
                        output.accept(ModItems.VOIDBORN_EXECUTOR_SPAWN_EGG.get());
                        output.accept(ModItems.HORNLESS_VOIDBORN_SPAWN_EGG.get());
                        output.accept(ModItems.HORNED_VOIDBORN_SPAWN_EGG.get());
                        output.accept(ModItems.COLOSSAL_VOIDBORN_SPAWN_EGG.get());
                        output.accept(ModItems.ESCAPE_SCROLL.get());
                        output.accept(ModItems.ADVENTURER_CARD.get());
                        output.accept(ModItems.SPIRITUAL_SPELLBOOK.get());
                        output.accept(ModItems.HIGH_EXPERIENCE_BOTTLE.get());
                        output.accept(ModItems.LESSER_SOUL_FRAGMENT.get());
                        output.accept(ModItems.RESONANT_SOUL_CRYSTAL.get());
                        output.accept(ModItems.RADIANT_SOUL_GEM.get());
                        output.accept(ModItems.PRIMORDIAL_SOUL_CORE.get());
                        output.accept(com.pablo.rpglevels.init.ModBlocks.ABYSSAL_STONE.get());
                        output.accept(com.pablo.rpglevels.init.ModBlocks.VOIDSTEEL_ORE.get());
                        output.accept(ModItems.RAW_VOIDSTEEL.get());
                        output.accept(ModItems.VOIDSTEEL_INGOT.get());
                        output.accept(com.pablo.rpglevels.init.ModBlocks.RIFT_MONOLITH.get());
                    })
                    .build());
}
