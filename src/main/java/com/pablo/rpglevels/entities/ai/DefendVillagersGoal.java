package com.pablo.rpglevels.entities.ai;

import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes Adventurers defend nearby Villagers and Townswomen from attacking Monsters & Bandits.
 */
public class DefendVillagersGoal extends TargetGoal {

    private final AdventurerNpcEntity adventurer;
    private LivingEntity attacker;

    public DefendVillagersGoal(AdventurerNpcEntity adventurer) {
        super(adventurer, false, true);
        this.adventurer = adventurer;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.adventurer.isHired() && !this.adventurer.isStaying()) {
            return false; // Hired following adventurers focus on defending their hiring player
        }

        // Stagger scan to once per second (20 ticks) per adventurer to prevent per-tick AABB entity scans in villages
        if ((this.adventurer.tickCount + this.adventurer.getId()) % 20 != 0) {
            return false;
        }

        List<LivingEntity> nearbyCivilians = this.adventurer.level().getEntitiesOfClass(
                LivingEntity.class,
                this.adventurer.getBoundingBox().inflate(24.0, 8.0, 24.0),
                e -> e instanceof AbstractVillager || e instanceof TownswomanNpcEntity
        );

        for (LivingEntity civilian : nearbyCivilians) {
            LivingEntity lastHurtBy = civilian.getLastHurtByMob();
            if (lastHurtBy instanceof Monster monster && monster.isAlive() && this.canAttack(monster, TargetingConditions.DEFAULT)) {
                this.attacker = monster;
                return true;
            }
        }

        return false;
    }

    @Override
    public void start() {
        this.adventurer.setTarget(this.attacker);
        super.start();
    }
}
