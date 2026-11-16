package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.world.dimension.ModDimensions;
import com.pablo.rpglevels.world.dimension.AbyssTeleportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class AbyssDungeonEvents {

    // Tracks player-placed blocks in the Abyss Rift to prevent place-and-break ore farming exploits
    private static final Set<Long> PLAYER_PLACED_ORES = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getLevel() instanceof Level level && ModDimensions.isAbyssRift(level)) {
            BlockState placed = event.getPlacedBlock();
            if (isOreBlock(placed)) {
                PLAYER_PLACED_ORES.add(event.getPos().asLong());
            }
        }
    }

    private static boolean isOreBlock(BlockState state) {
        return state.is(net.neoforged.neoforge.common.Tags.Blocks.ORES)
                || state.getBlock() instanceof DropExperienceBlock
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.DIAMOND_ORES);
    }

    /**
     * Radial Ore & Mineral Reward Scaling:
     * When mining ores in the Abyss Rift, distance from (0,0) enriches drops with rarer minerals & artifacts.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        if (level.isClientSide() || !ModDimensions.isAbyssRift(level)) {
            return;
        }

        if (event.getPlayer().isCreative()) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        if (!isOreBlock(state)) {
            return;
        }

        // Prevent infinite ore duplication exploit from player-placed blocks
        if (PLAYER_PLACED_ORES.remove(pos.asLong())) {
            return;
        }

        // Prevent infinite ore duplication exploit when mining with Silk Touch
        ItemStack tool = event.getPlayer().getMainHandItem();
        var lookup = level.registryAccess().lookup(Registries.ENCHANTMENT);
        if (lookup.isPresent()) {
            var silkTouch = lookup.get().get(Enchantments.SILK_TOUCH);
            if (silkTouch.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(silkTouch.get(), tool) > 0) {
                return;
            }
        }

        double distance = Math.sqrt((double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ());
        RandomSource random = level.getRandom();
        ItemStack bonusDrop = ItemStack.EMPTY;

        // Luck stat bonus (+0.5% chance per Luck point)
        float luckBonus = 0.0f;
        com.pablo.rpglevels.data.PlayerRpgData data = event.getPlayer().getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            int luck = data.getTotalStatPoints(event.getPlayer(), com.pablo.rpglevels.data.StatType.LUCK);
            luckBonus = luck * 0.005f;
        }

        if (distance < 250) {
            // Zone 1: Extra Iron / Copper / Coal
            if (random.nextFloat() < (0.25f + luckBonus)) {
                bonusDrop = random.nextBoolean() ? new ItemStack(Items.RAW_IRON, 1 + random.nextInt(2))
                        : new ItemStack(Items.RAW_COPPER, 2 + random.nextInt(3));
            }
        } else if (distance < 600) {
            // Zone 2: Gold, Redstone, Lapis, Amethyst
            if (random.nextFloat() < (0.30f + luckBonus)) {
                float r = random.nextFloat();
                if (r < 0.35f) bonusDrop = new ItemStack(Items.RAW_GOLD, 1 + random.nextInt(2));
                else if (r < 0.70f) bonusDrop = new ItemStack(Items.REDSTONE, 2 + random.nextInt(4));
                else bonusDrop = new ItemStack(Items.AMETHYST_SHARD, 1 + random.nextInt(2));
            }
        } else if (distance < 1100) {
            // Zone 3: Diamonds, Emeralds
            if (random.nextFloat() < (0.25f + luckBonus)) {
                bonusDrop = random.nextFloat() < 0.65f ? new ItemStack(Items.DIAMOND, 1 + random.nextInt(2))
                        : new ItemStack(Items.EMERALD, 1 + random.nextInt(2));
            }
        } else if (distance < 1800) {
            // Zone 4: Ancient Debris, Netherite Scrap, Diamond Blocks
            if (random.nextFloat() < (0.20f + luckBonus)) {
                float r = random.nextFloat();
                if (r < 0.50f) bonusDrop = new ItemStack(Items.NETHERITE_SCRAP, 1);
                else if (r < 0.80f) bonusDrop = new ItemStack(Items.ANCIENT_DEBRIS, 1);
                else bonusDrop = new ItemStack(Items.DIAMOND, 3);
            }
        } else {
            // Heart of Abyss: Pure Netherite Ingot, Ancient Debris
            if (random.nextFloat() < (0.15f + luckBonus)) {
                bonusDrop = random.nextFloat() < 0.35f ? new ItemStack(Items.NETHERITE_INGOT, 1)
                        : new ItemStack(Items.ANCIENT_DEBRIS, 2);
            }
        }

        if (!bonusDrop.isEmpty() && level instanceof ServerLevel serverLevel) {
            ItemEntity entity = new ItemEntity(serverLevel, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, bonusDrop);
            entity.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(entity);

            serverLevel.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.1);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.3f);

            event.getPlayer().displayClientMessage(
                    Component.translatable("rpglevels.abyss.resonance", (int) distance, bonusDrop.getHoverName(), bonusDrop.getCount()),
                    true
            );
        }
    }

    /**
     * Infinite Cave Dungeon Zone HUD:
     * Periodically updates the player's action bar with their current radial distance, zone name, and threat level.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ModDimensions.isAbyssRift(player.level())) {
            return;
        }

        // Run check every 2 seconds (40 ticks)
        if (player.tickCount % 40 == 0) {
            // Comprobación de Rechazo del Vacío: si expira la protección o no tiene 500 MP Máximo, es expulsado
            if (!player.isCreative() && !player.isSpectator()) {
                com.pablo.rpglevels.data.PlayerRpgData rpgData = player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
                float maxMana = (rpgData != null) ? rpgData.getMaxMana(player) : 0.0f;
                boolean hasWard = player.hasEffect(com.pablo.rpglevels.init.ModMobEffects.ABYSSAL_WARD);

                if (maxMana < 500.0f && !hasWard) {
                    AbyssTeleportHelper.exitAbyssRift(player);
                    com.pablo.rpglevels.world.dimension.AbyssPortalBlock.executeVoidRejection(player, maxMana);
                    return;
                }
            }

            int dist = (int) Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ());
            Component zoneComp = AbyssTeleportHelper.getZoneComponent(dist);
            Component threatComp = AbyssTeleportHelper.getThreatBadge(dist);

            Component zoneText = Component.literal("§5[Abismo] §d§l")
                    .append(zoneComp)
                    .append(" §7| §e" + dist + "m §7| ")
                    .append(threatComp);

            player.displayClientMessage(zoneText, true);
        }

        // Random Cultist Boss spawn check every 10 seconds (200 ticks)
        if (player.tickCount % 200 == 0) {
            checkCultistSpawn(player);
        }
    }

    private static void checkCultistSpawn(ServerPlayer player) {
        double distFromCenter = Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ());
        if (distFromCenter < 50.0) {
            return; // Don't spawn in safe central monolith sanctuary
        }

        long gameTime = player.level().getGameTime();
        long lastSpawn = player.getPersistentData().getLong("RpgLastCultistSpawnAttempt");
        if (lastSpawn > 0 && gameTime - lastSpawn < 2400L) {
            return; // At least 2 minutes between spawn events
        }

        ServerLevel serverLevel = player.serverLevel();
        List<com.pablo.rpglevels.entities.CultistBossEntity> existing = serverLevel.getEntitiesOfClass(
                com.pablo.rpglevels.entities.CultistBossEntity.class,
                player.getBoundingBox().inflate(128.0)
        );
        if (!existing.isEmpty()) {
            return;
        }

        boolean killedBefore = player.getPersistentData().getBoolean("RpgKilledCultistBoss");
        com.pablo.rpglevels.quests.PlayerQuestData qData = player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_QUEST_DATA);
        com.pablo.rpglevels.data.PlayerRpgData rpgData = player.getData(com.pablo.rpglevels.data.ModAttachments.PLAYER_RPG_DATA);
        boolean hasQuestOrRankB = (qData != null && qData.isQuestActive("alice_cultist_hunt"))
                || (rpgData != null && rpgData.getRank().getIndex() >= com.pablo.rpglevels.data.AdventurerRank.B.getIndex());

        if (!hasQuestOrRankB && !killedBefore) {
            return; // Only spawn if on the quest / Rank B+, or re-fighting
        }

        float spawnChance = killedBefore ? 0.03f : 0.15f;

        if (player.getRandom().nextFloat() < spawnChance) {
            // Find valid floor position 18 to 28 blocks away from player
            double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = 18.0 + player.getRandom().nextDouble() * 10.0;
            int targetX = (int) (player.getX() + Math.cos(angle) * radius);
            int targetZ = (int) (player.getZ() + Math.sin(angle) * radius);
            int playerY = (int) player.getY();

            BlockPos columnCheck = new BlockPos(targetX, playerY, targetZ);
            if (!serverLevel.hasChunkAt(columnCheck)) {
                return;
            }

            BlockPos spawnPos = null;
            for (int dy = -16; dy <= 16; dy++) {
                BlockPos check = new BlockPos(targetX, playerY + dy, targetZ);
                if (serverLevel.getBlockState(check).isAir()
                        && serverLevel.getBlockState(check.above()).isAir()
                        && !serverLevel.getBlockState(check.below()).isAir()
                        && !serverLevel.getBlockState(check.below()).liquid()) {
                    spawnPos = check;
                    break;
                }
            }

            if (spawnPos != null) {
                com.pablo.rpglevels.entities.CultistBossEntity cultist =
                        com.pablo.rpglevels.entities.ModEntities.CULTIST.get().create(serverLevel);
                if (cultist != null) {
                    cultist.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, player.getRandom().nextFloat() * 360.0F, 0.0F);
                    cultist.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(spawnPos), net.minecraft.world.entity.MobSpawnType.EVENT, null);
                    serverLevel.addFreshEntity(cultist);

                    player.getPersistentData().putLong("RpgLastCultistSpawnAttempt", gameTime);

                    // Menacing notification sound and title
                    for (ServerPlayer sp : serverLevel.players()) {
                        if (ModDimensions.isAbyssRift(sp.level())) {
                            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
                            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("§5§l¡EL CULTISTA HA APARECIDO!")));
                            sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("§cUn Segador Corrupto acecha entre las sombras del Abismo")));

                            sp.sendSystemMessage(Component.literal("§5[✦] §cUna densa presencia maligna distorsiona el aire del Abismo... ¡El Cultista de los demonios ha emergido!"));
                            sp.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2F, 0.8F);
                            sp.playNotifySound(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.0F, 1.2F);
                        }
                    }
                }
            }
        }
    }

    /**
     * All monsters killed inside the Abyss Rift drop an Escape Scroll
     * ensuring players are never trapped even if they lost their initial scroll.
     */
    @SubscribeEvent
    public static void onLivingDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (ModDimensions.isAbyssRift(event.getEntity().level())) {
            if (event.getEntity() instanceof net.minecraft.world.entity.monster.Monster) {
                event.getEntity().spawnAtLocation(new ItemStack(com.pablo.rpglevels.items.ModItems.ESCAPE_SCROLL.get(), 1));
            }
        }
    }
}
