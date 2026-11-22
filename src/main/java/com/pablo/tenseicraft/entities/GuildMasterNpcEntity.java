package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.GreekGod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.network.OpenNpcDialoguePayload;
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
            GreekGod god = data.getPatronGod();
            String godName = (god != null) ? god.getDisplayName() : "los Dioses";
            String dialogue;

            if (rep < 0) {
                dialogue = "¡Un criminal buscado! Aunque portes la mirada de " + godName + ", el Gremio de Aventureros no tolera forajidos en sus sedes. Márchate antes de que alerte a la guardia.";
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.9F, 1.0F);
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 1.8, this.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
                }
            } else if (!data.isGuildRegistered()) {
                if (data.hasDivineBlessingUnlocked() && god != null) {
                    dialogue = "Saludos, viajero. Soy " + masterName + ", Maestro del Gremio. Percibo en tu espíritu la Bendición Divina de " + god.getDisplayName() + ". ¿Deseas inscribirte como Aventurero de Rango F? Recuerda que para progresar en los rangos deberás alcanzar cada Hito de Nivel para que tu Dios te otorgue la Ascensión en el Plano Astral.";
                } else {
                    dialogue = "Saludos, viajero. Soy " + masterName + ", Maestro del Gremio. En este mundo los Dioses observan desde el Plano Astral: abate a tu primer enemigo y duerme para recibir su Bendición y poder subir de nivel. ¿Deseas inscribirte formalmente como Aventurero de Rango F?";
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.1F);
            } else {
                AdventurerRank rank = data.getRank();
                if (!data.hasDivineBlessingUnlocked()) {
                    dialogue = "¡Bienvenido, Aventurero " + player.getName().getString() + " (Rango " + rank.getName() + ")! Aún no posees la Bendición de un Dios. Derrota a tu primer monstruo y ve a dormir para que un Dios te convoque al Espacio Astral y desbloquee tu subida de nivel.";
                } else if (data.isAtMilestoneCap()) {
                    dialogue = "¡Asombroso, " + player.getName().getString() + "! Has alcanzado el Hito de Nivel " + data.getCurrentMilestoneCap() + " de Rango " + rank.getName() + ". Como dicta la ley divina, ve a dormir a una cama: " + godName + " te convocará al Espacio Astral para felicitarte y otorgarte la Ascensión al siguiente rango.";
                } else {
                    dialogue = "¡Bienvenido, " + player.getName().getString() + " (Rango " + rank.getName() + ", Bendecido por " + godName + ")! Para ascender de rango debes llegar al Hito de Nivel " + data.getCurrentMilestoneCap() + " (actual: Nivel " + data.getRpgLevel() + ") y dormir para que " + godName + " te otorgue la Ascensión en el Plano Astral.";
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.9F, 1.2F);
            }

            List<AdventurerNpcEntity> companions = this.level().getEntitiesOfClass(
                    AdventurerNpcEntity.class,
                    player.getBoundingBox().inflate(16),
                    adv -> adv.isHiredBy(player) && adv.isReadyToAscend()
            );
            boolean anyReady = !companions.isEmpty();

            PacketDistributor.sendToPlayer(serverPlayer, new OpenNpcDialoguePayload(
                    this.getId(),
                    masterName,
                    "guild_master",
                    0,
                    data.getRank().getIndex(),
                    50,
                    data.isGuildRegistered(),
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
