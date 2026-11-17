package com.pablo.rpglevels.events;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.items.AncientRingItem;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import com.pablo.rpglevels.entities.AdventurerNpcEntity;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ThreatLevel;
import java.util.List;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class CombatEvents {

    /**
     * 1. EVASION & DEFLECTION
     * - I-Frames: Complete damage immunity during 360 Dash/Dodge
     * - Fortitude: Chance to deflect incoming projectiles
     */
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }

        // --- NO FUEGO AMIGO PARA MIEMBROS DE LA PARTY Y COMPAÑEROS ---
        DamageSource damageSource = event.getSource();
        Entity attackerEntity = damageSource.getEntity();
        if (attackerEntity != null && com.pablo.rpglevels.party.PartyManager.isAllied(attackerEntity, target)) {
            event.setCanceled(true);
            return;
        }
        Entity directEntity = damageSource.getDirectEntity();
        if (directEntity != null && com.pablo.rpglevels.party.PartyManager.isAllied(directEntity, target)) {
            event.setCanceled(true);
            return;
        }

        if (!(target instanceof Player player)) {
            return;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return;
        }

        // --- I-FRAME EVASION CHECK (Action Game Dodge) ---
        long currentTick = player.level().getGameTime();
        if (data.isDashing(currentTick)) {
            event.setCanceled(true);

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SWEEP_ATTACK,
                        player.getX(), player.getY() + 0.8, player.getZ(),
                        1, 0.0, 0.0, 0.0, 0.0
                );
                serverLevel.sendParticles(
                        ParticleTypes.POOF,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        4, 0.2, 0.2, 0.2, 0.05
                );
                serverLevel.playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_NODAMAGE,
                        SoundSource.PLAYERS,
                        1.0f,
                        1.8f
                );
            }

            player.displayClientMessage(
                    Component.translatable("rpglevels.combat.perfect_dodge").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    true
            );
            return;
        }

        // --- PERFECT PARRY CHECK (Shield Parry or Sword Block Parry ONLY; weapon clash removed) ---
        // Shield or Sword block parry: player raised shield or sword within the last 8 ticks (0.4s)
        boolean isUsingShield = player.isBlocking() || (player.isUsingItem() && (player.getUseItem().is(net.minecraft.world.item.Items.SHIELD) || player.getUseItem().getItem() instanceof SpiritualSwordItem));
        boolean shieldParry = isUsingShield && player.getTicksUsingItem() <= 8;

        Entity attacker = event.getSource().getEntity();
        boolean facingAttacker = true;
        if (attacker != null) {
            Vec3 playerLook = player.getLookAngle();
            Vec3 toAttacker = attacker.position().subtract(player.position()).normalize();
            facingAttacker = playerLook.dot(toAttacker) > -0.2; // Frontal arc
        }

        if (shieldParry && facingAttacker) {
            // PROBABILIDAD DE PARRY BASADA EN DESTREZA:
            // Base 15% + 1.2% por punto de Destreza, tope máximo del 80%
            int dex = (data != null) ? data.getTotalStatPoints(player, StatType.DEXTERITY) : 0;
            float parryChance = Math.min(0.80f, 0.15f + (dex * 0.012f));
            boolean parrySuccess = player.getRandom().nextFloat() < parryChance;

            if (parrySuccess) {
                event.setCanceled(true);

                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, 1.6f);
                    serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8f, 1.8f);
                    serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                            SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0f, 1.5f);

                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            16, 0.3, 0.3, 0.3, 0.15);
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            10, 0.2, 0.2, 0.2, 0.1);
                    serverLevel.sendParticles(ParticleTypes.FLASH,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            1, 0, 0, 0, 0);
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            player.getX(), player.getY() + 0.8, player.getZ(),
                            1, 0, 0, 0, 0);
                }

                player.displayClientMessage(
                        Component.translatable("rpglevels.combat.perfect_parry"),
                        true
                );

                // Rotura de Postura (Stagger) on attacker for 2 seconds (40 ticks)
                if (attacker instanceof LivingEntity livingAttacker) {
                    livingAttacker.getPersistentData().putLong("RpgStaggerUntilTick", currentTick + 40);
                    livingAttacker.setDeltaMovement(0, 0, 0);
                    livingAttacker.hurtMarked = true;
                    livingAttacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false, false));
                    livingAttacker.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 9, false, false, false));
                }
                return;
            }
        }

        DamageSource source = event.getSource();
        boolean isProjectile = source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile;

        // --- SWORD BLOCKING DAMAGE MITIGATION (Classic Sword Block) ---
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof SpiritualSwordItem) {
            Vec3 playerLook = player.getLookAngle();
            Vec3 toDamage = null;
            if (attacker != null) {
                toDamage = attacker.position().subtract(player.position()).normalize();
            } else if (source.getDirectEntity() != null) {
                toDamage = source.getDirectEntity().position().subtract(player.position()).normalize();
            }

            boolean isFrontal = toDamage == null || playerLook.dot(toDamage) > -0.1;
            if (isFrontal) {
                player.getPersistentData().putLong("RpgLastSwordBlockTick", currentTick);

                // Block 65% of damage (Baluarte blocks 80%)
                SpiritualSwordItem.BladeStyle style = SpiritualSwordItem.getBladeStyle(player.getUseItem());
                float reductionRatio = (style == SpiritualSwordItem.BladeStyle.BULWARK) ? 0.80f : 0.65f;
                float originalDmg = event.getAmount();
                float blockedDmg = originalDmg * reductionRatio;
                float finalDmg = Math.max(0.5f, originalDmg - blockedDmg);
                event.setAmount(finalDmg);

                // Reduce durability on the sword (clamped by SpiritualSwordItem.setDamage so it never breaks)
                player.getUseItem().hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));

                // Audio & particle feedback
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9f, 1.4f);
                    serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(),
                            6, 0.2, 0.2, 0.2, 0.05);

                    // Elemental burst on block if sword has an element
                    SpiritualSwordItem.BladeElement element = SpiritualSwordItem.getBladeElement(player.getUseItem());
                    if (element != SpiritualSwordItem.BladeElement.NONE) {
                        SpiritualSwordItem.spawnHeldElementalAura(serverLevel, player, element);
                    }
                }

                if (isProjectile && source.getDirectEntity() instanceof Projectile projectile) {
                    projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.4).add(0, 0.15, 0));
                }
            }
        }

        // --- BULWARK PASSIVE: BARRERA ÉGIDA DE MANÁ (When hit below 45% HP while holding Bulwark Sword) ---
        ItemStack mainSword = player.getMainHandItem();
        if (mainSword.getItem() instanceof SpiritualSwordItem
                && SpiritualSwordItem.getBladeStyle(mainSword) == SpiritualSwordItem.BladeStyle.BULWARK
                && (player.getHealth() - event.getAmount() <= player.getMaxHealth() * 0.45f)) {
            long lastBarrier = player.getPersistentData().getLong("RpgBulwarkBarrierCooldown");
            if (currentTick - lastBarrier >= 160 && data.consumeMana(player, 4.0f)) {
                player.getPersistentData().putLong("RpgBulwarkBarrierCooldown", currentTick);
                event.setAmount(event.getAmount() * 0.55f);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 0, false, false, true));
                if (player instanceof ServerPlayer sp) {
                    ModNetworking.syncMana(sp);
                    sp.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.4, 0.5, 0.4, 0.1);
                    sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);
                }
            }
        }

        if (!isProjectile) {
            return;
        }

        int fortPoints = data.getTotalStatPoints(player, StatType.FORTITUDE);
        if (fortPoints <= 0) {
            return;
        }

        // 0.7% per fortitude point, max 70%
        double deflectChance = Math.min(0.70, fortPoints * 0.007);
        if (player.getRandom().nextDouble() < deflectChance) {
            // Deflect successful!
            event.setCanceled(true);

            // Audio-visual feedback
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.3, 0.3, 0.15);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
            }
            player.level().playSound(null, player.getX(), player.getY() + 1.0, player.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.3f);

            // Deflect projectile velocity
            Entity direct = source.getDirectEntity();
            if (direct instanceof Projectile projectile) {
                projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.5).add(0, 0.2, 0));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerAttackEntity(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (target != null && com.pablo.rpglevels.party.PartyManager.isAllied(player, target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget != null) {
            LivingEntity entity = event.getEntity();
            if (com.pablo.rpglevels.party.PartyManager.isAllied(entity, newTarget)) {
                event.setNewAboutToBeSetTarget(null);
                event.setCanceled(true);
            }
        }
    }

    /**
     * 2. DAMAGE MODIFIERS:
     * - DEXTERITY: Boost ranged projectile damage (+4% per point).
     * - RIPOSTE (GOLPE DE REMATE): Brutal critical damage against Staggered enemies!
     */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        LivingEntity victim = event.getEntity();

        // Bloquear daño de barrido/ataques si son aliados en la party
        if (attacker != null && com.pablo.rpglevels.party.PartyManager.isAllied(attacker, victim)) {
            event.setNewDamage(0.0f);
            return;
        }
        if (direct != null && com.pablo.rpglevels.party.PartyManager.isAllied(direct, victim)) {
            event.setNewDamage(0.0f);
            return;
        }

        // --- DEFENSA DE BRASAS DE BRAMBI ---
        // Si un enemigo golpea a su dueño o a un aliado, Brambi contraataca con un cabezazo ardiente
        if (attacker instanceof LivingEntity livingAttacker && !victim.level().isClientSide()) {
            List<com.pablo.rpglevels.entities.BrambiEntity> brambis = victim.level().getEntitiesOfClass(
                    com.pablo.rpglevels.entities.BrambiEntity.class,
                    victim.getBoundingBox().inflate(8.0D),
                    b -> b.isAlive() && !b.isOrderedToSit() && com.pablo.rpglevels.party.PartyManager.isAllied(b, victim)
            );
            for (var b : brambis) {
                if (b.triggerEmberDefense(livingAttacker)) {
                    break;
                }
            }
        }

        // Check for Riposte against Staggered target
        boolean isRiposte = false;
        if (attacker instanceof ServerPlayer player) {
            long currentTick = victim.level().getGameTime();
            long staggerUntil = victim.getPersistentData().getLong("RpgStaggerUntilTick");

            if (staggerUntil > currentTick) {
                isRiposte = true;
                // Consume Stagger state
                victim.getPersistentData().remove("RpgStaggerUntilTick");
                victim.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                victim.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);

                PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                int dex = (data != null) ? data.getTotalStatPoints(player, StatType.DEXTERITY) : 0;
                int fort = (data != null) ? data.getTotalStatPoints(player, StatType.FORTITUDE) : 0;
                float riposteMult = 2.5f + (dex * 0.05f) + (fort * 0.025f);

                event.setNewDamage(event.getNewDamage() * riposteMult);

                ServerLevel serverLevel = player.serverLevel();
                serverLevel.playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                        SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.3f, 1.1f);
                serverLevel.playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.85f);
                serverLevel.playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7f, 1.6f);

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
                serverLevel.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, victim.getX(), victim.getY() + 0.8, victim.getZ(), 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 25, 0.5, 0.5, 0.5, 0.2);

                player.displayClientMessage(
                        Component.translatable("rpglevels.combat.riposte", String.format("%.1f", riposteMult)),
                        true
                );
            }
        }

        // --- SPIRITUAL SWORD 4x4x4x4 SYSTEM & ANCIENT RING LIFESTEAL ---
        double bonusSwordCritChance = 0.0;
        double bonusSwordCritMult = 0.0;

        if (attacker instanceof ServerPlayer player && (direct == attacker || !(direct instanceof Projectile))) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                ItemStack heldSword = player.getMainHandItem();
                if (heldSword.getItem() instanceof SpiritualSwordItem && !SpiritualSwordItem.isTemporarilyExhausted(heldSword)) {
                    String ownerUuid = SpiritualSwordItem.getOwnerUUID(heldSword);
                    boolean isOwner = ownerUuid.isEmpty() || ownerUuid.equals(player.getUUID().toString());

                    if (isOwner) {
                        if (!SpiritualSwordItem.isInitialized(heldSword)) {
                            SpiritualSwordItem.imprintSoulBlade(heldSword, player);
                        }

                        if (SpiritualSwordItem.isFirstRelease(heldSword)) {
                            // Power-up de Primera Liberación: +50% al daño cuerpo a cuerpo general
                            event.setNewDamage(event.getNewDamage() * 1.50f);
                        }

                        long currentTick = player.level().getGameTime();
                        boolean overdrive = player.getPersistentData().getLong("RpgSwordOverdriveUntil") > currentTick;
                        float overdriveMult = overdrive ? 2.0f : 1.0f;

                        SpiritualSwordItem.BladeStyle style = SpiritualSwordItem.getBladeStyle(heldSword);
                        SpiritualSwordItem.BladeElement element = SpiritualSwordItem.getBladeElement(heldSword);
                        SpiritualSwordItem.BladeResonance resonance = SpiritualSwordItem.getBladeResonance(heldSword);

                        // A. ESTILO PASIVO (Duelista / Frenesí / Baluarte / Filo Fantasma)
                        switch (style) {
                            case DUELIST -> {
                                // Duelista: +15% daño base de Gran Espada y +40% adicional si contraataca tras un bloqueo o parry reciente
                                long lastBlock = player.getPersistentData().getLong("RpgLastSwordBlockTick");
                                boolean counterStrike = isRiposte || (currentTick - lastBlock <= 60);
                                float duelistMult = counterStrike ? 1.55f : 1.15f;
                                event.setNewDamage(event.getNewDamage() * duelistMult);
                            }
                            case FRENZY -> {
                                // Frenesí: Golpes consecutivos acumulan hasta 5 cargas (+8% daño por carga y Prisa en mano)
                                long lastHit = player.getPersistentData().getLong("RpgFrenzyLastHitTick");
                                int stacks = (currentTick - lastHit <= 70) ? player.getPersistentData().getInt("RpgFrenzyStacks") : 0;
                                stacks = Math.min(5, stacks + 1);
                                player.getPersistentData().putLong("RpgFrenzyLastHitTick", currentTick);
                                player.getPersistentData().putInt("RpgFrenzyStacks", stacks);

                                event.setNewDamage(event.getNewDamage() * (1.0f + stacks * 0.08f));
                                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                        net.minecraft.world.effect.MobEffects.DIG_SPEED, 65, Math.min(2, stacks - 1), false, false, false));
                            }
                            default -> {}
                        }

                        // B. RESONANCIA DEL ALMA (Sinergia con Estadísticas RPG)
                        switch (resonance) {
                            case SPIRIT -> {
                                int spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
                                event.setNewDamage(event.getNewDamage() + (spirit * 0.45f * overdriveMult));
                                data.regenerateMana(player, 1.5f * overdriveMult);
                                ModNetworking.syncMana(player);
                            }
                            case FORTITUDE -> {
                                int fort = data.getTotalStatPoints(player, StatType.FORTITUDE);
                                event.setNewDamage(event.getNewDamage() + (fort * 0.40f * overdriveMult));
                                Vec3 kb = player.getLookAngle().normalize().scale(0.35);
                                victim.push(kb.x, 0.1, kb.z);
                            }
                            case DEXTERITY -> {
                                int dex = data.getTotalStatPoints(player, StatType.DEXTERITY);
                                event.setNewDamage(event.getNewDamage() + (dex * 0.30f * overdriveMult));
                                bonusSwordCritChance = 12.0 * overdriveMult;
                                bonusSwordCritMult = 0.35 * overdriveMult;
                            }
                            case LUCK -> {
                                int luck = data.getTotalStatPoints(player, StatType.LUCK);
                                event.setNewDamage(event.getNewDamage() + (luck * 0.35f * overdriveMult));
                                double jackpotChance = (15.0 + luck * 1.2) * overdriveMult;
                                if (player.getRandom().nextDouble() * 100.0 < jackpotChance) {
                                    event.setNewDamage(event.getNewDamage() * 1.85f);
                                    data.regenerateMana(player, 4.0f);
                                    ModNetworking.syncMana(player);
                                    if (player.level() instanceof ServerLevel sl) {
                                        sl.addFreshEntity(new ExperienceOrb(sl, victim.getX(), victim.getY() + 0.5, victim.getZ(), 4 + luck));
                                        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, victim.getX(), victim.getY() + 1.0, victim.getZ(), 12, 0.3, 0.4, 0.3, 0.12);
                                    }
                                }
                            }
                        }

                        // C. EFECTO DE IMPACTO ELEMENTAL (Ignis / Fulgur / Glacies / Umbra)
                        if (player.level() instanceof ServerLevel sl) {
                            SpiritualSwordItem.applyElementalStatusEffect(sl, player, victim, element, event.getNewDamage());
                        }
                    }
                }

                // 2. Ancient Ring: Lifesteal on melee hits
                var curiosOpt = CuriosApi.getCuriosInventory(player);
                if (curiosOpt.isPresent()) {
                    var ancientOpt = curiosOpt.get().findFirstCurio(s -> s.getItem() instanceof AncientRingItem);
                    if (ancientOpt.isPresent() && player.getHealth() < player.getMaxHealth()) {
                        float lifestealRatio = AncientRingItem.getLifestealFraction(ancientOpt.get().stack());
                        float healAmount = Math.min(6.0f, event.getNewDamage() * lifestealRatio);
                        if (healAmount >= 0.2f) {
                            player.heal(healAmount);
                            if (player.level() instanceof ServerLevel sl) {
                                sl.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.5, player.getZ(), 1, 0.2, 0.2, 0.2, 0.02);
                            }
                        }
                    }
                }
            }
        }

        // --- RPG CRITICAL STRIKES & SUPER-CRÍTICOS (>100% Crit Chance) ---
        if (!isRiposte && attacker instanceof ServerPlayer player && (direct == attacker || !(direct instanceof Projectile))) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                double critChance = data.getCritChance(player) + bonusSwordCritChance;
                boolean isCrit = (critChance >= 100.0) || (player.getRandom().nextDouble() * 100.0 < critChance);

                if (isCrit) {
                    float critMult = (float) (data.getCritDamageMultiplier(critChance) + bonusSwordCritMult);
                    event.setNewDamage(event.getNewDamage() * critMult);

                    ServerLevel serverLevel = player.serverLevel();
                    if (critChance > 100.0) {
                        // Super-Crítico: Excess chance increases critical damage multiplier!
                        serverLevel.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1, 0, 0, 0, 0);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 1.0, victim.getZ(), 20, 0.4, 0.4, 0.4, 0.15);
                        serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 25, 0.4, 0.4, 0.4, 0.15);
                        serverLevel.playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.75f);
                        serverLevel.playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4f, 1.8f);

                        player.displayClientMessage(
                                Component.translatable("rpglevels.combat.super_crit", String.format("%.2f", critMult)),
                                true
                        );
                    } else {
                        // Standard Critical Strike
                        serverLevel.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 16, 0.3, 0.3, 0.3, 0.1);
                        serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + 1.0, victim.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
                        serverLevel.playSound(null, victim.getX(), victim.getY() + 1.0, victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.15f);

                        player.displayClientMessage(
                                Component.translatable("rpglevels.combat.crit"),
                                true
                        );
                    }
                }
            }
        }

        // Dexterity projectile bonus
        if (attacker instanceof ServerPlayer player && direct instanceof Projectile) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                int dexPoints = data.getTotalStatPoints(player, StatType.DEXTERITY);
                if (dexPoints > 0) {
                    float multiplier = 1.0f + (dexPoints * 0.04f);
                    event.setNewDamage(event.getNewDamage() * multiplier);
                }
            }
        }

        // --- VICTIM IS IRREGULAR MOB: ANTI-ONESHOT BURST CAP (MAX 40% HP PER HIT) ---
        if (victim instanceof net.minecraft.world.entity.Mob irregularMob
                && com.pablo.rpglevels.mobs.IrregularMobHandler.isIrregular(irregularMob)) {
            float maxBurst = Math.max(25.0f, irregularMob.getMaxHealth() * 0.40f);
            if (event.getNewDamage() > maxBurst) {
                event.setNewDamage(maxBurst);
            }
        }

        // --- VICTIM IS PLAYER: FORTITUDE REDUCTION & ONE-SHOT PROTECTION ---
        if (victim instanceof ServerPlayer player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                // 1. Passive Fortitude Damage Reduction: 0.45% per point (capped at 35%)
                int fort = data.getTotalStatPoints(player, StatType.FORTITUDE);
                if (fort > 0) {
                    float reduction = Math.min(0.35f, fort * 0.0045f);
                    event.setNewDamage(event.getNewDamage() * (1.0f - reduction));
                }

                // 2. One-Shot Protection (Anti-Muerte Súbita)
                // If player is at > 65% HP and incoming damage is lethal, survive with 1 heart (2 HP)
                float incomingDmg = event.getNewDamage();
                if (player.getHealth() >= player.getMaxHealth() * 0.65f && incomingDmg >= player.getHealth()) {
                    long currentTick = player.level().getGameTime();
                    long cooldownUntil = player.getPersistentData().getLong("RpgOneShotProtectionCooldown");

                    if (currentTick >= cooldownUntil) {
                        float survivableDmg = Math.max(0.0f, player.getHealth() - 2.0f);
                        event.setNewDamage(survivableDmg);

                        // 60-second cooldown (1200 ticks)
                        player.getPersistentData().putLong("RpgOneShotProtectionCooldown", currentTick + 1200);

                        // Brief Resistance buff to allow dodging out of harm's way
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, true));

                        ServerLevel serverLevel = player.serverLevel();
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.4, 0.4, 0.15);
                        serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
                        serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, 0.6f);
                        serverLevel.playSound(null, player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9f, 1.6f);

                        player.displayClientMessage(
                                Component.translatable("rpglevels.combat.oneshot_protection"),
                                true
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity victim = event.getEntity();
        Entity killer = event.getSource().getEntity();
        if (killer == null) {
            killer = victim.getLastHurtByMob();
        }

        Player responsiblePlayer = null;
        if (killer instanceof Player p) {
            responsiblePlayer = p;
        } else if (killer instanceof AdventurerNpcEntity adv && adv.isHired()) {
            responsiblePlayer = adv.getHiringPlayer();
            adv.addExperience(35);
        } else if (victim.getLastHurtByMob() instanceof Player p) {
            responsiblePlayer = p;
        }

        if (killer instanceof AdventurerNpcEntity advKiller && !(killer instanceof AdventurerNpcEntity advHired && advHired.isHired())) {
            advKiller.addExperience(25);
        }

        boolean isHostile = (victim instanceof net.minecraft.world.entity.monster.Enemy 
                || victim instanceof net.minecraft.world.entity.monster.Monster
                || victim.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER
                || victim instanceof com.pablo.rpglevels.entities.BanditEntity);

        if (responsiblePlayer instanceof ServerPlayer serverPlayer && isHostile) {
            ThreatLevel threat = ThreatLevel.determineThreatLevel(victim);

            // Aumento de afinidad con la Espada Espiritual según el nivel de amenaza derrotado (hasta +15)
            ItemStack sword = null;
            if (serverPlayer.getMainHandItem().getItem() instanceof SpiritualSwordItem) {
                sword = serverPlayer.getMainHandItem();
            } else if (serverPlayer.getOffhandItem().getItem() instanceof SpiritualSwordItem) {
                sword = serverPlayer.getOffhandItem();
            } else {
                for (ItemStack item : serverPlayer.getInventory().items) {
                    if (item.getItem() instanceof SpiritualSwordItem && SpiritualSwordItem.isSoulBound(item)) {
                        sword = item;
                        break;
                    }
                }
            }

            if (sword != null && SpiritualSwordItem.isSoulBound(sword)) {
                int affinityGain = switch (threat) {
                    case GENESIS, CATASTROPHE, GRAND_DISASTER -> 15;
                    case DISASTER -> 13;
                    case CALAMITY -> 11;
                    case HAZARD -> 9;
                    case A -> 8;
                    case B -> 7;
                    case C -> 5;
                    case D -> 3;
                    case E -> 2;
                    default -> 1;
                };

                int currentAff = SpiritualSwordItem.getAffinity(sword);
                int maxAff = SpiritualSwordItem.getMaxAffinity(sword);
                if (currentAff < maxAff) {
                    int newAff = Math.min(maxAff, currentAff + affinityGain);
                    SpiritualSwordItem.setAffinity(sword, newAff);

                    serverPlayer.displayClientMessage(
                            Component.literal("§d✦ +" + (newAff - currentAff) + " Afinidad con tu Espada Espiritual §8[" + newAff + "/" + maxAff + "]"),
                            true
                    );

                    if (newAff >= 100 && !SpiritualSwordItem.isFirstReleaseUnlocked(sword) && currentAff < 100) {
                        serverPlayer.sendSystemMessage(Component.empty());
                        serverPlayer.sendSystemMessage(Component.literal("§d✦ §l[ESPADA ESPIRITUAL] §f¡Tu hoja emana una inmensa concentración de energía espiritual!"));
                        serverPlayer.sendSystemMessage(Component.literal("§eHas alcanzado el 100% de Afinidad. El poder elemental se ha manifestado por completo."));
                        serverPlayer.sendSystemMessage(Component.literal("§bConsulta con Alice para descubrir el camino hacia la Primera Liberación."));
                        serverPlayer.sendSystemMessage(Component.empty());

                        serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                        serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
                        serverPlayer.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                                serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 40, 0.4, 0.6, 0.4, 0.15);
                    }
                }
            }

            int baseReward = switch (threat) {
                case GENESIS -> 9000;
                case CATASTROPHE -> 5500;
                case GRAND_DISASTER -> 3500;
                case DISASTER -> 2200;
                case CALAMITY -> 1400;
                case HAZARD -> 800;
                case A -> 450;
                case B -> 250;
                case C -> 140;
                case D -> 80;
                case E -> 45;
                default -> 25;
            };

            // Detección y recompensa aumentada para Jefes Mundiales y Jefes del Mod
            boolean isBoss = victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                    || victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                    || victim instanceof net.minecraft.world.entity.monster.warden.Warden
                    || victim instanceof net.minecraft.world.entity.monster.ElderGuardian
                    || victim instanceof com.pablo.rpglevels.entities.CultistBossEntity
                    || victim instanceof com.pablo.rpglevels.entities.UndeadAdventurerBossEntity
                    || victim instanceof com.pablo.rpglevels.entities.WindMasterBossEntity
                    || victim instanceof com.pablo.rpglevels.entities.VoidbornExecutorEntity
                    || victim instanceof com.pablo.rpglevels.entities.ColossalVoidbornEntity;

            if (isBoss) {
                if (victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                    baseReward = Math.max(baseReward, 3500);
                } else if (victim instanceof net.minecraft.world.entity.monster.warden.Warden) {
                    baseReward = Math.max(baseReward, 3000);
                } else if (victim instanceof net.minecraft.world.entity.boss.wither.WitherBoss) {
                    baseReward = Math.max(baseReward, 2500);
                } else if (victim instanceof com.pablo.rpglevels.entities.WindMasterBossEntity windMaster) {
                    baseReward = windMaster.isEmpowered() ? 1800 : 1200;
                } else if (victim instanceof com.pablo.rpglevels.entities.ColossalVoidbornEntity) {
                    baseReward = Math.max(baseReward, 1400);
                } else if (victim instanceof com.pablo.rpglevels.entities.VoidbornExecutorEntity) {
                    baseReward = Math.max(baseReward, 1000);
                } else if (victim instanceof com.pablo.rpglevels.entities.UndeadAdventurerBossEntity) {
                    baseReward = Math.max(baseReward, 1000);
                } else if (victim instanceof com.pablo.rpglevels.entities.CultistBossEntity) {
                    baseReward = Math.max(baseReward, 900);
                } else if (victim instanceof net.minecraft.world.entity.monster.ElderGuardian) {
                    baseReward = Math.max(baseReward, 700);
                } else {
                    baseReward = Math.max(baseReward, 800);
                }
            }

            // Otorgar XP RPG escalada exponencialmente al jugador responsable según la diferencia de rango
            PlayerRpgData killerData = serverPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
            int killerXp = baseReward;
            if (killerData != null) {
                AdventurerRank pRank = killerData.getRank();
                killerXp = calculateRankScaledXp(baseReward, pRank, threat);

                if (killerData.hasDivineBlessingUnlocked()) {
                    killerData.addRpgXp(serverPlayer, killerXp);
                }

                int rankDiff = threat.ordinal() - pRank.getIndex();
                if (rankDiff >= 2) {
                    double mult = (double) killerXp / (double) baseReward;
                    serverPlayer.sendSystemMessage(Component.literal(
                            "§6✦ §l[¡PROEZA CONTRA EL DESTINO!] §eSiendo Rango §f" + pRank.getName()
                                    + " §ehas abatido a un enemigo §b" + threat.getDisplayName()
                                    + " §7(§a+" + killerXp + " XP RPG §econ bono exponencial §d×" + String.format(java.util.Locale.US, "%.1f", mult) + "§7)"));
                    serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.3f);
                    serverPlayer.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                            serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 30, 0.4, 0.5, 0.4, 0.2);
                } else if (isBoss) {
                    serverPlayer.sendSystemMessage(Component.literal(
                            "§6✦ §l[¡JEFE DERROTADO!] §eHas vencido a §f" + victim.getDisplayName().getString() + " §7(§a+" + killerXp + " XP RPG§7)"));
                    serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.1f);
                }
            }

            // Repartir experiencia a los aventureros NPC y aliados de party cercanos
            List<LivingEntity> recipients = com.pablo.rpglevels.party.PartyManager.getPartyXpRecipients(serverPlayer, 48.0);
            if (!recipients.isEmpty()) {
                for (LivingEntity recipient : recipients) {
                    if (recipient instanceof AdventurerNpcEntity adv) {
                        int advShare = Math.max(isBoss ? 150 : 25, baseReward / recipients.size());
                        adv.addExperience(advShare);
                        // Bonificación de afinidad en combate
                        if (adv.isHiredBy(serverPlayer) && (isBoss || threat.ordinal() >= ThreatLevel.C.ordinal())) {
                            adv.addAffinity(serverPlayer, isBoss ? 3 : 1);
                        }
                    } else if (recipient instanceof ServerPlayer allyPlayer && allyPlayer != serverPlayer) {
                        PlayerRpgData allyData = allyPlayer.getData(ModAttachments.PLAYER_RPG_DATA);
                        if (allyData != null && allyData.hasDivineBlessingUnlocked()) {
                            // Cada aliado en party calcula su propio escalamiento exponencial según su propio rango
                            int allyBase = Math.max(15, baseReward / 2);
                            int allyXp = calculateRankScaledXp(allyBase, allyData.getRank(), threat);
                            allyData.addRpgXp(allyPlayer, allyXp);

                            int allyRankDiff = threat.ordinal() - allyData.getRank().getIndex();
                            if (allyRankDiff >= 2) {
                                double allyMult = (double) allyXp / (double) allyBase;
                                allyPlayer.sendSystemMessage(Component.literal(
                                        "§6✦ §l[¡PROEZA EN GRUPO!] §eTu grupo abatió a un enemigo §b" + threat.getDisplayName()
                                                + " §7(§a+" + allyXp + " XP RPG §econ bono exponencial §d×" + String.format(java.util.Locale.US, "%.1f", allyMult) + "§7)"));
                            } else if (isBoss) {
                                allyPlayer.sendSystemMessage(Component.literal(
                                        "§6✦ §l[¡JEFE DERROTADO!] §eTu grupo venció a §f" + victim.getDisplayName().getString() + " §7(§a+" + allyXp + " XP RPG§7)"));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates exponential RPG XP based on the difference between the monster's threat level
     * and the adventurer's rank. If a low-rank adventurer takes down a high-threat monster,
     * they are rewarded exponentially (e.g. Rank F defeating a Rank B monster grants ~13x XP).
     */
    public static int calculateRankScaledXp(int baseReward, AdventurerRank playerRank, ThreatLevel threat) {
        if (playerRank == null || threat == null || baseReward <= 0) {
            return baseReward;
        }

        int playerRankIndex = playerRank.getIndex(); // 0 (F) to 5 (A)
        int mobThreatIndex = threat.ordinal();       // 0 (F) to 11 (GENESIS)
        int rankDiff = mobThreatIndex - playerRankIndex;

        if (rankDiff > 0) {
            // Escalamiento exponencial cuando el enemigo supera el rango del cazador:
            // Cada rango de diferencia superior multiplica exponencialmente por 1.9^rankDiff (tope de exponente 7)
            double exponent = Math.min(7.0, (double) rankDiff);
            double exponentialMultiplier = Math.pow(1.9, exponent);
            return Math.max(baseReward, (int) Math.round(baseReward * exponentialMultiplier));
        } else if (rankDiff < 0) {
            // Penalización suave cuando un cazador de rango alto elimina criaturas muy débiles
            double underlevelMult = Math.max(0.25, 1.0 - (Math.abs(rankDiff) * 0.15));
            return Math.max(1, (int) Math.round(baseReward * underlevelMult));
        } else {
            // Mismo rango: recompensa base completa
            return baseReward;
        }
    }
}
