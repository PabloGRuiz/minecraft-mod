package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.mobs.IrregularMobHandler;
import com.pablo.tenseicraft.mobs.MobRankHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class MobRankEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFinalizeSpawnScaleMobs(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.CHUNK_GENERATION
                || event.getSpawnType() == MobSpawnType.STRUCTURE) {
            return;
        }

        Mob mob = event.getEntity();
        if (mob == null || mob.level().isClientSide()) {
            return;
        }
        if (!(mob instanceof Enemy || mob instanceof Monster)) {
            return;
        }
        if (MobRankHandler.isBossOrBlacklisted(mob) || IrregularMobHandler.isIrregular(mob)) {
            return;
        }
        if (mob.getPersistentData().getBoolean("RpgMobScaled")) {
            return;
        }

        double tier = IrregularMobHandler.computeWorldEffectiveTier(mob.level(), mob.getX(), mob.getY(), mob.getZ());
        if (tier < 0.4) {
            return;
        }

        mob.getPersistentData().putBoolean("RpgMobScaled", true);

        AttributeInstance maxHp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            double newHp = Math.round(maxHp.getBaseValue() * (1.0 + tier * 0.40));
            maxHp.setBaseValue(newHp);
            mob.setHealth((float) newHp);
        }

        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getBaseValue() > 0.0) {
            dmg.setBaseValue(dmg.getBaseValue() * (1.0 + tier * 0.25));
        }

        AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(armor.getBaseValue() + Math.min(12.0, tier * 1.5));
        }

        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * (1.0 + Math.min(0.20, tier * 0.03)));
        }

        if (mob instanceof com.pablo.tenseicraft.entities.BanditEntity bandit) {
            bandit.updateMeasuredRankAndTitle();
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Monster || entity instanceof Enemy || com.pablo.tenseicraft.mobs.BossScalingHandler.isBoss(entity))) {
            return;
        }

        ThreatLevel threat = MobRankHandler.getThreatLevel(entity);
        float multiplier = MobRankHandler.getLootMultiplier(threat);

        // Luck Stat bonus: +3% drop loot multiplier per point
        Player killer = null;
        if (event.getSource() != null && event.getSource().getEntity() instanceof Player p) {
            killer = p;
        } else if (entity.getKillCredit() instanceof Player p) {
            killer = p;
        } else if (entity.getLastAttacker() instanceof Player p) {
            killer = p;
        }

        if (killer != null) {
            PlayerRpgData data = killer.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                int luck = data.getTotalStatPoints(killer, StatType.LUCK);
                if (luck > 0) {
                    multiplier += (luck * 0.03f);
                }
            }
        }

        if (multiplier <= 1.0f) {
            return;
        }

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            int originalCount = stack.getCount();
            float rawCount = originalCount * multiplier;
            int guaranteed = (int) rawCount;
            float remainder = rawCount - guaranteed;
            if (entity.getRandom().nextFloat() < remainder) {
                guaranteed++;
            }
            stack.setCount(Math.max(1, guaranteed));
            drop.setItem(stack);
        }
    }

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof Monster || entity instanceof Enemy || com.pablo.tenseicraft.mobs.BossScalingHandler.isBoss(entity))) {
            return;
        }

        int baseExp = event.getDroppedExperience();

        // Irregular mobs drop 4x experience upon death (balanceado)
        if (IrregularMobHandler.isIrregular(entity)) {
            event.setDroppedExperience(Math.min(250, Math.max(25, baseExp * 4)));
            return;
        }

        ThreatLevel threat = MobRankHandler.getThreatLevel(entity);
        float expMult = 1.0f + Math.min(1.5f, threat.ordinal() * 0.20f);
        if (expMult <= 1.0f) {
            return;
        }

        event.setDroppedExperience(Math.max(1, Math.round(baseExp * expMult)));
    }
}
