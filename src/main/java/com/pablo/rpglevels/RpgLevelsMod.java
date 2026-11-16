package com.pablo.rpglevels;

import com.mojang.logging.LogUtils;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.entities.ModEntities;
import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import com.pablo.rpglevels.items.ModCreativeTabs;
import com.pablo.rpglevels.items.ModItems;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import org.slf4j.Logger;

import com.pablo.rpglevels.config.RpgClientConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(RpgLevelsMod.MOD_ID)
public class RpgLevelsMod {
    public static final String MOD_ID = "rpglevels";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RpgLevelsMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Inicializando RPG Levels Mod para Minecraft 1.21.1 en NeoForge...");

        // Register Config
        modContainer.registerConfig(ModConfig.Type.CLIENT, RpgClientConfig.SPEC);

        // Register Blocks, Items & Creative Tab
        com.pablo.rpglevels.init.ModBlocks.BLOCKS.register(modEventBus);
        com.pablo.rpglevels.init.ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Register Entities
        ModEntities.ENTITIES.register(modEventBus);

        // Register Attachment Types
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register Spiritual Magic School & Attributes (Iron's Spells Integration)
        com.pablo.rpglevels.magic.spiritual.SpiritualAttributes.ATTRIBUTES.register(modEventBus);
        com.pablo.rpglevels.magic.spiritual.SpiritualSchool.SCHOOLS.register(modEventBus);
        com.pablo.rpglevels.magic.spiritual.SpiritualSpells.SPELLS.register(modEventBus);
        modEventBus.addListener(com.pablo.rpglevels.magic.spiritual.SpiritualAttributes::modifyEntityAttributes);

        // Register Entity Attributes
        modEventBus.addListener(this::registerEntityAttributes);

        // Register Spawn Placements
        modEventBus.addListener(this::registerSpawnPlacements);

        // Soft Integrations
        com.pablo.rpglevels.compat.irons.IronsSpellsCompat.init();
    }

    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ADVENTURER_NPC.get(), AdventurerNpcEntity.createAttributes().build());
        event.put(ModEntities.TOWNSWOMAN_NPC.get(), TownswomanNpcEntity.createAttributes().build());
        event.put(ModEntities.BANDIT.get(), com.pablo.rpglevels.entities.BanditEntity.createAttributes().build());
        event.put(ModEntities.UNDEAD_ADVENTURER.get(), com.pablo.rpglevels.entities.UndeadAdventurerBossEntity.createAttributes().build());
        event.put(ModEntities.WIND_MASTER.get(), com.pablo.rpglevels.entities.WindMasterBossEntity.createAttributes().build());
        event.put(ModEntities.WIND_MASTER_NPC.get(), com.pablo.rpglevels.entities.WindMasterNpcEntity.createAttributes().build());
        event.put(ModEntities.GUILD_MASTER.get(), com.pablo.rpglevels.entities.GuildMasterNpcEntity.createAttributes().build());
        event.put(ModEntities.ALICE.get(), com.pablo.rpglevels.entities.AliceNpcEntity.createAttributes().build());
        event.put(ModEntities.SWORD_SPIRIT.get(), com.pablo.rpglevels.entities.SwordSpiritEntity.createAttributes().build());
        event.put(ModEntities.CULTIST.get(), com.pablo.rpglevels.entities.CultistBossEntity.createAttributes().build());
        event.put(ModEntities.PELOTITA.get(), com.pablo.rpglevels.entities.PelotitaEntity.createAttributes().build());
        event.put(ModEntities.VOIDBORN.get(), com.pablo.rpglevels.entities.VoidbornEntity.createAttributes().build());
        event.put(ModEntities.VOIDBORN_EXECUTOR.get(), com.pablo.rpglevels.entities.VoidbornExecutorEntity.createAttributes().build());
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
        event.register(ModEntities.BANDIT.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VOIDBORN.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.VOIDBORN_EXECUTOR.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
