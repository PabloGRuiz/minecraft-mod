package com.pablo.tenseicraft.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Cinnamoroll: Cachorrito blanco nacido en las nubes (Café Cinnamon).
 * - Camina tiernamente por el suelo dando pasitos cortos y rebotando sus largas orejas.
 * - Planea suavemente batiendo sus orejas al saltar o caer desde lugares altos (sin daño por caída).
 * - Tímido, dulce y pacífico: se domestica con galletas, azúcar, pasteles, leche, miel o bayas dulces.
 * - Acariciarlo (Click Der con mano vacía) lo hace levitar un poquito, girar feliz y otorga un aura reconfortante.
 * - Agacharse + Click Der (domesticado) lo hace descansar/dormir la siesta.
 * - Protege pasivamente a su dueño: lo atrapa con Caída Lenta de nube si cae desde alto y lo cura suavemente.
 */
public class CinnamorollEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> HAPPY_SPIN =
            SynchedEntityData.defineId(CinnamorollEntity.class, EntityDataSerializers.BOOLEAN);

    private int happySpinTicks = 0;
    private int healOwnerCooldown = 120;
    private int cloudRescueCooldown = 0;
    private long lastPetGameTime = 0L;

    public CinnamorollEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HAPPY_SPIN, false);
    }

    public boolean isHappySpinning() {
        return this.entityData.get(HAPPY_SPIN);
    }

    public void triggerHappySpin(int ticks) {
        this.happySpinTicks = Math.max(this.happySpinTicks, ticks);
        this.entityData.set(HAPPY_SPIN, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.35D));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.15D, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.25D, 5.0F, 2.2F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false; // Planea con sus orejitas, nunca sufre daño por caída
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Sin daño por caída
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.COOKIE)
                || stack.is(Items.SUGAR)
                || stack.is(Items.CAKE)
                || stack.is(Items.PUMPKIN_PIE)
                || stack.is(Items.MILK_BUCKET)
                || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.HONEY_BOTTLE);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Planeo suave con sus orejas al saltar o caer desde un bloque alto
        Vec3 vel = this.getDeltaMovement();
        if (!this.onGround() && vel.y < -0.08) {
            this.setDeltaMovement(vel.x, Math.max(vel.y * 0.65, -0.08), vel.z);
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel sl) {
            // Temporizador de giro feliz
            if (this.happySpinTicks > 0) {
                this.happySpinTicks--;
                if (this.tickCount % 3 == 0) {
                    sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.9, this.getZ(), 1, 0.3, 0.25, 0.3, 0.02);
                    sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.25, this.getZ(), 2, 0.3, 0.15, 0.3, 0.02);
                }
                if (this.happySpinTicks <= 0) {
                    this.entityData.set(HAPPY_SPIN, false);
                }
            }

            // Nubecitas sutiles al caminar o planear
            if (!this.onGround() && this.tickCount % 4 == 0) {
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.15, this.getZ(), 1, 0.2, 0.08, 0.2, 0.01);
            } else if (!this.isInSittingPose() && this.tickCount % 26 == 0) {
                if (this.random.nextFloat() < 0.5f) {
                    sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.45, this.getZ(), 1, 0.25, 0.2, 0.25, 0.006);
                }
            } else if (this.isInSittingPose() && this.tickCount % 36 == 0) {
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.7, this.getZ(), 1, 0.12, 0.08, 0.12, 0.005);
            }

            // Habilidades dulces de apoyo para su dueño (cuando no está sentado)
            LivingEntity owner = this.getOwner();
            if (this.isTame() && !this.isInSittingPose() && owner instanceof Player ownerPlayer && ownerPlayer.isAlive()
                    && ownerPlayer.distanceToSqr(this) <= 225.0D) {

                // 1. Rescate de Nube: si el dueño cae desde alto, Cinnamoroll lo envuelve en Caída Lenta
                if (this.cloudRescueCooldown > 0) {
                    this.cloudRescueCooldown--;
                } else if (ownerPlayer.fallDistance > 4.5F && !ownerPlayer.hasEffect(MobEffects.SLOW_FALLING)) {
                    ownerPlayer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
                    this.cloudRescueCooldown = 160;
                    this.triggerHappySpin(28);
                    sl.playSound(null, ownerPlayer.getX(), ownerPlayer.getY(), ownerPlayer.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.9F, 1.6F);
                    sl.sendParticles(ParticleTypes.CLOUD, ownerPlayer.getX(), ownerPlayer.getY() + 0.2, ownerPlayer.getZ(),
                            18, 0.5, 0.2, 0.5, 0.04);
                    ownerPlayer.displayClientMessage(Component.literal("§b☁ §fCinnamoroll §7te atrapó suavemente con una nube."), true);
                }

                // 2. Aroma de Café Cinnamon: cura suavemente al dueño si está herido
                if (this.healOwnerCooldown > 0) {
                    this.healOwnerCooldown--;
                } else if (ownerPlayer.getHealth() < ownerPlayer.getMaxHealth() * 0.78F) {
                    ownerPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false, true));
                    this.healOwnerCooldown = 360;
                    this.triggerHappySpin(24);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 0.85F, 1.35F);
                    sl.sendParticles(ParticleTypes.HEART, ownerPlayer.getX(), ownerPlayer.getY() + 1.2, ownerPlayer.getZ(),
                            5, 0.4, 0.3, 0.4, 0.03);
                    sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5, this.getZ(),
                            10, 0.4, 0.3, 0.4, 0.03);
                }
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (this.level().isClientSide()) {
            boolean willInteract = this.isFood(stack) || (this.isTame() && this.isOwnedBy(player)) || stack.isEmpty();
            return willInteract ? InteractionResult.CONSUME : super.mobInteract(player, hand);
        }

        ServerLevel sl = (ServerLevel) this.level();

        // 1. Alimentar o domesticar con dulces (galletas, azúcar, pastel, leche, bayas)
        if (this.isFood(stack)) {
            if (!player.getAbilities().instabuild) {
                if (stack.is(Items.MILK_BUCKET)) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.BUCKET));
                } else if (stack.is(Items.HONEY_BOTTLE)) {
                    stack.shrink(1);
                    player.addItem(new ItemStack(Items.GLASS_BOTTLE));
                } else {
                    stack.shrink(1);
                }
            }

            if (!this.isTame()) {
                this.tame(player);
                this.setOrderedToSit(false);
                this.triggerHappySpin(45);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 1.0F, 1.3F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.5F);
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.9, this.getZ(), 12, 0.45, 0.4, 0.45, 0.05);
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5, this.getZ(), 16, 0.5, 0.3, 0.5, 0.04);
                player.displayClientMessage(Component.literal("§b☁ §f¡Cinnamoroll §7adoró el dulce y ahora camina feliz a tu lado!"), true);
            } else {
                this.heal(8.0F);
                this.triggerHappySpin(36);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.85F, 1.4F);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 0.9F, 1.45F);
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.9, this.getZ(), 8, 0.35, 0.35, 0.35, 0.04);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, false, true));
                player.displayClientMessage(Component.literal("§b☁ §fCinnamoroll §7bate sus orejitas muy feliz por el dulce."), true);
            }
            return InteractionResult.SUCCESS;
        }

        // 2. Agacharse + Click Der (dueño): Sentarse / Dormir la siesta vs. Caminar de nuevo
        if (this.isTame() && this.isOwnedBy(player) && player.isShiftKeyDown()) {
            boolean nextSit = !this.isOrderedToSit();
            this.setOrderedToSit(nextSit);
            this.jumping = false;
            this.navigation.stop();
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.NEUTRAL, 0.8F, nextSit ? 1.05F : 1.45F);
            sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.5, this.getZ(), 8, 0.3, 0.2, 0.3, 0.02);
            player.displayClientMessage(Component.literal(nextSit
                    ? "§b☁ §fCinnamoroll §7se acurruca a tomar una siesta."
                    : "§b☁ §fCinnamoroll §7se levanta feliz y vuelve a caminar contigo."), true);
            return InteractionResult.SUCCESS;
        }

        // 3. Click Der con mano vacía: ¡Acariciar a Cinnamoroll!
        if (stack.isEmpty()) {
            long now = this.level().getGameTime();
            if (now - this.lastPetGameTime >= 20L) {
                this.lastPetGameTime = now;
                this.triggerHappySpin(30);
                sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.NEUTRAL, 0.85F, 1.5F);
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.9, this.getZ(), 6, 0.35, 0.3, 0.35, 0.03);
                sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.4, this.getZ(), 8, 0.35, 0.2, 0.35, 0.02);
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false, true));
                player.displayClientMessage(Component.literal("§b☁ §7Acariciaste a §fCinnamoroll§7... ¡Se siente tan suave como una nube!"), true);
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.35F + (this.random.nextFloat() - 0.5F) * 0.2F;
    }
}
