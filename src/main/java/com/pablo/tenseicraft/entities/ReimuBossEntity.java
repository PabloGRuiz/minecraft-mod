package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.ThreatLevel;
import com.pablo.tenseicraft.magic.SpiritualTechnique;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.world.dimension.ModDimensions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Jefe Opcional: Reimu Hakurei (Miko del Santuario Hakurei - El Gensokyo).
 *
 * Características:
 * - Rango Calamidad fijo, 5000 HP fijos (sin escalado x10).
 * - Fase previa cinemática (180 ticks / 9s) donde confronta al jugador creyendo
 *   que es un invasor de otra dimensión antes de iniciar el combate.
 * - Si no quedan jugadores vivos en El Gensokyo, despawnea automáticamente junto a sus orbes.
 * - Sin daño por contacto ni ataques melee; dispara patrones Danmaku letales al instante.
 * - Loop: esquivar -> golpear -> aturdida 5s (100 ticks) -> onda de choque SIN daño que aleja al jugador.
 * - Al derrotarla, se convierte en {@link ReimuNpcEntity} y enseña Técnica Espiritual Nro. 890: Ha-ku-rei.
 */
public class ReimuBossEntity extends Monster {

    private static final EntityDataAccessor<Integer> DATA_STUN_TICKS =
            SynchedEntityData.defineId(ReimuBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(ReimuBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_INTRO_TICKS =
            SynchedEntityData.defineId(ReimuBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_CINEMATIC_MODE =
            SynchedEntityData.defineId(ReimuBossEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("§d§l[Santuario Hakurei] ").append(ThreatLevel.CALAMITY.getBadge()).append(" §fReimu Hakurei"),
            BossEvent.BossBarColor.PINK,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(false);

    // Ancla posicional para que flote limpiamente en su zona sin ser empujada fuera del claro
    private boolean hasAnchor = false;
    private double anchorX = 0.0;
    private double anchorY = 64.0;
    private double anchorZ = 0.0;

    // Introducción, Transición Cinemática, Stun, Push y Patrones
    private static final float STAGE_2_THRESHOLD = 0.60F; // 3000 / 5000 HP
    private static final float STAGE_3_THRESHOLD = 0.15F; // 750 / 5000 HP

    private int introTicks = 180;
    private int cinematicMode = 1; // 0 = ninguna, 1 = intro, 2 = transición etapa 2, 3 = transición etapa 3
    private int stunTicks = 0;
    private int graceTicks = 15;
    private int patternTick = 0;
    private int subPatternIndex = 0;
    private float spiralAngle = 0.0F;
    private int lastAnnouncedStage = 1;
    private boolean transformedToNpc = false;
    private boolean hpCapApplied = false;
    private boolean readyForPhaseGate = false;
    private boolean allowBypassPhaseGate = false;

    public ReimuBossEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.xpReward = 0;
        this.setPersistenceRequired();
        this.setHealth(this.getMaxHealth());
        this.readyForPhaseGate = true;
    }

    @Override
    public void setHealth(float health) {
        if (this.readyForPhaseGate
                && !this.allowBypassPhaseGate
                && this.level() instanceof ServerLevel sl
                && !this.isDeadOrDying()
                && health < this.getHealth()) {
            if (this.introTicks > 0) {
                return;
            }
            float maxHp = this.getMaxHealth();
            if (maxHp > 0.0F) {
                if (this.lastAnnouncedStage < 2) {
                    float stage2Floor = maxHp * STAGE_2_THRESHOLD;
                    if (health <= stage2Floor) {
                        super.setHealth(stage2Floor);
                        this.entityData.set(DATA_STAGE, 2);
                        this.bossEvent.setProgress(STAGE_2_THRESHOLD);
                        onStageTransition(sl, 2);
                        return;
                    }
                } else if (this.lastAnnouncedStage < 3) {
                    float stage3Floor = maxHp * STAGE_3_THRESHOLD;
                    if (health <= stage3Floor) {
                        super.setHealth(stage3Floor);
                        this.entityData.set(DATA_STAGE, 3);
                        this.bossEvent.setProgress(STAGE_3_THRESHOLD);
                        onStageTransition(sl, 3);
                        return;
                    }
                }
            }
        }
        super.setHealth(health);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 5000.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STUN_TICKS, 0);
        builder.define(DATA_STAGE, 1);
        builder.define(DATA_INTRO_TICKS, 180);
        builder.define(DATA_CINEMATIC_MODE, 1);
    }

    public int getStunTicks() {
        return this.entityData.get(DATA_STUN_TICKS);
    }

    public int getBossStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public int getIntroTicks() {
        return this.entityData.get(DATA_INTRO_TICKS);
    }

    public int getCinematicMode() {
        return this.entityData.get(DATA_CINEMATIC_MODE);
    }

    public void startIntroSequence(int ticks) {
        int clamped = Math.max(0, ticks);
        this.introTicks = clamped;
        this.cinematicMode = (clamped > 0) ? 1 : 0;
        this.entityData.set(DATA_INTRO_TICKS, clamped);
        this.entityData.set(DATA_CINEMATIC_MODE, this.cinematicMode);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 48.0F));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setData(ModAttachments.MOB_RANK, ThreatLevel.CALAMITY.ordinal());
        this.setCustomName(Component.literal("§c§l[Jefe] ").append(ThreatLevel.CALAMITY.getBadge()).append(" §fReimu Hakurei"));
        this.setCustomNameVisible(true);

        var maxHp = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(5000.0);
        }
        this.setHealth(this.getMaxHealth());

        captureAnchor();
        return data;
    }

    public void setAnchorPos(double x, double y, double z) {
        this.hasAnchor = true;
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
    }

    private void captureAnchor() {
        if (!this.hasAnchor) {
            this.hasAnchor = true;
            this.anchorX = this.getX();
            this.anchorY = this.getY();
            this.anchorZ = this.getZ();
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Sin daño por contacto ni colisión física brusca
    }

    @Override
    public void push(Entity entity) {
        // Sin empuje ni daño por contacto
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Reimu nunca golpea en cuerpo a cuerpo
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            if (getStunTicks() > 0 && this.random.nextFloat() < 0.65F) {
                this.level().addParticle(ParticleTypes.CRIT,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.9,
                        this.getY() + 1.8 + this.random.nextDouble() * 0.3,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.9,
                        0.0, 0.02, 0.0);
            } else if ((getIntroTicks() > 0 || getBossStage() == 3) && this.random.nextFloat() < 0.45F) {
                this.level().addParticle(ParticleTypes.CHERRY_LEAVES,
                        this.getX() + (this.random.nextDouble() - 0.5) * 1.4,
                        this.getY() + 0.6 + this.random.nextDouble() * 1.4,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 1.4,
                        (this.random.nextDouble() - 0.5) * 0.05, 0.03, (this.random.nextDouble() - 0.5) * 0.05);
            }
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();

        if (!this.hpCapApplied) {
            this.hpCapApplied = true;
            var maxHp = this.getAttribute(Attributes.MAX_HEALTH);
            if (maxHp != null && maxHp.getBaseValue() < 5000.0) {
                maxHp.setBaseValue(5000.0);
            }
            if (this.getHealth() == 1024.0F || this.getHealth() == 2000.0F) {
                this.setHealth(this.getMaxHealth());
            }
        }

        // Despawnear automáticamente si no queda ningún jugador vivo en El Gensokyo
        if (ModDimensions.isGensokyo(serverLevel)) {
            boolean anyAlivePlayer = serverLevel.players().stream()
                    .anyMatch(p -> p.isAlive() && !p.isSpectator());
            if (!anyAlivePlayer) {
                clearOwnedProjectiles(serverLevel);
                this.bossEvent.removeAllPlayers();
                this.discard();
                return;
            }
        }

        captureAnchor();

        // Mantener levitación suave alrededor del ancla (altura accesible para combate melee)
        double hoverOffset = (this.stunTicks > 0) ? 0.05 : (0.55 + Math.sin(this.tickCount * 0.08) * 0.22);
        this.setDeltaMovement(Vec3.ZERO);
        this.setPos(this.anchorX, this.anchorY + hoverOffset, this.anchorZ);
        this.fallDistance = 0.0F;

        // Mirar siempre al jugador desafiante más cercano
        Player target = serverLevel.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 64.0,
                p -> !p.isSpectator() && p.isAlive() && !(p instanceof Player pl && pl.isCreative()));
        if (target == null) {
            target = serverLevel.getNearestPlayer(this.getX(), this.getY(), this.getZ(), 64.0,
                    p -> !p.isSpectator() && p.isAlive());
        }

        if (target != null) {
            this.lookAt(target, 30.0F, 30.0F);
        }

        float healthPct = this.getHealth() / this.getMaxHealth();
        int currentStage = computeStage(healthPct);
        this.entityData.set(DATA_STAGE, currentStage);
        this.bossEvent.setProgress(Math.max(0.0F, Math.min(1.0F, healthPct)));

        // Fase cinemática activa (introducción o transición entre etapas): enfoca cámara e inmoviliza al jugador
        if (this.introTicks > 0) {
            this.introTicks--;
            this.entityData.set(DATA_INTRO_TICKS, this.introTicks);

            freezeNearbyPlayersDuringCinematic(serverLevel);

            if (this.cinematicMode == 2) {
                this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
                this.bossEvent.setName(Component.literal("§b§l[Etapa 2: Danza de Cerezos] ")
                        .append(ThreatLevel.CALAMITY.getBadge())
                        .append(" §fReimu Hakurei"));
            } else if (this.cinematicMode == 3) {
                this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
                this.bossEvent.setName(Component.literal("§5§l[Etapa Final: Musou Fuuin] ")
                        .append(ThreatLevel.CALAMITY.getBadge())
                        .append(" §fReimu Hakurei"));
            } else {
                this.bossEvent.setColor(BossEvent.BossBarColor.PINK);
                this.bossEvent.setName(Component.literal("§d§l[Santuario Hakurei] ")
                        .append(ThreatLevel.CALAMITY.getBadge())
                        .append(" §fReimu Hakurei"));
            }

            handleCinematicDialogue(serverLevel);

            if (this.introTicks == 0) {
                this.cinematicMode = 0;
                this.entityData.set(DATA_CINEMATIC_MODE, 0);
                this.graceTicks = 15;
            }
            return;
        }

        if (currentStage != this.lastAnnouncedStage) {
            onStageTransition(serverLevel, currentStage);
            if (this.introTicks > 0) {
                return;
            }
        }

        if (this.stunTicks > 0) {
            this.stunTicks--;
            this.entityData.set(DATA_STUN_TICKS, this.stunTicks);

            String secStr = String.format(Locale.ROOT, "%.1fs", this.stunTicks / 20.0F);
            this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
            this.bossEvent.setName(Component.literal("§e§l[ATURDIDA: " + secStr + "] ")
                    .append(ThreatLevel.CALAMITY.getBadge())
                    .append(" §fReimu Hakurei"));

            if (this.stunTicks == 0) {
                triggerRecoveryShockwave(serverLevel);
            }
            return;
        }

        updateNormalBossBar(currentStage);

        if (this.graceTicks > 0) {
            this.graceTicks--;
            return;
        }

        if (target != null && !target.isCreative() && !target.isSpectator()) {
            this.patternTick++;
            tickDanmakuPatterns(serverLevel, target, currentStage);
        } else {
            if (this.tickCount % 40 == 0) {
                this.patternTick = 0;
            }
        }
    }

    private void freezeNearbyPlayersDuringCinematic(ServerLevel sl) {
        for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(96.0),
                pl -> pl.isAlive() && !pl.isSpectator())) {
            Vec3 vel = p.getDeltaMovement();
            if (Math.abs(vel.x) > 1.0E-4 || Math.abs(vel.z) > 1.0E-4 || vel.y > 0.0) {
                p.setDeltaMovement(0.0, Math.min(0.0, vel.y), 0.0);
                p.hurtMarked = true;
            }
        }
    }

    private void handleCinematicDialogue(ServerLevel sl) {
        if (this.cinematicMode == 2) {
            switch (this.introTicks) {
                case 75 -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHERRY_WOOD_BREAK, SoundSource.HOSTILE, 1.3F, 1.2F);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.2, this.getZ(), 45, 1.4, 1.0, 1.4, 0.12);
                    broadcastMessage(sl, Component.literal("§b§l[ETAPA 2 - INTERMEDIA] §fReimu Hakurei: §7«Nada mal... Consigues leer mis patrones entre los pétalos.»"));
                }
                case 35 -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.35F);
                    broadcastMessage(sl, Component.literal("§b§l[Reimu Hakurei]: §b«Tomemos un ritmo más sereno... ¡Danza Carmesí bajo los Cerezos!»"));
                }
            }
            return;
        }

        if (this.cinematicMode == 3) {
            switch (this.introTicks) {
                case 95 -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.3F, 1.2F);
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.2, this.getZ(), 1, 0, 0, 0, 0);
                    broadcastMessage(sl, Component.literal("§5§l[ETAPA FINAL - EXTREMA] §cReimu Hakurei: §f«¡Se acabó contenerme! Has demostrado ser una amenaza real para el Santuario.»"));
                }
                case 45 -> {
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.4F);
                    sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.2, this.getZ(), 60, 1.4, 1.2, 1.4, 0.16);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.2, this.getZ(), 85, 1.8, 1.4, 1.8, 0.18);
                    broadcastMessage(sl, Component.literal("§4§l[SPELL CARD FINAL] §cReimu Hakurei: §e«¡Musou Fuuin — Fantasía del Paraíso!»"));
                }
            }
            return;
        }

        // Modo 1: Introducción inicial
        switch (this.introTicks) {
            case 165 -> {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2F, 1.1F);
                broadcastMessage(sl, Component.literal("§c§l[Reimu Hakurei]: §f«¿Una fisura forzada en la Gran Barrera Hakurei...? ¡Tú no perteneces a este mundo!»"));
            }
            case 115 -> {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHERRY_WOOD_STEP, SoundSource.HOSTILE, 1.2F, 1.0F);
                broadcastMessage(sl, Component.literal("§c§l[Reimu Hakurei]: §f«Los invasores de otra dimensión que amenazan el equilibrio del Gensokyo son mi responsabilidad como Miko del Santuario.»"));
            }
            case 65 -> {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.1F, 1.25F);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.2, this.getZ(), 45, 1.4, 1.0, 1.4, 0.12);
                broadcastMessage(sl, Component.literal("§c§l[Reimu Hakurei]: §e«No sé cómo rasgaste la barrera bajo los cerezos, ¡pero te expulsaré ahora mismo con las Reglas de las Spell Cards!»"));
            }
            case 20 -> {
                sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.1F, 1.35F);
                sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.2, this.getZ(), 2, 0.3, 0.3, 0.3, 0.0);
                sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.2, this.getZ(), 40, 1.0, 1.0, 1.0, 0.16);
                broadcastMessage(sl, Component.literal("§4§l[¡SPELL CARD ACTIVADA!] §cReimu Hakurei: §6«¡Sello Espiritual — Danza Carmesí del Santuario Hakurei!»"));
            }
        }
    }

    private int computeStage(float healthPct) {
        if (healthPct > STAGE_2_THRESHOLD) return 1; // 100% - 60% HP (5000 - 3000)
        if (healthPct > STAGE_3_THRESHOLD) return 2; // 60% - 15% HP  (3000 - 750)
        return 3;                                    // 15% - 0% HP   (750 - 0)
    }

    private void onStageTransition(ServerLevel sl, int newStage) {
        this.lastAnnouncedStage = newStage;
        this.patternTick = 0;
        this.subPatternIndex = 0;
        this.stunTicks = 0;
        this.entityData.set(DATA_STUN_TICKS, 0);

        clearOwnedProjectiles(sl);

        // Si el jugador estaba muy pegado a Reimu (< 9 bloques), alejarlo limpiamente a ~11 bloques para el encuadre
        for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(96.0),
                pl -> pl.isAlive() && !pl.isSpectator())) {
            double dist = Math.hypot(p.getX() - this.anchorX, p.getZ() - this.anchorZ);
            if (dist < 9.0) {
                Vec3 away = p.position().subtract(this.anchorX, this.anchorY, this.anchorZ);
                Vec3 flat = new Vec3(away.x, 0.0, away.z);
                if (flat.lengthSqr() < 1.0E-4) {
                    flat = new Vec3(0.0, 0.0, -1.0);
                } else {
                    flat = flat.normalize();
                }
                double destX = this.anchorX + flat.x * 11.0;
                double destZ = this.anchorZ + flat.z * 11.0;
                if (ModDimensions.isGensokyo(sl)
                        && (destX < -12.0 || destX > 12.0 || destZ < -5.0 || destZ > 26.0)) {
                    destX = this.anchorX;
                    destZ = this.anchorZ - 11.0;
                }
                p.teleportTo(destX, this.anchorY, destZ);
            }
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
        }

        if (newStage == 2) {
            this.cinematicMode = 2;
            this.introTicks = 85;
            this.entityData.set(DATA_CINEMATIC_MODE, 2);
            this.entityData.set(DATA_INTRO_TICKS, 85);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.2, this.getZ(), 50, 1.5, 1.2, 1.5, 0.12);
        } else if (newStage == 3) {
            this.cinematicMode = 3;
            this.introTicks = 105;
            this.entityData.set(DATA_CINEMATIC_MODE, 3);
            this.entityData.set(DATA_INTRO_TICKS, 105);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.2, this.getZ(), 90, 2.0, 1.5, 2.0, 0.18);
        }
    }

    private void updateNormalBossBar(int stage) {
        switch (stage) {
            case 1 -> {
                this.bossEvent.setColor(BossEvent.BossBarColor.RED);
                this.bossEvent.setName(Component.literal("§c§l[Etapa 1: Impacto Carmesí] ")
                        .append(ThreatLevel.CALAMITY.getBadge())
                        .append(" §fReimu Hakurei"));
            }
            case 2 -> {
                this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
                this.bossEvent.setName(Component.literal("§b§l[Etapa 2: Danza de Cerezos] ")
                        .append(ThreatLevel.CALAMITY.getBadge())
                        .append(" §fReimu Hakurei"));
            }
            default -> {
                this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
                this.bossEvent.setName(Component.literal("§5§l[Etapa Final: Fantasía del Paraíso] ")
                        .append(ThreatLevel.CALAMITY.getBadge())
                        .append(" §fReimu Hakurei"));
            }
        }
    }

    private void tickDanmakuPatterns(ServerLevel sl, Player target, int stage) {
        Vec3 origin = new Vec3(this.getX(), this.getY() + 0.9, this.getZ());
        Vec3 toPlayer = target.position().add(0.0, 0.9, 0.0).subtract(origin);
        double flatLen = Math.hypot(toPlayer.x, toPlayer.z);
        float aimYaw = (float) Math.atan2(toPlayer.z, toPlayer.x);
        float aimPitchY = flatLen > 1.0E-4 ? (float) Math.max(-0.25, Math.min(0.25, toPlayer.y / flatLen * 0.4)) : 0.0F;

        switch (stage) {
            case 1 -> {
                if (this.patternTick % 90 == 0) {
                    this.subPatternIndex = (this.subPatternIndex + 1) % 3;
                }
                if (this.subPatternIndex == 0) {
                    if (this.patternTick % 4 == 0) {
                        this.spiralAngle += 0.24F;
                        for (int arm = 0; arm < 4; arm++) {
                            float ang = this.spiralAngle + (float) (arm * Math.PI / 2.0);
                            spawnDanmakuOrb(sl, origin, new Vec3(Math.cos(ang), 0.0, Math.sin(ang)).scale(0.36), 0.85F, 140);
                        }
                        if (this.patternTick % 12 == 0) {
                            sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.7F, 1.5F);
                        }
                    }
                    if (this.patternTick % 26 == 0) {
                        spawnAimedFan(sl, origin, aimYaw, aimPitchY, 5, 0.20F, 0.42F, 1.05F);
                    }
                } else if (this.subPatternIndex == 1) {
                    if (this.patternTick % 45 == 1) {
                        spawnOrbitingRing(sl, origin, 8, 2.6F, 0.12F, 30, 0.34F, 0.95F);
                        sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 0.9F, 1.4F);
                    }
                    if (this.patternTick % 18 == 0) {
                        spawnAimedFan(sl, origin, aimYaw, aimPitchY, 3, 0.22F, 0.38F, 0.75F);
                    }
                } else {
                    if (this.patternTick % 5 == 0) {
                        this.spiralAngle += 0.20F;
                        float angA = this.spiralAngle;
                        float angB = -this.spiralAngle;
                        spawnDanmakuOrb(sl, origin, new Vec3(Math.cos(angA), 0.0, Math.sin(angA)).scale(0.34), 0.8F, 150);
                        spawnDanmakuOrb(sl, origin, new Vec3(Math.cos(angB), 0.0, Math.sin(angB)).scale(0.34), 0.8F, 150);
                    }
                    if (this.patternTick % 35 == 0) {
                        Vec3 bigDir = toPlayer.normalize().scale(0.24);
                        spawnDanmakuOrb(sl, origin, bigDir, 1.55F, 180);
                    }
                }
            }
            case 2 -> {
                if (this.patternTick % 22 == 0) {
                    this.spiralAngle += 0.35F;
                    int count = 10;
                    for (int i = 0; i < count; i++) {
                        float ang = this.spiralAngle + (float) (i * (Math.PI * 2.0 / count));
                        spawnDanmakuOrb(sl, origin, new Vec3(Math.cos(ang), 0.0, Math.sin(ang)).scale(0.26), 0.90F, 160);
                    }
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, 1.3F);
                }
                if (this.patternTick % 34 == 0) {
                    spawnAimedFan(sl, origin, aimYaw, aimPitchY, 3, 0.26F, 0.30F, 1.0F);
                }
            }
            default -> {
                if (this.patternTick % 3 == 0) {
                    this.spiralAngle += 0.21F;
                    for (int arm = 0; arm < 3; arm++) {
                        float a1 = this.spiralAngle + (float) (arm * (Math.PI * 2.0 / 3.0));
                        float a2 = -this.spiralAngle * 0.85F + (float) (arm * (Math.PI * 2.0 / 3.0));
                        spawnDanmakuOrb(sl, origin, new Vec3(Math.cos(a1), 0.0, Math.sin(a1)).scale(0.40), 0.55F, 150);
                        spawnDanmakuOrb(sl, origin, new Vec3(Math.cos(a2), 0.0, Math.sin(a2)).scale(0.32), 0.70F, 150);
                    }
                }
                if (this.patternTick % 22 == 0) {
                    spawnAimedFan(sl, origin, aimYaw, aimPitchY, 7, 0.16F, 0.44F, 0.85F);
                    spawnDanmakuOrb(sl, origin, toPlayer.normalize().scale(0.21), 2.1F, 200);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.0F, 1.8F);
                }
                if (this.patternTick % 50 == 1) {
                    spawnOrbitingRing(sl, origin, 12, 3.2F, -0.15F, 26, 0.38F, 0.90F);
                }
            }
        }
    }

    private void spawnAimedFan(ServerLevel sl, Vec3 origin, float centerYaw, float pitchY, int count,
                               float spreadAngle, float speed, float scale) {
        int half = count / 2;
        for (int i = -half; i <= half; i++) {
            float yaw = centerYaw + i * spreadAngle;
            Vec3 dir = new Vec3(Math.cos(yaw), pitchY, Math.sin(yaw)).normalize().scale(speed);
            spawnDanmakuOrb(sl, origin, dir, scale, 160);
        }
    }

    private void spawnOrbitingRing(ServerLevel sl, Vec3 center, int count, float radius,
                                   float angularSpeed, int orbitTicks, float launchSpeed, float scale) {
        for (int i = 0; i < count; i++) {
            float startAng = (float) (i * (Math.PI * 2.0 / count));
            EnergyAttackEntity orb = new EnergyAttackEntity(sl, this, scale, 0.0F);
            orb.setLethalInstakill(true);
            orb.setMaxLifeTicks(orbitTicks + 150);
            orb.configureOrbit(center, radius, startAng, angularSpeed, orbitTicks, launchSpeed, null);
            sl.addFreshEntity(orb);
        }
    }

    private void spawnDanmakuOrb(ServerLevel sl, Vec3 pos, Vec3 velocity, float scale, int lifeTicks) {
        EnergyAttackEntity orb = new EnergyAttackEntity(sl, this, scale, 0.0F);
        orb.setLethalInstakill(true);
        orb.setMaxLifeTicks(lifeTicks);
        orb.setPos(pos.x, pos.y, pos.z);
        orb.setDeltaMovement(velocity);
        sl.addFreshEntity(orb);
    }

    private void clearOwnedProjectiles(ServerLevel sl) {
        List<EnergyAttackEntity> orbs = sl.getEntitiesOfClass(EnergyAttackEntity.class, this.getBoundingBox().inflate(80.0),
                orb -> orb.getOwner() == this || orb.isLethalInstakill());
        for (EnergyAttackEntity orb : orbs) {
            orb.dissipate(false);
        }
    }

    private void triggerRecoveryShockwave(ServerLevel sl) {
        double x = this.getX();
        double y = this.getY() + 1.0;
        double z = this.getZ();

        sl.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4F, 1.4F);
        sl.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.1F, 1.3F);

        sl.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 2, 0.2, 0.2, 0.2, 0.0);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, x, y, z, 65, 2.0, 0.8, 2.0, 0.25);
        sl.sendParticles(ParticleTypes.CLOUD, x, y - 0.5, z, 50, 2.5, 0.3, 2.5, 0.25);

        List<Player> players = sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(20.0),
                p -> p.isAlive() && !p.isSpectator());
        for (Player player : players) {
            Vec3 away = player.position().subtract(this.position());
            Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
            if (horizontal.lengthSqr() < 1.0E-4) {
                horizontal = new Vec3(0.0, 0.0, -1.0);
            } else {
                horizontal = horizontal.normalize();
            }
            player.setDeltaMovement(horizontal.x * 2.35, 0.62, horizontal.z * 2.35);
            player.hurtMarked = true;
            player.displayClientMessage(Component.literal("§c⛩ ¡Onda de choque del Santuario Hakurei! Esquiva para acercarte de nuevo."), true);
        }

        this.graceTicks = 25;
        this.patternTick = 0;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) {
            return false;
        }
        // Inmune mientras habla en su diálogo de introducción
        if (this.introTicks > 0) {
            return false;
        }
        if (source.getEntity() == this || (source.getDirectEntity() instanceof EnergyAttackEntity orb && orb.isLethalInstakill())) {
            return false;
        }
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            this.setPos(this.anchorX, this.anchorY + 0.5, this.anchorZ);
            return false;
        }
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.FALL)) {
            return false;
        }

        boolean hurt;
        if (source.is(DamageTypes.GENERIC_KILL)) {
            this.allowBypassPhaseGate = true;
            try {
                hurt = super.hurt(source, amount);
            } finally {
                this.allowBypassPhaseGate = false;
            }
        } else {
            hurt = super.hurt(source, amount);
        }
        if (hurt && this.isAlive() && amount > 0.0F) {
            this.invulnerableTime = 3;

            if (this.introTicks <= 0 && this.stunTicks <= 0 && (source.getEntity() instanceof LivingEntity)) {
                this.stunTicks = 100;
                this.entityData.set(DATA_STUN_TICKS, 100);

                if (this.level() instanceof ServerLevel sl) {
                    clearOwnedProjectiles(sl);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.5F, 1.3F);
                    sl.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.2F, 1.1F);
                    sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.2, this.getZ(), 35, 0.6, 0.6, 0.6, 0.15);
                    sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY() + 1.0, this.getZ(), 40, 1.0, 0.8, 1.0, 0.15);
                    broadcastMessage(sl, Component.literal("§e⛩ ¡Has roto su ritmo! Reimu está §lATURDIDA durante 5 segundos§e — ¡Golpéala todo lo que puedas!"));
                }
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource cause) {
        this.bossEvent.removeAllPlayers();

        if (!this.level().isClientSide() && !this.transformedToNpc && this.level() instanceof ServerLevel sl) {
            this.transformedToNpc = true;
            clearOwnedProjectiles(sl);

            for (ReimuNpcEntity existingNpc : sl.getEntities(ModEntities.REIMU_NPC.get(), e -> true)) {
                existingNpc.discard();
            }
            for (ReimuBossEntity existingBoss : sl.getEntities(ModEntities.REIMU_BOSS.get(), e -> e != this)) {
                existingBoss.discard();
            }

            ReimuNpcEntity npc = ModEntities.REIMU_NPC.get().create(sl);
            if (npc != null) {
                npc.moveTo(this.anchorX, this.anchorY, this.anchorZ, this.getYRot(), 0.0F);
                npc.setCustomName(Component.literal("§c⛩ Reimu Hakurei"));
                npc.setCustomNameVisible(true);
                npc.setPersistenceRequired();
                sl.addFreshEntity(npc);
            }

            sl.playSound(null, this.anchorX, this.anchorY, this.anchorZ, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2F, 1.05F);
            sl.playSound(null, this.anchorX, this.anchorY, this.anchorZ, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9F, 1.3F);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, this.anchorX, this.anchorY + 1.2, this.anchorZ, 80, 1.5, 1.2, 1.5, 0.15);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.anchorX, this.anchorY + 1.0, this.anchorZ, 45, 0.6, 0.8, 0.6, 0.2);

            for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(96.0))) {
                p.getPersistentData().putBoolean("DefeatedReimuBoss", true);
                if (p instanceof ServerPlayer sp) {
                    PlayerRpgData data = sp.getData(ModAttachments.PLAYER_RPG_DATA);
                    if (data != null && !data.hasSkill(SpiritualTechnique.HAKUREI.getId())) {
                        data.unlockSkill(SpiritualTechnique.HAKUREI.getId());
                        ModNetworking.syncToPlayer(sp);
                        sp.sendSystemMessage(Component.literal("§c✦ §l[REGALO DEL SANTUARIO HAKUREI] §eHas desbloqueado: §6§lTécnica Espiritual Nro. 890: Ha-ku-rei §7([ALT] + 3 - 1 - 3 - 1)"));
                    }
                    sp.sendSystemMessage(Component.literal("§c§l[Reimu Hakurei]: §f\"Vaya... sí que lograste colarte entre mis barreras. Nada mal para alguien de fuera del Gensokyo. Toma esto como obsequio del santuario, y pasa a charlar cuando quieras.\""));
                }
            }
        }

        super.die(cause);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    private void broadcastMessage(ServerLevel sl, Component msg) {
        for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(72.0))) {
            p.sendSystemMessage(msg);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasAnchor", this.hasAnchor);
        compound.putDouble("AnchorX", this.anchorX);
        compound.putDouble("AnchorY", this.anchorY);
        compound.putDouble("AnchorZ", this.anchorZ);
        compound.putInt("IntroTicks", this.introTicks);
        compound.putInt("CinematicMode", this.cinematicMode);
        compound.putInt("StunTicks", this.stunTicks);
        compound.putInt("LastStage", this.lastAnnouncedStage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        if (compound.contains("LastStage")) {
            this.lastAnnouncedStage = compound.getInt("LastStage");
        }
        if (compound.contains("IntroTicks")) {
            this.introTicks = compound.getInt("IntroTicks");
            this.entityData.set(DATA_INTRO_TICKS, this.introTicks);
        }
        if (compound.contains("CinematicMode")) {
            this.cinematicMode = compound.getInt("CinematicMode");
            this.entityData.set(DATA_CINEMATIC_MODE, this.cinematicMode);
        }
        this.allowBypassPhaseGate = true;
        try {
            super.readAdditionalSaveData(compound);
        } finally {
            this.allowBypassPhaseGate = false;
        }
        this.hasAnchor = compound.getBoolean("HasAnchor");
        if (this.hasAnchor) {
            this.anchorX = compound.getDouble("AnchorX");
            this.anchorY = compound.getDouble("AnchorY");
            this.anchorZ = compound.getDouble("AnchorZ");
        }
        this.stunTicks = compound.getInt("StunTicks");
        this.entityData.set(DATA_STUN_TICKS, this.stunTicks);
    }
}
