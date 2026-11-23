package com.pablo.tenseicraft.client;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.client.renderer.AdventurerNpcRenderer;
import com.pablo.tenseicraft.client.renderer.BanditRenderer;
import com.pablo.tenseicraft.client.renderer.TownswomanNpcRenderer;
import com.pablo.tenseicraft.client.renderer.VoidbornRenderer;
import com.pablo.tenseicraft.entities.ModEntities;
import com.pablo.tenseicraft.items.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID, value = Dist.CLIENT)
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
                    com.pablo.tenseicraft.init.ModBlocks.ABYSS_PORTAL.get(),
                    net.minecraft.client.renderer.RenderType.translucent());

            // Capa de renderizado cutout para el cultivo de la Flor de la Vida
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    com.pablo.tenseicraft.init.ModBlocks.LIFE_FLOWER_CROP.get(),
                    net.minecraft.client.renderer.RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.PelotitaModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.PelotitaModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.VoidbornModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.VoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.HornlessVoidbornModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.HornlessVoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.HornedVoidbornModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.HornedVoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.ColossalVoidbornModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.ColossalVoidbornModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.StarryModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.StarryModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.BrambiModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.BrambiModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.ChubbyModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.ChubbyModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.RedfoxModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.RedfoxModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.HumoModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.HumoModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.EnergyAttackModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.EnergyAttackModel::createBodyLayer);
        event.registerLayerDefinition(com.pablo.tenseicraft.client.model.CinnamorollModel.LAYER_LOCATION,
                com.pablo.tenseicraft.client.model.CinnamorollModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new com.pablo.tenseicraft.client.renderer.HumoPlayerLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ADVENTURER_NPC.get(), AdventurerNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.TOWNSWOMAN_NPC.get(), TownswomanNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.BANDIT.get(), BanditRenderer::new);
        event.registerEntityRenderer(ModEntities.UNDEAD_ADVENTURER.get(),
                com.pablo.tenseicraft.client.renderer.UndeadAdventurerBossRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_MASTER.get(),
                com.pablo.tenseicraft.client.renderer.WindMasterBossRenderer::new);
        event.registerEntityRenderer(ModEntities.WIND_MASTER_NPC.get(),
                com.pablo.tenseicraft.client.renderer.WindMasterNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.GUILD_MASTER.get(),
                com.pablo.tenseicraft.client.renderer.GuildMasterNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.ALICE.get(),
                com.pablo.tenseicraft.client.renderer.AliceNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.SWORD_SPIRIT.get(),
                com.pablo.tenseicraft.client.renderer.SwordSpiritRenderer::new);
        event.registerEntityRenderer(ModEntities.CULTIST.get(),
                com.pablo.tenseicraft.client.renderer.CultistBossRenderer::new);
        event.registerEntityRenderer(ModEntities.PELOTITA.get(),
                com.pablo.tenseicraft.client.renderer.PelotitaRenderer::new);
        event.registerEntityRenderer(ModEntities.VOIDBORN.get(), VoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.VOIDBORN_EXECUTOR.get(), VoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.HORNLESS_VOIDBORN.get(),
                com.pablo.tenseicraft.client.renderer.HornlessVoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.HORNED_VOIDBORN.get(),
                com.pablo.tenseicraft.client.renderer.HornedVoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.COLOSSAL_VOIDBORN.get(),
                com.pablo.tenseicraft.client.renderer.ColossalVoidbornRenderer::new);
        event.registerEntityRenderer(ModEntities.STARRY.get(),
                com.pablo.tenseicraft.client.renderer.StarryRenderer::new);
        event.registerEntityRenderer(ModEntities.BRAMBI.get(),
                com.pablo.tenseicraft.client.renderer.BrambiRenderer::new);
        event.registerEntityRenderer(ModEntities.CHUBBY.get(),
                com.pablo.tenseicraft.client.renderer.ChubbyRenderer::new);
        event.registerEntityRenderer(ModEntities.REDFOX.get(),
                com.pablo.tenseicraft.client.renderer.RedfoxRenderer::new);
        event.registerEntityRenderer(ModEntities.MEGU.get(),
                com.pablo.tenseicraft.client.renderer.MeguNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.ENERGY_ATTACK.get(),
                com.pablo.tenseicraft.client.renderer.EnergyAttackRenderer::new);
        event.registerEntityRenderer(ModEntities.REIMU_BOSS.get(),
                com.pablo.tenseicraft.client.renderer.ReimuBossRenderer::new);
        event.registerEntityRenderer(ModEntities.REIMU_NPC.get(),
                com.pablo.tenseicraft.client.renderer.ReimuNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.CINNAMOROLL.get(),
                com.pablo.tenseicraft.client.renderer.CinnamorollRenderer::new);
        event.registerEntityRenderer(ModEntities.HOLLOW_ROCK.get(),
                (ctx) -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 2.2f, false));
        event.registerEntityRenderer(ModEntities.SPIRITUAL_BEAM.get(),
                com.pablo.tenseicraft.client.renderer.SpiritualBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGIC_FIREBALL.get(),
                (ctx) -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.4f, true));
        event.registerEntityRenderer(ModEntities.MAGIC_SPELL_PROJECTILE.get(),
                (ctx) -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.4f, true));
        event.registerEntityRenderer(ModEntities.THROWN_HIGH_EXPERIENCE_BOTTLE.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
    }
}
