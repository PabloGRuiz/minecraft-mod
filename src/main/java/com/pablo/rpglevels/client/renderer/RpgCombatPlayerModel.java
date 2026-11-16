package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.entities.ai.RpgCombatDuelist;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Dynamic combat model extending PlayerModel to render realistic, fluid
 * sword slashes, directional combos, and parry poses for NPCs.
 */
public class RpgCombatPlayerModel<T extends LivingEntity & RpgCombatDuelist> extends PlayerModel<T> {

    public RpgCombatPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isUsingItem() && entity.isHolding(item -> item.getItem() instanceof net.minecraft.world.item.BowItem)) {
            this.rightArmPose = ArmPose.BOW_AND_ARROW;
        } else if (entity.isHolding(item -> item.getItem() instanceof net.minecraft.world.item.BowItem)) {
            this.rightArmPose = ArmPose.ITEM;
        } else {
            this.rightArmPose = ArmPose.EMPTY;
        }

        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Boss Special Poses (Eating / Stunned Guard Break)
        if (entity instanceof com.pablo.rpglevels.entities.WindMasterBossEntity windMaster) {
            if (windMaster.isEating()) {
                this.rightArm.xRot = -1.35F;
                this.rightArm.yRot = -0.30F;
                this.rightArm.zRot = -0.15F;
                this.head.xRot = 0.35F;
                return;
            }
        }
        if (entity instanceof com.pablo.rpglevels.entities.UndeadAdventurerBossEntity boss) {
            if (boss.isStunned()) {
                this.head.xRot = 0.55F;
                this.body.xRot = 0.22F;
                this.rightArm.xRot = 0.15F;
                this.rightArm.zRot = 0.20F;
                this.leftArm.xRot = 0.15F;
                this.leftArm.zRot = -0.20F;
                return;
            }
            if (boss.isEating()) {
                this.leftArm.xRot = -1.35F;
                this.leftArm.yRot = 0.40F;
                this.leftArm.zRot = 0.15F;
                this.head.xRot = 0.35F;
                return;
            }
        }

        // 1. Parry / Defensive Blade Posture
        if (entity.isBlockingOrParrying()) {
            this.rightArm.xRot = -0.95F;
            this.rightArm.yRot = -0.55F;
            this.rightArm.zRot = 0.35F;
            this.leftArm.xRot = -0.85F;
            this.leftArm.yRot = 0.45F;
            return;
        }

        // 2. Combo-Specific Slash Animations
        if (this.attackTime > 0.0F) {
            int combo = entity.getCombatCombo();
            float swingProgress = this.attackTime;
            float swingArc = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

            switch (combo) {
                case 1 -> {
                    // Combo 1: Wide Right-to-Left Horizontal Slash
                    this.rightArm.xRot = -0.65F - swingArc * 0.3F;
                    this.rightArm.yRot = -0.85F + swingArc * 1.7F;
                    this.rightArm.zRot = 0.2F;
                    this.body.yRot = -0.35F + swingArc * 0.7F;
                }
                case 2 -> {
                    // Combo 2: Fast Left-to-Right Reverse Slash
                    this.rightArm.xRot = -0.5F - swingArc * 0.4F;
                    this.rightArm.yRot = 0.85F - swingArc * 1.7F;
                    this.rightArm.zRot = -0.2F;
                    this.body.yRot = 0.35F - swingArc * 0.7F;
                }
                case 3 -> {
                    // Combo 3: Heavy Overhead Chop / Forward Thrust
                    this.rightArm.xRot = -1.6F + swingArc * 1.5F;
                    this.rightArm.yRot = 0.0F;
                    this.rightArm.zRot = 0.0F;
                    this.body.xRot = 0.18F * swingArc;
                }
            }
        }
    }
}
