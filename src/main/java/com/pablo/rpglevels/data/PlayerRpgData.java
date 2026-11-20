package com.pablo.rpglevels.data;

import com.pablo.rpglevels.items.AncientRingItem;
import com.pablo.rpglevels.items.ExperienceAmuletItem;
import com.pablo.rpglevels.items.WarriorRingItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerRpgData implements INBTSerializable<CompoundTag> {
    private final Map<Integer, StatType> chosenMilestones = new HashMap<>();
    private final Set<String> unlockedSkills = new HashSet<>();
    private int lastNotifiedMilestone = 0;
    private int rankIndex = 0; // Default to 0 (Rank F) once registered
    private boolean isGuildRegistered = false;
    private boolean hasReceivedAdventurerCard = false;

    // Allocated attribute points from RPG Level progression (allocated 1 by 1)
    private int currentLifeFortitude = 0;
    private int currentLifeDexterity = 0;
    private int currentLifeSpirit = 0;
    private int currentLifeLuck = 0;

    // Base/Permanent attributes (rolled 1..10 at birth, standard baseline = 5)
    private int permanentFortitude = 5;
    private int permanentDexterity = 5;
    private int permanentSpirit = 5;
    private int permanentLuck = 5;
    private boolean birthStatsRolled = false;

    // Divine Blessing (Falna) & Independent RPG Level Progression
    private boolean divineBlessingUnlocked = false;
    private String patronGodId = "";
    private int rpgLevel = 1;
    private int rpgXp = 0;

    // Channel Magic skill progression & temporary spirit surge
    private int channelMagicUses = 0;
    private long spiritSurgeUntilTick = 0;
    private int spiritSurgeBonus = 0;

    // Dragon Ball Ki Aura State & Sense
    private long auraVisibleUntilTick = 0;
    private long auraSenseUntilTick = 0;
    private long oppressiveAuraUntilTick = 0;

    // Resonancia Espiritual (Modo Supremo a 200 de Afinidad)
    private long spiritualResonanceUntilTick = 0;
    private long spiritualResonanceCinematicUntilTick = 0;

    // Guardián Espiritual Vinculado (Obtenido al ascender a Rango B: "starry", "chubby", "brambi")
    public static final int MAX_GUARDIAN_VITAL_ENERGY = 100;
    private String chosenGuardian = "";
    private java.util.UUID summonedGuardianUuid = null;
    private int guardianVitalEnergy = 0;

    // 360 Dash / Dodge State
    private long lastDashTick = -10000L;
    private long dashIFrameUntilTick = -100;

    // Bonus pending points granted via commands / rewards
    private int bonusPendingPoints = 0;

    // Double Jump State
    private boolean canDoubleJumpAirborne = true;
    private long lastDoubleJumpTick = -100;
    private int jumpCount = 0;

    // Power Control & Suppression System
    private float powerOutputRatio = 1.0f; // 0.01f a 1.0f (1% a 100%)
    private float moveSpeedRatio = 1.0f;   // 0.10f a 1.0f (10% a 100%)
    private float attackSpeedRatio = 1.0f; // 0.10f a 1.0f (10% a 100%)
    private float jumpHeightRatio = 1.0f;  // 0.00f a 1.0f (0% a 100%)

    // Current Mana (starts initialized to maxMana)
    private float currentMana = -1.0f;

    // Reputation & Morality System (-100 to +100, starts at 0 = Neutral)
    private int reputationScore = 0;

    // Physical Skill Tree & God of War Trial State
    private boolean hasKilledFirstMonster = false;
    private boolean warTrialCompleted = false;
    private int physicalSkillPoints = 0;

    // Alice / Spiritual Reapers Quest State
    private boolean aliceQuestActive = false;
    private int aliceDMonstersKilled = 0;
    private boolean aliceQuestCompleted = false;
    private boolean receivedSpiritualSword = false;
    private boolean firstReleaseUnlocked = false;

    // Espada Espiritual Única por Partida (se genera aleatoriamente una sola vez por mundo/partida)
    private boolean soulBladeRolled = false;
    private String soulBladeStyle = "";
    private String soulBladeElement = "";
    private String soulBladeRelease = "";
    private String soulBladeResonance = "";
    private int soulBladeVariant = 0;

    // Segadores Espirituales (Mushoku Tensei Tier System)
    private int spiritualTierLevel = 1;
    private boolean spiritualEngineer = false;

    // Transformación / Manto Celestial (Hagoromo divino estilo Gear 5)
    private boolean celestialMantleActive = false;
    private long celestialMantleUntilTick = 0L;

    // Mana suppression while First Release is active
    private float lastReleasedManaCap = -1.0f;

    // Bandit Nemesis / Revenge System
    private int banditsKilledToday = 0;
    private int banditsKilledYesterday = 0;
    private long lastAmbushCheckDay = -1L;
    private boolean pendingAmbushToday = false;

    // Cooldown de Técnicas Espirituales (spellId -> gameTick en que termina el enfriamiento)
    private final Map<String, Long> skillCooldownUntilTick = new HashMap<>();
    private final Map<String, Integer> skillCooldownTotalTicks = new HashMap<>();

    // Nombre de ubicación actual (Reino / Aldea)
    private String currentLocationName = "";

    public PlayerRpgData() {
    }

    public boolean hasKilledFirstMonster() {
        return hasKilledFirstMonster;
    }

    public void setHasKilledFirstMonster(boolean hasKilled) {
        this.hasKilledFirstMonster = hasKilled;
    }

    public boolean isWarTrialCompleted() {
        return warTrialCompleted;
    }

    public void setWarTrialCompleted(boolean completed) {
        this.warTrialCompleted = completed;
    }

    public boolean isAliceQuestActive() {
        return aliceQuestActive;
    }

    public void setAliceQuestActive(boolean active) {
        this.aliceQuestActive = active;
    }

    public int getAliceDMonstersKilled() {
        return aliceDMonstersKilled;
    }

    public void setAliceDMonstersKilled(int killed) {
        this.aliceDMonstersKilled = killed;
    }

    public void incrementAliceDMonstersKilled() {
        this.aliceDMonstersKilled++;
    }

    public boolean isAliceQuestCompleted() {
        return aliceQuestCompleted;
    }

    public void setAliceQuestCompleted(boolean completed) {
        this.aliceQuestCompleted = completed;
    }

    public boolean hasReceivedSpiritualSword() {
        return receivedSpiritualSword;
    }

    public void setReceivedSpiritualSword(boolean received) {
        this.receivedSpiritualSword = received;
    }

    public boolean isFirstReleaseUnlocked() {
        return firstReleaseUnlocked;
    }

    public void setFirstReleaseUnlocked(boolean unlocked) {
        this.firstReleaseUnlocked = unlocked;
    }

    public boolean hasRolledSoulBlade() {
        return soulBladeRolled;
    }

    public void setSoulBladeTraits(String style, String element, String release, String resonance) {
        setSoulBladeTraits(style, element, release, resonance, 0);
    }

    public void setSoulBladeTraits(String style, String element, String release, String resonance, int variant) {
        this.soulBladeRolled = true;
        this.soulBladeStyle = style != null ? style : "";
        this.soulBladeElement = element != null ? element : "";
        this.soulBladeRelease = release != null ? release : "";
        this.soulBladeResonance = resonance != null ? resonance : "";
        this.soulBladeVariant = Math.max(0, variant % 2);
    }

    public void resetSoulBladeTraits() {
        this.soulBladeRolled = false;
        this.soulBladeStyle = "";
        this.soulBladeElement = "";
        this.soulBladeRelease = "";
        this.soulBladeResonance = "";
        this.soulBladeVariant = 0;
    }

    public int getSoulBladeVariant() {
        return soulBladeVariant;
    }

    public void setSoulBladeVariant(int soulBladeVariant) {
        this.soulBladeVariant = Math.max(0, soulBladeVariant % 2);
    }

    public String getSoulBladeStyle() {
        return soulBladeStyle;
    }

    public void setSoulBladeStyle(String soulBladeStyle) {
        this.soulBladeStyle = soulBladeStyle != null ? soulBladeStyle : "";
    }

    public String getSoulBladeElement() {
        return soulBladeElement;
    }

    public void setSoulBladeElement(String soulBladeElement) {
        this.soulBladeElement = soulBladeElement != null ? soulBladeElement : "";
    }

    public String getSoulBladeRelease() {
        return soulBladeRelease;
    }

    public void setSoulBladeRelease(String soulBladeRelease) {
        this.soulBladeRelease = soulBladeRelease != null ? soulBladeRelease : "";
    }

    public String getSoulBladeResonance() {
        return soulBladeResonance;
    }

    public void setSoulBladeResonance(String soulBladeResonance) {
        this.soulBladeResonance = soulBladeResonance != null ? soulBladeResonance : "";
    }

    public int getSpiritualTierLevel() {
        return spiritualTierLevel;
    }

    public void setSpiritualTierLevel(int level) {
        this.spiritualTierLevel = Math.max(1, Math.min(7, level));
    }

    public com.pablo.rpglevels.magic.spiritual.SpiritualTier getSpiritualTier() {
        return com.pablo.rpglevels.magic.spiritual.SpiritualTier.fromLevel(spiritualTierLevel);
    }

    public boolean isSpiritualEngineer() {
        return spiritualEngineer;
    }

    public void setSpiritualEngineer(boolean isEngineer) {
        this.spiritualEngineer = isEngineer;
    }

    public String getSpiritualTitle() {
        return getSpiritualTier().getTitle(spiritualEngineer);
    }

    public int getReputationScore() {
        return reputationScore;
    }

    public void setReputationScore(int score) {
        this.reputationScore = Math.max(-100, Math.min(100, score));
    }

    public int addReputation(int delta) {
        int oldScore = this.reputationScore;
        this.reputationScore = Math.max(-100, Math.min(100, this.reputationScore + delta));
        return this.reputationScore - oldScore;
    }

    public ReputationTier getReputationTier() {
        return ReputationTier.fromScore(this.reputationScore);
    }

    public boolean isCriminal() {
        return reputationScore < 0;
    }

    public int getPhysicalSkillPoints() {
        return physicalSkillPoints;
    }

    public void addPhysicalSkillPoints(int points) {
        this.physicalSkillPoints = Math.max(0, this.physicalSkillPoints + points);
    }

    public void setPhysicalSkillPoints(int points) {
        this.physicalSkillPoints = Math.max(0, points);
    }

    public boolean unlockPhysicalSkill(String skillId) {
        if (physicalSkillPoints < 1 || unlockedSkills.contains(skillId)) {
            return false;
        }
        // Validate prerequisites
        if (skillId.equals("agility_double_jump") && !hasSkill("agility_dash")) {
            return false;
        }
        if (skillId.equals("agility_air_dash") && !hasSkill("agility_double_jump")) {
            return false;
        }
        if (skillId.equals("strength_2") && !hasSkill("strength_1")) {
            return false;
        }
        if (skillId.equals("strength_3") && !hasSkill("strength_2")) {
            return false;
        }
        if (skillId.equals("fortitude_2") && !hasSkill("fortitude_1")) {
            return false;
        }
        if (skillId.equals("fortitude_3") && !hasSkill("fortitude_2")) {
            return false;
        }

        physicalSkillPoints--;
        unlockedSkills.add(skillId);
        return true;
    }

    public void resetPhysicalSkills() {
        int refunded = 0;
        Set<String> toRemove = new HashSet<>();
        for (String s : unlockedSkills) {
            if (s.startsWith("agility_") || s.startsWith("strength_") || s.startsWith("fortitude_")) {
                refunded++;
                toRemove.add(s);
            }
        }
        unlockedSkills.removeAll(toRemove);
        physicalSkillPoints += refunded;
    }

    public int getBonusPhysicalAttackDamage() {
        int bonus = 0;
        if (hasSkill("strength_1")) bonus++;
        if (hasSkill("strength_2")) bonus++;
        if (hasSkill("strength_3")) bonus++;
        if (hasSkill("fortitude_1")) bonus++;
        if (hasSkill("fortitude_2")) bonus++;
        if (hasSkill("fortitude_3")) bonus++;
        return bonus;
    }

    public boolean isDashing(long currentTick) {
        return currentTick < dashIFrameUntilTick;
    }

    public void triggerDash(long currentTick, int iFrameTicks) {
        this.dashIFrameUntilTick = currentTick + iFrameTicks;
        this.lastDashTick = currentTick;
    }

    public static final int BASE_DASH_COOLDOWN_TICKS = 600; // 30 segundos (3 veces menos que antes)
    public static final float DASH_MANA_COST = 20.0f; // Consume 20 PM al dashear

    public long getLastDashTick() {
        return lastDashTick;
    }

    public int getDashCooldownTicks(Player player) {
        int dexterity = (player != null) ? getTotalStatPoints(player, StatType.DEXTERITY) : 0;
        // Base 600 ticks (30s), reducido en 10 ticks (0.5s) por cada punto de Destreza hasta un mínimo de 60 ticks (3s)
        int reduction = dexterity * 10;
        int baseCd = Math.max(60, BASE_DASH_COOLDOWN_TICKS - reduction);
        if (this.divineBlessingUnlocked && getPatronGod() == GreekGod.HERMES) {
            int tier = GreekGod.getBlessingTier(getRank());
            double mult = tier == 1 ? 0.80 : (tier == 2 ? 0.70 : 0.65);
            return Math.max(40, (int) Math.round(baseCd * mult));
        }
        return baseCd;
    }

    public long getDashCooldownRemainingTicks(long currentTick, Player player) {
        if (lastDashTick < 0) {
            return 0L;
        }
        long until = lastDashTick + getDashCooldownTicks(player);
        return Math.max(0L, until - currentTick);
    }

    public boolean canDash(long currentTick, Player player) {
        return hasSkill("agility_dash")
                && (lastDashTick < 0 || (currentTick - lastDashTick) >= getDashCooldownTicks(player))
                && (player == null || getCurrentMana(player) >= DASH_MANA_COST);
    }

    public int getJumpCount() {
        return jumpCount;
    }

    public void setJumpCount(int count) {
        this.jumpCount = Math.max(0, count);
    }

    public boolean incrementJumpCount(Player player) {
        this.jumpCount++;
        if (this.jumpCount >= 2000 && !hasSkill("agility_double_jump")) {
            unlockedSkills.add("agility_double_jump");
            return true;
        }
        return false;
    }

    public boolean canDoubleJumpAirborne() {
        return hasSkill("agility_double_jump") && canDoubleJumpAirborne;
    }

    public void setCanDoubleJumpAirborne(boolean can) {
        this.canDoubleJumpAirborne = can;
    }

    public long getLastDoubleJumpTick() {
        return lastDoubleJumpTick;
    }

    public void triggerDoubleJump(long currentTick) {
        this.canDoubleJumpAirborne = false;
        this.lastDoubleJumpTick = currentTick;
    }

    public float getMaxMana(Player player) {
        if (player != null && com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            float ironsMax = com.pablo.rpglevels.compat.irons.IronsSpellsCompat.getIronsMaxMana(player);
            if (ironsMax > 0.0f) {
                return ironsMax;
            }
        }
        int tier = getChannelMagicTier();
        int spirit = (player != null) ? getTotalStatPoints(player, StatType.SPIRIT) : 5;
        // Standard baseline at 5 Spirit (tier 1) = 100.0 MP. +/- 10 MP per Spirit point relative to 5, +25 MP per extra channel tier
        return Math.max(20.0f, 100.0f + ((spirit - 5) * 10.0f) + ((tier - 1) * 25.0f));
    }

    public float getCurrentMana(Player player) {
        if (player != null && com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            float ironsMana = com.pablo.rpglevels.compat.irons.IronsSpellsCompat.getIronsMana(player);
            if (ironsMana >= 0.0f) {
                this.currentMana = ironsMana;
                return ironsMana;
            }
        }
        if (currentMana < 0.0f) {
            currentMana = getMaxMana(player);
        }
        return Math.min(currentMana, getMaxMana(player));
    }

    public void setCurrentMana(float mana) {
        this.currentMana = Math.max(0.0f, mana);
    }

    public void setCurrentMana(Player player, float mana) {
        this.currentMana = Math.max(0.0f, mana);
        if (player != null && com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            com.pablo.rpglevels.compat.irons.IronsSpellsCompat.setIronsMana(player, this.currentMana);
        }
    }

    public boolean consumeMana(Player player, float amount) {
        if (player != null && com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            boolean consumed = com.pablo.rpglevels.compat.irons.IronsSpellsCompat.consumeIronsMana(player, amount);
            if (consumed) {
                this.currentMana = com.pablo.rpglevels.compat.irons.IronsSpellsCompat.getIronsMana(player);
                return true;
            }
            return false;
        }
        float current = getCurrentMana(player);
        if (current >= amount) {
            this.currentMana = current - amount;
            return true;
        }
        return false;
    }

    public void regenerateMana(Player player, float amount) {
        if (player != null && isHoldingReleasedSpiritualSword(player)) {
            // Con la Primera Liberación activa, el espíritu está en trance: el maná NO se regenera pasivamente
            return;
        }
        if (player != null && com.pablo.rpglevels.compat.irons.IronsSpellsCompat.isLoaded()) {
            // Iron's Spells gestiona su propia regeneración continua vía AttributeRegistry.MANA_REGEN
            return;
        }
        float max = getMaxMana(player);
        float current = getCurrentMana(player);
        this.currentMana = Math.min(max, current + amount);
    }

    public boolean isSkillOnCooldown(String skillId, long currentTick) {
        Long until = skillCooldownUntilTick.get(skillId);
        return until != null && currentTick < until;
    }

    public long getSkillCooldownRemainingTicks(String skillId, long currentTick) {
        Long until = skillCooldownUntilTick.get(skillId);
        if (until == null) return 0;
        return Math.max(0, until - currentTick);
    }

    public int getSkillCooldownTotalTicks(String skillId) {
        return skillCooldownTotalTicks.getOrDefault(skillId, 100);
    }

    public Map<String, Long> getSkillCooldownUntilMap() {
        return Collections.unmodifiableMap(skillCooldownUntilTick);
    }

    /**
     * Reduce cooldown ticks proportionally with player Dexterity (-1.2% per point, max -60% at 50 Dex).
     */
    public int getGenericCooldownTicks(Player player, int baseTicks) {
        int dex = (player != null) ? getTotalStatPoints(player, StatType.DEXTERITY) : 0;
        float cdr = Math.min(0.60f, Math.max(0, dex) * 0.012f);
        return Math.max(Math.min(baseTicks, 20), Math.round(baseTicks * (1.0f - cdr)));
    }

    public int getTechniqueCooldownTicks(Player player, com.pablo.rpglevels.magic.SpiritualTechnique tech) {
        return getGenericCooldownTicks(player, tech.getCooldownTicks());
    }

    public void setSkillCooldown(String skillId, long currentTick, int cooldownTicks) {
        skillCooldownUntilTick.put(skillId, currentTick + cooldownTicks);
        skillCooldownTotalTicks.put(skillId, Math.max(1, cooldownTicks));
    }

    public float getLastReleasedManaCap() {
        return lastReleasedManaCap;
    }

    public void setLastReleasedManaCap(float cap) {
        this.lastReleasedManaCap = cap;
    }

    public int getBanditsKilledToday() {
        return banditsKilledToday;
    }

    public void incrementBanditsKilledToday() {
        this.banditsKilledToday++;
    }

    public int getBanditsKilledYesterday() {
        return banditsKilledYesterday;
    }

    public void setBanditsKilledYesterday(int count) {
        this.banditsKilledYesterday = count;
    }

    public long getLastAmbushCheckDay() {
        return lastAmbushCheckDay;
    }

    public void setLastAmbushCheckDay(long day) {
        this.lastAmbushCheckDay = day;
    }

    public boolean isPendingAmbushToday() {
        return pendingAmbushToday;
    }

    public void setPendingAmbushToday(boolean pending) {
        this.pendingAmbushToday = pending;
    }

    public void advanceBanditDay(long newDay) {
        this.banditsKilledYesterday = this.banditsKilledToday;
        this.banditsKilledToday = 0;
        this.lastAmbushCheckDay = newDay;
    }

    public int getChannelMagicUses() {
        return channelMagicUses;
    }

    public void setChannelMagicUses(int uses) {
        this.channelMagicUses = Math.max(0, uses);
    }

    public int getChannelMagicTier() {
        if (channelMagicUses >= 100) return 5;
        if (channelMagicUses >= 65) return 4;
        if (channelMagicUses >= 35) return 3;
        if (channelMagicUses >= 15) return 2;
        return 1;
    }

    public boolean incrementChannelMagicUses() {
        int oldTier = getChannelMagicTier();
        this.channelMagicUses++;
        int newTier = getChannelMagicTier();
        return newTier > oldTier;
    }

    public void activateSpiritSurge(long currentTick, int durationTicks, int bonus) {
        this.spiritSurgeUntilTick = currentTick + durationTicks;
        this.spiritSurgeBonus = bonus;
    }

    public boolean isSpiritSurgeActive(long currentTick) {
        return currentTick < spiritSurgeUntilTick;
    }

    public int getActiveSpiritSurgeBonus(long currentTick) {
        return isSpiritSurgeActive(currentTick) ? spiritSurgeBonus : 0;
    }

    public void activateAura(long currentTick, int durationTicks) {
        this.auraVisibleUntilTick = currentTick + durationTicks;
        this.auraSenseUntilTick = currentTick + durationTicks;
    }

    public boolean isAuraVisible(long currentTick) {
        return currentTick < auraVisibleUntilTick;
    }

    public boolean hasAuraSenseActive(long currentTick) {
        return currentTick < auraSenseUntilTick;
    }

    public long getAuraVisibleUntilTick() {
        return auraVisibleUntilTick;
    }

    public long getAuraSenseUntilTick() {
        return auraSenseUntilTick;
    }

    public void activateOppressiveAura(long currentTick, int durationTicks) {
        this.oppressiveAuraUntilTick = currentTick + durationTicks;
    }

    public boolean isOppressiveAuraActive(long currentTick) {
        return currentTick < oppressiveAuraUntilTick;
    }

    public long getOppressiveAuraUntilTick() {
        return oppressiveAuraUntilTick;
    }

    public boolean isCelestialMantleActive(long currentTick) {
        return celestialMantleActive || (currentTick > 0 && currentTick < celestialMantleUntilTick);
    }

    public boolean isCelestialMantleActive() {
        return celestialMantleActive;
    }

    public void setCelestialMantleActive(boolean active) {
        this.celestialMantleActive = active;
    }

    public void activateCelestialMantle(long currentTick, int durationTicks) {
        this.celestialMantleUntilTick = currentTick + durationTicks;
    }

    public long getCelestialMantleUntilTick() {
        return celestialMantleUntilTick;
    }

    // Métodos alias para compatibilidad
    public boolean isDivineSmokeActive(long currentTick) { return isCelestialMantleActive(currentTick); }
    public boolean isDivineSmokeActive() { return isCelestialMantleActive(); }
    public void setDivineSmokeActive(boolean active) { setCelestialMantleActive(active); }
    public void activateDivineSmoke(long currentTick, int durationTicks) { activateCelestialMantle(currentTick, durationTicks); }

    public void activateSpiritualResonance(long currentTick, int durationTicks, int cinematicTicks) {
        this.spiritualResonanceUntilTick = currentTick + durationTicks;
        this.spiritualResonanceCinematicUntilTick = currentTick + cinematicTicks;
        this.auraVisibleUntilTick = currentTick + durationTicks;
        this.oppressiveAuraUntilTick = currentTick + cinematicTicks;
    }

    public boolean isSpiritualResonanceActive(long currentTick) {
        return currentTick < spiritualResonanceUntilTick;
    }

    public boolean isSpiritualResonanceCinematicActive(long currentTick) {
        return currentTick < spiritualResonanceCinematicUntilTick;
    }

    public long getSpiritualResonanceUntilTick() {
        return spiritualResonanceUntilTick;
    }

    public long getSpiritualResonanceCinematicUntilTick() {
        return spiritualResonanceCinematicUntilTick;
    }

    public String getChosenGuardian() {
        return chosenGuardian != null ? chosenGuardian : "";
    }

    public void setChosenGuardian(String guardian) {
        this.chosenGuardian = (guardian != null) ? guardian.toLowerCase(java.util.Locale.ROOT).trim() : "";
    }

    public boolean hasChosenGuardian() {
        return chosenGuardian != null && !chosenGuardian.isEmpty();
    }

    public java.util.UUID getSummonedGuardianUuid() {
        return summonedGuardianUuid;
    }

    public void setSummonedGuardianUuid(java.util.UUID uuid) {
        this.summonedGuardianUuid = uuid;
    }

    public int getGuardianVitalEnergy() {
        return guardianVitalEnergy;
    }

    public void setGuardianVitalEnergy(int vitalEnergy) {
        this.guardianVitalEnergy = Math.max(0, vitalEnergy);
    }

    public void addGuardianVitalEnergy(int amount) {
        if (amount > 0) {
            this.guardianVitalEnergy += amount;
        }
    }

    /**
     * Calcula la estadística de Aura (Poder del Alma):
     * Promedio equilibrado de todas las estadísticas activas del jugador.
     * Cuanto más fuerte es el jugador, más imponente y grande es su aura.
     */
    public float getAuraStat(Player player) {
        if (player == null) return 1.0f;
        int fort = getTotalStatPoints(player, StatType.FORTITUDE);
        int dex = getTotalStatPoints(player, StatType.DEXTERITY);
        int spirit = getTotalStatPoints(player, StatType.SPIRIT);
        int luck = getTotalStatPoints(player, StatType.LUCK);
        float rawAura = Math.max(1.0f, (fort + dex + spirit + luck) / 4.0f);
        return Math.max(1.0f, rawAura * powerOutputRatio);
    }

    public float getPowerOutputRatio() {
        return powerOutputRatio;
    }

    public void setPowerOutputRatio(float ratio) {
        this.powerOutputRatio = Math.max(0.01f, Math.min(1.0f, ratio));
    }

    public float getMoveSpeedRatio() {
        return moveSpeedRatio;
    }

    public void setMoveSpeedRatio(float ratio) {
        this.moveSpeedRatio = Math.max(0.10f, Math.min(1.0f, ratio));
    }

    public float getAttackSpeedRatio() {
        return attackSpeedRatio;
    }

    public void setAttackSpeedRatio(float ratio) {
        this.attackSpeedRatio = Math.max(0.10f, Math.min(1.0f, ratio));
    }

    public float getJumpHeightRatio() {
        return jumpHeightRatio;
    }

    public void setJumpHeightRatio(float ratio) {
        this.jumpHeightRatio = Math.max(0.00f, Math.min(1.0f, ratio));
    }

    public void setPowerControl(float power, float speed, float attack, float jump) {
        setPowerOutputRatio(power);
        setMoveSpeedRatio(speed);
        setAttackSpeedRatio(attack);
        setJumpHeightRatio(jump);
    }

    public Map<Integer, StatType> getChosenMilestones() {
        return Collections.unmodifiableMap(chosenMilestones);
    }

    public boolean hasChosenMilestone(int milestone) {
        return chosenMilestones.containsKey(milestone);
    }

    public void setMilestoneChoice(int milestone, StatType statType) {
        chosenMilestones.put(milestone, statType);
    }

    public Set<String> getUnlockedSkills() {
        return Collections.unmodifiableSet(unlockedSkills);
    }

    public boolean hasSkill(String skillId) {
        return unlockedSkills.contains(skillId);
    }

    public boolean unlockSkill(String skillId) {
        return unlockedSkills.add(skillId);
    }

    public int getLastNotifiedMilestone() {
        return lastNotifiedMilestone;
    }

    public void setLastNotifiedMilestone(int milestone) {
        this.lastNotifiedMilestone = Math.max(this.lastNotifiedMilestone, milestone);
    }

    public AdventurerRank getRank() {
        return AdventurerRank.fromIndex(rankIndex);
    }

    public void setRank(AdventurerRank rank) {
        this.rankIndex = rank.getIndex();
    }

    public boolean isGuildRegistered() {
        return isGuildRegistered;
    }

    public void setGuildRegistered(boolean registered) {
        this.isGuildRegistered = registered;
    }

    public boolean hasReceivedAdventurerCard() {
        return hasReceivedAdventurerCard;
    }

    public void setReceivedAdventurerCard(boolean received) {
        this.hasReceivedAdventurerCard = received;
    }

    public int getPermanentPoints(StatType statType) {
        return switch (statType) {
            case FORTITUDE -> permanentFortitude;
            case DEXTERITY -> permanentDexterity;
            case SPIRIT -> permanentSpirit;
            case LUCK -> permanentLuck;
        };
    }

    public void addPermanentPoints(StatType statType, int points) {
        switch (statType) {
            case FORTITUDE -> permanentFortitude += points;
            case DEXTERITY -> permanentDexterity += points;
            case SPIRIT -> permanentSpirit += points;
            case LUCK -> permanentLuck += points;
        }
    }

    public void setPermanentPoints(StatType statType, int points) {
        switch (statType) {
            case FORTITUDE -> permanentFortitude = Math.max(0, points);
            case DEXTERITY -> permanentDexterity = Math.max(0, points);
            case SPIRIT -> permanentSpirit = Math.max(0, points);
            case LUCK -> permanentLuck = Math.max(0, points);
        }
    }

    public int getBonusPendingPoints() {
        return bonusPendingPoints;
    }

    public void addBonusPendingPoints(int points) {
        this.bonusPendingPoints += points;
    }

    public void setBonusPendingPoints(int points) {
        this.bonusPendingPoints = Math.max(0, points);
    }

    public boolean hasRolledBirthStats() {
        return birthStatsRolled;
    }

    public void setBirthStatsRolled(boolean rolled) {
        this.birthStatsRolled = rolled;
    }

    /**
     * Rolls innate base stats between 1 and 10 when the player first spawns in the world.
     * Standard player baseline is 5 in all stats.
     */
    public boolean rollBirthStatsIfNeeded(net.minecraft.util.RandomSource random) {
        if (this.birthStatsRolled) {
            return false;
        }
        this.birthStatsRolled = true;
        this.permanentFortitude = 1 + random.nextInt(10);
        this.permanentDexterity = 1 + random.nextInt(10);
        this.permanentSpirit = 1 + random.nextInt(10);
        this.permanentLuck = 1 + random.nextInt(10);
        return true;
    }

    public boolean hasDivineBlessingUnlocked() {
        return divineBlessingUnlocked;
    }

    public void setDivineBlessingUnlocked(boolean unlocked) {
        this.divineBlessingUnlocked = unlocked;
    }

    public String getPatronGodId() {
        return patronGodId != null ? patronGodId : "";
    }

    public void setPatronGodId(String godId) {
        this.patronGodId = godId != null ? godId : "";
    }

    public GreekGod getPatronGod() {
        return GreekGod.fromId(this.patronGodId);
    }

    public int getRpgLevel() {
        return Math.max(1, rpgLevel);
    }

    public void setRpgLevel(int level) {
        this.rpgLevel = Math.max(1, Math.min(125, level));
    }

    public int getRpgXp() {
        return Math.max(0, rpgXp);
    }

    public void setRpgXp(int xp) {
        this.rpgXp = Math.max(0, xp);
    }

    public int getXpForNextLevel(int level) {
        int lvl = Math.max(1, level);
        return 80 + ((lvl - 1) * 35) + ((lvl - 1) * (lvl - 1) * 3);
    }

    public int getXpForNextLevel() {
        return getXpForNextLevel(getRpgLevel());
    }

    public int getCurrentMilestoneCap() {
        return getRank().getRequiredAscensionLevel();
    }

    public boolean isAtMilestoneCap() {
        return !getRank().isMaxRank() && getRpgLevel() >= getCurrentMilestoneCap();
    }

    /**
     * Grants independent RPG XP if the player has unlocked their Divine Blessing in the Astral Plane.
     */
    public boolean addRpgXp(net.minecraft.server.level.ServerPlayer player, int baseAmount) {
        if (baseAmount <= 0 || !this.divineBlessingUnlocked) {
            return false;
        }

        int cap = getCurrentMilestoneCap();
        if (!getRank().isMaxRank() && this.rpgLevel >= cap) {
            this.rpgXp = 0;
            return false;
        }

        int spirit = (player != null) ? getTotalStatPoints(player, StatType.SPIRIT) : 5;
        double spiritMult = 1.0 + (Math.max(0, spirit - 5) * 0.02);
        if (this.divineBlessingUnlocked && getPatronGod() == GreekGod.ATHENA) {
            int tier = GreekGod.getBlessingTier(getRank());
            double athenaMult = tier == 1 ? 1.15 : (tier == 2 ? 1.20 : 1.25);
            spiritMult *= athenaMult;
        }
        int amount = Math.max(1, (int) Math.round(baseAmount * spiritMult));

        this.rpgXp += amount;
        boolean leveledUp = false;

        while (this.rpgXp >= getXpForNextLevel(this.rpgLevel)) {
            if (!getRank().isMaxRank() && this.rpgLevel >= cap) {
                this.rpgXp = 0;
                break;
            }
            this.rpgXp -= getXpForNextLevel(this.rpgLevel);
            this.rpgLevel++;
            leveledUp = true;

            if (!getRank().isMaxRank() && this.rpgLevel >= cap) {
                this.rpgXp = 0;
                break;
            }
        }

        if (player != null) {
            if (leveledUp) {
                int pending = getPendingPointsCount();
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6✦ §l[¡NIVEL RPG SUBIDO!] §eHas alcanzado el §fNivel " + this.rpgLevel
                                + " §7(Puntos disponibles: §a" + pending + "§7 - Pulsa §b[K]§7)"));
                player.playNotifySound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.9f, 1.2f);

                if (isAtMilestoneCap()) {
                    GreekGod god = getPatronGod();
                    String godName = (god != null) ? god.getDisplayName() : "tu Dios";
                    player.sendSystemMessage(net.minecraft.network.chat.Component.empty());
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§d⚡ « HITO DIVINO ALCANZADO (NIVEL " + cap + ") »").withStyle(net.minecraft.ChatFormatting.BOLD));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eHas alcanzado el límite mortal de tu rango actual."));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6Duerme en una cama §epara comparecer ante §b" + godName + " §een el §dEspacio Astral §ey recibir tu §6Ascensión de Rango§e."));
                    player.sendSystemMessage(net.minecraft.network.chat.Component.empty());
                    player.playNotifySound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
                }

                com.pablo.rpglevels.events.ModAttributesHandler.updatePlayerAttributes(player);
            }
            com.pablo.rpglevels.network.ModNetworking.syncToPlayer(player);
            com.pablo.rpglevels.network.ModNetworking.sendXpGain(
                    player,
                    amount,
                    this.rpgXp,
                    getXpForNextLevel(this.rpgLevel),
                    this.rpgLevel,
                    isAtMilestoneCap()
            );
        }
        return leveledUp;
    }

    public static final int POINTS_PER_MILESTONE = 3;

    public int getTotalEarnedPoints() {
        if (!this.divineBlessingUnlocked) {
            return this.bonusPendingPoints;
        }
        return Math.max(0, this.rpgLevel - 1) + this.bonusPendingPoints;
    }

    public int getTotalEarnedPoints(int ignoredLevel) {
        return getTotalEarnedPoints();
    }

    public int getAllocatedPointsCount() {
        return currentLifeFortitude + currentLifeDexterity + currentLifeSpirit + currentLifeLuck;
    }

    public int getPendingPointsCount() {
        return Math.max(0, getTotalEarnedPoints() - getAllocatedPointsCount());
    }

    public int getPendingPointsCount(int ignoredLevel) {
        return getPendingPointsCount();
    }

    public boolean allocatePoint(StatType statType) {
        if (getPendingPointsCount() < 1) {
            return false;
        }
        switch (statType) {
            case FORTITUDE -> currentLifeFortitude++;
            case DEXTERITY -> currentLifeDexterity++;
            case SPIRIT -> currentLifeSpirit++;
            case LUCK -> currentLifeLuck++;
        }
        return true;
    }

    public boolean allocatePoint(StatType statType, int ignoredLevel) {
        return allocatePoint(statType);
    }

    public int getLifePoints(StatType statType) {
        return switch (statType) {
            case FORTITUDE -> currentLifeFortitude;
            case DEXTERITY -> currentLifeDexterity;
            case SPIRIT -> currentLifeSpirit;
            case LUCK -> currentLifeLuck;
        };
    }

    public void setLifePoints(StatType statType, int points) {
        switch (statType) {
            case FORTITUDE -> currentLifeFortitude = Math.max(0, points);
            case DEXTERITY -> currentLifeDexterity = Math.max(0, points);
            case SPIRIT -> currentLifeSpirit = Math.max(0, points);
            case LUCK -> currentLifeLuck = Math.max(0, points);
        }
    }

    public void resetAllocatedPoints() {
        this.currentLifeFortitude = 0;
        this.currentLifeDexterity = 0;
        this.currentLifeSpirit = 0;
        this.currentLifeLuck = 0;
    }

    public int getActiveMilestonePoints(StatType statType, int ignoredLevel) {
        return getLifePoints(statType);
    }

    public int getCuriosBonus(Player player, StatType statType) {
        if (player == null) return 0;
        int bonus = 0;
        var curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isPresent()) {
            var inv = curiosOpt.get();
            if (statType == StatType.FORTITUDE) {
                var ringOpt = inv.findFirstCurio(s -> s.getItem() instanceof WarriorRingItem);
                if (ringOpt.isPresent()) {
                    bonus += WarriorRingItem.getFortitudeBonus(ringOpt.get().stack());
                }
            } else if (statType == StatType.SPIRIT) {
                var amuletOpt = inv.findFirstCurio(s -> s.getItem() instanceof ExperienceAmuletItem);
                if (amuletOpt.isPresent()) {
                    bonus += ExperienceAmuletItem.getSpiritBonus(amuletOpt.get().stack());
                }
            } else if (statType == StatType.DEXTERITY || statType == StatType.LUCK) {
                var ancientOpt = inv.findFirstCurio(s -> s.getItem() instanceof AncientRingItem);
                if (ancientOpt.isPresent()) {
                    bonus += AncientRingItem.getDexterityAndLuckBonus(ancientOpt.get().stack());
                }
            }
        }
        return bonus;
    }

    public int getTotalStatPoints(StatType statType, int ignoredLevel) {
        return getPermanentPoints(statType) + getLifePoints(statType);
    }

    public int getTotalStatPoints(Player player, StatType statType) {
        int total = getPermanentPoints(statType) + getLifePoints(statType) + getCuriosBonus(player, statType);
        if (statType == StatType.SPIRIT && player != null && player.level() != null) {
            total += getActiveSpiritSurgeBonus(player.level().getGameTime());
        }
        if (player != null && isHoldingReleasedSpiritualSword(player)) {
            // Power-up masivo de Primera Liberación: +50% a todas las estadísticas RPG (+5 puntos base)
            total = (int) Math.round(total * 1.5) + 5;
        }
        if (player != null && player.level() != null && isSpiritualResonanceActive(player.level().getGameTime())) {
            // Resonancia Espiritual (Modo Supremo a 200 de Afinidad): ¡x2 (DUPLICACIÓN) a todas las estadísticas RPG!
            total = total * 2;
        }
        return total;
    }

    public static boolean isHoldingReleasedSpiritualSword(Player player) {
        if (player == null) return false;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof com.pablo.rpglevels.items.SpiritualSwordItem && com.pablo.rpglevels.items.SpiritualSwordItem.isFirstRelease(main)) {
            return true;
        }
        ItemStack off = player.getOffhandItem();
        return off.getItem() instanceof com.pablo.rpglevels.items.SpiritualSwordItem && com.pablo.rpglevels.items.SpiritualSwordItem.isFirstRelease(off);
    }

    public int getNextPendingMilestone(int ignoredLevel) {
        return (getPendingPointsCount() > 0) ? Math.min(90, getCurrentMilestoneCap()) : -1;
    }

    /**
     * Physical Attack Damage Bonus determined by the relationship between Fortitude and Dexterity.
     * Standard baseline at Fortitude = 5 and Dexterity = 5 gives +0.0 (so player base attack damage is 1.0).
     */
    public double getPhysicalRelationDamageBonus(Player player) {
        int rawFort = (player != null) ? getTotalStatPoints(player, StatType.FORTITUDE) : 5;
        int rawDex = (player != null) ? getTotalStatPoints(player, StatType.DEXTERITY) : 5;
        double fDelta = (rawFort * powerOutputRatio) - 5.0;
        double dDelta = (rawDex * powerOutputRatio) - 5.0;
        double baseRelation = (fDelta * 0.15) + (dDelta * 0.15);
        double synergy = (fDelta > 0.0 && dDelta > 0.0) ? Math.sqrt(fDelta * dDelta) * 0.10 : 0.0;
        return Math.max(-0.5, baseRelation + synergy);
    }

    /**
     * Bonus damage granted to Spiritual Swords from the player's Spirit attribute.
     */
    public double getSpiritualSwordSpiritBonus(Player player) {
        int rawSpirit = (player != null) ? getTotalStatPoints(player, StatType.SPIRIT) : 5;
        double effSpirit = rawSpirit * powerOutputRatio;
        return Math.max(0.0, effSpirit * 0.25);
    }

    /**
     * Calculates player's RPG Critical Strike Chance percentage.
     * Standard baseline at Luck = 5 and Dexterity = 5 is 5.0%.
     */
    public double getCritChance(Player player) {
        int luck = (player != null) ? getTotalStatPoints(player, StatType.LUCK) : 5;
        int dex = (player != null) ? getTotalStatPoints(player, StatType.DEXTERITY) : 5;
        double chance = 5.0 + ((luck - 5) * 2.0) + (Math.max(0, dex - 5) * 0.5);
        if (this.divineBlessingUnlocked && getPatronGod() == GreekGod.ARTEMIS) {
            int tier = GreekGod.getBlessingTier(getRank());
            chance += (tier == 1 ? 8.0 : (tier == 2 ? 12.0 : 15.0));
        }
        return Math.max(1.0, chance);
    }

    /**
     * Calculates RPG Critical Strike Damage Multiplier.
     * Standard Crit (<= 100% chance): 1.5x (150% damage)
     * Super-Crit (> 100% chance): 1.5x + ((critChance - 100.0) * 0.015)
     */
    public double getCritDamageMultiplier(double critChance) {
        if (critChance <= 100.0) {
            return 1.5;
        }
        return 1.5 + ((critChance - 100.0) * 0.015);
    }

    /**
     * Performs Divine Ascension in the Astral Plane:
     * 1. Preserves all innate and allocated stats (never resets player progress).
     * 2. Grants +2 bonus attribute points as a divine gift.
     * 3. Increases Adventurer Rank and unlocks the next level milestone cap.
     */
    public boolean performAscension() {
        AdventurerRank currentRank = getRank();
        if (currentRank.isMaxRank()) {
            return false;
        }

        this.bonusPendingPoints += 2;
        setRank(currentRank.getNextRank());
        return true;
    }

    public void resetProgress() {
        this.chosenMilestones.clear();
        this.unlockedSkills.clear();
        this.lastNotifiedMilestone = 0;
        this.rankIndex = 0;
        this.isGuildRegistered = false;
        this.hasReceivedAdventurerCard = false;
        this.currentLifeFortitude = 0;
        this.currentLifeDexterity = 0;
        this.currentLifeSpirit = 0;
        this.currentLifeLuck = 0;
        this.permanentFortitude = 5;
        this.permanentDexterity = 5;
        this.permanentSpirit = 5;
        this.permanentLuck = 5;
        this.birthStatsRolled = false;
        this.divineBlessingUnlocked = false;
        this.patronGodId = "";
        this.rpgLevel = 1;
        this.rpgXp = 0;
        this.channelMagicUses = 0;
        this.spiritSurgeUntilTick = 0;
        this.spiritSurgeBonus = 0;
        this.bonusPendingPoints = 0;
        this.currentMana = -1.0f;
        this.hasKilledFirstMonster = false;
        this.warTrialCompleted = false;
        this.physicalSkillPoints = 0;
    }

    public void copyFrom(PlayerRpgData other) {
        this.chosenMilestones.clear();
        this.chosenMilestones.putAll(other.chosenMilestones);
        this.unlockedSkills.clear();
        this.unlockedSkills.addAll(other.unlockedSkills);
        this.lastNotifiedMilestone = other.lastNotifiedMilestone;
        this.rankIndex = other.rankIndex;
        this.isGuildRegistered = other.isGuildRegistered;
        this.hasReceivedAdventurerCard = other.hasReceivedAdventurerCard;
        this.currentLifeFortitude = other.currentLifeFortitude;
        this.currentLifeDexterity = other.currentLifeDexterity;
        this.currentLifeSpirit = other.currentLifeSpirit;
        this.currentLifeLuck = other.currentLifeLuck;
        this.permanentFortitude = other.permanentFortitude;
        this.permanentDexterity = other.permanentDexterity;
        this.permanentSpirit = other.permanentSpirit;
        this.permanentLuck = other.permanentLuck;
        this.birthStatsRolled = other.birthStatsRolled;
        this.divineBlessingUnlocked = other.divineBlessingUnlocked;
        this.patronGodId = other.patronGodId;
        this.rpgLevel = other.rpgLevel;
        this.rpgXp = other.rpgXp;
        this.channelMagicUses = other.channelMagicUses;
        this.currentMana = other.currentMana;
        this.bonusPendingPoints = other.bonusPendingPoints;
        this.hasKilledFirstMonster = other.hasKilledFirstMonster;
        this.warTrialCompleted = other.warTrialCompleted;
        this.physicalSkillPoints = other.physicalSkillPoints;
        this.jumpCount = other.jumpCount;
        this.aliceQuestActive = other.aliceQuestActive;
        this.aliceDMonstersKilled = other.aliceDMonstersKilled;
        this.aliceQuestCompleted = other.aliceQuestCompleted;
        this.receivedSpiritualSword = other.receivedSpiritualSword;
        this.firstReleaseUnlocked = other.firstReleaseUnlocked;
        this.soulBladeRolled = other.soulBladeRolled;
        this.soulBladeStyle = other.soulBladeStyle;
        this.soulBladeElement = other.soulBladeElement;
        this.soulBladeRelease = other.soulBladeRelease;
        this.soulBladeResonance = other.soulBladeResonance;
        this.soulBladeVariant = other.soulBladeVariant;
        this.spiritualTierLevel = other.spiritualTierLevel;
        this.spiritualEngineer = other.spiritualEngineer;
        this.banditsKilledToday = other.banditsKilledToday;
        this.banditsKilledYesterday = other.banditsKilledYesterday;
        this.lastAmbushCheckDay = other.lastAmbushCheckDay;
        this.pendingAmbushToday = other.pendingAmbushToday;
        this.lastDashTick = other.lastDashTick;
        this.spiritualResonanceUntilTick = other.spiritualResonanceUntilTick;
        this.spiritualResonanceCinematicUntilTick = other.spiritualResonanceCinematicUntilTick;
        this.chosenGuardian = other.chosenGuardian;
        this.summonedGuardianUuid = other.summonedGuardianUuid;
        this.guardianVitalEnergy = other.guardianVitalEnergy;
        this.currentLocationName = other.currentLocationName;
        this.skillCooldownUntilTick.clear();
        this.skillCooldownUntilTick.putAll(other.skillCooldownUntilTick);
        this.skillCooldownTotalTicks.clear();
        this.skillCooldownTotalTicks.putAll(other.skillCooldownTotalTicks);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("JumpCount", jumpCount);
        tag.putLong("LastDashTick", lastDashTick);
        tag.putInt("LastNotifiedMilestone", lastNotifiedMilestone);
        tag.putInt("RankIndex", rankIndex);
        tag.putBoolean("IsGuildRegistered", isGuildRegistered);
        tag.putBoolean("HasReceivedAdventurerCard", hasReceivedAdventurerCard);
        tag.putInt("CurrentLifeFortitude", currentLifeFortitude);
        tag.putInt("CurrentLifeDexterity", currentLifeDexterity);
        tag.putInt("CurrentLifeSpirit", currentLifeSpirit);
        tag.putInt("CurrentLifeLuck", currentLifeLuck);
        tag.putInt("PermFortitude", permanentFortitude);
        tag.putInt("PermDexterity", permanentDexterity);
        tag.putInt("PermSpirit", permanentSpirit);
        tag.putInt("PermLuck", permanentLuck);
        tag.putBoolean("BirthStatsRolled", birthStatsRolled);
        tag.putBoolean("DivineBlessingUnlocked", divineBlessingUnlocked);
        tag.putString("PatronGodId", getPatronGodId());
        tag.putInt("RpgLevel", rpgLevel);
        tag.putInt("RpgXp", rpgXp);
        tag.putInt("ChannelMagicUses", channelMagicUses);
        tag.putLong("AuraVisibleUntilTick", auraVisibleUntilTick);
        tag.putLong("AuraSenseUntilTick", auraSenseUntilTick);
        tag.putLong("OppressiveAuraUntilTick", oppressiveAuraUntilTick);
        tag.putLong("SpiritualResonanceUntilTick", spiritualResonanceUntilTick);
        tag.putLong("SpiritualResonanceCinematicUntilTick", spiritualResonanceCinematicUntilTick);
        tag.putString("ChosenGuardian", getChosenGuardian());
        if (summonedGuardianUuid != null) {
            tag.putUUID("SummonedGuardianUuid", summonedGuardianUuid);
        }
        tag.putInt("GuardianVitalEnergy", this.guardianVitalEnergy);
        tag.putFloat("CurrentMana", currentMana);
        tag.putFloat("PowerOutputRatio", powerOutputRatio);
        tag.putFloat("MoveSpeedRatio", moveSpeedRatio);
        tag.putFloat("AttackSpeedRatio", attackSpeedRatio);
        tag.putFloat("JumpHeightRatio", jumpHeightRatio);
        tag.putInt("ReputationScore", reputationScore);
        tag.putInt("BonusPendingPoints", bonusPendingPoints);
        tag.putBoolean("HasKilledFirstMonster", hasKilledFirstMonster);
        tag.putBoolean("WarTrialCompleted", warTrialCompleted);
        tag.putInt("PhysicalSkillPoints", physicalSkillPoints);
        tag.putBoolean("AliceQuestActive", aliceQuestActive);
        tag.putInt("AliceDMonstersKilled", aliceDMonstersKilled);
        tag.putBoolean("AliceQuestCompleted", aliceQuestCompleted);
        tag.putBoolean("ReceivedSpiritualSword", receivedSpiritualSword);
        tag.putBoolean("FirstReleaseUnlocked", firstReleaseUnlocked);
        tag.putBoolean("SoulBladeRolled", soulBladeRolled);
        tag.putString("SoulBladeStyle", soulBladeStyle);
        tag.putString("SoulBladeElement", soulBladeElement);
        tag.putString("SoulBladeRelease", soulBladeRelease);
        tag.putString("SoulBladeResonance", soulBladeResonance);
        tag.putInt("SoulBladeVariant", soulBladeVariant);
        tag.putInt("SpiritualTierLevel", spiritualTierLevel);
        tag.putBoolean("SpiritualEngineer", spiritualEngineer);
        tag.putInt("BanditsKilledToday", banditsKilledToday);
        tag.putInt("BanditsKilledYesterday", banditsKilledYesterday);
        tag.putLong("LastAmbushCheckDay", lastAmbushCheckDay);
        tag.putBoolean("PendingAmbushToday", pendingAmbushToday);
        tag.putBoolean("CelestialMantleActive", this.celestialMantleActive);
        tag.putLong("CelestialMantleUntilTick", this.celestialMantleUntilTick);
        // Compatibilidad hacia atrás
        tag.putBoolean("DivineSmokeActive", this.celestialMantleActive);
        tag.putLong("DivineSmokeUntilTick", this.celestialMantleUntilTick);

        ListTag list = new ListTag();
        for (Map.Entry<Integer, StatType> entry : chosenMilestones.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putInt("Milestone", entry.getKey());
            entryTag.putString("Stat", entry.getValue().getId());
            list.add(entryTag);
        }
        tag.put("Milestones", list);

        ListTag skillList = new ListTag();
        for (String skill : unlockedSkills) {
            CompoundTag sTag = new CompoundTag();
            sTag.putString("Id", skill);
            skillList.add(sTag);
        }
        tag.put("Skills", skillList);

        ListTag cooldownList = new ListTag();
        for (Map.Entry<String, Long> entry : skillCooldownUntilTick.entrySet()) {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("Id", entry.getKey());
            cTag.putLong("UntilTick", entry.getValue());
            cTag.putInt("TotalTicks", skillCooldownTotalTicks.getOrDefault(entry.getKey(), 100));
            cooldownList.add(cTag);
        }
        tag.put("SkillCooldowns", cooldownList);
        tag.putString("CurrentLocationName", getCurrentLocationName());

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        chosenMilestones.clear();
        unlockedSkills.clear();
        skillCooldownUntilTick.clear();
        skillCooldownTotalTicks.clear();

        if (tag.contains("JumpCount", Tag.TAG_INT)) {
            this.jumpCount = tag.getInt("JumpCount");
        }
        if (tag.contains("LastDashTick", Tag.TAG_LONG)) {
            this.lastDashTick = tag.getLong("LastDashTick");
        }

        if (tag.contains("LastNotifiedMilestone", Tag.TAG_INT)) {
            this.lastNotifiedMilestone = tag.getInt("LastNotifiedMilestone");
        }
        if (tag.contains("RankIndex", Tag.TAG_INT)) {
            this.rankIndex = tag.getInt("RankIndex");
        }
        if (tag.contains("IsGuildRegistered", Tag.TAG_BYTE)) {
            this.isGuildRegistered = tag.getBoolean("IsGuildRegistered");
        }
        if (tag.contains("HasReceivedAdventurerCard", Tag.TAG_BYTE)) {
            this.hasReceivedAdventurerCard = tag.getBoolean("HasReceivedAdventurerCard");
        }
        if (tag.contains("ChannelMagicUses", Tag.TAG_INT)) {
            this.channelMagicUses = tag.getInt("ChannelMagicUses");
        }
        if (tag.contains("AuraVisibleUntilTick", Tag.TAG_LONG)) {
            this.auraVisibleUntilTick = tag.getLong("AuraVisibleUntilTick");
        }
        if (tag.contains("AuraSenseUntilTick", Tag.TAG_LONG)) {
            this.auraSenseUntilTick = tag.getLong("AuraSenseUntilTick");
        }
        if (tag.contains("OppressiveAuraUntilTick", Tag.TAG_LONG)) {
            this.oppressiveAuraUntilTick = tag.getLong("OppressiveAuraUntilTick");
        }
        if (tag.contains("SpiritualResonanceUntilTick", Tag.TAG_LONG)) {
            this.spiritualResonanceUntilTick = tag.getLong("SpiritualResonanceUntilTick");
        }
        if (tag.contains("SpiritualResonanceCinematicUntilTick", Tag.TAG_LONG)) {
            this.spiritualResonanceCinematicUntilTick = tag.getLong("SpiritualResonanceCinematicUntilTick");
        }
        if (tag.contains("ChosenGuardian", Tag.TAG_STRING)) {
            this.chosenGuardian = tag.getString("ChosenGuardian");
        }
        if (tag.contains("SummonedGuardianUuid")) {
            this.summonedGuardianUuid = tag.getUUID("SummonedGuardianUuid");
        }
        if (tag.contains("GuardianVitalEnergy")) {
            this.guardianVitalEnergy = Math.max(0, tag.getInt("GuardianVitalEnergy"));
        }
        if (tag.contains("CurrentMana", Tag.TAG_FLOAT)) {
            this.currentMana = tag.getFloat("CurrentMana");
        }
        if (tag.contains("PowerOutputRatio", Tag.TAG_FLOAT)) {
            this.powerOutputRatio = tag.getFloat("PowerOutputRatio");
        }
        if (tag.contains("MoveSpeedRatio", Tag.TAG_FLOAT)) {
            this.moveSpeedRatio = tag.getFloat("MoveSpeedRatio");
        }
        if (tag.contains("AttackSpeedRatio", Tag.TAG_FLOAT)) {
            this.attackSpeedRatio = tag.getFloat("AttackSpeedRatio");
        }
        if (tag.contains("JumpHeightRatio", Tag.TAG_FLOAT)) {
            this.jumpHeightRatio = tag.getFloat("JumpHeightRatio");
        }
        if (tag.contains("ReputationScore", Tag.TAG_INT)) {
            this.reputationScore = tag.getInt("ReputationScore");
        }
        if (tag.contains("BonusPendingPoints", Tag.TAG_INT)) {
            this.bonusPendingPoints = tag.getInt("BonusPendingPoints");
        }
        if (tag.contains("HasKilledFirstMonster", Tag.TAG_BYTE)) {
            this.hasKilledFirstMonster = tag.getBoolean("HasKilledFirstMonster");
        }
        if (tag.contains("WarTrialCompleted", Tag.TAG_BYTE)) {
            this.warTrialCompleted = tag.getBoolean("WarTrialCompleted");
        }
        if (tag.contains("PhysicalSkillPoints", Tag.TAG_INT)) {
            this.physicalSkillPoints = tag.getInt("PhysicalSkillPoints");
        }
        if (tag.contains("AliceQuestActive", Tag.TAG_BYTE)) {
            this.aliceQuestActive = tag.getBoolean("AliceQuestActive");
        }
        if (tag.contains("AliceDMonstersKilled", Tag.TAG_INT)) {
            this.aliceDMonstersKilled = tag.getInt("AliceDMonstersKilled");
        }
        if (tag.contains("AliceQuestCompleted", Tag.TAG_BYTE)) {
            this.aliceQuestCompleted = tag.getBoolean("AliceQuestCompleted");
        }
        if (tag.contains("ReceivedSpiritualSword", Tag.TAG_BYTE)) {
            this.receivedSpiritualSword = tag.getBoolean("ReceivedSpiritualSword");
        }
        if (tag.contains("FirstReleaseUnlocked", Tag.TAG_BYTE)) {
            this.firstReleaseUnlocked = tag.getBoolean("FirstReleaseUnlocked");
        }
        if (tag.contains("SoulBladeRolled", Tag.TAG_BYTE)) {
            this.soulBladeRolled = tag.getBoolean("SoulBladeRolled");
        }
        if (tag.contains("SoulBladeStyle", Tag.TAG_STRING)) {
            this.soulBladeStyle = tag.getString("SoulBladeStyle");
        }
        if (tag.contains("SoulBladeElement", Tag.TAG_STRING)) {
            this.soulBladeElement = tag.getString("SoulBladeElement");
        }
        if (tag.contains("SoulBladeRelease", Tag.TAG_STRING)) {
            this.soulBladeRelease = tag.getString("SoulBladeRelease");
        }
        if (tag.contains("SoulBladeResonance", Tag.TAG_STRING)) {
            this.soulBladeResonance = tag.getString("SoulBladeResonance");
        }
        if (tag.contains("SoulBladeVariant", Tag.TAG_INT)) {
            this.soulBladeVariant = tag.getInt("SoulBladeVariant");
        }
        if (tag.contains("SpiritualTierLevel", Tag.TAG_INT)) {
            this.spiritualTierLevel = tag.getInt("SpiritualTierLevel");
        }
        if (tag.contains("SpiritualEngineer", Tag.TAG_BYTE)) {
            this.spiritualEngineer = tag.getBoolean("SpiritualEngineer");
        }
        if (tag.contains("BanditsKilledToday", Tag.TAG_INT)) {
            this.banditsKilledToday = tag.getInt("BanditsKilledToday");
        }
        if (tag.contains("BanditsKilledYesterday", Tag.TAG_INT)) {
            this.banditsKilledYesterday = tag.getInt("BanditsKilledYesterday");
        }
        if (tag.contains("LastAmbushCheckDay", Tag.TAG_LONG)) {
            this.lastAmbushCheckDay = tag.getLong("LastAmbushCheckDay");
        }
        if (tag.contains("PendingAmbushToday", Tag.TAG_BYTE)) {
            this.pendingAmbushToday = tag.getBoolean("PendingAmbushToday");
        }
        if (tag.contains("CelestialMantleActive", Tag.TAG_BYTE)) {
            this.celestialMantleActive = tag.getBoolean("CelestialMantleActive");
        } else if (tag.contains("DivineSmokeActive", Tag.TAG_BYTE)) {
            this.celestialMantleActive = tag.getBoolean("DivineSmokeActive");
        }
        if (tag.contains("CelestialMantleUntilTick", Tag.TAG_LONG)) {
            this.celestialMantleUntilTick = tag.getLong("CelestialMantleUntilTick");
        } else if (tag.contains("DivineSmokeUntilTick", Tag.TAG_LONG)) {
            this.celestialMantleUntilTick = tag.getLong("DivineSmokeUntilTick");
        }
        if (tag.contains("CurrentLifeFortitude", Tag.TAG_INT)) {
            this.currentLifeFortitude = tag.getInt("CurrentLifeFortitude");
            this.currentLifeDexterity = tag.getInt("CurrentLifeDexterity");
            this.currentLifeSpirit = tag.getInt("CurrentLifeSpirit");
            if (tag.contains("CurrentLifeLuck", Tag.TAG_INT)) {
                this.currentLifeLuck = tag.getInt("CurrentLifeLuck");
            }
        }
        if (tag.contains("PermFortitude", Tag.TAG_INT)) {
            this.permanentFortitude = tag.getInt("PermFortitude");
        }
        if (tag.contains("PermDexterity", Tag.TAG_INT)) {
            this.permanentDexterity = tag.getInt("PermDexterity");
        }
        if (tag.contains("PermSpirit", Tag.TAG_INT)) {
            this.permanentSpirit = tag.getInt("PermSpirit");
        }
        if (tag.contains("PermLuck", Tag.TAG_INT)) {
            this.permanentLuck = tag.getInt("PermLuck");
        }
        if (tag.contains("BirthStatsRolled", Tag.TAG_BYTE)) {
            this.birthStatsRolled = tag.getBoolean("BirthStatsRolled");
        }
        if (tag.contains("DivineBlessingUnlocked", Tag.TAG_BYTE)) {
            this.divineBlessingUnlocked = tag.getBoolean("DivineBlessingUnlocked");
        }
        if (tag.contains("PatronGodId", Tag.TAG_STRING)) {
            this.patronGodId = tag.getString("PatronGodId");
        }
        if (tag.contains("RpgLevel", Tag.TAG_INT)) {
            this.rpgLevel = Math.max(1, tag.getInt("RpgLevel"));
        }
        if (tag.contains("RpgXp", Tag.TAG_INT)) {
            this.rpgXp = Math.max(0, tag.getInt("RpgXp"));
        }

        if (tag.contains("Milestones", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Milestones", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entryTag = list.getCompound(i);
                int milestone = entryTag.getInt("Milestone");
                String statId = entryTag.getString("Stat");
                StatType statType = StatType.fromString(statId);
                if (statType != null && milestone > 0) {
                    chosenMilestones.put(milestone, statType);
                }
            }
            if (!tag.contains("CurrentLifeFortitude") && !chosenMilestones.isEmpty()) {
                for (StatType type : chosenMilestones.values()) {
                    switch (type) {
                        case FORTITUDE -> this.currentLifeFortitude += POINTS_PER_MILESTONE;
                        case DEXTERITY -> this.currentLifeDexterity += POINTS_PER_MILESTONE;
                        case SPIRIT -> this.currentLifeSpirit += POINTS_PER_MILESTONE;
                        case LUCK -> this.currentLifeLuck += POINTS_PER_MILESTONE;
                    }
                }
            }
        }

        if (tag.contains("Skills", Tag.TAG_LIST)) {
            ListTag skillList = tag.getList("Skills", Tag.TAG_COMPOUND);
            for (int i = 0; i < skillList.size(); i++) {
                CompoundTag sTag = skillList.getCompound(i);
                String id = sTag.getString("Id");
                if (!id.isEmpty()) {
                    unlockedSkills.add(id);
                }
            }
        }

        if (tag.contains("SkillCooldowns", Tag.TAG_LIST)) {
            ListTag cdList = tag.getList("SkillCooldowns", Tag.TAG_COMPOUND);
            for (int i = 0; i < cdList.size(); i++) {
                CompoundTag cTag = cdList.getCompound(i);
                String id = cTag.getString("Id");
                if (!id.isEmpty()) {
                    skillCooldownUntilTick.put(id, cTag.getLong("UntilTick"));
                    skillCooldownTotalTicks.put(id, Math.max(1, cTag.getInt("TotalTicks")));
                }
            }
        }

        if (tag.contains("CurrentLocationName", Tag.TAG_STRING)) {
            this.currentLocationName = tag.getString("CurrentLocationName");
        }
    }

    public String getCurrentLocationName() {
        return (this.currentLocationName != null) ? this.currentLocationName : "";
    }

    public void setCurrentLocationName(String location) {
        this.currentLocationName = (location != null) ? location : "";
    }
}
