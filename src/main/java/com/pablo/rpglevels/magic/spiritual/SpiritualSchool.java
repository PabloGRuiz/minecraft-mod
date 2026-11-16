package com.pablo.rpglevels.magic.spiritual;

import com.pablo.rpglevels.RpgLevelsMod;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.ISSDamageTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SpiritualSchool {

    public static final ResourceLocation SPIRITUAL_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "spiritual");

    public static final TagKey<Item> SPIRITUAL_FOCUS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "spiritual_focus"));

    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, RpgLevelsMod.MOD_ID);

    public static final DeferredHolder<SchoolType, SchoolType> SPIRITUAL =
            SCHOOLS.register("spiritual", () -> new SchoolType(
                    SPIRITUAL_RESOURCE,
                    SPIRITUAL_FOCUS,
                    Component.translatable("school.rpglevels.spiritual").withStyle(Style.EMPTY.withColor(0x38E5B5)),
                    SpiritualAttributes.SPIRITUAL_SPELL_POWER,
                    SpiritualAttributes.SPIRITUAL_MAGIC_RESIST,
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME),
                    ISSDamageTypes.EVOCATION_MAGIC
            ));
}
