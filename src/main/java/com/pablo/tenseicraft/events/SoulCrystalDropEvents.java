package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.items.ModItems;
import com.pablo.tenseicraft.mobs.MobRankHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class SoulCrystalDropEvents {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity instanceof Player) {
            return;
        }

        // Determinar si fue asesinado por un jugador (o bajo combate con un jugador)
        Player killer = null;
        if (event.getSource() != null && event.getSource().getEntity() instanceof Player p) {
            killer = p;
        } else if (entity.getKillCredit() instanceof Player p) {
            killer = p;
        } else if (entity.getLastAttacker() instanceof Player p) {
            killer = p;
        }

        float dropMultiplier = 1.0f;
        if (killer != null) {
            PlayerRpgData data = killer.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                int luck = data.getTotalStatPoints(killer, StatType.LUCK);
                // Cada punto de Suerte otorga +2.5% de probabilidad de drop de cristales
                dropMultiplier += (luck * 0.025f);
            }
            var lookup = killer.level().registryAccess().lookup(Registries.ENCHANTMENT);
            if (lookup.isPresent()) {
                var lootingHolder = lookup.get().get(Enchantments.LOOTING);
                if (lootingHolder.isPresent()) {
                    int looting = EnchantmentHelper.getItemEnchantmentLevel(lootingHolder.get(), killer.getMainHandItem());
                    if (looting > 0) {
                        dropMultiplier += (looting * 0.15f);
                    }
                }
            }
        }

        ThreatLevel threat = MobRankHandler.getThreatLevel(entity);
        RandomSource random = entity.getRandom();

        // 1. Fragmento de Alma Menor (Lesser Soul Fragment)
        // Criaturas pasivas / pacíficas y monstruos menores
        float lesserChance = switch (threat) {
            case F -> 0.06f; // Vacas, ovejas, cerdos, etc.
            case E -> 0.15f; // Zombies comunes, esqueletos, arañas
            case D -> 0.22f; // Creepers, etc.
            default -> 0.35f; // Mobs de rango C o superior
        };
        if (random.nextFloat() < (lesserChance * dropMultiplier)) {
            int count = (threat.ordinal() >= ThreatLevel.B.ordinal()) ? 1 + random.nextInt(2) : 1;
            addDrop(event, entity, new ItemStack(ModItems.LESSER_SOUL_FRAGMENT.get(), count));
        }

        // 2. Cristal de Alma Resonante (Resonant Soul Crystal)
        // Enemigos peligrosos a partir de Rango E (raro) hasta Rango A / Hazard
        float resonantChance = switch (threat) {
            case F -> 0.0f; // Animales pacíficos no sueltan cristal resonante
            case E -> 0.02f; // Muy raro en zombies
            case D -> 0.06f; // Endermans, brujas
            case C -> 0.14f; // Bandidos, vindicadores
            case B -> 0.30f; // Voidborn Rango B, jefes menores
            case A -> 0.55f; // Voidborn Rango A, Wind Master
            default -> 1.0f; // Hazard o superior: garantizado
        };
        if (random.nextFloat() < (resonantChance * dropMultiplier)) {
            int count = (threat.ordinal() >= ThreatLevel.HAZARD.ordinal()) ? 1 + random.nextInt(3) : 1;
            addDrop(event, entity, new ItemStack(ModItems.RESONANT_SOUL_CRYSTAL.get(), count));
            playDropEffects(entity, 2);
        }

        // 3. Gema de Alma Radiante (Radiant Soul Gem)
        // Monstruos de élite (Rango B/A), jefes Hazard (Voidborn Executor, Cultist, Warden)
        float radiantChance = switch (threat) {
            case F, E -> 0.0f;
            case D -> 0.003f; // Golpe de suerte extremo en mob común
            case C -> 0.015f;
            case B -> 0.08f;  // Voidborn B
            case A -> 0.25f;  // Voidborn A
            case HAZARD -> 0.65f; // Voidborn Executor, Cultista
            default -> 1.0f; // Calamity, Disaster, etc.: Garantizado
        };
        if (random.nextFloat() < (radiantChance * dropMultiplier)) {
            int count = (threat.ordinal() >= ThreatLevel.CALAMITY.ordinal()) ? 2 + random.nextInt(2) : 1;
            addDrop(event, entity, new ItemStack(ModItems.RADIANT_SOUL_GEM.get(), count));
            playDropEffects(entity, 3);
        }

        // 4. Núcleo de Alma Primordial (Primordial Soul Core)
        // Jefes Mundiales y Criaturas Supremas (Wither, Ender Dragon, Wardens, Voidborn Executor)
        float primordialChance = switch (threat) {
            case F, E, D, C -> 0.0f;
            case B -> 0.001f;
            case A -> 0.015f;
            case HAZARD -> 0.18f; // Voidborn Executor, Cultista
            default -> 1.0f; // Calamity, Disaster, Catastrophe, Genesis (Wither, Ender Dragon): 100% Garantizado
        };
        if (random.nextFloat() < (primordialChance * dropMultiplier)) {
            int count = (threat.ordinal() >= ThreatLevel.DISASTER.ordinal()) ? 2 : 1;
            addDrop(event, entity, new ItemStack(ModItems.PRIMORDIAL_SOUL_CORE.get(), count));
            playDropEffects(entity, 4);
        }
    }

    private static void addDrop(LivingDropsEvent event, LivingEntity entity, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.35, entity.getZ(), stack);
        itemEntity.setDefaultPickUpDelay();
        event.getDrops().add(itemEntity);
    }

    private static void playDropEffects(LivingEntity entity, int tier) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        double x = entity.getX();
        double y = entity.getY() + 0.5;
        double z = entity.getZ();

        if (tier == 2) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 8, 0.25, 0.25, 0.25, 0.02);
            serverLevel.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 1.3f);
        } else if (tier == 3) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 14, 0.3, 0.3, 0.3, 0.05);
            serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 6, 0.2, 0.2, 0.2, 0.02);
            serverLevel.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.9f);
        } else if (tier == 4) {
            serverLevel.sendParticles(ParticleTypes.FLASH, x, y, z, 2, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.SOUL, x, y, z, 20, 0.4, 0.4, 0.4, 0.04);
            serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 16, 0.35, 0.35, 0.35, 0.06);
            serverLevel.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
            serverLevel.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.5f);
        }
    }
}
