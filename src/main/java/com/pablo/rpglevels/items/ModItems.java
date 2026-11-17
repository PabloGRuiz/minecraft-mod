package com.pablo.rpglevels.items;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.entities.ModEntities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RpgLevelsMod.MOD_ID);

    // Amuleto de Experiencia (Accesorio Curios para la ranura 'necklace' o 'charm')
    public static final DeferredItem<Item> EXPERIENCE_AMULET = ITEMS.register("experience_amulet",
            () -> new ExperienceAmuletItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // Anillo de Fuerza Ancestral (Accesorio Curios para la ranura 'ring')
    public static final DeferredItem<Item> ANCIENT_RING = ITEMS.register("ancient_ring",
            () -> new AncientRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // Anillo del Guerrero (Accesorio Evolutivo con Autoataque)
    public static final DeferredItem<Item> WARRIOR_RING = ITEMS.register("warrior_ring",
            () -> new WarriorRingItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // Huevo generador de NPC Aventurero
    public static final DeferredItem<Item> ADVENTURER_SPAWN_EGG = ITEMS.register("adventurer_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.ADVENTURER_NPC,
                    0x2B3D52, // Primario (Azul acero)
                    0xD4AF37, // Secundario (Dorado)
                    new Item.Properties()));

    // Huevo generador de NPC Aldeana
    public static final DeferredItem<Item> TOWNSWOMAN_SPAWN_EGG = ITEMS.register("townswoman_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.TOWNSWOMAN_NPC,
                    0xBD8B72, // Primario (Terracota cálido)
                    0x55A065, // Secundario (Verde esmeralda / lazo)
                    new Item.Properties()));

    // Huevo generador de Bandido
    public static final DeferredItem<Item> BANDIT_SPAWN_EGG = ITEMS.register("bandit_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.BANDIT,
                    0x36393E, // Primario (Gris oscuro forajido)
                    0x9E1F27, // Secundario (Rojo carmesí bandana)
                    new Item.Properties()));

    // Huevo generador de Undead Adventurer Boss
    public static final DeferredItem<Item> UNDEAD_ADVENTURER_SPAWN_EGG = ITEMS.register("undead_adventurer_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.UNDEAD_ADVENTURER,
                    0x1B122C, // Primario (Morado oscuro sepulcral)
                    0x9B2226, // Secundario (Rojo sangre espectral)
                    new Item.Properties()));

    // Huevo generador de Wind Master Boss (Kaelen)
    public static final DeferredItem<Item> WIND_MASTER_SPAWN_EGG = ITEMS.register("wind_master_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.WIND_MASTER,
                    0x2E5A36, // Primario (Verde esmeralda viento)
                    0xE6C35C, // Secundario (Dorado ágil)
                    new Item.Properties()));

    // Huevo generador de Guild Master (Maestro del Gremio)
    public static final DeferredItem<Item> GUILD_MASTER_SPAWN_EGG = ITEMS.register("guild_master_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.GUILD_MASTER,
                    0xD4AF37, // Primario (Dorado de gremio)
                    0x2C3E50, // Secundario (Azul marino solemne)
                    new Item.Properties()));

    // Huevo generador de Alice (Ingeniera de los Segadores Espirituales)
    public static final DeferredItem<Item> ALICE_SPAWN_EGG = ITEMS.register("alice_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.ALICE,
                    0x6A0DAD, // Primario (Púrpura Segador)
                    0xE6E6FA, // Secundario (Lavanda etéreo)
                    new Item.Properties()));

    // Espada Espiritual (Hoja Despierta)
    public static final DeferredItem<Item> SPIRITUAL_SWORD = ITEMS.register("spiritual_sword",
            () -> new SpiritualSwordItem(new Item.Properties()));

    // Pergamino de Escape del Abismo
    public static final DeferredItem<Item> ESCAPE_SCROLL = ITEMS.register("escape_scroll",
            EscapeScrollItem::new);

    // Tarjeta de Aventurero (Documento de Identificación y Ficha Oficial del Gremio)
    public static final DeferredItem<Item> ADVENTURER_CARD = ITEMS.register("adventurer_card",
            () -> new AdventurerCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // Bastón del Patrón (Overseer's Cane)
    public static final DeferredItem<Item> OVERSEER_CANE = ITEMS.register("overseer_cane",
            () -> new OverseerCaneItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    // Espada Espiritual Despertada del Cultista (Shadowblade)
    public static final DeferredItem<Item> CULTIST_SPIRITUAL_SWORD = ITEMS.register("cultist_spiritual_sword",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.NETHERITE,
                    new Item.Properties().attributes(
                            net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.NETHERITE, 8, -2.4f)
                    ).rarity(Rarity.EPIC)));

    // Huevo generador de Cultista (Jefe Hazard)
    public static final DeferredItem<Item> CULTIST_SPAWN_EGG = ITEMS.register("cultist_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.CULTIST,
                    0x2B0D38, // Morado oscuro abisal
                    0xA31828, // Carmesí hereje
                    new Item.Properties()));

    // Huevo generador de Pelotita
    public static final DeferredItem<Item> PELOTITA_SPAWN_EGG = ITEMS.register("pelotita_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.PELOTITA,
                    0xFF9EC7, // Rosa pastel
                    0xFFFFFF, // Blanco
                    new Item.Properties()));

    // Huevo generador de Nacido del Vacío (Voidborn)
    public static final DeferredItem<Item> VOIDBORN_SPAWN_EGG = ITEMS.register("voidborn_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.VOIDBORN,
                    0x150B24, // Púrpura oscuro abisal / Vacío
                    0xEDE7E3, // Blanco máscara ósea Hollow
                    new Item.Properties()));

    // Huevo generador de Voidborn Executor (Rango HAZARD con Espada Espiritual)
    public static final DeferredItem<Item> VOIDBORN_EXECUTOR_SPAWN_EGG = ITEMS.register("voidborn_executor_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.VOIDBORN_EXECUTOR,
                    0x2A0B3D, // Púrpura abisal real
                    0xFFB300, // Dorado Hazard brillante
                    new Item.Properties()));

    // Huevo generador de Hornless Voidborn
    public static final DeferredItem<Item> HORNLESS_VOIDBORN_SPAWN_EGG = ITEMS.register("hornless_voidborn_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.HORNLESS_VOIDBORN,
                    0x1E1230, // Púrpura abisal
                    0xE5DFD5, // Blanco hueso
                    new Item.Properties()));

    // Huevo generador de Horned Voidborn
    public static final DeferredItem<Item> HORNED_VOIDBORN_SPAWN_EGG = ITEMS.register("horned_voidborn_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.HORNED_VOIDBORN,
                    0x291038, // Púrpura oscuro
                    0xC92A2A, // Carmesí cuernos
                    new Item.Properties()));

    // Huevo generador de Colossal Voidborn
    public static final DeferredItem<Item> COLOSSAL_VOIDBORN_SPAWN_EGG = ITEMS.register("colossal_voidborn_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.COLOSSAL_VOIDBORN,
                    0x11071C, // Negro abisal
                    0x9C36B5, // Violeta espiritual
                    new Item.Properties()));

    // Huevo generador de Starry (Compañero zorro místico)
    public static final DeferredItem<Item> STARRY_SPAWN_EGG = ITEMS.register("starry_spawn_egg",
            () -> new DeferredSpawnEggItem(
                    ModEntities.STARRY,
                    0xFFA6D5, // Rosa pastel floreciente
                    0x7EE8FA, // Celeste estelar celestial
                    new Item.Properties()));

    // Grimorio del Segador Espiritual (Libro de Hechizos Oficial de Iron's Spells - 3 ranuras rápidas)
    public static final DeferredItem<Item> SPIRITUAL_SPELLBOOK = ITEMS.register("spiritual_spellbook",
            () -> new io.redspace.ironsspellbooks.item.SpellBook(3, new Item.Properties().rarity(Rarity.RARE).stacksTo(1))
                    .withAttribute(com.pablo.rpglevels.magic.spiritual.SpiritualAttributes.SPIRITUAL_SPELL_POWER, 0.15)
                    .withAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA, 100.0)
                    .withAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION, 0.10));

    // Botella de Gran Experiencia (10x Experiencia de la botella común)
    public static final DeferredItem<Item> HIGH_EXPERIENCE_BOTTLE = ITEMS.register("high_experience_bottle",
            () -> new HighExperienceBottleItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // Cristales de Alma (Solidificación anímica para reforjas y armería humana)
    public static final DeferredItem<Item> LESSER_SOUL_FRAGMENT = ITEMS.register("lesser_soul_fragment",
            () -> new SoulCrystalItem(SoulCrystalItem.Tier.LESSER));

    public static final DeferredItem<Item> RESONANT_SOUL_CRYSTAL = ITEMS.register("resonant_soul_crystal",
            () -> new SoulCrystalItem(SoulCrystalItem.Tier.RESONANT));

    public static final DeferredItem<Item> RADIANT_SOUL_GEM = ITEMS.register("radiant_soul_gem",
            () -> new SoulCrystalItem(SoulCrystalItem.Tier.RADIANT));

    public static final DeferredItem<Item> PRIMORDIAL_SOUL_CORE = ITEMS.register("primordial_soul_core",
            () -> new SoulCrystalItem(SoulCrystalItem.Tier.PRIMORDIAL));

    // Voidsteel: Mineral exclusivo del Abismo (Violeta fluorescente)
    public static final DeferredItem<Item> RAW_VOIDSTEEL = ITEMS.register("raw_voidsteel",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredItem<Item> VOIDSTEEL_INGOT = ITEMS.register("voidsteel_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
}
