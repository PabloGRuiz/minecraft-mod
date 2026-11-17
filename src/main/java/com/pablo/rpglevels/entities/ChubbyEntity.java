package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.party.PartyManager;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Chubby: Nubecita de Tormenta Espiritual.
 * - Flota y vuela grácilmente alrededor de su dueño como un espíritu meteorológico.
 * - Ataque Básico (Chispa Voltaica): Dispara relámpagos tácticos a distancia contra enemigos.
 * - Habilidad 1 (Rayo Devastador): Canaliza una sobrecarga estática e invoca un auténtico rayo del cielo.
 * - Habilidad 2 (Lluvia Extintora): Si el dueño o aliados arden en llamas, Chubby vierte una lluvia protectora.
 * - Interactivo: Se alimenta de amatistas, redstone y polvos celestiales; ronronea con truenos lejanos.
 */
public class ChubbyEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> CHARGING_LIGHTNING =
            SynchedEntityData.defineId(ChubbyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EXCITED_SPIN =
            SynchedEntityData.defineId(ChubbyEntity.class, EntityDataSerializers.BOOLEAN);

    private int thunderStrikeCooldownTicks = 160;
    private int rainCooldownTicks = 100;
    private int chargingTicks = 0;
    private int spinTicks = 0;
    private long lastPetGameTime = 0L;

    public ChubbyEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 15, true);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING_LIGHTNING, false);
        builder.define(EXCITED_SPIN, false);
    }

    public boolean isChargingLightning() {
        return this.entityData.get(CHARGING_LIGHTNING);
    }

    public void setChargingLightning(boolean charging) {
        this.entityData.set(CHARGING_LIGHTNING, charging);
    }

    public boolean isExcitedSpin() {
        return this.entityData.get(EXCITED_SPIN);
    }

    public void setExcitedSpin(boolean spin) {
        this.entityData.set(EXCITED_SPIN, spin);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation flyingNav = new FlyingPathNavigation(this, level);
        flyingNav.setCanOpenDoors(false);
        flyingNav.setCanFloat(true);
        flyingNav.setCanPassDoors(true);
        return flyingNav;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D) // 12 corazones base
                .add(Attributes.FLYING_SPEED, 0.65D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new ChubbyLightningAttackGoal(this));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.35D, 6.0F, 2.0F));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.15D, this::isFood, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

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
            healthAttr.setBaseValue(24.0D + (spirit * 0.5D));
        }
        var attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(7.0D + (spirit * 0.3D));
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Partículas pasivas de nube, lluvia diminuta y chispitas estáticas
        if (this.level().isClientSide()) {
            if (this.random.nextFloat() < 0.35F) {
                this.level().addParticle(ParticleTypes.CLOUD,
                        this.getRandomX(0.5D), this.getY() + 0.2D, this.getRandomZ(0.5D),
                        0.0D, -0.01D, 0.0D);
            }
            if (this.random.nextFloat() < 0.20F) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                        this.getRandomX(0.5D), this.getY() + 0.4D, this.getRandomZ(0.5D),
                        (this.random.nextFloat() - 0.5F) * 0.05D, 0.05D, (this.random.nextFloat() - 0.5F) * 0.05D);
            }
        }

        // Control de animación de carga eléctrica
        if (chargingTicks > 0) {
            chargingTicks--;
            if (chargingTicks <= 0 && isChargingLightning()) {
                setChargingLightning(false);
            }
        }

        // Control de animación de giro alegre por caricia
        if (spinTicks > 0) {
            spinTicks--;
            if (spinTicks <= 0 && isExcitedSpin()) {
                setExcitedSpin(false);
            }
        }

        if (!this.level().isClientSide() && this.isAlive() && this.isTame()) {
            if (thunderStrikeCooldownTicks > 0) thunderStrikeCooldownTicks--;
            if (rainCooldownTicks > 0) rainCooldownTicks--;

            LivingEntity owner = this.getOwner();
            if (owner instanceof ServerPlayer playerOwner && playerOwner.isAlive()) {
                if (this.tickCount % 200 == 0) {
                    updateStatsFromOwner(playerOwner);
                }

                if (!this.isOrderedToSit()) {
                    // 1. Lluvia Extintora: Apaga el fuego del dueño o aliados
                    if (rainCooldownTicks <= 0) {
                        checkAndExtinguishFire(playerOwner);
                    }

                    // 2. Rayo Devastador: Invocación de rayo del cielo en combate
                    if (thunderStrikeCooldownTicks <= 0 && this.getTarget() != null && this.getTarget().isAlive()) {
                        executeThunderstrike(this.getTarget(), playerOwner);
                        this.thunderStrikeCooldownTicks = 360; // 18 segundos
                    }
                }
            }
        }
    }

    /**
     * Habilidad 1: Rayo Devastador del Cielo sobre el objetivo hostil
     */
    private void executeThunderstrike(LivingEntity target, ServerPlayer owner) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        this.chargingTicks = 25;
        this.setChargingLightning(true);

        // Rayo visual espectacular del cielo sin fuego amigo
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning != null) {
            lightning.moveTo(target.position());
            lightning.setVisualOnly(true); // Solo visual y sonoro para no dañar aliados ni mascotas
            serverLevel.addFreshEntity(lightning);
        }

        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.2F, 1.3F);

        // Daño eléctrico directo y exclusivo a monstruos hostiles en el área
        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;
        float damage = 14.0F + (spirit * 0.4F);

        List<Monster> hostiles = serverLevel.getEntitiesOfClass(Monster.class, target.getBoundingBox().inflate(3.5D), Monster::isAlive);
        DamageSource thunderSource = this.damageSources().mobAttack(this);
        for (Monster m : hostiles) {
            m.hurt(thunderSource, damage);
            m.setRemainingFireTicks(60);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + 0.5D, m.getZ(),
                    15, 0.3D, 0.3D, 0.3D, 0.1D);
        }
        if (!hostiles.contains(target)) {
            target.hurt(thunderSource, damage);
            target.setRemainingFireTicks(60);
        }

        owner.displayClientMessage(
                Component.literal("§e[⚡] §f¡Chubby descargó un §6Rayo Devastador §fsobre " + target.getDisplayName().getString() + "!"),
                true
        );
    }

    /**
     * Habilidad 2: Lluvia Extintora contra fuego y lava
     */
    private void checkAndExtinguishFire(ServerPlayer owner) {
        List<LivingEntity> allies = PartyManager.getAllies(owner, 16.0D);
        LivingEntity burningAlly = null;

        for (LivingEntity ally : allies) {
            if (ally.isAlive() && (ally.isOnFire() || ally.isInLava())) {
                burningAlly = ally;
                break;
            }
        }

        if (burningAlly != null) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            burningAlly.clearFire();
            burningAlly.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0, false, true, true));

            // Chubby vierte lluvia sobre el aliado
            for (int i = 0; i < 20; i++) {
                serverLevel.sendParticles(ParticleTypes.RAIN,
                        burningAlly.getX() + (this.random.nextFloat() - 0.5D) * 0.8D,
                        burningAlly.getY() + burningAlly.getBbHeight() + 0.6D,
                        burningAlly.getZ() + (this.random.nextFloat() - 0.5D) * 0.8D,
                        1, 0, -0.4D, 0, 0.05D);
            }
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    burningAlly.getX(), burningAlly.getY() + 0.5D, burningAlly.getZ(),
                    15, 0.3D, 0.3D, 0.3D, 0.1D);

            serverLevel.playSound(null, burningAlly.getX(), burningAlly.getY(), burningAlly.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.4F);

            String name = (burningAlly == owner) ? "ti" : burningAlly.getDisplayName().getString();
            owner.displayClientMessage(
                    Component.literal("§b[🌧️] §f¡Chubby vertió lluvia extintora sobre §e" + name + "§f! §b(Resistencia al Fuego)"),
                    true
            );
            this.rainCooldownTicks = 200; // 10 segundos
        }
    }

    /**
     * Ataque Básico: Chispas Eléctricas a Distancia
     */
    public void shootLightningZap(LivingEntity target) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            this.chargingTicks = 12;
            this.setChargingLightning(true);

            Vec3 start = this.position().add(0, 0.4D, 0);
            Vec3 end = target.position().add(0, target.getBbHeight() * 0.5D, 0);
            Vec3 diff = end.subtract(start);
            int steps = Math.max(5, (int) (diff.length() * 3));

            // Haz de chispas en zigzag simulando electricidad
            for (int i = 0; i <= steps; i++) {
                double prog = (double) i / steps;
                double jitterX = (this.random.nextFloat() - 0.5F) * 0.25D;
                double jitterY = (this.random.nextFloat() - 0.5F) * 0.25D;
                double jitterZ = (this.random.nextFloat() - 0.5F) * 0.25D;
                Vec3 p = start.add(diff.scale(prog)).add(jitterX, jitterY, jitterZ);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0.02D);
            }

            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.TRIDENT_THUNDER.value(), SoundSource.NEUTRAL, 0.8F, 1.8F);

            LivingEntity owner = this.getOwner();
            int spirit = 0;
            if (owner instanceof Player p) {
                PlayerRpgData data = p.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    spirit = data.getTotalStatPoints(p, StatType.SPIRIT);
                }
            }
            float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) + (spirit * 0.25F);

            // Daño aumentado si el enemigo está en agua o bajo lluvia
            if (target.isInWaterOrRain()) {
                damage *= 1.4F;
                serverLevel.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + 0.5D, target.getZ(),
                        10, 0.3D, 0.3D, 0.3D, 0.1D);
            }

            target.hurt(this.damageSources().mobAttack(this), damage);
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 1. Alimentar / Domesticar con minerales de energía pura
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
                                    this.getX(), this.getY() + 0.5D, this.getZ(),
                                    14, 0.35D, 0.35D, 0.35D, 0.1D);
                            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                    this.getX(), this.getY() + 0.6D, this.getZ(),
                                    16, 0.35D, 0.35D, 0.35D, 0.08D);
                        }
                        this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.7F);
                        player.displayClientMessage(
                                Component.literal("§e[⚡] §a¡Chubby se ha sintonizado con tu energía espiritual! Te seguirá como tu nube de tormenta."),
                                false
                        );
                    } else {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SMOKE,
                                    this.getX(), this.getY() + 0.4D, this.getZ(),
                                    6, 0.2D, 0.2D, 0.2D, 0.02D);
                        }
                        this.playSound(SoundEvents.BEE_STING, 0.8F, 1.8F);
                    }
                } else {
                    // Alimentar: cura y sobrecarga de estática
                    this.heal(6.0F);
                    this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.8F);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.8F);

                    this.chargingTicks = 20;
                    this.setChargingLightning(true);

                    // Pequeño bono de velocidad estática
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0, false, false, true));
                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0, false, false, true));

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + 0.5D, this.getZ(),
                                8, 0.3D, 0.3D, 0.3D, 0.08D);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                this.getX(), this.getY() + 0.5D, this.getZ(),
                                12, 0.25D, 0.25D, 0.25D, 0.06D);
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 2. Dueño agachado + clic derecho -> Anclar en el aire / Seguir
        if (this.isOwnedBy(player) && player.isShiftKeyDown() && !this.level().isClientSide()) {
            this.setOrderedToSit(!this.isOrderedToSit());
            this.navigation.stop();
            this.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 2.0F);
            player.displayClientMessage(
                    Component.literal("§e[Chubby] " + (this.isOrderedToSit() ?
                            "§7Se queda flotando estáticamente en esta posición." :
                            "§a¡Comienza a flotar detrás de ti!")),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 3. Dueño con mano vacía -> Acariciar a Chubby (cooldown 2 segundos / 40 ticks)
        if (this.isOwnedBy(player) && itemstack.isEmpty()) {
            player.swing(hand, true);
            if (!this.level().isClientSide()) {
                long gameTime = this.level().getGameTime();
                if (gameTime - this.lastPetGameTime < 40L) {
                    return InteractionResult.sidedSuccess(this.level().isClientSide());
                }
                this.lastPetGameTime = gameTime;

                this.spinTicks = 35;
                this.setExcitedSpin(true);
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.22D, 0)); // Salto de alegría en el aire
                this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.6F);
                this.playSound(SoundEvents.BEE_POLLINATE, 1.0F, 1.8F);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + 0.6D, this.getZ(),
                            8, 0.3D, 0.3D, 0.3D, 0.08D);
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            this.getX(), this.getY() + 0.5D, this.getZ(),
                            10, 0.25D, 0.25D, 0.25D, 0.05D);
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                            this.getX(), this.getY() + 0.4D, this.getZ(),
                            6, 0.3D, 0.2D, 0.3D, 0.02D);
                }
                player.displayClientMessage(
                        Component.literal("§e[⚡] §fAcaricias la esponjosa nubecita de Chubby. ¡Gira alegremente dando piruetas de emoción!"),
                        true
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.AMETHYST_SHARD)
                || stack.is(Items.REDSTONE)
                || stack.is(Items.GLOWSTONE_DUST)
                || stack.is(Items.LAPIS_LAZULI)
                || stack.is(Items.SUGAR);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_POLLINATE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.LIGHTNING_BOLT_IMPACT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FIREWORK_ROCKET_BLAST;
    }

    @Override
    public float getVoicePitch() {
        return 1.8F;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (PartyManager.isAllied(this, target)) {
            return false;
        }
        if (owner != null && PartyManager.isAllied(owner, target)) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return super.wantsToAttack(target, owner);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (PartyManager.isAllied(this, target)) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return super.canAttack(target);
    }

    /**
     * Goal de ataque a distancia para Chubby
     */
    static class ChubbyLightningAttackGoal extends Goal {
        private final ChubbyEntity chubby;
        private int attackTimer = 0;

        public ChubbyLightningAttackGoal(ChubbyEntity chubby) {
            this.chubby = chubby;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.chubby.getTarget();
            return target != null && target.isAlive() && !this.chubby.isOrderedToSit();
        }

        @Override
        public void tick() {
            LivingEntity target = this.chubby.getTarget();
            if (target == null) return;

            this.chubby.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distSq = this.chubby.distanceToSqr(target);

            // Mantenerse a distancia de 4 a 8 bloques
            if (distSq > 36.0D) {
                this.chubby.getNavigation().moveTo(target, 1.25D);
            } else if (distSq < 16.0D) {
                Vec3 away = this.chubby.position().subtract(target.position()).normalize().scale(2.0D);
                this.chubby.getNavigation().moveTo(this.chubby.getX() + away.x, this.chubby.getY() + 1.0D, this.chubby.getZ() + away.z, 1.2D);
            }

            if (attackTimer > 0) {
                attackTimer--;
            } else if (distSq <= 100.0D) {
                this.chubby.shootLightningZap(target);
                attackTimer = 25; // Disparo cada 1.25 segundos
            }
        }
    }
}
