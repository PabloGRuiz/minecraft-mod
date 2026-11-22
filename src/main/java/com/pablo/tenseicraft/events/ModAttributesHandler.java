package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class ModAttributesHandler {

    private static final ResourceLocation MOD_FORT_HP = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_fortitude_hp");
    private static final ResourceLocation MOD_FORT_ARMOR = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_fortitude_armor");
    private static final ResourceLocation MOD_FORT_TOUGHNESS = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_fortitude_toughness");
    private static final ResourceLocation MOD_FORT_KNOCKBACK = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_fortitude_knockback");
    private static final ResourceLocation MOD_FORT_DAMAGE = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_fortitude_damage");
    private static final ResourceLocation MOD_SPIRIT_SWORD_DAMAGE = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_spirit_sword_damage");

    private static final ResourceLocation MOD_DEX_ATTACK_SPEED = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_dexterity_attack_speed");
    private static final ResourceLocation MOD_DEX_SPEED = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_dexterity_movement_speed");
    private static final ResourceLocation MOD_DEX_JUMP = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_dexterity_jump");
    private static final ResourceLocation MOD_DEX_FALL = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_dexterity_safe_fall");
    private static final ResourceLocation MOD_LUCK = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_luck_bonus");
    private static final ResourceLocation MOD_SKILL_PHYSICAL_DAMAGE = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_skill_physical_damage");
    private static final ResourceLocation MOD_SPIRIT_SPELL_POWER = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_spirit_spell_power");

    // God Blessing Passive Modifiers
    private static final ResourceLocation MOD_GOD_HERMES_SPEED = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "god_hermes_speed");
    private static final ResourceLocation MOD_GOD_HERMES_LUCK = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "god_hermes_luck");
    private static final ResourceLocation MOD_GOD_HEPHAESTUS_ARMOR = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "god_hephaestus_armor");
    private static final ResourceLocation MOD_GOD_APOLLO_ATK_SPEED = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "god_apollo_attack_speed");
    private static final ResourceLocation MOD_GOD_ARTEMIS_NIGHT_SPEED = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "god_artemis_night_speed");

    // Reforge Equipment Bonus Modifiers
    private static final ResourceLocation MOD_REFORGE_DAMAGE = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "reforge_damage");
    private static final ResourceLocation MOD_REFORGE_ATK_SPEED = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "reforge_attack_speed");
    private static final ResourceLocation MOD_REFORGE_HP = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "reforge_max_health");
    private static final ResourceLocation MOD_REFORGE_ARMOR = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "reforge_armor");
    private static final ResourceLocation MOD_REFORGE_SPEED = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "reforge_move_speed");
    private static final ResourceLocation MOD_REFORGE_LUCK = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "reforge_luck");

    // Old modifier IDs for clean removal
    private static final ResourceLocation OLD_MOD_STR = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_buff_strength");
    private static final ResourceLocation OLD_MOD_HP = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_buff_health");
    private static final ResourceLocation OLD_MOD_AGI = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "stat_buff_agility");

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

        // Baseline estándar = 5 puntos en cada habilidad
        double fortDelta = fortitude - 5.0;
        double dexDelta = dexterity - 5.0;
        double spiritDelta = spirit - 5.0;
        double luckDelta = luck - 5.0;

        // Clean up legacy modifiers if present
        removeModifier(player, Attributes.ATTACK_DAMAGE, OLD_MOD_STR);
        removeModifier(player, Attributes.MAX_HEALTH, OLD_MOD_HP);
        removeModifier(player, Attributes.MOVEMENT_SPEED, OLD_MOD_AGI);

        // 1. FORTITUDE (Max Health, Armor, Toughness, Knockback Resistance - ya NO da daño por sí sola)
        // En Fortitud = 5 -> +0 HP (20 de vida total). Por encima de 5: +2.0 HP/pt. Por debajo de 5: -1.5 HP/pt.
        double hpBonus = (fortDelta >= 0.0) ? (fortDelta * 2.0) : (fortDelta * 1.5);
        applyModifier(player, Attributes.MAX_HEALTH, MOD_FORT_HP, hpBonus, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        applyModifier(player, Attributes.ARMOR, MOD_FORT_ARMOR, Math.max(0.0, fortDelta * 0.35), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ARMOR_TOUGHNESS, MOD_FORT_TOUGHNESS, Math.max(0.0, fortDelta * 0.15), AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.KNOCKBACK_RESISTANCE, MOD_FORT_KNOCKBACK, Math.max(0.0, fortDelta * 0.015), AttributeModifier.Operation.ADD_VALUE);

        // 1b. DAÑO FÍSICO (Determinado por la relación entre Fortitud y Destreza; 0.0 cuando ambas son 5 -> 1.0 daño base)
        double relationDamageBonus = data.getPhysicalRelationDamageBonus(player);
        applyModifier(player, Attributes.ATTACK_DAMAGE, MOD_FORT_DAMAGE, relationDamageBonus, AttributeModifier.Operation.ADD_VALUE);

        // 1c. BONO DE ESPÍRITU EN ESPADAS ESPIRITUALES
        boolean holdingSpiritualSword = player.getMainHandItem().getItem() instanceof SpiritualSwordItem
                && !SpiritualSwordItem.isTemporarilyExhausted(player.getMainHandItem());
        double spiritSwordBonus = holdingSpiritualSword ? data.getSpiritualSwordSpiritBonus(player) : 0.0;
        applyModifier(player, Attributes.ATTACK_DAMAGE, MOD_SPIRIT_SWORD_DAMAGE, spiritSwordBonus, AttributeModifier.Operation.ADD_VALUE);

        // 2. DEXTERITY (Attack Speed, Movement Speed, Jump Strength, Safe Fall Distance - estándar = 0% en Destreza 5)
        applyModifier(player, Attributes.ATTACK_SPEED, MOD_DEX_ATTACK_SPEED, dexDelta * 0.025 * atkSpeedRatio, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_DEX_SPEED, dexDelta * 0.012 * moveRatio, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.JUMP_STRENGTH, MOD_DEX_JUMP, dexDelta * 0.01 * jumpRatio, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.SAFE_FALL_DISTANCE, MOD_DEX_FALL, Math.max(0.0, dexDelta * 0.4 * jumpRatio), AttributeModifier.Operation.ADD_VALUE);

        // 3. LUCK (Native Minecraft Luck: 0.0 en Suerte 5, +/-0.2 por punto)
        applyModifier(player, Attributes.LUCK, MOD_LUCK, luckDelta * 0.2, AttributeModifier.Operation.ADD_VALUE);

        // 4. Physical Skill Tree Damage Bonuses (+1 damage per Strength / Fortitude node)
        int bonusDmg = data.getBonusPhysicalAttackDamage();
        applyModifier(player, Attributes.ATTACK_DAMAGE, MOD_SKILL_PHYSICAL_DAMAGE, (double) bonusDmg * powerRatio, AttributeModifier.Operation.ADD_VALUE);

        // 5. SPIRIT (Poder Mágico Espiritual: +2.5% por punto de Espíritu sobre la base)
        applyModifier(player, com.pablo.tenseicraft.magic.spiritual.SpiritualAttributes.SPIRITUAL_SPELL_POWER,
                MOD_SPIRIT_SPELL_POWER, Math.max(0.0, spiritDelta * 0.025), AttributeModifier.Operation.ADD_VALUE);

        if (com.pablo.tenseicraft.compat.irons.IronsSpellsCompat.isLoaded()) {
            boolean surge = data.isSpiritSurgeActive(player.level().getGameTime());
            com.pablo.tenseicraft.compat.irons.IronsSpellsCompat.applySpiritModifiers(player, (int) Math.round(spirit), data.getChannelMagicTier(), surge);
        }

        // 6. BONIFICACIONES PASIVAS DE BENDICIÓN DIVINA (Dios Olímpico)
        com.pablo.tenseicraft.data.GreekGod activeGod = data.hasDivineBlessingUnlocked() ? data.getPatronGod() : null;
        int godTier = com.pablo.tenseicraft.data.GreekGod.getBlessingTier(data.getRank());

        double hermesSpeed = (activeGod == com.pablo.tenseicraft.data.GreekGod.HERMES)
                ? (godTier == 1 ? 0.10 : (godTier == 2 ? 0.15 : 0.20)) : 0.0;
        double hermesLuck = (activeGod == com.pablo.tenseicraft.data.GreekGod.HERMES)
                ? (godTier == 1 ? 1.5 : (godTier == 2 ? 2.5 : 3.5)) : 0.0;
        double hephaestusArmor = (activeGod == com.pablo.tenseicraft.data.GreekGod.HEPHAESTUS)
                ? (godTier * 2.0) : 0.0;
        double apolloAtkSpd = (activeGod == com.pablo.tenseicraft.data.GreekGod.APOLLO
                && GodBlessingEvents.isInSunlight(player, player.level()))
                ? (godTier == 1 ? 0.05 : (godTier == 2 ? 0.08 : 0.12)) : 0.0;

        boolean isNightTime = player.level().dimensionType().natural()
                && (player.level().getDayTime() % 24000L) >= 12500L
                && (player.level().getDayTime() % 24000L) <= 23500L;
        double artemisNightSpeed = (activeGod == com.pablo.tenseicraft.data.GreekGod.ARTEMIS && isNightTime)
                ? (godTier == 1 ? 0.08 : (godTier == 2 ? 0.12 : 0.16)) : 0.0;

        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_GOD_HERMES_SPEED, hermesSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.LUCK, MOD_GOD_HERMES_LUCK, hermesLuck, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ARMOR, MOD_GOD_HEPHAESTUS_ARMOR, hephaestusArmor, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ATTACK_SPEED, MOD_GOD_APOLLO_ATK_SPEED, apolloAtkSpd, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_GOD_ARTEMIS_NIGHT_SPEED, artemisNightSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

        // 7. BONIFICACIONES DE EQUIPAMIENTO REFORJADO (Yunque Mágico / Megu)
        double refDmg = 0.0;
        double refAtkSpd = 0.0;
        double refHp = 0.0;
        double refArmor = 0.0;
        double refSpeed = 0.0;
        double refLuck = 0.0;
        for (net.minecraft.world.item.ItemStack eq : com.pablo.tenseicraft.reforge.ReforgeManager.getActiveEquippedStacks(player)) {
            refDmg += com.pablo.tenseicraft.reforge.ReforgeManager.getStat(eq, com.pablo.tenseicraft.reforge.ReforgeManager.TAG_DMG);
            refAtkSpd += com.pablo.tenseicraft.reforge.ReforgeManager.getStat(eq, com.pablo.tenseicraft.reforge.ReforgeManager.TAG_ATK_SPD);
            refHp += com.pablo.tenseicraft.reforge.ReforgeManager.getStat(eq, com.pablo.tenseicraft.reforge.ReforgeManager.TAG_HP);
            refArmor += com.pablo.tenseicraft.reforge.ReforgeManager.getStat(eq, com.pablo.tenseicraft.reforge.ReforgeManager.TAG_ARMOR);
            refSpeed += com.pablo.tenseicraft.reforge.ReforgeManager.getStat(eq, com.pablo.tenseicraft.reforge.ReforgeManager.TAG_SPEED);
            refLuck += com.pablo.tenseicraft.reforge.ReforgeManager.getStat(eq, com.pablo.tenseicraft.reforge.ReforgeManager.TAG_LUCK);
        }
        applyModifier(player, Attributes.ATTACK_DAMAGE, MOD_REFORGE_DAMAGE, refDmg, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.ATTACK_SPEED, MOD_REFORGE_ATK_SPEED, refAtkSpd, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.MAX_HEALTH, MOD_REFORGE_HP, refHp, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        applyModifier(player, Attributes.ARMOR, MOD_REFORGE_ARMOR, refArmor, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(player, Attributes.MOVEMENT_SPEED, MOD_REFORGE_SPEED, refSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(player, Attributes.LUCK, MOD_REFORGE_LUCK, refLuck, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double value, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.amount() - value) < 0.0001 && existing.operation() == op) {
                return;
            }
            instance.removeModifier(id);
        }
        if (Math.abs(value) > 0.0001) {
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
