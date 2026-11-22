package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.entities.ai.RpgCombatDuelist;
import com.pablo.tenseicraft.entities.ai.RpgDuelCombatGoal;
import com.pablo.tenseicraft.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Final Alpha Boss: "Cultista" (Hazard Threat Level)
 * A corrupted Spiritual Reaper seeking to resurrect the ancient demon race.
 *
 * 3 Dynamic Combat Phases:
 * Phase 1 (100% -> 60% HP):
 * - Wields standard Spiritual Sword.
 * - Relentless melee pursuit with gap-closing dashes.
 * - High propensity to switch targets between party members.
 * - Casts ranged hindering techniques: Low Paralize and Shadow Balls.
 * - Chanting pause ("Afinidad Mágica"): vulnerability window for players.
 *
 * Phase 2 (<= 60% HP):
 * - Struck by lightning, awakens his blade: "¡Despierta: Shadowblade!".
 * - Switches weapon to awakened Cultist Spiritual Sword.
 * - Significantly increased attack damage.
 *
 * Phase 3 / Rage Mode (<= 20% HP):
 * - Enters Rage state (enhanced speed, fury particles, red boss bar).
 * - Heals back up to 50% HP (300 HP) using "Técnica Espiritual Nro. 15: High Fast Healing".
 * - Shadow Step: teleports behind target and delivers a heavy strike.
 * - Occasional breathers/pauses retained for balanced damage windows.
 *
 * Defeat:
 * - Broadcasts Alpha Version completion toast & title.
 */
public class CultistBossEntity extends Monster implements RpgCombatDuelist {

    private static final EntityDataAccessor<Integer> DATA_COMBAT_COMBO =
            SynchedEntityData.defineId(CultistBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_PARRYING =
            SynchedEntityData.defineId(CultistBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOSS_PHASE =
            SynchedEntityData.defineId(CultistBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_CHANTER =
            SynchedEntityData.defineId(CultistBossEntity.class, EntityDataSerializers.BOOLEAN);

    // Boss bar
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("§5§l[Jefe] ").append(ThreatLevel.HAZARD.getBadge()).append(" §cCultista §7- §dSegador Corrupto"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(true);

    // AI Goals
    private RpgDuelCombatGoal<CultistBossEntity> meleeGoal;

    // Timers & Cooldowns
    private long lastAttackGameTime = 0L;
    private int dashCooldown = 80;
    private int retargetCooldown = 100;
    private int rangedSpellCooldown = 90;
    private int chantPauseCooldown = 240;
    private int chantPauseTicks = 0;
    private int shadowStepCooldown = 110;
    private boolean healedPhase3 = false;

    public CultistBossEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        if (this.meleeGoal == null) {
            this.meleeGoal = new RpgDuelCombatGoal<>(this, 1.35D);
        }
        this.xpReward = 600;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COMBAT_COMBO, 0);
        builder.define(DATA_IS_PARRYING, false);
        builder.define(DATA_BOSS_PHASE, 1);
        builder.define(DATA_IS_CHANTER, false);
    }

    public int getBossPhase() {
        return this.entityData.get(DATA_BOSS_PHASE);
    }

    public void setBossPhase(int phase) {
        this.entityData.set(DATA_BOSS_PHASE, phase);
    }

    public boolean isChanting() {
        return this.entityData.get(DATA_IS_CHANTER);
    }

    public void setChanting(boolean chanting) {
        this.entityData.set(DATA_IS_CHANTER, chanting);
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
        return this.entityData.get(DATA_IS_PARRYING);
    }

    @Override
    public void setBlockingOrParrying(boolean parrying) {
        this.entityData.set(DATA_IS_PARRYING, parrying);
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
    public boolean isCombatSuspended() {
        return isChanting();
    }

    @Override
    protected void registerGoals() {
        if (this.meleeGoal == null) {
            this.meleeGoal = new RpgDuelCombatGoal<>(this, 1.35D);
        }
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, this.meleeGoal);
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AdventurerNpcEntity.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        this.setData(ModAttachments.MOB_RANK, ThreatLevel.HAZARD.ordinal());
        this.setCustomName(Component.literal("§5§l[Jefe] ").append(ThreatLevel.HAZARD.getBadge()).append(" §cCultista §7- §dSegador Corrupto"));
        this.setCustomNameVisible(true);

        // Equip initial gear: Standard Spiritual Sword
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SPIRITUAL_SWORD.get()));
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.setBossPhase(1);

        return data;
    }

    @Override
    public void tick() {
        super.tick();

        // Update Boss Bar Progress
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.level().isClientSide()) {
            if (isChanting()) {
                Vec3 p = this.position();
                this.level().addParticle(ParticleTypes.ENCHANTED_HIT, p.x, p.y + 1.2, p.z,
                        (this.random.nextDouble() - 0.5) * 0.4, 0.2, (this.random.nextDouble() - 0.5) * 0.4);
                this.level().addParticle(ParticleTypes.PORTAL, p.x, p.y + 1.0, p.z,
                        (this.random.nextDouble() - 0.5) * 0.3, 0.1, (this.random.nextDouble() - 0.5) * 0.3);
            }
            if (getBossPhase() == 3) {
                Vec3 p = this.position();
                if (this.random.nextFloat() < 0.35f) {
                    this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 0.8, p.z, 0, 0.03, 0);
                    this.level().addParticle(ParticleTypes.SMOKE, p.x, p.y + 1.5, p.z, 0, 0.02, 0);
                }
            }
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();
        float currentHp = this.getHealth();
        int currentPhase = getBossPhase();

        // --- PHASE 2 TRANSITION: <= 60% HP (<= 360 HP) ---
        if (currentPhase == 1 && currentHp <= 360.0F) {
            enterPhase2(serverLevel);
        }

        // --- PHASE 3 / RAGE TRANSITION: <= 20% HP (<= 120 HP) ---
        if (currentPhase == 2 && currentHp <= 120.0F && !healedPhase3) {
            enterPhase3(serverLevel);
        }

        // --- CHANTING PAUSE TICK ("Afinidad Mágica" Vulnerability Window) ---
        if (this.chantPauseTicks > 0) {
            this.chantPauseTicks--;
            this.getNavigation().stop();
            if (this.chantPauseTicks % 15 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.2, this.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
            }
            if (this.chantPauseTicks <= 0) {
                setChanting(false);
            }
            return; // Suspends attacks while channeling pause
        }

        // Trigger chanting pause periodically (damage window for players)
        if (--this.chantPauseCooldown <= 0) {
            triggerChantPause(serverLevel);
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        double distSq = this.distanceToSqr(target);
        double dist = Math.sqrt(distSq);

        // --- TARGET SWITCHING ---
        // Highly prone to switching target between party members / players
        if (--this.retargetCooldown <= 0) {
            this.retargetCooldown = 80 + this.getRandom().nextInt(60); // 4-7 seconds
            attemptSwitchTarget(serverLevel);
            // Refresh target after switch
            target = this.getTarget();
            if (target == null) return;
            distSq = this.distanceToSqr(target);
            dist = Math.sqrt(distSq);
        }

        // --- GAP-CLOSING DASH (Melee Aggression) ---
        if (--this.dashCooldown <= 0 && dist >= 4.0 && dist <= 18.0) {
            this.dashCooldown = (currentPhase == 3) ? 50 + this.getRandom().nextInt(30) : 75 + this.getRandom().nextInt(40);
            performDash(serverLevel, target);
        }

        // --- RANGED HINDERING SPELLS (When target is far: > 8 blocks) ---
        if (--this.rangedSpellCooldown <= 0 && dist > 8.0) {
            this.rangedSpellCooldown = (currentPhase == 3) ? 60 + this.getRandom().nextInt(30) : 80 + this.getRandom().nextInt(40);
            castRangedTechnique(serverLevel, target);
        }

        // --- PHASE 3 ONLY: SHADOW STEP (Teleport Behind Target) ---
        if (currentPhase == 3 && --this.shadowStepCooldown <= 0) {
            this.shadowStepCooldown = 90 + this.getRandom().nextInt(40); // 4.5-6.5 seconds
            performShadowStep(serverLevel, target);
        }
    }

    private void enterPhase2(ServerLevel serverLevel) {
        setBossPhase(2);

        // Visual lightning bolt striking the Cultist
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning != null) {
            lightning.moveTo(this.position());
            lightning.setVisualOnly(true);
            serverLevel.addFreshEntity(lightning);
        }

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.2F, 1.0F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.3F);

        serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.5, 0.8, 0.5, 0.15);
        serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 50, 0.6, 0.8, 0.6, 0.3);

        // Sword Awakening Announcement
        broadcastChatMessage(serverLevel, Component.literal("§5[Cultista]: §d\"¡Despierta: Shadowblade!\""));

        // Equip Awakened Sword
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.CULTIST_SPIRITUAL_SWORD.get()));

        // Increase base attack damage significantly
        var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.setBaseValue(22.0);
        }
    }

    private void enterPhase3(ServerLevel serverLevel) {
        setBossPhase(3);
        this.healedPhase3 = true;

        // Roar and Rage visuals
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1.0F, 1.2F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.4F, 1.0F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5F, 0.9F);

        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0, this.getZ(), 80, 0.6, 1.0, 0.6, 0.25);
        serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 2.0, this.getZ(), 15, 0.4, 0.4, 0.4, 0.05);

        // Chat announcement of ancient advanced technique
        broadcastChatMessage(serverLevel, Component.literal("§5[Cultista]: §c\"¡Sentid el verdadero poder del abismo! ¡Técnica Espiritual Nro. 15: High Fast Healing!\""));

        // Heal back up to 50% HP (300 HP)
        this.setHealth(300.0F);

        // Rage mode stat buffs: red boss bar, speed boost, max damage
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);

        var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.38); // +20% faster
        }

        var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.setBaseValue(28.0);
        }
    }

    private void triggerChantPause(ServerLevel serverLevel) {
        int currentPhase = getBossPhase();
        this.chantPauseCooldown = (currentPhase == 3) ? 280 + this.getRandom().nextInt(60) : 220 + this.getRandom().nextInt(60);
        this.chantPauseTicks = 60; // 3 seconds vulnerability pause
        setChanting(true);

        this.getNavigation().stop();
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.2F, 1.4F);
        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 25, 0.4, 0.6, 0.4, 0.08);

        broadcastChatMessage(serverLevel, Component.literal("§5[Cultista]: §d«Anima Aether Fluctuo!» §7(Canalizando energía... ¡Atacad ahora!)"));
    }

    private void performDash(ServerLevel serverLevel, LivingEntity target) {
        Vec3 diff = target.position().subtract(this.position());
        Vec3 norm = diff.normalize();

        // Propel forward
        this.setDeltaMovement(norm.x * 1.45, 0.3, norm.z * 1.45);
        this.hasImpulse = true;

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 1.1F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.7F, 1.8F);

        serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.8, this.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
        serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 15, 0.2, 0.2, 0.2, 0.05);
    }

    private void performShadowStep(ServerLevel serverLevel, LivingEntity target) {
        // Calculate position 1.8 blocks directly behind the target
        Vec3 targetLook = target.getLookAngle();
        Vec3 behind = target.position().subtract(targetLook.scale(1.8));

        // Ensure floor is solid
        BlockPos targetFloor = BlockPos.containing(behind.x, target.getY(), behind.z);
        if (serverLevel.getBlockState(targetFloor).isAir() && !serverLevel.getBlockState(targetFloor.below()).isAir()) {
            behind = new Vec3(behind.x, targetFloor.getY(), behind.z);
        } else {
            behind = new Vec3(behind.x, target.getY(), behind.z);
        }

        // Particle effect at old position
        serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 1.0F);

        // Teleport
        this.teleportTo(behind.x, behind.y, behind.z);
        this.lookAt(target, 360, 360);

        // Particle effect at new position
        serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
        serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.2F, 0.8F);

        // Immediate melee strike
        this.doHurtTarget(target);
    }

    private void castRangedTechnique(ServerLevel serverLevel, LivingEntity target) {
        Vec3 toTarget = target.getEyePosition().subtract(this.getEyePosition()).normalize();

        boolean useParalysis = this.getRandom().nextBoolean();
        if (useParalysis) {
            // Paralysis Spell Projectile
            MagicSpellProjectileEntity proj = new MagicSpellProjectileEntity(
                    serverLevel, this, MagicSpellProjectileEntity.SpellType.LOW_PARALIZE, "none", 0.0f
            );
            proj.setPos(this.getX() + toTarget.x * 0.8, this.getEyeY() - 0.1 + toTarget.y * 0.8, this.getZ() + toTarget.z * 0.8);
            proj.shoot(toTarget.x, toTarget.y, toTarget.z, 1.6f, 1.0f);
            serverLevel.addFreshEntity(proj);

            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 0.5F, 2.0F);
        } else {
            // Shadow Ball (Elemental Ball with umbra / shadow element)
            float damage = (getBossPhase() == 3) ? 14.0f : 10.0f;
            MagicSpellProjectileEntity proj = new MagicSpellProjectileEntity(
                    serverLevel, this, MagicSpellProjectileEntity.SpellType.ELEMENTAL_BALL, "umbra", damage
            );
            proj.setPos(this.getX() + toTarget.x * 0.8, this.getEyeY() - 0.1 + toTarget.y * 0.8, this.getZ() + toTarget.z * 0.8);
            proj.shoot(toTarget.x, toTarget.y, toTarget.z, 1.5f, 1.2f);
            serverLevel.addFreshEntity(proj);

            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8F, 1.2F);
        }
    }

    private void attemptSwitchTarget(ServerLevel serverLevel) {
        List<LivingEntity> potentialTargets = new ArrayList<>();

        for (Player p : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0))) {
            if (!p.isSpectator() && p.isAlive()) {
                potentialTargets.add(p);
            }
        }

        for (AdventurerNpcEntity adv : serverLevel.getEntitiesOfClass(AdventurerNpcEntity.class, this.getBoundingBox().inflate(24.0))) {
            if (adv.isAlive() && adv.isHired()) {
                potentialTargets.add(adv);
            }
        }

        if (potentialTargets.size() > 1) {
            // Pick a different target at random
            LivingEntity current = this.getTarget();
            potentialTargets.remove(current);
            if (!potentialTargets.isEmpty()) {
                LivingEntity newTarget = potentialTargets.get(this.getRandom().nextInt(potentialTargets.size()));
                this.setTarget(newTarget);
            }
        }
    }

    private void broadcastChatMessage(ServerLevel serverLevel, Component message) {
        for (Player player : serverLevel.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(64.0))) {
            player.sendSystemMessage(message);
        }
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
    public boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);

        // Guaranteed Boss XP drop
        net.minecraft.world.entity.ExperienceOrb.award(level, this.position(), 600);

        // Alpha Finale Drops: Awakened Sword, Escape Scrolls, Emeralds, Diamonds
        this.spawnAtLocation(new ItemStack(ModItems.CULTIST_SPIRITUAL_SWORD.get(), 1));
        this.spawnAtLocation(new ItemStack(ModItems.ESCAPE_SCROLL.get(), 3));
        this.spawnAtLocation(new ItemStack(Items.EMERALD, 16 + level.getRandom().nextInt(16)));
        this.spawnAtLocation(new ItemStack(Items.DIAMOND, 3 + level.getRandom().nextInt(4)));
        this.spawnAtLocation(new ItemStack(Items.NETHERITE_SCRAP, 1 + level.getRandom().nextInt(2)));

        // Celebrate Alpha Completion with all players in the dimension/nearby
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(96.0))) {
            player.getPersistentData().putBoolean("RpgKilledCultistBoss", true);

            com.pablo.tenseicraft.quests.PlayerQuestData questData = player.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_QUEST_DATA);
            if (questData != null && questData.isQuestActive("alice_cultist_hunt")) {
                questData.setProgress("alice_cultist_hunt", 1);
            }

            if (player instanceof ServerPlayer sp) {
                var party = com.pablo.tenseicraft.party.PartyManager.getParty(sp.getUUID());
                if (party != null) {
                    for (java.util.UUID memberUuid : party.getMemberUUIDs()) {
                        ServerPlayer member = sp.serverLevel().getServer().getPlayerList().getPlayer(memberUuid);
                        if (member != null) {
                            member.getPersistentData().putBoolean("RpgKilledCultistBoss", true);
                            var mQuestData = member.getData(com.pablo.tenseicraft.data.ModAttachments.PLAYER_QUEST_DATA);
                            if (mQuestData != null && mQuestData.isQuestActive("alice_cultist_hunt")) {
                                mQuestData.setProgress("alice_cultist_hunt", 1);
                                com.pablo.tenseicraft.network.ModNetworking.syncQuestsToPlayer(member);
                            }
                        }
                    }
                }
                com.pablo.tenseicraft.network.ModNetworking.syncQuestsToPlayer(sp);
                com.pablo.tenseicraft.advancements.ModAdvancements.award(sp, com.pablo.tenseicraft.advancements.ModAdvancements.DEFEAT_CULTIST);
                sp.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
                sp.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l¡ALPHA COMPLETADA!")));
                sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eHas purificado al Segador Corrupto en el Abismo")));
            }

            player.sendSystemMessage(Component.literal("§6====================================================="));
            player.sendSystemMessage(Component.literal("§e§l              ¡FELICITACIONES, AVENTURERO!"));
            player.sendSystemMessage(Component.literal("§f  Has derrotado al §5Cultista (Hazard)§f, concluyendo con"));
            player.sendSystemMessage(Component.literal("§f  éxito todo el contenido de la §6Versión Alpha§f del mod."));
            player.sendSystemMessage(Component.literal("§7  ¡El gremio y los Segadores Espirituales están a salvo!"));
            player.sendSystemMessage(Component.literal("§6====================================================="));

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2F, 1.0F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("BossPhase", getBossPhase());
        compound.putBoolean("HealedPhase3", this.healedPhase3);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("BossPhase")) {
            setBossPhase(compound.getInt("BossPhase"));
        }
        if (compound.contains("HealedPhase3")) {
            this.healedPhase3 = compound.getBoolean("HealedPhase3");
        }
        if (getBossPhase() == 2) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.CULTIST_SPIRITUAL_SWORD.get()));
        } else if (getBossPhase() == 3) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.CULTIST_SPIRITUAL_SWORD.get()));
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
    }

    @Override
    public void die(DamageSource damageSource) {
        this.bossEvent.removeAllPlayers();
        super.die(damageSource);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayerSq) {
        return false;
    }
}

