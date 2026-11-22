package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.magic.SpiritualTechnique;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.network.OpenNpcDialoguePayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reimu Hakurei (NPC tras derrotar al jefe en El Gensokyo).
 * Permite charlar con referencias a Touhou Project, hacer donaciones para el Santuario Hakurei
 * y volver a desafiarla cuando el jugador quiera.
 */
public class ReimuNpcEntity extends PathfinderMob {

    private final Map<UUID, Integer> affinityMap = new HashMap<>();

    public ReimuNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.isCreativePlayer();
    }

    public int getAffinity(Player player) {
        if (player == null) return 0;
        return affinityMap.getOrDefault(player.getUUID(), 0);
    }

    public void addAffinity(Player player, int amount) {
        if (player == null) return;
        int current = getAffinity(player);
        affinityMap.put(player.getUUID(), Math.max(0, Math.min(100, current + amount)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setCustomName(Component.literal("§c⛩ Reimu Hakurei"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        return data;
    }

    /**
     * Transforma a Reimu NPC nuevamente en el jefe ReimuBossEntity para un nuevo duelo Danmaku.
     */
    public void startRematch(ServerPlayer challenger) {
        if (!(this.level() instanceof ServerLevel sl)) return;

        for (ReimuBossEntity existingBoss : sl.getEntities(ModEntities.REIMU_BOSS.get(), e -> true)) {
            existingBoss.discard();
        }
        for (EnergyAttackEntity existingOrb : sl.getEntities(ModEntities.ENERGY_ATTACK.get(), e -> true)) {
            existingOrb.discard();
        }
        for (ReimuNpcEntity existingNpc : sl.getEntities(ModEntities.REIMU_NPC.get(), e -> e != this)) {
            existingNpc.discard();
        }

        ReimuBossEntity boss = ModEntities.REIMU_BOSS.get().create(sl);
        if (boss != null) {
            boss.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
            boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.EVENT, null);
            boss.setAnchorPos(this.getX(), this.getY(), this.getZ());
            boss.startIntroSequence(0);
            sl.addFreshEntity(boss);

            // Alejar ligeramente al retador si estaba pegado para darle margen al primer patrón
            var away = challenger.position().subtract(this.position());
            var flat = new net.minecraft.world.phys.Vec3(away.x, 0.0, away.z);
            if (flat.lengthSqr() < 1.0E-4) {
                flat = new net.minecraft.world.phys.Vec3(0.0, 0.0, -1.0);
            } else {
                flat = flat.normalize();
            }
            challenger.setDeltaMovement(flat.x * 1.45, 0.42, flat.z * 1.45);
            challenger.hurtMarked = true;

            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.1F, 1.35F);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.0, this.getZ(), 60, 1.2, 1.0, 1.2, 0.15);
            challenger.sendSystemMessage(Component.literal("§c§l[¡NUEVO DESAFÍO HAKUREI!] §fReimu: §e\"¿Otra ronda de reglas de Spell Cards? ¡Perfecto, no te contengas!\""));
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

            // Si por algún motivo un jugador aliado interactúa sin haberla recibido antes, otorgar Ha-ku-rei
            if (!data.hasSkill(SpiritualTechnique.HAKUREI.getId())) {
                data.unlockSkill(SpiritualTechnique.HAKUREI.getId());
                ModNetworking.syncToPlayer(serverPlayer);
                serverPlayer.sendSystemMessage(Component.literal("§c✦ §l[REGALO DEL SANTUARIO HAKUREI] §eHas aprendido: §6§lTécnica Espiritual Nro. 890: Ha-ku-rei §7([ALT] + 3 - 1 - 3 - 1)"));
                serverPlayer.serverLevel().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.2F);
            }

            String greeting = "¿Otro visitante cruzando la Gran Barrera Hakurei? Qué raro ver a alguien aparecer entre los cerezos sin que Yukari tenga nada que ver... Aunque si no traes esmeraldas para la caja de donaciones, al menos quédate a tomar el té.";

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.CHERRY_WOOD_STEP, SoundSource.NEUTRAL, 0.8F, 1.2F);

            AdventurerRank rank = data.getRank();
            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    "Reimu Hakurei",
                    "reimu",
                    0,
                    rank.getIndex(),
                    this.getAffinity(serverPlayer),
                    false,
                    false,
                    0L,
                    false,
                    0,
                    false,
                    0,
                    false,
                    0,
                    false,
                    greeting
            ));

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        CompoundTag affTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : affinityMap.entrySet()) {
            affTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("Affinities", affTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Affinities", Tag.TAG_COMPOUND)) {
            CompoundTag affTag = tag.getCompound("Affinities");
            for (String key : affTag.getAllKeys()) {
                try {
                    affinityMap.put(UUID.fromString(key), affTag.getInt(key));
                } catch (Exception ignored) {}
            }
        }
    }
}
