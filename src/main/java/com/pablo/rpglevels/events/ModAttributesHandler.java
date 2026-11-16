package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class ModAttributesHandler {

    private static final ResourceLocation MOD_FORT_HP = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_fortitude_hp");
    private static final ResourceLocation MOD_FORT_ARMOR = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_fortitude_armor");
    private static final ResourceLocation MOD_FORT_TOUGHNESS = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_fortitude_toughness");
    private static final ResourceLocation MOD_FORT_KNOCKBACK = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_fortitude_knockback");
    private static final ResourceLocation MOD_FORT_DAMAGE = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_fortitude_damage");

    private static final ResourceLocation MOD_DEX_ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_dexterity_attack_speed");
    private static final ResourceLocation MOD_DEX_SPEED = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_dexterity_movement_speed");
    private static final ResourceLocation MOD_DEX_JUMP = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_dexterity_jump");
    private static final ResourceLocation MOD_DEX_FALL = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_dexterity_safe_fall");
    private static final ResourceLocation MOD_LUCK = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_luck_bonus");
    private static final ResourceLocation MOD_SKILL_PHYSICAL_DAMAGE = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_skill_physical_damage");
    private static final ResourceLocation MOD_SPIRIT_SPELL_POWER = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_spirit_spell_power");

    // Old modifier IDs for clean removal
    private static final ResourceLocation OLD_MOD_STR = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_buff_strength");
    private static final ResourceLocation OLD_MOD_HP = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_buff_health");
    private static final ResourceLocation OLD_MOD_AGI = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_buff_agility");

    public static void updatePlayerAttributes(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return;
        }

        float powerRatio = data.getPowerOutputRatio();
        float moveRatio = data.getMoveSpeedRatio();
        float atkSpeedRatio = data.getAttackSpeedRatio();
        float jumpRatio = data.getJumpHeightRatio();

        int rawFortitude = data.getTotalStatPoints(player, StatType.FORTITUDE);
        int rawDexterity = data.getTotalStatPoints(player, StatType.DEXTERITY);
        int rawSpirit = data.getTotalStatPoints(player, StatType.SPIRIT);
        int rawLuck = data.getTotalStatPoints(player, StatType.LUCK);

        // Supresión / Regulación de Poder Total (Power Control)
        double fortitude = rawFortitude * powerRatio;
        double dexterity = rawDexterity * powerRatio;
        double spirit = rawSpirit * powerRatio;
        double luck = rawLuck * powerRatio;

        // Clean up legacy modifiers if present
        removeModifier(player, Attributes.ATTACK_DAMAGE, OLD_MOD_STR);
        removeModifier(player, Attributes.MAX_HEALTH, OLD_MOD_HP);
        removeModifier(player, Attributes.MOVEMENT_SPEED, OLD_MOD_AGI);

        // 1. FORTITUDE (Max Health, Armor, Toughness, Knockback Resistance, Attack Damage)
        applyModifier(player, Attributes.MAX_HEALTH, MOD_FORT_HP, fortitude * 1.5, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ARMOR, MOD_FORT_ARMOR, fortitude * 0.35, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ARMOR_TOUGHNESS, MOD_FORT_TOUGHNESS, fortitude * 0.15, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_FORT_KNOCKBACK, fortitude * 0.015, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ATTACK_DAMAGE, MOD_FORT_DAMAGE, fortitude * 0.25, AttributeModifier.Operation.ADD_VALUE);

        // 2. DEXTERITY (Attack Speed, Movement Speed, Jump Strength, Safe Fall Distance)
        applyModifier(player, Attributes.ATTACK_SPEED, MOD_DEX_ATTACK_SPEED, dexterity * 0.025 * atkSpeedRatio, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_DEX_SPEED, dexterity * 0.012 * moveRatio, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.JUMP_STRENGTH, MOD_DEX_JUMP, dexterity * 0.01 * jumpRatio, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_DEX_FALL, dexterity * 0.4 * jumpRatio, AttributeModifier.Operation.ADD_VALUE);

        // 3. LUCK (Native Minecraft Luck: +0.2 per point for fishing and dungeon loot)
        applyModifier(player, Attributes.LUCK, MOD_LUCK, luck * 0.2, AttributeModifier.Operation.ADD_VALUE);

        // 4. Physical Skill Tree Damage Bonuses (+1 damage per Strength / Fortitude node)
        int bonusDmg = data.getBonusPhysicalAttackDamage();
        applyModifier(player, Attributes.ATTACK_DAMAGE, MOD_SKILL_PHYSICAL_DAMAGE, (double) bonusDmg * powerRatio, AttributeModifier.Operation.ADD_VALUE);

        // 5. SPIRIT (Poder Mágico Espiritual: +2.5% por punto de Espíritu)
        applyModifier(player, com.pablo.rpglevels.magic.spiritual.SpiritualAttributes.SPIRITUAL_SPELL_POWER,
                MOD_SPIRIT_SPELL_POWER, spirit * 0.025, AttributeModifier.Operation.ADD_VALUE);

        if (com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            boolean surge = data.isSpiritSurgeActive(player.level().getGameTime());
            com.pablo.rpglevels.compat.irons.IronsSpellsCompat.applySpiritModifiers(player, (int) spirit, data.getChannelMagicTier(), surge);
        }
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double value, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.amount() - value) < 0.0001 && existing.operation() == op) {
                return; // Valor idéntico; no remover ni recrear para evitar sobrecarga y eventos de cambio constantes
            }
            instance.removeModifier(id);
        }
        if (value > 0.0) {
            AttributeModifier modifier = new AttributeModifier(id, value, op);
            instance.addTransientModifier(modifier);
        }
    }

    private static void removeModifier(Player player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }
}
