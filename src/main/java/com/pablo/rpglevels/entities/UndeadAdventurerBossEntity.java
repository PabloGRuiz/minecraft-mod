package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.entities.ai.RpgCombatDuelist;
import com.pablo.rpglevels.entities.ai.RpgDuelCombatGoal;
import com.pablo.rpglevels.entities.ai.RpgTacticalBowGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * First Major Boss: "Undead Adventurer"
 * Always Rank C, dynamically scales with nearby players at spawn.
 *
 * 3 Dynamic Combat Phases:
 * 1. Phase 1 (100% - 75% HP): Tactical Archer (high-ground leaps, kiting, evasive backdashes).
 * 2. Phase 2 (75% - 10% HP): Relentless Swordmaster with Regenerative Breather (eats Rotten Flesh to heal,
 *    giving players a window to drink potions/regroup or interrupt him!).
 * 3. Phase 3 (< 10% HP): Iron Bastion (Shield + Sword guard stance with Posture / Guard Break mechanic).
 */
public class UndeadAdventurerBossEntity extends Monster implements RpgCombatDuelist, RangedAttackMob {

    private static final EntityDataAccessor<Integer> DATA_COMBAT_COMBO =
            SynchedEntityData.defineId(UndeadAdventurerBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_PARRYING =
            SynchedEntityData.defineId(UndeadAdventurerBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOSS_PHASE =
            SynchedEntityData.defineId(UndeadAdventurerBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_EATING =
            SynchedEntityData.defineId(UndeadAdventurerBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_STUNNED =
            SynchedEntityData.defineId(UndeadAdventurerBossEntity.class, EntityDataSerializers.BOOLEAN);

    // Boss bar
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("§5§l[Jefe] ").append(ThreatLevel.B.getBadge()).append(" §cUndead Adventurer"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(true);

    // AI Goals
    private RpgTacticalBowGoal<UndeadAdventurerBossEntity> bowGoal;
    private RpgDuelCombatGoal<UndeadAdventurerBossEntity> meleeGoal;

    // Combat states & timers
    private long lastAttackGameTime = 0L;
    private int parryTicks = 0;
    private int eatTicks = 0;
    private int eatCooldown = 500; // ~25 seconds between eating windows in Phase 2
    private float eatInterruptionDamage = 0.0F;
    private int dashTimer = 100; // Cooldown to dash towards player in melee phases

    // Phase 3 Guard & Posture
    private float guardPosture = 100.0F;
    private int stunTicks = 0;

    // Phase 1 High ground leap timer
    private int highGroundLeapTimer = 160;

    public UndeadAdventurerBossEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.bowGoal = new RpgTacticalBowGoal<>(this, 1.05D, 16, 18.0F);
        this.meleeGoal = new RpgDuelCombatGoal<>(this, 1.30D);
        this.reassessWeaponGoal();
        this.xpReward = 350;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.45)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COMBAT_COMBO, 0);
        builder.define(DATA_IS_PARRYING, false);
        builder.define(DATA_BOSS_PHASE, 1);
        builder.define(DATA_IS_EATING, false);
        builder.define(DATA_IS_STUNNED, false);
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

    public boolean isStunned() {
        return this.entityData.get(DATA_IS_STUNNED);
    }

    public void setStunned(boolean stunned) {
        this.entityData.set(DATA_IS_STUNNED, stunned);
    }

    @Override
    public int getCombatCombo() {
        return this.entityData.get(DATA_COMBAT_COMBO);
    }

    @Override
    public void setCombatCombo(int combo) {
        this.entityData.set(DATA_COMBAT_COMBO, combo);
    }

    @Override
    public boolean isBlockingOrParrying() {
        return this.entityData.get(DATA_IS_PARRYING) || (getBossPhase() == 3 && !isStunned());
    }

    @Override
    public void setBlockingOrParrying(boolean parrying) {
        this.entityData.set(DATA_IS_PARRYING, parrying);
        if (parrying) {
            this.parryTicks = 8;
        }
    }

    @Override
    public long getLastAttackGameTime() {
        return this.lastAttackGameTime;
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
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AdventurerNpcEntity.class, true));
    }

    public void reassessWeaponGoal() {
        if (this.level() != null && !this.level().isClientSide) {
            if (this.meleeGoal == null || this.bowGoal == null) {
                return;
            }
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);

            int phase = getBossPhase();
            if (phase == 1) {
                this.goalSelector.addGoal(2, this.bowGoal);
            } else {
                this.goalSelector.addGoal(2, this.meleeGoal);
            }
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        // Always Threat Level B
        this.setData(ModAttachments.MOB_RANK, ThreatLevel.B.ordinal());
        this.setCustomName(Component.literal("§5§l[Jefe] ").append(ThreatLevel.B.getBadge()).append(" §cUndead Adventurer"));
        this.setCustomNameVisible(true);

        // Multiplayer / Proximity HP and Damage Scaling
        if (level instanceof ServerLevel serverLevel) {
            List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(
                    Player.class,
                    this.getBoundingBox().inflate(48.0),
                    p -> !p.isSpectator() && p.isAlive()
            );
            int playerCount = Math.max(1, nearbyPlayers.size());

            double scaledMaxHealth = 200.0 + (playerCount - 1) * 75.0;
            double scaledDamage = 6.0 + (playerCount - 1) * 1.5;

            var hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) {
                hpAttr.setBaseValue(scaledMaxHealth);
                this.setHealth((float) scaledMaxHealth);
            }

            var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) {
                dmgAttr.setBaseValue(scaledDamage);
            }
        }

        // Start with Phase 1 equipment
        setupPhase1Equipment();
        return data;
    }

    private void setupPhase1Equipment() {
        this.setBossPhase(1);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        reassessWeaponGoal();
    }

    private void setupPhase2Equipment() {
        this.setBossPhase(2);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        reassessWeaponGoal();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5F, 0.8F);

            // Knockback nearby players slightly
            for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(6.0))) {
                Vec3 push = player.position().subtract(this.position()).normalize().scale(1.2).add(0, 0.3, 0);
                player.push(push.x, push.y, push.z);
                player.sendSystemMessage(Component.literal("§5§l[Undead Adventurer]: §c\"¡El arco fue solo un calentamiento... ahora probad el filo de mi acero!\""));
            }
        }
    }

    private void setupPhase3Equipment() {
        if (isEating()) {
            stopEatingWindow();
        }
        this.setBossPhase(3);
        this.guardPosture = 100.0F;
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        reassessWeaponGoal();

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_USE, SoundSource.HOSTILE, 1.5F, 0.7F);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.8F, 1.4F);

            for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(12.0))) {
                player.sendSystemMessage(Component.literal("§4§l[Undead Adventurer]: §c\"¡Incluso al borde de la muerte, mi guardia es impenetrable! ¡Desgastad mi postura si queréis derribarme!\""));
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Update Boss Bar
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.level().isClientSide()) return;

        float healthRatio = this.getHealth() / this.getMaxHealth();
        int currentPhase = getBossPhase();

        // --- PHASE TRANSITIONS ---
        if (currentPhase == 1 && healthRatio < 0.10F) {
            setupPhase3Equipment();
        } else if (currentPhase == 1 && healthRatio < 0.75F) {
            setupPhase2Equipment();
        } else if (currentPhase == 2 && healthRatio < 0.10F) {
            setupPhase3Equipment();
        }

        // --- PARRIED STATUS TICK ---
        if (this.parryTicks > 0) {
            this.parryTicks--;
            if (this.parryTicks <= 0) {
                setBlockingOrParrying(false);
            }
        }

        // --- STUNNED TICK (GUARD BROKEN) ---
        if (isStunned()) {
            this.stunTicks--;
            this.getNavigation().stop();
            if (this.level() instanceof ServerLevel sLevel) {
                sLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.0, this.getZ(), 3, 0.2, 0.1, 0.2, 0.05);
                sLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.8, this.getZ(), 2, 0.15, 0.15, 0.15, 0.05);
            }
            if (this.stunTicks <= 0) {
                setStunned(false);
                this.guardPosture = 60.0F; // Partially recovered guard
            }
            return;
        }

        // --- PHASE 1: HIGH GROUND LEAP ---
        if (currentPhase == 1 && this.getTarget() != null) {
            if (--this.highGroundLeapTimer <= 0) {
                this.highGroundLeapTimer = 160 + this.getRandom().nextInt(80);
                attemptHighGroundLeap();
            }
        }

        // --- PHASE 2: REGENERATIVE BREATHER (EATING ROTTEN FLESH) ---
        if (currentPhase == 2 && !isStunned()) {
            if (isEating()) {
                handleEatingTick();
            } else {
                if (--this.eatCooldown <= 0 && this.getHealth() < (this.getMaxHealth() * 0.70F)) {
                    startEatingWindow();
                }
            }
        }

        // --- MELEE DASH TO CLOSE GAP (PHASES 2 & 3) ---
        if ((currentPhase == 2 || currentPhase == 3) && !isStunned() && !isEating()) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                if (--this.dashTimer <= 0) {
                    double distSq = this.distanceToSqr(target);
                    if (distSq >= 16.0 && distSq <= 256.0 && this.onGround()) {
                        performClosingDash(target);
                        this.dashTimer = 70 + this.getRandom().nextInt(40);
                    } else {
                        this.dashTimer = 25;
                    }
                }
            }
        }
    }

    private void performClosingDash(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        Vec3 toTarget = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(toTarget.x * 1.30, 0.22, toTarget.z * 1.30);
        this.hurtMarked = true;

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 1.3F);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 0.6F, 1.7F);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.8, this.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + toTarget.x * 1.2, this.getY() + 0.8, this.getZ() + toTarget.z * 1.2, 1, 0, 0, 0, 0);
    }

    /**
     * Phase 1 Mechanic: Searches for elevated terrain and leaps up to gain tactical advantage.
     */
    private void attemptHighGroundLeap() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        BlockPos currentPos = this.blockPosition();

        for (int dx = -6; dx <= 6; dx += 2) {
            for (int dz = -6; dz <= 6; dz += 2) {
                for (int dy = 2; dy <= 4; dy++) {
                    BlockPos candidate = currentPos.offset(dx, dy, dz);
                    if (serverLevel.getBlockState(candidate.below()).isSolid()
                            && serverLevel.getBlockState(candidate).isAir()
                            && serverLevel.getBlockState(candidate.above()).isAir()) {

                        Vec3 jumpVec = new Vec3(dx * 0.2, 0.65, dz * 0.2);
                        this.setDeltaMovement(jumpVec);
                        this.hasImpulse = true;

                        serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 10, 0.3, 0.1, 0.3, 0.08);
                        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.0F, 1.2F);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public boolean isCombatSuspended() {
        return isEating() || isStunned();
    }

    /**
     * Phase 2 Mechanic: Enters the eating breather window.
     * He eats rotten flesh as a pacing break for players and becomes completely invulnerable.
     */
    private void startEatingWindow() {
        setEating(true);
        this.eatTicks = 100; // 5 full seconds of breather window for players
        this.getNavigation().stop();
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ROTTEN_FLESH));

        // Jump backward slightly to initiate breather distance
        LivingEntity target = this.getTarget();
        if (target != null) {
            Vec3 away = this.position().subtract(target.position()).normalize().scale(0.75).add(0, 0.25, 0);
            this.setDeltaMovement(away);
            this.hasImpulse = true;
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.1F, 0.7F);
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.7F, 1.8F);

            for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0))) {
                player.sendSystemMessage(Component.literal("§7§o[Undead Adventurer devora carne pútrida protegido por un aura impenetrable... ¡Aprovecha el respiro!]"));
            }
        }
    }

    private void handleEatingTick() {
        this.eatTicks--;
        this.getNavigation().stop();

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.eatTicks % 5 == 0) {
                serverLevel.playSound(null, this.getX(), this.getY() + 1.5, this.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 0.9F, 0.8F + this.getRandom().nextFloat() * 0.3F);
                serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ROTTEN_FLESH)),
                        this.getX(), this.getY() + 1.5, this.getZ(),
                        5, 0.15, 0.15, 0.15, 0.05
                );
            }

            // Protective invulnerability barrier aura around him
            if (this.eatTicks % 8 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.0, this.getZ(), 4, 0.3, 0.4, 0.3, 0.05);
            }
        }

        // Cosmetic breather: NO HP regeneration so it doesn't extend the battle artificially!

        if (this.eatTicks <= 0) {
            stopEatingWindow();
        }
    }

    private void stopEatingWindow() {
        setEating(false);
        this.eatCooldown = 600; // Reset cooldown (30 seconds)
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0F, 0.8F);
            serverLevel.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.2, this.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return super.hurt(source, amount);
        }

        // Invulnerable during Phase 2 eating breather window!
        if (isEating()) {
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 1.4F);
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.2, 0.2, 0.2, 0.1);
            }
            return false;
        }

        if (isStunned()) {
            // Takes +50% extra vulnerability damage when stunned!
            return super.hurt(source, amount * 1.5F);
        }

        // --- PHASE 3: SHIELD GUARD & POSTURE WEAR-DOWN ---
        if (getBossPhase() == 3) {
            Entity attacker = source.getEntity();
            boolean isFrontal = false;
            if (attacker != null) {
                Vec3 toAttacker = attacker.position().subtract(this.position()).normalize();
                Vec3 look = this.getLookAngle();
                if (look.dot(toAttacker) > 0.25) {
                    isFrontal = true;
                }
            }

            if (isFrontal && !source.is(DamageTypes.MAGIC) && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
                // Determine guard damage
                float guardDmg = 15.0F;
                if (attacker instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof AxeItem) {
                    guardDmg = 45.0F; // Axes crush shields!
                } else if (amount >= 10.0F) {
                    guardDmg = 25.0F; // Heavy hits
                }

                this.guardPosture -= guardDmg;

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2F, 1.0F);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.3, this.getZ(), 10, 0.2, 0.2, 0.2, 0.15);
                }

                // Check Guard Break!
                if (this.guardPosture <= 0.0F) {
                    // STUNNED GUARD BREAK!
                    setStunned(true);
                    this.stunTicks = 80; // 4 seconds helpless stun
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.HOSTILE, 1.5F, 0.85F);
                        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.2F, 0.9F);
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.2, this.getZ(), 1, 0, 0, 0, 0);

                        for (Player p : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(12.0))) {
                            p.sendSystemMessage(Component.literal("§e§l[¡GUARDIA ROTA!] §f¡El Undead Adventurer ha quedado aturdido y desprotegido!"));
                        }
                    }
                }

                // Absorbs 90% frontal damage while guard holds
                return super.hurt(source, amount * 0.10F);
            }
        }

        return super.hurt(source, amount);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack bowStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack arrowStack = this.getProjectile(bowStack);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, distanceFactor, bowStack);

        if (arrow instanceof Arrow tipped) {
            // Lingering wither / slowness effect for the boss's arrows
            tipped.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            tipped.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + dist * 0.16D, dz, 1.8F, (float)(10 - this.level().getDifficulty().getId() * 3));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 0.9F / (this.getRandom().nextFloat() * 0.3F + 0.8F));
        this.level().addFreshEntity(arrow);
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

    @Override
    public void die(DamageSource damageSource) {
        this.bossEvent.removeAllPlayers();
        super.die(damageSource);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        // Generous Boss Loot
        this.spawnAtLocation(new ItemStack(Items.DIAMOND, 4 + level.getRandom().nextInt(4)));
        this.spawnAtLocation(new ItemStack(Items.EMERALD, 8 + level.getRandom().nextInt(6)));
        this.spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
        this.spawnAtLocation(new ItemStack(Items.BOW, 1));
        this.spawnAtLocation(new ItemStack(Items.DIAMOND_SWORD, 1));

        // Victory fanfare in chat & mark boss defeated once
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32.0))) {
            player.getPersistentData().putBoolean("DefeatedUndeadBossOnce", true);
            if (player instanceof ServerPlayer sp) {
                com.pablo.rpglevels.advancements.ModAdvancements.award(sp, com.pablo.rpglevels.advancements.ModAdvancements.DEFEAT_UNDEAD_ADVENTURER);
            }
            player.sendSystemMessage(Component.literal("§6§l[¡VICTORIA!] §e¡El Undead Adventurer ha sido derrotado! Las almas de los caídos por fin descansan."));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("BossPhase", getBossPhase());
        compound.putFloat("GuardPosture", this.guardPosture);
        compound.putInt("EatCooldown", this.eatCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("BossPhase")) {
            setBossPhase(compound.getInt("BossPhase"));
        }
        if (compound.contains("GuardPosture")) {
            this.guardPosture = compound.getFloat("GuardPosture");
        }
        if (compound.contains("EatCooldown")) {
            this.eatCooldown = compound.getInt("EatCooldown");
        }
        reassessWeaponGoal();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayerSq) {
        return false;
    }
}
