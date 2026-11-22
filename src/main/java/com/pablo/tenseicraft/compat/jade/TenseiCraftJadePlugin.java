package com.pablo.tenseicraft.compat.jade;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(TenseiCraftMod.MOD_ID)
public class TenseiCraftJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(MobRankJadeProvider.INSTANCE, LivingEntity.class);
    }
}
