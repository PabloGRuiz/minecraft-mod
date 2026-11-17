package com.pablo.rpglevels.magic.spiritual;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.magic.spiritual.spells.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SpiritualSpells {

    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, RpgLevelsMod.MOD_ID);

    public static final DeferredHolder<AbstractSpell, PhysicalBoostSpell> PHYSICAL_BOOST =
            SPELLS.register("physical_boost", PhysicalBoostSpell::new);

    public static final DeferredHolder<AbstractSpell, LowHealingSpell> LOW_HEALING =
            SPELLS.register("low_healing", LowHealingSpell::new);

    public static final DeferredHolder<AbstractSpell, LowParalizeSpell> LOW_PARALIZE =
            SPELLS.register("low_paralize", LowParalizeSpell::new);

    public static final DeferredHolder<AbstractSpell, LowPurificationSpell> LOW_PURIFICATION =
            SPELLS.register("low_purification", LowPurificationSpell::new);

    public static final DeferredHolder<AbstractSpell, ElementalBallSpell> ELEMENTAL_BALL =
            SPELLS.register("elemental_ball", ElementalBallSpell::new);

    public static final DeferredHolder<AbstractSpell, MagicAffinitySpell> MAGIC_AFFINITY =
            SPELLS.register("magic_affinity", MagicAffinitySpell::new);

    // =========================================================================
    // HABILIDADES DESPERTADAS INNATAS DE LA ESPADA ESPIRITUAL
    // =========================================================================
    public static final DeferredHolder<AbstractSpell, SpiritualSlashSpell> SPIRITUAL_SLASH =
            SPELLS.register("spiritual_slash", SpiritualSlashSpell::new);

    public static final DeferredHolder<AbstractSpell, SoulVortexSpell> SOUL_VORTEX =
            SPELLS.register("soul_vortex", SoulVortexSpell::new);

    public static final DeferredHolder<AbstractSpell, ShadowStepSpell> SHADOW_STEP =
            SPELLS.register("shadow_step", ShadowStepSpell::new);

    public static final DeferredHolder<AbstractSpell, SpiritualOverdriveSpell> SPIRITUAL_OVERDRIVE =
            SPELLS.register("spiritual_overdrive", SpiritualOverdriveSpell::new);

    public static final DeferredHolder<AbstractSpell, DimensionalJudgementSpell> DIMENSIONAL_JUDGEMENT =
            SPELLS.register("dimensional_judgement", DimensionalJudgementSpell::new);

    public static final DeferredHolder<AbstractSpell, ThousandBladesSpell> THOUSAND_BLADES =
            SPELLS.register("thousand_blades", ThousandBladesSpell::new);

    public static final DeferredHolder<AbstractSpell, SpiritDominionSpell> SPIRIT_DOMINION =
            SPELLS.register("spirit_dominion", SpiritDominionSpell::new);

    public static final DeferredHolder<AbstractSpell, CelestialPiercerSpell> CELESTIAL_PIERCER =
            SPELLS.register("celestial_piercer", CelestialPiercerSpell::new);

    public static final DeferredHolder<AbstractSpell, ConnectRealitiesSpell> CONNECT_REALITIES =
            SPELLS.register("connect_realities", ConnectRealitiesSpell::new);

    public static final DeferredHolder<AbstractSpell, SummonLesserSoulSpell> SUMMON_LESSER_SOUL =
            SPELLS.register("summon_lesser_soul", SummonLesserSoulSpell::new);

    public static final DeferredHolder<AbstractSpell, SpiritualBeamSpell> SPIRITUAL_BEAM =
            SPELLS.register("spiritual_beam", SpiritualBeamSpell::new);

    // =========================================================================
    // HABILIDADES EXCLUSIVAS DE ZANPAKUTO: SENBONZAKURA (BYAKUYA KUCHIKI)
    // =========================================================================
    public static final DeferredHolder<AbstractSpell, SenbonzakuraChireSpell> SENBONZAKURA_CHIRE =
            SPELLS.register("senbonzakura_chire", SenbonzakuraChireSpell::new);

    public static final DeferredHolder<AbstractSpell, SenbonzakuraVortexSpell> SENBONZAKURA_VORTEX =
            SPELLS.register("senbonzakura_vortex", SenbonzakuraVortexSpell::new);
}

