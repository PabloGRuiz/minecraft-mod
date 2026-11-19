package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.items.ModItems;
import com.pablo.rpglevels.items.PrismaticPotionItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Recetas de alquimia en el Soporte para Pociones para el sistema de Pociones Prismáticas
 * y el Frasco de Experiencia RPG universal.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class PrismaticBrewingEvents {

    @SubscribeEvent
    public static void onRegisterBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        // 1. Agua o Poción Rara + Pétalos Arcoíris -> Poción Prismática (base alquímica)
        addCustomRecipe(builder,
                input -> {
                    if (!input.is(Items.POTION)) return false;
                    PotionContents contents = input.get(DataComponents.POTION_CONTENTS);
                    return contents != null && (contents.is(Potions.WATER) || contents.is(Potions.AWKWARD));
                },
                ing -> ing.is(ModItems.RAINBOW_PETALS.get()),
                input -> new ItemStack(ModItems.PRISMATIC_POTION.get()));

        // 2. Poción Prismática + Polvo de Blaze -> Poción Prismática de Fuerza (x1.20 Daño)
        addSimpleMix(builder, ModItems.PRISMATIC_POTION, () -> Items.BLAZE_POWDER, ModItems.PRISMATIC_STRENGTH_POTION);

        // 3. Poción Prismática + Azúcar -> Poción Prismática de Rapidez (x1.20 Velocidad)
        addSimpleMix(builder, ModItems.PRISMATIC_POTION, () -> Items.SUGAR, ModItems.PRISMATIC_SWIFTNESS_POTION);

        // 4. Poción Prismática + Sandía Reluciente -> Poción Prismática de Curación (20% Vida Máx.)
        addSimpleMix(builder, ModItems.PRISMATIC_POTION, () -> Items.GLISTERING_MELON_SLICE, ModItems.PRISMATIC_HEALING_POTION);

        // 5. Poción Prismática + Frasco con Hechizo (XP vanilla) -> Frasco de Experiencia RPG Universal
        addSimpleMix(builder, ModItems.PRISMATIC_POTION, () -> Items.EXPERIENCE_BOTTLE, ModItems.RPG_EXPERIENCE_BOTTLE);

        // 6. Redstone: extender duración (3:00 -> 8:00) en Fuerza / Rapidez Prismática sin mejoras previas
        addCustomRecipe(builder,
                input -> (input.is(ModItems.PRISMATIC_STRENGTH_POTION.get())
                        || input.is(ModItems.PRISMATIC_SWIFTNESS_POTION.get()))
                        && PrismaticPotionItem.getTier(input) == 0
                        && !PrismaticPotionItem.isExtended(input),
                ing -> ing.is(Items.REDSTONE),
                input -> PrismaticPotionItem.withExtended(input, true));

        // 7. Polvo de Glowstone: potenciar a Nivel II (+40% / 40% Vida) en Fuerza / Rapidez / Curación Prismática sin mejoras previas
        addCustomRecipe(builder,
                input -> (input.is(ModItems.PRISMATIC_STRENGTH_POTION.get())
                        || input.is(ModItems.PRISMATIC_SWIFTNESS_POTION.get())
                        || input.is(ModItems.PRISMATIC_HEALING_POTION.get()))
                        && PrismaticPotionItem.getTier(input) == 0
                        && !PrismaticPotionItem.isExtended(input),
                ing -> ing.is(Items.GLOWSTONE_DUST),
                input -> PrismaticPotionItem.withTier(input, 1));
    }

    private static void addSimpleMix(PotionBrewing.Builder builder,
                                     Supplier<? extends Item> inputItem,
                                     Supplier<? extends Item> ingredientItem,
                                     Supplier<? extends Item> outputItem) {
        addCustomRecipe(builder,
                stack -> stack.is(inputItem.get()),
                ing -> ing.is(ingredientItem.get()),
                stack -> new ItemStack(outputItem.get()));
    }

    private static void addCustomRecipe(PotionBrewing.Builder builder,
                                        Predicate<ItemStack> inputCheck,
                                        Predicate<ItemStack> ingredientCheck,
                                        Function<ItemStack, ItemStack> outputFactory) {
        builder.addRecipe(new IBrewingRecipe() {
            @Override
            public boolean isInput(ItemStack input) {
                return !input.isEmpty() && inputCheck.test(input);
            }

            @Override
            public boolean isIngredient(ItemStack ingredient) {
                return !ingredient.isEmpty() && ingredientCheck.test(ingredient);
            }

            @Override
            public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
                if (isInput(input) && isIngredient(ingredient)) {
                    return outputFactory.apply(input);
                }
                return ItemStack.EMPTY;
            }
        });
    }
}
