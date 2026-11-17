package com.pablo.rpglevels.party;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.entities.TownswomanNpcEntity;
import com.pablo.rpglevels.network.ModNetworking;
import com.pablo.rpglevels.network.SyncPartyPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class PartyManager {

    private static final Map<UUID, Party> PARTIES_BY_ID = new HashMap<>();
    private static final Map<UUID, UUID> PLAYER_TO_PARTY = new HashMap<>();
    private static final Map<UUID, UUID> PENDING_INVITES = new HashMap<>(); // targetUUID -> leaderUUID

    public static Party getParty(UUID playerUUID) {
        UUID partyId = PLAYER_TO_PARTY.get(playerUUID);
        return partyId != null ? PARTIES_BY_ID.get(partyId) : null;
    }

    public static Party createParty(ServerPlayer leader) {
        UUID leaderUUID = leader.getUUID();
        if (PLAYER_TO_PARTY.containsKey(leaderUUID)) {
            leader.sendSystemMessage(Component.literal("§c¡Ya formas parte de un grupo! Abandónalo primero antes de crear uno nuevo."));
            return null;
        }

        Party party = new Party(leaderUUID, leader.getName().getString());
        PARTIES_BY_ID.put(party.getPartyId(), party);
        PLAYER_TO_PARTY.put(leaderUUID, party.getPartyId());

        leader.sendSystemMessage(Component.empty());
        leader.sendSystemMessage(Component.literal("§6« ¡GRUPO DE AVENTURA CREADO! »").withStyle(ChatFormatting.BOLD));
        leader.sendSystemMessage(Component.literal("§eEres el líder de tu grupo. Puedes invitar a otros aventureros con §b/party invite <jugador>§e."));
        leader.sendSystemMessage(Component.empty());

        syncPartyToMembers(party, leader.server);
        return party;
    }

    public static boolean invitePlayer(ServerPlayer leader, ServerPlayer target) {
        Party party = getParty(leader.getUUID());
        if (party == null || !party.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§cSolo el líder del grupo puede enviar invitaciones."));
            return false;
        }

        if (target.getUUID().equals(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§cNo puedes invitarte a ti mismo."));
            return false;
        }

        if (PLAYER_TO_PARTY.containsKey(target.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c" + target.getName().getString() + " ya está en un grupo de aventura."));
            return false;
        }

        if (party.getMemberCount() >= Party.MAX_MEMBERS) {
            leader.sendSystemMessage(Component.literal("§cEl grupo ha alcanzado el límite máximo de " + Party.MAX_MEMBERS + " miembros."));
            return false;
        }

        PENDING_INVITES.put(target.getUUID(), leader.getUUID());

        leader.sendSystemMessage(Component.literal("§aInvitación enviada a §b" + target.getName().getString() + "§a."));

        target.sendSystemMessage(Component.empty());
        target.sendSystemMessage(Component.literal("§6[✦] §b" + leader.getName().getString() + " §ete ha invitado a su Grupo de Aventura."));
        target.sendSystemMessage(Component.literal("§aEscribe §6/party accept §ao usa la interfaz para unirte al equipo."));
        target.sendSystemMessage(Component.empty());

        return true;
    }

    public static boolean acceptInvite(ServerPlayer target) {
        UUID targetUUID = target.getUUID();
        UUID leaderUUID = PENDING_INVITES.remove(targetUUID);

        if (leaderUUID == null) {
            target.sendSystemMessage(Component.literal("§cNo tienes invitaciones de grupo pendientes."));
            return false;
        }

        Party party = getParty(leaderUUID);
        if (party == null) {
            target.sendSystemMessage(Component.literal("§cEl grupo de la invitación ya no existe."));
            return false;
        }

        if (party.addMember(targetUUID, target.getName().getString())) {
            PLAYER_TO_PARTY.put(targetUUID, party.getPartyId());

            // Notify all members
            broadcastToParty(party, target.server,
                    Component.literal("§a✦ §b" + target.getName().getString() + " §ase ha unido al Grupo de Aventura."));

            syncPartyToMembers(party, target.server);
            return true;
        } else {
            target.sendSystemMessage(Component.literal("§cNo se pudo unir al grupo. Puede que esté lleno."));
            return false;
        }
    }

    public static boolean leaveParty(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        Party party = getParty(playerUUID);
        if (party == null) {
            player.sendSystemMessage(Component.literal("§cNo perteneces a ningún grupo de aventura."));
            return false;
        }

        if (party.isLeader(playerUUID)) {
            // Si el líder se va y hay otros miembros, traspasa el liderazgo o disuelve si está solo
            if (party.getMemberCount() <= 1) {
                disbandParty(player);
                return true;
            } else {
                party.removeMember(playerUUID);
                PLAYER_TO_PARTY.remove(playerUUID);
                UUID nextLeader = party.getMemberUUIDs().iterator().next();
                String nextName = party.getMemberNames().getOrDefault(nextLeader, "Líder");
                party.setLeader(nextLeader, nextName);

                player.sendSystemMessage(Component.literal("§eHas abandonado el grupo de aventura."));
                PacketDistributor.sendToPlayer(player, new SyncPartyPayload(false, false, "", 0, List.of()));

                broadcastToParty(party, player.server,
                        Component.literal("§c✦ §b" + player.getName().getString() + " §eha dejado el grupo. Nuevo líder: §6" + nextName));
                syncPartyToMembers(party, player.server);
                return true;
            }
        } else {
            party.removeMember(playerUUID);
            PLAYER_TO_PARTY.remove(playerUUID);

            player.sendSystemMessage(Component.literal("§eHas abandonado el grupo de aventura."));
            PacketDistributor.sendToPlayer(player, new SyncPartyPayload(false, false, "", 0, List.of()));

            broadcastToParty(party, player.server,
                    Component.literal("§c✦ §b" + player.getName().getString() + " §eha dejado el grupo de aventura."));
            syncPartyToMembers(party, player.server);
            return true;
        }
    }

    public static boolean disbandParty(ServerPlayer leader) {
        UUID leaderUUID = leader.getUUID();
        Party party = getParty(leaderUUID);
        if (party == null || !party.isLeader(leaderUUID)) {
            leader.sendSystemMessage(Component.literal("§cSolo el líder puede disolver el grupo."));
            return false;
        }

        broadcastToParty(party, leader.server,
                Component.literal("§c✦ El grupo de aventura ha sido disuelto por el líder."));

        for (UUID memberUUID : party.getMemberUUIDs()) {
            PLAYER_TO_PARTY.remove(memberUUID);
            ServerPlayer member = leader.server.getPlayerList().getPlayer(memberUUID);
            if (member != null) {
                PacketDistributor.sendToPlayer(member, new SyncPartyPayload(false, false, "", 0, List.of()));
            }
        }

        PARTIES_BY_ID.remove(party.getPartyId());
        return true;
    }

    public static boolean kickMember(ServerPlayer leader, UUID targetUUID) {
        Party party = getParty(leader.getUUID());
        if (party == null || !party.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§cSolo el líder puede expulsar miembros."));
            return false;
        }

        if (leader.getUUID().equals(targetUUID)) {
            leader.sendSystemMessage(Component.literal("§cNo puedes expulsarte a ti mismo. Usa /party leave para salir."));
            return false;
        }

        String targetName = party.getMemberNames().getOrDefault(targetUUID, "Aventurero");
        if (party.removeMember(targetUUID)) {
            PLAYER_TO_PARTY.remove(targetUUID);

            ServerPlayer targetPlayer = leader.server.getPlayerList().getPlayer(targetUUID);
            if (targetPlayer != null) {
                targetPlayer.sendSystemMessage(Component.literal("§cHas sido expulsado del grupo de aventura por el líder."));
                PacketDistributor.sendToPlayer(targetPlayer, new SyncPartyPayload(false, false, "", 0, List.of()));
            }

            broadcastToParty(party, leader.server,
                    Component.literal("§c✦ §b" + targetName + " §eha sido expulsado del grupo de aventura."));

            syncPartyToMembers(party, leader.server);
            return true;
        }

        return false;
    }

    /**
     * Retorna todas las entidades aliadas en el radio especificado para el auto-targeteo y soporte.
     * Incluye: al propio jugador, miembros de la party, NPCs contratados (aldeanas/aventureros) y mascotas.
     */
    public static List<LivingEntity> getAllies(ServerPlayer player, double radius) {
        List<LivingEntity> allies = new ArrayList<>();
        allies.add(player);

        Party party = getParty(player.getUUID());
        Set<UUID> alliedPlayerUUIDs = new HashSet<>();
        alliedPlayerUUIDs.add(player.getUUID());

        if (party != null) {
            alliedPlayerUUIDs.addAll(party.getMemberUUIDs());
        }

        // Buscar en la esfera alrededor del jugador
        List<LivingEntity> nearby = player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e.isAlive() && e != player
        );

        for (LivingEntity entity : nearby) {
            // Otros jugadores de la party
            if (entity instanceof ServerPlayer otherPlayer) {
                if (alliedPlayerUUIDs.contains(otherPlayer.getUUID())) {
                    allies.add(otherPlayer);
                }
            }
            // Aldeanas contratadas
            else if (entity instanceof TownswomanNpcEntity tw) {
                for (UUID ownerUUID : alliedPlayerUUIDs) {
                    ServerPlayer owner = player.server.getPlayerList().getPlayer(ownerUUID);
                    if (owner != null && tw.isHiredBy(owner)) {
                        allies.add(tw);
                        break;
                    }
                }
            }
            // Aventureros contratados
            else if (entity instanceof AdventurerNpcEntity adv) {
                for (UUID ownerUUID : alliedPlayerUUIDs) {
                    ServerPlayer owner = player.server.getPlayerList().getPlayer(ownerUUID);
                    if (owner != null && adv.isHiredBy(owner)) {
                        allies.add(adv);
                        break;
                    }
                }
            }
            // Mascotas domadas
            else if (entity instanceof TamableAnimal pet) {
                if (pet.getOwnerUUID() != null && alliedPlayerUUIDs.contains(pet.getOwnerUUID())) {
                    allies.add(pet);
                }
            }
        }

        return allies;
    }

    /**
     * Resuelve el UUID del jugador dueño o controlador de una entidad (jugador, aventurero contratado, mascota).
     */
    public static UUID getEntityOwnerUUID(Entity entity) {
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        if (entity instanceof AdventurerNpcEntity adv && adv.isHired()) {
            return adv.getHiringPlayerUUID();
        }
        if (entity instanceof TownswomanNpcEntity tw && tw.isHired()) {
            return tw.getHiringPlayerUUID();
        }
        if (entity instanceof TamableAnimal pet && pet.isTame()) {
            return pet.getOwnerUUID();
        }
        return null;
    }

    /**
     * Comprueba si dos entidades son aliadas dentro del sistema de Party (para No Fuego Amigo).
     * Devuelve true si pertenecen al mismo jugador, a la misma party o son compañeros/mascotas aliados.
     */
    public static boolean isAllied(Entity a, Entity b) {
        if (a == null || b == null) {
            return false;
        }
        if (a == b) {
            return true;
        }

        // Mascotas y animales domesticados son siempre aliados entre sí
        if (a instanceof TamableAnimal petA && petA.isTame() && b instanceof TamableAnimal petB && petB.isTame()) {
            return true;
        }

        UUID ownerA = getEntityOwnerUUID(a);
        UUID ownerB = getEntityOwnerUUID(b);

        if (ownerA == null || ownerB == null) {
            return false;
        }

        // Mismo jugador o misma mascota/acompañante del mismo dueño
        if (ownerA.equals(ownerB)) {
            return true;
        }

        // Comprobar si ambos jugadores pertenecen a la misma party
        Party partyA = getParty(ownerA);
        if (partyA != null && partyA.isMember(ownerB)) {
            return true;
        }

        Party partyB = getParty(ownerB);
        return partyB != null && partyB.isMember(ownerA);
    }

    /**
     * Obtiene los destinatarios de experiencia compartida en el radio especificado.
     * Incluye al jugador, miembros de la party y aventureros aliados contratados.
     */
    public static List<LivingEntity> getPartyXpRecipients(ServerPlayer player, double radius) {
        List<LivingEntity> recipients = new ArrayList<>();
        recipients.add(player);

        Party party = getParty(player.getUUID());
        Set<UUID> alliedPlayerUUIDs = new HashSet<>();
        alliedPlayerUUIDs.add(player.getUUID());

        if (party != null) {
            alliedPlayerUUIDs.addAll(party.getMemberUUIDs());
        }

        List<LivingEntity> nearby = player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e.isAlive() && e != player
        );

        for (LivingEntity entity : nearby) {
            if (entity instanceof ServerPlayer otherPlayer) {
                if (alliedPlayerUUIDs.contains(otherPlayer.getUUID())) {
                    recipients.add(otherPlayer);
                }
            } else if (entity instanceof AdventurerNpcEntity adv && adv.isHired()) {
                if (adv.getHiringPlayerUUID() != null && alliedPlayerUUIDs.contains(adv.getHiringPlayerUUID())) {
                    recipients.add(adv);
                }
            }
        }

        return recipients;
    }

    public static void broadcastToParty(Party party, MinecraftServer server, Component message) {
        for (UUID memberUUID : party.getMemberUUIDs()) {
            ServerPlayer p = server.getPlayerList().getPlayer(memberUUID);
            if (p != null) {
                p.sendSystemMessage(message);
            }
        }
    }

    public static void syncPartyToMembers(Party party, MinecraftServer server) {
        List<PartyMemberInfo> memberInfos = new ArrayList<>();
        int totalRankPoints = 0;
        int count = 0;

        // 1. Jugadores miembros y acompañantes procesados en una sola pasada
        for (UUID memberUUID : party.getMemberUUIDs()) {
            ServerPlayer p = server.getPlayerList().getPlayer(memberUUID);
            if (p != null) {
                PlayerRpgData data = p.getData(ModAttachments.PLAYER_RPG_DATA);
                String rankStr;
                if (data != null && data.isGuildRegistered()) {
                    rankStr = data.getRank().getBadge().getString();
                    totalRankPoints += data.getRank().getIndex();
                } else {
                    rankStr = party.isLeader(memberUUID) ? "§6★ Líder" : "Miembro";
                    totalRankPoints += com.pablo.rpglevels.data.AdventurerRank.F.getIndex();
                }
                count++;

                float curMana = (data != null) ? data.getCurrentMana(p) : 0.0f;
                float maxMana = (data != null) ? data.getMaxMana(p) : 0.0f;

                memberInfos.add(new PartyMemberInfo(
                        memberUUID,
                        p.getName().getString(),
                        p.getHealth(),
                        p.getMaxHealth(),
                        curMana,
                        maxMana,
                        false,
                        rankStr
                ));

                // Escaneo espacial de acompañantes (unificado para memberInfos y cálculo de rango)
                List<LivingEntity> nearbyCompanions = p.serverLevel().getEntitiesOfClass(
                        LivingEntity.class,
                        p.getBoundingBox().inflate(48),
                        e -> (e instanceof TownswomanNpcEntity tw && tw.isHiredBy(p))
                                || (e instanceof AdventurerNpcEntity adv && adv.isHiredBy(p))
                );

                for (LivingEntity comp : nearbyCompanions) {
                    if (comp instanceof AdventurerNpcEntity adv) {
                        totalRankPoints += adv.getRank().getIndex();
                        count++;
                    }
                    String role = (comp instanceof TownswomanNpcEntity) ? "Aldeana Vasalla" : "Aventurero Escolta";
                    memberInfos.add(new PartyMemberInfo(
                            comp.getUUID(),
                            comp.getDisplayName().getString(),
                            comp.getHealth(),
                            comp.getMaxHealth(),
                            0.0f,
                            0.0f,
                            true,
                            "§a" + role
                    ));
                }
            } else {
                memberInfos.add(new PartyMemberInfo(
                        memberUUID,
                        party.getMemberNames().getOrDefault(memberUUID, "Desconectado"),
                        0.0f,
                        20.0f,
                        0.0f,
                        0.0f,
                        false,
                        "§7Desconectado"
                ));
            }
        }

        // Calcular rango promedio del grupo directamente sin redundancia
        int avgRank = (count > 0) ? (int) Math.round((double) totalRankPoints / count) : com.pablo.rpglevels.data.AdventurerRank.F.getIndex();
        com.pablo.rpglevels.data.AdventurerRank partyRank = com.pablo.rpglevels.data.AdventurerRank.fromIndex(avgRank);

        // Enviar paquete sincronizado a cada miembro
        for (UUID memberUUID : party.getMemberUUIDs()) {
            ServerPlayer p = server.getPlayerList().getPlayer(memberUUID);
            if (p != null) {
                boolean isLeader = party.isLeader(memberUUID);
                PacketDistributor.sendToPlayer(p, new SyncPartyPayload(true, isLeader, party.getLeaderName(), partyRank.getIndex(), memberInfos));
            }
        }
    }

    public static com.pablo.rpglevels.data.AdventurerRank calculatePartyRank(Party party, MinecraftServer server) {
        if (party == null || server == null) return com.pablo.rpglevels.data.AdventurerRank.F;

        int totalRankPoints = 0;
        int count = 0;

        for (UUID memberUUID : party.getMemberUUIDs()) {
            ServerPlayer p = server.getPlayerList().getPlayer(memberUUID);
            if (p != null) {
                PlayerRpgData data = p.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null && data.isGuildRegistered()) {
                    totalRankPoints += data.getRank().getIndex();
                } else {
                    totalRankPoints += com.pablo.rpglevels.data.AdventurerRank.F.getIndex();
                }
                count++;

                List<AdventurerNpcEntity> companions = p.serverLevel().getEntitiesOfClass(
                        AdventurerNpcEntity.class,
                        p.getBoundingBox().inflate(48),
                        adv -> adv.isHiredBy(p)
                );
                for (AdventurerNpcEntity adv : companions) {
                    totalRankPoints += adv.getRank().getIndex();
                    count++;
                }
            }
        }

        if (count == 0) return com.pablo.rpglevels.data.AdventurerRank.F;
        int avg = (int) Math.round((double) totalRankPoints / count);
        return com.pablo.rpglevels.data.AdventurerRank.fromIndex(avg);
    }

    /**
     * Sincronización periódica de vida y miembros cada 20 ticks (1 segundo).
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0 || PARTIES_BY_ID.isEmpty()) return;

        MinecraftServer server = event.getServer();
        for (Party party : PARTIES_BY_ID.values()) {
            syncPartyToMembers(party, server);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PENDING_INVITES.remove(player.getUUID());
        }
    }
}

