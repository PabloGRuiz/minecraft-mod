package com.pablo.rpglevels.client;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.client.renderer.AdventurerNpcRenderer;
import com.pablo.rpglevels.client.renderer.BanditRenderer;
import com.pablo.rpglevels.client.renderer.TownswomanNpcRenderer;
import com.pablo.rpglevels.client.renderer.VoidbornRenderer;
import com.pablo.rpglevels.entities.ModEntities;
import com.pablo.rpglevels.items.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID, value = Dist.CLIENT)
public class ClientEntityEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ModItems.SPIRITUAL_SWORD.get(),
                    ResourceLocation.withDefaultNamespace("blocking"),
                    (stack, level, entity,
                            seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F
                                    : 0.0F);

            // Registrar modelo 3D en la cadera para el Grimorio Espiritual en Curios
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(
                    ModItems.SPIRITUAL_SPELLBOOK.get(),
                    io.redspace.ironsspellbooks.render.SpellBookCurioRenderer::new);

            // Capa de renderizado translúcida para el Portal del Abismo
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    com.pablo.rpglevels.init.ModBlocks.ABYSS_PORTAL.get(),
                    net.minecraft.client.renderer.RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.PelotitaModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.PelotitaModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.VoidbornModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.VoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.HornlessVoidbornModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.HornlessVoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.HornedVoidbornModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.HornedVoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.ColossalVoidbornModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.ColossalVoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.StarryModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.StarryModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.BrambiModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.BrambiModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.rpglevels.client.model.ChubbyModel.LAYER_LOCATION,
                com.pablo.rpglevels.client.model.ChubbyModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ADVENTURER_NPC.get(), AdventurerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.TOWNSWOMAN_NPC.get(), TownswomanNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.BANDIT.get(), BanditRenderer::new);
        event.registerEntityRenderer(ModEntities.UNDEAD_ADVENTURER.get(),
                com.pablo.rpglevels.client.renderer.UndeadAdventurerBossRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_MASTER.get(),
                com.pablo.rpglevels.client.renderer.WindMasterBossRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_MASTER_NPC.get(),
                com.pablo.rpglevels.client.renderer.WindMasterNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.GUILD_MASTER.get(),
                com.pablo.rpglevels.client.renderer.GuildMasterNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.ALICE.get(),
                com.pablo.rpglevels.client.renderer.AliceNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_SPIRIT.get(),
                com.pablo.rpglevels.client.renderer.SwordSpiritRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIST.get(),
                com.pablo.rpglevels.client.renderer.CultistBossRenderer::new);
        event.registerEntityRenderer(ModEntities.PELOTITA.get(),
                com.pablo.rpglevels.client.renderer.PelotitaRenderer::new);
        event.registerEntityRenderer(ModEntities.VOIDBORN.get(), VoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.VOIDBORN_EXECUTOR.get(), VoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.HORNLESS_VOIDBORN.get(),
                com.pablo.rpglevels.client.renderer.HornlessVoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.HORNED_VOIDBORN.get(),
                com.pablo.rpglevels.client.renderer.HornedVoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.COLOSSAL_VOIDBORN.get(),
                com.pablo.rpglevels.client.renderer.ColossalVoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.STARRY.get(),
                com.pablo.rpglevels.client.renderer.StarryRenderer::new);
        event.registerEntityRenderer(ModEntities.BRAMBI.get(),
                com.pablo.rpglevels.client.renderer.BrambiRenderer::new);
        event.registerEntityRenderer(ModEntities.CHUBBY.get(),
                com.pablo.rpglevels.client.renderer.ChubbyRenderer::new);
        event.registerEntityRenderer(ModEntities.HOLLOW_ROCK.get(),
                (ctx) -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 2.2f, false));
        event.registerEntityRenderer(ModEntities.SPIRITUAL_BEAM.get(),
                com.pablo.rpglevels.client.renderer.SpiritualBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGIC_FIREBALL.get(),
                (ctx) -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.4f, true));
        event.registerEntityRenderer(ModEntities.MAGIC_SPELL_PROJECTILE.get(),
                (ctx) -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.4f, true));
        event.registerEntityRenderer(ModEntities.THROWN_HIGH_EXPERIENCE_BOTTLE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
}
