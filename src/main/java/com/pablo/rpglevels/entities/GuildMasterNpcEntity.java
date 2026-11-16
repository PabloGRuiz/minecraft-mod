package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.network.OpenNpcDialoguePayload;
import java.util.List;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class GuildMasterNpcEntity extends PathfinderMob {

    private static final String[] GUILD_MASTER_NAMES = {
            "Gareth", "Aldous", "Vance", "Theron", "Cedric", "Darius", "Morcant", "Bastian"
    };

    public GuildMasterNpcEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
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
        RandomSource random = level.getRandom();

        if (!this.hasCustomName()) {
            String name = GUILD_MASTER_NAMES[random.nextInt(GUILD_MASTER_NAMES.length)];
            this.setCustomName(Component.literal("§6" + name + " §7- §eMaestro del Gremio"));
            this.setCustomNameVisible(true);
        }
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

            int rep = data.getReputationScore();
            String masterName = this.getName().getString();
            String dialogue;

            if (rep < 0) {
                dialogue = "¡Un criminal buscado! El Gremio de Aventureros no tolera forajidos ni escoria como tú. Márchate de aquí antes de que alerte a la guardia.";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.8, this.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
                }
            } else if (!data.isGuildRegistered()) {
                dialogue = "Saludos, viajero. Soy " + masterName + ", Maestro del Gremio de Aventureros. Veo gran potencial en ti... ¿Deseas inscribirte formalmente en el gremio para iniciar tu carrera como Aventurero de Rango F?";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.1F);
            } else {
                AdventurerRank rank = data.getRank();
                dialogue = "¡Bienvenido de vuelta, Aventurero " + player.getName().getString() + " (Rango " + rank.getName() + ")! ¿Vienes a consultar las misiones disponibles del gremio o necesitas asistencia?";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.2F);
            }

            List<AdventurerNpcEntity> companions = this.level().getEntitiesOfClass(
                    AdventurerNpcEntity.class,
                    player.getBoundingBox().inflate(16),
                    adv -> adv.isHiredBy(player) && adv.isReadyToAscend()
            );
            boolean anyReady = !companions.isEmpty();

            // Open Guild Master Dialogue Screen
            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    masterName,
                    "guild_master",
                    0,
                    data.getRank().getIndex(),
                    50,
                    data.isGuildRegistered(), // repurposed to indicate registration status
                    false,
                    0L,
                    false,
                    0,
                    false,
                    0,
                    false,
                    0,
                    anyReady,
                    dialogue
            ));

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }
}

