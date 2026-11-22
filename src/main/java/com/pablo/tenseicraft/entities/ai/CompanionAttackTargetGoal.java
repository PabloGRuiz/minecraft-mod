package com.pablo.tenseicraft.entities.ai;

import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class CompanionAttackTargetGoal extends TargetGoal {

    private final AdventurerNpcEntity npc;
    private LivingEntity ownerLastHurt;
    private int timestamp;

    public CompanionAttackTargetGoal(AdventurerNpcEntity npc) {
        super(npc, false);
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!npc.isHired()) {
            return false;
        }

        Player owner = npc.getHiringPlayer();
        if (owner == null) {
            return false;
        }

        this.ownerLastHurt = owner.getLastHurtMob();
        int lastHurtTimestamp = owner.getLastHurtMobTimestamp();
        return lastHurtTimestamp != this.timestamp && this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurt);
        Player owner = npc.getHiringPlayer();
        if (owner != null) {
            this.timestamp = owner.getLastHurtMobTimestamp();
        }
        super.start();
    }
}
