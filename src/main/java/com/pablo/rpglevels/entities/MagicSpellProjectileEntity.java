package com.pablo.rpglevels.entities;

import com.pablo.rpglevels.items.SpiritualSwordItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class MagicSpellProjectileEntity extends ThrowableProjectile implements ItemSupplier {

    public enum SpellType {
        LOW_HEALING,
        LOW_PARALIZE,
        ELEMENTAL_BALL
    }

    private static final EntityDataAccessor<Integer> TYPE_ORDINAL = SynchedEntityData
            .defineId(MagicSpellProjectileEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ELEMENT_NAME = SynchedEntityData
            .defineId(MagicSpellProjectileEntity.class, EntityDataSerializers.STRING);

    private float spellPower = 5.0f;
    private int ticksInAir = 0;

    public MagicSpellProjectileEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
    }

    public MagicSpellProjectileEntity(Level level, LivingEntity shooter, SpellType type, String element, float power) {
        super(ModEntities.MAGIC_SPELL_PROJECTILE.get(), shooter, level);
        this.entityData.set(TYPE_ORDINAL, type.ordinal());
        this.entityData.set(ELEMENT_NAME, element != null ? element : "none");
        this.spellPower = power;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE_ORDINAL, SpellType.ELEMENTAL_BALL.ordinal());
        builder.define(ELEMENT_NAME, "none");
    }

    public SpellType getSpellType() {
        int ord = this.entityData.get(TYPE_ORDINAL);
        SpellType[] values = SpellType.values();
        return (ord >= 0 && ord < values.length) ? values[ord] : SpellType.ELEMENTAL_BALL;
    }

    public String getElementName() {
        return this.entityData.get(ELEMENT_NAME);
    }

    @Override
    public ItemStack getItem() {
        SpellType type = getSpellType();
        return switch (type) {
            case LOW_HEALING -> new ItemStack(Items.GLISTERING_MELON_SLICE);
            case LOW_PARALIZE -> new ItemStack(Items.LIGHTNING_ROD);
            case ELEMENTAL_BALL -> {
                String elem = getElementName().toLowerCase();
                yield switch (elem) {
                    case "ignis", "fuego" -> new ItemStack(Items.FIRE_CHARGE);
                    case "glacies", "hielo" -> new ItemStack(Items.SNOWBALL);
                    case "fulgur", "rayo" -> new ItemStack(Items.AMETHYST_SHARD);
                    case "umbra", "sombra" -> new ItemStack(Items.ECHO_SHARD);
                    default -> new ItemStack(Items.FIRE_CHARGE);
                };
            }
        };
    }

    @Override
    public void tick() {
        super.tick();
        ticksInAir++;

        if (this.level().isClientSide()) {
            Vec3 pos = this.position();
            SpellType type = getSpellType();
            switch (type) {
                case LOW_HEALING -> {
                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y, pos.z, 0, 0.02, 0);
                    if (this.random.nextFloat() < 0.25f) {
                        this.level().addParticle(ParticleTypes.HEART, pos.x, pos.y + 0.1, pos.z, 0, 0.05, 0);
                    }
                }
                case LOW_PARALIZE -> {
                    this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z,
                            (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.1,
                            (this.random.nextDouble() - 0.5) * 0.1);
                    if (this.random.nextFloat() < 0.2f) {
                        this.level().addParticle(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 0, 0, 0);
                    }
                }
                case ELEMENTAL_BALL -> {
                    String elem = getElementName().toLowerCase();
                    for (int i = 0; i < 3; i++) {
                        double ox = (this.random.nextDouble() - 0.5) * 0.15;
                        double oy = (this.random.nextDouble() - 0.5) * 0.15;
                        double oz = (this.random.nextDouble() - 0.5) * 0.15;
                        switch (elem) {
                            case "ignis", "fuego" -> {
                                this.level().addParticle(ParticleTypes.FLAME, pos.x + ox, pos.y + oy, pos.z + oz, 0, 0.01, 0);
                                this.level().addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 0, 0.01, 0);
                            }
                            case "glacies", "hielo" -> {
                                this.level().addParticle(ParticleTypes.SNOWFLAKE, pos.x + ox, pos.y + oy, pos.z + oz, 0, 0.01, 0);
                                this.level().addParticle(ParticleTypes.ITEM_SNOWBALL, pos.x, pos.y, pos.z, 0, 0.01, 0);
                            }
                            case "fulgur", "rayo" -> {
                                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, pos.x + ox, pos.y + oy, pos.z + oz, ox * 0.2, oy * 0.2, oz * 0.2);
                                this.level().addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 0, 0.01, 0);
                            }
                            case "umbra", "sombra" -> {
                                this.level().addParticle(ParticleTypes.PORTAL, pos.x + ox, pos.y + oy, pos.z + oz, ox, oy, oz);
                                this.level().addParticle(ParticleTypes.SCULK_SOUL, pos.x, pos.y, pos.z, 0, 0.01, 0);
                            }
                            default -> {
                                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x + ox, pos.y + oy, pos.z + oz, 0, 0.01, 0);
                                this.level().addParticle(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 0, 0.02, 0);
                            }
                        }
                    }
                }
            }
        }

        // Dissipate if in air for more than 4 seconds without hit
        if (!this.level().isClientSide() && ticksInAir > 80) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide())
            return;

        if (result.getEntity() instanceof LivingEntity target) {
            // Cannot hit shooter
            if (this.getOwner() != null && target.is(this.getOwner())) {
                return;
            }

            SpellType type = getSpellType();

            // No Friendly Fire: No dañar ni paralizar a miembros de la party ni compañeros aliados
            if (type != SpellType.LOW_HEALING && com.pablo.rpglevels.party.PartyManager.isAllied(this.getOwner(), target)) {
                return;
            }

            ServerLevel serverLevel = (ServerLevel) this.level();

            switch (type) {
                case LOW_HEALING -> {
                    // Solo cura a otros jugadores, NPCs aliados contratados o mascotas domesticadas (NO a uno mismo ni a enemigos)
                    if (!isValidHealingTarget(this.getOwner(), target)) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE, target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.4f);
                        this.discard();
                        return;
                    }

                    target.heal(this.spellPower);
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1.0,
                            target.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
                    serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.2, target.getZ(), 8,
                            0.3, 0.4, 0.3, 0.1);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.3f);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_LEVELUP,
                            SoundSource.PLAYERS, 0.7f, 1.8f);

                    if (this.getOwner() instanceof net.minecraft.server.level.ServerPlayer shooter) {
                        shooter.displayClientMessage(
                                net.minecraft.network.chat.Component.literal("§a✦ ¡Low Healing sanó a §f"
                                        + target.getName().getString() + " §a(+" + (int) this.spellPower + " HP)!"),
                                true);
                    }
                }
                case LOW_PARALIZE -> {
                    // Paralyze target: base 3s (60 ticks) + scaling from spellPower (Spirit) up to
                    // 7s
                    int durationTicks = Math.min(140, 60 + (int) this.spellPower);
                    target.addEffect(
                            new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 6, false, true, true));
                    target.addEffect(
                            new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 4, false, true, true));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 4, false, true, true));
                    target.setDeltaMovement(0, 0, 0);

                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0,
                            target.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_STEP,
                            SoundSource.PLAYERS, 1.2f, 0.8f);

                    if (this.getOwner() instanceof net.minecraft.server.level.ServerPlayer shooter) {
                        float sec = durationTicks / 20.0f;
                        shooter.displayClientMessage(
                                net.minecraft.network.chat.Component
                                        .literal("§e✦ ¡Low Paralize paralizó a §f" + target.getName().getString()
                                                + " §epor " + String.format(java.util.Locale.ROOT, "%.1f", sec) + "s!"),
                                true);
                    }
                }
                case ELEMENTAL_BALL -> {
                    triggerElementalExplosion(serverLevel, result.getLocation(), target);
                }
            }

            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            if (getSpellType() == SpellType.ELEMENTAL_BALL) {
                triggerElementalExplosion(serverLevel, result.getLocation(), null);
            }
            this.discard();
        }
    }

    private void triggerElementalExplosion(ServerLevel serverLevel, Vec3 hitPos, LivingEntity directTarget) {
        String elem = getElementName().toLowerCase();

        // 1. Partículas de detonación central
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, hitPos.x, hitPos.y + 0.3, hitPos.z, 1, 0, 0, 0, 0);

        switch (elem) {
            case "ignis", "fuego" -> {
                serverLevel.sendParticles(ParticleTypes.FLAME, hitPos.x, hitPos.y + 0.5, hitPos.z, 30, 0.5, 0.5, 0.5, 0.1);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2f, 1.0f);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
            }
            case "glacies", "hielo" -> {
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, hitPos.x, hitPos.y + 0.5, hitPos.z, 35, 0.6, 0.5, 0.6, 0.05);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.4f);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.5f);
            }
            case "fulgur", "rayo" -> {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, hitPos.x, hitPos.y + 0.5, hitPos.z, 35, 0.6, 0.6, 0.6, 0.15);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.9f, 1.6f);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
            }
            case "umbra", "sombra" -> {
                serverLevel.sendParticles(ParticleTypes.SCULK_SOUL, hitPos.x, hitPos.y + 0.5, hitPos.z, 25, 0.5, 0.5, 0.5, 0.05);
                serverLevel.sendParticles(ParticleTypes.PORTAL, hitPos.x, hitPos.y + 0.5, hitPos.z, 30, 0.5, 0.5, 0.5, 0.2);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.8f);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            default -> {
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, hitPos.x, hitPos.y + 0.5, hitPos.z, 25, 0.5, 0.5, 0.5, 0.1);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.2f);
            }
        }

        // 2. Daño al objetivo directo si lo hubo
        if (directTarget != null && directTarget.isAlive()) {
            directTarget.hurt(this.damageSources().thrown(this, this.getOwner()), this.spellPower);
            applyElementDebuff(directTarget, elem);
        }

        // 3. Daño residual en área (radio 3.5 bloques)
        net.minecraft.world.phys.AABB blastArea = new net.minecraft.world.phys.AABB(
                hitPos.x - 3.5, hitPos.y - 2.5, hitPos.z - 3.5,
                hitPos.x + 3.5, hitPos.y + 2.5, hitPos.z + 3.5
        );
        java.util.List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, blastArea,
                e -> e != this.getOwner() && e != directTarget && SpiritualSwordItem.isValidCombatTarget(this.getOwner(), e));

        for (LivingEntity splashTarget : nearby) {
            splashTarget.hurt(this.damageSources().thrown(this, this.getOwner()), this.spellPower * 0.7f);
            applyElementDebuff(splashTarget, elem);
        }
    }

    private void applyElementDebuff(LivingEntity target, String elem) {
        switch (elem) {
            case "ignis", "fuego" -> target.igniteForSeconds(4);
            case "glacies", "hielo" -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                target.setTicksFrozen(120);
            }
            case "fulgur", "rayo" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2));
            case "umbra", "sombra" -> {
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("SpellType", this.entityData.get(TYPE_ORDINAL));
        tag.putString("Element", this.entityData.get(ELEMENT_NAME));
        tag.putFloat("SpellPower", this.spellPower);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SpellType")) {
            this.entityData.set(TYPE_ORDINAL, tag.getInt("SpellType"));
        }
        if (tag.contains("Element")) {
            this.entityData.set(ELEMENT_NAME, tag.getString("Element"));
        }
        if (tag.contains("SpellPower")) {
            this.spellPower = tag.getFloat("SpellPower");
        }
    }

    public static boolean isValidHealingTarget(net.minecraft.world.entity.Entity shooter, LivingEntity target) {
        if (target == null || !target.isAlive()) return false;
        if (shooter != null && target.is(shooter)) return false; // Jamás nos cura a nosotros mismos

        // Jamás cura a enemigos o criaturas hostiles
        if (target instanceof net.minecraft.world.entity.monster.Enemy
                || target instanceof net.minecraft.world.entity.monster.Monster
                || target.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER
                || target instanceof com.pablo.rpglevels.entities.BanditEntity) {
            return false;
        }

        // Objetivos válidos:
        // 1. Otros jugadores
        if (target instanceof net.minecraft.world.entity.player.Player) return true;

        // 2. NPCs contratados (Aventureros y Aldeanas/Ciudadanas)
        if (target instanceof AdventurerNpcEntity) return true;
        if (target instanceof TownswomanNpcEntity) return true;

        // 3. Mascotas domesticadas (lobos, gatos, caballos, etc.)
        if (target instanceof net.minecraft.world.entity.TamableAnimal tamed && tamed.isTame()) return true;
        if (target instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()) return true;

        // 4. Aliados de la party
        if (shooter != null && com.pablo.rpglevels.party.PartyManager.isAllied(shooter, target)) return true;

        return false;
    }
}
