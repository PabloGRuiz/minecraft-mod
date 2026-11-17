package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.entities.ai.AdventurerCompanionGoal;
import com.pablo.rpglevels.entities.ai.CompanionAttackTargetGoal;
import com.pablo.rpglevels.entities.ai.CompanionDefendOwnerGoal;
import com.pablo.rpglevels.network.OpenNpcDialoguePayload;
import com.pablo.rpglevels.social.NpcSocialManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.pablo.rpglevels.entities.ai.AdventurerCompanionGoal;
import com.pablo.rpglevels.entities.ai.CompanionAttackTargetGoal;
import com.pablo.rpglevels.entities.ai.CompanionDefendOwnerGoal;
import com.pablo.rpglevels.entities.ai.DefendVillagersGoal;
import com.pablo.rpglevels.entities.ai.RpgCombatDuelist;
import com.pablo.rpglevels.entities.ai.RpgDuelCombatGoal;
import com.pablo.rpglevels.entities.ai.RpgTacticalBowGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdventurerNpcEntity extends PathfinderMob implements RpgCombatDuelist, RangedAttackMob {

    private static final EntityDataAccessor<Integer> DATA_SKIN_VARIANT =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_COMBAT_COMBO =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_PARRYING =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ADVENTURER_LEVEL =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ADVENTURER_XP =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_RANK =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_HIRED =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AFFINITY =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_ETERNAL =
            SynchedEntityData.defineId(AdventurerNpcEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int TOTAL_VARIANTS = 3;

    // Tactical combat goals
    private final RpgTacticalBowGoal<AdventurerNpcEntity> bowGoal =
            new RpgTacticalBowGoal<>(this, 1.0D, 18, 16.0F);
    private final RpgDuelCombatGoal<AdventurerNpcEntity> meleeGoal =
            new RpgDuelCombatGoal<>(this, 1.25D);

    // Hiring & Companion status
    private UUID hiredOwnerUuid = null;
    private long hireExpiryGameTime = 0L;
    private boolean isStaying = false;
    private boolean isEternalComrade = false;
    private CompanionStance stance = CompanionStance.FOLLOW;
    private net.minecraft.core.BlockPos homeBasePos = null;

    // Progression: Level & Experience
    private int adventurerLevel = 1;
    private int adventurerXp = 0;

    // Social & Relationships
    private final Map<UUID, Integer> affinityMap = new HashMap<>();
    private final Map<UUID, Long> lastChatGameTime = new HashMap<>();

    // Combat tracking
    private long lastAttackGameTime = 0L;
    private int parryTicks = 0;

    // Companion Backpack & Autocare
    public static final int COMPANION_INV_SIZE = 18;
    private final SimpleContainer companionInventory = new SimpleContainer(COMPANION_INV_SIZE);
    private int healCooldownTicks = 0;
    private long lastBanterGameTime = 0L;
    private long lastOwnerDangerAlertGameTime = 0L;
    private int giftPersonalityId = 0;

    public int getGiftPersonalityId() {
        return this.giftPersonalityId;
    }

    public void setGiftPersonalityId(int id) {
        this.giftPersonalityId = Math.abs(id) % 5;
    }

    public AdventurerNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.reassessWeaponGoal();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isHired() && !this.isPersistenceRequired() && !this.hasCustomName();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN_VARIANT, 0);
        builder.define(DATA_COMBAT_COMBO, 0);
        builder.define(DATA_IS_PARRYING, false);
        builder.define(DATA_ADVENTURER_LEVEL, 1);
        builder.define(DATA_ADVENTURER_XP, 0);
        builder.define(DATA_RANK, 0);
        builder.define(DATA_IS_HIRED, false);
        builder.define(DATA_AFFINITY, 0);
        builder.define(DATA_IS_ETERNAL, false);
    }

    public int getSkinVariant() {
        return this.entityData.get(DATA_SKIN_VARIANT);
    }

    public void setSkinVariant(int variant) {
        this.entityData.set(DATA_SKIN_VARIANT, Math.max(0, variant % TOTAL_VARIANTS));
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
    protected void registerGoals() {
        // Basic survival
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // Companion escort & intelligent idle inspection (priority 1 when hired)
        this.goalSelector.addGoal(1, new AdventurerCompanionGoal(this));

        // Autonomous wander & look around when not following
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Targeting: Defend & attack with owner when hired
        this.targetSelector.addGoal(1, new CompanionDefendOwnerGoal(this));
        this.targetSelector.addGoal(2, new CompanionAttackTargetGoal(this));

        // Self-defense & target hostile monsters
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // High priority: Boss threat detection
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, UndeadAdventurerBossEntity.class, true));
        // Defend civilians (villagers and townswomen)
        this.targetSelector.addGoal(5, new DefendVillagersGoal(this));
        // High priority hunt on Bandits
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, BanditEntity.class, true));
        // General monsters
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, Monster.class, true));
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

        if (!this.level().isClientSide() && isHired()) {
            if (this.healCooldownTicks > 0) {
                this.healCooldownTicks--;
            }

            routineAutoHeal();
            routineCollectNearbyLoot();
            routineCompanionBanter();

            if (!this.isEternalComrade) {
                // Check contract expiration (1 Minecraft day per emerald = 24,000 ticks)
                if (this.level().getGameTime() >= hireExpiryGameTime) {
                    Player owner = getHiringPlayer();
                    if (owner != null) {
                        String name = hasCustomName() ? getCustomName().getString() : "Aventurero";
                        owner.sendSystemMessage(Component.literal("§e[" + name + "]: §f\"Mi contrato de escolta ha concluido, colega. ¡Fue un verdadero honor luchar a tu lado! Si me necesitas de nuevo, búscame.\""));
                    }
                    dismissContract();
                } else if (this.level().getGameTime() % 24000L == 0) {
                    // Periodic bond increase while adventuring together
                    Player owner = getHiringPlayer();
                    if (owner != null && this.distanceToSqr(owner) < 1024.0) {
                        addAffinity(owner, 5);
                    }
                }
            }

            // Stance: Return to home base if in base wander mode and wandered too far
            if (this.stance == CompanionStance.WANDER_BASE && this.homeBasePos != null) {
                if (this.tickCount % 40 == 0) {
                    double distSq = this.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(this.homeBasePos));
                    if (distSq > 256.0) { // More than 16 blocks away
                        this.getNavigation().moveTo(homeBasePos.getX() + 0.5, homeBasePos.getY(), homeBasePos.getZ() + 0.5, 1.0);
                    }
                }
            }
        }
    }

    public SimpleContainer getCompanionInventory() {
        return this.companionInventory;
    }

    public void openCompanionInventory(ServerPlayer player) {
        if (player == null || !this.isHiredBy(player)) return;
        String title = (this.hasCustomName() ? this.getCustomName().getString() : "Camarada");
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInv, p) -> new ChestMenu(MenuType.GENERIC_9x2, containerId, playerInv, this.companionInventory, 2),
                Component.literal("§6Mochila: §f" + title + " §7(Comida, Pociones, Botín)")
        ));
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    public void sayBanter(String text, boolean important) {
        if (this.level().isClientSide() || !this.isHired()) return;
        long now = this.level().getGameTime();
        if (!important && (now - lastBanterGameTime) < 600L) {
            return;
        }
        lastBanterGameTime = now;
        Player owner = getHiringPlayer();
        if (owner != null && this.distanceToSqr(owner) < 576.0) {
            String name = hasCustomName() ? getCustomName().getString() : "Camarada";
            owner.sendSystemMessage(Component.literal("§e[§6" + name + "§e]: §f\"" + text + "\""));
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.7F, 1.2F);
        }
    }

    private void routineAutoHeal() {
        if (!this.isAlive() || this.healCooldownTicks > 0) return;
        if (this.tickCount % 20 != 0) return;

        float maxHp = this.getMaxHealth();
        float currentHp = this.getHealth();
        float ratio = currentHp / maxHp;
        boolean hasHarmfulEffect = this.hasEffect(MobEffects.POISON) || this.hasEffect(MobEffects.WITHER);

        if (ratio >= 0.75f && !hasHarmfulEffect) {
            return;
        }

        // Caso Crítico (< 35% HP o efecto dañino severo): buscar Manzanas Doradas o Pociones Curativas
        if (ratio < 0.35f || hasHarmfulEffect) {
            // 1. Manzana Dorada Encantada
            int gappleSlot = findSlotWithItem(Items.ENCHANTED_GOLDEN_APPLE);
            if (gappleSlot >= 0) {
                consumeItemAt(gappleSlot);
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0));
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
                this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
                this.heal(8.0f);
                spawnHealEffects(true);
                sayBanter("¡Por poco! Comí una manzana dorada encantada para resistir la embestida.", true);
                this.healCooldownTicks = 60;
                return;
            }

            // 2. Manzana Dorada normal
            gappleSlot = findSlotWithItem(Items.GOLDEN_APPLE);
            if (gappleSlot >= 0) {
                consumeItemAt(gappleSlot);
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0));
                this.heal(4.0f);
                spawnHealEffects(true);
                sayBanter("¡Uf! Comí una manzana dorada de mi mochila para recuperar fuerzas.", false);
                this.healCooldownTicks = 60;
                return;
            }

            // 3. Poción curativa o de regeneración
            int potionSlot = findPotionSlot();
            if (potionSlot >= 0) {
                ItemStack potionStack = this.companionInventory.getItem(potionSlot);
                PotionContents contents = potionStack.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    for (MobEffectInstance effect : contents.getAllEffects()) {
                        this.addEffect(new MobEffectInstance(effect));
                    }
                }
                consumeItemAt(potionSlot);
                this.companionInventory.addItem(new ItemStack(Items.GLASS_BOTTLE));
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                spawnHealEffects(false);
                sayBanter("¡Bebí una poción de mi mochila! Ya me siento mucho mejor.", false);
                this.healCooldownTicks = 40;
                return;
            }
        }

        // Caso Moderado (< 70% HP): comer comida común
        int foodSlot = findFoodSlot();
        if (foodSlot >= 0) {
            ItemStack foodStack = this.companionInventory.getItem(foodSlot);
            FoodProperties food = foodStack.get(DataComponents.FOOD);
            if (food != null) {
                float healAmount = Math.max(3.0f, (float) food.nutrition() * 1.5f);
                this.heal(healAmount);
                for (FoodProperties.PossibleEffect pe : food.effects()) {
                    if (this.getRandom().nextFloat() < pe.probability()) {
                        this.addEffect(new MobEffectInstance(pe.effect()));
                    }
                }
                consumeItemAt(foodSlot);
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F);
                spawnHealEffects(false);
                if (this.getRandom().nextFloat() < 0.25f) {
                    sayBanter("Comiendo un buen bocado de mi mochila para recuperar energías...", false);
                }
                this.healCooldownTicks = 50;
            }
        }
    }

    private int findSlotWithItem(net.minecraft.world.item.Item item) {
        for (int i = 0; i < this.companionInventory.getContainerSize(); i++) {
            ItemStack s = this.companionInventory.getItem(i);
            if (!s.isEmpty() && s.is(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findPotionSlot() {
        for (int i = 0; i < this.companionInventory.getContainerSize(); i++) {
            ItemStack s = this.companionInventory.getItem(i);
            if (!s.isEmpty() && s.is(Items.POTION)) {
                PotionContents contents = s.get(DataComponents.POTION_CONTENTS);
                if (contents != null) {
                    for (MobEffectInstance effect : contents.getAllEffects()) {
                        if (effect.getEffect() == MobEffects.HEAL || effect.getEffect() == MobEffects.REGENERATION) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private int findFoodSlot() {
        for (int i = 0; i < this.companionInventory.getContainerSize(); i++) {
            ItemStack s = this.companionInventory.getItem(i);
            if (!s.isEmpty() && s.has(DataComponents.FOOD)) {
                if (!s.is(Items.ROTTEN_FLESH) && !s.is(Items.SPIDER_EYE) && !s.is(Items.POISONOUS_POTATO)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void consumeItemAt(int slot) {
        ItemStack s = this.companionInventory.getItem(slot);
        if (!s.isEmpty()) {
            s.shrink(1);
            if (s.isEmpty()) {
                this.companionInventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private void spawnHealEffects(boolean golden) {
        if (this.level() instanceof ServerLevel sl) {
            if (golden) {
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.2, this.getZ(), 16, 0.3, 0.4, 0.3, 0.1);
            } else {
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.3, 0.4, 0.3, 0.05);
            }
        }
    }

    private void routineCollectNearbyLoot() {
        if (!this.isHired() || this.isStaying() || this.getStance() != CompanionStance.FOLLOW) {
            return;
        }
        if (this.tickCount % 20 != 0) return;

        List<ItemEntity> items = this.level().getEntitiesOfClass(
                ItemEntity.class,
                this.getBoundingBox().inflate(2.5, 1.2, 2.5),
                item -> item.isAlive() && !item.hasPickUpDelay()
        );

        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) continue;
            int originalCount = stack.getCount();
            ItemStack leftover = this.companionInventory.addItem(stack);
            if (leftover.getCount() < originalCount) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4F, 1.3F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, itemEntity.getX(), itemEntity.getY() + 0.2, itemEntity.getZ(), 3, 0.1, 0.1, 0.1, 0.02);
                }
                if (leftover.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(leftover);
                }
            }
        }
    }

    private void routineCompanionBanter() {
        if (this.tickCount % 60 != 0) return;
        Player owner = getHiringPlayer();
        if (owner == null || !owner.isAlive() || this.distanceToSqr(owner) > 400.0) return;

        long now = this.level().getGameTime();

        // 1. Alerta de peligro si el dueño está con vida crítica (< 25%)
        if (owner.getHealth() < owner.getMaxHealth() * 0.25f && (now - lastOwnerDangerAlertGameTime) > 400L) {
            lastOwnerDangerAlertGameTime = now;
            sayBanter("¡" + owner.getName().getString() + ", estás malherido! ¡Retrocede, yo cubro tu espalda!", true);
            return;
        }

        // 2. Comentario de campamento / descanso si hay una fogata encendida cerca y no hay monstruos
        if ((now - lastBanterGameTime) > 1200L && this.getTarget() == null) {
            net.minecraft.core.BlockPos pos = this.blockPosition();
            for (net.minecraft.core.BlockPos near : net.minecraft.core.BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3))) {
                BlockState state = this.level().getBlockState(near);
                if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
                    sayBanter("Un fuego acogedor... justo lo que necesitábamos tras tanta marcha. Recuperemos el aliento aquí.", false);
                    return;
                }
            }

            // 3. Comentario de cueva oscura / subterráneo
            if (this.getY() < 40 && this.level().getBrightness(LightLayer.SKY, pos) == 0 && this.getRandom().nextFloat() < 0.2f) {
                sayBanter("La penumbra se vuelve espesa en estas profundidades... Mantengamos las antorchas en alto.", false);
                return;
            }
        }
    }

    public boolean isHired() {
        if (this.level().isClientSide()) {
            return (this.entityData != null) && (this.entityData.get(DATA_IS_HIRED) || this.entityData.get(DATA_IS_ETERNAL));
        }
        if (this.isEternalComrade) {
            return this.hiredOwnerUuid != null;
        }
        return hiredOwnerUuid != null && this.level().getGameTime() < hireExpiryGameTime;
    }

    public boolean isHiredBy(Player player) {
        return isHired() && player != null && player.getUUID().equals(hiredOwnerUuid);
    }

    public Player getHiringPlayer() {
        if (hiredOwnerUuid == null) return null;
        return this.level().getPlayerByUUID(hiredOwnerUuid);
    }

    public UUID getHiringPlayerUUID() {
        return this.hiredOwnerUuid;
    }

    public void hire(Player player, int days) {
        this.hiredOwnerUuid = player.getUUID();
        long now = this.level().getGameTime();
        long baseTime = Math.max(now, hireExpiryGameTime);
        this.hireExpiryGameTime = baseTime + (days * 24000L);
        this.isStaying = false;
        this.stance = CompanionStance.FOLLOW;
        if (this.entityData != null) {
            this.entityData.set(DATA_IS_HIRED, true);
        }
    }

    public void dismissContract() {
        this.hiredOwnerUuid = null;
        this.hireExpiryGameTime = 0L;
        this.isStaying = false;
        this.stance = CompanionStance.FOLLOW;
        if (this.entityData != null) {
            this.entityData.set(DATA_IS_HIRED, false);
        }
    }

    public boolean toggleStaying() {
        this.isStaying = !this.isStaying;
        if (this.isStaying) {
            this.stance = CompanionStance.STAY_GUARD;
            this.getNavigation().stop();
        } else {
            this.stance = CompanionStance.FOLLOW;
        }
        return this.isStaying;
    }

    public boolean isStaying() {
        if (this.isEternalComrade) {
            return this.stance != CompanionStance.FOLLOW;
        }
        return this.isStaying;
    }

    public CompanionStance getStance() {
        return this.stance;
    }

    public void setStance(CompanionStance newStance) {
        this.stance = newStance;
        if (newStance == CompanionStance.STAY_GUARD) {
            this.isStaying = true;
            this.getNavigation().stop();
        } else if (newStance == CompanionStance.WANDER_BASE) {
            this.isStaying = false;
            if (this.homeBasePos == null) {
                this.homeBasePos = this.blockPosition();
            }
        } else {
            this.isStaying = false;
        }
    }

    public net.minecraft.core.BlockPos getHomeBasePos() {
        return this.homeBasePos;
    }

    public void setHomeBasePos(net.minecraft.core.BlockPos pos) {
        this.homeBasePos = pos;
    }

    public boolean isEternalComrade() {
        if (this.level().isClientSide()) {
            return this.entityData != null && this.entityData.get(DATA_IS_ETERNAL);
        }
        return this.isEternalComrade;
    }

    public void setEternalComrade(boolean eternal) {
        this.isEternalComrade = eternal;
        if (this.entityData != null) {
            this.entityData.set(DATA_IS_ETERNAL, eternal);
            if (eternal) {
                this.entityData.set(DATA_IS_HIRED, true);
            }
        }
        if (eternal) {
            this.hireExpiryGameTime = Long.MAX_VALUE;
        }
    }

    public boolean hasPendingFriendshipEvent(Player player) {
        if (player == null) return false;
        return isHiredBy(player) && !isEternalComrade && getAffinity(player) >= 100;
    }

    public int getAdventurerLevel() {
        return this.entityData != null ? this.entityData.get(DATA_ADVENTURER_LEVEL) : this.adventurerLevel;
    }

    public void setAdventurerLevel(int level) {
        this.adventurerLevel = Math.max(1, level);
        if (this.entityData != null) {
            this.entityData.set(DATA_ADVENTURER_LEVEL, this.adventurerLevel);
        }
    }

    public int getAdventurerXp() {
        return this.entityData != null ? this.entityData.get(DATA_ADVENTURER_XP) : this.adventurerXp;
    }

    public void setAdventurerXp(int xp) {
        this.adventurerXp = Math.max(0, xp);
        if (this.entityData != null) {
            this.entityData.set(DATA_ADVENTURER_XP, this.adventurerXp);
        }
    }

    public int getRequiredXpForNextLevel() {
        return 40 + (this.getAdventurerLevel() * 20);
    }

    public boolean isReadyToAscend() {
        AdventurerRank current = getRank();
        if (current.isMaxRank()) return false;
        return this.getAdventurerLevel() >= 20;
    }

    public void addExperience(int amount) {
        if (amount <= 0) return;
        this.adventurerXp = getAdventurerXp() + amount;
        this.adventurerLevel = getAdventurerLevel();
        int req = getRequiredXpForNextLevel();

        while (this.adventurerXp >= req) {
            this.adventurerXp -= req;
            this.adventurerLevel++;

            // Al superar el nivel 20: ¡Ascensión de Rango automática con gran aumento de estadísticas!
            if (this.adventurerLevel > 20) {
                AdventurerRank currentRank = getRank();
                if (!currentRank.isMaxRank()) {
                    ascendRank();
                    this.adventurerLevel = 1;
                    setAdventurerLevel(this.adventurerLevel);
                    setAdventurerXp(this.adventurerXp);
                    req = getRequiredXpForNextLevel();
                    continue;
                } else {
                    this.adventurerLevel = 20;
                    this.adventurerXp = req - 1;
                    break;
                }
            }

            // Aplicar incremento de estadísticas por nivel dentro de su rango
            setAdventurerLevel(this.adventurerLevel);
            setAdventurerXp(this.adventurerXp);
            applyRankAttributes(getRank());
            req = getRequiredXpForNextLevel();

            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8F, 1.4F);
            }

            Player owner = getHiringPlayer();
            if (owner != null) {
                String name = hasCustomName() ? getCustomName().getString() : "Tu aventurero";
                owner.sendSystemMessage(Component.literal("§6[" + name + "] §a¡Ha subido al nivel " + this.adventurerLevel + "! §7(XP: " + this.adventurerXp + "/" + req + ")"));
            }
        }
        setAdventurerLevel(this.adventurerLevel);
        setAdventurerXp(this.adventurerXp);
    }

    @Override
    public void awardKillScore(net.minecraft.world.entity.Entity victim, int scoreValue, net.minecraft.world.damagesource.DamageSource damageSource) {
        super.awardKillScore(victim, scoreValue, damageSource);
        if (!this.level().isClientSide() && victim instanceof LivingEntity target) {
            int xpReward = (target instanceof Monster) ? 25 : 15;
            this.addExperience(xpReward);

            // Banter de victoria tras abatir a un enemigo imponente (Hazard o superior)
            ThreatLevel threat = ThreatLevel.determineThreatLevel(target);
            if (threat.ordinal() >= ThreatLevel.HAZARD.ordinal()) {
                sayBanter("¡Victoria! Esa bestia de rango " + threat.getId() + " fue un verdadero desafío. ¡Excelente trabajo en equipo!", true);
            }
        }
    }

    public void ascendRank() {
        AdventurerRank current = getRank();
        if (current.isMaxRank()) return;
        AdventurerRank next = current.getNextRank();
        setRank(next);
        applyRankAttributes(next);
        populateDefaultEquipmentSlots(this.level().getRandom(), this.level().getCurrentDifficultyAt(this.blockPosition()));

        // Curar por completo al ascender
        this.setHealth(this.getMaxHealth());

        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0, this.getZ(), 35, 0.4, 0.6, 0.4, 0.15);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2, this.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2F, 1.0F);
        }

        Player owner = getHiringPlayer();
        if (owner != null) {
            String name = hasCustomName() ? getCustomName().getString() : "Tu aventurero";
            owner.sendSystemMessage(Component.literal("§6⚡ [" + name + "] §e¡Ha superado el nivel 20 y ha ascendido formalmente a ").append(next.getBadge()).append("§e!"));
            owner.sendSystemMessage(Component.literal("§aSus atributos de salud, armadura y daño han aumentado notablemente."));
        }
    }

    public long getHireRemainingTicks(long currentGameTime) {
        if (!isHired()) return 0L;
        return Math.max(0L, hireExpiryGameTime - currentGameTime);
    }

    public int getAffinity(Player player) {
        if (player == null) return 0;
        if (this.level().isClientSide()) {
            return this.entityData != null ? this.entityData.get(DATA_AFFINITY) : 0;
        }
        return affinityMap.getOrDefault(player.getUUID(), 0);
    }

    public void addAffinity(Player player, int amount) {
        if (player == null) return;
        int current = getAffinity(player);
        int newScore = Math.max(0, Math.min(100, current + amount));
        affinityMap.put(player.getUUID(), newScore);
        if (this.entityData != null) {
            this.entityData.set(DATA_AFFINITY, newScore);
        }
        if (newScore >= 50 && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.pablo.rpglevels.advancements.ModAdvancements.award(sp, com.pablo.rpglevels.advancements.ModAdvancements.HIGH_AFFINITY);
        }
    }

    public boolean canChat(Player player) {
        if (player == null) return false;
        long last = lastChatGameTime.getOrDefault(player.getUUID(), -24000L);
        // Cooldown: 2 minutes (2400 ticks)
        return (this.level().getGameTime() - last) >= 2400L;
    }

    public void recordChat(Player player) {
        if (player != null) {
            lastChatGameTime.put(player.getUUID(), this.level().getGameTime());
        }
    }

    public AdventurerRank getRank() {
        if (this.entityData != null) {
            int rankIdx = this.entityData.get(DATA_RANK);
            if (rankIdx >= 0 && rankIdx < AdventurerRank.values().length) {
                return AdventurerRank.fromIndex(rankIdx);
            }
        }
        int rankIndex = this.hasData(ModAttachments.MOB_RANK) ? this.getData(ModAttachments.MOB_RANK) : 0;
        return AdventurerRank.fromIndex(rankIndex);
    }

    public static AdventurerRank rollRandomRank(RandomSource random) {
        int roll = random.nextInt(100);
        if (roll < 35) return AdventurerRank.F;       // 35% Novatos
        if (roll < 65) return AdventurerRank.E;       // 30% Iniciados
        if (roll < 83) return AdventurerRank.D;       // 18% Competentes
        if (roll < 93) return AdventurerRank.C;       // 10% Experimentados
        if (roll < 98) return AdventurerRank.B;       // 5% Veteranos
        return AdventurerRank.A;                      // 2% Élite Legendaria
    }

    public void setRank(AdventurerRank rank) {
        this.setData(ModAttachments.MOB_RANK, rank.getIndex());
        if (this.entityData != null) {
            this.entityData.set(DATA_RANK, rank.getIndex());
        }
        applyRankAttributes(rank);
    }

    public void applyRankAttributes(AdventurerRank rank) {
        double maxHp = switch (rank) {
            case F -> 30.0;
            case E -> 45.0;
            case D -> 65.0;
            case C -> 90.0;
            case B -> 130.0;
            case A -> 180.0;
        };
        double attack = switch (rank) {
            case F -> 4.0;
            case E -> 5.5;
            case D -> 7.0;
            case C -> 9.0;
            case B -> 12.0;
            case A -> 15.0;
        };
        double armor = switch (rank) {
            case F -> 4.0;
            case E -> 7.0;
            case D -> 10.0;
            case C -> 14.0;
            case B -> 18.0;
            case A -> 20.0;
        };
        double speed = switch (rank) {
            case F -> 0.26;
            case E -> 0.27;
            case D -> 0.28;
            case C -> 0.29;
            case B -> 0.30;
            case A -> 0.32;
        };

        // Escala adicional por nivel dentro del rango (1 al 20)
        double levelBonusHp = Math.max(0, this.adventurerLevel - 1) * 1.5;
        double levelBonusAtk = Math.max(0, this.adventurerLevel - 1) * 0.25;

        var hpInst = this.getAttribute(Attributes.MAX_HEALTH);
        if (hpInst != null) {
            hpInst.setBaseValue(maxHp + levelBonusHp);
        }
        var dmgInst = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgInst != null) dmgInst.setBaseValue(attack + levelBonusAtk);
        var armInst = this.getAttribute(Attributes.ARMOR);
        if (armInst != null) armInst.setBaseValue(armor);
        var spdInst = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spdInst != null) spdInst.setBaseValue(speed);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        RandomSource random = level.getRandom();

        // 1. Asignar Rango y Estadísticas de combate escaladas
        if (!this.hasData(ModAttachments.MOB_RANK)) {
            AdventurerRank rank = rollRandomRank(random);
            this.setData(ModAttachments.MOB_RANK, rank.getIndex());

            // Nivel inicial aleatorio del 1 al 20
            setAdventurerLevel(1 + random.nextInt(20));
            setAdventurerXp(0);
            applyRankAttributes(rank);
            this.setHealth(this.getMaxHealth());
        } else {
            if (getAdventurerLevel() < 1 || getAdventurerLevel() > 20) {
                setAdventurerLevel(1 + random.nextInt(20));
            }
            applyRankAttributes(getRank());
        }

        // 2. Variante de skin aleatoria y personalidad de gustos
        setSkinVariant(random.nextInt(TOTAL_VARIANTS));
        setGiftPersonalityId(random.nextInt(5));

        // 3. Equipamiento acorde al rango
        populateDefaultEquipmentSlots(random, difficulty);

        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        AdventurerRank rank = getRank();
        boolean isArcher = random.nextFloat() < 0.25f;

        if (isArcher) {
            ItemStack bow = switch (rank) {
                case A, B -> new ItemStack(Items.BOW);
                case C, D -> new ItemStack(Items.CROSSBOW);
                default -> new ItemStack(Items.BOW);
            };
            this.setItemSlot(EquipmentSlot.MAINHAND, bow);
        } else {
            ItemStack melee = switch (rank) {
                case A -> new ItemStack(Items.NETHERITE_SWORD);
                case B -> new ItemStack(Items.DIAMOND_SWORD);
                case C -> random.nextBoolean() ? new ItemStack(Items.DIAMOND_SWORD) : new ItemStack(Items.IRON_AXE);
                case D -> new ItemStack(Items.IRON_SWORD);
                case E -> random.nextBoolean() ? new ItemStack(Items.IRON_SWORD) : new ItemStack(Items.IRON_AXE);
                default -> random.nextBoolean() ? new ItemStack(Items.STONE_SWORD) : new ItemStack(Items.IRON_AXE);
            };
            this.setItemSlot(EquipmentSlot.MAINHAND, melee);

            if (rank == AdventurerRank.A || rank == AdventurerRank.B || rank == AdventurerRank.C) {
                this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                this.setDropChance(EquipmentSlot.OFFHAND, 0.05F);
            }
        }

        // Armaduras acordes al rango
        switch (rank) {
            case A -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case B -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            }
            case C -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
            case D -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            }
            case E -> {
                this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
                this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            }
            default -> { // F
                if (random.nextBoolean()) {
                    this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
                }
                this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            }
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.04F);
        }
        reassessWeaponGoal();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // Shift + Clic Derecho: Abrir directamente la mochila del camarada
        if (player.isShiftKeyDown() && this.isHiredBy(player)) {
            if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                openCompanionInventory(serverPlayer);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            String npcName = this.hasCustomName() ? this.getCustomName().getString() : "Aventurero";
            AdventurerRank rank = getRank();
            int affinity = getAffinity(player);
            long remainingTicks = getHireRemainingTicks(this.level().getGameTime());

            String greeting = NpcSocialManager.getGreeting(
                    "adventurer", affinity, rank, npcName, player.getName().getString(),
                    isHired(), isStaying()
            );

            // Open MCA-style dialogue screen
            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    npcName,
                    "adventurer",
                    this.getSkinVariant(),
                    rank.getIndex(),
                    affinity,
                    this.isHired(),
                    this.isHiredBy(player),
                    remainingTicks,
                    this.isStaying(),
                    0,
                    false,
                    0,
                    this.isEternalComrade(),
                    this.getStance().getId(),
                    this.isReadyToAscend(),
                    greeting
            ));

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SkinVariant", getSkinVariant());
        compound.putInt("GiftPersonality", this.giftPersonalityId);

        compound.putInt("AdventurerLevel", adventurerLevel);
        compound.putInt("AdventurerXp", adventurerXp);
        compound.putBoolean("IsEternalComrade", isEternalComrade);
        compound.putInt("Stance", stance.getId());
        if (homeBasePos != null) {
            compound.putInt("HomeBaseX", homeBasePos.getX());
            compound.putInt("HomeBaseY", homeBasePos.getY());
            compound.putInt("HomeBaseZ", homeBasePos.getZ());
        }

        if (hiredOwnerUuid != null) {
            compound.putUUID("HiredOwner", hiredOwnerUuid);
            compound.putLong("HireExpiry", hireExpiryGameTime);
            compound.putBoolean("IsStaying", isStaying);
        }

        // Affinities
        CompoundTag affTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : affinityMap.entrySet()) {
            affTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        compound.put("Affinities", affTag);

        // Companion Backpack inventory (18 slots)
        ListTag invList = new ListTag();
        for (int i = 0; i < companionInventory.getContainerSize(); i++) {
            ItemStack stack = companionInventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                invList.add(stack.save(this.registryAccess(), itemTag));
            }
        }
        compound.put("CompanionInventory", invList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SkinVariant")) {
            setSkinVariant(compound.getInt("SkinVariant"));
        }
        if (compound.contains("GiftPersonality")) {
            setGiftPersonalityId(compound.getInt("GiftPersonality"));
        }

        if (compound.contains("AdventurerLevel")) {
            setAdventurerLevel(compound.getInt("AdventurerLevel"));
        }
        if (compound.contains("AdventurerXp")) {
            setAdventurerXp(compound.getInt("AdventurerXp"));
        }
        if (compound.contains("IsEternalComrade")) {
            setEternalComrade(compound.getBoolean("IsEternalComrade"));
        }
        if (compound.contains("Stance")) {
            this.stance = CompanionStance.fromId(compound.getInt("Stance"));
        }
        if (compound.contains("HomeBaseX")) {
            this.homeBasePos = new net.minecraft.core.BlockPos(
                    compound.getInt("HomeBaseX"),
                    compound.getInt("HomeBaseY"),
                    compound.getInt("HomeBaseZ")
            );
        }

        if (compound.hasUUID("HiredOwner")) {
            this.hiredOwnerUuid = compound.getUUID("HiredOwner");
            this.hireExpiryGameTime = compound.getLong("HireExpiry");
            this.isStaying = compound.getBoolean("IsStaying");
            if (this.entityData != null) {
                this.entityData.set(DATA_IS_HIRED, this.isHired());
            }
        }
        if (this.entityData != null) {
            this.entityData.set(DATA_RANK, getRank().getIndex());
        }

        if (compound.contains("Affinities", Tag.TAG_COMPOUND)) {
            CompoundTag affTag = compound.getCompound("Affinities");
            for (String key : affTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    affinityMap.put(uuid, affTag.getInt(key));
                } catch (Exception ignored) {}
            }
        }

        // Companion Backpack inventory load
        if (compound.contains("CompanionInventory", Tag.TAG_LIST)) {
            ListTag invList = compound.getList("CompanionInventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < invList.size(); i++) {
                CompoundTag itemTag = invList.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot < companionInventory.getContainerSize()) {
                    companionInventory.setItem(slot, ItemStack.parse(this.registryAccess(), itemTag).orElse(ItemStack.EMPTY));
                }
            }
        }
        reassessWeaponGoal();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        // Drop backpack contents upon death so player doesn't lose items
        for (int i = 0; i < this.companionInventory.getContainerSize(); i++) {
            ItemStack stack = this.companionInventory.getItem(i);
            if (!stack.isEmpty()) {
                this.spawnAtLocation(stack);
                this.companionInventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack bowStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem || item instanceof CrossbowItem));
        ItemStack arrowStack = this.getProjectile(bowStack.isEmpty() ? new ItemStack(Items.BOW) : bowStack);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, arrowStack, distanceFactor, bowStack);

        double dx = target.getX() - this.getX();
        double dy = target.getY(0.3333333333333333D) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        arrow.shoot(dx, dy + dist * 0.18D, dz, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }
}
