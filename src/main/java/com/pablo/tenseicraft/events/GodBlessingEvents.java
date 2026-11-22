package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.GreekGod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.entities.BanditEntity;
import com.pablo.tenseicraft.init.ModMobEffects;
import com.pablo.tenseicraft.items.PrismaticPotionItem;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.party.PartyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementa las Bendiciones Divinas únicas de los 12 Dioses del Olimpo.
 * Cada bendición otorga identidad jugable temprana desde Rango F y escala en Poder (Nivel I, II y III)
 * conforme el aventurero asciende de rango (I: F-E, II: D-C, III: B-A).
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class GodBlessingEvents {

    private static boolean inChainLightning = false;

    public static GreekGod getActiveGod(Player player) {
        if (player == null) return null;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.hasDivineBlessingUnlocked()) return null;
        return data.getPatronGod();
    }

    public static int getTier(Player player) {
        if (player == null) return 1;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return 1;
        return GreekGod.getBlessingTier(data.getRank());
    }

    // -------------------------------------------------------------------------
    // 1. PASIVAS PERIÓDICAS (Tick)
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) return;
        GreekGod god = getActiveGod(player);
        if (god == null) return;

        int tier = getTier(player);
        ServerLevel level = player.serverLevel();

        // Cada 1 segundo (20 ticks): Poseidón (agua/lluvia) y Artemisa (noche)
        if (player.tickCount % 20 == 0) {
            if (god == GreekGod.POSEIDON && player.isInWaterOrRain()) {
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
                if (!player.hasEffect(MobEffects.REGENERATION)) {
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
                }
            } else if (god == GreekGod.ARTEMIS && isNight(level)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
            }
        }

        // Cada 2 segundos (40 ticks): Apolo (luz solar) y Deméter (cultivos + regeneración natural)
        if (player.tickCount % 40 == 0) {
            if (god == GreekGod.APOLLO && isInSunlight(player, level)) {
                float healAmt = tier == 1 ? 0.5F : (tier == 2 ? 1.0F : 1.5F);
                if (player.getHealth() < player.getMaxHealth()) {
                    player.heal(healAmt);
                }
            } else if (god == GreekGod.DEMETER) {
                BlockPos belowPos = player.blockPosition().below();
                BlockState below = level.getBlockState(belowPos);
                if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT) || below.is(Blocks.FARMLAND)
                        || below.is(Blocks.MOSS_BLOCK) || below.is(Blocks.PODZOL) || below.is(Blocks.MYCELIUM)) {
                    float healAmt = tier == 1 ? 0.5F : (tier == 2 ? 1.0F : 1.5F);
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.heal(healAmt);
                    }
                }
                boostNearbyCrops(level, player.blockPosition(), tier + 3, tier);
            }
        }

        // Cada 3 segundos (60 ticks): Hefesto (autorreparación) y Afrodita (aura sanadora de aliados)
        if (player.tickCount % 60 == 0) {
            if (god == GreekGod.HEPHAESTUS) {
                repairOneDamagedItem(player);
            } else if (god == GreekGod.APHRODITE) {
                float healAmt = tier == 1 ? 1.0F : (tier == 2 ? 1.5F : 2.5F);
                List<LivingEntity> allies = PartyManager.getAllies(player, 10.0D);
                for (LivingEntity ally : allies) {
                    if (ally != player && ally.isAlive() && ally.getHealth() < ally.getMaxHealth()) {
                        ally.heal(healAmt);
                        if (ally.getHealth() < ally.getMaxHealth() * 0.5F) {
                            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, true, false, true));
                        }
                        level.sendParticles(ParticleTypes.HEART,
                                ally.getX(), ally.getY() + ally.getBbHeight() + 0.2D, ally.getZ(),
                                2, 0.25D, 0.15D, 0.25D, 0.01D);
                    }
                }
            }
        }
    }

    private static boolean isNight(ServerLevel level) {
        if (!level.dimensionType().natural()) return false;
        long dayTime = level.getDayTime() % 24000L;
        return dayTime >= 12500L && dayTime <= 23500L;
    }

    public static boolean isInSunlight(Player player, net.minecraft.world.level.Level level) {
        if (!level.dimensionType().hasSkyLight() || level.isRaining() || level.isThundering()) return false;
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime >= 12500L) return false;
        return level.canSeeSky(player.blockPosition().above());
    }

    private static void boostNearbyCrops(ServerLevel level, BlockPos center, int radius, int tier) {
        RandomSource rand = level.getRandom();
        float chancePerBlock = tier == 1 ? 0.14F : (tier == 2 ? 0.22F : 0.30F);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
                        if (rand.nextFloat() < chancePerBlock) {
                            int nextAge = Math.min(crop.getMaxAge(), crop.getAge(state) + 1);
                            level.setBlock(pos, crop.getStateForAge(nextAge), Block.UPDATE_CLIENTS);
                            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                    pos.getX() + 0.5D, pos.getY() + 0.45D, pos.getZ() + 0.5D,
                                    2, 0.2D, 0.15D, 0.2D, 0.02D);
                        }
                    }
                }
            }
        }
    }

    private static void repairOneDamagedItem(ServerPlayer player) {
        EquipmentSlot[] slots = new EquipmentSlot[]{
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. MITIGACIÓN DEFENSIVA (Zeus / Hefesto / Afrodita)
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (victim.level().isClientSide()) return;

        GreekGod god = getActiveGod(victim);
        if (god == null) return;
        int tier = getTier(victim);
        DamageSource source = event.getSource();

        if (god == GreekGod.ZEUS && source.is(DamageTypeTags.IS_LIGHTNING)) {
            if (tier >= 3) {
                event.setCanceled(true);
            } else {
                float factor = tier == 1 ? 0.40F : 0.20F;
                event.setAmount(event.getAmount() * factor);
            }
            return;
        }

        if (god == GreekGod.HEPHAESTUS && source.is(DamageTypeTags.IS_FIRE)) {
            float factor = tier == 1 ? 0.60F : (tier == 2 ? 0.35F : 0.10F);
            event.setAmount(event.getAmount() * factor);
            return;
        }

        if (god == GreekGod.APHRODITE) {
            Entity attacker = source.getEntity();
            if (attacker instanceof BanditEntity
                    || attacker instanceof AbstractIllager
                    || attacker instanceof AbstractPiglin
                    || attacker instanceof Witch) {
                float factor = tier == 1 ? 0.90F : (tier == 2 ? 0.85F : 0.80F);
                event.setAmount(event.getAmount() * factor);
            }
        }
    }

    // -------------------------------------------------------------------------
    // 3. BONOS OFENSIVOS EN COMBATE (Zeus / Hades / Poseidón / Ares / Apolo / Artemisa)
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (inChainLightning) return;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_LIGHTNING)) return;

        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player) || attacker == victim) return;

        GreekGod god = getActiveGod(player);
        if (god == null) return;

        int tier = getTier(player);
        ServerLevel level = player.serverLevel();
        float damage = event.getNewDamage();
        if (damage <= 0.0F) return;

        switch (god) {
            case ZEUS -> {
                if (level.isRaining() || level.isThundering()) {
                    float mult = tier == 1 ? 1.08F : (tier == 2 ? 1.12F : 1.16F);
                    damage *= mult;
                    event.setNewDamage(damage);
                }

                long gameTime = level.getGameTime();
                long cdUntil = player.getPersistentData().getLong("GodZeusChainCd");
                float procChance = (level.isRaining() || level.isThundering()) ? 0.50F : 0.25F;
                if (gameTime >= cdUntil && player.getRandom().nextFloat() < procChance) {
                    int maxTargets = tier + 1;
                    int cdTicks = (7 - tier) * 20;
                    float chainDmg = Math.max(2.0F, damage * (tier == 1 ? 0.25F : (tier == 2 ? 0.35F : 0.45F)));
                    if (triggerZeusChainLightning(level, player, victim, maxTargets, chainDmg)) {
                        player.getPersistentData().putLong("GodZeusChainCd", gameTime + cdTicks);
                    }
                }
            }
            case HADES -> {
                if (victim.getType().is(EntityTypeTags.UNDEAD)) {
                    float mult = tier == 1 ? 1.12F : (tier == 2 ? 1.18F : 1.25F);
                    event.setNewDamage(damage * mult);
                }
            }
            case POSEIDON -> {
                boolean isTrident = direct instanceof ThrownTrident
                        || player.getMainHandItem().getItem() instanceof TridentItem;
                boolean isWet = player.isInWaterOrRain() || victim.isInWaterOrRain();
                if (isTrident) {
                    float mult = tier == 1 ? 1.20F : (tier == 2 ? 1.30F : 1.40F);
                    event.setNewDamage(damage * mult);
                } else if (isWet) {
                    float mult = tier == 1 ? 1.12F : (tier == 2 ? 1.18F : 1.25F);
                    event.setNewDamage(damage * mult);
                }
            }
            case ARES -> {
                float maxHp = Math.max(1.0F, player.getMaxHealth());
                float missingRatio = Math.max(0.0F, Math.min(1.0F, (maxHp - player.getHealth()) / maxHp));
                float maxBonus = tier == 1 ? 0.20F : (tier == 2 ? 0.30F : 0.40F);
                damage *= (1.0F + missingRatio * maxBonus);
                event.setNewDamage(damage);

                // Robo de vida cuerpo a cuerpo cuando está por debajo del 60% de HP (CD 1.5s)
                if (!(direct instanceof Projectile) && player.getHealth() <= maxHp * 0.60F) {
                    long gameTime = level.getGameTime();
                    long healCd = player.getPersistentData().getLong("GodAresHealCd");
                    if (gameTime >= healCd) {
                        player.getPersistentData().putLong("GodAresHealCd", gameTime + 30L);
                        float stealRatio = tier == 1 ? 0.04F : (tier == 2 ? 0.06F : 0.08F);
                        float heal = Math.min(8.0F, Math.max(0.5F, damage * stealRatio));
                        player.heal(heal);
                    }
                }
            }
            case APOLLO -> {
                if (direct instanceof Projectile) {
                    float mult = tier == 1 ? 1.15F : (tier == 2 ? 1.22F : 1.30F);
                    event.setNewDamage(damage * mult);
                    if (victim.getType().is(EntityTypeTags.UNDEAD) || isInSunlight(player, level)) {
                        victim.igniteForSeconds(tier + 2);
                    }
                }
            }
            case ARTEMIS -> {
                boolean isRangedWeapon = direct instanceof AbstractArrow
                        || player.getMainHandItem().getItem() instanceof BowItem
                        || player.getMainHandItem().getItem() instanceof CrossbowItem
                        || player.distanceToSqr(victim) > 64.0D;
                if (isRangedWeapon) {
                    float mult = tier == 1 ? 1.15F : (tier == 2 ? 1.22F : 1.30F);
                    event.setNewDamage(damage * mult);
                }
            }
            default -> {}
        }
    }

    private static boolean triggerZeusChainLightning(ServerLevel level, ServerPlayer caster,
                                                     LivingEntity primaryVictim, int maxTargets, float chainDamage) {
        AABB box = primaryVictim.getBoundingBox().inflate(7.0D);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box, e ->
                e != caster && e != primaryVictim && e.isAlive()
                        && (e instanceof Monster || e instanceof Enemy || e instanceof BanditEntity)
                        && !PartyManager.isAllied(caster, e));
        if (candidates.isEmpty()) return false;

        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(primaryVictim)));
        Vec3 start = primaryVictim.position().add(0.0D, primaryVictim.getBbHeight() * 0.55D, 0.0D);

        int struck = 0;
        inChainLightning = true;
        try {
            for (LivingEntity target : candidates) {
                if (struck >= maxTargets) break;
                target.hurt(level.damageSources().lightningBolt(), chainDamage);
                Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
                for (int i = 0; i <= 8; i++) {
                    Vec3 p = start.lerp(end, i / 8.0D);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.08D, 0.08D, 0.08D, 0.02D);
                }
                struck++;
            }
        } finally {
            inChainLightning = false;
        }

        if (struck > 0) {
            level.playSound(null, primaryVictim.getX(), primaryVictim.getY(), primaryVictim.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.75F, 1.7F);
        }
        return struck > 0;
    }

    // -------------------------------------------------------------------------
    // 4. HADES: Cosecha de Almas al Matar Hostiles
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Monster || victim instanceof Enemy || victim instanceof BanditEntity)) return;

        Entity killerEntity = event.getSource().getEntity();
        ServerPlayer player = null;
        if (killerEntity instanceof ServerPlayer sp) {
            player = sp;
        } else if (killerEntity instanceof TamableAnimal tamable && tamable.getOwner() instanceof ServerPlayer sp) {
            player = sp;
        } else if (victim.getKillCredit() instanceof ServerPlayer sp) {
            player = sp;
        }
        if (player == null) return;

        GreekGod god = getActiveGod(player);
        if (god != GreekGod.HADES) return;

        int tier = getTier(player);
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        float pct = tier == 1 ? 0.06F : (tier == 2 ? 0.08F : 0.10F);
        float heal = Math.max(2.0F, player.getMaxHealth() * pct);
        player.heal(heal);

        float manaGain = tier == 1 ? 5.0F : (tier == 2 ? 8.0F : 12.0F);
        data.regenerateMana(player, manaGain);
        ModNetworking.syncMana(player);

        ServerLevel sl = player.serverLevel();
        sl.sendParticles(ParticleTypes.SOUL,
                victim.getX(), victim.getY() + 0.7D, victim.getZ(), 8, 0.25D, 0.3D, 0.25D, 0.04D);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.03D);
    }

    // -------------------------------------------------------------------------
    // 5. HEFESTO (Fundición Bonus) Y DEMÉTER (Cosecha Bonus)
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;

        GreekGod god = getActiveGod(player);
        if (god == null) return;

        int tier = getTier(player);
        ServerLevel sl = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (god == GreekGod.HEPHAESTUS) {
            ItemStack smelted = ItemStack.EMPTY;
            if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
                smelted = new ItemStack(Items.IRON_INGOT);
            } else if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE)) {
                smelted = new ItemStack(Items.GOLD_INGOT);
            } else if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
                smelted = new ItemStack(Items.COPPER_INGOT);
            }

            if (!smelted.isEmpty()) {
                float chance = tier == 1 ? 0.25F : (tier == 2 ? 0.40F : 0.60F);
                if (sl.getRandom().nextFloat() < chance) {
                    ItemEntity drop = new ItemEntity(sl, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, smelted);
                    sl.addFreshEntity(drop);
                    sl.sendParticles(ParticleTypes.FLAME,
                            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                            8, 0.2D, 0.2D, 0.2D, 0.03D);
                    sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.35F, 1.4F);
                }
            }
        } else if (god == GreekGod.DEMETER && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            float chance = tier == 1 ? 0.30F : (tier == 2 ? 0.45F : 0.60F);
            if (sl.getRandom().nextFloat() < chance) {
                List<ItemStack> drops = Block.getDrops(state, sl, pos, null, player, player.getMainHandItem());
                if (!drops.isEmpty()) {
                    ItemStack bonus = drops.get(0).copyWithCount(1);
                    if (!bonus.isEmpty()) {
                        ItemEntity extra = new ItemEntity(sl, pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D, bonus);
                        sl.addFreshEntity(extra);
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                pos.getX() + 0.5D, pos.getY() + 0.4D, pos.getZ() + 0.5D,
                                5, 0.2D, 0.2D, 0.2D, 0.03D);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 6. DIONISO Y DEMÉTER: Uso de Comidas y Pociones
    // -------------------------------------------------------------------------
    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GreekGod god = getActiveGod(player);
        if (god == null) return;

        int tier = getTier(player);
        ItemStack stack = event.getItem();
        FoodProperties food = stack.getFoodProperties(player);
        boolean isFood = (food != null);
        boolean isPotion = (stack.getItem() instanceof PotionItem || stack.getItem() instanceof PrismaticPotionItem);

        if (god == GreekGod.DEMETER && isFood) {
            float satBonus = tier == 1 ? 1.0F : (tier == 2 ? 1.5F : 2.0F);
            player.getFoodData().eat(1, satBonus);
            float extraHeal = tier == 1 ? 2.0F : (tier == 2 ? 3.0F : 5.0F);
            player.heal(extraHeal);
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.3D, 0.3D, 0.3D, 0.03D);
            return;
        }

        if (god == GreekGod.DIONYSUS && (isFood || isPotion)) {
            if (isPotion) {
                extendConsumedPotionEffects(player, stack, tier);
            }

            // Frenesí Festivo (Velocidad I + Fuerza I + PM)
            int frenzyTicks = (tier == 1 ? 8 : (tier == 2 ? 12 : 16)) * 20;
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, frenzyTicks, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, frenzyTicks, 0, false, false, true));

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                float mana = tier == 1 ? 4.0F : (tier == 2 ? 6.0F : 10.0F);
                data.regenerateMana(player, mana);
                ModNetworking.syncMana(player);
            }
        }
    }

    private static void extendConsumedPotionEffects(ServerPlayer player, ItemStack stack, int tier) {
        double mult = tier == 1 ? 1.25D : (tier == 2 ? 1.35D : 1.50D);
        Set<Holder<MobEffect>> targetEffects = new HashSet<>();

        if (stack.getItem() instanceof PrismaticPotionItem prism) {
            if (prism.getPrismaticType() == PrismaticPotionItem.PrismaticType.STRENGTH) {
                targetEffects.add(ModMobEffects.PRISMATIC_STRENGTH);
            } else if (prism.getPrismaticType() == PrismaticPotionItem.PrismaticType.SWIFTNESS) {
                targetEffects.add(ModMobEffects.PRISMATIC_SWIFTNESS);
            }
        } else {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                contents.forEachEffect(inst -> targetEffects.add(inst.getEffect()));
            }
        }

        if (targetEffects.isEmpty()) return;

        List<MobEffectInstance> toUpgrade = new ArrayList<>();
        for (MobEffectInstance active : player.getActiveEffects()) {
            Holder<MobEffect> eff = active.getEffect();
            if (targetEffects.contains(eff) && eff.value().isBeneficial()
                    && !eff.value().isInstantenous() && !active.isInfiniteDuration()) {
                int extendedDuration = (int) Math.round(active.getDuration() * mult);
                toUpgrade.add(new MobEffectInstance(
                        eff,
                        extendedDuration,
                        active.getAmplifier(),
                        active.isAmbient(),
                        active.isVisible(),
                        active.showIcon()
                ));
            }
        }
        for (MobEffectInstance upgraded : toUpgrade) {
            player.addEffect(upgraded);
        }
    }
}
