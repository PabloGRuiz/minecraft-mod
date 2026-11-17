package com.pablo.rpglevels.network;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.quests.PlayerQuestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // RPG Data
        registrar.playToClient(
                SyncRpgDataPayload.TYPE,
                SyncRpgDataPayload.STREAM_CODEC,
                SyncRpgDataPayload::handle
        );

        // Quest Data Sync
        registrar.playToClient(
                SyncQuestDataPayload.TYPE,
                SyncQuestDataPayload.STREAM_CODEC,
                SyncQuestDataPayload::handle
        );

        // Quest HUD Toast
        registrar.playToClient(
                QuestToastPayload.TYPE,
                QuestToastPayload.STREAM_CODEC,
                QuestToastPayload::handle
        );

        // Quest Action (Client -> Server)
        registrar.playToServer(
                QuestActionPayload.TYPE,
                QuestActionPayload.STREAM_CODEC,
                QuestActionPayload::handle
        );

        // Skill Unlocked (Tensura Voice of the World Announcement)
        registrar.playToClient(
                SkillUnlockedPayload.TYPE,
                SkillUnlockedPayload.STREAM_CODEC,
                SkillUnlockedPayload::handle
        );

        // Cast Spell (Client -> Server)
        registrar.playToServer(
                CastSpellPayload.TYPE,
                CastSpellPayload.STREAM_CODEC,
                CastSpellPayload::handle
        );

        // Perform Dash (Client -> Server)
        registrar.playToServer(
                PerformDashPayload.TYPE,
                PerformDashPayload.STREAM_CODEC,
                PerformDashPayload::handle
        );

        // Perform Double Jump (Client -> Server)
        registrar.playToServer(
                PerformDoubleJumpPayload.TYPE,
                PerformDoubleJumpPayload.STREAM_CODEC,
                PerformDoubleJumpPayload::handle
        );

        // Open NPC Dialogue (Server -> Client)
        registrar.playToClient(
                OpenNpcDialoguePayload.TYPE,
                OpenNpcDialoguePayload.STREAM_CODEC,
                OpenNpcDialoguePayload::handle
        );

        // NPC Dialogue Action (Client -> Server)
        registrar.playToServer(
                NpcDialogueActionPayload.TYPE,
                NpcDialogueActionPayload.STREAM_CODEC,
                NpcDialogueActionPayload::handle
        );

        // Buy Spiritual Skill from Alice (Client -> Server)
        registrar.playToServer(
                BuySpiritualSkillPayload.TYPE,
                BuySpiritualSkillPayload.STREAM_CODEC,
                BuySpiritualSkillPayload::handle
        );

        // Unlock Skill (Client -> Server)
        registrar.playToServer(
                UnlockSkillPayload.TYPE,
                UnlockSkillPayload.STREAM_CODEC,
                UnlockSkillPayload::handle
        );

        // Sync Mana (Server -> Client)
        registrar.playToClient(
                SyncManaPayload.TYPE,
                SyncManaPayload.STREAM_CODEC,
                SyncManaPayload::handle
        );

        // Sync Party (Server -> Client)
        registrar.playToClient(
                SyncPartyPayload.TYPE,
                SyncPartyPayload.STREAM_CODEC,
                SyncPartyPayload::handle
        );

        // Party Action (Client -> Server)
        registrar.playToServer(
                PartyActionPayload.TYPE,
                PartyActionPayload.STREAM_CODEC,
                PartyActionPayload::handle
        );

        // Open Party Screen (Server -> Client)
        registrar.playToClient(
                OpenPartyScreenPayload.TYPE,
                OpenPartyScreenPayload.STREAM_CODEC,
                OpenPartyScreenPayload::handle
        );

        // Toggle Sword Release / Summon Spirit (Client -> Server)
        registrar.playToServer(
                ToggleSwordReleasePayload.TYPE,
                ToggleSwordReleasePayload.STREAM_CODEC,
                ToggleSwordReleasePayload::handle
        );

        // Rename Companion (Client -> Server)
        registrar.playToServer(
                RenameCompanionPayload.TYPE,
                RenameCompanionPayload.STREAM_CODEC,
                RenameCompanionPayload::handle
        );

        // Open Owner-Bound Adventurer Card (Server -> Client)
        registrar.playToClient(
                OpenAdventurerCardPayload.TYPE,
                OpenAdventurerCardPayload.STREAM_CODEC,
                OpenAdventurerCardPayload::handle
        );

        // Sync Entity Ki Aura (Server -> Client)
        registrar.playToClient(
                SyncEntityAuraPayload.TYPE,
                SyncEntityAuraPayload.STREAM_CODEC,
                SyncEntityAuraPayload::handle
        );

        // Sync Oppressive Cylindrical Aura (Server -> Client)
        registrar.playToClient(
                SyncOppressiveAuraPayload.TYPE,
                SyncOppressiveAuraPayload.STREAM_CODEC,
                SyncOppressiveAuraPayload::handle
        );

        // Beam Clash: Left Click Spam (Client -> Server)
        registrar.playToServer(
                BeamClickSpamPayload.TYPE,
                BeamClickSpamPayload.STREAM_CODEC,
                BeamClickSpamPayload::handle
        );

        // Beam Clash: State Sync (Server -> Client)
        registrar.playToClient(
                SyncBeamClashPayload.TYPE,
                SyncBeamClashPayload.STREAM_CODEC,
                SyncBeamClashPayload::handle
        );

        // Beam Clash: Resolution & Explosion (Server -> Client)
        registrar.playToClient(
                EndBeamClashPayload.TYPE,
                EndBeamClashPayload.STREAM_CODEC,
                EndBeamClashPayload::handle
        );

        // Power Control Settings (Client -> Server)
        registrar.playToServer(
                UpdatePowerControlPayload.TYPE,
                UpdatePowerControlPayload.STREAM_CODEC,
                UpdatePowerControlPayload::handle
        );

        // RPG XP Gain Notification (Server -> Client)
        registrar.playToClient(
                SyncXpGainPayload.TYPE,
                SyncXpGainPayload.STREAM_CODEC,
                SyncXpGainPayload::handle
        );
    }

    public static void openPartyScreen(ServerPlayer player) {
        if (player != null) {
            PacketDistributor.sendToPlayer(player, new OpenPartyScreenPayload());
        }
    }

    public static void sendPartyAction(int action, String targetData) {
        PacketDistributor.sendToServer(new PartyActionPayload(action, targetData != null ? targetData : ""));
    }

    public static void syncMana(ServerPlayer player) {
        if (player == null) return;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            float current = data.getCurrentMana(player);
            float max = data.getMaxMana(player);
            PacketDistributor.sendToPlayer(player, new SyncManaPayload(current, max));
        }
    }

    public static void sendNpcDialogueAction(int entityId, int actionType) {
        PacketDistributor.sendToServer(new NpcDialogueActionPayload(entityId, actionType));
    }

    public static void sendCastSpell(String spellId) {
        PacketDistributor.sendToServer(new CastSpellPayload(spellId));
    }

    public static void sendBuySpiritualSkill(String skillId) {
        PacketDistributor.sendToServer(new BuySpiritualSkillPayload(skillId));
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            CompoundTag tag = data.serializeNBT(player.registryAccess());
            PacketDistributor.sendToPlayer(player, new SyncRpgDataPayload(tag));
        }
    }

    public static void syncQuestsToPlayer(ServerPlayer player) {
        if (player == null) return;
        PlayerQuestData data = player.getData(ModAttachments.PLAYER_QUEST_DATA);
        if (data != null) {
            CompoundTag tag = data.serializeNBT(player.registryAccess());
            PacketDistributor.sendToPlayer(player, new SyncQuestDataPayload(tag));
        }
    }

    public static void sendQuestToast(ServerPlayer player, String title, String progress, boolean isCompleted) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new QuestToastPayload(title, progress, isCompleted));
    }

    public static void sendSkillUnlocked(ServerPlayer player, String skillId, String skillName, String skillCategory, String description) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new SkillUnlockedPayload(skillId, skillName, skillCategory, description));
    }

    public static void sendXpGain(ServerPlayer player, int gainedAmount, int currentXp, int maxXp, int currentLevel, boolean atCap) {
        if (player == null) return;
        PacketDistributor.sendToPlayer(player, new SyncXpGainPayload(gainedAmount, currentXp, maxXp, currentLevel, atCap));
    }
}
