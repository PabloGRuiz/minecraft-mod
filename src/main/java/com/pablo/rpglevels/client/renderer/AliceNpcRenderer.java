package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.AliceNpcEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class AliceNpcRenderer extends MobRenderer<AliceNpcEntity, PlayerModel<AliceNpcEntity>> {

    private static final ResourceLocation ALICE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/npc/alice.png");

    public AliceNpcRenderer(EntityRendererProvider.Context context) {
        // Slim model (Alex / female player model) fits Alice's texture perfectly
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);

        // Armor layers
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
                context.getModelManager()));

        // Held items layer
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AliceNpcEntity entity) {
        return ALICE_TEXTURE;
    }

    @Override
    protected boolean shouldShowName(AliceNpcEntity entity) {
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            return false;
        }
        return super.shouldShowName(entity);
    }
}

