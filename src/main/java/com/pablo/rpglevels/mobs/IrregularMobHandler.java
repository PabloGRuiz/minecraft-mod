package com.pablo.rpglevels.mobs;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class IrregularMobHandler {

    public static final String TAG_IRREGULAR = "RpgIrregular";
    public static final double BASE_HEALTH = 90.0;
    public static final double BASE_DAMAGE = 14.0;
    public static final double SPEED_MULTIPLIER = 1.18;

    /**
     * Calcula el nivel de escalado del mundo/jugadores cercanos para que enemigos e Irregulares no se queden atrás.
     */
    public static double computeWorldEffectiveTier(Level level, double x, double y, double z) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0.0;
        }

        int bestRank = 0;
        int bestLevel = 0;
        boolean foundNearby = false;

        for (net.minecraft.server.level.ServerPlayer sp : serverLevel.players()) {
            if (sp.distanceToSqr(x, y, z) <= 128.0 * 128.0) {
                foundNearby = true;
                var data = sp.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
                int r = (data != null && data.isGuildRegistered()) ? data.getRank().getIndex() : 0;
                if (r > bestRank) bestRank = r;
                if (sp.experienceLevel > bestLevel) bestLevel = sp.experienceLevel;
            }
        }

        if (!foundNearby) {
            for (net.minecraft.server.level.ServerPlayer sp : serverLevel.getServer().getPlayerList().getPlayers()) {
                var data = sp.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
                int r = (data != null && data.isGuildRegistered()) ? data.getRank().getIndex() : 0;
                if (r > bestRank) bestRank = r;
                if (sp.experienceLevel > bestLevel) bestLevel = sp.experienceLevel;
            }
        }

        double dimBonus = 0.0;
        if (level.dimension() == Level.NETHER) dimBonus = 1.0;
        else if (level.dimension() == Level.END) dimBonus = 1.5;
        if (com.pablo.rpglevels.world.dimension.ModDimensions.isAbyssRift(level)) {
            dimBonus = 2.5;
        }

        return (bestRank * 1.15) + (bestLevel / 30.0) + dimBonus;
    }

    /**
     * Checks whether an entity is an Irregular variant.
     */
    public static boolean isIrregular(LivingEntity entity) {
        if (entity == null) return false;
        return entity.getPersistentData().getBoolean(TAG_IRREGULAR);
    }

    /**
     * Converts a mob into an Irregular variant that scales dynamically with world progression.
     * Works for ANY hostile mob, both Minecraft vanilla and modded.
     */
    public static void makeIrregular(Mob mob) {
        if (mob == null || isIrregular(mob) || mob instanceof com.pablo.rpglevels.entities.VoidbornEntity) {
            return;
        }

        mob.getPersistentData().putBoolean(TAG_IRREGULAR, true);

        double tier = computeWorldEffectiveTier(mob.level(), mob.getX(), mob.getY(), mob.getZ());
        // Oscila normalmente entre Rango B (85-115 HP) y Rango A (116-159 HP), alcanzando como máximo Hazard (160-185 HP)
        double scaledHp = Math.min(185.0, Math.round(BASE_HEALTH + (tier * 24.0)));
        double scaledDamage = Math.min(22.0, BASE_DAMAGE + (tier * 1.8));
        double scaledArmor = Math.min(10.0, 3.0 + (tier * 1.2));
        double scaledToughness = Math.min(3.5, tier * 0.7);
        double speedMult = 1.15 + Math.min(0.12, tier * 0.025);
        double kbRes = Math.min(0.50, 0.20 + (tier * 0.05));

        // 1. Health (dinámico según rango/progresión)
        AttributeInstance maxHp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(Math.max(maxHp.getBaseValue(), scaledHp));
            mob.setHealth(mob.getMaxHealth());
        }

        // 2. Damage (dinámico)
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.setBaseValue(Math.max(dmg.getBaseValue(), scaledDamage));
        }

        // 3. Armor & Toughness para que no caigan de pocos golpes
        AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(Math.max(armor.getBaseValue(), scaledArmor));
        }
        AttributeInstance toughness = mob.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughness != null) {
            toughness.setBaseValue(Math.max(toughness.getBaseValue(), scaledToughness));
        }

        // 4. Movement Speed
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * speedMult);
        }

        // 5. Knockback Resistance & Follow Range
        AttributeInstance kb = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.setBaseValue(Math.max(kbRes, kb.getBaseValue()));
        }

        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.setBaseValue(Math.max(40.0, follow.getBaseValue()));
        }

        // 6. Name formatting según nivel de amenaza escalado (B, A o Hazard como máximo)
        if (mob instanceof com.pablo.rpglevels.entities.BanditEntity bandit) {
            bandit.updateMeasuredRankAndTitle();
        } else {
            String originalName = mob.getType().getDescription().getString();
            com.pablo.rpglevels.data.ThreatLevel threat = com.pablo.rpglevels.data.ThreatLevel.determineThreatLevel(mob);
            String badgePrefix = switch (threat) {
                case HAZARD -> "§6[Hazard] §c";
                case A -> "§d[Rango A] §c";
                default -> "§5[Rango B] §c";
            };
            mob.setCustomName(Component.literal(badgePrefix + originalName + " Irregular"));
            mob.setCustomNameVisible(true);
        }
    }

    /**
     * Determines if a mob prefers ranged attacks (Bows, Crossbows, Potions).
     */
    public static boolean isRanged(Mob mob) {
        if (mob instanceof RangedAttackMob) {
            ItemStack hand = mob.getMainHandItem();
            if (hand.getItem() instanceof BowItem || hand.getItem() instanceof CrossbowItem) {
                return true;
            }
            if (mob instanceof AbstractSkeleton) {
                return !(hand.getItem() instanceof SwordItem);
            }
            if (mob instanceof Witch || mob instanceof Pillager) {
                return true;
            }
        }
        return false;
    }

    /**
     * Spawns Irregular variants naturally with a chance for eligible hostile monsters.
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION
                || event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.STRUCTURE) {
            return;
        }

        Mob mob = event.getEntity();
        if (mob == null || mob.level().isClientSide()) {
            return;
        }

        if (!(mob instanceof Enemy || mob instanceof Monster)) {
            return;
        }

        if (MobRankHandler.isBossOrBlacklisted(mob) || mob instanceof com.pablo.rpglevels.entities.VoidbornEntity) {
            return;
        }

        if (isIrregular(mob)) {
            return;
        }

        // 6% chance in Overworld, 12% in Nether / End, 45% in Abyss Rift (super common)
        float chance = 0.06f;
        if (mob.level().dimension() == Level.NETHER || mob.level().dimension() == Level.END) {
            chance = 0.12f;
        }
        if (com.pablo.rpglevels.world.dimension.ModDimensions.isAbyssRift(mob.level())) {
            chance = 0.45f;
        }

        if (mob.getRandom().nextFloat() < chance) {
            makeIrregular(mob);
        }
    }

    /**
     * Menacing particle aura for Irregular mobs.
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)) {
            return;
        }

        if (mob.level().isClientSide() || !mob.isAlive()) {
            return;
        }

        if (!isIrregular(mob)) {
            return;
        }

        long currentTick = mob.level().getGameTime();
        long staggerUntil = mob.getPersistentData().getLong("RpgStaggerUntilTick");
        if (staggerUntil > currentTick) {
            // Los Irregulares se recuperan 2x más rápido del aturdimiento
            mob.getPersistentData().putLong("RpgStaggerUntilTick", staggerUntil - 1);
        }

        // Regeneración tenaz en combate cada 2 segundos (si no está aturdido)
        if (mob.tickCount % 40 == 0 && mob.getTarget() != null && mob.getHealth() < mob.getMaxHealth() && staggerUntil <= currentTick) {
            float regen = Math.max(3.0f, mob.getMaxHealth() * 0.01f);
            mob.heal(regen);
        }

        // Emit menacing aura particles every 6 ticks only if a player is within 32 blocks
        if (mob.tickCount % 6 == 0 && mob.level() instanceof ServerLevel serverLevel) {
            if (!serverLevel.hasNearbyAlivePlayer(mob.getX(), mob.getY(), mob.getZ(), 32.0)) {
                return;
            }
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                    1, 0.25, 0.25, 0.25, 0.02);
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.4, mob.getZ(),
                    2, 0.2, 0.2, 0.2, 0.01);
        }
    }
}

