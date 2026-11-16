package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.party.PartyManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * Pelotita: Criatura invocada mediante la "Técnica Espiritual Nº 7: Convocar Alma Menor".
 * - Vida base: 4 corazones (8 HP), escalable con el Espíritu del invocador.
 * - Sigue fielmente a su dueño y se une a su grupo.
 * - Cura con Low Healing cada 30 segundos, distinguiendo si el grupo está en combate o descansando.
 * - Se alimenta y cura con Azúcar.
 */
public class PelotitaEntity extends TamableAnimal {

    private int healCooldownTicks = 0;

    public PelotitaEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D) // 4 corazones base
                .add(Attributes.MOVEMENT_SPEED, 0.26D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        // Sigue al dueño a todos lados como un leal compañero
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2D, 6.0F, 2.0F));
        // Sigue al jugador que lleve azúcar
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.15D, stack -> stack.is(Items.SUGAR), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /**
     * Ajusta la salud máxima y potencia de la Pelotita según el Espíritu de su invocador.
     */
    public void updateStatsFromOwner(Player owner) {
        if (owner == null) return;
        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;

        // 8 HP base + 0.4 HP por punto de Espíritu
        double bonusHp = spirit * 0.4D;
        var healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(8.0D + bonusHp);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide() && this.isAlive() && this.isTame()) {
            if (healCooldownTicks > 0) {
                healCooldownTicks--;
            } else if (this.tickCount % 10 == 0) {
                LivingEntity owner = this.getOwner();
                if (owner instanceof net.minecraft.server.level.ServerPlayer playerOwner && playerOwner.isAlive()) {
                    attemptLowHealingSupport(playerOwner);
                }
            }
        }
    }

    /**
     * Intenta lanzar Low Healing al dueño o a un miembro del grupo con salud comprometida.
     * Distingue si están en combate (umbral 70%) o fuera de combate (umbral 90%).
     */
    private void attemptLowHealingSupport(net.minecraft.server.level.ServerPlayer owner) {
        boolean inCombat = isOwnerInCombat(owner);
        float threshold = inCombat ? 0.70F : 0.90F;

        // Obtener todos los aliados del grupo en un radio de 18 bloques
        List<LivingEntity> allies = PartyManager.getAllies(owner, 18.0D);

        LivingEntity lowestAlly = null;
        float lowestPct = 1.0F;

        for (LivingEntity ally : allies) {
            if (!ally.isAlive()) continue;
            float pct = ally.getHealth() / ally.getMaxHealth();
            if (pct < threshold && pct < lowestPct) {
                lowestPct = pct;
                lowestAlly = ally;
            }
        }

        // Si encontramos a alguien herido, lanzamos Low Healing
        if (lowestAlly != null) {
            castSupportLowHealing(owner, lowestAlly, inCombat);
            this.healCooldownTicks = 600; // 30 segundos de enfriamiento
        }
    }

    private void castSupportLowHealing(Player owner, LivingEntity target, boolean inCombat) {
        ServerLevel serverLevel = (ServerLevel) this.level();

        // Potencia de sanación: Base 4.0 HP + 0.35 HP por punto de Espíritu del dueño
        PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
        int spirit = (data != null) ? data.getTotalStatPoints(owner, StatType.SPIRIT) : 0;
        float healAmount = 4.0F + (spirit * 0.35F);

        target.heal(healAmount);

        // Efectos visuales sagrados entre la Pelotita y el objetivo
        Vec3 start = this.position().add(0, 0.4, 0);
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 diff = end.subtract(start);
        int steps = Math.max(5, (int) (diff.length() * 3));

        for (int i = 0; i <= steps; i++) {
            double prog = (double) i / steps;
            Vec3 p = start.add(diff.scale(prog));
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.01);
            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
        serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 0.5, this.getZ(), 12, 0.2, 0.2, 0.2, 0.1);

        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.8F);

        // Notificación al dueño
        String targetName = (target == owner) ? "ti" : target.getDisplayName().getString();
        String combatBadge = inCombat ? "§c[¡En Combate!] " : "§b[Soporte] ";
        owner.displayClientMessage(
                Component.literal("§a[✦] " + combatBadge + "§fTu Pelotita usó §eLow Healing §fen §b" + targetName + " §a(+" + String.format(Locale.ROOT, "%.1f", healAmount) + " HP)"),
                true
        );
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

        // 1. Alimentar con azúcar: cura 4 HP y suelta corazones
        if (itemstack.is(Items.SUGAR)) {
            if (!this.level().isClientSide()) {
                if (!player.isCreative()) {
                    itemstack.shrink(1);
                }
                this.heal(4.0F);

                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.6F);
                this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.8F);

                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HEART,
                            this.getX(), this.getY() + 0.6D, this.getZ(),
                            7, 0.25D, 0.25D, 0.25D, 0.1D);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 2. Ordenar sentarse / quedarse si es el dueño
        if (this.isOwnedBy(player) && !this.level().isClientSide() && player.isShiftKeyDown()) {
            this.setOrderedToSit(!this.isOrderedToSit());
            this.jumping = false;
            this.navigation.stop();
            player.displayClientMessage(
                    Component.literal("§e[Pelotita] " + (this.isOrderedToSit() ? "§7Se queda esperando aquí." : "§aVuelve a seguirte.")),
                    true
            );
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SUGAR);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null; // Entidad espiritual única invocada
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F;
    }
}
