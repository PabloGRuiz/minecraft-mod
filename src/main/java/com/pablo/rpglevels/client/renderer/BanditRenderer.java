package com.pablo.rpglevels.client.renderer;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.BanditEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class BanditRenderer extends MobRenderer<BanditEntity, RpgCombatPlayerModel<BanditEntity>> {

    private static final ResourceLocation[] SKINS = new ResourceLocation[] {
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/npc/bandit_0.png"),
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/npc/bandit_1.png"),
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/npc/bandit_2.png")
    };

    private static final ResourceLocation LEADER_SKIN =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "textures/entity/npc/bandit_leader.png");

    public BanditRenderer(EntityRendererProvider.Context context) {
        super(context, new RpgCombatPlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);

        // Armor layers (inner & outer armor)
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));

        // Held items layer (bows, swords, shields)
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(BanditEntity entity) {
        if (entity.isLeader()) {
            return LEADER_SKIN;
        }

        int variant = entity.getSkinVariant();
        if (variant < 0 || variant >= SKINS.length) {
            variant = 0;
        }
        return SKINS[variant];
    }

    @Override
    protected boolean shouldShowName(BanditEntity entity) {
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            return false;
        }
        return super.shouldShowName(entity);
    }
}
