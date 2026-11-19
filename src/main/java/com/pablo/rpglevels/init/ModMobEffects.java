package com.pablo.rpglevels.init;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.effects.AbyssalWardEffect;
import com.pablo.rpglevels.effects.PrismaticMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, RpgLevelsMod.MOD_ID);

    /**
     * Manto de Protección Espiritual de Alice (Abyssal Ward):
     * Protege el alma del jugador contra el aplastante rechazo del Vacío Abisal durante 20 minutos.
     */
    public static final DeferredHolder<MobEffect, AbyssalWardEffect> ABYSSAL_WARD =
            MOB_EFFECTS.register("abyssal_ward", () -> new AbyssalWardEffect(MobEffectCategory.BENEFICIAL, 0x4B0082));

    /**
     * Fuerza Prismática: multiplica el daño de ataque total en +20% por nivel (x1.20 en nivel I, x1.40 en nivel II).
     */
    public static final DeferredHolder<MobEffect, MobEffect> PRISMATIC_STRENGTH =
            MOB_EFFECTS.register("prismatic_strength", () -> new PrismaticMobEffect(MobEffectCategory.BENEFICIAL, 0xFF3E64)
                    .addAttributeModifier(
                            Attributes.ATTACK_DAMAGE,
                            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "effect.prismatic_strength"),
                            0.20D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /**
     * Rapidez Prismática: multiplica la velocidad de movimiento total en +20% por nivel (x1.20 en nivel I, x1.40 en nivel II).
     */
    public static final DeferredHolder<MobEffect, MobEffect> PRISMATIC_SWIFTNESS =
            MOB_EFFECTS.register("prismatic_swiftness", () -> new PrismaticMobEffect(MobEffectCategory.BENEFICIAL, 0x38E4FF)
                    .addAttributeModifier(
                            Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "effect.prismatic_swiftness"),
                            0.20D,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
}

