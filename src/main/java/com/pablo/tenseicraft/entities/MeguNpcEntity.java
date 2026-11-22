package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.network.OpenNpcDialoguePayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Megu - Artesana y Herrera de Reforjas:
 * Realiza reforjas de equipamiento en las aldeas. Cuando el equipo o poder del jugador
 * supera los límites de su herrería mortal, reconoce su límite y ofrece en venta
 * el Yunque Mágico para que el aventurero pueda reforjar por sí mismo.
 */
public class MeguNpcEntity extends PathfinderMob {

    private final Map<UUID, Integer> affinityMap = new HashMap<>();

    public MeguNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 70.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
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
                    UUID uuid = UUID.fromString(key);
                    affinityMap.put(uuid, affTag.getInt(key));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setCustomName(Component.literal("§6Megu §7- §eArtesana de Reforjas"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
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

            boolean anvilUnlocked = data.isMeguAnvilUnlocked();
            int reforgeCount = data.getMeguReforgeCount();
            String dialogue;

            if (anvilUnlocked) {
                dialogue = "¡Hola de nuevo, " + player.getName().getString() + "! Como te confesé, el nivel de poder de tu equipamiento y de los Cristales de Alma que manejas ya supera lo que mis manos pueden templar sin riesgo. Si aún necesitas tu propio Yunque Mágico para forjar sin límites por tu cuenta, tengo uno reservado para ti en mi taller.";
            } else if (reforgeCount > 0) {
                dialogue = "¡Bienvenido a mi forja, " + player.getName().getString() + "! Ya he trabajado en tu equipamiento " + reforgeCount + " " + (reforgeCount == 1 ? "vez" : "veces") + ". Cada vez noto una resonancia más intensa en tus armas y armaduras... Muéstrame tu equipo cuando quieras que lo examine o temple.";
            } else {
                dialogue = "¡Hola, viajero! Soy Megu, artesana y herrera especializada en reforjas con Cristales de Alma. Puedo templar y despertar el potencial oculto de tu equipamiento... aunque te advierto que si algún día traes reliquias o un poder que supere mis límites como herrera, tendrás que aprender a usar un Yunque Mágico con tus propias manos.";
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ANVIL_PLACE, SoundSource.NEUTRAL, 0.45F, 1.35F);

            AdventurerRank rank = data.getRank();
            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    "Megu",
                    "megu",
                    0,
                    rank.getIndex(),
                    this.getAffinity(serverPlayer),
                    false,
                    false,
                    0L,
                    false,
                    reforgeCount,
                    anvilUnlocked,
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
