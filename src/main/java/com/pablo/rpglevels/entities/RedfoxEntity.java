package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.OpenNpcDialoguePayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Redfox - Aventurera Kitsune errante de las aldeas.
 * Vaga libremente por la aldea empuñando un sable espiritual o espada.
 * Interactuable con diálogo y saludos personalizados.
 */
public class RedfoxEntity extends PathfinderMob {

    private final Map<UUID, Integer> affinityMap = new HashMap<>();

    private static final List<String> GREETING_DIALOGUES = List.of(
            "¡Hola, viajero! Qué agradable ver caras nuevas por estos lados. ¿También viniste siguiendo los rumores del aether?",
            "¡Ey! Me alegra encontrarte. Mis colas me dijeron que un guerrero con gran potencial andaba cerca.",
            "¡Saludos! Siempre estoy explorando las aldeas y caminos. Ten cuidado si te adentras en los bosques de noche.",
            "¡Hola! Descansa un momento en la aldea. Los viajes largos cansan hasta a los más veteranos.",
            "¡Qué tal! Ando revisando mi equipo antes de la próxima expedición. Pronto tendré algunas misiones interesantes si te animas."
    );

    public RedfoxEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 6.0);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setCustomName(Component.literal("§6Redfox §7- §eAventurera Kitsune"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();

        // Equipar arma: Espada de hierro o espada espiritual virgen si está disponible
        ItemStack weapon;
        try {
            weapon = new ItemStack(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SWORD.get());
        } catch (Throwable t) {
            weapon = new ItemStack(Items.IRON_SWORD);
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon);

        return data;
    }

    public int getAffinity(Player player) {
        if (player == null) return 0;
        return affinityMap.getOrDefault(player.getUUID(), 0);
    }

    public void addAffinity(Player player, int amount) {
        if (player == null) return;
        int current = getAffinity(player);
        affinityMap.put(player.getUUID(), Math.min(100, Math.max(0, current + amount)));
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
        if (tag.contains("Affinities", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag affTag = tag.getCompound("Affinities");
            for (String key : affTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    affinityMap.put(uuid, affTag.getInt(key));
                } catch (Exception ignored) {}
            }
        }
    }

    public String getRandomGreeting() {
        return GREETING_DIALOGUES.get(this.random.nextInt(GREETING_DIALOGUES.size()));
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource damageSource) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlayerRpgData data = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            int rankIdx = data != null ? data.getRank().getIndex() : AdventurerRank.F.getIndex();

            // Saludo con sonido de zorro amigable (sniff / chillido suave) y partículas amistosas
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.FOX_SNIFF, SoundSource.NEUTRAL, 1.0F, 1.15F);

            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 1.2, this.getZ(),
                        5, 0.3, 0.3, 0.3, 0.02);
            }

            String greeting = getRandomGreeting();

            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    "Redfox",
                    "redfox",
                    0,
                    rankIdx,
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
}
