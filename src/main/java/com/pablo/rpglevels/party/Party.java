package com.pablo.rpglevels.party;

import java.util.*;

public class Party {
    private final UUID partyId;
    private UUID leaderUUID;
    private String leaderName;
    private final Set<UUID> memberUUIDs = new LinkedHashSet<>();
    private final Map<UUID, String> memberNames = new HashMap<>();

    public Party(UUID leaderUUID, String leaderName) {
        this.partyId = UUID.randomUUID();
        this.leaderUUID = leaderUUID;
        this.leaderName = leaderName;
        this.memberUUIDs.add(leaderUUID);
        this.memberNames.put(leaderUUID, leaderName);
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getLeaderUUID() {
        return leaderUUID;
    }

    public void setLeader(UUID newLeader, String name) {
        this.leaderUUID = newLeader;
        this.leaderName = name;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public Set<UUID> getMemberUUIDs() {
        return Collections.unmodifiableSet(memberUUIDs);
    }

    public Map<UUID, String> getMemberNames() {
        return Collections.unmodifiableMap(memberNames);
    }

    public static final int MAX_MEMBERS = 4;

    public boolean isLeader(UUID playerUUID) {
        return leaderUUID.equals(playerUUID);
    }

    public boolean isMember(UUID playerUUID) {
        return memberUUIDs.contains(playerUUID);
    }

    public boolean addMember(UUID playerUUID, String name) {
        if (memberUUIDs.size() >= MAX_MEMBERS) {
            return false; // Límite máximo de 4 miembros por grupo de aventura
        }
        memberNames.put(playerUUID, name);
        return memberUUIDs.add(playerUUID);
    }

    public boolean removeMember(UUID playerUUID) {
        memberNames.remove(playerUUID);
        return memberUUIDs.remove(playerUUID);
    }

    public int getMemberCount() {
        return memberUUIDs.size();
    }
}

