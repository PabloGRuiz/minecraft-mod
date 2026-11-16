package com.pablo.rpglevels.compat.jade;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ThreatLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum MobRankJadeProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "mob_rank");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        if (!(entity instanceof LivingEntity living) || entity instanceof Player) {
            return;
        }

        if (living instanceof Monster || living instanceof Enemy || living instanceof com.pablo.rpglevels.entities.BanditEntity) {
            ThreatLevel threat = ThreatLevel.determineThreatLevel(living);
            if (com.pablo.rpglevels.mobs.IrregularMobHandler.isIrregular(living)) {
                tooltip.add(Component.literal("§7Nivel de Amenaza: ").append(threat.getBadge()).append(Component.literal(" §c(Irregular)")));
            } else {
                tooltip.add(Component.literal("§7Nivel de Amenaza: ").append(threat.getBadge()));
            }
        } else if (living instanceof com.pablo.rpglevels.entities.AdventurerNpcEntity adventurer) {
            tooltip.add(Component.literal("§7Gremio: ").append(adventurer.getRank().getBadge()));
        } else if (living instanceof com.pablo.rpglevels.entities.GuildMasterNpcEntity) {
            tooltip.add(Component.literal("§6★ Maestro del Gremio de Aventureros"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
