package com.pablo.tenseicraft.mobs;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ThreatLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * Gestor de escalado y balanceo colosal para Jefes del mod Cataclysm, Vanilla y mods en general.
 * - Aplica un multiplicador de x10 en Vida Máxima para que los jefes ofrezcan un desafío épico
 *   a los aventureros con estadísticas y magia RPG avanzadas.
 * - Multiplica el Daño de Ataque por x2.5 (con mínimos acordes al rango de amenaza).
 * - Aumenta Armadura (+15), Dureza de Armadura (+12) y otorga 100% de Resistencia al Retroceso.
 * - Asigna rangos de amenaza característicos entre Rango A y Catástrofe (CATASTROPHE).
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class BossScalingHandler {

    public static final String NBT_BOSS_SCALED = "RpgBossScaled";
    public static final String NBT_ORIGINAL_HP = "RpgBossOriginalHp";

    private static final TagKey<net.minecraft.world.entity.EntityType<?>> C_BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("c", "bosses"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> FORGE_BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("forge", "bosses"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> NEOFORGE_BOSSES =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("neoforge", "bosses"));

    /**
     * Jefes de progresión propios de nuestro mod que poseen un Nivel de Amenaza
     * y estadísticas fijas por diseño y NO deben recibir el multiplicador global (x10 HP, x2.5 Daño).
     */
    public static boolean isFixedStatsModBoss(LivingEntity entity) {
        if (entity == null) return false;
        return entity instanceof com.pablo.tenseicraft.entities.WindMasterBossEntity
                || entity instanceof com.pablo.tenseicraft.entities.UndeadAdventurerBossEntity
                || entity instanceof com.pablo.tenseicraft.entities.CultistBossEntity
                || entity instanceof com.pablo.tenseicraft.entities.ReimuBossEntity;
    }

    /**
     * Determina si la entidad viva es un Jefe o Mini-jefe (de Cataclysm, Vanilla, TenseiCraft u otros mods).
     */
    public static boolean isBoss(LivingEntity entity) {
        if (entity == null || entity instanceof Player) return false;

        // No escalar mascotas domesticadas del jugador
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) return false;

        ResourceLocation loc = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (loc != null) {
            String ns = loc.getNamespace();
            String path = loc.getPath();

            // Excluir proyectiles, mascotas o partes
            if (path.contains("pet") || path.contains("baby") || path.contains("ministrosity")
                    || path.contains("part") || path.contains("projectile") || path.contains("effect")) {
                return false;
            }

            // 1. Jefes y Mini-jefes de L_Ender's Cataclysm
            if ("cataclysm".equals(ns)) {
                return isCataclysmBossOrMiniboss(path);
            }
        }

        // 2. Jefes de Vanilla
        if (entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof Warden
                || entity instanceof ElderGuardian) {
            return true;
        }

        // 3. Jefes del mod TenseiCraft
        if (entity instanceof com.pablo.tenseicraft.entities.WindMasterBossEntity
                || entity instanceof com.pablo.tenseicraft.entities.UndeadAdventurerBossEntity
                || entity instanceof com.pablo.tenseicraft.entities.CultistBossEntity
                || entity instanceof com.pablo.tenseicraft.entities.ReimuBossEntity
                || entity instanceof com.pablo.tenseicraft.entities.ColossalVoidbornEntity
                || entity instanceof com.pablo.tenseicraft.entities.VoidbornExecutorEntity) {
            return true;
        }

        // 4. Tags de jefes de la comunidad (c:bosses, forge:bosses, neoforge:bosses)
        if (entity.getType().is(C_BOSSES) || entity.getType().is(FORGE_BOSSES) || entity.getType().is(NEOFORGE_BOSSES)) {
            return true;
        }

        // 5. Detección genérica para jefes de otros mods (Vida base >= 150 y hostil)
        if (entity instanceof Enemy || entity instanceof Monster) {
            AttributeInstance maxHpAttr = entity.getAttribute(Attributes.MAX_HEALTH);
            if (maxHpAttr != null && maxHpAttr.getBaseValue() >= 150.0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Identifica los jefes y mini-jefes del mod Cataclysm.
     */
    public static boolean isCataclysmBossOrMiniboss(String path) {
        return switch (path) {
            case "the_leviathan",
                 "ignis",
                 "netherite_monstrosity",
                 "ender_guardian",
                 "the_harbinger",
                 "ancient_remnant",
                 "maledictus",
                 "scylla",
                 "ender_golem",
                 "ignited_revenant",
                 "the_prowler",
                 "kobolediator",
                 "wadjet",
                 "coralssus",
                 "amethyst_crab",
                 "aptrgangr",
                 "coral_golem",
                 "royal_draugr",
                 "the_watcher",
                 "ignited_berserker" -> true;
            default -> false;
        };
    }

    /**
     * Mapea cada jefe de Cataclysm a su Rango de Amenaza característico (entre Rango A y Catástrofe).
     */
    public static ThreatLevel getCataclysmThreat(String path) {
        return switch (path) {
            // Nivel Catástrofe: Dioses destructores de mundos
            case "the_leviathan", "ignis" -> ThreatLevel.CATASTROPHE;

            // Gran Desastre: Titanes ancestrales colosales
            case "netherite_monstrosity", "ender_guardian", "the_harbinger" -> ThreatLevel.GRAND_DISASTER;

            // Desastre: Reyes de ruinas y palacios caídos
            case "ancient_remnant", "maledictus", "scylla" -> ThreatLevel.DISASTER;

            // Calamidad: Mini-jefes mayores de fortalezas
            case "ender_golem", "ignited_revenant", "the_prowler" -> ThreatLevel.CALAMITY;

            // Alerta (Hazard): Guardianes de tumbas y campeones de arena
            case "kobolediator", "wadjet", "coralssus", "amethyst_crab" -> ThreatLevel.HAZARD;

            // Rango A: Élite de mazmorras y comandantes
            case "aptrgangr", "coral_golem", "royal_draugr", "the_watcher", "ignited_berserker" -> ThreatLevel.A;

            default -> ThreatLevel.A;
        };
    }

    /**
     * Calcula la experiencia base otorgada al derrotar al jefe (antes del escalado exponencial de rango).
     */
    public static int getBossBaseXp(LivingEntity entity) {
        ResourceLocation loc = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (loc != null && "cataclysm".equals(loc.getNamespace())) {
            String path = loc.getPath();
            return switch (path) {
                case "the_leviathan" -> 7000;
                case "ignis" -> 6500;
                case "netherite_monstrosity" -> 5500;
                case "ender_guardian", "the_harbinger" -> 5000;
                case "ancient_remnant", "maledictus", "scylla" -> 4200;
                case "ender_golem", "the_prowler", "ignited_revenant" -> 3200;
                case "kobolediator", "coralssus", "amethyst_crab", "wadjet" -> 2500;
                case "aptrgangr", "coral_golem", "royal_draugr", "the_watcher", "ignited_berserker" -> 1800;
                default -> 2000;
            };
        }

        if (entity instanceof EnderDragon) return 4500;
        if (entity instanceof Warden) return 4000;
        if (entity instanceof WitherBoss) return 3500;
        if (entity instanceof ElderGuardian) return 1200;

        // 1. Jefes propios del Mod (ordenados según su rango fijo: E -> D -> B -> Hazard)
        if (entity instanceof com.pablo.tenseicraft.entities.WindMasterBossEntity boss) {
            return boss.isEmpowered() ? 700 : 400;
        }
        if (entity instanceof com.pablo.tenseicraft.entities.UndeadAdventurerBossEntity) return 1100;
        if (entity instanceof com.pablo.tenseicraft.entities.CultistBossEntity) return 1800;
        if (entity instanceof com.pablo.tenseicraft.entities.ReimuBossEntity) return 3500;
        if (entity instanceof com.pablo.tenseicraft.entities.VoidbornExecutorEntity) return 1600;
        if (entity instanceof com.pablo.tenseicraft.entities.ColossalVoidbornEntity) return 2200;

        return 2000;
    }

    /**
     * Aplica el escalado x10 de vida, potenciación de daño, armadura y resistencia al empuje.
     */
    public static void scaleBoss(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        if (isFixedStatsModBoss(entity)) return;
        if (entity.getPersistentData().getBoolean(NBT_BOSS_SCALED)) return;

        // Marcar como escalado para garantizar idempotencia
        entity.getPersistentData().putBoolean(NBT_BOSS_SCALED, true);

        // 1. Escalado x10 de Vida Máxima
        AttributeInstance maxHpAttr = entity.getAttribute(Attributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            double originalHp = maxHpAttr.getBaseValue();
            entity.getPersistentData().putDouble(NBT_ORIGINAL_HP, originalHp);
            double newHp = originalHp * 10.0;
            maxHpAttr.setBaseValue(newHp);
            entity.setHealth((float) newHp);
        }

        // 2. Escalado x2.5 de Daño de Ataque (con mínimos garantizados según amenaza)
        ThreatLevel threat = ThreatLevel.determineThreatLevel(entity);
        AttributeInstance dmgAttr = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null && dmgAttr.getBaseValue() > 0.0) {
            double originalDmg = dmgAttr.getBaseValue();
            double minDmg = getMinimumDamageForThreat(threat);
            double newDmg = Math.max(originalDmg * 2.5, minDmg);
            dmgAttr.setBaseValue(newDmg);
        }

        // 3. Incremento de Armadura y Dureza
        AttributeInstance armorAttr = entity.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(armorAttr.getBaseValue() + 15.0);
        }

        AttributeInstance toughnessAttr = entity.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (toughnessAttr != null) {
            toughnessAttr.setBaseValue(toughnessAttr.getBaseValue() + 12.0);
        }

        // 4. Resistencia al empuje inamovible (100% Knockback Resistance)
        AttributeInstance kbAttr = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr.setBaseValue(1.0);
        }

        // Efectos al manifestarse en el mundo
        if (entity.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + 1.0, entity.getZ(), 2, 0.5, 0.5, 0.5, 0.0);
            sl.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.2F, 0.8F);
        }
    }

    private static double getMinimumDamageForThreat(ThreatLevel threat) {
        if (threat == null) return 25.0;
        return switch (threat) {
            case GENESIS, CATASTROPHE -> 75.0;
            case GRAND_DISASTER -> 65.0;
            case DISASTER -> 55.0;
            case CALAMITY -> 45.0;
            case HAZARD -> 38.0;
            case A -> 32.0;
            default -> 25.0;
        };
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!isBoss(living)) return;

        scaleBoss(living);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity living = event.getEntity();
        if (living == null || living.level().isClientSide()) return;
        if (isBoss(living) && !living.getPersistentData().getBoolean(NBT_BOSS_SCALED)) {
            scaleBoss(living);
        }
    }
}
