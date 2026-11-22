package com.pablo.tenseicraft.entities;

import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.entities.ai.RpgCombatDuelist;
import com.pablo.tenseicraft.entities.ai.RpgDuelCombatGoal;
import com.pablo.tenseicraft.events.ModAttributesHandler;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.network.ModNetworking;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Jefe de Prueba: Espíritu de la Espada
 * Un avatar espectral que porta la Espada Espiritual liberada y replica
 * fielmente las estadísticas (HP, Daño, Velocidad, Crítico) y habilidades (Dash, Doble Salto) del invocador.
 */
public class SwordSpiritEntity extends Monster implements RpgCombatDuelist {

    private static final EntityDataAccessor<Integer> DATA_COMBAT_COMBO =
            SynchedEntityData.defineId(SwordSpiritEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_PARRYING =
            SynchedEntityData.defineId(SwordSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ELEMENT =
            SynchedEntityData.defineId(SwordSpiritEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STYLE =
            SynchedEntityData.defineId(SwordSpiritEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("§5§l[Jefe] §dEspíritu de la Espada"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(true);

    private UUID summonerUUID;
    private String summonerName = "";
    private double critChance = 10.0;
    private double critMultiplier = 1.5;
    private boolean hasDash = false;
    private boolean hasDoubleJump = false;

    private long lastAttackGameTime = 0L;
    private int dashCooldown = 80;
    private int abilityCooldown = 120;
    private int despawnTimer = 3600; // 3 minutos si el jugador se marcha

    public SwordSpiritEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_COMBAT_COMBO, 0);
        builder.define(DATA_IS_PARRYING, false);
        builder.define(DATA_ELEMENT, 0);
        builder.define(DATA_STYLE, 1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RpgDuelCombatGoal<>(this, 1.30D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    /**
     * Clona fielmente las estadísticas y habilidades del jugador convocante.
     */
    public void setupFromSummoner(ServerPlayer summoner, ItemStack playerSword) {
        this.summonerUUID = summoner.getUUID();
        this.summonerName = summoner.getScoreboardName();

        // 1. Replicar Vida Máxima
        double summonerMaxHp = summoner.getMaxHealth();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(40.0, summonerMaxHp));
        this.setHealth((float) Math.max(40.0, summonerMaxHp));

        // 2. Replicar Daño de Ataque
        double summonerAtk = summoner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(6.0, summonerAtk));

        // 3. Replicar Velocidad de Movimiento
        double summonerSpeed = summoner.getAttributeValue(Attributes.MOVEMENT_SPEED);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(Math.max(0.32, summonerSpeed * 1.05));

        // 4. Replicar Armadura y Resistencia
        this.getAttribute(Attributes.ARMOR).setBaseValue(summoner.getAttributeValue(Attributes.ARMOR));
        this.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(summoner.getAttributeValue(Attributes.ARMOR_TOUGHNESS));

        // 5. Estadísticas de RPG y Habilidades Físicas
        PlayerRpgData data = summoner.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            this.critChance = data.getCritChance(summoner);
            this.critMultiplier = data.getCritDamageMultiplier(this.critChance);
            this.hasDash = data.hasSkill("agility_dash");
            this.hasDoubleJump = data.hasSkill("agility_double_jump");
        }

        // 6. Equipar la Espada Espiritual Liberada idéntica a la del invocador
        SpiritualSwordItem.BladeStyle style = SpiritualSwordItem.getBladeStyle(playerSword);
        SpiritualSwordItem.BladeElement element = SpiritualSwordItem.getBladeElement(playerSword);
        SpiritualSwordItem.BladeRelease release = SpiritualSwordItem.getBladeRelease(playerSword);
        SpiritualSwordItem.BladeResonance resonance = SpiritualSwordItem.getBladeResonance(playerSword);
        int variant = SpiritualSwordItem.getBladeVariant(playerSword);

        ItemStack spiritSword = SpiritualSwordItem.createCustomSword(summoner, style, element, release, resonance, variant);
        SpiritualSwordItem.setFirstRelease(spiritSword, true); // En manos del espíritu siempre está liberada
        this.setItemSlot(EquipmentSlot.MAINHAND, spiritSword);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        // Copiar piezas de armadura visualmente para un duelo contra la sombra
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmor()) {
                ItemStack armorPiece = summoner.getItemBySlot(slot);
                if (!armorPiece.isEmpty()) {
                    this.setItemSlot(slot, armorPiece.copy());
                    this.setDropChance(slot, 0.0F);
                }
            }
        }

        this.entityData.set(DATA_STYLE, style.getCustomModelData());
        this.entityData.set(DATA_ELEMENT, element.ordinal());

        this.bossEvent.setName(Component.literal("§5§l[Jefe] §dEspíritu de la Espada de " + this.summonerName));
        this.setTarget(summoner);
    }

    public SpiritualSwordItem.BladeElement getBladeElement() {
        int ord = this.entityData.get(DATA_ELEMENT);
        SpiritualSwordItem.BladeElement[] elements = SpiritualSwordItem.BladeElement.values();
        return (ord >= 0 && ord < elements.length) ? elements[ord] : SpiritualSwordItem.BladeElement.IGNIS;
    }

    public SpiritualSwordItem.BladeStyle getBladeStyle() {
        int cmd = this.entityData.get(DATA_STYLE);
        for (SpiritualSwordItem.BladeStyle style : SpiritualSwordItem.BladeStyle.values()) {
            if (style.getCustomModelData() == cmd) return style;
        }
        return SpiritualSwordItem.BladeStyle.FRENZY;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide()) {
            // Partículas etéreas alrededor del espíritu en cliente
            if (this.random.nextFloat() < 0.35f) {
                this.level().addParticle(ParticleTypes.PORTAL,
                        this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5),
                        (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2, (this.random.nextDouble() - 0.5) * 0.2);
            }
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();

        if (!this.isAlive() || this.isRemoved()) {
            this.bossEvent.removeAllPlayers();
            return;
        }

        // 1. Actualizar barra de jefe
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        updateBossBarPlayers();

        // 2. Comportamiento de Despawn si el invocador murió o se fue lejos
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > 2500.0) {
            this.despawnTimer--;
            if (this.despawnTimer <= 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 40, 0.4, 0.6, 0.4, 0.15);
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.8F);
                this.discard();
                return;
            }
        } else {
            this.despawnTimer = 3600;
        }

        // 3. Aura Elemental continua alrededor de la espada
        if (this.tickCount % 4 == 0) {
            SpiritualSwordItem.spawnHeldElementalAura(serverLevel, this, getBladeElement());
        }

        // 4. Mecánica de DASH si el jugador tiene la habilidad desbloqueada
        if (this.dashCooldown > 0) this.dashCooldown--;
        if (this.hasDash && this.dashCooldown <= 0 && target != null) {
            double distSqr = this.distanceToSqr(target);
            if (distSqr >= 16.0 && distSqr <= 196.0 && this.onGround()) {
                performSpiritDash(serverLevel, target);
                this.dashCooldown = 90 + this.random.nextInt(35);
            }
        }

        // 5. Mecánica de DOBLE SALTO acrobático si el jugador lo tiene
        if (this.hasDoubleJump && target != null && this.distanceToSqr(target) < 16.0 && this.onGround()) {
            if (this.random.nextFloat() < 0.025f) {
                this.setDeltaMovement(this.getDeltaMovement().x, 0.62, this.getDeltaMovement().z);
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 0.8f, 1.6f);
                serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 10, 0.25, 0.1, 0.25, 0.05);
            }
        }

        // 6. Lanzamiento de Habilidad de Primera Liberación cada 6-8 segundos
        if (this.abilityCooldown > 0) this.abilityCooldown--;
        if (this.abilityCooldown <= 0 && target != null) {
            performReleaseAbility(serverLevel, target);
            this.abilityCooldown = 130 + this.random.nextInt(40);
        }
    }

    private void performSpiritDash(ServerLevel level, LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(toTarget.x * 1.35, 0.25, toTarget.z * 1.35);
        this.hurtMarked = true;

        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 1.4F);
        level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.7F, 1.6F);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.8, this.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
    }

    private void performReleaseAbility(ServerLevel level, LivingEntity target) {
        ItemStack held = this.getMainHandItem();
        SpiritualSwordItem.BladeRelease release = SpiritualSwordItem.getBladeRelease(held);
        SpiritualSwordItem.BladeElement element = getBladeElement();

        switch (release) {
            case SLASH -> {
                // Corte de luna elemental hacia el jugador
                Vec3 look = this.getLookAngle().normalize();
                Vec3 start = this.getEyePosition();
                level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2f, 0.8f);
                level.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.HOSTILE, 0.9f, 1.2f);

                for (double d = 1.0; d <= 8.0; d += 0.8) {
                    Vec3 pt = start.add(look.scale(d));
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                    SpiritualSwordItem.spawnHeldElementalAura(level, this, element);
                }

                if (this.distanceToSqr(target) <= 64.0) {
                    target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.25f);
                    SpiritualSwordItem.applyElementalStatusEffect(level, this, target, element, 6.0f);
                    SpiritualSwordItem.spawnElementalImpactBurst(level, target, element);
                }
            }
            case VORTEX -> {
                // Vórtice elemental que daña y atrae
                level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.4f);
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.5),
                        e -> e != this && e.isAlive())) {
                    nearby.hurt(this.damageSources().mobAttack(this), 6.5f);
                    SpiritualSwordItem.applyElementalStatusEffect(level, this, nearby, element, 6.5f);
                    SpiritualSwordItem.spawnElementalImpactBurst(level, nearby, element);
                }
            }
            case STEP -> {
                // Teletransporte sombrío detrás del objetivo
                Vec3 behind = target.position().subtract(target.getLookAngle().scale(1.5));
                level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                this.teleportTo(behind.x, target.getY(), behind.z);
                level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.3f);
                this.doHurtTarget(target);
            }
            case OVERDRIVE -> {
                // Trance potenciado con resistencia y fuerza
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 0, false, false, true));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0, false, false, true));
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1, false, false, true));
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.0, this.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
                level.playSound(null, this.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 0.8f, 1.4f);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float baseDmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);

        // Replicar Golpe Crítico del jugador
        boolean isCrit = this.random.nextDouble() * 100.0 < this.critChance;
        if (isCrit) {
            baseDmg *= (float) this.critMultiplier;
        }

        boolean success = target.hurt(this.damageSources().mobAttack(this), baseDmg);
        if (success && !this.level().isClientSide() && this.level() instanceof ServerLevel sl && target instanceof LivingEntity living) {
            SpiritualSwordItem.BladeElement element = getBladeElement();
            SpiritualSwordItem.spawnElementalImpactBurst(sl, living, element);
            SpiritualSwordItem.applyElementalStatusEffect(sl, this, living, element, baseDmg);

            if (isCrit) {
                sl.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY() + 1.0, living.getZ(), 20, 0.3, 0.4, 0.3, 0.15);
                sl.playSound(null, living.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.0F, 1.1F);
            }
        }
        return success;
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            this.bossEvent.removeAllPlayers();

            ServerPlayer summoner = getSummonerPlayer(serverLevel);
            if (summoner != null && summoner.isAlive()) {
                // 1. Desbloquear permanentemente la Primera Liberación en la espada
                ItemStack sword = findSpiritualSword(summoner);
                if (sword != null && !sword.isEmpty()) {
                    SpiritualSwordItem.setFirstReleaseUnlocked(sword, true);
                    SpiritualSwordItem.setFirstRelease(sword, true); // Se activa de inmediato
                    SpiritualSwordItem.syncAwakenedSpell(sword);
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                            summoner,
                            new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
                    );
                }

                // 2. Desbloquear en los datos del jugador
                PlayerRpgData data = summoner.getData(ModAttachments.PLAYER_RPG_DATA);
                if (data != null) {
                    data.setFirstReleaseUnlocked(true);
                    ModNetworking.syncToPlayer(summoner);
                }

                // 3. Recalcular estadísticas del jugador (+2 en todo por tener la espada liberada)
                ModAttributesHandler.updatePlayerAttributes(summoner);

                // 4. Mensajes ceremoniales y fanfarria
                summoner.sendSystemMessage(Component.empty());
                summoner.sendSystemMessage(Component.literal("§6§l✦━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━✦"));
                summoner.sendSystemMessage(Component.literal("§a§l  ¡HAS SUPERADO LA PRUEBA DEL ESPÍRITU DE LA ESPADA!"));
                summoner.sendSystemMessage(Component.literal("§f  Has demostrado tu valía y tu hoja ha despertado su poder latente."));
                summoner.sendSystemMessage(Component.literal("§d  ✦ ¡Primera Liberación Desbloqueada! ✦"));
                summoner.sendSystemMessage(Component.literal("§e  Presiona §6[Z] §epara activarla o desactivarla (+2 a todas tus estadísticas)."));
                summoner.sendSystemMessage(Component.literal("§6§l✦━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━✦"));
                summoner.sendSystemMessage(Component.empty());

                serverLevel.playSound(null, summoner.getX(), summoner.getY(), summoner.getZ(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
                serverLevel.playSound(null, summoner.getX(), summoner.getY(), summoner.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                serverLevel.playSound(null, summoner.getX(), summoner.getY(), summoner.getZ(),
                        SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.3f);

                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        summoner.getX(), summoner.getY() + 1.0, summoner.getZ(), 80, 0.6, 0.8, 0.6, 0.25);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    private ServerPlayer getSummonerPlayer(ServerLevel level) {
        if (this.summonerUUID != null) {
            return (ServerPlayer) level.getPlayerByUUID(this.summonerUUID);
        }
        return null;
    }

    private static ItemStack findSpiritualSword(ServerPlayer player) {
        if (player == null) return null;
        if (player.getMainHandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getOffhandItem();
        }
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof SpiritualSwordItem) {
                return item;
            }
        }
        return null;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.isAlive() && !this.isRemoved()) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    private void updateBossBarPlayers() {
        if (!this.isAlive() || this.isRemoved()) {
            this.bossEvent.removeAllPlayers();
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            AABB box = this.getBoundingBox().inflate(32.0);
            List<ServerPlayer> nearby = serverLevel.getEntitiesOfClass(ServerPlayer.class, box);
            for (ServerPlayer player : serverLevel.players()) {
                if (nearby.contains(player)) {
                    this.bossEvent.addPlayer(player);
                } else {
                    this.bossEvent.removePlayer(player);
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.summonerUUID != null) {
            tag.putUUID("SummonerUUID", this.summonerUUID);
        }
        tag.putString("SummonerName", this.summonerName);
        tag.putDouble("CritChance", this.critChance);
        tag.putDouble("CritMultiplier", this.critMultiplier);
        tag.putBoolean("HasDash", this.hasDash);
        tag.putBoolean("HasDoubleJump", this.hasDoubleJump);
        tag.putInt("SpiritElement", this.entityData.get(DATA_ELEMENT));
        tag.putInt("SpiritStyle", this.entityData.get(DATA_STYLE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("SummonerUUID")) {
            this.summonerUUID = tag.getUUID("SummonerUUID");
        }
        if (tag.contains("SummonerName")) {
            this.summonerName = tag.getString("SummonerName");
            this.bossEvent.setName(Component.literal("§5§l[Jefe] §dEspíritu de la Espada de " + this.summonerName));
        }
        if (tag.contains("CritChance")) this.critChance = tag.getDouble("CritChance");
        if (tag.contains("CritMultiplier")) this.critMultiplier = tag.getDouble("CritMultiplier");
        if (tag.contains("HasDash")) this.hasDash = tag.getBoolean("HasDash");
        if (tag.contains("HasDoubleJump")) this.hasDoubleJump = tag.getBoolean("HasDoubleJump");
        if (tag.contains("SpiritElement")) this.entityData.set(DATA_ELEMENT, tag.getInt("SpiritElement"));
        if (tag.contains("SpiritStyle")) this.entityData.set(DATA_STYLE, tag.getInt("SpiritStyle"));
    }

    // =========================================================================
    // RpgCombatDuelist Implementation
    // =========================================================================
    @Override
    public int getCombatCombo() {
        return this.entityData.get(DATA_COMBAT_COMBO);
    }

    @Override
    public void setCombatCombo(int combo) {
        this.entityData.set(DATA_COMBAT_COMBO, combo);
    }

    @Override
    public boolean isBlockingOrParrying() {
        return this.entityData.get(DATA_IS_PARRYING);
    }

    @Override
    public void setBlockingOrParrying(boolean parrying) {
        this.entityData.set(DATA_IS_PARRYING, parrying);
    }

    @Override
    public long getLastAttackGameTime() {
        return this.lastAttackGameTime;
    }

    @Override
    public void setLastAttackGameTime(long time) {
        this.lastAttackGameTime = time;
    }

    @Override
    public net.minecraft.world.entity.PathfinderMob getDuelistMob() {
        return this;
    }
}
