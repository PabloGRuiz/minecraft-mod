package com.pablo.tenseicraft.world.gen;

import com.mojang.datafixers.util.Pair;
import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class VillagePoolModifier {

    private static final ResourceLocation PLAINS_HOUSES_POOL = ResourceLocation.parse("minecraft:village/plains/houses");
    private static final ResourceLocation GUILD_HOUSE_RL = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "village/plains/houses/guild_house");

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        try {
            Registry<StructureTemplatePool> templatePoolRegistry = event.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
            StructureTemplatePool pool = templatePoolRegistry.get(PLAINS_HOUSES_POOL);
            if (pool == null) {
                TenseiCraftMod.LOGGER.warn("Could not find plains houses pool to inject Guild House!");
                return;
            }

            Registry<StructureProcessorList> processorRegistry = event.getServer().registryAccess().registryOrThrow(Registries.PROCESSOR_LIST);
            Holder<StructureProcessorList> emptyProcessor = processorRegistry.getHolderOrThrow(
                    ResourceKey.create(Registries.PROCESSOR_LIST, ResourceLocation.parse("minecraft:empty"))
            );

            // Create piece
            StructurePoolElement piece = StructurePoolElement.legacy(GUILD_HOUSE_RL.toString(), emptyProcessor)
                    .apply(StructureTemplatePool.Projection.RIGID);

            int weight = 6; // High enough to appear in plains villages!

            // Inject into pool
            addPieceToPool(pool, piece, weight);
            TenseiCraftMod.LOGGER.info("Successfully injected Guild House into Plains Village houses pool (weight: {})", weight);
        } catch (Throwable t) {
            TenseiCraftMod.LOGGER.error("Failed to inject Guild House into plains village pool: ", t);
        }
    }

    private static void addPieceToPool(StructureTemplatePool pool, StructurePoolElement piece, int weight) throws Exception {
        // Find rawTemplates and templates in StructureTemplatePool
        Field rawTemplatesField = null;
        Field templatesField = null;

        for (Field f : StructureTemplatePool.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                Object val = f.get(pool);
                if (val instanceof List<?> list && !list.isEmpty()) {
                    Object first = list.get(0);
                    if (first instanceof Pair) {
                        rawTemplatesField = f;
                    } else if (first instanceof StructurePoolElement) {
                        templatesField = f;
                    }
                }
            }
        }

        if (rawTemplatesField != null) {
            @SuppressWarnings("unchecked")
            List<Pair<StructurePoolElement, Integer>> raw = (List<Pair<StructurePoolElement, Integer>>) rawTemplatesField.get(pool);
            for (Pair<StructurePoolElement, Integer> p : raw) {
                if (p.getFirst().toString().contains("guild_house")) {
                    TenseiCraftMod.LOGGER.info("Guild House is already present in Plains Village pool (loaded via datapack/json).");
                    return;
                }
            }
            List<Pair<StructurePoolElement, Integer>> mutableRaw = new ArrayList<>(raw);
            mutableRaw.add(Pair.of(piece, weight));
            rawTemplatesField.set(pool, mutableRaw);
        }

        if (templatesField != null) {
            @SuppressWarnings("unchecked")
            List<StructurePoolElement> templates = (List<StructurePoolElement>) templatesField.get(pool);
            // templates field in vanilla is ObjectArrayList
            for (int i = 0; i < weight; i++) {
                templates.add(piece);
            }
        }
    }
}
