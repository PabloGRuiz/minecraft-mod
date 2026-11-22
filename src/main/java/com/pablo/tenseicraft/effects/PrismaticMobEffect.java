package com.pablo.tenseicraft.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Efecto beneficioso prismático con modificadores multiplicativos totales
 * que escalan sobre las estadísticas acumuladas del jugador.
 */
public class PrismaticMobEffect extends MobEffect {
    public PrismaticMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
