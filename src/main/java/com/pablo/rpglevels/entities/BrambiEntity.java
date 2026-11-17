package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.party.PartyManager;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Brambi: Compañero noble con forma de venado / ciervo espiritual de la naturaleza.
 * - Fiel protector que ataca con sus astas y puede enraizar enemigos en el suelo.
 * - Habilidad 1 (Manto de Espinas & Savia): Otorga Resistencia y Absorción protectora al grupo.
 * - Habilidad 2 (Santuario de Flora Viva): Cura en área con esporas, pétalos y regeneración viva.
 * - Interactivo: se alimenta de trigo/zanahorias/bayas, se puede acariciar y tiene animaciones elegantes.
 */
public class BrambiEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> BLOOMING =
            SynchedEntityData.defineId(BrambiEntity.class, EntityDataSerializers.BOOLEAN);

    private int bloomCooldownTicks = 300;
    private int thornsCooldownTicks = 160;
    private int bloomTicks = 0;

    public BrambiEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLOOMING, false);
    }

    public boolean isBlooming() {
        return this.entityData.get(BLOOMING);
    }

    public void setBlooming(boolean blooming) {
        this.entityData.set(BLOOMING, blooming);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D) // 16 corazones base
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 7.5D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.25D, 7.0F, 2.5F));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.15D, this::isFood, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    /**
     * Escala los atributos de Brambi con el Espíritu del invocador/dueño.
     */
    public void updateStatsFromOwner(Player owner) {
        if (owner == null) return;
        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;

        var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(32.0D + (spirit * 0.6D));
        }
        var attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(7.5D + (spirit * 0.3D));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Partículas sutiles de hojas/esporas en reposo o al caminar
        if (this.level().isClientSide() && this.random.nextFloat() < 0.20F) {
            this.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,
                    this.getRandomX(0.7D), this.getY() + 0.6D, this.getRandomZ(0.7D),
                    0.0D, 0.02D, 0.0D);
        }

        // Control de animación de florecimiento
        if (bloomTicks > 0) {
            bloomTicks--;
            if (bloomTicks <= 0 && isBlooming()) {
                setBlooming(false);
            }
            if (this.level().isClientSide() && this.random.nextFloat() < 0.4F) {
                this.level().addParticle(ParticleTypes.FALLING_SPORE_BLOSSOM,
                        this.getRandomX(0.8D), this.getY() + 1.2D, this.getRandomZ(0.8D),
                        0.0D, -0.04D, 0.0D);
            }
        }

        if (!this.level().isClientSide() && this.isAlive() && this.isTame()) {
            if (bloomCooldownTicks > 0) bloomCooldownTicks--;
            if (thornsCooldownTicks > 0) thornsCooldownTicks--;

            LivingEntity owner = this.getOwner();
            if (owner instanceof ServerPlayer playerOwner && playerOwner.isAlive()) {
                if (this.tickCount % 200 == 0) {
                    updateStatsFromOwner(playerOwner);
                }

                if (!this.isOrderedToSit()) {
                    // 1. Manto de Espinas & Savia en combate (Cooldown: 30s)
                    if (thornsCooldownTicks <= 0) {
                        checkAndCastBarkskin(playerOwner);
                    }

                    // 2. Santuario de Flora Viva cuando alguien está herido (Cooldown: 45s)
                    if (bloomCooldownTicks <= 0) {
                        checkAndCastBloomSanctuary(playerOwner);
                    }
                }
            }
        }
    }

    /**
     * Habilidad 1: Manto de Espinas & Savia Protectora (Barkskin & Thorns)
     */
    private void checkAndCastBarkskin(ServerPlayer owner) {
        boolean inCombat = isOwnerInCombat(owner);
        if (inCombat) {
            castBarkskin(owner);
            this.thornsCooldownTicks = 600; // 30 segundos
        }
    }

    private void castBarkskin(ServerPlayer owner) {
        ServerLevel serverLevel = (ServerLevel) this.level();
        List<LivingEntity> allies = PartyManager.getAllies(owner, 16.0D);

        this.bloomTicks = 30;
        this.setBlooming(true);

        for (LivingEntity ally : allies) {
            if (!ally.isAlive()) continue;

            // Resistencia I (15s) y Absorción I (30s)
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0, false, true, true));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0, false, true, true));

            serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.5D, ally.getZ(),
                    16, 0.4D, 0.4D, 0.4D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                    ally.getX(), ally.getY() + 0.3D, ally.getZ(),
                    8, 0.3D, 0.1D, 0.3D, 0.05D);
        }

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BAMBOO_WOOD_BREAK, SoundSource.PLAYERS, 1.2F, 1.4F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.5F);

        owner.displayClientMessage(
                Component.literal("§2[🌿] §f¡Brambi invoca el §aManto de Espinas y Savia Protectora §fpara el grupo!"),
                true
        );
    }

    /**
     * Habilidad 2: Santuario de Flora Viva (AoE Heal & Regeneración viva)
     */
    private void checkAndCastBloomSanctuary(ServerPlayer owner) {
        List<LivingEntity> allies = PartyManager.getAllies(owner, 16.0D);
        boolean someoneInjured = false;
        for (LivingEntity ally : allies) {
            if (ally.isAlive() && (ally.getHealth() / ally.getMaxHealth()) < 0.80F) {
                someoneInjured = true;
                break;
            }
        }

        if (someoneInjured) {
            castBloomSanctuary(owner, allies);
            this.bloomCooldownTicks = 900; // 45 segundos
        }
    }

    private void castBloomSanctuary(ServerPlayer owner, List<LivingEntity> allies) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        this.bloomTicks = 50;
        this.setBlooming(true);

        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;
        float healAmount = 7.0F + (spirit * 0.35F);

        // Anillo de esporas y hojas alrededor de Brambi
        for (int i = 0; i < 36; i++) {
            double angle = (i / 36.0D) * Math.PI * 2;
            double vx = Math.cos(angle) * 0.38D;
            double vz = Math.sin(angle) * 0.38D;
            serverLevel.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                    this.getX(), this.getY() + 0.6D, this.getZ(),
                    1, vx, 0.12D, vz, 0.08D);
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        1, vx * 0.6D, 0.1D, vz * 0.6D, 0.05D);
            }
        }

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.CHERRY_WOOD_STEP, SoundSource.PLAYERS, 1.2F, 1.2F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1.2F, 1.5F);

        for (LivingEntity ally : allies) {
            if (!ally.isAlive()) continue;

            ally.heal(healAmount);
            // Regeneración II por 10 segundos
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true, true));

            // Purificar toxinas de la naturaleza
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.WITHER);
            ally.removeEffect(MobEffects.BLINDNESS);

            serverLevel.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.6D, ally.getZ(),
                    16, 0.35D, 0.35D, 0.35D, 0.06D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.4D, ally.getZ(),
                    8, 0.3D, 0.3D, 0.3D, 0.05D);
        }

        owner.displayClientMessage(
                Component.literal("§a[🌸] §f¡El §2Santuario de la Naturaleza §fde Brambi florece y vigoriza al grupo! §a(+" +
                        String.format(Locale.ROOT, "%.1f", healAmount) + " HP)"),
                true
        );
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            if (this.level() instanceof ServerLevel serverLevel) {
                // Golpe de astas: ramas y hojas
                serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        12, 0.3D, 0.3D, 0.3D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        10, 0.25D, 0.25D, 0.25D, 0.05D);
            }
            this.playSound(SoundEvents.WOOD_BREAK, 1.0F, 1.4F);

            // 40% de probabilidad de enraizar al enemigo en el suelo
            if (this.random.nextFloat() < 0.40F) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false, true));
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.ROOTED_DIRT.defaultBlockState()),
                            living.getX(), living.getY(), living.getZ(),
                            15, 0.3D, 0.1D, 0.3D, 0.05D);
                }
                this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.1F, 1.2F);
            }
        }
        return hurt;
    }

    private boolean isOwnerInCombat(Player owner) {
        long gameTime = owner.level().getGameTime();
        if (owner.getLastHurtByMob() != null && gameTime - owner.getLastHurtByMobTimestamp() < 140L) {
            return true;
        }
        if (owner.getLastHurtMob() != null && gameTime - owner.getLastHurtMobTimestamp() < 140L) {
            return true;
        }
        List<Monster> hostiles = this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(14.0D),
                m -> m.isAlive() && (m.getTarget() == owner || PartyManager.isAllied(owner, m.getTarget())));
        return !hostiles.isEmpty();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // 1. Alimentar / Domesticar con manjares del bosque
        if (this.isFood(itemstack)) {
            if (!this.level().isClientSide()) {
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }

                if (!this.isTame()) {
                    // Intento de domesticación (35% de éxito)
                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        updateStatsFromOwner(player);

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                    this.getX(), this.getY() + 1.0D, this.getZ(),
                                    16, 0.4D, 0.4D, 0.4D, 0.1D);
                            serverLevel.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                                    this.getX(), this.getY() + 1.2D, this.getZ(),
                                    14, 0.35D, 0.35D, 0.35D, 0.08D);
                        }
                        this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.5F);
                        player.displayClientMessage(
                                Component.literal("§2[🦌] §a¡Brambi confía en tu espíritu y se une a tu lado como guardián natural!"),
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
                    // Alimentar cuando ya es tu compañero: cura y da Gracia del Bosque
                    this.heal(8.0F);
                    this.playSound(SoundEvents.HORSE_EAT, 1.0F, 1.3F);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.6F);

                    this.bloomTicks = 25;
                    this.setBlooming(true);

                    // Gracia del Bosque: Velocidad y Salto para ambos
                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0, false, false, true));
                    this.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 0, false, false, true));

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + 1.0D, this.getZ(),
                                10, 0.35D, 0.35D, 0.35D, 0.1D);
                        serverLevel.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                                this.getX(), this.getY() + 1.2D, this.getZ(),
                                12, 0.35D, 0.35D, 0.35D, 0.08D);
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 2. Dueño: Agacharse + Clic Derecho -> Ordenar Sentarse / Seguir
        if (this.isOwnedBy(player) && !this.level().isClientSide() && player.isShiftKeyDown()) {
            this.setOrderedToSit(!this.isOrderedToSit());
            this.jumping = false;
            this.navigation.stop();
            this.playSound(SoundEvents.CAMEL_AMBIENT, 1.0F, 1.4F);
            player.displayClientMessage(
                    Component.literal("§e[Brambi] " + (this.isOrderedToSit() ?
                            "§7Se echa pacíficamente en la hierba a descansar y montar guardia." :
                            "§a¡Se pone en pie con elegancia y te sigue alegremente!")),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 3. Dueño: Clic Derecho sin nada en la mano -> Acariciar las astas de Brambi
        if (this.isOwnedBy(player) && !this.level().isClientSide() && itemstack.isEmpty()) {
            this.bloomTicks = 20;
            this.setBlooming(true);
            this.playSound(SoundEvents.LLAMA_AMBIENT, 1.0F, 1.6F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + 1.1D, this.getZ(),
                        6, 0.3D, 0.3D, 0.3D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        this.getX(), this.getY() + 1.2D, this.getZ(),
                        8, 0.3D, 0.3D, 0.3D, 0.05D);
            }
            player.displayClientMessage(
                    Component.literal("§a[🦌] §fAcaricias las suaves astas de Brambi. ¡Menea su colita feliz de estar contigo!"),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
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
