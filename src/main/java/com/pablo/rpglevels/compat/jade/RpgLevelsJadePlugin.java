package com.pablo.rpglevels.compat.jade;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(RpgLevelsMod.MOD_ID)
public class RpgLevelsJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(MobRankJadeProvider.INSTANCE, LivingEntity.class);
    }
}
