package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.network.SyncRpgDataPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Friendly NPC version of Kaelen that appears after her boss duel.
 * Uses standard Player Model with boss01.png skin.
 * Interacting with her unlocks Dash Level 1.
 */
public class WindMasterNpcEntity extends PathfinderMob {

    private int despawnDelay = 0;

    public WindMasterNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public void setDespawnDelay(int delay) {
        this.despawnDelay = delay;
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide() && this.despawnDelay > 0) {
            this.despawnDelay--;
            if (this.despawnDelay <= 0) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 1.0, this.getZ(), 25, 0.4, 0.5, 0.4, 0.1);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
                    serverLevel.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.NEUTRAL, 1.0F, 1.4F);
                    for (Player p : serverLevel.players()) {
                        if (p.distanceToSqr(this) <= 1024.0) {
                            p.sendSystemMessage(Component.literal("§a[✦] §7Kaelen ha saltado hacia el horizonte siguiendo el fluir del viento."));
                        }
                    }
                }
                this.discard();
            }
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("DespawnDelay", this.despawnDelay);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("DespawnDelay")) {
            this.despawnDelay = compound.getInt("DespawnDelay");
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide() && source.getEntity() instanceof ServerPlayer player) {
            triggerRematch(player);
            return true;
        }
        return super.hurt(source, amount);
    }

    private void triggerRematch(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        boolean hasDash = data != null && data.hasSkill("agility_dash");

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(this.level());
        if (lightning != null) {
            lightning.moveTo(this.getX(), this.getY(), this.getZ());
            lightning.setVisualOnly(true);
            this.level().addFreshEntity(lightning);
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 1.0F);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.2F, 1.2F);

        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.GUST, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.4, 0.5, 0.4, 0.1);
        }

        if (hasDash) {
            player.sendSystemMessage(Component.literal("§c§l[DESAFÍO ACEPTADO] §fKaelen: §e\"¿Así que buscas un verdadero desafío? ¡Esta vez no será tan sencillo!\"").withStyle(ChatFormatting.RED));
        } else {
            player.sendSystemMessage(Component.literal("§c§l[¡DUELO INICIADO!] §fKaelen: §e\"¡Prepárate! ¡Veamos si tienes los reflejos para merecer la técnica del viento!\"").withStyle(ChatFormatting.RED));
        }

        WindMasterBossEntity boss = ModEntities.WIND_MASTER.get().create(this.level());
        if (boss != null) {
            boss.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            boss.setEmpowered(hasDash);
            boss.setTarget(player);
            this.level().addFreshEntity(boss);
        }

        this.discard();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerRpgData data = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) {
                return InteractionResult.SUCCESS;
            }

            if (!data.hasSkill("agility_dash")) {
                boolean defeatedDuel = serverPlayer.getPersistentData().getBoolean("DefeatedKaelenDuel");
                if (defeatedDuel) {
                    // Defeated duel! Teach Dash Level 1!
                    data.unlockSkill("agility_dash");
                    serverPlayer.getPersistentData().remove("DefeatedKaelenDuel");

                    // Sync data
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncRpgDataPayload(data.serializeNBT(serverPlayer.registryAccess())));

                    // Audio & visual fanfare
                    serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    serverPlayer.serverLevel().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.2F);

                    if (serverPlayer.serverLevel() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.GUST, serverPlayer.getX(), serverPlayer.getY() + 0.5, serverPlayer.getZ(), 15, 0.3, 0.3, 0.3, 0.1);
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
                    }

                    serverPlayer.sendSystemMessage(Component.literal("§b§l[¡HABILIDAD APRENDIDA!] §fHas aprendido el §b§lDash Nivel 1§f.").withStyle(ChatFormatting.AQUA));
                    serverPlayer.sendSystemMessage(Component.literal("§7(Presiona la tecla [Left Alt] para dashear en 360° en el plano horizontal).").withStyle(ChatFormatting.GRAY));
                    serverPlayer.sendSystemMessage(Component.literal("§a[Kaelen] §f\"¡Excelente combate! Hacía mucho tiempo que nadie resistía mis flechas y mi frenesí. Reconozco tu destreza. Te he enseñado los fundamentos del viento: el Impulso Táctico (Dash Nivel 1). Practícalo en combate, ¡y regresa cuando quieras dominar técnicas más avanzadas!\"").withStyle(ChatFormatting.WHITE));
                } else {
                    // Has not defeated her in duel yet: Challenge the player!
                    serverPlayer.serverLevel().playSound(null, this.getX(), this.getY(), this.getZ(),
                            SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    serverPlayer.sendSystemMessage(Component.literal("§a[Kaelen] §f\"¡Saludos! Soy Kaelen, maestra del viento y del impulso táctico. Si deseas aprender el secreto del Dash, ¡primero debes demostrarme tu valor en un duelo! Golpéame cuando estés listo para comenzar.\"").withStyle(ChatFormatting.WHITE));
                }
            } else {
                // Already learned Dash
                serverPlayer.serverLevel().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.1F);
                serverPlayer.sendSystemMessage(Component.literal("§a[Kaelen] §f\"¡Saludos, camarada! Veo que estás dominando el Dash. Si buscas un verdadero desafío para ponerte a prueba, ¡golpéame para un combate con todo mi poder!\"").withStyle(ChatFormatting.WHITE));
            }
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }
}
