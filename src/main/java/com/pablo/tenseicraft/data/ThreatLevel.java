package com.pablo.tenseicraft.data;

import com.pablo.tenseicraft.mobs.BossScalingHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

/**
 * Scale of Threat Levels (Niveles de Amenaza) inspired by Tensura.
 * Used for all hostile entities and criminal / negative reputation players.
 */
public enum ThreatLevel {
    F("F", "Rango F", ChatFormatting.GRAY, false, 0.0, 0.0),
    E("E", "Rango E", ChatFormatting.GREEN, false, 25.0, 5.0),
    D("D", "Rango D", ChatFormatting.AQUA, false, 36.0, 7.0),
    C("C", "Rango C", ChatFormatting.BLUE, false, 55.0, 10.0),
    B("B", "Rango B", ChatFormatting.DARK_PURPLE, false, 85.0, 14.0),
    A("A", "Rango A", ChatFormatting.LIGHT_PURPLE, false, 125.0, 18.0),
    HAZARD("Hazard", "Hazard / Alerta", ChatFormatting.GOLD, true, 160.0, 22.0),
    CALAMITY("Calamity", "Calamity / Calamidad", ChatFormatting.RED, true, 251.0, 31.0),
    DISASTER("Disaster", "Disaster / Desastre", ChatFormatting.DARK_RED, true, 451.0, 46.0),
    GRAND_DISASTER("Grand Disaster", "Grand Disaster / Gran Desastre", ChatFormatting.LIGHT_PURPLE, true, 801.0, 66.0),
    CATASTROPHE("Catastrophe", "Catastrophe / Catástrofe", ChatFormatting.DARK_AQUA, true, 1501.0, 101.0),
    GENESIS("Genesis", "Genesis / Génesis", ChatFormatting.YELLOW, true, 3001.0, 151.0);

    private final String id;
    private final String displayName;
    private final ChatFormatting color;
    private final boolean bold;
    private final double minHp;
    private final double minDamage;

    ThreatLevel(String id, String displayName, ChatFormatting color, boolean bold, double minHp, double minDamage) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.bold = bold;
        this.minHp = minHp;
        this.minDamage = minDamage;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public boolean isBold() {
        return bold;
    }

    public double getMinHp() {
        return minHp;
    }

    public double getMinDamage() {
        return minDamage;
    }

    public MutableComponent getBadge() {
        MutableComponent comp = Component.literal("[Amenaza: " + displayName + "]").withStyle(color);
        if (bold) {
            comp = comp.withStyle(ChatFormatting.BOLD);
        }
        return comp;
    }

    public MutableComponent getShortBadge() {
        MutableComponent comp = Component.literal("[" + id + "]").withStyle(color);
        if (bold) {
            comp = comp.withStyle(ChatFormatting.BOLD);
        }
        return comp;
    }

    /**
     * Determines threat level for any hostile entity based on its base stats and lore status.
     */
    public static ThreatLevel determineThreatLevel(LivingEntity entity) {
        if (entity == null) return F;

        // Irregular mobs scale between B and A, with HAZARD as absolute maximum
        if (com.pablo.tenseicraft.mobs.IrregularMobHandler.isIrregular(entity)) {
            double irrHp = entity.getMaxHealth();
            double irrDmg = 0.0;
            AttributeInstance dmgAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) irrDmg = dmgAttr.getValue();

            if (irrHp >= HAZARD.minHp || irrDmg >= HAZARD.minDamage) return HAZARD;
            if (irrHp >= A.minHp || irrDmg >= A.minDamage) return A;
            return B;
        }

        // Special Lore & Cataclysm Bosses
        ResourceLocation typeLoc = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeLoc != null && "cataclysm".equals(typeLoc.getNamespace())) {
            String path = typeLoc.getPath();
            if (BossScalingHandler.isCataclysmBossOrMiniboss(path)) {
                return BossScalingHandler.getCataclysmThreat(path);
            }
        }

        // Vanilla & World Bosses (escalados con poder colosal)
        if (entity instanceof EnderDragon) return GRAND_DISASTER;
        if (entity instanceof Warden) return GRAND_DISASTER;
        if (entity instanceof WitherBoss) return DISASTER;
        if (entity instanceof net.minecraft.world.entity.monster.ElderGuardian) return CALAMITY;
        // 1. Jefes de progresión de nuestro mod con Nivel de Amenaza fijo:
        // - 1er Jefe: Kaelen (Maestra del Viento) -> Rango E (1ª forma) / Rango D (2ª forma)
        // - 2do Jefe: Aventurero No-Muerto -> Rango B
        // - 3er Jefe: Líder Cultista del Vacío -> Rango Hazard
        if (entity instanceof com.pablo.tenseicraft.entities.WindMasterBossEntity boss) {
            return boss.isEmpowered() ? D : E;
        }
        if (entity instanceof com.pablo.tenseicraft.entities.UndeadAdventurerBossEntity) {
            return B;
        }
        if (entity instanceof com.pablo.tenseicraft.entities.CultistBossEntity) {
            return HAZARD;
        }
        if (entity instanceof com.pablo.tenseicraft.entities.ReimuBossEntity) {
            return CALAMITY;
        }
        if (entity instanceof com.pablo.tenseicraft.entities.ColossalVoidbornEntity colossal) {
            return colossal.isElite() ? DISASTER : CALAMITY;
        }
        if (entity instanceof com.pablo.tenseicraft.entities.VoidbornExecutorEntity) {
            return CALAMITY;
        }

        // Specific Voidborn variants (Rank B, Rank A, or HAZARD Executor)
        if (entity instanceof com.pablo.tenseicraft.entities.VoidbornEntity voidborn) {
            if (voidborn.isExecutor()) {
                return HAZARD;
            }
            if (voidborn.isRankA()) {
                return A;
            }
            return B;
        }

        // Specific Rank D monsters explicitly designated by lore
        if (entity instanceof net.minecraft.world.entity.monster.hoglin.Hoglin) return D;
        if (entity instanceof net.minecraft.world.entity.monster.Witch) return D;
        if (entity instanceof net.minecraft.world.entity.monster.Drowned drowned) {
            if (drowned.getMainHandItem().is(net.minecraft.world.item.Items.TRIDENT)
                    || drowned.getOffhandItem().is(net.minecraft.world.item.Items.TRIDENT)) {
                return D;
            }
        }

        double hp = entity.getMaxHealth();
        AttributeInstance maxHpAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            hp = Math.max(hp, maxHpAttr.getValue());
        }

        double damage = 0.0;
        AttributeInstance dmgAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            damage = dmgAttr.getValue();
        }

        // Genesis: World primeval entities breaking standard scale
        if (hp >= GENESIS.minHp || damage >= GENESIS.minDamage) return GENESIS;
        if (hp >= CATASTROPHE.minHp || damage >= CATASTROPHE.minDamage) return CATASTROPHE;
        if (hp >= GRAND_DISASTER.minHp || damage >= GRAND_DISASTER.minDamage) return GRAND_DISASTER;
        if (hp >= DISASTER.minHp || damage >= DISASTER.minDamage) return DISASTER;
        if (hp >= CALAMITY.minHp || damage >= CALAMITY.minDamage) return CALAMITY;
        if (hp >= HAZARD.minHp || damage >= HAZARD.minDamage) return HAZARD;
        if (hp >= A.minHp || damage >= A.minDamage) return A;
        if (hp >= B.minHp || damage >= B.minDamage) return B;
        if (hp >= C.minHp || damage >= C.minDamage) return C;
        if (hp >= D.minHp || damage >= D.minDamage) return D;
        if (hp >= E.minHp || damage >= E.minDamage) return E;
        return F;
    }

    public int getRankIndex() {
        return this.ordinal();
    }

    public boolean isAtLeast(ThreatLevel other) {
        return this.ordinal() >= other.ordinal();
    }

    public static boolean isRankDOrHigher(LivingEntity entity) {
        if (entity == null) return false;
        ThreatLevel level = determineThreatLevel(entity);
        return level.ordinal() >= D.ordinal();
    }

    /**
     * Calculates threat level for a player with negative reputation (outlaws / villains)
     * based on their RPG level, stat power and negative reputation depth.
     */
    public static ThreatLevel getThreatForPlayer(Player player, PlayerRpgData data) {
        if (player == null || data == null) return F;

        int rep = data.getReputationScore();
        if (rep >= 0) return F; // Only negative reputation players get threat levels

        int level = player.experienceLevel;
        int totalStats = data.getTotalStatPoints(player, StatType.FORTITUDE)
                + data.getTotalStatPoints(player, StatType.DEXTERITY)
                + data.getTotalStatPoints(player, StatType.SPIRIT)
                + data.getTotalStatPoints(player, StatType.LUCK);

        // Infamy depth: rep ranges from -1 to -100
        int infamy = Math.abs(rep); // 1 to 100

        // High end threats
        if (level >= 90 && infamy >= 80 && totalStats >= 160) {
            return CATASTROPHE;
        }
        if (level >= 75 && infamy >= 65 && totalStats >= 120) {
            return GRAND_DISASTER;
        }
        if (level >= 60 && infamy >= 50 && totalStats >= 90) {
            return DISASTER;
        }
        if (level >= 45 && infamy >= 35 && totalStats >= 60) {
            return CALAMITY;
        }
        if (level >= 30 && infamy >= 25 && totalStats >= 40) {
            return HAZARD;
        }
        if (level >= 25 || totalStats >= 32) {
            return A;
        }
        if (level >= 20 || totalStats >= 25) {
            return B;
        }
        if (level >= 12 || totalStats >= 15) {
            return C;
        }
        if (level >= 6 || totalStats >= 8) {
            return D;
        }
        if (level >= 2) {
            return E;
        }
        return F;
    }
}

