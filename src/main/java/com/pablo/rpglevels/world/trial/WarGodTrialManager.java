package com.pablo.rpglevels.world.trial;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.blocks.StatueBlock;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.GreekGod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.entities.BrambiEntity;
import com.pablo.rpglevels.entities.ChubbyEntity;
import com.pablo.rpglevels.entities.ModEntities;
import com.pablo.rpglevels.entities.StarryEntity;
import com.pablo.rpglevels.events.ModAttributesHandler;
import com.pablo.rpglevels.init.ModBlocks;
import com.pablo.rpglevels.network.ModNetworking;
import com.pablo.rpglevels.network.StartAstralCinematicPayload;
import com.pablo.rpglevels.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class WarGodTrialManager {

    private static final AtomicInteger NEXT_ARENA_INDEX = new AtomicInteger(0);
    private static final int ARENA_SPACING = 64;

    public enum EncounterType {
        FIRST_BLESSING,
        ASCENSION
    }

    private static class AstralSession {
        final UUID playerUuid;
        final int centerX;
        final int centerZ;
        final EncounterType type;
        final GreekGod god;
        final boolean isAscendingToB;
        boolean guardianChosen = false;
        int postChoiceTicks = 0;
        int tickCounter = 0;
        int step = 0;

        AstralSession(UUID playerUuid, int centerX, int centerZ, EncounterType type, GreekGod god, boolean isAscendingToB) {
            this.playerUuid = playerUuid;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.type = type;
            this.god = god;
            this.isAscendingToB = isAscendingToB;
        }
    }

    private static final Map<UUID, AstralSession> ACTIVE_SESSIONS = new HashMap<>();
    private static final Map<UUID, Integer> PENDING_ENCOUNTERS = new HashMap<>();

    public static boolean isInTrial(UUID playerUuid) {
        return ACTIVE_SESSIONS.containsKey(playerUuid) || PENDING_ENCOUNTERS.containsKey(playerUuid);
    }

    /**
     * 1. Detect when player kills their first enemy -> A random Greek God takes interest in them!
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity victim = event.getEntity();
        boolean isHostile = (victim instanceof Monster
                || victim instanceof Enemy
                || victim.getType().getCategory() == MobCategory.MONSTER
                || victim instanceof com.pablo.rpglevels.entities.BanditEntity);

        if (!isHostile) return;

        ServerPlayer player = null;
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof ServerPlayer sp) {
            player = sp;
        } else if (sourceEntity instanceof TamableAnimal tamable && tamable.getOwner() instanceof ServerPlayer sp) {
            player = sp;
        } else if (victim.getLastHurtByMob() instanceof ServerPlayer sp) {
            player = sp;
        }

        if (player == null) return;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null && !data.hasKilledFirstMonster()) {
            data.setHasKilledFirstMonster(true);
            if (data.getPatronGod() == null) {
                GreekGod chosenGod = GreekGod.pickRandom(player.getRandom());
                data.setPatronGodId(chosenGod.getId());
            }
            ModNetworking.syncToPlayer(player);

            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§5✦ §d§l[MIRADA DEL OLIMPO] §5✦"));
            player.sendSystemMessage(Component.literal("§d§oAl abatir a tu primer enemigo, sientes la mirada de una deidad ancestral posarse sobre tu alma desde el Plano Astral..."));
            player.sendSystemMessage(Component.literal("§eVe a dormir en una cama para acudir a su llamado y despertar tu potencial."));
            player.sendSystemMessage(Component.empty());

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9f, 1.4f);
        }
    }

    /**
     * 2. Detect when player wakes up from bed to trigger the Astral Encounter
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide() || ModDimensions.isAstral(player.level())) return;

        checkAndScheduleAstralEncounter(player, 15);
    }

    /**
     * Also allow triggering when interacting with a bed if an Astral Encounter is ready (even during daytime!)
     */
    @SubscribeEvent
    public static void onRightClickBed(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide() || ModDimensions.isAstral(player.level())) return;

        if (player.level().getBlockState(event.getPos()).getBlock() instanceof BedBlock) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) return;

            boolean needsFirstBlessing = data.hasKilledFirstMonster() && !data.hasDivineBlessingUnlocked();
            boolean needsAscension = data.hasDivineBlessingUnlocked() && data.isAtMilestoneCap();

            if ((needsFirstBlessing || needsAscension) && !isInTrial(player.getUUID())) {
                // Save bed position as spawn/return point and transport to Astral Dream
                PENDING_ENCOUNTERS.put(player.getUUID(), 15);
                player.displayClientMessage(Component.literal("§d✦ Cierras los ojos y tu consciencia es arrastrada hacia el Espacio Astral..."), true);
            }
        }
    }

    public static void checkAndScheduleAstralEncounter(ServerPlayer player, int delayTicks) {
        if (isInTrial(player.getUUID())) return;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        boolean needsFirstBlessing = data.hasKilledFirstMonster() && !data.hasDivineBlessingUnlocked();
        boolean needsAscension = data.hasDivineBlessingUnlocked() && data.isAtMilestoneCap();

        if (needsFirstBlessing || needsAscension) {
            PENDING_ENCOUNTERS.put(player.getUUID(), Math.max(5, delayTicks));
        }
    }

    /**
     * Starts an Astral Encounter (either First Blessing or Milestone Rank Ascension)
     */
    public static void startTrial(ServerPlayer player) {
        if (player == null || !player.isAlive() || ModDimensions.isAstral(player.level())) return;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        GreekGod god = data.getPatronGod();
        if (god == null) {
            god = GreekGod.pickRandom(player.getRandom());
            data.setPatronGodId(god.getId());
        }

        EncounterType type = (!data.hasDivineBlessingUnlocked())
                ? EncounterType.FIRST_BLESSING
                : EncounterType.ASCENSION;

        ServerLevel astralLevel = player.getServer().getLevel(ModDimensions.ASTRAL_LEVEL);
        if (astralLevel == null) {
            astralLevel = player.getServer().getLevel(ModDimensions.VOID_TRIAL_LEVEL);
        }
        if (astralLevel == null) {
            // Fallback if dimension couldn't load
            completeEncounterEffects(player, data, type, god);
            return;
        }

        boolean isAscendingToB = (type == EncounterType.ASCENSION && data.getRank() == AdventurerRank.C);

        // Save player's current position & dimension to return after the Astral audience
        player.getPersistentData().putString("WarTrialReturnDim", player.level().dimension().location().toString());
        player.getPersistentData().putDouble("WarTrialReturnX", player.getX());
        player.getPersistentData().putDouble("WarTrialReturnY", player.getY());
        player.getPersistentData().putDouble("WarTrialReturnZ", player.getZ());

        int index = NEXT_ARENA_INDEX.getAndIncrement();
        int centerX = index * ARENA_SPACING;
        int centerZ = 0;

        buildBarrierRoom(astralLevel, centerX, centerZ, isAscendingToB, player);

        player.teleportTo(astralLevel, centerX + 0.5, 65.0, centerZ + 0.5, Set.of(), 0.0f, 0.0f);
        player.resetFallDistance();

        astralLevel.playSound(null, centerX + 0.5, 65.0, centerZ + 0.5,
                SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.8f, 1.4f);

        // Enviar cinemática con varios planos alrededor del jugador en el espacio astral
        int cinematicDuration = isAscendingToB ? 400 : 320;
        PacketDistributor.sendToPlayer(player, new StartAstralCinematicPayload(player.getId(), cinematicDuration, isAscendingToB));

        ACTIVE_SESSIONS.put(player.getUUID(), new AstralSession(player.getUUID(), centerX, centerZ, type, god, isAscendingToB));
    }

    private static void buildBarrierRoom(ServerLevel level, int centerX, int centerZ, boolean isAscendingToB, ServerPlayer player) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                level.setBlockAndUpdate(new BlockPos(x, 64, z), Blocks.BARRIER.defaultBlockState());
                level.setBlockAndUpdate(new BlockPos(x, 69, z), Blocks.BARRIER.defaultBlockState());

                for (int y = 65; y <= 68; y++) {
                    if (dx == -4 || dx == 4 || dz == -4 || dz == 4) {
                        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.BARRIER.defaultBlockState());
                    } else {
                        level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        if (isAscendingToB && player != null) {
            // Pedestal Izquierdo: Starry
            level.setBlockAndUpdate(new BlockPos(centerX - 2, 64, centerZ + 3), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX - 2, 65, centerZ + 3), Blocks.PURPUR_PILLAR.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX - 2, 66, centerZ + 3),
                    ModBlocks.STARRY_STATUE.get().defaultBlockState().setValue(StatueBlock.FACING, Direction.SOUTH));

            // Pedestal Central: Chubby
            level.setBlockAndUpdate(new BlockPos(centerX, 64, centerZ + 3), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX, 65, centerZ + 3), Blocks.QUARTZ_PILLAR.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX, 66, centerZ + 3),
                    ModBlocks.CHUBBY_STATUE.get().defaultBlockState().setValue(StatueBlock.FACING, Direction.SOUTH));

            // Pedestal Derecho: Brambi
            level.setBlockAndUpdate(new BlockPos(centerX + 2, 64, centerZ + 3), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX + 2, 65, centerZ + 3), Blocks.OCHRE_FROGLIGHT.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(centerX + 2, 66, centerZ + 3),
                    ModBlocks.BRAMBI_STATUE.get().defaultBlockState().setValue(StatueBlock.FACING, Direction.SOUTH));
        }
    }

    private static void cleanupAstralSession(ServerLevel level, UUID playerUuid, int centerX, int centerZ) {
        level.setBlockAndUpdate(new BlockPos(centerX - 2, 66, centerZ + 3), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(centerX, 66, centerZ + 3), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(centerX + 2, 66, centerZ + 3), Blocks.AIR.defaultBlockState());

        AABB box = new AABB(centerX - 6, 62, centerZ - 6, centerX + 6, 72, centerZ + 6);
        List<Entity> statues = level.getEntitiesOfClass(Entity.class, box,
                e -> e.getTags().contains("Session_" + playerUuid) || e.getTags().contains("AstralStatue"));
        for (Entity s : statues) {
            s.discard();
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(pos);

        AstralSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null || !session.isAscendingToB || session.guardianChosen) return;

        String guardianType = "";
        String guardianName = "";
        if (state.is(ModBlocks.STARRY_STATUE.get())) {
            guardianType = "starry";
            guardianName = "Starry (Espíritu Celestial)";
        } else if (state.is(ModBlocks.CHUBBY_STATUE.get())) {
            guardianType = "chubby";
            guardianName = "Chubby (Nubecita del Trueno)";
        } else if (state.is(ModBlocks.BRAMBI_STATUE.get())) {
            guardianType = "brambi";
            guardianName = "Brambi (Venado de Brasa y Bosque)";
        }

        if (!guardianType.isEmpty()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            selectGuardian(player, session, guardianType, guardianName, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Entity target = event.getTarget();
        if (target == null || !target.getTags().contains("AstralStatue")) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        AstralSession session = ACTIVE_SESSIONS.get(player.getUUID());
        if (session == null || !session.isAscendingToB || session.guardianChosen) return;

        String guardianType = "";
        String guardianName = "";
        if (target.getTags().contains("Guardian_starry")) {
            guardianType = "starry";
            guardianName = "Starry (Espíritu Celestial)";
        } else if (target.getTags().contains("Guardian_chubby")) {
            guardianType = "chubby";
            guardianName = "Chubby (Nubecita del Trueno)";
        } else if (target.getTags().contains("Guardian_brambi")) {
            guardianType = "brambi";
            guardianName = "Brambi (Venado de Brasa y Bosque)";
        }

        if (!guardianType.isEmpty()) {
            selectGuardian(player, session, guardianType, guardianName, target.getX(), target.getY() + 0.8, target.getZ());
        }
    }

    private static void selectGuardian(ServerPlayer player, AstralSession session, String guardianType, String guardianName, double x, double y, double z) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        data.setChosenGuardian(guardianType);
        ModNetworking.syncToPlayer(player);
        session.guardianChosen = true;

        ServerLevel level = player.serverLevel();
        level.playSound(null, x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.3f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 45, 0.4, 0.5, 0.4, 0.12);
        level.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);

        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(session.god.getChatPrefix()
                .append(Component.literal("«¡Elección bendecida! Has forjado un pacto de sangre y espíritu con ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(guardianName).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(". Podrás convocarlo a la batalla en cualquier momento pulsando la tecla [C] con un coste de 50 MP.»").withStyle(ChatFormatting.GOLD)));
        player.sendSystemMessage(Component.empty());

        player.displayClientMessage(Component.literal("§a✦ ¡Has vinculado a " + guardianName + " como tu Guardián! Pulsa [C] en combate."), false);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!PENDING_ENCOUNTERS.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> pIt = PENDING_ENCOUNTERS.entrySet().iterator();
            while (pIt.hasNext()) {
                Map.Entry<UUID, Integer> pEntry = pIt.next();
                int delay = pEntry.getValue() - 1;
                if (delay <= 0) {
                    pIt.remove();
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(pEntry.getKey());
                    if (player != null && player.isAlive() && !ACTIVE_SESSIONS.containsKey(pEntry.getKey())) {
                        startTrial(player);
                    }
                } else {
                    pEntry.setValue(delay);
                }
            }
        }

        if (ACTIVE_SESSIONS.isEmpty()) return;

        Iterator<Map.Entry<UUID, AstralSession>> it = ACTIVE_SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, AstralSession> entry = it.next();
            AstralSession session = entry.getValue();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.playerUuid);

            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }

            session.tickCounter++;

            // Ambient cosmic particles in the Astral Plane
            if (session.tickCounter % 10 == 0 && player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        6, 2.5, 1.5, 2.5, 0.02);
                sl.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        10, 1.5, 1.0, 1.5, 0.1);
            }

            // Step 0 -> Step 1 (Tick 30: God's Presentation / Greeting)
            if (session.step == 0 && session.tickCounter >= 30) {
                session.step = 1;

                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§5✦ §d§lESPACIO ASTRAL §5✦")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("§7Presencia Divina: ").append(session.god.getBadge())));

                player.sendSystemMessage(Component.empty());
                if (session.type == EncounterType.FIRST_BLESSING) {
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«" + session.god.getIntroLine1() + "»").withStyle(ChatFormatting.WHITE)));
                } else if (session.isAscendingToB) {
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«Bienvenido nuevamente a mi templo astral, mi más valiente elegido. Tu alma ha alcanzado la cúspide del Rango C y estás preparado para cruzar el umbral del Rango B.»").withStyle(ChatFormatting.WHITE)));
                } else {
                    PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                    int lvl = (data != null) ? data.getRpgLevel() : 20;
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«Compareces de nuevo ante mí en el Espacio Astral, mi elegido, tras alcanzar el Hito de Nivel " + lvl + ".»").withStyle(ChatFormatting.WHITE)));
                }

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.7f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 0.8f);
            }

            // Step 1 -> Step 2 (Tick 120: Recognition / Guardian Offering)
            else if (session.step == 1 && session.tickCounter >= 120) {
                session.step = 2;

                if (session.type == EncounterType.FIRST_BLESSING) {
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«" + session.god.getIntroLine2() + "»").withStyle(ChatFormatting.YELLOW)));
                } else if (session.isAscendingToB) {
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«Para hacer frente a las calamidades que acechan a los Héroes, he condensado la esencia cósmica de este plano para forjar un don sagrado: un Guardián Espiritual inmortal que combatirá a tu lado.»").withStyle(ChatFormatting.YELLOW)));
                } else {
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«" + session.god.getAscensionCongratulation() + "»").withStyle(ChatFormatting.YELLOW)));
                }

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 1.0f);
            }

            // Step 2 -> Step 3 (Tick 220: Choosing Guardian or Granting Ascension)
            else if (session.step == 2 && session.tickCounter >= 220) {
                session.step = 3;

                if (session.type == EncounterType.FIRST_BLESSING) {
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«" + session.god.getIntroLine3() + "»").withStyle(ChatFormatting.GOLD)));
                } else if (session.isAscendingToB) {
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«He erigido ante ti las estatuas sagradas de las tres criaturas: §dStarry§6, §bChubby §6y §eBrambi§6. Acércate e interactúa (clic derecho) con tu estatua predilecta para elegir a tu Guardián. Podrás invocarlo tocando la tecla §f[C] §6consumiendo §b50 MP§6, y su poder ascenderá junto a tu propio Nivel RPG.»").withStyle(ChatFormatting.GOLD)));
                    player.displayClientMessage(Component.literal("§e✦ Toca la estatua de Starry, Chubby o Brambi para forjar tu vínculo eterno."), false);
                } else {
                    PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                    AdventurerRank nextRank = (data != null) ? data.getRank().getNextRank() : AdventurerRank.E;
                    player.sendSystemMessage(session.god.getChatPrefix()
                            .append(Component.literal("«¡Rompe las cadenas de tu límite mortal! En mi nombre, te otorgo la §d§lAscensión a ").withStyle(ChatFormatting.GOLD))
                            .append(nextRank.getBadge())
                            .append(Component.literal("§6! ¡Ve y logra hazañas aún más grandiosas!»")));
                }
                player.sendSystemMessage(Component.empty());

                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2f, 1.2f);
            }

            // Step 3 -> Step 4 (Lightning Strike & Return Home)
            else if (session.step == 3) {
                if (session.isAscendingToB) {
                    // Si es ascensión a B, esperamos a que el jugador elija a su guardián
                    if (!session.guardianChosen) {
                        if (session.tickCounter % 160 == 0) {
                            player.displayClientMessage(Component.literal("§e✦ Elige a tu Guardián haciendo clic derecho sobre una de las 3 estatuas."), true);
                        }
                        continue;
                    }

                    session.postChoiceTicks++;
                    if (session.postChoiceTicks < 45) {
                        continue;
                    }
                } else {
                    if (session.tickCounter < 300) {
                        continue;
                    }
                }

                session.step = 4;

                // Spawn visual lightning bolt in the Astral Plane
                spawnVisualLightning(player.serverLevel(), player.getX(), player.getY(), player.getZ());

                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    completeEncounterEffects(player, data, session.type, session.god);
                }

                cleanupAstralSession(player.serverLevel(), session.playerUuid, session.centerX, session.centerZ);

                returnPlayerToOverworld(player);
                // Also strike visual lightning at their waking location
                spawnVisualLightning(player.serverLevel(), player.getX(), player.getY(), player.getZ());

                it.remove();
            }
        }
    }

    private static void completeEncounterEffects(ServerPlayer player, PlayerRpgData data, EncounterType type, GreekGod god) {
        if (type == EncounterType.FIRST_BLESSING) {
            data.setDivineBlessingUnlocked(true);
            data.setWarTrialCompleted(true);
            data.addBonusPendingPoints(1); // 1 welcome attribute point with the blessing
            ModAttributesHandler.updatePlayerAttributes(player);
            ModNetworking.syncToPlayer(player);

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l¡PROGRESIÓN DESBLOQUEADA!")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal("§eBendición de ").append(god.getBadge()).append(Component.literal(" §erecibida"))));

            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§6⚡ « BENDICIÓN DIVINA GRABADA EN TU ALMA » ⚡").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("§eHas recibido la bendición de ").append(god.getBadge()).append(Component.literal(" §7(" + god.getTitle() + ")§e.")));
            player.sendSystemMessage(Component.literal("§a✔ ¡Has desbloqueado la progresión de Nivel RPG y mejora de estadísticas!"));
            player.sendSystemMessage(Component.literal("§7• Sube de nivel abatiendo criaturas, abriendo cofres de botín, minando y cumpliendo misiones del Gremio."));
            player.sendSystemMessage(Component.literal("§7• Al alcanzar el §eHito de Nivel §7de tu rango actual, duerme para volver al Plano Astral y recibir tu Ascensión."));
            player.sendSystemMessage(Component.empty());

            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            boolean ascended = data.performAscension();
            if (ascended) {
                ModAttributesHandler.updatePlayerAttributes(player);
                ModNetworking.syncToPlayer(player);

                com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.FIRST_ASCENSION);
                if (data.getRank().getIndex() >= AdventurerRank.B.getIndex()) {
                    com.pablo.rpglevels.advancements.ModAdvancements.award(player, com.pablo.rpglevels.advancements.ModAdvancements.RANK_B);
                }

                Component rankBadge = data.getRank().getBadge();
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§6§l¡ASCENSIÓN DIVINA!")));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal("§eNuevo Rango: ").append(rankBadge)));

                player.sendSystemMessage(Component.empty());
                player.sendSystemMessage(Component.literal("§6⚡ « ASCENSIÓN OTORGADA POR " + god.getDisplayName().toUpperCase() + " » ⚡").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("§a¡Tu existencia ha ascendido a ").append(rankBadge).append(Component.literal("§a!")));
                player.sendSystemMessage(Component.literal("§e• Se ha desbloqueado el nuevo techo de progresión hasta el §fNivel " + data.getCurrentMilestoneCap() + "§e."));
                player.sendSystemMessage(Component.literal("§b• Tu Dios te ha concedido §a+2 Puntos de Atributo §badicionales (Pulsa §f[K]§b)."));
                player.sendSystemMessage(Component.empty());

                player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }

    private static void spawnVisualLightning(ServerLevel level, double x, double y, double z) {
        if (level == null) return;
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.2f, 1.0f);
        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0f, 1.1f);
        level.sendParticles(ParticleTypes.FLASH, x, y + 1.0, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z, 35, 0.5, 0.6, 0.5, 0.15);
    }

    private static void returnPlayerToOverworld(ServerPlayer player) {
        ServerLevel targetLevel = player.getServer().overworld();
        if (player.getPersistentData().contains("WarTrialReturnDim")) {
            String dimStr = player.getPersistentData().getString("WarTrialReturnDim");
            ResourceLocation dimLoc = ResourceLocation.tryParse(dimStr);
            if (dimLoc != null) {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
                ServerLevel resolved = player.getServer().getLevel(dimKey);
                if (resolved != null && !ModDimensions.isAstral(resolved)) {
                    targetLevel = resolved;
                }
            }
        }
        if (targetLevel == null) return;

        double targetX = player.getPersistentData().contains("WarTrialReturnX")
                ? player.getPersistentData().getDouble("WarTrialReturnX")
                : targetLevel.getSharedSpawnPos().getX() + 0.5;
        double targetY = player.getPersistentData().contains("WarTrialReturnY")
                ? player.getPersistentData().getDouble("WarTrialReturnY")
                : targetLevel.getSharedSpawnPos().getY() + 1.0;
        double targetZ = player.getPersistentData().contains("WarTrialReturnZ")
                ? player.getPersistentData().getDouble("WarTrialReturnZ")
                : targetLevel.getSharedSpawnPos().getZ() + 0.5;

        player.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), player.getYRot(), player.getXRot());
        player.resetFallDistance();

        targetLevel.playSound(null, targetX, targetY, targetZ, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0f, 1.2f);
    }
}
