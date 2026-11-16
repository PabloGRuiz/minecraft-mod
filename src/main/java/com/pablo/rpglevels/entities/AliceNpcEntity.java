package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.ModNetworking;
import com.pablo.rpglevels.network.OpenNpcDialoguePayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AliceNpcEntity extends PathfinderMob {

    private int despawnDelay = 0;
    private boolean isVisiting = false;

    // Affinities & Companion Escort
    private final Map<UUID, Integer> affinityMap = new HashMap<>();
    private UUID escortOwnerUuid = null;
    private long escortExpiryGameTime = 0L;
    private int fortifyCooldownTicks = 0;
    private int healAllyCooldownTicks = 0;
    private int attackSlashCooldownTicks = 0;

    public AliceNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public void setDespawnDelay(int delay) {
        this.despawnDelay = delay;
        this.isVisiting = delay > 0;
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    public boolean isVisiting() {
        return this.isVisiting;
    }

    public int getAffinity(Player player) {
        if (player == null) return 0;
        return affinityMap.getOrDefault(player.getUUID(), 0);
    }

    public void addAffinity(Player player, int amount) {
        if (player == null) return;
        int current = getAffinity(player);
        int next = Math.max(0, Math.min(100, current + amount));
        affinityMap.put(player.getUUID(), next);
    }

    public boolean isEscorting() {
        return escortOwnerUuid != null && this.level().getGameTime() < escortExpiryGameTime;
    }

    public boolean isEscorting(Player player) {
        return isEscorting() && player != null && player.getUUID().equals(escortOwnerUuid);
    }

    public Player getEscortOwner() {
        if (escortOwnerUuid == null) return null;
        return this.level().getPlayerByUUID(escortOwnerUuid);
    }

    public void startEscort(Player player, long durationTicks) {
        this.escortOwnerUuid = player.getUUID();
        this.escortExpiryGameTime = this.level().getGameTime() + durationTicks;
        this.setPersistenceRequired();
    }

    public void stopEscort() {
        this.escortOwnerUuid = null;
        this.escortExpiryGameTime = 0L;
        this.getNavigation().stop();
        this.setTarget(null);
    }

    public long getEscortRemainingTicks(long currentGameTime) {
        if (!isEscorting()) return 0L;
        return Math.max(0L, escortExpiryGameTime - currentGameTime);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DespawnDelay", this.despawnDelay);
        tag.putBoolean("IsVisiting", this.isVisiting);

        if (escortOwnerUuid != null) {
            tag.putUUID("EscortOwner", escortOwnerUuid);
            tag.putLong("EscortExpiry", escortExpiryGameTime);
        }

        net.minecraft.nbt.CompoundTag affTag = new net.minecraft.nbt.CompoundTag();
        for (Map.Entry<UUID, Integer> entry : affinityMap.entrySet()) {
            affTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("Affinities", affTag);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DespawnDelay")) {
            this.despawnDelay = tag.getInt("DespawnDelay");
        }
        if (tag.contains("IsVisiting")) {
            this.isVisiting = tag.getBoolean("IsVisiting");
        }
        if (tag.hasUUID("EscortOwner")) {
            this.escortOwnerUuid = tag.getUUID("EscortOwner");
            this.escortExpiryGameTime = tag.getLong("EscortExpiry");
        }
        if (tag.contains("Affinities", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            net.minecraft.nbt.CompoundTag affTag = tag.getCompound("Affinities");
            for (String key : affTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    affinityMap.put(uuid, affTag.getInt(key));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;

        // Asistencia Espiritual activa en combate
        if (this.isEscorting()) {
            Player owner = getEscortOwner();
            if (owner == null || !owner.isAlive()) {
                stopEscort();
                return;
            }

            if (this.level().getGameTime() >= this.escortExpiryGameTime) {
                owner.sendSystemMessage(Component.literal("§5[Alice]: §d«Mi tiempo de asistencia espiritual ha concluido por hoy, " + owner.getName().getString() + ". ¡Fue un verdadero honor luchar a tu lado! Nos vemos en el campamento.»"));
                stopEscort();
                return;
            }

            // Seguimiento al dueño
            double distSq = this.distanceToSqr(owner);
            if (distSq > 900.0) { // > 30 bloques
                this.teleportTo(owner.getX(), owner.getY(), owner.getZ());
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                    sl.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.2F);
                }
            } else if (distSq > 20.0) { // > 4.5 bloques
                this.getNavigation().moveTo(owner, 1.25D);
            }

            // Cooldowns de Técnicas Espirituales
            if (fortifyCooldownTicks > 0) fortifyCooldownTicks--;
            if (healAllyCooldownTicks > 0) healAllyCooldownTicks--;
            if (attackSlashCooldownTicks > 0) attackSlashCooldownTicks--;

            // 1. Técnica Espiritual N° 17: Fortificar Aliado (Spiritual Technique #17: Fortify Ally)
            if (fortifyCooldownTicks <= 0 && distSq <= 196.0) {
                boolean inCombat = (owner.getLastHurtByMob() != null && owner.getLastHurtByMob().isAlive())
                        || (owner.getLastHurtMob() != null && owner.getLastHurtMob().isAlive())
                        || (this.getTarget() != null && this.getTarget().isAlive());
                if (inCombat) {
                    fortifyCooldownTicks = 500; // 25 segundos
                    owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1)); // Fuerza II (15s)
                    owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1)); // Resistencia II (15s)
                    owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1)); // Absorción II (30s)

                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, owner.getX(), owner.getY() + 1.2, owner.getZ(), 25, 0.3, 0.5, 0.3, 0.05);
                        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, owner.getX(), owner.getY() + 1.2, owner.getZ(), 30, 0.4, 0.5, 0.4, 0.1);
                        sl.playSound(null, owner.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                    owner.sendSystemMessage(Component.literal("§5[Alice]: §d«¡Técnica Espiritual N° 17: Fortificar Aliado!» §aTus músculos y tu alma se llenan de poder arcano."));
                }
            }

            // 2. Técnica de Sanación Espiritual
            if (healAllyCooldownTicks <= 0 && distSq <= 196.0) {
                if (owner.getHealth() < owner.getMaxHealth() * 0.40f) {
                    healAllyCooldownTicks = 400; // 20 segundos
                    owner.heal(10.0F); // 5 corazones
                    owner.removeEffect(MobEffects.POISON);
                    owner.removeEffect(MobEffects.WITHER);
                    owner.clearFire();

                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HEART, owner.getX(), owner.getY() + 1.2, owner.getZ(), 10, 0.3, 0.4, 0.3, 0.05);
                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, owner.getX(), owner.getY() + 1.0, owner.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
                        sl.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.3F);
                    }
                    owner.sendSystemMessage(Component.literal("§5[Alice]: §b«¡Aliento Espiritual! ¡Resiste, " + owner.getName().getString() + "! Recupera tus energías.»"));
                }
            }

            // 3. Cortes Mágicos Ofensivos de Área contra monstruos hostiles
            if (attackSlashCooldownTicks <= 0) {
                var monsters = this.level().getEntitiesOfClass(
                        Monster.class,
                        this.getBoundingBox().inflate(4.0, 2.0, 4.0),
                        m -> m.isAlive()
                );
                if (!monsters.isEmpty()) {
                    attackSlashCooldownTicks = 35; // 1.75 segundos
                    for (Monster m : monsters) {
                        m.hurt(this.damageSources().mobAttack(this), 12.0F);
                        m.knockback(0.6, this.getX() - m.getX(), this.getZ() - m.getZ());
                    }
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.0, this.getZ(), 5, 0.5, 0.2, 0.5, 0.0);
                        sl.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.0, this.getZ(), 15, 0.5, 0.3, 0.5, 0.05);
                        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 1.0F, 1.1F);
                    }
                }
            }

            return; // No procesar timer de despawn en escolta activa
        }

        if (this.isVisiting && this.despawnDelay > 0) {
            // Verificar si algún jugador cercano (en 64 bloques) está en medio de la misión de purificación
            boolean hasActiveQuestNearby = false;
            for (Player player : this.level().players()) {
                if (player.distanceToSqr(this) <= 4096.0) {
                    PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                    if (data != null && data.isAliceQuestActive() && !data.hasReceivedSpiritualSword()) {
                        hasActiveQuestNearby = true;
                        break;
                    }
                }
            }

            // Si hay un jugador realizando la misión, Alice espera pacientemente y NO reduce su temporizador
            if (!hasActiveQuestNearby) {
                this.despawnDelay--;
                if (this.despawnDelay <= 0) {
                    if (this.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 35, 0.4, 0.6, 0.4, 0.15);
                        serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1.0F, 1.2F);
                        for (Player p : serverLevel.players()) {
                            if (p.distanceToSqr(this) <= 1024.0) {
                                p.sendSystemMessage(Component.literal("§5[✦] §7Alice ha partido hacia nuevas misiones de los Segadores Espirituales."));
                            }
                        }
                    }
                    this.discard();
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.25D, true) {
            @Override
            public boolean canUse() {
                return AliceNpcEntity.this.isEscorting() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return AliceNpcEntity.this.isEscorting() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.55D) {
            @Override
            public boolean canUse() {
                return !AliceNpcEntity.this.isEscorting() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));

        // Targeting goals when escorting
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Monster.class, true, e -> this.isEscorting()));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setCustomName(Component.literal("§dAlice §7- §5Ingeniera de los Segadores"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SWORD.get()));
        return data;
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

            if (data.isAliceQuestActive() && !data.isAliceQuestCompleted()) {
                com.pablo.rpglevels.quests.PlayerQuestData questData = serverPlayer.getData(ModAttachments.PLAYER_QUEST_DATA);
                if (questData != null && !questData.isQuestActive("alice_spiritual_trial") && !questData.isQuestClaimed("alice_spiritual_trial")) {
                    questData.acceptQuest("alice_spiritual_trial");
                    if (data.getAliceDMonstersKilled() > 0) {
                        questData.setProgress("alice_spiritual_trial", data.getAliceDMonstersKilled());
                    }
                    ModNetworking.syncQuestsToPlayer(serverPlayer);
                }
            }

            String dialogue;
            if (!data.isAliceQuestActive()) {
                dialogue = "Saludos, aventurero. Soy Alice, ingeniera de la facción de los Segadores Espirituales. Nos encargamos de combatir a los agentes del mal en este mundo. Si deseas recibir nuestra Espada Espiritual oficial, primero debes demostrar tu temple eliminando a 5 monstruos de Rango D (Brujas, Hoglins, Ahogados con tridente...). ¿Aceptas la misión?";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.2F);
            } else if (!data.isAliceQuestCompleted()) {
                int killed = data.getAliceDMonstersKilled();
                dialogue = "¿Cómo va tu purga de monstruos? Has eliminado a " + killed + "/5 monstruos de Rango D. Búscalos en ciénagas, el nether o en profundidades oceánicas.";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.1F);
            } else if (!data.hasReceivedSpiritualSword()) {
                dialogue = "¡Impresionante desempeño! Has eliminado a los 5 monstruos de Rango D. Cumplo con mi palabra: aquí tienes tu Espada Espiritual base y te enseñaré a Canalizar Magia. Recuerda que para despertar su Vínculo Maestro-Arma, primero debes asignarle un nombre en un yunque y luego canalizar tu magia teniendo al menos 200 de Maná.";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.NEUTRAL, 1.0F, 1.2F);
            } else if (data.getRank().getIndex() >= AdventurerRank.B.getIndex()) {
                dialogue = "¡Atención, camarada de Rango B! Un miembro de los Segadores Espirituales se ha corrompido y se ha unido a un culto herético que busca resucitar a la extinta raza de los demonios. El traidor ha escapado hacia la oscura dimensión del Abismo. Si tú y tu grupo están listos, puedo abrir una brecha y transportarlos directamente allí para cazarlo y purificar su alma.";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.2F);
            } else {
                dialogue = "Recuerda, camarada: nombra tu Espada Espiritual en un yunque, acumula 200 unidades de Maná entrenando tu Espíritu, y empuña el arma para canalizar tu magia. Así forjarás el Vínculo Maestro-Arma oficial y comenzará a autorrepararse. ¡Alcanza el Rango B de aventurero para recibir la misión del Segador Corrupto!";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.2F);
            }

            boolean hasDeliveredShadowblade = serverPlayer.getPersistentData().getBoolean("AliceShadowbladeGifted");
            long remainingTicks = this.getEscortRemainingTicks(this.level().getGameTime());

            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    "Alice",
                    "alice",
                    0,
                    data.getRank().getIndex(),
                    this.getAffinity(serverPlayer),
                    this.isEscorting(),
                    this.isEscorting(serverPlayer),
                    remainingTicks,
                    false,
                    0,
                    hasDeliveredShadowblade,
                    0,
                    false,
                    0,
                    false,
                    dialogue
            ));

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }
}

