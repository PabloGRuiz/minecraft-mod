package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.mobs.IrregularMobHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import com.pablo.tenseicraft.entities.ai.RpgCombatDuelist;
import com.pablo.tenseicraft.entities.ai.RpgDuelCombatGoal;
import com.pablo.tenseicraft.entities.ai.RpgTacticalBowGoal;

public class BanditEntity extends Monster implements RangedAttackMob, RpgCombatDuelist {

    private static final EntityDataAccessor<Integer> DATA_SKIN_VARIANT =
            SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_LEADER =
            SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COMBAT_COMBO =
            SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_PARRYING =
            SynchedEntityData.defineId(BanditEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int TOTAL_REGULAR_VARIANTS = 3;

    private final RpgTacticalBowGoal<BanditEntity> bowGoal =
            new RpgTacticalBowGoal<>(this, 1.0D, 18, 16.0F);
    private final RpgDuelCombatGoal<BanditEntity> meleeGoal =
            new RpgDuelCombatGoal<>(this, 1.25D);

    private long lastAttackGameTime = 0L;
    private int parryTicks = 0;

    public BanditEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.reassessWeaponGoal();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN_VARIANT, 0);
        builder.define(DATA_IS_LEADER, false);
        builder.define(DATA_COMBAT_COMBO, 0);
        builder.define(DATA_IS_PARRYING, false);
    }

    public int getSkinVariant() {
        return this.entityData.get(DATA_SKIN_VARIANT);
    }

    public void setSkinVariant(int variant) {
        this.entityData.set(DATA_SKIN_VARIANT, Math.max(0, variant % TOTAL_REGULAR_VARIANTS));
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
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            if (this.parryTicks > 0) {
                this.parryTicks--;
                if (this.parryTicks <= 0) {
                    setBlockingOrParrying(false);
                }
            }
        }
    }

    public boolean isLeader() {
        return this.entityData.get(DATA_IS_LEADER);
    }

    public void setLeader(boolean leader) {
        this.entityData.set(DATA_IS_LEADER, leader);
        if (leader) {
            // Boost leader stats
            var hp = this.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(55.0);
                this.setHealth(55.0f);
            }
            var dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmg != null) {
                dmg.setBaseValue(7.0);
            }
            var armor = this.getAttribute(Attributes.ARMOR);
            if (armor != null) {
                armor.setBaseValue(10.0);
            }
            var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.31);
            }

            // Set Rank from actual measured stats
            updateMeasuredRankAndTitle();
        }
    }

    /**
     * Measures the bandit's actual current stats (HP, weapon damage, Irregular status)
     * via {@link ThreatLevel#determineThreatLevel} and updates both MOD_RANK and overhead tag
     * so they always match what Jade and internal threat logic see.
     */
    public void updateMeasuredRankAndTitle() {
        ThreatLevel measured = ThreatLevel.determineThreatLevel(this);
        this.setData(ModAttachments.MOB_RANK, measured.getRankIndex());

        boolean leader = this.isLeader();
        boolean irregular = IrregularMobHandler.isIrregular(this);
        boolean archer = !leader && this.getMainHandItem().getItem() instanceof BowItem;
        String roleLabel = leader ? "Bandido Veterano" : (archer ? "Bandido Arquero" : "Bandido Asaltante");

        MutableComponent rankBadge = measured.getBadge();

        MutableComponent name = Component.empty();
        if (irregular) {
            name.append(measured.getShortBadge()).append(" ");
            if (leader) {
                name.append("§c§l[Líder] ");
            }
            name.append("§c" + roleLabel + " Irregular");
        } else if (leader) {
            name.append("§c§l[Líder] ").append(rankBadge).append(" §c" + roleLabel);
        } else {
            name.append("§c[" + roleLabel + "] ").append(rankBadge);
        }

        this.setCustomName(name);
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Targets: Alert allies when hurt, attack players, villagers, townswomen and adventurers
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, BanditEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AdventurerNpcEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, TownswomanNpcEntity.class, true));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
    }

    public void reassessWeaponGoal() {
        if (this.level() != null && !this.level().isClientSide) {
            if (this.meleeGoal == null || this.bowGoal == null) {
                return;
            }
            this.goalSelector.removeGoal(this.meleeGoal);
            this.goalSelector.removeGoal(this.bowGoal);
            ItemStack itemstack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
            if (itemstack.is(Items.BOW)) {
                this.goalSelector.addGoal(2, this.bowGoal);
            } else {
                this.goalSelector.addGoal(2, this.meleeGoal);
            }
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!this.level().isClientSide && slot == EquipmentSlot.MAINHAND) {
            this.reassessWeaponGoal();
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        RandomSource random = level.getRandom();

        setSkinVariant(random.nextInt(TOTAL_REGULAR_VARIANTS));
        populateDefaultEquipmentSlots(random, difficulty);
        reassessWeaponGoal();
        updateMeasuredRankAndTitle();

        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);

        if (this.isLeader()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.15F);
            this.setDropChance(EquipmentSlot.OFFHAND, 0.15F);
            return;
        }

        boolean alreadyIrregular = IrregularMobHandler.isIrregular(this);

        // Regular bandits: 35% archer, 65% melee
        boolean isArcher = random.nextFloat() < 0.35f;
        if (isArcher) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            if (!alreadyIrregular) {
                var hp = this.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    double rolledHp = 18.0 + random.nextInt(9); // 18..26 HP
                    hp.setBaseValue(rolledHp);
                    this.setHealth((float) rolledHp);
                }
            }
        } else {
            int weaponRoll = random.nextInt(3);
            ItemStack meleeWeapon = switch (weaponRoll) {
                case 0 -> new ItemStack(Items.IRON_SWORD);
                case 1 -> new ItemStack(Items.IRON_AXE);
                default -> new ItemStack(Items.STONE_SWORD);
            };
            this.setItemSlot(EquipmentSlot.MAINHAND, meleeWeapon);

            // 45% chance to have a shield in offhand
            if (random.nextFloat() < 0.45f) {
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }

            if (random.nextBoolean()) {
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            }

            if (!alreadyIrregular) {
                var hp = this.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    double rolledHp = switch (weaponRoll) {
                        case 1 -> 32.0 + random.nextInt(13); // Veteran axe raider: 32..44 HP
                        case 0 -> 24.0 + random.nextInt(10); // Standard iron sword raider: 24..33 HP
                        default -> 14.0 + random.nextInt(8); // Novice stone sword bandit: 14..21 HP
                    };
                    hp.setBaseValue(rolledHp);
                    this.setHealth((float) rolledHp);
                }
            }
        }

        updateMeasuredRankAndTitle();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack bowStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack arrowStack = this.getProjectile(bowStack);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, distanceFactor, bowStack);

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + dist * 0.2D, dz, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        RandomSource random = this.getRandom();

        // 1. Lingotes de Hierro
        int ironCount = isLeader() ? (2 + random.nextInt(4)) : (random.nextInt(3));
        if (ironCount > 0) {
            this.spawnAtLocation(new ItemStack(Items.IRON_INGOT, ironCount));
        }

        // 2. Cuero
        int leatherCount = 1 + random.nextInt(isLeader() ? 4 : 3);
        this.spawnAtLocation(new ItemStack(Items.LEATHER, leatherCount));

        // 3. Comida: Pan o Carne Cocida
        int foodCount = 1 + random.nextInt(3);
        ItemStack food = random.nextBoolean() ? new ItemStack(Items.BREAD, foodCount) : new ItemStack(Items.COOKED_BEEF, foodCount);
        this.spawnAtLocation(food);

        // 4. Esmeraldas
        int emeraldCount = isLeader() ? (3 + random.nextInt(5)) : (random.nextInt(3));
        if (emeraldCount > 0) {
            this.spawnAtLocation(new ItemStack(Items.EMERALD, emeraldCount));
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VINDICATOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VINDICATOR_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SkinVariant", getSkinVariant());
        compound.putBoolean("IsLeader", isLeader());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SkinVariant")) {
            setSkinVariant(compound.getInt("SkinVariant"));
        }
        if (compound.contains("IsLeader")) {
            this.entityData.set(DATA_IS_LEADER, compound.getBoolean("IsLeader"));
        }
        reassessWeaponGoal();
        updateMeasuredRankAndTitle();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayerSq) {
        return !this.isPersistenceRequired() && !this.hasCustomName();
    }
}
