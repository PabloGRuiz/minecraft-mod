package com.pablo.rpglevels.entities.ai;

import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class CompanionDefendOwnerGoal extends TargetGoal {

    private final AdventurerNpcEntity npc;
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public CompanionDefendOwnerGoal(AdventurerNpcEntity npc) {
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

        this.ownerLastHurtBy = owner.getLastHurtByMob();
        int lastHurtTimestamp = owner.getLastHurtByMobTimestamp();
        return lastHurtTimestamp != this.timestamp && this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.ownerLastHurtBy);
        Player owner = npc.getHiringPlayer();
        if (owner != null) {
            this.timestamp = owner.getLastHurtByMobTimestamp();
        }
        super.start();
    }
}
