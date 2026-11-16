package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.entities.ai.RpgCombatDuelist;
import com.pablo.rpglevels.entities.ai.RpgTacticalBowGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Boss: "Kaelen, Maestra del Viento"
 *
 * 3 Combat Phases:
 * Phase 1 (100% - 50% HP):
 * - Pure Tactical Archery: maintains 8-14 blocks distance, strafes and kites with bow.
 * - If player closes in (<= 3.6 blocks), equips Sword, strikes with Knockback ("Punch"),
 *   propelling the player backwards before resuming archery pressure!
 * - Energy Breather every 25s: 3s eating cooked beef (vulnerability window, heals 4 HP).
 *
 * Phase 2 (50% - 10% HP) - LAS TRES DANZAS DEL VIENTO:
 * Alternates through 3 distinct combat patterns, with a 3-second eating breather between each:
 * 1. Pattern 0: Orbital Dash & Backstab (circles player at high speed, teleports behind, backstabs).
 * 2. Pattern 1: Aerial Float & Arrow Volley (high leap, floats with smoke footsteps, fires 3 downward arrows).
 * 3. Pattern 2: Hit & Run (rapid frontal dash -> sword slash -> evasive backdash).
 *
 * Phase 3 (< 10% HP) - FRENESÍ / RAGE MODE:
 * - Roar, red boss bar, angry villager & flame particles.
 * - Relentless sword aggression, speed 0.36 - 0.38, frontal dashes every 2.2s, no breathers.
 *
 * Rematch / Empowered Mode:
 * - Triggered by hitting the friendly NPC.
 * - 300 HP, 12 Armor, 7 Attack Damage, Yellow Boss Bar, even greater challenge than the first boss!
 */
public class WindMasterBossEntity extends Monster implements RangedAttackMob, RpgCombatDuelist {

    private static final EntityDataAccessor<Integer> DATA_BOSS_PHASE =
            SynchedEntityData.defineId(WindMasterBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_EATING =
            SynchedEntityData.defineId(WindMasterBossEntity.class, EntityDataSerializers.BOOLEAN);

    // Boss health bar
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("§2§l[Jefe] §aKaelen, Maestra del Viento"),
            BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(false);

    // Weapons
    private ItemStack bowItem;
    private ItemStack swordItem;
    private ItemStack foodItem;

    // AI Goals
    private RpgTacticalBowGoal<WindMasterBossEntity> bowGoal;
    private MeleeAttackGoal meleeGoal;

    // Empowered / Rematch flag
    private boolean isEmpowered = false;

    // Phase 1 timer
    private int eatCooldown = 450; // 22.5s before first breather
    private int eatTicks = 0;

    // Phase 2 State Machine
    private int currentPatternIndex = 0; // 0 = Orbit, 1 = Aerial Volley, 2 = Hit & Run
    private int patternTicks = 0;
    private boolean isPhase2Breather = false;
    private int phase2BreatherTicks = 0;
    private boolean hasHitDuringDash = false;

    // Phase 3 timer
    private int rageDashCooldown = 0;
    private boolean defeated = false;

    private long lastAttackGameTime = 0L;

    public WindMasterBossEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 90.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 36.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BOSS_PHASE, 1);
        builder.define(DATA_IS_EATING, false);
    }

    public int getBossPhase() {
        return this.entityData.get(DATA_BOSS_PHASE);
    }

    public void setBossPhase(int phase) {
        this.entityData.set(DATA_BOSS_PHASE, phase);
    }

    public boolean isEating() {
        return this.entityData.get(DATA_IS_EATING);
    }

    public void setEating(boolean eating) {
        this.entityData.set(DATA_IS_EATING, eating);
    }

    public boolean isHoldingBow() {
        return this.getMainHandItem().is(Items.BOW);
    }

    public boolean isEmpowered() {
        return this.isEmpowered;
    }

    public void setEmpowered(boolean empowered) {
        this.isEmpowered = empowered;
        if (empowered) {
            this.xpReward = 150;
            var maxHp = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHp != null) {
                maxHp.setBaseValue(300.0);
                this.setHealth(300.0F);
            }
            var armor = this.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.setBaseValue(12.0);
            }
            var damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damage != null) {
                damage.setBaseValue(7.0);
            }
            var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.30);
            }
            this.bossEvent.setName(Component.literal("§6§l[Jefe Despertado] §eKaelen, Tempestad del Viento"));
            this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            this.setCustomName(Component.literal("§6§lKaelen, Tempestad del Viento"));
        }
    }

    @Override
    protected void registerGoals() {
        this.bowGoal = new RpgTacticalBowGoal<>(this, 1.05, 20, 16.0F);
        this.meleeGoal = new MeleeAttackGoal(this, 1.25, false) {
            @Override
            public boolean canUse() {
                // ONLY engage melee navigation when holding sword and not eating
                return !WindMasterBossEntity.this.isHoldingBow() && !WindMasterBossEntity.this.isEating() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !WindMasterBossEntity.this.isHoldingBow() && !WindMasterBossEntity.this.isEating() && super.canContinueToUse();
            }
        };

        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, this.bowGoal);   // Archery has priority when holding bow!
        this.goalSelector.addGoal(3, this.meleeGoal); // Melee acts when sword is equipped
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        this.bowItem = new ItemStack(Items.BOW);
        this.swordItem = new ItemStack(Items.IRON_SWORD);
        this.foodItem = new ItemStack(Items.COOKED_BEEF);

        // Start with Bow equipped
        this.setItemSlot(EquipmentSlot.MAINHAND, this.bowItem);

        if (!this.hasCustomName()) {
            this.setCustomName(Component.literal("§2§lKaelen, Maestra del Viento"));
            this.setCustomNameVisible(true);
        }
        this.setPersistenceRequired();

        return data;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            // Client particles in Phase 3 (Rage)
            if (getBossPhase() == 3) {
                if (this.random.nextFloat() < 0.7F) {
                    this.level().addParticle(
                            ParticleTypes.ANGRY_VILLAGER,
                            this.getRandomX(0.5), this.getY() + 1.2 + (this.random.nextDouble() * 0.4), this.getRandomZ(0.5),
                            0.0, 0.05, 0.0
                    );
                }
                if (this.random.nextFloat() < 0.4F) {
                    this.level().addParticle(
                            ParticleTypes.FLAME,
                            this.getRandomX(0.4), this.getY() + 0.8, this.getRandomZ(0.4),
                            (this.random.nextDouble() - 0.5) * 0.05, 0.02, (this.random.nextDouble() - 0.5) * 0.05
                    );
                }
            }
            return;
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        LivingEntity target = this.getTarget();
        float healthPct = this.getHealth() / this.getMaxHealth();

        // Phase Transitions
        if (healthPct <= 0.10F && getBossPhase() < 3 && !defeated) {
            triggerPhase3Rage();
        } else if (healthPct <= 0.50F && getBossPhase() < 2 && !defeated) {
            triggerPhase2Intermediate();
        }

        // Phase Tick Handlers
        if (getBossPhase() == 1) {
            handlePhase1Tick(target);
        } else if (getBossPhase() == 2) {
            handlePhase2Tick(target);
        } else {
            handlePhase3Tick(target);
        }
    }

    // --- PHASE 1: Tactical Archery, Close Melee Knockback & 25s Breather ---

    private void handlePhase1Tick(@Nullable LivingEntity target) {
        // Breather eating
        if (isEating()) {
            eatTicks--;
            this.getNavigation().stop();

            if (eatTicks % 6 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 0.9F, 1.0F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COOKED_BEEF)),
                            this.getX(), this.getEyeY() - 0.1, this.getZ(), 4, 0.15, 0.1, 0.1, 0.05);
                }
            }

            if (eatTicks <= 0) {
                this.heal(isEmpowered ? 8.0F : 4.0F);
                setEating(false);
                this.eatCooldown = 500;
                if (this.bowItem == null) this.bowItem = new ItemStack(Items.BOW);
                this.setItemSlot(EquipmentSlot.MAINHAND, this.bowItem);

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 0.8F, 1.1F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
                }
            }
            return;
        }

        // Breather timer
        if (eatCooldown > 0) {
            eatCooldown--;
        } else if (target != null && this.distanceToSqr(target) >= 6.0 * 6.0 && this.getHealth() < this.getMaxHealth()) {
            setEating(true);
            this.eatTicks = 60; // 3 seconds window
            if (this.foodItem == null) this.foodItem = new ItemStack(Items.COOKED_BEEF);
            this.setItemSlot(EquipmentSlot.MAINHAND, this.foodItem);
            return;
        }

        // Dynamic weapon swap: Bow at range, Sword in close-quarters
        if (target != null) {
            double distSq = this.distanceToSqr(target);

            if (distSq <= 3.6 * 3.6) {
                // Cornered in close range: swap to sword to knock player away!
                if (!this.getMainHandItem().is(Items.IRON_SWORD)) {
                    if (this.swordItem == null) this.swordItem = new ItemStack(Items.IRON_SWORD);
                    this.setItemSlot(EquipmentSlot.MAINHAND, this.swordItem);
                }
            } else if (distSq >= 5.5 * 5.5) {
                // Distance achieved: switch back to bow and let bowGoal shoot!
                if (!this.getMainHandItem().is(Items.BOW)) {
                    if (this.bowItem == null) this.bowItem = new ItemStack(Items.BOW);
                    this.setItemSlot(EquipmentSlot.MAINHAND, this.bowItem);
                }
            }
        }
    }

    // --- PHASE 2: Las Tres Danzas del Viento (3 Patterns + 3s Breathers) ---

    private void triggerPhase2Intermediate() {
        setBossPhase(2);
        setEating(false);
        this.currentPatternIndex = 0;
        this.patternTicks = 0;
        this.isPhase2Breather = false;

        this.bossEvent.setColor(isEmpowered ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.BLUE);
        String title = isEmpowered ? "§6§l[TRES DANZAS] §eKaelen, Tempestad Despierta" : "§9§l[TRES DANZAS] §bKaelen, Danza del Viento";
        this.bossEvent.setName(Component.literal(title));

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 1.0F, 1.5F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 15, 0.3, 0.1, 0.3, 0.05);
        }

        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0))) {
            player.sendSystemMessage(Component.literal("§e§l[FASE 2] §6Kaelen ha adoptado las Tres Danzas del Viento. ¡Atento a sus patrones y aprovecha sus descansos!").withStyle(ChatFormatting.GOLD));
        }
    }

    private void handlePhase2Tick(@Nullable LivingEntity target) {
        if (target == null) return;

        // 1. Breather between patterns
        if (isPhase2Breather) {
            handlePhase2BreatherTick();
            return;
        }

        // 2. Execute current pattern
        switch (currentPatternIndex) {
            case 0 -> handlePatternOrbitBackstab(target);
            case 1 -> handlePatternAerialVolley(target);
            case 2 -> handlePatternHitAndRun(target);
        }
    }

    private boolean isWithinMeleeReach(LivingEntity target) {
        if (target == null) return false;
        double reach = (double) (this.getBbWidth() * 2.0F) + target.getBbWidth() + 0.6D;
        return this.distanceToSqr(target) <= (reach * reach) || this.getBoundingBox().inflate(0.6D).intersects(target.getBoundingBox());
    }

    private void handlePatternOrbitBackstab(LivingEntity target) {
        patternTicks++;
        this.getLookControl().setLookAt(target, 40.0F, 40.0F);

        if (patternTicks <= 35) {
            // Rapid orbital dash around target
            double angle = (patternTicks / 35.0) * Math.PI * 2.0;
            double radius = 4.5;
            double targetX = target.getX() + Math.cos(angle) * radius;
            double targetZ = target.getZ() + Math.sin(angle) * radius;
            Vec3 dashDir = new Vec3(targetX - this.getX(), 0.05, targetZ - this.getZ()).normalize().scale(0.9);
            this.setDeltaMovement(dashDir);
            this.hurtMarked = true;

            if (patternTicks % 4 == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 1.4F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 0.2, this.getZ(), 4, 0.2, 0.1, 0.2, 0.05);
                }
            }
        } else if (patternTicks == 36) {
            // Teleport behind target's back
            Vec3 look = target.getLookAngle();
            Vec3 behind = target.position().subtract(look.x * 2.0, 0, look.z * 2.0);
            this.teleportTo(behind.x, target.getY(), behind.z);
            this.setYRot(target.getYRot());

            if (this.swordItem == null) this.swordItem = new ItemStack(Items.IRON_SWORD);
            this.setItemSlot(EquipmentSlot.MAINHAND, this.swordItem);

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.9F, 1.2F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
            }
        } else if (patternTicks <= 50) {
            // Strike with backstab bonus (strictly check melee reach so dodging with Dash works!)
            if (patternTicks == 40) {
                this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                if (this.isWithinMeleeReach(target)) {
                    this.doHurtTarget(target);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.3, 0.3, 0.3, 0.15);
                    }
                } else {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE, 1.0F, 1.2F);
                }
            }
            if (patternTicks == 50) {
                startPhase2Breather();
            }
        }
    }

    private void handlePatternAerialVolley(LivingEntity target) {
        patternTicks++;

        if (patternTicks == 1) {
            // Explosive leap high into the air
            if (this.bowItem == null) this.bowItem = new ItemStack(Items.BOW);
            this.setItemSlot(EquipmentSlot.MAINHAND, this.bowItem);
            this.setDeltaMovement(0, 1.35, 0);
            this.hurtMarked = true;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.2F, 1.2F);
        } else if (patternTicks <= 12) {
            // Upward propulsion boost to reach ~6 blocks above target
            double targetFlyY = target.getY() + 5.5;
            if (this.getY() < targetFlyY) {
                Vec3 vel = this.getDeltaMovement();
                this.setDeltaMovement(vel.x * 0.7, Math.max(0.35, vel.y), vel.z * 0.7);
                this.hurtMarked = true;
            }
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() - 0.2, this.getZ(), 3, 0.2, 0.05, 0.2, 0.05);
            }
        } else if (patternTicks <= 60) {
            // Hover high in air with dense smoke/clouds simulating aerial flight
            double targetFlyY = target.getY() + 5.5;
            double yDiff = targetFlyY - this.getY();
            double yVel = Math.max(-0.04, Math.min(0.10, yDiff * 0.2));
            this.setDeltaMovement(this.getDeltaMovement().x * 0.4, yVel, this.getDeltaMovement().z * 0.4);
            this.hurtMarked = true;
            this.getLookControl().setLookAt(target, 45.0F, 45.0F);

            // Dense smoke & cloud footsteps holding her suspended
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() - 0.1, this.getZ(), 4, 0.25, 0.05, 0.25, 0.02);
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() - 0.15, this.getZ(), 3, 0.2, 0.05, 0.2, 0.03);
            }

            // Rapid-Fire Volley: fires rapid arrows every 7 ticks! (ticks 18, 25, 32, 39, 46, 53)
            if (patternTicks == 18 || patternTicks == 25 || patternTicks == 32 || patternTicks == 39 || patternTicks == 46 || patternTicks == 53) {
                this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                performRangedAttack(target, 1.25F);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 0.9F, 1.8F);
            }
        } else if (patternTicks <= 68) {
            // Controlled glide descent
            this.setDeltaMovement(this.getDeltaMovement().x * 0.6, -0.25, this.getDeltaMovement().z * 0.6);
            this.hurtMarked = true;
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() - 0.1, this.getZ(), 2, 0.2, 0.05, 0.2, 0.02);
            }
        } else if (patternTicks >= 70) {
            startPhase2Breather();
        }
    }

    private void handlePatternHitAndRun(LivingEntity target) {
        patternTicks++;

        if (patternTicks == 1) {
            // Frontal dash initialization
            this.hasHitDuringDash = false;
            if (this.swordItem == null) this.swordItem = new ItemStack(Items.IRON_SWORD);
            this.setItemSlot(EquipmentSlot.MAINHAND, this.swordItem);

            Vec3 toTarget = target.position().subtract(this.position()).normalize().scale(1.30);
            this.setDeltaMovement(toTarget.x, 0.18, toTarget.z);
            this.hurtMarked = true;

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.0F, 1.3F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
            }
        } else if (patternTicks <= 14) {
            // While dashing forward: ONLY connect if she actually enters melee reach!
            this.getLookControl().setLookAt(target, 35.0F, 35.0F);
            if (!this.hasHitDuringDash && this.isWithinMeleeReach(target)) {
                this.hasHitDuringDash = true;
                this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.doHurtTarget(target);
            }
        } else if (patternTicks == 15) {
            // End of dash forward: if within reach hit, else whiff into empty air!
            if (!this.hasHitDuringDash) {
                this.getLookControl().setLookAt(target, 35.0F, 35.0F);
                this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                if (this.isWithinMeleeReach(target)) {
                    this.hasHitDuringDash = true;
                    this.doHurtTarget(target);
                } else {
                    // Dodged! Sound of sword whistling through empty air
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.HOSTILE, 1.0F, 1.2F);
                }
            }
        } else if (patternTicks == 20) {
            // Evasive backdash: jump backward away from player!
            Vec3 away = this.position().subtract(target.position()).normalize().scale(1.2);
            this.setDeltaMovement(away.x, 0.26, away.z);
            this.hurtMarked = true;

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.0F, 1.6F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 10, 0.3, 0.1, 0.3, 0.05);
            }
        } else if (patternTicks == 35) {
            startPhase2Breather();
        }
    }

    private void startPhase2Breather() {
        this.isPhase2Breather = true;
        this.phase2BreatherTicks = 60; // 3 seconds breather
        setEating(true);
        if (this.foodItem == null) this.foodItem = new ItemStack(Items.COOKED_BEEF);
        this.setItemSlot(EquipmentSlot.MAINHAND, this.foodItem);
        this.getNavigation().stop();
    }

    private void handlePhase2BreatherTick() {
        phase2BreatherTicks--;
        this.getNavigation().stop();

        if (phase2BreatherTicks % 6 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 0.9F, 1.0F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COOKED_BEEF)),
                        this.getX(), this.getEyeY() - 0.1, this.getZ(), 4, 0.15, 0.1, 0.1, 0.05);
            }
        }

        if (phase2BreatherTicks <= 0) {
            this.isPhase2Breather = false;
            setEating(false);
            this.heal(isEmpowered ? 8.0F : 4.0F);

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 0.8F, 1.1F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            }

            // Advance to next pattern
            this.currentPatternIndex = (currentPatternIndex + 1) % 3;
            this.patternTicks = 0;
        }
    }

    // --- PHASE 3: Modo Rage / Frenesí Desesperado (< 10% HP) ---

    private void triggerPhase3Rage() {
        setBossPhase(3);
        setEating(false);
        this.isPhase2Breather = false;

        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        String title = isEmpowered ? "§4§l[FRENESÍ SUPREMO] §cKaelen, Ojo del Huracán" : "§4§l[FRENESÍ] §cKaelen, Furia Desesperada";
        this.bossEvent.setName(Component.literal(title));

        if (this.swordItem == null) this.swordItem = new ItemStack(Items.IRON_SWORD);
        this.setItemSlot(EquipmentSlot.MAINHAND, this.swordItem);

        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(isEmpowered ? 0.38 : 0.36);
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 1.25F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 20, 0.4, 0.4, 0.4, 0.0);
        }

        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0))) {
            player.sendSystemMessage(Component.literal("§c§l[FRENESÍ] §eKaelen ha entrado en furia total. ¡Todo o nada!").withStyle(ChatFormatting.RED));
        }
    }

    private void handlePhase3Tick(@Nullable LivingEntity target) {
        if (rageDashCooldown > 0) {
            rageDashCooldown--;
        }

        // Relentless dashes towards player every 2.2 seconds
        if (target != null && rageDashCooldown <= 0 && this.distanceToSqr(target) > 3.0 * 3.0) {
            Vec3 dir = target.position().subtract(this.position()).normalize().scale(1.3);
            this.setDeltaMovement(dir.x, 0.22, dir.z);
            this.hurtMarked = true;

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.0F, 1.2F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.1F);

            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
            }

            rageDashCooldown = 45;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity livingTarget) {
            if (getBossPhase() == 1) {
                // Phase 1 Sword: Heavy Knockback impulse to push player back into archery range
                if (!livingTarget.isBlocking()) {
                    Vec3 knock = livingTarget.position().subtract(this.position()).normalize().scale(1.2).add(0, 0.25, 0);
                    livingTarget.setDeltaMovement(knock);
                    livingTarget.hurtMarked = true;
                    this.level().playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                            SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.0F, 1.1F);
                }
            } else {
                // Phase 2 / Phase 3 hits: angry particles & crit
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, livingTarget.getX(), livingTarget.getY() + 1.0, livingTarget.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
                    sl.sendParticles(ParticleTypes.CRIT, livingTarget.getX(), livingTarget.getY() + 1.0, livingTarget.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }
        return hurt;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (isEating()) return;

        ItemStack bow = this.getMainHandItem();
        if (!(bow.getItem() instanceof BowItem)) return;

        ItemStack arrowStack = new ItemStack(Items.ARROW);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, distanceFactor, bow);

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + dist * 0.18D, dz, 1.9F, 1.5F);

        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    public void die(DamageSource damageSource) {
        if (!this.level().isClientSide() && !defeated) {
            defeated = true;
            triggerDefeatTransformation();
            return;
        }
        super.die(damageSource);
    }

    private void triggerDefeatTransformation() {
        this.bossEvent.removeAllPlayers();

        // 1. Visual Lightning Bolt
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
        if (lightning != null) {
            lightning.moveTo(this.getX(), this.getY(), this.getZ());
            lightning.setVisualOnly(true);
            this.level().addFreshEntity(lightning);
        }

        // 2. Audio & Particles
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.0F, 1.1F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.9F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 1.0, this.getZ(), 25, 0.4, 0.5, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2, this.getZ(), 40, 0.5, 0.6, 0.5, 0.2);
        }

        // 3. Drop loot if empowered rematch
        if (isEmpowered) {
            this.spawnAtLocation(new ItemStack(Items.EMERALD, 8));
            this.spawnAtLocation(new ItemStack(Items.DIAMOND, 2));
            this.spawnAtLocation(new ItemStack(Items.GOLDEN_APPLE, 3));
        }

        // 4. Spawn Friendly NPC
        WindMasterNpcEntity npc = ModEntities.WIND_MASTER_NPC.get().create(this.level());
        if (npc != null) {
            npc.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            npc.setCustomName(Component.literal("§a[Maestra del Dash] §fKaelen"));
            npc.setCustomNameVisible(true);
            npc.setPersistenceRequired();
            this.level().addFreshEntity(npc);
        }

        // 5. Broadcast message & register victory
        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0))) {
            player.getPersistentData().putBoolean("DefeatedKaelenDuel", true);
            com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.DEFEAT_WIND_MASTER);
            if (isEmpowered) {
                player.sendSystemMessage(Component.literal("§6§l[DESAFÍO COMPLETADO] §e¡Has superado la tempestad despierta de Kaelen!").withStyle(ChatFormatting.GOLD));
            } else {
                player.sendSystemMessage(Component.literal("§6§l[DUELO FINALIZADO] §eHas derrotado a §cKaelen§e. El relámpago ha disipado su frenesí.").withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.literal("§a¡Habla con ella para que te enseñe el §b§lDash Nivel 1§a!").withStyle(ChatFormatting.GREEN));
            }
        }

        this.discard();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // --- RpgCombatDuelist Interface ---

    @Override
    public int getCombatCombo() {
        return 0;
    }

    @Override
    public void setCombatCombo(int combo) {
    }

    @Override
    public boolean isBlockingOrParrying() {
        return false;
    }

    @Override
    public void setBlockingOrParrying(boolean parrying) {
    }

    @Override
    public long getLastAttackGameTime() {
        return lastAttackGameTime;
    }

    @Override
    public void setLastAttackGameTime(long time) {
        this.lastAttackGameTime = time;
    }

    @Override
    public PathfinderMob getDuelistMob() {
        return this;
    }

    @Override
    public boolean isCombatSuspended() {
        return isEating() || isPhase2Breather || defeated;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("BossPhase", getBossPhase());
        compound.putBoolean("IsEating", isEating());
        compound.putInt("EatCooldown", eatCooldown);
        compound.putBoolean("IsEmpowered", isEmpowered);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("BossPhase")) {
            setBossPhase(compound.getInt("BossPhase"));
        }
        if (compound.contains("IsEating")) {
            setEating(compound.getBoolean("IsEating"));
        }
        if (compound.contains("EatCooldown")) {
            this.eatCooldown = compound.getInt("EatCooldown");
        }
        if (compound.contains("IsEmpowered")) {
            setEmpowered(compound.getBoolean("IsEmpowered"));
        }
    }
}
