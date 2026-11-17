package com.pablo.rpglevels.mobs;

import com.pablo.rpglevels.entities.BanditEntity;
import com.pablo.rpglevels.entities.ModEntities;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.party.PartyManager;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BanditAmbushManager {

    public static class AmbushInstance {
        public final UUID playerId;
        public final ServerBossEvent bossEvent;
        public final Set<UUID> banditUuids = new HashSet<>();
        public final int totalBandits;
        public int aliveBandits;
        public final long startTime;
        public final BlockPos centerPos;

        public AmbushInstance(UUID playerId, ServerBossEvent bossEvent, int totalBandits, BlockPos centerPos) {
            this.playerId = playerId;
            this.bossEvent = bossEvent;
            this.totalBandits = totalBandits;
            this.aliveBandits = totalBandits;
            this.startTime = System.currentTimeMillis();
            this.centerPos = centerPos;
        }
    }

    private static final Map<UUID, AmbushInstance> ACTIVE_AMBUSHES = new ConcurrentHashMap<>();

    public static boolean hasActiveAmbush(ServerPlayer player) {
        return ACTIVE_AMBUSHES.containsKey(player.getUUID());
    }

    public static boolean triggerAmbush(ServerPlayer player) {
        return triggerAmbush(player, false, 0);
    }

    public static boolean triggerAmbush(ServerPlayer player, boolean isRevenge, int killedYesterday) {
        if (player == null || player.level().isClientSide()) {
            return false;
        }

        if (hasActiveAmbush(player)) {
            player.sendSystemMessage(Component.literal("§cYa hay una emboscada activa acechándote."));
            return false;
        }

        ServerLevel level = player.serverLevel();

        // 1. Find suitable surface spawn location 24 to 32 blocks away
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double distance = 24.0 + level.random.nextDouble() * 8.0;
        int targetX = player.getBlockX() + (int)(Math.cos(angle) * distance);
        int targetZ = player.getBlockZ() + (int)(Math.sin(angle) * distance);
        int targetY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);

        BlockPos centerPos = new BlockPos(targetX, targetY, targetZ);

        // 2. Play pillager raid horn sound at global volume
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64.0F, 1.0F);

        // 3. Screen Title and Subtitle announcement
        if (isRevenge) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l¡VENGANZA DE BANDIDOS!")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c\"¡Mataste a nuestros camaradas ayer! ¡Pagarás con sangre!\"")));

            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§4§l[¡REPRESALIA FORAJIDA!] §c¡Una partida de venganza ha llegado a cobrarse la sangre de los camaradas que eliminaste ayer!").withStyle(ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("§eLos forajidos vienen enfurecidos y fuertemente armados (" + killedYesterday + " abatidos ayer). ¡Defiende tu vida!"));
            player.sendSystemMessage(Component.empty());
        } else {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§4§l¡EMBOSCADA DE BANDIDOS!")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§c¡Una partida de forajidos te ha rodeado!")));

            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§4§l[¡ALERTA DE EMBOSCADA!] §c¡El cuerno de asalto resuena en la lejanía!").withStyle(ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("§eUna peligrosa banda de forajidos te ha tendido una emboscada. ¡Derrótalos para reclamar su tesoro de experiencia!"));
            player.sendSystemMessage(Component.empty());
        }

        // 4. Determine group size (5 to 10 normal, 8 to 14 in revenge)
        int totalBandits = isRevenge ? (8 + level.random.nextInt(7)) : (5 + level.random.nextInt(6));

        // 5. Create Raid-style BossBar
        Component bossBarTitle = isRevenge
                ? Component.literal("§4§lVenganza de Bandidos: §f" + totalBandits + " / " + totalBandits + " restantes")
                : Component.literal("§c§lEmboscada de Bandidos: §f" + totalBandits + " / " + totalBandits + " restantes");

        ServerBossEvent bossEvent = new ServerBossEvent(
                bossBarTitle,
                isRevenge ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.NOTCHED_10
        );
        bossEvent.setProgress(1.0F);
        bossEvent.addPlayer(player);

        AmbushInstance ambush = new AmbushInstance(player.getUUID(), bossEvent, totalBandits, centerPos);

        // 6. Spawn Leader (Guaranteed Rank C)
        BanditEntity leader = new BanditEntity(ModEntities.BANDIT.get(), level);
        leader.moveTo(targetX + 0.5, targetY, targetZ + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        leader.setLeader(true);
        leader.setPersistenceRequired();
        leader.finalizeSpawn(level, level.getCurrentDifficultyAt(centerPos), MobSpawnType.EVENT, null);
        leader.setTarget(player);
        level.addFreshEntity(leader);
        ambush.banditUuids.add(leader.getUUID());

        // 7. Spawn remaining bandits around the leader
        for (int i = 1; i < totalBandits; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 6.0;
            int bx = targetX + (int) offsetX;
            int bz = targetZ + (int) offsetZ;
            int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);

            BanditEntity bandit = new BanditEntity(ModEntities.BANDIT.get(), level);
            bandit.moveTo(bx + 0.5, by, bz + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            bandit.setPersistenceRequired();
            bandit.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(bx, by, bz)), MobSpawnType.EVENT, null);
            bandit.setTarget(player);
            level.addFreshEntity(bandit);
            ambush.banditUuids.add(bandit.getUUID());
        }

        ACTIVE_AMBUSHES.put(player.getUUID(), ambush);
        return true;
    }

    public static void onBanditDeath(BanditEntity bandit) {
        if (bandit == null || bandit.level().isClientSide()) {
            return;
        }

        UUID deadId = bandit.getUUID();
        for (Iterator<Map.Entry<UUID, AmbushInstance>> it = ACTIVE_AMBUSHES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, AmbushInstance> entry = it.next();
            AmbushInstance ambush = entry.getValue();

            if (ambush.banditUuids.contains(deadId)) {
                ambush.banditUuids.remove(deadId);
                ambush.aliveBandits = Math.max(0, ambush.aliveBandits - 1);

                // Update boss bar
                ambush.bossEvent.setName(Component.literal("§c§lEmboscada de Bandidos: §f" + ambush.aliveBandits + " / " + ambush.totalBandits + " restantes"));
                ambush.bossEvent.setProgress((float) ambush.aliveBandits / (float) ambush.totalBandits);

                // Check victory condition
                if (ambush.aliveBandits <= 0) {
                    completeAmbushVictory(entry.getKey(), ambush, (ServerLevel) bandit.level());
                    it.remove();
                }
                break;
            }
        }
    }

    private static void completeAmbushVictory(UUID playerId, AmbushInstance ambush, ServerLevel level) {
        ambush.bossEvent.removeAllPlayers();

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }

        // 1. Fanfare and victory sounds
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);

        // 2. Victory title and chat
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§a§l¡EMBOSCADA SUPERADA!")));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§e¡Los forajidos han sido derrotados!")));

        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§a§l¡VICTORIA CONTRA LA EMBOSCADA!").withStyle(ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("§eHas eliminado a todos los bandidos forajidos. ¡Un torrente de experiencia cae sobre ti!"));
        player.sendSystemMessage(Component.empty());

        // Reputation Reward (+8 Rep)
        com.pablo.rpglevels.events.ReputationEvents.modifyReputation(player, 8, "rpglevels.reputation.reason.ambush_cleared");

        // Recompensa de Experiencia RPG para el jugador que superó la emboscada
        PlayerRpgData rpgData = player.getData(ModAttachments.PLAYER_RPG_DATA);
        int baseAmbushXp = 400 + (ambush.totalBandits * 25);
        if (rpgData != null && rpgData.hasDivineBlessingUnlocked()) {
            int finalAmbushXp = com.pablo.rpglevels.events.CombatEvents.calculateRankScaledXp(
                    baseAmbushXp, rpgData.getRank(), com.pablo.rpglevels.data.ThreatLevel.C);
            rpgData.addRpgXp(player, finalAmbushXp);
            player.sendSystemMessage(Component.literal("§6✦ §l[¡EMBOSCADA SUPERADA!] §eHas obtenido §a+" + finalAmbushXp + " XP RPG §epor erradicar la banda forajida."));
        }

        // Repartir experiencia de emboscada a los compañeros de grupo
        List<LivingEntity> recipients = PartyManager.getPartyXpRecipients(player, 48.0);
        for (LivingEntity recipient : recipients) {
            if (recipient instanceof ServerPlayer ally && ally != player) {
                PlayerRpgData allyData = ally.getData(ModAttachments.PLAYER_RPG_DATA);
                if (allyData != null && allyData.hasDivineBlessingUnlocked()) {
                    int allyBase = 200 + (ambush.totalBandits * 12);
                    int allyXp = com.pablo.rpglevels.events.CombatEvents.calculateRankScaledXp(
                            allyBase, allyData.getRank(), com.pablo.rpglevels.data.ThreatLevel.C);
                    allyData.addRpgXp(ally, allyXp);
                    ally.sendSystemMessage(Component.literal("§6✦ §l[¡EMBOSCADA SUPERADA!] §eHas obtenido §a+" + allyXp + " XP RPG §epor cooperar en el grupo."));
                }
            } else if (recipient instanceof AdventurerNpcEntity adv) {
                adv.addExperience(300);
            }
        }

        // 3. Ender Dragon style XP Rain: multiple cascading orbs totaling ~2000 XP
        for (int i = 0; i < 35; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 5.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 5.0;
            double offsetY = 2.5 + level.random.nextDouble() * 2.0;

            int xpValue = 40 + level.random.nextInt(35); // ~1400 - 2600 XP total
            ExperienceOrb orb = new ExperienceOrb(level, player.getX() + offsetX, player.getY() + offsetY, player.getZ() + offsetZ, xpValue);
            orb.setDeltaMovement(
                    (level.random.nextDouble() - 0.5) * 0.25,
                    0.25 + level.random.nextDouble() * 0.15,
                    (level.random.nextDouble() - 0.5) * 0.25
            );
            level.addFreshEntity(orb);
        }

        // 4. Celebratory particles around player
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 60, 1.0, 1.0, 1.0, 0.3);
        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 2.0, player.getZ(), 40, 0.8, 0.8, 0.8, 0.15);
    }

    public static void tickServerAmbushes(ServerLevel level) {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, AmbushInstance>> it = ACTIVE_AMBUSHES.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, AmbushInstance> entry = it.next();
            AmbushInstance ambush = entry.getValue();

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            // If player disconnected or timeout (10 minutes)
            if (player == null || (now - ambush.startTime > 600000L)) {
                ambush.bossEvent.removeAllPlayers();
                it.remove();
                continue;
            }

            // Actively verify living entities so phantom despawns or out-of-bounds never freeze the raid
            ambush.banditUuids.removeIf(uuid -> {
                net.minecraft.world.entity.Entity entity = level.getEntity(uuid);
                return entity == null || !entity.isAlive() || entity.isRemoved();
            });

            ambush.aliveBandits = ambush.banditUuids.size();
            ambush.bossEvent.setName(Component.literal("§c§lEmboscada de Bandidos: §f" + ambush.aliveBandits + " / " + ambush.totalBandits + " restantes"));
            ambush.bossEvent.setProgress((float) ambush.aliveBandits / (float) ambush.totalBandits);

            if (ambush.aliveBandits <= 0) {
                completeAmbushVictory(entry.getKey(), ambush, level);
                it.remove();
            }
        }
    }
}
