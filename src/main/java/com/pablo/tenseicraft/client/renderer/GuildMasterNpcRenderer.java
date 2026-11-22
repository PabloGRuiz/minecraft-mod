package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.GuildMasterNpcEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class GuildMasterNpcRenderer extends MobRenderer<GuildMasterNpcEntity, PlayerModel<GuildMasterNpcEntity>> {

    private static final ResourceLocation GUILD_MASTER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/npc/guild_master00.png");

    public GuildMasterNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        // Armor layers
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));

        // Held items layer
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(GuildMasterNpcEntity entity) {
        return GUILD_MASTER_TEXTURE;
    }

    @Override
    protected boolean shouldShowName(GuildMasterNpcEntity entity) {
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            return false;
        }
        return super.shouldShowName(entity);
    }
}

