package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.entities.BanditEntity;
import com.pablo.rpglevels.entities.BrambiEntity;
import com.pablo.rpglevels.entities.ChubbyEntity;
import com.pablo.rpglevels.entities.StarryEntity;
import com.pablo.rpglevels.items.ModItems;
import com.pablo.rpglevels.mobs.BossScalingHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mecánica de recolección de Energía Vital para Espíritus Protectores (Starry, Chubby y Brambi).
 * - Mientras estén invocados y cerca del jugador, absorben Energía Vital de las entidades que mueren alrededor.
 * - Al acumular 100 de Energía Vital, notifican al portador y dejan caer Semillas de Vida.
 */
@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class GuardianVitalEnergyEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        LivingEntity victim = event.getEntity();
        if (victim instanceof Player || victim instanceof ArmorStand) return;
        if (victim instanceof TamableAnimal tamable && tamable.isTame()) return;

        AABB searchBox = victim.getBoundingBox().inflate(32.0D);
        List<TamableAnimal> nearbyGuardians = level.getEntitiesOfClass(TamableAnimal.class, searchBox,
                GuardianVitalEnergyEvents::isEligibleGuardian);
        if (nearbyGuardians.isEmpty()) return;

        // Agrupar por dueño para recolectar como máximo una vez por dueño por muerte
        Map<UUID, TamableAnimal> guardianByOwner = new HashMap<>();
        for (TamableAnimal guardian : nearbyGuardians) {
            UUID ownerUuid = guardian.getOwnerUUID();
            if (ownerUuid == null) continue;
            TamableAnimal existing = guardianByOwner.get(ownerUuid);
            if (existing == null || (!existing.getTags().contains("SummonedGuardian")
                    && guardian.getTags().contains("SummonedGuardian"))) {
                guardianByOwner.put(ownerUuid, guardian);
            }
        }

        int energyGain = computeVitalEnergy(victim);
        if (energyGain <= 0) return;

        for (Map.Entry<UUID, TamableAnimal> entry : guardianByOwner.entrySet()) {
            UUID ownerUuid = entry.getKey();
            TamableAnimal guardian = entry.getValue();

            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
            if (owner == null || !owner.isAlive() || owner.level() != level) continue;

            boolean withinRange = guardian.distanceToSqr(victim) <= 24.0D * 24.0D
                    || (owner.distanceToSqr(victim) <= 24.0D * 24.0D && guardian.distanceToSqr(owner) <= 32.0D * 32.0D);
            if (!withinRange) continue;

            PlayerRpgData data = owner.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) continue;

            data.addGuardianVitalEnergy(energyGain);
            spawnVitalTrailParticles(level, victim, guardian);
            level.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.45F, 1.6F);

            String guardianName = resolveGuardianName(guardian);
            int seedsDropped = 0;

            while (data.getGuardianVitalEnergy() >= PlayerRpgData.MAX_GUARDIAN_VITAL_ENERGY) {
                data.setGuardianVitalEnergy(data.getGuardianVitalEnergy() - PlayerRpgData.MAX_GUARDIAN_VITAL_ENERGY);
                seedsDropped++;
                dropLifeSeed(level, guardian, owner);
            }
            com.pablo.rpglevels.network.ModNetworking.syncToPlayer(owner);

            if (seedsDropped > 0) {
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        guardian.getX(), guardian.getY() + 0.6D, guardian.getZ(),
                        30, 0.35D, 0.45D, 0.35D, 0.10D);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        guardian.getX(), guardian.getY() + 0.6D, guardian.getZ(),
                        20, 0.4D, 0.4D, 0.4D, 0.05D);
                level.sendParticles(ParticleTypes.END_ROD,
                        guardian.getX(), guardian.getY() + 0.6D, guardian.getZ(),
                        15, 0.3D, 0.4D, 0.3D, 0.06D);

                level.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                        SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.1F, 1.35F);
                owner.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.45F);

                String seedLabel = seedsDropped == 1
                        ? "una Semilla de Vida"
                        : seedsDropped + " Semillas de Vida";
                owner.sendSystemMessage(Component.empty());
                owner.sendSystemMessage(Component.literal("§a✦ §d[" + guardianName + "] §e¡Energía Vital al máximo!")
                        .append(Component.literal(" Tu Espíritu Protector ha condensado " + seedLabel
                                + " y la dejó caer a tu lado. Plántala en tierra arada para cultivar la Flor de la Vida.")
                                .withStyle(ChatFormatting.GREEN)));
                owner.sendSystemMessage(Component.empty());

                owner.displayClientMessage(Component.literal(
                        "§a✦ ¡" + guardianName + " soltó " + seedLabel + "! §8(§e"
                                + data.getGuardianVitalEnergy() + "§7/" + PlayerRpgData.MAX_GUARDIAN_VITAL_ENERGY + "§8)"),
                        true);
            } else {
                owner.displayClientMessage(Component.literal(
                        "§a✦ §d" + guardianName + " §7recolectó §a+" + energyGain + " Energía Vital §8(§e"
                                + data.getGuardianVitalEnergy() + "§7/" + PlayerRpgData.MAX_GUARDIAN_VITAL_ENERGY + "§8)"),
                        true);
            }
        }
    }

    private static boolean isEligibleGuardian(TamableAnimal entity) {
        if (!entity.isAlive() || !entity.isTame() || entity.getOwnerUUID() == null) return false;
        return entity instanceof StarryEntity || entity instanceof ChubbyEntity || entity instanceof BrambiEntity;
    }

    private static int computeVitalEnergy(LivingEntity victim) {
        float maxHp = victim.getMaxHealth();
        if (BossScalingHandler.isBoss(victim) || maxHp >= 500.0F) {
            return maxHp >= 1000.0F ? 100 : 75;
        }
        if (maxHp >= 150.0F) {
            return 35;
        }
        if (maxHp >= 50.0F) {
            return 20;
        }
        if (victim instanceof Monster || victim instanceof Enemy
                || victim.getType().getCategory() == MobCategory.MONSTER
                || victim instanceof BanditEntity
                || maxHp >= 15.0F) {
            return 10;
        }
        return 5;
    }

    private static void spawnVitalTrailParticles(ServerLevel level, LivingEntity victim, TamableAnimal guardian) {
        Vec3 start = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = guardian.position().add(0.0D, guardian.getBbHeight() * 0.5D + 0.1D, 0.0D);
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 p = start.lerp(end, t);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0.05D, 0.05D, 0.05D, 0.01D);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02D, 0.02D, 0.02D, 0.005D);
            }
        }
    }

    private static void dropLifeSeed(ServerLevel level, TamableAnimal guardian, ServerPlayer owner) {
        ItemEntity item = new ItemEntity(level, guardian.getX(), guardian.getY() + 0.55D, guardian.getZ(),
                new ItemStack(ModItems.LIFE_SEEDS.get(), 1));
        Vec3 diff = owner.position().subtract(guardian.position());
        Vec3 toOwner = diff.lengthSqr() > 1.0E-4 ? diff.normalize() : Vec3.ZERO;
        item.setDeltaMovement(toOwner.x * 0.14D, 0.28D, toOwner.z * 0.14D);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private static String resolveGuardianName(TamableAnimal guardian) {
        if (guardian instanceof StarryEntity) return "Starry";
        if (guardian instanceof ChubbyEntity) return "Chubby";
        if (guardian instanceof BrambiEntity) return "Brambi";
        return guardian.getName().getString();
    }
}
