package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.TenseiCraftMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TenseiCraftMod.MOD_ID);

    public static final Supplier<EntityType<AdventurerNpcEntity>> ADVENTURER_NPC =
            ENTITIES.register("adventurer_npc", () -> EntityType.Builder.of(AdventurerNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("adventurer_npc"));

    public static final Supplier<EntityType<TownswomanNpcEntity>> TOWNSWOMAN_NPC =
            ENTITIES.register("townswoman_npc", () -> EntityType.Builder.of(TownswomanNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("townswoman_npc"));

    public static final Supplier<EntityType<BanditEntity>> BANDIT =
            ENTITIES.register("bandit", () -> EntityType.Builder.of(BanditEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("bandit"));

    public static final Supplier<EntityType<MagicFireballEntity>> MAGIC_FIREBALL =
            ENTITIES.register("magic_fireball", () -> EntityType.Builder.<MagicFireballEntity>of(MagicFireballEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("magic_fireball"));

    public static final Supplier<EntityType<UndeadAdventurerBossEntity>> UNDEAD_ADVENTURER =
            ENTITIES.register("undead_adventurer", () -> EntityType.Builder.of(UndeadAdventurerBossEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(12)
                    .build("undead_adventurer"));

    public static final Supplier<EntityType<WindMasterBossEntity>> WIND_MASTER =
            ENTITIES.register("wind_master", () -> EntityType.Builder.of(WindMasterBossEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(12)
                    .build("wind_master"));

    public static final Supplier<EntityType<WindMasterNpcEntity>> WIND_MASTER_NPC =
            ENTITIES.register("wind_master_npc", () -> EntityType.Builder.of(WindMasterNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("wind_master_npc"));

    public static final Supplier<EntityType<GuildMasterNpcEntity>> GUILD_MASTER =
            ENTITIES.register("guild_master", () -> EntityType.Builder.of(GuildMasterNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("guild_master"));

    public static final Supplier<EntityType<AliceNpcEntity>> ALICE =
            ENTITIES.register("alice", () -> EntityType.Builder.of(AliceNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("alice"));

    public static final Supplier<EntityType<SwordSpiritEntity>> SWORD_SPIRIT =
            ENTITIES.register("sword_spirit", () -> EntityType.Builder.of(SwordSpiritEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(12)
                    .build("sword_spirit"));

    public static final Supplier<EntityType<MagicSpellProjectileEntity>> MAGIC_SPELL_PROJECTILE =
            ENTITIES.register("magic_spell_projectile", () -> EntityType.Builder.<MagicSpellProjectileEntity>of(MagicSpellProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("magic_spell_projectile"));

    public static final Supplier<EntityType<CultistBossEntity>> CULTIST =
            ENTITIES.register("cultist", () -> EntityType.Builder.of(CultistBossEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(16)
                    .build("cultist"));

    public static final Supplier<EntityType<ThrownHighExperienceBottleEntity>> THROWN_HIGH_EXPERIENCE_BOTTLE =
            ENTITIES.register("thrown_high_experience_bottle", () -> EntityType.Builder.<ThrownHighExperienceBottleEntity>of(ThrownHighExperienceBottleEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("thrown_high_experience_bottle"));

    public static final Supplier<EntityType<PelotitaEntity>> PELOTITA =
            ENTITIES.register("pelotita", () -> EntityType.Builder.of(PelotitaEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 0.85f)
                    .clientTrackingRange(8)
                    .build("pelotita"));

    public static final Supplier<EntityType<VoidbornEntity>> VOIDBORN =
            ENTITIES.register("voidborn", () -> EntityType.Builder.of(VoidbornEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .eyeHeight(1.74f)
                    .clientTrackingRange(12)
                    .build("voidborn"));

    public static final Supplier<EntityType<VoidbornExecutorEntity>> VOIDBORN_EXECUTOR =
            ENTITIES.register("voidborn_executor", () -> EntityType.Builder.<VoidbornExecutorEntity>of(VoidbornExecutorEntity::new, MobCategory.MONSTER)
                    .sized(0.72f, 2.38f)
                    .eyeHeight(2.1f)
                    .clientTrackingRange(16)
                    .build("voidborn_executor"));

    public static final Supplier<EntityType<SpiritualBeamEntity>> SPIRITUAL_BEAM =
            ENTITIES.register("spiritual_beam", () -> EntityType.Builder.<SpiritualBeamEntity>of(SpiritualBeamEntity::new, MobCategory.MISC)
                    .sized(1.2f, 1.2f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("spiritual_beam"));

    public static final Supplier<EntityType<HornlessVoidbornEntity>> HORNLESS_VOIDBORN =
            ENTITIES.register("hornless_voidborn", () -> EntityType.Builder.<HornlessVoidbornEntity>of(HornlessVoidbornEntity::new, MobCategory.MONSTER)
                    .sized(0.85f, 3.0f)
                    .eyeHeight(2.65f)
                    .clientTrackingRange(14)
                    .build("hornless_voidborn"));

    public static final Supplier<EntityType<HornedVoidbornEntity>> HORNED_VOIDBORN =
            ENTITIES.register("horned_voidborn", () -> EntityType.Builder.<HornedVoidbornEntity>of(HornedVoidbornEntity::new, MobCategory.MONSTER)
                    .sized(0.90f, 3.1f)
                    .eyeHeight(2.75f)
                    .clientTrackingRange(14)
                    .build("horned_voidborn"));

    public static final Supplier<EntityType<ColossalVoidbornEntity>> COLOSSAL_VOIDBORN =
            ENTITIES.register("colossal_voidborn", () -> EntityType.Builder.<ColossalVoidbornEntity>of(ColossalVoidbornEntity::new, MobCategory.MONSTER)
                    .sized(1.15f, 2.8f)
                    .eyeHeight(2.45f)
                    .clientTrackingRange(16)
                    .build("colossal_voidborn"));

    public static final Supplier<EntityType<HollowRockProjectileEntity>> HOLLOW_ROCK =
            ENTITIES.register("hollow_rock", () -> EntityType.Builder.<HollowRockProjectileEntity>of(HollowRockProjectileEntity::new, MobCategory.MISC)
                    .sized(0.75f, 0.75f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("hollow_rock"));

    public static final Supplier<EntityType<StarryEntity>> STARRY =
            ENTITIES.register("starry", () -> EntityType.Builder.of(StarryEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 0.75f)
                    .eyeHeight(0.6f)
                    .clientTrackingRange(10)
                    .build("starry"));

    public static final Supplier<EntityType<BrambiEntity>> BRAMBI =
            ENTITIES.register("brambi", () -> EntityType.Builder.of(BrambiEntity::new, MobCategory.CREATURE)
                    .sized(0.85f, 1.15f)
                    .eyeHeight(0.95f)
                    .clientTrackingRange(10)
                    .build("brambi"));

    public static final Supplier<EntityType<ChubbyEntity>> CHUBBY =
            ENTITIES.register("chubby", () -> EntityType.Builder.of(ChubbyEntity::new, MobCategory.CREATURE)
                    .sized(0.7f, 0.6f)
                    .eyeHeight(0.35f)
                    .clientTrackingRange(10)
                    .build("chubby"));

    public static final Supplier<EntityType<RedfoxEntity>> REDFOX =
            ENTITIES.register("redfox", () -> EntityType.Builder.of(RedfoxEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("redfox"));

    public static final Supplier<EntityType<MeguNpcEntity>> MEGU =
            ENTITIES.register("megu", () -> EntityType.Builder.of(MeguNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(10)
                    .build("megu"));

    public static final Supplier<EntityType<EnergyAttackEntity>> ENERGY_ATTACK =
            ENTITIES.register("energy_attack", () -> EntityType.Builder.<EnergyAttackEntity>of(EnergyAttackEntity::new, MobCategory.MISC)
                    .sized(0.85f, 0.35f)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("energy_attack"));

    public static final Supplier<EntityType<ReimuBossEntity>> REIMU_BOSS =
            ENTITIES.register("reimu_boss", () -> EntityType.Builder.of(ReimuBossEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(16)
                    .build("reimu_boss"));

    public static final Supplier<EntityType<ReimuNpcEntity>> REIMU_NPC =
            ENTITIES.register("reimu_npc", () -> EntityType.Builder.of(ReimuNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.8f)
                    .eyeHeight(1.62f)
                    .clientTrackingRange(12)
                    .build("reimu_npc"));
}
