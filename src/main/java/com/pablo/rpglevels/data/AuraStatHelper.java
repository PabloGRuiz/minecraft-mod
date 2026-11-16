package com.pablo.rpglevels.data;

import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import com.pablo.rpglevels.mobs.MobRankHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * Calculador de equivalencia universal de Aura (Poder del Alma) para cualquier criatura.
 * Funciona de forma 100% automática y agnóstica para entidades Vanilla y de cualquier Mod
 * (Alex's Mobs, Cataclysm, Mowzie's, Twilight Forest, Iron's Spells, Born in Chaos, etc.).
 *
 * Mapea dinámicamente los atributos nativos de la criatura (Vida, Daño, Velocidad, Armadura,
 * Dureza, Resistencia al empuje, Rangos de amenaza, tags de Boss y volumen de masa) a la misma
 * escala exacta de los 4 Atributos RPG del jugador (Fortaleza, Destreza, Espíritu, Suerte).
 */
public class AuraStatHelper {

    public static final TagKey<EntityType<?>> C_BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));

    public record AuraBreakdown(
            float fortitude,
            float dexterity,
            float spirit,
            float luck,
            float presenceBonus,
            float totalAura
    ) {}

    public static float calculateEntityAura(LivingEntity entity) {
        return getAuraBreakdown(entity).totalAura();
    }

    public static AuraBreakdown getAuraBreakdown(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return new AuraBreakdown(1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        }

        // 1. JUGADORES: Promedio exacto de sus 4 estadísticas asignadas
        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                float fort = data.getTotalStatPoints(player, StatType.FORTITUDE);
                float dex = data.getTotalStatPoints(player, StatType.DEXTERITY);
                float spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
                float luck = data.getTotalStatPoints(player, StatType.LUCK);
                float total = Math.max(1.0f, (fort + dex + spirit + luck) / 4.0f);
                return new AuraBreakdown(fort, dex, spirit, luck, 0.0f, total);
            }
            return new AuraBreakdown(1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f);
        }

        // 2. AVENTUREROS DEL GREMIO: Escala oficial con Rango y Nivel
        if (entity instanceof AdventurerNpcEntity adv) {
            float rankPower = (adv.getRank().getIndex() * 12.0f) + (adv.getAdventurerLevel() * 1.5f) + 4.0f;
            return new AuraBreakdown(rankPower, rankPower, rankPower, rankPower, 0.0f, Math.max(2.0f, rankPower));
        }

        if (entity instanceof TownswomanNpcEntity) {
            return new AuraBreakdown(8.0f, 8.0f, 8.0f, 8.0f, 0.0f, 8.0f);
        }

        // 3. CÁLCULO DINÁMICO UNIVERSAL PARA CUALQUIER MOB (VANILLA O DE CUALQUIER MOD)

        // A. EQUIVALENTE DE FORTALEZA (Basado en Max HP + Resistencia al Empuje)
        double maxHp = entity.getMaxHealth();
        float equivFort;
        if (maxHp <= 20.0) {
            equivFort = (float) Math.max(0.4, (maxHp / 20.0) * 2.0); // Gallinas ~0.4, Cerdos/Vacas ~1.0
        } else {
            // Cada 1.5 HP por encima de 20 equivale a 1 punto de Fortaleza
            equivFort = (float) (2.0 + (maxHp - 20.0) / 1.5);
        }

        // Resistencia al empuje (0.0 a 1.0): Indica solidez y masa física masiva
        double kbResist = getAttrValue(entity, Attributes.KNOCKBACK_RESISTANCE, 0.0);
        if (kbResist > 0.0) {
            equivFort += (float) (kbResist * 25.0);
        }

        // B. EQUIVALENTE DE DESTREZA (Basado en Velocidad de movimiento + Vuelo/Agilidad)
        double speed = getAttrValue(entity, Attributes.MOVEMENT_SPEED, 0.10);
        float equivDex = (float) Math.max(0.5, ((speed - 0.10) / 0.0012) * 0.12);
        if (entity instanceof AbstractHorse) {
            equivDex += 15.0f;
        }
        if (entity instanceof FlyingAnimal || entity instanceof FlyingMob || entity.isNoGravity()) {
            equivDex += 8.0f;
        }

        // C. EQUIVALENTE DE ESPÍRITU (Mapeo heurístico automático del poder del alma)
        float equivSpirit = 1.0f;

        // 1. Daño de Ataque: Monstruos que golpean fuerte canalizan gran presión de alma
        double attackDamage = getAttrValue(entity, Attributes.ATTACK_DAMAGE, 0.0);
        if (attackDamage > 2.0) {
            equivSpirit += (float) ((attackDamage - 2.0) * 2.2);
        }

        // 2. Rango de Amenaza Automático (Rank F a Genesis basado en daño y vida)
        ThreatLevel threat = MobRankHandler.getThreatLevel(entity);
        float threatBaseSpirit = switch (threat) {
            case F -> 1.0f;
            case E -> 3.0f;
            case D -> 8.0f;
            case C -> 18.0f;
            case B -> 36.0f;
            case A -> 58.0f;
            case HAZARD -> 88.0f;
            case CALAMITY -> 120.0f;
            case DISASTER -> 150.0f;
            case GRAND_DISASTER -> 200.0f;
            case CATASTROPHE -> 250.0f;
            case GENESIS -> 320.0f;
        };
        equivSpirit = Math.max(equivSpirit, threatBaseSpirit);

        // 3. Indicadores de Jefe de Mod o Vanilla (Tags de Boss, dimensiones restringidas, etc.)
        if (entity.getType().is(C_BOSSES)) {
            equivSpirit += 40.0f;
        }
        if (!entity.canChangeDimensions(entity.level(), entity.level())) {
            equivSpirit += 20.0f;
        }

        // 4. Naturaleza mística / proyectiles / atacantes a distancia
        if (entity instanceof RangedAttackMob) {
            equivSpirit += 15.0f;
        }

        // 5. Soporte dinámico para mods mágicos (Iron's Spells u otros)
        double spellPower = getCustomAttributeValue(entity, "irons_spellbooks:spell_power");
        if (spellPower > 0.0) {
            equivSpirit += (float) (spellPower * 20.0);
        }

        // 6. Bestias no hostiles / animales pacíficos (mantienen espíritu modesto a menos que sean legendarias)
        if (entity instanceof Animal && !(entity instanceof Enemy) && maxHp <= 40.0 && attackDamage <= 2.0) {
            equivSpirit = Math.min(equivSpirit, 1.5f);
        } else if (entity instanceof AbstractVillager) {
            equivSpirit = 3.0f;
        }

        // D. EQUIVALENTE DE SUERTE / DEFENSA (Armadura natural y Dureza)
        double armor = getAttrValue(entity, Attributes.ARMOR, 0.0);
        double tough = getAttrValue(entity, Attributes.ARMOR_TOUGHNESS, 0.0);
        float equivLuck = (float) (armor * 1.5 + tough * 2.5);

        // E. BONIFICACIÓN DE PRESENCIA FÍSICA (Volumen de la criatura)
        // Titanes gigantescos de mods (dragones, monstruosidades de 4x4x6) irradian presencia imponente
        float volume = entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight();
        float presenceBonus = 0.0f;
        if (volume > 3.0f) {
            presenceBonus = Math.min(25.0f, (volume - 3.0f) * 0.8f);
        }

        // Promedio de las 4 estadísticas equivalentes + presencia
        float baseAverage = (equivFort + equivDex + equivSpirit + equivLuck) / 4.0f;
        float totalAura = Math.max(0.5f, baseAverage + presenceBonus);

        return new AuraBreakdown(equivFort, equivDex, equivSpirit, equivLuck, presenceBonus, totalAura);
    }

    private static double getAttrValue(LivingEntity entity, Holder<Attribute> attr, double def) {
        AttributeInstance inst = entity.getAttribute(attr);
        return inst != null ? inst.getValue() : def;
    }

    private static double getCustomAttributeValue(LivingEntity entity, String attrId) {
        ResourceLocation rl = ResourceLocation.tryParse(attrId);
        if (rl != null && BuiltInRegistries.ATTRIBUTE.containsKey(rl)) {
            Attribute attr = BuiltInRegistries.ATTRIBUTE.get(rl);
            if (attr != null) {
                AttributeInstance inst = entity.getAttribute(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attr));
                return inst != null ? inst.getValue() : 0.0;
            }
        }
        return 0.0;
    }
}
