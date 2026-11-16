package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.CultistBossEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class CultistBossRenderer extends MobRenderer<CultistBossEntity, RpgCombatPlayerModel<CultistBossEntity>> {

    private static final ResourceLocation BOSS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/npc/boss02_cultist.png");

    public CultistBossRenderer(EntityRendererProvider.Context context) {
        super(context, new RpgCombatPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        // Armor layers
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));

        // Held items layer (Spiritual Sword, Cultist Sword)
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CultistBossEntity entity) {
        return BOSS_TEXTURE;
    }

    @Override
    protected boolean shouldShowName(CultistBossEntity entity) {
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            return false;
        }
        return super.shouldShowName(entity);
    }
}

