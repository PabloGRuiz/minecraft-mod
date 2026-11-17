package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Brambi: Venado Guardián de la Tierra & Montura del Bosque.
 * - Personalidad: Combatiente terrestre, arrollador y montura ágil para el jugador.
 * - Ataque 1 (Cornada Arrolladora): Embestida física con gran retroceso.
 * - Ataque 2 (Pisotón Sísmico): Se alza en dos patas y golpea el suelo aturdiendo a enemigos con Lentitud IV.
 * - Utilidad (Hurgador del Bosque): Escarba periódicamente la tierra buscando bayas y vegetales para su dueño.
 * - Fertilidad Viva: Acelera el crecimiento de cultivos cercanos a su paso.
 * - Montura: Puede ser montado por su dueño para correr velozmente por el bosque y saltar obstáculos.
 */
public class BrambiEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> STOMPING =
            SynchedEntityData.defineId(BrambiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FORAGING =
            SynchedEntityData.defineId(BrambiEntity.class, EntityDataSerializers.BOOLEAN);

    private int stompCooldownTicks = 200;
    private int foragerTimerTicks = 1200; // Cada ~60s busca un regalito
    private int stompingAnimationTicks = 0;
    private int foragingAnimationTicks = 0;

    public BrambiEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STOMPING, false);
        builder.define(FORAGING, false);
    }

    public boolean isStomping() {
        return this.entityData.get(STOMPING);
    }

    public void setStomping(boolean stomping) {
        this.entityData.set(STOMPING, stomping);
    }

    public boolean isForaging() {
        return this.entityData.get(FORAGING);
    }

    public void setForaging(boolean foraging) {
        this.entityData.set(FORAGING, foraging);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D) // 18 corazones base
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 8.5D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D)
                .add(Attributes.STEP_HEIGHT, 1.0D); // Sube 1 bloque sin saltar
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.42F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.30D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.30D, 7.0F, 2.5F));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.15D, this::isFood, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    public void updateStatsFromOwner(Player owner) {
        if (owner == null) return;
        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;

        var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(36.0D + (spirit * 0.6D));
        }
        var attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(8.5D + (spirit * 0.35D));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Partículas sutiles de hojas/tierra al caminar
        if (this.level().isClientSide() && this.getDeltaMovement().horizontalDistanceSqr() > 0.01D && this.random.nextFloat() < 0.25F) {
            this.level().addParticle(ParticleTypes.COMPOSTER,
                    this.getRandomX(0.6D), this.getY() + 0.1D, this.getRandomZ(0.6D),
                    0.0D, 0.05D, 0.0D);
        }

        // Animación de Pisotón Sísmico
        if (stompingAnimationTicks > 0) {
            stompingAnimationTicks--;
            if (stompingAnimationTicks <= 0 && isStomping()) {
                setStomping(false);
            }
        }

        // Animación de Hurgador
        if (foragingAnimationTicks > 0) {
            foragingAnimationTicks--;
            if (foragingAnimationTicks <= 0 && isForaging()) {
                setForaging(false);
            }
        }

        if (!this.level().isClientSide() && this.isAlive() && this.isTame()) {
            if (stompCooldownTicks > 0) stompCooldownTicks--;
            if (foragerTimerTicks > 0) foragerTimerTicks--;

            LivingEntity owner = this.getOwner();
            if (owner instanceof ServerPlayer playerOwner && playerOwner.isAlive()) {
                if (this.tickCount % 200 == 0) {
                    updateStatsFromOwner(playerOwner);
                }

                if (!this.isOrderedToSit() && this.getPassengers().isEmpty()) {
                    // Pisotón Sísmico en combate si hay monstruos cerca (Cooldown: 15-20s)
                    if (stompCooldownTicks <= 0 && isOwnerOrSelfInCombat(playerOwner)) {
                        List<Monster> hostiles = this.level().getEntitiesOfClass(Monster.class,
                                this.getBoundingBox().inflate(5.0D), Monster::isAlive);
                        if (!hostiles.isEmpty()) {
                            executeEarthTremor(hostiles, playerOwner);
                            this.stompCooldownTicks = 400; // 20 segundos
                        }
                    }

                    // Hurgador de la naturaleza (fuera de combate)
                    if (foragerTimerTicks <= 0 && !isOwnerOrSelfInCombat(playerOwner)) {
                        attemptForage(playerOwner);
                        this.foragerTimerTicks = 1400; // ~70s
                    }
                }
            }

            // Fertilidad viva: Acelera cultivos cercanos suavemente cada 160 ticks
            if (this.tickCount % 160 == 0 && this.onGround()) {
                accelerateSurroundingCrops();
            }
        }
    }

    /**
     * Pisotón Sísmico: Se alza y golpea el suelo provocando una onda expansiva de tierra y aturdimiento.
     */
    private void executeEarthTremor(List<Monster> targets, ServerPlayer owner) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        this.stompingAnimationTicks = 30;
        this.setStomping(true);

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_STUNNED, SoundSource.NEUTRAL, 1.2F, 1.2F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ROOTED_DIRT_BREAK, SoundSource.NEUTRAL, 1.4F, 0.9F);

        // Ondas de partículas de tierra desprendida
        BlockParticleOption dirtOption = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState());
        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0D) * Math.PI * 2;
            double vx = Math.cos(angle) * 0.45D;
            double vz = Math.sin(angle) * 0.45D;
            serverLevel.sendParticles(dirtOption, this.getX(), this.getY() + 0.1D, this.getZ(),
                    1, vx, 0.25D, vz, 0.15D);
            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.COMPOSTER, this.getX(), this.getY() + 0.2D, this.getZ(),
                        1, vx * 0.7D, 0.15D, vz * 0.7D, 0.08D);
            }
        }

        DamageSource damageSource = this.damageSources().mobAttack(this);
        for (Monster m : targets) {
            m.hurt(damageSource, 8.5F);
            // Empuja hacia arriba y aturde fuertemente
            m.setDeltaMovement(m.getDeltaMovement().add(0, 0.45D, 0));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, false, true));
            m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, true));
        }

        owner.displayClientMessage(
                Component.literal("§2[🦌] §f¡Brambi ejecuta un §6Pisotón Sísmico §fque aturde a los enemigos!"),
                true
        );
    }

    /**
     * Hurgador: Escarba la tierra buscando bayas, frutos o zanahorias para su dueño.
     */
    private void attemptForage(ServerPlayer owner) {
        BlockPos posBelow = this.blockPosition().below();
        BlockState stateBelow = this.level().getBlockState(posBelow);

        if (stateBelow.is(Blocks.GRASS_BLOCK) || stateBelow.is(Blocks.DIRT) || stateBelow.is(Blocks.PODZOL)) {
            this.foragingAnimationTicks = 45;
            this.setForaging(true);

            this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 1.2F);
            this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.1F);

            ItemStack gift = switch (this.random.nextInt(6)) {
                case 0 -> new ItemStack(Items.SWEET_BERRIES, 2 + this.random.nextInt(3));
                case 1 -> new ItemStack(Items.GLOW_BERRIES, 1 + this.random.nextInt(2));
                case 2 -> new ItemStack(Items.APPLE, 1);
                case 3 -> new ItemStack(Items.CARROT, 2);
                case 4 -> new ItemStack(Items.WHEAT, 2);
                default -> new ItemStack(Items.GOLDEN_CARROT, 1);
            };

            this.spawnAtLocation(gift);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 0.3D, this.getZ(),
                        8, 0.3D, 0.2D, 0.3D, 0.05D);
            }

            owner.displayClientMessage(
                    Component.literal("§a[🦌] §fBrambi escarbó la hierba y desenterró un manjar: §e" + gift.getHoverName().getString()),
                    true
            );
        }
    }

    private void accelerateSurroundingCrops() {
        BlockPos current = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(current.offset(-3, -1, -3), current.offset(3, 1, 3))) {
            BlockState st = this.level().getBlockState(p);
            if (st.getBlock() instanceof BonemealableBlock bonemealable && this.level() instanceof ServerLevel sl) {
                if (bonemealable.isValidBonemealTarget(sl, p, st)) {
                    bonemealable.performBonemeal(sl, sl.random, p, st);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                            3, 0.2D, 0.2D, 0.2D, 0.02D);
                    break;
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            // Embestida de astas: gran empuje físico
            Vec3 knockbackDir = living.position().subtract(this.position()).normalize().scale(0.8D);
            living.setDeltaMovement(living.getDeltaMovement().add(knockbackDir.x, 0.2D, knockbackDir.z));
            this.playSound(SoundEvents.RAVAGER_ATTACK, 1.0F, 1.6F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.1D);
            }
        }
        return hurt;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 1. Alimentar / Domesticar
        if (this.isFood(itemstack)) {
            if (!this.level().isClientSide()) {
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }

                if (!this.isTame()) {
                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        updateStatsFromOwner(player);

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                    this.getX(), this.getY() + 1.0D, this.getZ(),
                                    16, 0.4D, 0.4D, 0.4D, 0.1D);
                        }
                        this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);
                        player.displayClientMessage(
                                Component.literal("§2[🦌] §a¡Brambi confía en ti y te permitirá cabalgarlo y luchar a tu lado!"),
                                false
                        );
                    } else {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SMOKE,
                                    this.getX(), this.getY() + 0.8D, this.getZ(),
                                    6, 0.2D, 0.2D, 0.2D, 0.02D);
                        }
                        this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.2F);
                    }
                } else {
                    // Alimentar: sana y da vigor
                    this.heal(8.0F);
                    this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.3F);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.6F);

                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1, false, false, true));
                    this.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 1, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0, false, false, true));

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + 1.0D, this.getZ(),
                                8, 0.3D, 0.3D, 0.3D, 0.1D);
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 2. Dueño agachado + clic derecho -> Ordenar Sentarse / Quedarse
        if (this.isOwnedBy(player) && player.isShiftKeyDown() && !this.level().isClientSide()) {
            this.setOrderedToSit(!this.isOrderedToSit());
            this.jumping = false;
            this.navigation.stop();
            this.playSound(SoundEvents.CAMEL_AMBIENT, 1.0F, 1.4F);
            player.displayClientMessage(
                    Component.literal("§e[Brambi] " + (this.isOrderedToSit() ?
                            "§7Se echa pacíficamente en la hierba a montar guardia." :
                            "§a¡Se pone en pie con brío y vigor!")),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 3. Montar a Brambi: Dueño con mano vacía sin agacharse cuando no está sentado
        if (this.isOwnedBy(player) && !this.isOrderedToSit() && itemstack.isEmpty() && !player.isPassenger()) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
                this.playSound(SoundEvents.HORSE_ARMOR, 1.0F, 1.4F);
                player.displayClientMessage(
                        Component.literal("§2[🦌] §a¡Cabalgas sobre Brambi! (Usa SHIFT para desmontar)"),
                        true
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player && this.isOwnedBy(player)) {
            return player;
        }
        return null;
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        float f = player.xxa * 0.5F;
        float f1 = player.zza;
        if (f1 <= 0.0F) {
            f1 *= 0.25F;
        }
        return new Vec3(f, 0.0D, f1);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.15F;
    }

    private boolean isOwnerOrSelfInCombat(Player owner) {
        long gameTime = owner.level().getGameTime();
        if (owner.getLastHurtByMob() != null && gameTime - owner.getLastHurtByMobTimestamp() < 140L) {
            return true;
        }
        if (owner.getLastHurtMob() != null && gameTime - owner.getLastHurtMobTimestamp() < 140L) {
            return true;
        }
        if (this.getLastHurtByMob() != null && gameTime - this.getLastHurtByMobTimestamp() < 140L) {
            return true;
        }
        List<Monster> hostiles = this.level().getEntitiesOfClass(Monster.class,
                this.getBoundingBox().inflate(10.0D), Monster::isAlive);
        return !hostiles.isEmpty();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT)
                || stack.is(Items.CARROT)
                || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.APPLE)
                || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.HAY_BLOCK);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CAMEL_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.CAMEL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CAMEL_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.45F;
    }
}
