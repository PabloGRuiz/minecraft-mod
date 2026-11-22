package com.pablo.tenseicraft.magic.beam;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.compat.irons.IronsSpellsCompat;
import com.pablo.tenseicraft.entities.SpiritualBeamEntity;
import com.pablo.tenseicraft.network.EndBeamClashPayload;
import com.pablo.tenseicraft.network.SyncBeamClashPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor del sistema de Choques de Poderes (Beam Clash).
 * Administra las batallas de haz entre dos combatientes (Jugador vs Jugador o Jugador vs Voidborn Executor).
 * Controla el minijuego de puja por spam de clics, drenaje de maná, cálculo de posición del choque,
 * cinemática y cataclismo final de resolución.
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class BeamClashManager {

    public static final int WARMUP_TICKS = 30; // 1.5 segundos de retardo inicial de preparación
    public static final int MAX_CLASH_TICKS = 230; // 30 ticks de warmup + 200 ticks de forcejeo activo

    public static class ClashSession {
        public final UUID clashId;
        public final ServerLevel level;
        public final LivingEntity entityA;
        public final LivingEntity entityB;
        public final SpiritualBeamEntity beamA;
        public final SpiritualBeamEntity beamB;

        public float scoreA = 10.0F;
        public float scoreB = 10.0F;
        public float progress = 0.5F; // 0.5 = centro; >0.5 empuja hacia B (gana A); <0.5 empuja hacia A (gana B)
        public int elapsedTicks = 0;
        public Vec3 clashPos;

        public float simulatedManaA = 300.0F;
        public float simulatedManaB = 300.0F;

        public ClashSession(UUID clashId, ServerLevel level, LivingEntity a, LivingEntity b,
                            SpiritualBeamEntity beamA, SpiritualBeamEntity beamB, Vec3 initialPos) {
            this.clashId = clashId;
            this.level = level;
            this.entityA = a;
            this.entityB = b;
            this.beamA = beamA;
            this.beamB = beamB;
            this.clashPos = initialPos;
        }

        public boolean isInWarmup() {
            return elapsedTicks < WARMUP_TICKS;
        }

        public int getWarmupRemainingTicks() {
            return Math.max(0, WARMUP_TICKS - elapsedTicks);
        }
    }

    private static final Map<UUID, ClashSession> SERVER_SESSIONS = new ConcurrentHashMap<>();
    private static final Map<Integer, UUID> ENTITY_TO_CLASH = new ConcurrentHashMap<>();

    // Estado del Choque en Cliente
    public static class ClientClashState {
        public UUID clashId = null;
        public int entityAId = -1;
        public int entityBId = -1;
        public float scoreA = 0.0F;
        public float scoreB = 0.0F;
        public float progress = 0.5F;
        public Vec3 clashPos = Vec3.ZERO;
        public int remainingTicks = 0;
        public int warmupRemainingTicks = 0;
        public float manaA = 0.0F;
        public float manaB = 0.0F;
        public int colorA = 0x33A6FF;
        public int colorB = 0xFF5533;
        public long lastSyncTick = 0;
    }

    public static final ClientClashState CLIENT_STATE = new ClientClashState();

    public static void startClash(ServerLevel level, SpiritualBeamEntity beamA, SpiritualBeamEntity beamB) {
        LivingEntity casterA = beamA.getCaster();
        LivingEntity casterB = beamB.getCaster();
        if (casterA == null || casterB == null) return;
        if (ENTITY_TO_CLASH.containsKey(casterA.getId()) || ENTITY_TO_CLASH.containsKey(casterB.getId())) return;

        UUID clashId = UUID.randomUUID();
        Vec3 posA = casterA.getEyePosition();
        Vec3 posB = casterB.getEyePosition();
        Vec3 midPoint = posA.lerp(posB, 0.5);

        beamA.setClashing(true);
        beamA.setClashPos(midPoint);
        beamA.setClashId(clashId);

        beamB.setClashing(true);
        beamB.setClashPos(midPoint);
        beamB.setClashId(clashId);

        ClashSession session = new ClashSession(clashId, level, casterA, casterB, beamA, beamB, midPoint);
        SERVER_SESSIONS.put(clashId, session);
        ENTITY_TO_CLASH.put(casterA.getId(), clashId);
        ENTITY_TO_CLASH.put(casterB.getId(), clashId);

        // Sonidos y explosión de contacto inicial
        level.playSound(null, midPoint.x, midPoint.y, midPoint.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.5F, 1.8F);
        level.playSound(null, midPoint.x, midPoint.y, midPoint.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 1.4F);
        level.playSound(null, midPoint.x, midPoint.y, midPoint.z,
                SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 2.5F, 0.7F);

        level.sendParticles(ParticleTypes.FLASH, midPoint.x, midPoint.y, midPoint.z, 2, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, midPoint.x, midPoint.y, midPoint.z, 2, 0.3, 0.3, 0.3, 0);

        broadcastSync(session);
    }

    public static void handlePlayerClick(ServerPlayer player) {
        UUID clashId = ENTITY_TO_CLASH.get(player.getId());
        if (clashId == null) return;
        ClashSession session = SERVER_SESSIONS.get(clashId);
        if (session == null) return;

        // Durante el calentamiento inicial (1.5s), no se acumulan ventajas desmedidas
        if (session.isInWarmup()) {
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.40F, 2.0F);
            return;
        }

        // Fórmula: 1 clic base * (1.0 + reservas de maná / 100)
        float currentMana = Math.max(0.0F, IronsSpellsCompat.getIronsMana(player));
        float multiplier = 1.0F + (currentMana / 100.0F);
        float points = 1.0F * multiplier;

        if (session.entityA == player) {
            session.scoreA += points;
        } else if (session.entityB == player) {
            session.scoreB += points;
        }

        // Sonido sutil de pulso al jugador
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.45F, 1.7F);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (SERVER_SESSIONS.isEmpty()) return;

        List<UUID> toRemove = new ArrayList<>();

        for (ClashSession session : SERVER_SESSIONS.values()) {
            session.elapsedTicks++;

            // 1. Validar que ambos combatientes y haces sigan vivos y activos
            if (!session.entityA.isAlive() || session.entityA.isRemoved() || !session.beamA.isAlive() || session.beamA.isRemoved()) {
                resolveClash(session.level, session, session.entityB, session.entityA, false);
                toRemove.add(session.clashId);
                continue;
            }
            if (!session.entityB.isAlive() || session.entityB.isRemoved() || !session.beamB.isAlive() || session.beamB.isRemoved()) {
                resolveClash(session.level, session, session.entityA, session.entityB, false);
                toRemove.add(session.clashId);
                continue;
            }

            // 2. Anclar en el sitio a los participantes y orientarlos hacia el choque
            Vec3 posA = session.entityA.getEyePosition();
            Vec3 posB = session.entityB.getEyePosition();
            session.entityA.setDeltaMovement(0, session.entityA.getDeltaMovement().y > 0 ? 0 : session.entityA.getDeltaMovement().y, 0);
            session.entityB.setDeltaMovement(0, session.entityB.getDeltaMovement().y > 0 ? 0 : session.entityB.getDeltaMovement().y, 0);

            // 3. Drenaje continuo de maná
            float manaDrain = 0.25F; // 5 MP/s
            drainMana(session.entityA, session, true, manaDrain);
            drainMana(session.entityB, session, false, manaDrain);

            // Verificar si alguno se quedó sin maná
            float manaA = getMana(session.entityA, session, true);
            float manaB = getMana(session.entityB, session, false);
            if (manaA <= 0.0F) {
                resolveClash(session.level, session, session.entityB, session.entityA, false);
                toRemove.add(session.clashId);
                continue;
            }
            if (manaB <= 0.0F) {
                resolveClash(session.level, session, session.entityA, session.entityB, false);
                toRemove.add(session.clashId);
                continue;
            }

            if (session.isInWarmup()) {
                // Durante el retardo inicial de preparación (1.5 segundos):
                // La barra permanece fija y centrada (0.5F) para dar tiempo a que ambos jugadores se preparen
                session.progress = 0.5F;
                session.clashPos = posA.lerp(posB, 0.5);
                session.beamA.setClashPos(session.clashPos);
                session.beamB.setClashPos(session.clashPos);

                if (session.elapsedTicks % 10 == 0) {
                    session.level.playSound(null, session.clashPos.x, session.clashPos.y, session.clashPos.z,
                            SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.4F, 1.8F);
                }
            } else {
                // 4. IA para Mobs (ej. Voidborn Executor): simula clics de puja automáticamente
                if (!(session.entityA instanceof Player)) {
                    simulateMobClick(session, true);
                }
                if (!(session.entityB instanceof Player)) {
                    simulateMobClick(session, false);
                }

                // 5. Cálculo del progreso del forcejeo (Tug-of-War)
                // progress = 0.5 (medio); >0.5 empuja hacia B (gana A); <0.5 empuja hacia A (gana B)
                float totalScore = session.scoreA + session.scoreB;
                float diff = session.scoreA - session.scoreB;
                float targetProgress = Mth.clamp(0.5F + (diff / (Math.max(25.0F, totalScore * 0.45F) + 35.0F)), 0.04F, 0.96F);
                session.progress = Mth.lerp(0.12F, session.progress, targetProgress);

                // Posición física del punto de choque
                session.clashPos = posA.lerp(posB, session.progress);
                session.beamA.setClashPos(session.clashPos);
                session.beamB.setClashPos(session.clashPos);

                // Efectos sonoros y partículas del choque continuo cada 8 ticks
                if (session.elapsedTicks % 8 == 0) {
                    session.level.playSound(null, session.clashPos.x, session.clashPos.y, session.clashPos.z,
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.8F, 1.6F);
                    session.level.playSound(null, session.clashPos.x, session.clashPos.y, session.clashPos.z,
                            SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.2F, 1.8F);
                    session.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            session.clashPos.x, session.clashPos.y, session.clashPos.z, 10, 0.4, 0.4, 0.4, 0.1);
                }

                // 6. Condiciones de victoria
                if (session.progress >= 0.94F) {
                    // Haz de A penetró completamente la defensa de B
                    resolveClash(session.level, session, session.entityA, session.entityB, false);
                    toRemove.add(session.clashId);
                    continue;
                } else if (session.progress <= 0.06F) {
                    // Haz de B penetró totalmente a A
                    resolveClash(session.level, session, session.entityB, session.entityA, false);
                    toRemove.add(session.clashId);
                    continue;
                } else if (session.elapsedTicks >= MAX_CLASH_TICKS) {
                    // Se acabó el tiempo total: gana el que tenga mayor puntaje acumulado
                    LivingEntity winner = (session.scoreA >= session.scoreB) ? session.entityA : session.entityB;
                    LivingEntity loser = (winner == session.entityA) ? session.entityB : session.entityA;
                    resolveClash(session.level, session, winner, loser, true);
                    toRemove.add(session.clashId);
                    continue;
                }
            }

            // 7. Sincronización periódica a los clientes
            if (session.elapsedTicks % 2 == 0) {
                broadcastSync(session);
            }
        }

        for (UUID id : toRemove) {
            ClashSession session = SERVER_SESSIONS.remove(id);
            if (session != null) {
                ENTITY_TO_CLASH.remove(session.entityA.getId());
                ENTITY_TO_CLASH.remove(session.entityB.getId());
            }
        }
    }

    private static void drainMana(LivingEntity entity, ClashSession session, boolean isA, float amount) {
        if (entity instanceof Player player) {
            IronsSpellsCompat.consumeIronsMana(player, amount);
        } else {
            if (isA) {
                session.simulatedManaA = Math.max(0.0F, session.simulatedManaA - amount);
            } else {
                session.simulatedManaB = Math.max(0.0F, session.simulatedManaB - amount);
            }
        }
    }

    private static float getMana(LivingEntity entity, ClashSession session, boolean isA) {
        if (entity instanceof Player player) {
            float m = IronsSpellsCompat.getIronsMana(player);
            return m >= 0 ? m : 100.0F;
        } else {
            return isA ? session.simulatedManaA : session.simulatedManaB;
        }
    }

    private static void simulateMobClick(ClashSession session, boolean isA) {
        // La IA del mob hace clics a un ritmo competitivo (cada 3-5 ticks)
        if (session.level.random.nextFloat() < 0.60F) {
            float mana = isA ? session.simulatedManaA : session.simulatedManaB;
            float mult = 1.0F + (mana / 100.0F);
            float points = 1.0F * mult;
            if (isA) {
                session.scoreA += points;
            } else {
                session.scoreB += points;
            }
        }
    }

    private static void resolveClash(ServerLevel level, ClashSession session, LivingEntity winner, LivingEntity loser, boolean wasTimeout) {
        Vec3 clash = session.clashPos;

        // Cataclismo final: sonido colosal y explosión masiva
        level.playSound(null, clash.x, clash.y, clash.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.5F, 0.65F);
        level.playSound(null, clash.x, clash.y, clash.z,
                SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 2.5F, 1.25F);
        level.playSound(null, clash.x, clash.y, clash.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 2.8F, 0.7F);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, clash.x, clash.y, clash.z, 4, 0.5, 0.5, 0.5, 0);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, clash.x, clash.y, clash.z, 120, 2.0, 2.0, 2.0, 0.25);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, clash.x, clash.y, clash.z, 80, 1.8, 1.8, 1.8, 0.15);

        // El perdedor sufre daño masivo directo y retroceso extremo
        float explosionDamage = 52.0F;
        var damageSource = (winner instanceof Player player)
                ? level.damageSources().playerAttack(player)
                : level.damageSources().mobAttack(winner);
        loser.hurt(damageSource, explosionDamage);

        Vec3 kbDir = loser.position().subtract(clash).normalize();
        loser.setDeltaMovement(kbDir.x * 2.8, 0.9, kbDir.z * 2.8);
        loser.hurtMarked = true;

        loser.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 1, false, false));
        loser.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 3, false, false));

        // Desactivar haces
        if (session.beamA != null && session.beamA.isAlive()) {
            if (winner == session.entityA) {
                session.beamA.setClashing(false);
                session.beamA.setBeamLength(SpiritualBeamEntity.MAX_BEAM_RANGE);
            } else {
                session.beamA.discard();
            }
        }
        if (session.beamB != null && session.beamB.isAlive()) {
            if (winner == session.entityB) {
                session.beamB.setClashing(false);
                session.beamB.setBeamLength(SpiritualBeamEntity.MAX_BEAM_RANGE);
            } else {
                session.beamB.discard();
            }
        }

        // Notificar a todos los clientes cercanos
        EndBeamClashPayload endPayload = new EndBeamClashPayload(
                session.clashId,
                winner.getId(),
                loser.getId(),
                clash.x, clash.y, clash.z,
                wasTimeout
        );
        PacketDistributor.sendToPlayersTrackingEntity(winner, endPayload);
        if (winner instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, endPayload);
        if (loser instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, endPayload);
    }

    private static void broadcastSync(ClashSession session) {
        int remTicks = Math.max(0, MAX_CLASH_TICKS - session.elapsedTicks);
        float manaA = getMana(session.entityA, session, true);
        float manaB = getMana(session.entityB, session, false);

        SyncBeamClashPayload payload = new SyncBeamClashPayload(
                session.clashId,
                session.entityA.getId(),
                session.entityB.getId(),
                session.scoreA,
                session.scoreB,
                session.progress,
                session.clashPos.x,
                session.clashPos.y,
                session.clashPos.z,
                remTicks,
                session.getWarmupRemainingTicks(),
                manaA,
                manaB,
                session.beamA.getColorRgb(),
                session.beamB.getColorRgb()
        );

        PacketDistributor.sendToPlayersTrackingEntity(session.entityA, payload);
        if (session.entityA instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, payload);
        if (session.entityB instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, payload);
    }

    // =========================================================================
    // CLIENT-SIDE RECEIVERS & HELPERS
    // =========================================================================

    public static void handleClientSync(SyncBeamClashPayload payload) {
        CLIENT_STATE.clashId = payload.clashId();
        CLIENT_STATE.entityAId = payload.entityAId();
        CLIENT_STATE.entityBId = payload.entityBId();
        CLIENT_STATE.scoreA = payload.scoreA();
        CLIENT_STATE.scoreB = payload.scoreB();
        CLIENT_STATE.progress = payload.progress();
        CLIENT_STATE.clashPos = new Vec3(payload.clashX(), payload.clashY(), payload.clashZ());
        CLIENT_STATE.remainingTicks = payload.remainingTicks();
        CLIENT_STATE.warmupRemainingTicks = payload.warmupRemainingTicks();
        CLIENT_STATE.manaA = payload.manaA();
        CLIENT_STATE.manaB = payload.manaB();
        CLIENT_STATE.colorA = payload.colorA();
        CLIENT_STATE.colorB = payload.colorB();
        CLIENT_STATE.lastSyncTick = System.currentTimeMillis();
    }

    public static void handleClientEnd(EndBeamClashPayload payload) {
        if (CLIENT_STATE.clashId != null && CLIENT_STATE.clashId.equals(payload.clashId())) {
            CLIENT_STATE.clashId = null;
            CLIENT_STATE.entityAId = -1;
            CLIENT_STATE.entityBId = -1;
            CLIENT_STATE.remainingTicks = 0;
        }
    }

    public static boolean isPlayerInClash(int playerId) {
        return CLIENT_STATE.clashId != null &&
                (CLIENT_STATE.entityAId == playerId || CLIENT_STATE.entityBId == playerId);
    }
}
