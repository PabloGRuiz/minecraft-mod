package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.entities.MagicSpellProjectileEntity;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.magic.SpiritualTechnique;
import com.pablo.tenseicraft.magic.spiritual.SpiritualSpells;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CastSpellPayload(String spellId) implements CustomPacketPayload {

    public static final Type<CastSpellPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "cast_spell"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    CastSpellPayload::spellId,
                    CastSpellPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastSpellPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) {
                return;
            }

            SpiritualTechnique tech = SpiritualTechnique.byId(payload.spellId());
            if (tech == null) {
                return;
            }

            if (!data.hasSkill(tech.getId())) {
                player.displayClientMessage(
                        Component.literal("§c¡No conoces esta técnica espiritual! Alice, de los Segadores, debe enseñártela."),
                        true
                );
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.6f);
                return;
            }

            // Cooldown check
            long currentTick = player.level().getGameTime();
            if (data.isSkillOnCooldown(tech.getId(), currentTick)) {
                long remainingTicks = data.getSkillCooldownRemainingTicks(tech.getId(), currentTick);
                float remainingSec = remainingTicks / 20.0f;
                player.displayClientMessage(
                        Component.literal("§e[" + tech.getDisplayName() + "] §cen enfriamiento. Espera §f" + String.format(java.util.Locale.ROOT, "%.1f", remainingSec) + "s"),
                        true
                );
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.2f);
                return;
            }

            switch (tech) {
                case PHYSICAL_BOOST -> executePhysicalBoost(player, data, tech);
                case LOW_HEALING -> executeLowHealing(player, data, tech);
                case LOW_PARALIZE -> executeLowParalize(player, data, tech);
                case LOW_PURIFICATION -> executeLowPurification(player, data, tech);
                case ELEMENTAL_BALL -> executeElementalBall(player, data, tech);
                case MAGIC_AFFINITY -> executeChannelMagic(player, data, tech);
                case SUMMON_LESSER_SOUL -> executeSummonLesserSoul(player, data, tech);
                case FAST_HEALING -> executeFastHealing(player, data, tech);
                case CONNECT_REALITIES -> executeConnectRealities(player, data, tech);
                case OPPRESSIVE_AURA -> executeOppressiveAura(player, data, tech);
                case HAKUREI -> executeHakurei(player, data, tech);
            }
        });
    }

    private static boolean consumeManaOrFree(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech, float customCost) {
        if (PlayerRpgData.isHoldingReleasedSpiritualSword(player)) {
            // Primera Liberación Activa: ¡Hechizos sin coste alguno de maná!
            player.serverLevel().sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.3, 0.4, 0.3, 0.1);
            return true;
        }

        float baseCost = customCost > 0 ? customCost : tech.getManaCost();
        boolean isBondingCost = Math.abs(customCost - 200.0f) < 0.01f;
        float manaCost = isBondingCost ? baseCost : (baseCost * data.getSkillManaCostMultiplier());
        if (!data.consumeMana(player, manaCost)) {
            player.displayClientMessage(
                    Component.literal("§c¡Maná insuficiente! Requiere " + (int) Math.ceil(manaCost) + " MP (Tienes: " + (int)data.getCurrentMana(player) + "/" + (int)data.getMaxMana(player) + " MP)"),
                    true
            );
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.4f);
            return false;
        }
        ModNetworking.syncMana(player);
        return true;
    }

    private static boolean consumeManaOrFree(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        return consumeManaOrFree(player, data, tech, -1.0f);
    }

    private static boolean checkSoulControlFailure(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        float failChance = data.getTechniqueFailChance();
        if (failChance <= 0.0f || player.getRandom().nextFloat() >= failChance) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.9f, 0.85f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.1, player.getZ(), 12, 0.3, 0.4, 0.3, 0.04);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.1, player.getZ(), 15, 0.35, 0.45, 0.35, 0.12);

        int halfCd = Math.max(20, data.getTechniqueCooldownTicks(player, tech) / 2);
        data.setSkillCooldown(tech.getId(), player.level().getGameTime(), halfCd);
        ModNetworking.syncToPlayer(player);

        player.displayClientMessage(
                Component.literal("§c⚡ ¡Tu inmensa energía espiritual se descontroló y la técnica falló!"),
                true
        );
        return true;
    }

    private static void applyTechniqueCooldown(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        int cd = data.getTechniqueCooldownTicks(player, tech);
        data.setSkillCooldown(tech.getId(), player.level().getGameTime(), cd);
        ModNetworking.syncToPlayer(player);
    }

    private static void broadcastIncantation(ServerPlayer player, String latinChant, String spellDisplayName, ChatFormatting color) {
        Component chantMsg = Component.literal("§8[✦] ")
                .append(Component.literal("<").withStyle(ChatFormatting.GRAY))
                .append(player.getDisplayName())
                .append(Component.literal(">: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(latinChant + " ").withStyle(color, ChatFormatting.BOLD))
                .append(Component.literal("— [").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(spellDisplayName).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("]").withStyle(ChatFormatting.GOLD));

        for (ServerPlayer nearby : player.serverLevel().players()) {
            if (nearby.distanceToSqr(player) <= 1024.0) {
                nearby.sendSystemMessage(chantMsg);
            }
        }
    }

    private static void triggerIronsSpellEffects(ServerPlayer player, PlayerRpgData data, AbstractSpell spell) {
        if (spell == null) return;
        int spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
        int rawLevel = Math.max(1, Math.min(10, 1 + (spirit / 15)));
        int spellLevel = Math.max(1, Math.min(10, (int) Math.round(rawLevel * data.getTechniqueEffectivenessMultiplier())));

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new OnCastStartedPacket(player.getUUID(), spell.getSpellId(), spellLevel)
        );
        spell.castSpell(player.serverLevel(), spellLevel, player, CastSource.COMMAND, false);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new OnClientCastPacket(spell.getSpellId(), spellLevel, CastSource.COMMAND, null)
        );
    }

    // =========================================================================
    // 1. TÉCNICA ESPIRITUAL NRO. 1: PHYSICAL BOOST (1 - 2 - 3 | Coste: 75 MP)
    // =========================================================================
    private static void executePhysicalBoost(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        triggerIronsSpellEffects(player, data, SpiritualSpells.PHYSICAL_BOOST.get());

        player.displayClientMessage(
                Component.literal("§6§l¡TÉCNICA ESPIRITUAL Nº 1: PHYSICAL BOOST! §e(+Fuerza, Resistencia y Vel. activados)"),
                true
        );
    }

    // =========================================================================
    // 2. TÉCNICA ESPIRITUAL NRO. 2: LOW HEALING (1 - 3 - 2 | Coste: 25 MP)
    // =========================================================================
    private static void executeLowHealing(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        triggerIronsSpellEffects(player, data, SpiritualSpells.LOW_HEALING.get());

        player.displayClientMessage(
                Component.literal("§a§l¡TÉCNICA ESPIRITUAL Nº 2: LOW HEALING! §e(Pulso sanador emitido)"),
                true
        );
    }

    // =========================================================================
    // 3. TÉCNICA ESPIRITUAL NRO. 3: LOW PARALIZE (2 - 1 - 3 | Coste: 60 MP)
    // =========================================================================
    private static void executeLowParalize(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        triggerIronsSpellEffects(player, data, SpiritualSpells.LOW_PARALIZE.get());

        player.displayClientMessage(
                Component.literal("§e§l¡TÉCNICA ESPIRITUAL Nº 3: LOW PARALIZE! §e(Rayo restrictivo disparado)"),
                true
        );
    }

    // =========================================================================
    // 4. TÉCNICA ESPIRITUAL NRO. 4: LOW PURIFICATION (3 - 2 - 1 | Coste: 20 MP)
    // =========================================================================
    private static void executeLowPurification(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        triggerIronsSpellEffects(player, data, SpiritualSpells.LOW_PURIFICATION.get());

        player.displayClientMessage(
                Component.literal("§b§l¡TÉCNICA ESPIRITUAL Nº 4: LOW PURIFICATION! §e(Purificación de alma ejecutada)"),
                true
        );
    }

    // =========================================================================
    // 5. TÉCNICA ESPIRITUAL NRO. 5: ELEMENTAL BALL (3 - 1 - 2 | Coste: 25 MP)
    // =========================================================================
    private static void executeElementalBall(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);

        String dynamicName = SpiritualTechnique.getDynamicElementalBallName(player);
        broadcastIncantation(player, tech.getChant(), dynamicName, tech.getColor());

        triggerIronsSpellEffects(player, data, SpiritualSpells.ELEMENTAL_BALL.get());

        player.displayClientMessage(
                Component.literal("§c§l¡" + dynamicName.toUpperCase() + "! §e(Esfera espiritual detonada)"),
                true
        );
    }

    // =========================================================================
    // 6. TÉCNICA ESPIRITUAL NRO. 6: AFINIDAD MÁGICA (2 - 3 - 1 | Coste: 50 MP)
    // =========================================================================
    private static void executeChannelMagic(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            held = player.getItemInHand(InteractionHand.OFF_HAND);
        }

        boolean isBondingSword = (held.getItem() instanceof SpiritualSwordItem) && !SpiritualSwordItem.isSoulBound(held);

        if (isBondingSword && data.getCurrentMana(player) < 200.0f) {
            player.displayClientMessage(
                    Component.literal("§c¡Maná insuficiente para forjar el Vínculo de Alma! Requiere al menos 200 MP (Tienes: " + (int) data.getCurrentMana(player) + "/200 MP)"),
                    true
            );
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.4f);
            return;
        }

        if (!consumeManaOrFree(player, data, tech, isBondingSword ? 200.0f : -1.0f)) return;
        if (!isBondingSword && checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);

        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        // Mastery progression
        boolean leveledUp = data.incrementChannelMagicUses();
        int tier = data.getChannelMagicTier();
        int uses = data.getChannelMagicUses();

        // Temporary boost of +5 Spirit for 15s (300 ticks) as requested
        data.activateSpiritSurge(player.level().getGameTime(), 300, 5);

        // Despertar del Aura estilo Dragon Ball por 5 segundos (100 ticks)
        data.activateAura(player.level().getGameTime(), 100);
        float auraStat = data.getAuraStat(player);
        int auraColor = getPlayerAuraColorRgb(player);

        // Sincronizar tanto al jugador como a todos los que lo observan
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncEntityAuraPayload(player.getId(), 100L, auraStat, auraColor)
        );

        ModNetworking.syncToPlayer(player);

        triggerIronsSpellEffects(player, data, SpiritualSpells.MAGIC_AFFINITY.get());

        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.6f);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.8f);

        player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.7, 0.4, 0.08);
        player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.5, 0.8, 0.5, 0.18);

        // Soul bonding if holding virgin sword
        if (isBondingSword) {
            SpiritualSwordItem.bindSoulVassal(held, player);

            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.2f, 1.2f);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.2f, 1.0f);
            player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(), 45, 0.5, 0.8, 0.5, 0.15);

            Component swordName = held.getHoverName();
            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§5« ¡VÍNCULO MAESTRO-ARMA FORJADO! »").withStyle(ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("§f«Has canalizado exactamente §d200 unidades de Maná§f en la hoja.»"));
            player.sendSystemMessage(Component.literal("§f«El enlace con tu alma se ha completado: §5[").append(swordName).append("§5]§f!»"));
            player.sendSystemMessage(Component.empty());
        }

        player.displayClientMessage(
                Component.literal("§b§l¡TÉCNICA ESPIRITUAL Nº 6: AFINIDAD MÁGICA! §d(+5 Espíritu | ¡Aura del Alma Despertada durante 5s! | Usos: " + uses + ")"),
                true
        );
    }

    private static int getPlayerAuraColorRgb(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            held = player.getOffhandItem();
        }
        if (held.getItem() instanceof SpiritualSwordItem) {
            SpiritualSwordItem.BladeElement elem = SpiritualSwordItem.getBladeElement(held);
            return switch (elem) {
                case IGNIS -> 0xFF8811;   // Fuego naranja / dorado
                case FULGUR -> 0xFFEE22;  // Rayo amarillo
                case GLACIES -> 0x33DDFF; // Hielo celeste
                case UMBRA -> 0xAA22FF;   // Sombra violeta
                default -> 0x44AAFF;      // Azul espiritual
            };
        }
        return 0xFFD700; // Dorado clásico de Ki por defecto
    }

    // =========================================================================
    // 7. TÉCNICA ESPIRITUAL NRO. 7: CONVOCAR ALMA MENOR (1 - 3 - 1 | Coste: 50 MP)
    // =========================================================================
    private static void executeSummonLesserSoul(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        com.pablo.tenseicraft.magic.spiritual.spells.SummonLesserSoulSpell.spawnOrTeleportPelotita(player, player.serverLevel());

        if (com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SUMMON_LESSER_SOUL != null) {
            triggerIronsSpellEffects(player, data, com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.SUMMON_LESSER_SOUL.get());
        }
    }

    // =========================================================================
    // 8. TÉCNICA ESPIRITUAL NRO. 10: FAST HEALING (3 - 2 - 1 - 2 | Coste: 80 MP)
    // =========================================================================
    private static void executeFastHealing(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        // Auto-target lowest % HP ally / party member within 24m (includes player)
        java.util.List<net.minecraft.world.entity.LivingEntity> allies = com.pablo.tenseicraft.party.PartyManager.getAllies(player, 24.0);
        net.minecraft.world.entity.LivingEntity target = null;
        float lowestPct = 1.0f;

        for (net.minecraft.world.entity.LivingEntity ally : allies) {
            if (!ally.isAlive()) continue;
            float maxHp = ally.getMaxHealth();
            float currentHp = ally.getHealth();
            if (currentHp >= maxHp) continue;

            float pct = currentHp / maxHp;
            if (pct < lowestPct) {
                lowestPct = pct;
                target = ally;
            }
        }

        if (target == null) {
            player.displayClientMessage(Component.literal("§e✦ No hay aliados ni compañeros heridos en 24 metros para sanar."), true);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.8f);
            return;
        }

        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);

        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        int spirit = data.getTotalStatPoints(player, StatType.SPIRIT);
        float healAmount = (14.0f + (spirit * 0.8f)) * data.getTechniqueEffectivenessMultiplier();
        target.heal(healAmount);

        // Visual beam
        Vec3 start = player.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 diff = end.subtract(start);
        double distance = diff.length();
        int steps = Math.max(6, (int) (distance * 4));

        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            Vec3 point = start.add(diff.scale(progress));
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, point.x, point.y, point.z, 1, 0.04, 0.04, 0.04, 0.01);
            if (i % 2 == 0) {
                player.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        player.serverLevel().sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 18, 0.4, 0.4, 0.4, 0.1);

        player.serverLevel().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f);
        player.serverLevel().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.8f);

        String targetName = target.getDisplayName().getString();
        float currentHp = target.getHealth();
        float maxHp = target.getMaxHealth();
        int pctInt = Math.min(100, Math.round((currentHp / maxHp) * 100.0f));

        player.displayClientMessage(
                Component.literal("§a§l¡TÉCNICA ESPIRITUAL Nº 10: FAST HEALING! §eSanado a §f" + targetName + " §epor §a+" + String.format(java.util.Locale.ROOT, "%.1f", healAmount) + " HP §7(" + (int)currentHp + "/" + (int)maxHp + " HP - " + pctInt + "%)"),
                true
        );

        if (target instanceof ServerPlayer targetPlayer && targetPlayer != player) {
            targetPlayer.sendSystemMessage(
                    Component.literal("§a[✦] §b" + player.getName().getString() + " §ate ha sanado §e+" + String.format(java.util.Locale.ROOT, "%.1f", healAmount) + " HP §acon Fast Healing §7(" + (int)currentHp + "/" + (int)maxHp + " HP)")
            );
        }
    }

    // =========================================================================
    // 20. TÉCNICA ESPIRITUAL NRO. 20: CONECTAR REALIDADES (Coste: 500 MP)
    // =========================================================================
    private static void executeConnectRealities(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        boolean exitingGensokyo = com.pablo.tenseicraft.world.dimension.ModDimensions.isGensokyo(player.level());
        if (!exitingGensokyo && checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        ServerLevel level = player.serverLevel();
        Vec3 pos = player.position();

        // 1. Shockwave particles (expanding sphere / ring)
        level.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y + 0.8, pos.z, 2, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1.0, pos.z, 80, 2.5, 1.2, 2.5, 0.2);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.8, pos.z, 40, 2.0, 0.5, 2.0, 0.1);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y + 0.5, pos.z, 50, 3.0, 0.8, 3.0, 0.05);

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.6f);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0f, 0.9f);

        // 2. Combat knockback on nearby monsters (radius 6m)
        AABB waveBox = player.getBoundingBox().inflate(6.0);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, waveBox,
                e -> e != player && e.isAlive() && !(e instanceof Player));
        for (LivingEntity e : enemies) {
            Vec3 delta = e.position().subtract(pos).normalize();
            e.setDeltaMovement(delta.x * 1.5, 0.45, delta.z * 1.5);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }

        // 3. Frame detection: check if there is an unlit Crying Obsidian portal frame nearby!
        boolean portalLit = com.pablo.tenseicraft.world.dimension.AbyssPortalBlock.attemptPortalIgnition(level, player.blockPosition(), 6);

        if (portalLit) {
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.4f, 0.7f);
            player.sendSystemMessage(Component.literal("§5✦ ¡EL PORTAL DEL ABISMO HA DESPERTADO! ✦"));
            player.sendSystemMessage(Component.literal("§dEl tejido dimensional se rasga ante tu poder espiritual. El portal de Obsidiana Llorosa está activo."));
            player.displayClientMessage(Component.literal("§d§l¡PORTAL DEL ABISMO ACTIVADO!"), true);
        } else {
            player.displayClientMessage(Component.literal("§5§l¡CONECTAR REALIDADES! §7(Onda expansiva desatada)"), true);
        }

        if (com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.CONNECT_REALITIES != null) {
            triggerIronsSpellEffects(player, data, com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.CONNECT_REALITIES.get());
        }
    }

    // =========================================================================
    // 10. TÉCNICA ESPIRITUAL NRO. 25: AURA OPRESIVA (1 - 2 - 1 - 3 | Coste: 120 MP)
    // =========================================================================
    private static void executeOppressiveAura(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        int durationTicks = 240; // 12 segundos
        data.activateOppressiveAura(player.level().getGameTime(), durationTicks);

        float auraStat = data.getAuraStat(player);
        float maxRadius = (float) Math.min(24.0, 6.0 + (auraStat * 0.10));
        int colorRgb = getPlayerAuraColorRgb(player);
        if (colorRgb == 0xFFD700) {
            colorRgb = 0xAA22FF; // Púrpura espiritual profundo por defecto para Aura Opresiva
        }

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player,
                new SyncOppressiveAuraPayload(player.getId(), (long) durationTicks, maxRadius, auraStat, colorRgb)
        );

        ModNetworking.syncToPlayer(player);

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.9f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 0.6f);

        // Ondas de presión en el suelo
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 0.2, player.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 0.5, player.getZ(), 60, 1.5, 0.3, 1.5, 0.15);

        player.displayClientMessage(
                Component.literal("§5§l¡TÉCNICA ESPIRITUAL Nº 25: AURA OPRESIVA! §d(Presión Espiritual Desatada | Radio: " + String.format("%.1f", maxRadius) + "m)"),
                true
        );
    }

    // =========================================================================
    // 11. TÉCNICA ESPIRITUAL NRO. 890: HA-KU-REI (3 - 1 - 3 - 1 | Coste: 90 MP)
    // =========================================================================
    private static void executeHakurei(ServerPlayer player, PlayerRpgData data, SpiritualTechnique tech) {
        if (!consumeManaOrFree(player, data, tech)) return;
        if (checkSoulControlFailure(player, data, tech)) return;
        applyTechniqueCooldown(player, data, tech);
        broadcastIncantation(player, tech.getChant(), tech.getDisplayName(), tech.getColor());

        triggerIronsSpellEffects(player, data, SpiritualSpells.HAKUREI.get());

        player.displayClientMessage(
                Component.literal("§c§l¡TÉCNICA ESPIRITUAL Nº 890: HA-KU-REI! §e(Orbe rebote lanzado)"),
                true
        );
    }
}
