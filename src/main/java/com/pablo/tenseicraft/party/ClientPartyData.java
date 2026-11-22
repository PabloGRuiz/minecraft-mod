package com.pablo.tenseicraft.party;

import com.pablo.tenseicraft.data.AdventurerRank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientPartyData {

    private static boolean inParty = false;
    private static boolean isLeader = false;
    private static String leaderName = "";
    private static int partyRankIndex = 0;
    private static List<PartyMemberInfo> members = new ArrayList<>();

    public static synchronized void update(boolean partyActive, boolean leader, String leaderNameStr, int rankIndex, List<PartyMemberInfo> memberList) {
        inParty = partyActive;
        isLeader = leader;
        leaderName = leaderNameStr;
        partyRankIndex = rankIndex;
        members = new ArrayList<>(memberList);
    }

    public static synchronized boolean isInParty() {
        return inParty;
    }

    public static synchronized boolean isLeader() {
        return isLeader;
    }

    public static synchronized String getLeaderName() {
        return leaderName;
    }

    public static synchronized int getPartyRankIndex() {
        return partyRankIndex;
    }

    public static synchronized AdventurerRank getPartyRank() {
        return AdventurerRank.fromIndex(partyRankIndex);
    }

    public static synchronized List<PartyMemberInfo> getMembers() {
        return Collections.unmodifiableList(members);
    }
}
