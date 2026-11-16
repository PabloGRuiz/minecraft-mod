package com.pablo.rpglevels.magic.spiritual;

import com.pablo.rpglevels.RpgLevelsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SpiritualAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, RpgLevelsMod.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> SPIRITUAL_SPELL_POWER =
            ATTRIBUTES.register("spiritual_spell_power", () ->
                    new RangedAttribute("attribute.rpglevels.spiritual_spell_power", 1.0, 0.0, 1000.0).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> SPIRITUAL_MAGIC_RESIST =
            ATTRIBUTES.register("spiritual_magic_resist", () ->
                    new RangedAttribute("attribute.rpglevels.spiritual_magic_resist", 1.0, 0.0, 1000.0).setSyncable(true));

    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SPIRITUAL_SPELL_POWER);
        event.add(EntityType.PLAYER, SPIRITUAL_MAGIC_RESIST);
    }
}

