package com.pablo.rpglevels.init;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.effects.AbyssalWardEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
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
}

