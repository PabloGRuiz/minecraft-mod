package com.pablo.tenseicraft;

import com.mojang.logging.LogUtils;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.entities.AdventurerNpcEntity;
import com.pablo.tenseicraft.entities.ModEntities;
import com.pablo.tenseicraft.entities.TownswomanNpcEntity;
import com.pablo.tenseicraft.items.ModCreativeTabs;
import com.pablo.tenseicraft.items.ModItems;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.pablo.tenseicraft.config.RpgClientConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(TenseiCraftMod.MOD_ID)
public class TenseiCraftMod {
    public static final String MOD_ID = "tenseicraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TenseiCraftMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Inicializando TenseiCraft Mod para Minecraft 1.21.1 en NeoForge...");

        // Register Config
        modContainer.registerConfig(ModConfig.Type.CLIENT, RpgClientConfig.SPEC);

        // Register Blocks, Items & Creative Tab
        com.pablo.tenseicraft.init.ModBlocks.BLOCKS.register(modEventBus);
        com.pablo.tenseicraft.init.ModMobEffects.MOB_EFFECTS.register(modEventBus);
        com.pablo.tenseicraft.init.ModSounds.SOUND_EVENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Register Entities
        ModEntities.ENTITIES.register(modEventBus);

        // Register Attachment Types
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register Spiritual Magic School & Attributes (Iron's Spells Integration)
        com.pablo.tenseicraft.magic.spiritual.SpiritualAttributes.ATTRIBUTES.register(modEventBus);
        com.pablo.tenseicraft.magic.spiritual.SpiritualSchool.SCHOOLS.register(modEventBus);
        com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SPELLS.register(modEventBus);
        modEventBus.addListener(com.pablo.tenseicraft.magic.spiritual.SpiritualAttributes::modifyEntityAttributes);

        // Register Common Setup (eleva el límite vanilla 1024 de MAX_HEALTH)
        modEventBus.addListener(this::commonSetup);

        // Register Entity Attributes
        modEventBus.addListener(this::registerEntityAttributes);

        // Register Spawn Placements
        modEventBus.addListener(this::registerSpawnPlacements);

        // Soft Integrations
        com.pablo.tenseicraft.compat.irons.IronsSpellsCompat.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TenseiCraftMod::raiseMaxHealthAttributeCap);
    }

    /**
     * Minecraft vanilla fija {@link RangedAttribute#maxValue} de {@link Attributes#MAX_HEALTH} en 1024.0,
     * lo cual recorta silenciosamente la vida de jefes como Reimu (5000 HP). Lo elevamos en cliente y servidor.
     */
    private static void raiseMaxHealthAttributeCap() {
        try {
            Attribute attr = Attributes.MAX_HEALTH.value();
            if (attr instanceof RangedAttribute ranged && ranged.getMaxValue() < 1_000_000.0D) {
                double oldMax = ranged.getMaxValue();
                Field maxField = null;
                try {
                    maxField = RangedAttribute.class.getDeclaredField("maxValue");
                } catch (NoSuchFieldException ignored) {
                    for (Field candidate : RangedAttribute.class.getDeclaredFields()) {
                        if (candidate.getType() == double.class) {
                            candidate.setAccessible(true);
                            if (Math.abs(candidate.getDouble(ranged) - oldMax) < 1.0E-6) {
                                maxField = candidate;
                                break;
                            }
                        }
                    }
                }
                if (maxField != null) {
                    if (!Modifier.isPublic(maxField.getModifiers()) || Modifier.isFinal(maxField.getModifiers())) {
                        maxField.setAccessible(true);
                    }
                    maxField.setDouble(ranged, 1_000_000.0D);
                    LOGGER.info("TenseiCraft: Attributes.MAX_HEALTH cap actualizado de {} a {} (test sanitize 5000 -> {})",
                            oldMax, ranged.getMaxValue(), ranged.sanitizeValue(5000.0D));
                } else {
                    LOGGER.warn("TenseiCraft: No se pudo localizar el campo maxValue de RangedAttribute.");
                }
            }
        } catch (Throwable t) {
            LOGGER.error("TenseiCraft: Error al elevar el límite máximo de Attributes.MAX_HEALTH", t);
        }
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ADVENTURER_NPC.get(), AdventurerNpcEntity.createAttributes().build());
        event.put(ModEntities.TOWNSWOMAN_NPC.get(), TownswomanNpcEntity.createAttributes().build());
        event.put(ModEntities.BANDIT.get(), com.pablo.tenseicraft.entities.BanditEntity.createAttributes().build());
        event.put(ModEntities.UNDEAD_ADVENTURER.get(), com.pablo.tenseicraft.entities.UndeadAdventurerBossEntity.createAttributes().build());
        event.put(ModEntities.WIND_MASTER.get(), com.pablo.tenseicraft.entities.WindMasterBossEntity.createAttributes().build());
        event.put(ModEntities.WIND_MASTER_NPC.get(), com.pablo.tenseicraft.entities.WindMasterNpcEntity.createAttributes().build());
        event.put(ModEntities.GUILD_MASTER.get(), com.pablo.tenseicraft.entities.GuildMasterNpcEntity.createAttributes().build());
        event.put(ModEntities.ALICE.get(), com.pablo.tenseicraft.entities.AliceNpcEntity.createAttributes().build());
        event.put(ModEntities.SWORD_SPIRIT.get(), com.pablo.tenseicraft.entities.SwordSpiritEntity.createAttributes().build());
        event.put(ModEntities.CULTIST.get(), com.pablo.tenseicraft.entities.CultistBossEntity.createAttributes().build());
        event.put(ModEntities.PELOTITA.get(), com.pablo.tenseicraft.entities.PelotitaEntity.createAttributes().build());
        event.put(ModEntities.VOIDBORN.get(), com.pablo.tenseicraft.entities.VoidbornEntity.createAttributes().build());
        event.put(ModEntities.VOIDBORN_EXECUTOR.get(), com.pablo.tenseicraft.entities.VoidbornExecutorEntity.createAttributes().build());
        event.put(ModEntities.HORNLESS_VOIDBORN.get(), com.pablo.tenseicraft.entities.HornlessVoidbornEntity.createAttributes().build());
        event.put(ModEntities.HORNED_VOIDBORN.get(), com.pablo.tenseicraft.entities.HornedVoidbornEntity.createAttributes().build());
        event.put(ModEntities.COLOSSAL_VOIDBORN.get(), com.pablo.tenseicraft.entities.ColossalVoidbornEntity.createAttributes().build());
        event.put(ModEntities.STARRY.get(), com.pablo.tenseicraft.entities.StarryEntity.createAttributes().build());
        event.put(ModEntities.BRAMBI.get(), com.pablo.tenseicraft.entities.BrambiEntity.createAttributes().build());
        event.put(ModEntities.CHUBBY.get(), com.pablo.tenseicraft.entities.ChubbyEntity.createAttributes().build());
        event.put(ModEntities.REDFOX.get(), com.pablo.tenseicraft.entities.RedfoxEntity.createAttributes().build());
        event.put(ModEntities.MEGU.get(), com.pablo.tenseicraft.entities.MeguNpcEntity.createAttributes().build());
        event.put(ModEntities.REIMU_BOSS.get(), com.pablo.tenseicraft.entities.ReimuBossEntity.createAttributes().build());
        event.put(ModEntities.REIMU_NPC.get(), com.pablo.tenseicraft.entities.ReimuNpcEntity.createAttributes().build());
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.TOWNSWOMAN_NPC.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TownswomanNpcEntity::checkTownswomanSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.ADVENTURER_NPC.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TownswomanNpcEntity::checkTownswomanSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.REDFOX.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TownswomanNpcEntity::checkTownswomanSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.MEGU.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                TownswomanNpcEntity::checkTownswomanSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BANDIT.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VOIDBORN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.pablo.tenseicraft.entities.VoidbornEntity::checkVoidbornSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VOIDBORN_EXECUTOR.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.pablo.tenseicraft.entities.VoidbornEntity::checkVoidbornSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HORNLESS_VOIDBORN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.pablo.tenseicraft.entities.VoidbornEntity::checkVoidbornSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.HORNED_VOIDBORN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.pablo.tenseicraft.entities.VoidbornEntity::checkVoidbornSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.COLOSSAL_VOIDBORN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.pablo.tenseicraft.entities.VoidbornEntity::checkVoidbornSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.STARRY.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.BRAMBI.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CHUBBY.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.animal.Animal::checkAnimalSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
