package com.pablo.tenseicraft.client.renderer;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.entities.ReimuBossEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class ReimuBossRenderer extends MobRenderer<ReimuBossEntity, PlayerModel<ReimuBossEntity>> {

    private static final ResourceLocation REIMU_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "textures/entity/npc/reimu.png");

    public ReimuBossRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(ReimuBossEntity entity) {
        return REIMU_TEXTURE;
    }

    @Override
    protected boolean shouldShowName(ReimuBossEntity entity) {
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            return false;
        }
        return super.shouldShowName(entity);
    }
}
