package com.pablo.tenseicraft.reforge;

import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.items.SoulCrystalItem;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Núcleo del sistema de Reforjas y Gambling con Cristales de Alma.
 * Gestiona compatibilidad de equipo, NBT de rango (F..Génesis), cálculo de probabilidades
 * según densidad de cristales, tiradas de estadísticas/efectos y límite mortal de Megu.
 */
public final class ReforgeManager {

    public static final String TAG_ROOT = "RpgReforge";
    public static final String TAG_RANK = "RankOrdinal";
    public static final String TAG_DMG = "BonusDamage";
    public static final String TAG_ATK_SPD = "BonusAttackSpeed";
    public static final String TAG_HP = "BonusMaxHealth";
    public static final String TAG_ARMOR = "BonusArmor";
    public static final String TAG_SPEED = "BonusMoveSpeed";
    public static final String TAG_LUCK = "BonusLuck";
    public static final String TAG_EFFECTS = "Effects";

    /** Límite máximo que Megu puede forjar con sus manos mortales; a partir de A requiere Yunque Mágico. */
    public static final ThreatLevel MEGU_MAX_RANK = ThreatLevel.B;

    public enum EffectType {
        REGENERATION("regeneration", "❤ Regeneración Vital", ChatFormatting.LIGHT_PURPLE, true),
        SPEED("speed", "💨 Celeridad del Viento", ChatFormatting.AQUA, true),
        STRENGTH("strength", "⚔ Fuerza Interior", ChatFormatting.RED, true),
        RESISTANCE("resistance", "🛡 Baluarte Protector", ChatFormatting.BLUE, true),
        LIFESTEAL("lifesteal", "🩸 Vampirismo", ChatFormatting.DARK_RED, false),
        FIRE_ASPECT("fire_aspect", "🔥 Filo Ígneo", ChatFormatting.GOLD, false),
        FROSTBITE("frostbite", "❄ Toque Gélido", ChatFormatting.AQUA, false),
        LIGHTNING("lightning", "⚡ Descarga Relámpago", ChatFormatting.YELLOW, false),
        VENOM("venom", "☠ Toxina Abisal", ChatFormatting.DARK_GREEN, false);

        private final String id;
        private final String displayName;
        private final ChatFormatting color;
        private final boolean passive;

        EffectType(String id, String displayName, ChatFormatting color, boolean passive) {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
            this.passive = passive;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public ChatFormatting getColor() { return color; }
        public boolean isPassive() { return passive; }

        public static EffectType byId(String id) {
            if (id == null) return null;
            for (EffectType type : values()) {
                if (type.id.equals(id)) return type;
            }
            return null;
        }
    }

    private ReforgeManager() {}

    public static boolean isSpiritualSword(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SpiritualSwordItem;
    }

    public static boolean canReforge(ItemStack stack) {
        if (stack == null || stack.isEmpty() || isSpiritualSword(stack) || stack.getMaxStackSize() > 1) {
            return false;
        }
        Item item = stack.getItem();
        if (item instanceof SoulCrystalItem || item instanceof PotionItem || item instanceof BlockItem || item instanceof SpawnEggItem) {
            return false;
        }
        if (item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TieredItem
                || item instanceof ArmorItem
                || item instanceof ProjectileWeaponItem
                || item instanceof TridentItem
                || item instanceof ShieldItem
                || item instanceof MaceItem
                || stack.isDamageableItem()) {
            return true;
        }
        var mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        return mods != null && !mods.modifiers().isEmpty();
    }

    public static boolean isValidEquippedReforge(ItemStack stack, EquipmentSlot slot) {
        if (stack == null || stack.isEmpty() || getReforgeRankOrdinal(stack) < 0) {
            return false;
        }
        Item item = stack.getItem();
        if (slot.isArmor()) {
            return item instanceof ArmorItem || item instanceof Equipable;
        }
        if (slot == EquipmentSlot.OFFHAND) {
            return !(item instanceof ArmorItem)
                    && (item instanceof ShieldItem
                    || item instanceof SwordItem
                    || item instanceof ProjectileWeaponItem
                    || item instanceof TridentItem
                    || stack.is(Items.TOTEM_OF_UNDYING));
        }
        return !(item instanceof ArmorItem);
    }

    public static List<ItemStack> getActiveEquippedStacks(Player player) {
        List<ItemStack> list = new ArrayList<>();
        if (player == null) return list;

        ItemStack main = player.getMainHandItem();
        if (isValidEquippedReforge(main, EquipmentSlot.MAINHAND)) {
            list.add(main);
        }
        ItemStack off = player.getOffhandItem();
        if (isValidEquippedReforge(off, EquipmentSlot.OFFHAND)) {
            list.add(off);
        }
        EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < armorSlots.length && i < player.getInventory().armor.size(); i++) {
            ItemStack armor = player.getInventory().armor.get(i);
            if (isValidEquippedReforge(armor, armorSlots[i])) {
                list.add(armor);
            }
        }
        return list;
    }

    @SuppressWarnings("deprecation")
    private static CompoundTag getRootTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag unsafe = cd.getUnsafe();
        if (unsafe != null && unsafe.contains(TAG_ROOT, Tag.TAG_COMPOUND)) {
            return unsafe.getCompound(TAG_ROOT);
        }
        return null;
    }

    public static int getReforgeRankOrdinal(ItemStack stack) {
        CompoundTag root = getRootTag(stack);
        if (root == null || !root.contains(TAG_RANK, Tag.TAG_INT)) {
            return -1;
        }
        int ord = root.getInt(TAG_RANK);
        ThreatLevel[] values = ThreatLevel.values();
        return (ord >= 0 && ord < values.length) ? ord : -1;
    }

    public static ThreatLevel getReforgeRank(ItemStack stack) {
        int ord = getReforgeRankOrdinal(stack);
        return (ord >= 0) ? ThreatLevel.values()[ord] : null;
    }

    public static ThreatLevel getNextTargetRank(ItemStack stack) {
        if (!canReforge(stack)) return null;
        int current = getReforgeRankOrdinal(stack);
        ThreatLevel[] values = ThreatLevel.values();
        if (current < 0) return ThreatLevel.F;
        if (current >= values.length - 1) return null; // Ya es Génesis
        return values[current + 1];
    }

    public static double getStat(ItemStack stack, String key) {
        CompoundTag root = getRootTag(stack);
        return (root != null && root.contains(key)) ? root.getDouble(key) : 0.0;
    }

    public static Map<EffectType, Integer> getEffects(ItemStack stack) {
        Map<EffectType, Integer> map = new LinkedHashMap<>();
        CompoundTag root = getRootTag(stack);
        if (root == null || !root.contains(TAG_EFFECTS, Tag.TAG_COMPOUND)) {
            return map;
        }
        CompoundTag effTag = root.getCompound(TAG_EFFECTS);
        for (EffectType type : EffectType.values()) {
            if (effTag.contains(type.getId())) {
                int tier = Math.max(1, Math.min(3, effTag.getInt(type.getId())));
                map.put(type, tier);
            }
        }
        return map;
    }

    public static int getCrystalDensity(SoulCrystalItem.Tier tier) {
        return switch (tier) {
            case LESSER -> 1;
            case RESONANT -> 5;
            case RADIANT -> 25;
            case PRIMORDIAL -> 125;
        };
    }

    public static int getRequiredSoulPowerForMax(ThreatLevel rank) {
        if (rank == null) return 3;
        return switch (rank) {
            case F -> 3;
            case E -> 6;
            case D -> 12;
            case C -> 25;
            case B -> 50;
            case A -> 100;
            case HAZARD -> 200;
            case CALAMITY -> 375;
            case DISASTER -> 625;
            case GRAND_DISASTER -> 1000;
            case CATASTROPHE -> 1500;
            case GENESIS -> 2500;
        };
    }

    public static int computeSoulPower(int lesser, int resonant, int radiant, int primordial) {
        return Math.max(0, lesser)
                + Math.max(0, resonant) * 5
                + Math.max(0, radiant) * 25
                + Math.max(0, primordial) * 125;
    }

    public static float calculateSuccessChance(ThreatLevel targetRank, int lesser, int resonant,
                                               int radiant, int primordial, int playerLuckPoints) {
        int totalPower = computeSoulPower(lesser, resonant, radiant, primordial);
        if (totalPower <= 0 || targetRank == null) {
            return 0.0f;
        }
        int req = getRequiredSoulPowerForMax(targetRank);
        double ratio = Math.min(1.0, (double) totalPower / Math.max(1, req));
        double baseChance = 5.0 + 90.0 * Math.pow(ratio, 0.85);
        double luckBonus = Math.max(0, playerLuckPoints - 5) * 0.25;
        float clamped = (float) Math.min(95.0, Math.max(1.0, baseChance + luckBonus));
        return Math.round(clamped * 10.0f) / 10.0f;
    }

    /**
     * Aplica la mejora de rango y los rolls aleatorios de estadísticas/efectos sobre el ítem.
     * Preserva encantamientos, durabilidad y otros datos existentes del ítem.
     */
    public static String applySuccessfulReforge(ItemStack stack, RandomSource random) {
        ThreatLevel nextRank = getNextTargetRank(stack);
        if (nextRank == null) return "";

        CustomData cd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag fullTag = cd.copyTag();
        CompoundTag root = fullTag.contains(TAG_ROOT, Tag.TAG_COMPOUND)
                ? fullTag.getCompound(TAG_ROOT).copy()
                : new CompoundTag();

        root.putInt(TAG_RANK, nextRank.ordinal());

        double scale = 1.0 + (nextRank.ordinal() * 0.45);
        int statRolls = (nextRank.ordinal() >= ThreatLevel.B.ordinal() && random.nextFloat() < 0.65f) ? 2 : 1;
        List<String> gainNotes = new ArrayList<>();

        for (int r = 0; r < statRolls; r++) {
            int pick = random.nextInt(6);
            switch (pick) {
                case 0 -> {
                    double add = round2((0.8 + random.nextDouble() * 1.4) * scale);
                    root.putDouble(TAG_DMG, round2(root.getDouble(TAG_DMG) + add));
                    gainNotes.add("§c+" + fmt(add) + " Daño");
                }
                case 1 -> {
                    double add = round3((0.03 + random.nextDouble() * 0.03) * scale);
                    root.putDouble(TAG_ATK_SPD, round3(root.getDouble(TAG_ATK_SPD) + add));
                    gainNotes.add("§e+" + fmt(add * 100.0) + "% Vel.Atq");
                }
                case 2 -> {
                    double add = round2((1.0 + random.nextDouble() * 1.5) * scale);
                    root.putDouble(TAG_HP, round2(root.getDouble(TAG_HP) + add));
                    gainNotes.add("§d+" + fmt(add) + " HP");
                }
                case 3 -> {
                    double add = round2((0.6 + random.nextDouble() * 0.9) * scale);
                    root.putDouble(TAG_ARMOR, round2(root.getDouble(TAG_ARMOR) + add));
                    gainNotes.add("§b+" + fmt(add) + " Armadura");
                }
                case 4 -> {
                    double add = round3((0.015 + random.nextDouble() * 0.02) * scale);
                    root.putDouble(TAG_SPEED, round3(root.getDouble(TAG_SPEED) + add));
                    gainNotes.add("§a+" + fmt(add * 100.0) + "% Velocidad");
                }
                default -> {
                    double add = round2((0.5 + random.nextDouble() * 0.7) * scale);
                    root.putDouble(TAG_LUCK, round2(root.getDouble(TAG_LUCK) + add));
                    gainNotes.add("§a+" + fmt(add) + " Suerte");
                }
            }
        }

        // Roll de efecto especial (65% en F..C, 100% en B..Génesis)
        float effChance = (nextRank.ordinal() >= ThreatLevel.B.ordinal()) ? 1.0f : 0.65f;
        if (random.nextFloat() < effChance) {
            CompoundTag effTag = root.contains(TAG_EFFECTS, Tag.TAG_COMPOUND)
                    ? root.getCompound(TAG_EFFECTS).copy()
                    : new CompoundTag();

            // Construir candidatos que aún no estén en Tier 3
            List<EffectType> candidates = new ArrayList<>();
            boolean isArmor = stack.getItem() instanceof ArmorItem;
            for (EffectType type : EffectType.values()) {
                // Armaduras priorizan efectos pasivos; armas pueden llevar pasivos y de impacto
                if (isArmor && !type.isPassive()) continue;
                int curTier = effTag.contains(type.getId()) ? effTag.getInt(type.getId()) : 0;
                if (curTier < 3) {
                    candidates.add(type);
                }
            }

            if (!candidates.isEmpty()) {
                EffectType chosen = candidates.get(random.nextInt(candidates.size()));
                int nextTier = Math.min(3, (effTag.contains(chosen.getId()) ? effTag.getInt(chosen.getId()) : 0) + 1);
                effTag.putInt(chosen.getId(), nextTier);
                root.put(TAG_EFFECTS, effTag);
                gainNotes.add(chosen.getColor() + chosen.getDisplayName() + " " + roman(nextTier));
            }
        }

        fullTag.put(TAG_ROOT, root);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(fullTag));

        return nextRank.getShortBadge().getString() + " §8| " + String.join(" §7• ", gainNotes);
    }

    public static String roman(int tier) {
        return switch (tier) {
            case 2 -> "II";
            case 3 -> "III";
            default -> "I";
        };
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
