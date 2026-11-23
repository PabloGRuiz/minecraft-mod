package com.pablo.tenseicraft.events;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.network.PrimordialFuryComboPayload;
import com.pablo.tenseicraft.network.SyncEntitySmokePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Motor del modo temporal "Furia Primordial" (estilo Modo Tachy + Manto Celestial).
 * - Se carga peleando con la Espada Espiritual sellada (0% -> 100%).
 * - Al 100%, presionar [Z] desata el Manto Celestial por tiempo limitado (~22s).
 * - Habilita 3 combos destructivos (100% visuales, sin romper bloques del terreno).
 */
@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class PrimordialFuryEvents {

    private static final float DRAIN_PER_TICK = 0.22f; // ~454 ticks (~22.7 segundos)
    private static final float CHARGE_PER_HIT = 1.8f;
    private static final float CHARGE_PER_KILL = 4.0f;
    private static final long CHARGE_HIT_COOLDOWN_TICKS = 5L;
    private static final long COMBO_COOLDOWN_TICKS = 14L;
    private static final long SLASH_COOLDOWN_TICKS = 7L;

    private static final Map<UUID, Long> LAST_CHARGE_HIT_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_COMBO_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_SLASH_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_ASCENDING_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> AERIAL_DROP_START_TICK = new ConcurrentHashMap<>();

    public static void tryActivateFury(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.isPrimordialFuryTrained()) return;

        if (data.isPrimordialFuryActive()) {
            player.displayClientMessage(Component.literal("§f✦ Furia Primordial ya está activa (§e"
                    + Math.round(data.getPrimordialFuryCharge()) + "% §frestante)."), true);
            return;
        }

        if (data.getPrimordialFuryCharge() < 100.0f) {
            player.displayClientMessage(Component.literal("§cFuria Primordial insuficiente (§e"
                    + Math.round(data.getPrimordialFuryCharge()) + "%§c/100%). Pelea con tu espada sellada para cargarla."), true);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7f, 1.3f);
            return;
        }

        // ¡DESATAR FURIA PRIMORDIAL!
        data.setPrimordialFuryActive(true);
        data.setCelestialMantleActive(true);
        ModNetworking.syncToPlayer(player);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncEntitySmokePayload(player.getId(), true, 0L));

        ServerLevel sl = player.serverLevel();
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        // Onda expansiva visual y sonora de activación
        sl.playSound(null, px, py, pz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.7f, 1.35f);
        sl.playSound(null, px, py, pz, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
        sl.playSound(null, px, py, pz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.45f);

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, px, py + 1.0, pz, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.CLOUD, px, py + 1.0, pz, 60, 0.9, 0.8, 0.9, 0.16);
        sl.sendParticles(ParticleTypes.END_ROD, px, py + 1.1, pz, 50, 0.8, 1.0, 0.8, 0.18);
        spawnShockwaveRing(sl, px, py + 0.15, pz, 4.5, 36, ParticleTypes.CLOUD);
        spawnShockwaveRing(sl, px, py + 0.35, pz, 3.5, 28, ParticleTypes.END_ROD);

        // Empuje inicial a enemigos cercanos al desatar el aura
        AABB pushArea = player.getBoundingBox().inflate(6.0, 3.0, 6.0);
        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, pushArea, e -> SpiritualSwordItem.isValidCombatTarget(player, e))) {
            Vec3 away = target.position().subtract(player.position());
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(1.0, 0.0, 0.0);
            Vec3 push = flat.normalize().scale(1.15).add(0.0, 0.45, 0.0);
            target.setDeltaMovement(push);
            target.hurtMarked = true;
        }

        player.displayClientMessage(Component.literal("§f§l✦ ¡FURIA PRIMORDIAL DESATADA! ✦ §e(Manto Celestial Activo)"), true);
        player.sendSystemMessage(Component.literal("§f§l[✦] §6¡FURIA PRIMORDIAL ACTIVA! §7Tus combos destructivos con la espada sellada están desatados."));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) return;

        long gameTime = player.level().getGameTime();

        // Verificar aterrizaje de Caída Destructiva (Combo 2)
        Long dropStart = AERIAL_DROP_START_TICK.get(player.getUUID());
        if (dropStart != null) {
            ServerLevel sl = player.serverLevel();
            sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.4, player.getZ(), 4, 0.25, 0.2, 0.25, 0.03);
            sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), 3, 0.2, 0.2, 0.2, 0.04);
            player.fallDistance = 0.0F;

            boolean landed = player.onGround() && (gameTime - dropStart) >= 2;
            boolean timedOut = (gameTime - dropStart) > 45;
            if (landed || timedOut) {
                AERIAL_DROP_START_TICK.remove(player.getUUID());
                if (landed) {
                    triggerAerialDropImpact(player, data);
                }
            }
        }

        if (!data.isPrimordialFuryActive()) return;

        // Drenar la barra gradualmente (modo temporal estilo Tachy)
        float nextCharge = data.getPrimordialFuryCharge() - DRAIN_PER_TICK;
        data.setPrimordialFuryCharge(nextCharge);
        player.fallDistance = 0.0F;

        // Buffs grosos durante Furia Primordial
        if (gameTime % 10 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0, false, false, true));
        }

        // Sincronizar cada 5 ticks para que la barra del HUD baje de forma fluida
        if (gameTime % 5 == 0) {
            ModNetworking.syncToPlayer(player);
        }

        // Cuando la barra llega a 0%, finalizar el modo y apagar el Manto Celestial
        if (data.getPrimordialFuryCharge() <= 0.0f) {
            data.setPrimordialFuryActive(false);
            data.setCelestialMantleActive(false);
            AERIAL_DROP_START_TICK.remove(player.getUUID());
            ModNetworking.syncToPlayer(player);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new SyncEntitySmokePayload(player.getId(), false, 0L));

            ServerLevel sl = player.serverLevel();
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.9f, 1.1f);
            sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.04);
            player.displayClientMessage(Component.literal("§7Tu Furia Primordial se apaga... Vuelve a pelear para cargar la barra."), true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!(player.getMainHandItem().getItem() instanceof SpiritualSwordItem)) return;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null && data.isPrimordialFuryActive()) {
            // +40% de daño en golpes con la espada sellada durante Furia Primordial
            event.setAmount(event.getAmount() * 1.40f);
        }
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!(player.getMainHandItem().getItem() instanceof SpiritualSwordItem)) return;
        if (!SpiritualSwordItem.isValidCombatTarget(player, event.getEntity())) return;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.isPrimordialFuryTrained()) return;

        ServerLevel sl = player.serverLevel();
        LivingEntity target = event.getEntity();

        if (data.isPrimordialFuryActive()) {
            // Chispas celestiales extra en cada golpe
            sl.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.3, 0.3, 0.3, 0.08);
            sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 4, 0.25, 0.25, 0.25, 0.04);
            return;
        }

        // Cargar barra de Furia Primordial al golpear con la espada sellada (con pequeño cooldown anti-spam)
        long now = player.level().getGameTime();
        long lastChargeTick = LAST_CHARGE_HIT_TICK.getOrDefault(player.getUUID(), 0L);
        if (now - lastChargeTick < CHARGE_HIT_COOLDOWN_TICKS) return;
        LAST_CHARGE_HIT_TICK.put(player.getUUID(), now);

        float before = data.getPrimordialFuryCharge();
        if (before < 100.0f) {
            data.addPrimordialFuryCharge(CHARGE_PER_HIT);
            notifyIfReady(player, before, data.getPrimordialFuryCharge());
            ModNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!(player.getMainHandItem().getItem() instanceof SpiritualSwordItem)) return;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.isPrimordialFuryTrained() || data.isPrimordialFuryActive()) return;

        float before = data.getPrimordialFuryCharge();
        if (before < 100.0f) {
            data.addPrimordialFuryCharge(CHARGE_PER_KILL);
            notifyIfReady(player, before, data.getPrimordialFuryCharge());
            ModNetworking.syncToPlayer(player);
        }
    }

    private static void notifyIfReady(ServerPlayer player, float before, float after) {
        if (before < 100.0f && after >= 100.0f) {
            ServerLevel sl = player.serverLevel();
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.4f);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);
            sl.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.1, player.getZ(), 20, 0.4, 0.5, 0.4, 0.08);
            player.displayClientMessage(Component.literal("§f§l✦ ¡FURIA PRIMORDIAL AL 100%! §ePresiona [Z] para desatar el Manto Celestial."), true);
        }
    }

    /**
     * Ejecuta uno de los 3 combos destructivos (100% impacto visual y daño en área, sin romper bloques).
     */
    public static void executeCombo(ServerPlayer player, int comboType) {
        if (player.isSpectator() || player.isDeadOrDying()) return;
        if (!(player.getMainHandItem().getItem() instanceof SpiritualSwordItem)) return;

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null || !data.isPrimordialFuryTrained() || !data.isPrimordialFuryActive()) return;

        long now = player.level().getGameTime();

        // Golpe normal potenciado (Onda Celestial): cooldown más corto e independiente
        if (comboType == PrimordialFuryComboPayload.COMBO_CELESTIAL_SLASH) {
            long lastSlash = LAST_SLASH_TICK.getOrDefault(player.getUUID(), 0L);
            if (now - lastSlash < SLASH_COOLDOWN_TICKS) return;
            LAST_SLASH_TICK.put(player.getUUID(), now);
            performCelestialSlash(player, data);
            return;
        }

        // Permitir encadenar Tajo Ascendente -> Caída Destructiva al instante en el aire
        boolean chainingAerialAfterAscending = (comboType == PrimordialFuryComboPayload.COMBO_AERIAL_DROP)
                && (now - LAST_ASCENDING_TICK.getOrDefault(player.getUUID(), -100L) <= 35L);

        long last = LAST_COMBO_TICK.getOrDefault(player.getUUID(), 0L);
        if (!chainingAerialAfterAscending && (now - last < COMBO_COOLDOWN_TICKS)) return;
        LAST_COMBO_TICK.put(player.getUUID(), now);

        switch (comboType) {
            case PrimordialFuryComboPayload.COMBO_SEISMIC_SLAM -> performSeismicSlam(player, data);
            case PrimordialFuryComboPayload.COMBO_AERIAL_DROP -> performAerialDropStart(player);
            case PrimordialFuryComboPayload.COMBO_ASCENDING_SLASH -> {
                LAST_ASCENDING_TICK.put(player.getUUID(), now);
                performAscendingSlash(player, data);
            }
        }
    }

    /**
     * GOLPE NORMAL POTENCIADO (Click Izq normal en Furia Primordial):
     * Desata un corte de presión celestial en media luna de 4.5m que atraviesa múltiples enemigos.
     */
    private static void performCelestialSlash(ServerPlayer player, PlayerRpgData data) {
        ServerLevel sl = player.serverLevel();
        Vec3 eye = player.getEyePosition().subtract(0.0, 0.25, 0.0);
        Vec3 look = player.getLookAngle().normalize();
        Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-4) right = new Vec3(1.0, 0.0, 0.0);
        right = right.normalize();

        // Sonido afilado de corte de aire celestial
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.35f);
        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.45f, 1.6f);

        // Arco en media luna proyectado hacia adelante (hasta 4.5 bloques)
        double tilt = (player.tickCount % 2 == 0) ? 0.35 : -0.35;
        for (int step = 1; step <= 3; step++) {
            double dist = 1.35 * step;
            for (int i = -4; i <= 4; i++) {
                double frac = i / 4.0;
                double spread = frac * (1.1 + step * 0.35);
                double arcBack = -(frac * frac) * 0.55;
                Vec3 p = eye.add(look.scale(dist + arcBack))
                        .add(right.scale(spread))
                        .add(0.0, frac * tilt, 0.0);
                sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.01);
                if (i % 2 == 0) {
                    sl.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.05, 0.04, 0.05, 0.015);
                }
            }
        }
        Vec3 sweepPos = eye.add(look.scale(2.4));
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, sweepPos.x, sweepPos.y, sweepPos.z, 2, 0.4, 0.2, 0.4, 0.0);

        // Daño en cono frontal (hasta 4.8 bloques de alcance)
        float dmg = computeComboDamage(player, data, 10.0f);
        Vec3 center = player.position().add(look.scale(2.6)).add(0.0, 1.0, 0.0);
        AABB area = new AABB(center.x - 3.0, center.y - 1.8, center.z - 3.0,
                center.x + 3.0, center.y + 2.0, center.z + 3.0);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area,
                e -> SpiritualSwordItem.isValidCombatTarget(player, e));
        for (LivingEntity target : targets) {
            Vec3 toTarget = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(eye);
            if (toTarget.lengthSqr() <= 24.0 && toTarget.normalize().dot(look) > 0.25) {
                target.hurt(player.damageSources().playerAttack(player), dmg);
                Vec3 push = new Vec3(look.x, 0.0, look.z).normalize().scale(0.45).add(0.0, 0.18, 0.0);
                target.setDeltaMovement(target.getDeltaMovement().add(push));
                target.hurtMarked = true;
            }
        }
    }

    private static float computeComboDamage(ServerPlayer player, PlayerRpgData data, float base) {
        int fort = data.getTotalStatPoints(player, StatType.FORTITUDE);
        int spi = data.getTotalStatPoints(player, StatType.SPIRIT);
        return base + (fort * 0.65f) + (spi * 0.45f);
    }

    /**
     * COMBO 1: Golpe Sísmico (Click Izq -> Click Izq -> Click Der en el suelo).
     */
    private static void performSeismicSlam(ServerPlayer player, PlayerRpgData data) {
        ServerLevel sl = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z).normalize();
        Vec3 center = player.position().add(flatLook.scale(2.2));

        BlockPos groundPos = BlockPos.containing(center.x, player.getY() - 0.5, center.z);
        BlockState groundState = sl.getBlockState(groundPos);
        if (groundState.isAir()) groundState = Blocks.STONE.defaultBlockState();

        // Efectos destructivos visuales (bloques/polvo volando, ondas expansivas, sonido pesado)
        sl.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.1f, 0.85f);
        sl.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 0.65f);
        sl.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.45f, 1.25f);

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 0.3, center.z, 1, 0, 0, 0, 0);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), center.x, center.y + 0.2, center.z, 75, 2.2, 0.4, 2.2, 0.25);
        spawnShockwaveRing(sl, center.x, center.y + 0.15, center.z, 2.5, 24, ParticleTypes.CLOUD);
        spawnShockwaveRing(sl, center.x, center.y + 0.2, center.z, 4.5, 36, ParticleTypes.END_ROD);
        spawnShockwaveRing(sl, center.x, center.y + 0.1, center.z, 5.5, 40, new BlockParticleOption(ParticleTypes.BLOCK, groundState));

        float dmg = computeComboDamage(player, data, 16.0f);
        AABB area = new AABB(center.x - 5.5, center.y - 2.0, center.z - 5.5, center.x + 5.5, center.y + 3.5, center.z + 5.5);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area, e -> SpiritualSwordItem.isValidCombatTarget(player, e));
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), dmg);
            Vec3 away = target.position().subtract(center);
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            if (flat.lengthSqr() < 1.0E-4) flat = flatLook;
            target.setDeltaMovement(flat.normalize().scale(1.1).add(0.0, 0.6, 0.0));
            target.hurtMarked = true;
        }

        player.swing(player.getUsedItemHand(), true);
        player.displayClientMessage(Component.literal("§f§l✦ ¡GOLPE SÍSMICO!"), true);
    }

    /**
     * COMBO 2 (Inicio): Caída Destructiva (Saltar / En el aire + Click Der).
     */
    private static void performAerialDropStart(ServerPlayer player) {
        ServerLevel sl = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 dive = new Vec3(look.x * 1.1, -1.45, look.z * 1.1);
        player.setDeltaMovement(dive);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;

        AERIAL_DROP_START_TICK.put(player.getUUID(), player.level().getGameTime());

        sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0f, 0.9f);
        sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.4, 0.4, 0.4, 0.12);
        player.swing(player.getUsedItemHand(), true);
    }

    /**
     * COMBO 2 (Impacto al aterrizar): Explosión meteórica visual sin romper bloques.
     */
    private static void triggerAerialDropImpact(ServerPlayer player, PlayerRpgData data) {
        ServerLevel sl = player.serverLevel();
        double cx = player.getX();
        double cy = player.getY();
        double cz = player.getZ();

        BlockPos groundPos = BlockPos.containing(cx, cy - 0.5, cz);
        BlockState groundState = sl.getBlockState(groundPos);
        if (groundState.isAir()) groundState = Blocks.STONE.defaultBlockState();

        sl.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.35f, 0.75f);
        sl.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.65f, 1.2f);
        sl.playSound(null, cx, cy, cz, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.55f, 1.15f);

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, cx, cy + 0.4, cz, 2, 0.6, 0.2, 0.6, 0);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, groundState), cx, cy + 0.25, cz, 110, 2.8, 0.6, 2.8, 0.35);
        spawnShockwaveRing(sl, cx, cy + 0.2, cz, 3.0, 28, ParticleTypes.CLOUD);
        spawnShockwaveRing(sl, cx, cy + 0.35, cz, 5.0, 42, ParticleTypes.END_ROD);
        spawnShockwaveRing(sl, cx, cy + 0.15, cz, 6.5, 48, new BlockParticleOption(ParticleTypes.BLOCK, groundState));

        float dmg = computeComboDamage(player, data, 22.0f);
        AABB area = new AABB(cx - 6.5, cy - 2.0, cz - 6.5, cx + 6.5, cy + 4.0, cz + 6.5);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area, e -> SpiritualSwordItem.isValidCombatTarget(player, e));
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), dmg);
            Vec3 away = target.position().subtract(cx, cy, cz);
            Vec3 flat = new Vec3(away.x, 0.0, away.z);
            if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(1.0, 0.0, 0.0);
            target.setDeltaMovement(flat.normalize().scale(1.35).add(0.0, 0.78, 0.0));
            target.hurtMarked = true;
        }

        player.displayClientMessage(Component.literal("§f§l✦ ¡CAÍDA DESTRUCTIVA!"), true);
    }

    /**
     * COMBO 3: Tajo Ascendente (Shift + Click Der).
     */
    private static void performAscendingSlash(ServerPlayer player, PlayerRpgData data) {
        ServerLevel sl = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z).normalize();
        Vec3 strikeCenter = player.position().add(flatLook.scale(2.3)).add(0.0, 1.0, 0.0);

        sl.playSound(null, strikeCenter.x, strikeCenter.y, strikeCenter.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.85f);
        sl.playSound(null, strikeCenter.x, strikeCenter.y, strikeCenter.z, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 1.25f);

        // Estela ascendente de tajo
        for (int i = 0; i < 16; i++) {
            double h = i * 0.22;
            sl.sendParticles(ParticleTypes.END_ROD, strikeCenter.x, player.getY() + 0.3 + h, strikeCenter.z, 2, 0.35, 0.05, 0.35, 0.04);
            sl.sendParticles(ParticleTypes.CLOUD, strikeCenter.x, player.getY() + 0.2 + h, strikeCenter.z, 2, 0.4, 0.05, 0.4, 0.03);
        }
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, strikeCenter.x, strikeCenter.y + 0.5, strikeCenter.z, 3, 0.5, 0.6, 0.5, 0.0);

        float dmg = computeComboDamage(player, data, 14.0f);
        AABB area = new AABB(strikeCenter.x - 3.5, player.getY() - 1.0, strikeCenter.z - 3.5,
                strikeCenter.x + 3.5, player.getY() + 4.5, strikeCenter.z + 3.5);
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class, area, e -> SpiritualSwordItem.isValidCombatTarget(player, e));
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), dmg);
            target.setDeltaMovement(flatLook.scale(0.35).add(0.0, 0.98, 0.0));
            target.hurtMarked = true;
        }

        // Impulsar al jugador hacia arriba para encadenar con Caída Destructiva
        player.setDeltaMovement(flatLook.scale(0.35).add(0.0, 0.78, 0.0));
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        player.swing(player.getUsedItemHand(), true);
        player.displayClientMessage(Component.literal("§f§l✦ ¡TAJO ASCENDENTE!"), true);
    }

    private static <T extends net.minecraft.core.particles.ParticleOptions> void spawnShockwaveRing(
            ServerLevel sl, double cx, double cy, double cz, double radius, int points, T particle) {
        for (int i = 0; i < points; i++) {
            double ang = (Math.PI * 2.0 * i) / points;
            double x = cx + Math.cos(ang) * radius;
            double z = cz + Math.sin(ang) * radius;
            sl.sendParticles(particle, x, cy, z, 1, 0.08, 0.05, 0.08, 0.04);
        }
    }
}
