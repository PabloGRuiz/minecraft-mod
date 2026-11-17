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
 * Starry: Compañero místico con apariencia de zorro multicolor celestial.
 * - Fiel combatiente que defiende a su dueño y embiste con saltos ágiles.
 * - Habilidad 1 (Danza Floral de Sakura): Cura en área a todo el grupo, eliminando efectos negativos.
 * - Habilidad 2 (Escudo Estelar): Otorga corazones de Absorción y Resistencia en momentos críticos.
 * - Habilidad 3 (Bendición Celestial): Regeneración II periódica para todo el equipo.
 * - Interactivo: se alimenta de bayas/dulces, se puede acariciar y tiene tiernas animaciones.
 */
public class StarryEntity extends TamableAnimal {

    private static final EntityDataAccessor<Boolean> DANCING =
            SynchedEntityData.defineId(StarryEntity.class, EntityDataSerializers.BOOLEAN);

    private int cherryBloomCooldownTicks = 200; // Primer uso rápido
    private int shieldCooldownTicks = 160;
    private int celestialRegenCooldownTicks = 600;
    private int danceTicks = 0;
    private long lastPetGameTime = 0L;

    public StarryEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DANCING, false);
    }

    public boolean isDancing() {
        return this.entityData.get(DANCING);
    }

    public void setDancing(boolean dancing) {
        this.entityData.set(DANCING, dancing);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D) // 13 corazones base
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.25D, 6.0F, 2.0F));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.15D, this::isFood, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    /**
     * Escala los atributos de Starry en función del Espíritu del invocador/dueño.
     */
    public void updateStatsFromOwner(Player owner) {
        if (owner == null) return;
        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;

        var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(26.0D + (spirit * 0.5D));
        }
        var attackAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(6.0D + (spirit * 0.25D));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        // Control de animación de danza/magia
        if (danceTicks > 0) {
            danceTicks--;
            if (danceTicks <= 0 && isDancing()) {
                setDancing(false);
            }
            if (this.level().isClientSide() && this.random.nextFloat() < 0.35F) {
                this.level().addParticle(ParticleTypes.CHERRY_LEAVES,
                        this.getRandomX(0.6D), this.getY() + 0.3D, this.getRandomZ(0.6D),
                        0.0D, 0.04D, 0.0D);
            }
        }

        if (!this.level().isClientSide() && this.isAlive() && this.isTame()) {
            if (cherryBloomCooldownTicks > 0) cherryBloomCooldownTicks--;
            if (shieldCooldownTicks > 0) shieldCooldownTicks--;
            if (celestialRegenCooldownTicks > 0) celestialRegenCooldownTicks--;

            LivingEntity owner = this.getOwner();
            if (owner instanceof ServerPlayer playerOwner && playerOwner.isAlive()) {
                // Sincronizar estadísticas cada 200 ticks
                if (this.tickCount % 200 == 0) {
                    updateStatsFromOwner(playerOwner);
                }

                if (!this.isOrderedToSit()) {
                    // 1. Escudo Estelar si alguien del grupo está bajo peligro (Cooldown: 25s)
                    if (shieldCooldownTicks <= 0) {
                        checkAndCastShield(playerOwner);
                    }

                    // 2. Danza Floral de Sakura si hay heridos (Cooldown: 40s)
                    if (cherryBloomCooldownTicks <= 0) {
                        checkAndCastCherryBloom(playerOwner);
                    }

                    // 3. Bendición de Regeneración celestial periódica (Cooldown: 90s)
                    if (celestialRegenCooldownTicks <= 0) {
                        checkAndCastCelestialRegen(playerOwner);
                    }
                }
            }
        }
    }

    /**
     * Habilidad 1: Danza Floral de Sakura (AoE Group Heal + Purificación)
     */
    private void checkAndCastCherryBloom(ServerPlayer owner) {
        List<LivingEntity> allies = PartyManager.getAllies(owner, 16.0D);
        boolean someoneInjured = false;
        for (LivingEntity ally : allies) {
            if (ally.isAlive() && (ally.getHealth() / ally.getMaxHealth()) < 0.85F) {
                someoneInjured = true;
                break;
            }
        }

        if (someoneInjured) {
            castCherryBloom(owner, allies);
            this.cherryBloomCooldownTicks = 800; // 40 segundos
        }
    }

    private void castCherryBloom(ServerPlayer owner, List<LivingEntity> allies) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        this.danceTicks = 45;
        this.setDancing(true);
        this.setDeltaMovement(this.getDeltaMovement().add(0, 0.32D, 0));

        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;
        float healAmount = 6.0F + (spirit * 0.35F);

        // Anillo de sakura expansivo desde Starry
        for (int i = 0; i < 32; i++) {
            double angle = (i / 32.0D) * Math.PI * 2;
            double vx = Math.cos(angle) * 0.35D;
            double vz = Math.sin(angle) * 0.35D;
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    this.getX(), this.getY() + 0.4D, this.getZ(),
                    1, vx, 0.1D, vz, 0.12D);
            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + 0.6D, this.getZ(),
                        1, vx * 0.5D, 0.15D, vz * 0.5D, 0.05D);
            }
        }

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 1.2F, 1.4F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);

        for (LivingEntity ally : allies) {
            if (!ally.isAlive()) continue;

            ally.heal(healAmount);
            // Purificación de efectos negativos
            ally.removeEffect(MobEffects.POISON);
            ally.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            ally.removeEffect(MobEffects.WEAKNESS);

            // Pétalos y corazones sobre el aliado
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.5D, ally.getZ(),
                    14, 0.35D, 0.35D, 0.35D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.HEART,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.7D, ally.getZ(),
                    6, 0.25D, 0.25D, 0.25D, 0.05D);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.4D, ally.getZ(),
                    8, 0.3D, 0.3D, 0.3D, 0.05D);
        }

        owner.displayClientMessage(
                Component.literal("§d[🌸] §f¡Starry danza con pétalos de sakura y cura al grupo! §a(+" +
                        String.format(Locale.ROOT, "%.1f", healAmount) + " HP)"),
                true
        );
    }

    /**
     * Habilidad 2: Escudo Estelar (Starlight Aegis)
     */
    private void checkAndCastShield(ServerPlayer owner) {
        boolean inCombat = isOwnerInCombat(owner);
        List<LivingEntity> allies = PartyManager.getAllies(owner, 16.0D);

        LivingEntity mostVulnerable = null;
        float lowestHpPct = 1.0F;

        for (LivingEntity ally : allies) {
            if (!ally.isAlive()) continue;
            float pct = ally.getHealth() / ally.getMaxHealth();
            if (pct < lowestHpPct) {
                lowestHpPct = pct;
                mostVulnerable = ally;
            }
        }

        // Se activa si están en combate o alguien baja del 70% de vida
        if (mostVulnerable != null && (inCombat || lowestHpPct < 0.70F)) {
            castStarlightShield(owner, mostVulnerable);
            this.shieldCooldownTicks = 500; // 25 segundos
        }
    }

    private void castStarlightShield(ServerPlayer owner, LivingEntity target) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        this.danceTicks = 25;
        this.setDancing(true);

        // Absorción II (4 corazones dorados) durante 30 segundos
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1, false, true, true));
        // Resistencia I durante 8 segundos
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0, false, true, true));

        // Rayo de destellos estelares entre Starry y el objetivo
        Vec3 start = this.position().add(0, 0.4D, 0);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        Vec3 diff = end.subtract(start);
        int steps = Math.max(6, (int) (diff.length() * 3));

        for (int i = 0; i <= steps; i++) {
            double prog = (double) i / steps;
            Vec3 p = start.add(diff.scale(prog));
            serverLevel.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.02);
            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                12, 0.35D, 0.35D, 0.35D, 0.08D);

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9F, 1.8F);
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);

        String targetName = (target == owner) ? "ti" : target.getDisplayName().getString();
        owner.displayClientMessage(
                Component.literal("§e[★] §f¡Starry conjuró un §6Escudo Estelar §fsobre §b" + targetName + "§f! §e(+4 Corazones de Absorción)"),
                true
        );
    }

    /**
     * Habilidad 3: Bendición Celestial (Regeneración de largo enfriamiento)
     */
    private void checkAndCastCelestialRegen(ServerPlayer owner) {
        List<LivingEntity> allies = PartyManager.getAllies(owner, 16.0D);
        ServerLevel serverLevel = (ServerLevel) this.level();

        this.danceTicks = 30;
        this.setDancing(true);

        for (LivingEntity ally : allies) {
            if (!ally.isAlive()) continue;
            // Regeneración II por 15 segundos
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 1, false, true, true));

            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.5D, ally.getZ(),
                    16, 0.4D, 0.4D, 0.4D, 0.1D);
            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    ally.getX(), ally.getY() + ally.getBbHeight() * 0.6D, ally.getZ(),
                    8, 0.3D, 0.3D, 0.3D, 0.05D);
        }

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.7F);
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 2.0F);

        owner.displayClientMessage(
                Component.literal("§b[✧] §f¡La Bendición Celestial de Starry llena al grupo de luz sanadora! §d(Regeneración II)"),
                true
        );
        this.celestialRegenCooldownTicks = 1800; // 90 segundos
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && target instanceof LivingEntity living) {
            // Efecto visual estelar en el ataque
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        10, 0.25D, 0.25D, 0.25D, 0.1D);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        6, 0.2D, 0.2D, 0.2D, 0.08D);
            }
            this.playSound(SoundEvents.FOX_BITE, 1.0F, 1.4F);

            // 25% de probabilidad de deslumbrar al enemigo con lentitud breve
            if (this.random.nextFloat() < 0.25F) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
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

        // 1. Alimentar / Domesticar con golosinas naturales
        if (this.isFood(itemstack)) {
            if (!this.level().isClientSide()) {
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }

                if (!this.isTame()) {
                    // Intento de domesticación (33% de éxito)
                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        updateStatsFromOwner(player);

                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                    this.getX(), this.getY() + 0.6D, this.getZ(),
                                    14, 0.35D, 0.35D, 0.35D, 0.1D);
                            serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                                    this.getX(), this.getY() + 0.5D, this.getZ(),
                                    12, 0.3D, 0.3D, 0.3D, 0.08D);
                        }
                        this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.6F);
                        player.displayClientMessage(
                                Component.literal("§d[★] §a¡Te has ganado el corazón de Starry! Ahora viajará y peleará a tu lado."),
                                false
                        );
                    } else {
                        if (this.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SMOKE,
                                    this.getX(), this.getY() + 0.5D, this.getZ(),
                                    6, 0.2D, 0.2D, 0.2D, 0.02D);
                        }
                        this.playSound(SoundEvents.FOX_SNIFF, 1.0F, 1.3F);
                    }
                } else {
                    // Alimentar cuando ya es tu compañero: cura y da corazoncitos
                    this.heal(6.0F);
                    this.playSound(SoundEvents.FOX_EAT, 1.0F, 1.4F);
                    this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.8F);

                    // Pequeño salto de alegría
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.28D, 0));
                    this.danceTicks = 20;
                    this.setDancing(true);

                    // Aumento de velocidad temporal por azúcar/bayas
                    this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 0, false, false, true));

                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HEART,
                                this.getX(), this.getY() + 0.6D, this.getZ(),
                                10, 0.3D, 0.3D, 0.3D, 0.1D);
                        serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                                this.getX(), this.getY() + 0.5D, this.getZ(),
                                10, 0.3D, 0.3D, 0.3D, 0.08D);
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
            this.playSound(SoundEvents.FOX_AMBIENT, 1.0F, 1.5F);
            player.displayClientMessage(
                    Component.literal("§e[Starry] " + (this.isOrderedToSit() ?
                            "§7Se acuesta a vigilar pacientemente." :
                            "§a¡Se pone de pie alegremente y te sigue!")),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 3. Dueño: Clic Derecho sin nada en la mano -> Acariciar a Starry (cooldown 2 segundos / 40 ticks)
        if (this.isOwnedBy(player) && !this.level().isClientSide() && itemstack.isEmpty()) {
            long gameTime = this.level().getGameTime();
            if (gameTime - this.lastPetGameTime < 40L) {
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
            this.lastPetGameTime = gameTime;

            this.danceTicks = 20;
            this.setDancing(true);
            this.playSound(SoundEvents.FOX_AMBIENT, 1.0F, 1.6F);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        this.getX(), this.getY() + 0.6D, this.getZ(),
                        5, 0.25D, 0.25D, 0.25D, 0.08D);
            }
            player.displayClientMessage(
                    Component.literal("§d[★] §fAcaricias tiernamente a Starry. ¡Mueve su colita con emoción!"),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.GLOW_BERRIES)
                || stack.is(Items.GOLDEN_CARROT)
                || stack.is(Items.APPLE)
                || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.SUGAR)
                || stack.is(Items.COOKIE);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null; // Compañero místico único
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
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
        return 1.45F; // Tono más agudo y tierno
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (com.pablo.rpglevels.party.PartyManager.isAllied(this, target)) {
            return false;
        }
        if (owner != null && com.pablo.rpglevels.party.PartyManager.isAllied(owner, target)) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return super.wantsToAttack(target, owner);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (com.pablo.rpglevels.party.PartyManager.isAllied(this, target)) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return super.canAttack(target);
    }
}
