package com.pablo.tenseicraft.init;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, TenseiCraftMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> REIMU_BOSS_MUSIC =
            SOUND_EVENTS.register("music.reimu_boss", () ->
                    SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "music.reimu_boss")));
}
