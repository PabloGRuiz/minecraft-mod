package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.entities.BrambiEntity;
import com.pablo.tenseicraft.entities.ChubbyEntity;
import com.pablo.tenseicraft.entities.ModEntities;
import com.pablo.tenseicraft.entities.StarryEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Paquete C->S para invocar o desinvocar al Guardián Espiritual elegido (Tecla C).
 * - Coste mínimo: 50 MP.
 * - Escala en poder exclusivamente según el Nivel RPG del jugador (vida, daño y armadura).
 */
public record SummonGuardianPayload() implements CustomPacketPayload {

    public static final Type<SummonGuardianPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "summon_guardian"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SummonGuardianPayload> STREAM_CODEC =
            StreamCodec.unit(new SummonGuardianPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SummonGuardianPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) return;

            // 1. Verificar si tiene guardián seleccionado
            if (!data.hasChosenGuardian()) {
                player.displayClientMessage(Component.literal("§cNo tienes ningún Guardián vinculado. Recibirás tu Guardián al alcanzar el Hito de Rango B en el Espacio Astral."), true);
                player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                return;
            }

            // 2. Verificar si ya tiene un guardián invocado en el mundo para desinvocarlo (Toggle)
            AABB searchBox = player.getBoundingBox().inflate(48.0);
            List<TamableAnimal> nearbyGuardians = level.getEntitiesOfClass(TamableAnimal.class, searchBox,
                    e -> e.isAlive() && e.getTags().contains("SummonedGuardian") && player.getUUID().equals(e.getOwnerUUID()));

            if (!nearbyGuardians.isEmpty()) {
                // Desinvocar guardián existente
                for (TamableAnimal guardian : nearbyGuardians) {
                    level.sendParticles(ParticleTypes.POOF, guardian.getX(), guardian.getY() + 0.5, guardian.getZ(), 25, 0.3, 0.4, 0.3, 0.05);
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, guardian.getX(), guardian.getY() + 0.5, guardian.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
                    guardian.discard();
                }
                data.setSummonedGuardianUuid(null);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.6f);
                player.displayClientMessage(Component.literal("§7Has devuelto a tu Guardián a tu mundo espiritual."), true);
                return;
            }

            // 3. Verificar Maná (Mínimo 50 MP * multiplicador de Densidad del Alma)
            float summonManaCost = 50.0f * data.getSkillManaCostMultiplier();
            float currentMana = data.getCurrentMana(player);
            if (currentMana < summonManaCost) {
                player.displayClientMessage(Component.literal("§c¡Maná insuficiente! Se requieren al menos §b" + (int) Math.ceil(summonManaCost) + " MP §c(Tienes §e" + Math.round(currentMana) + " MP§c)."), true);
                player.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.2f);
                return;
            }

            // Consumir MP
            data.consumeMana(player, summonManaCost);
            ModNetworking.syncMana(player);

            // 4. Instanciar la criatura elegida
            String guardianType = data.getChosenGuardian();
            TamableAnimal guardian;
            String defaultName;

            if ("starry".equals(guardianType)) {
                guardian = new StarryEntity(ModEntities.STARRY.get(), level);
                defaultName = "Starry";
            } else if ("chubby".equals(guardianType)) {
                guardian = new ChubbyEntity(ModEntities.CHUBBY.get(), level);
                defaultName = "Chubby";
            } else {
                guardian = new BrambiEntity(ModEntities.BRAMBI.get(), level);
                defaultName = "Brambi";
            }

            // 5. Vincular al jugador
            guardian.tame(player);
            guardian.setOwnerUUID(player.getUUID());
            guardian.setOrderedToSit(false);
            guardian.addTag("SummonedGuardian");
            guardian.addTag("Owner_" + player.getUUID());

            // 6. Escalado EXCLUSIVO según Nivel RPG del Jugador (sin ninguna otra stat)
            int playerLevel = Math.max(1, data.getRpgLevel());
            double scaledMaxHealth = 26.0 + (playerLevel * 2.5); // Nivel 45 = 138.5 HP, Nivel 60 = 176 HP, Nivel 90 = 251 HP
            double scaledDamage = 6.0 + (playerLevel * 0.35);    // Nivel 45 = 21.75 Daño, Nivel 60 = 27 Daño
            double scaledArmor = 4.0 + (playerLevel * 0.20);     // Nivel 45 = 13 Armadura, Nivel 60 = 16 Armadura

            if (guardian.getAttribute(Attributes.MAX_HEALTH) != null) {
                guardian.getAttribute(Attributes.MAX_HEALTH).setBaseValue(scaledMaxHealth);
            }
            if (guardian.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                guardian.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(scaledDamage);
            }
            if (guardian.getAttribute(Attributes.ARMOR) != null) {
                guardian.getAttribute(Attributes.ARMOR).setBaseValue(scaledArmor);
            }
            guardian.setHealth((float) scaledMaxHealth);

            guardian.setCustomName(Component.literal("§d✦ " + defaultName + " §e[Nv. " + playerLevel + "] §d✦"));
            guardian.setCustomNameVisible(true);

            // Posicionar junto al jugador
            guardian.moveTo(player.getX() + 1.2, player.getY(), player.getZ() + 1.2, player.getYRot(), 0.0f);
            level.addFreshEntity(guardian);
            data.setSummonedGuardianUuid(guardian.getUUID());

            // Efectos de invocación
            level.sendParticles(ParticleTypes.EXPLOSION, guardian.getX(), guardian.getY() + 0.5, guardian.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, guardian.getX(), guardian.getY() + 0.5, guardian.getZ(), 35, 0.4, 0.5, 0.4, 0.12);
            level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.2f, 1.2f);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 1.0f);

            player.displayClientMessage(Component.literal(
                    "§a✦ ¡Has convocado a " + defaultName + " [Nv. " + playerLevel + "]! §b[-50 MP] §8| §aEnergía Vital: §e"
                            + data.getGuardianVitalEnergy() + "§7/" + PlayerRpgData.MAX_GUARDIAN_VITAL_ENERGY
                            + " §7([C] para guardar)"), true);
        });
    }
}
