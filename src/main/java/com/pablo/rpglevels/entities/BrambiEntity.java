package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.party.PartyManager;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Brambi: Venado de la Naturaleza & Espíritu del Bosque y Fuego.
 * - Linterna Viviente: Ilumina dinámicamente cuevas y zonas oscuras gracias a su farolito y cola de brasa,
 *   y derrite la nieve por donde pisa.
 * - Pasos Fértiles: Su magia floral actúa como polvo de hueso automático al caminar cerca de cultivos.
 * - Defensa de Brasas: Si un enemigo ataca a su dueño o a él, embiste con un cabezazo de sus cuernos de rama
 *   y prende fuego al agresor.
 * - Acariciar: Acariciarlo con la mano vacía tiene 50% de probabilidad de otorgar Velocidad I por 5 segundos
 *   (con cooldown de 2 segundos para evitar spam).
 * - Hurgador del Bosque: En momentos de calma, escarba la tierra buscando bayas y manjares para su dueño.
 */
public class BrambiEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> HEADBUTTING =
            SynchedEntityData.defineId(BrambiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FORAGING =
            SynchedEntityData.defineId(BrambiEntity.class, EntityDataSerializers.BOOLEAN);

    private int emberDefenseCooldownTicks = 0;
    private int foragerTimerTicks = 1200;
    private int headbuttAnimationTicks = 0;
    private int foragingAnimationTicks = 0;
    private long lastPetGameTime = 0L;
    private BlockPos lastLightPos = null;

    public BrambiEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEADBUTTING, false);
        builder.define(FORAGING, false);
    }

    public boolean isHeadbutting() {
        return this.entityData.get(HEADBUTTING);
    }

    public void setHeadbutting(boolean headbutting) {
        this.entityData.set(HEADBUTTING, headbutting);
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
                .add(Attributes.STEP_HEIGHT, 1.0D)
                .add(Attributes.SCALE, 0.68D);
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

        // Partículas en cliente: Brasa en la cola, farolito en la cabeza, y hojas al caminar
        if (this.level().isClientSide()) {
            Vec3 look = this.getViewVector(1.0F);

            // Cola de brasa
            Vec3 tailPos = this.position().subtract(look.scale(0.55D)).add(0, 0.45D, 0);
            if (this.random.nextFloat() < 0.35F) {
                this.level().addParticle(ParticleTypes.SMALL_FLAME,
                        tailPos.x + (this.random.nextDouble() - 0.5D) * 0.15D,
                        tailPos.y + (this.random.nextDouble() - 0.5D) * 0.1D,
                        tailPos.z + (this.random.nextDouble() - 0.5D) * 0.15D,
                        0.0D, 0.015D, 0.0D);
            }

            // Farolito brillante cerca de las astas
            Vec3 hornPos = this.position().add(look.scale(0.25D)).add(0, 0.85D, 0);
            if (this.random.nextFloat() < 0.20F) {
                this.level().addParticle(ParticleTypes.GLOW,
                        hornPos.x + (this.random.nextDouble() - 0.5D) * 0.2D,
                        hornPos.y + (this.random.nextDouble() - 0.5D) * 0.15D,
                        hornPos.z + (this.random.nextDouble() - 0.5D) * 0.2D,
                        0.0D, 0.01D, 0.0D);
            }

            // Estela de hojas y esporas al correr/caminar
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.005D && this.random.nextFloat() < 0.35F) {
                this.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,
                        this.getRandomX(0.6D), this.getY() + 0.3D, this.getRandomZ(0.6D),
                        0.0D, 0.03D, 0.0D);
                if (this.random.nextFloat() < 0.20F) {
                    this.level().addParticle(ParticleTypes.CHERRY_LEAVES,
                            this.getRandomX(0.5D), this.getY() + 0.4D, this.getRandomZ(0.5D),
                            0.0D, 0.01D, 0.0D);
                }
            }
        }

        // Animación de Cabezazo con astas
        if (headbuttAnimationTicks > 0) {
            headbuttAnimationTicks--;
            if (headbuttAnimationTicks <= 0 && isHeadbutting()) {
                setHeadbutting(false);
            }
        }

        // Animación de Hurgador
        if (foragingAnimationTicks > 0) {
            foragingAnimationTicks--;
            if (foragingAnimationTicks <= 0 && isForaging()) {
                setForaging(false);
            }
        }

        if (!this.level().isClientSide() && this.isAlive()) {
            if (emberDefenseCooldownTicks > 0) emberDefenseCooldownTicks--;
            if (foragerTimerTicks > 0) foragerTimerTicks--;

            // 1. Linterna viviente: Ilumina dinámicamente alrededor de Brambi con luz real
            updateDynamicLight();

            // 2. Derretir la nieve por donde pisa
            if (this.onGround() && this.tickCount % 4 == 0) {
                meltSnowBeneath();
            }

            // 3. Pasos Fértiles: Actúa como polvo de hueso automático en cultivos cercanos
            if (this.onGround() && this.tickCount % 25 == 0) {
                fertilizeCropsAround();
            }

            LivingEntity owner = this.getOwner();
            if (owner instanceof ServerPlayer playerOwner && playerOwner.isAlive()) {
                if (this.tickCount % 200 == 0) {
                    updateStatsFromOwner(playerOwner);
                }

                if (!this.isOrderedToSit()) {
                    // Defensa de brasas reactiva: Si el dueño fue atacado recientemente
                    if (emberDefenseCooldownTicks <= 0) {
                        LivingEntity attacker = null;
                        long gameTime = this.level().getGameTime();
                        if (playerOwner.getLastHurtByMob() != null && gameTime - playerOwner.getLastHurtByMobTimestamp() < 30L) {
                            attacker = playerOwner.getLastHurtByMob();
                        } else if (this.getLastHurtByMob() != null && gameTime - this.getLastHurtByMobTimestamp() < 30L) {
                            attacker = this.getLastHurtByMob();
                        }

                        if (attacker != null && attacker.isAlive() && !PartyManager.isAllied(this, attacker)) {
                            triggerEmberDefense(attacker);
                        }
                    }

                    // Hurgador de la naturaleza (fuera de combate)
                    if (foragerTimerTicks <= 0 && !isOwnerOrSelfInCombat(playerOwner)) {
                        attemptForage(playerOwner);
                        this.foragerTimerTicks = 1400; // ~70s
                    }
                }
            }
        }
    }

    /**
     * Linterna viviente: Coloca un bloque invisible de luz (nivel 13) a la altura de sus ojos/farolito
     * y lo retira dinámicamente cuando se desplaza a otra casilla.
     */
    private void updateDynamicLight() {
        BlockPos currentPos = this.blockPosition().above();

        if (lastLightPos != null && !lastLightPos.equals(currentPos)) {
            if (this.level().getBlockState(lastLightPos).is(Blocks.LIGHT)) {
                this.level().removeBlock(lastLightPos, false);
            }
            lastLightPos = null;
        }

        if (this.isAlive() && !this.isRemoved()) {
            if (this.level().getBlockState(currentPos).isAir()) {
                this.level().setBlock(currentPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13), 3);
                lastLightPos = currentPos;
            }
        }
    }

    private void clearDynamicLight() {
        if (lastLightPos != null) {
            if (this.level().getBlockState(lastLightPos).is(Blocks.LIGHT)) {
                this.level().removeBlock(lastLightPos, false);
            }
            lastLightPos = null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearDynamicLight();
        super.remove(reason);
    }

    @Override
    public void die(DamageSource damageSource) {
        clearDynamicLight();
        super.die(damageSource);
    }

    /**
     * Derretir nieve: El calor natural de su cola de brasa y farolito derrite las capas de nieve
     * y el hielo blando bajo sus pezuñas.
     */
    private void meltSnowBeneath() {
        BlockPos feet = this.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    BlockPos p = feet.offset(dx, dy, dz);
                    BlockState st = this.level().getBlockState(p);
                    if (st.is(Blocks.SNOW)) {
                        this.level().removeBlock(p, false);
                        if (this.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.SMOKE, p.getX() + 0.5D, p.getY() + 0.1D, p.getZ() + 0.5D,
                                    2, 0.1D, 0.05D, 0.1D, 0.01D);
                        }
                    } else if (st.is(Blocks.POWDER_SNOW)) {
                        this.level().setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                        if (this.level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.CLOUD, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                                    3, 0.15D, 0.1D, 0.15D, 0.01D);
                        }
                    }
                }
            }
        }
    }

    /**
     * Pasos fértiles: Aplica efecto de polvo de hueso a cultivos cercanos (trigo, zanahorias, patatas, etc.)
     * con hermosas partículas de flores y esporas.
     */
    private void fertilizeCropsAround() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        BlockPos current = this.blockPosition();
        int applied = 0;
        for (BlockPos p : BlockPos.betweenClosed(current.offset(-2, -1, -2), current.offset(2, 1, 2))) {
            BlockState st = sl.getBlockState(p);
            if (st.getBlock() instanceof BonemealableBlock bonemealable) {
                if (bonemealable.isValidBonemealTarget(sl, p, st)) {
                    bonemealable.performBonemeal(sl, sl.random, p, st);
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5D, p.getY() + 0.4D, p.getZ() + 0.5D,
                            4, 0.25D, 0.25D, 0.25D, 0.02D);
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                            3, 0.2D, 0.2D, 0.2D, 0.02D);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D,
                            2, 0.2D, 0.2D, 0.2D, 0.02D);
                    applied++;
                    if (applied >= 2) break; // Máx 2 cultivos por pulso
                }
            }
        }
        if (applied > 0) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BONE_MEAL_USE, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
    }

    /**
     * Defensa de brasas: Si un enemigo ataca, Brambi embiste con un cabezazo de sus cuernos de rama
     * y prende fuego al agresor durante unos segundos.
     */
    public boolean triggerEmberDefense(LivingEntity attacker) {
        if (!this.isAlive() || this.isOrderedToSit() || this.emberDefenseCooldownTicks > 0) {
            return false;
        }
        if (attacker == null || !attacker.isAlive() || PartyManager.isAllied(this, attacker)) {
            return false;
        }
        if (this.distanceToSqr(attacker) > 81.0D) {
            return false;
        }

        this.emberDefenseCooldownTicks = 80; // 4 segundos de cooldown
        this.headbuttAnimationTicks = 20;
        this.setHeadbutting(true);

        // Impulso hacia el atacante
        Vec3 dir = attacker.position().subtract(this.position()).normalize();
        this.setDeltaMovement(this.getDeltaMovement().add(dir.x * 0.45D, 0.22D, dir.z * 0.45D));

        // Cabezazo con cuernos: daño de ataque + quemadura de 3.5 segundos
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) + 1.5F;
        attacker.hurt(this.damageSources().mobAttack(this), damage);
        attacker.setRemainingFireTicks(70);
        attacker.setDeltaMovement(attacker.getDeltaMovement().add(dir.x * 0.55D, 0.25D, dir.z * 0.55D));

        if (this.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.4F);
            sl.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.NEUTRAL, 1.2F, 1.3F);

            BlockParticleOption leaves = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AZALEA_LEAVES.defaultBlockState());
            sl.sendParticles(ParticleTypes.FLAME, attacker.getX(), attacker.getY() + 0.5D, attacker.getZ(),
                    14, 0.3D, 0.3D, 0.3D, 0.08D);
            sl.sendParticles(ParticleTypes.SMALL_FLAME, attacker.getX(), attacker.getY() + 0.6D, attacker.getZ(),
                    10, 0.25D, 0.25D, 0.25D, 0.05D);
            sl.sendParticles(leaves, attacker.getX(), attacker.getY() + 0.5D, attacker.getZ(),
                    12, 0.3D, 0.3D, 0.3D, 0.1D);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, attacker.getX(), attacker.getY() + 0.6D, attacker.getZ(),
                    8, 0.2D, 0.2D, 0.2D, 0.04D);
        }

        LivingEntity owner = this.getOwner();
        if (owner instanceof ServerPlayer sp) {
            sp.displayClientMessage(
                    Component.literal("§6[🦌] §f¡Brambi embiste con sus astas de rama en §cDefensa de Brasas§f!"),
                    true
            );
        }
        return true;
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

            this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 0.9F, 1.4F);
            this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.4F);

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
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        this.getX(), this.getY() + 0.3D, this.getZ(),
                        5, 0.2D, 0.1D, 0.2D, 0.02D);
            }

            owner.displayClientMessage(
                    Component.literal("§a[🦌] §fBrambi escarbó la hierba y desenterró un manjar: §e" + gift.getHoverName().getString()),
                    true
            );
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            this.headbuttAnimationTicks = 15;
            this.setHeadbutting(true);
            living.setRemainingFireTicks(50); // 2.5 segundos de fuego

            Vec3 knockbackDir = living.position().subtract(this.position()).normalize().scale(0.65D);
            living.setDeltaMovement(living.getDeltaMovement().add(knockbackDir.x, 0.2D, knockbackDir.z));

            this.playSound(SoundEvents.BLAZE_SHOOT, 0.8F, 1.6F);
            this.playSound(SoundEvents.CHERRY_LEAVES_BREAK, 1.0F, 1.4F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        8, 0.25D, 0.25D, 0.25D, 0.05D);
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
                            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                                    this.getX(), this.getY() + 0.8D, this.getZ(),
                                    12, 0.3D, 0.3D, 0.3D, 0.05D);
                        }
                        this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);
                        player.displayClientMessage(
                                Component.literal("§2[🦌] §a¡Brambi confía en ti y te acompañará como guardián leal!"),
                                false
                        );
                    } else {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SMOKE,
                                    this.getX(), this.getY() + 0.8D, this.getZ(),
                                    6, 0.2D, 0.2D, 0.2D, 0.02D);
                        }
                        this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.4F);
                    }
                } else {
                    // Alimentar: sana y da vigor
                    this.heal(8.0F);
                    this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.3F);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.6F);

                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0, false, false, true));

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + 1.0D, this.getZ(),
                                8, 0.3D, 0.3D, 0.3D, 0.1D);
                        serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                                this.getX(), this.getY() + 0.8D, this.getZ(),
                                6, 0.25D, 0.25D, 0.25D, 0.05D);
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
            this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.4F);
            player.displayClientMessage(
                    Component.literal("§e[Brambi] " + (this.isOrderedToSit() ?
                            "§7Se echa pacíficamente en la hierba a montar guardia." :
                            "§a¡Se pone en pie con brío y alegría!")),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 3. Dueño con mano vacía (sin agacharse) -> Acariciar a Brambi (cooldown 2 segundos / 40 ticks)
        if (this.isOwnedBy(player) && !this.level().isClientSide() && itemstack.isEmpty()) {
            long gameTime = this.level().getGameTime();
            if (gameTime - this.lastPetGameTime < 40L) {
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
            this.lastPetGameTime = gameTime;

            this.foragingAnimationTicks = 20;
            this.setForaging(true); // Hace una reverencia agachando la cabecita
            this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.5F);
            this.playSound(SoundEvents.CHERRY_LEAVES_BREAK, 0.8F, 1.6F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + 1.1D, this.getZ(),
                        6, 0.25D, 0.25D, 0.25D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        this.getX(), this.getY() + 1.2D, this.getZ(),
                        8, 0.3D, 0.3D, 0.3D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        this.getX(), this.getY() + 0.8D, this.getZ(),
                        5, 0.2D, 0.2D, 0.2D, 0.03D);
                serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                        this.getX(), this.getY() + 0.6D, this.getZ(),
                        4, 0.15D, 0.15D, 0.15D, 0.02D);
            }

            // Probabilidad (50%) de otorgar velocidad de movimiento por 5 segundos
            if (this.random.nextBoolean()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false, true));
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0, false, false, true));
                player.displayClientMessage(
                        Component.literal("§a[🦌] §fAcaricias a Brambi. ¡La brisa del bosque te llena de ligereza! §b(Velocidad I por 5s)"),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("§a[🦌] §fAcaricias tiernamente las suaves astas de Brambi. ¡Menea su colita de brasa feliz!"),
                        true
                );
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
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
        return false;
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
        return SoundEvents.FOX_SNIFF;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.35F;
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
}
