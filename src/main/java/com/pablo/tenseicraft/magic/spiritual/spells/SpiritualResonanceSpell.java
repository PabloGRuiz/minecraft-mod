package com.pablo.tenseicraft.magic.spiritual.spells;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.events.ModAttributesHandler;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.magic.spiritual.SpiritualSchool;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.network.StartSpiritualResonanceCinematicPayload;
import com.pablo.tenseicraft.network.SyncEntityAuraPayload;
import com.pablo.tenseicraft.network.SyncOppressiveAuraPayload;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * Técnica Suprema Universal de la Espada Espiritual (Desbloqueada a los 200 de Afinidad):
 * RESONANCIA ESPIRITUAL (Spiritual Resonance).
 *
 * Durante 30 segundos, el alma y el cuerpo alcanzan la resonancia perfecta:
 * - Multiplica x2 (Duplica) todas las estadísticas del jugador (vida, daño, velocidad, armadura, poder mágico, etc.).
 * - Desata un Aura Espiritual colosal y exaltada que permanece activa continuamente durante los 30 segundos.
 * - Al activarse, desencadena una cinemática dramática con barras negras panorámicas, vibración de cámara,
 *   enfoque en primer plano, aura opresora de protección (ralentización extrema a enemigos y desvío de proyectiles),
 *   seguida de una onda de choque expansiva que transiciona suavemente a la perspectiva de combate.
 * - Cooldown universal: 200 segundos.
 */
public class SpiritualResonanceSpell extends AbstractSpell {

    private final ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "spiritual_resonance");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SpiritualSchool.SPIRITUAL_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(200)
            .build();

    public SpiritualResonanceSpell() {
        this.baseManaCost = 50;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_TWO_HANDS;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.BEACON_ACTIVATE);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (!(level instanceof ServerLevel serverLevel)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        if (entity instanceof Player player) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                long currentTick = level.getGameTime();
                int durationTicks = 600; // 30 segundos
                int cinematicTicks = 70; // 3.5 segundos de cinemática épica

                // Activar modo en datos RPG del jugador
                data.activateSpiritualResonance(currentTick, durationTicks, cinematicTicks);

                // Determinar color del aura según la espada portada
                ItemStack held = player.getMainHandItem();
                if (!(held.getItem() instanceof SpiritualSwordItem)) {
                    held = player.getOffhandItem();
                }

                int auraColorRgb = 0x59D1FF; // Celeste espiritual por defecto
                if (held.getItem() instanceof SpiritualSwordItem) {
                    if (SpiritualSwordItem.isSenbonzakura(held)) {
                        auraColorRgb = 0xFFB7C5; // Pétalos de cerezo Sakura
                    } else {
                        SpiritualSwordItem.BladeElement element = SpiritualSwordItem.getBladeElement(held);
                        auraColorRgb = switch (element) {
                            case IGNIS -> 0xFF8014;
                            case FULGUR -> 0xFFF22E;
                            case GLACIES -> 0x40E0FF;
                            case UMBRA -> 0xAD26FF;
                            default -> 0x59D1FF;
                        };
                    }
                }

                if (player instanceof ServerPlayer serverPlayer) {
                    // Sincronizar datos RPG completos al cliente
                    ModNetworking.syncToPlayer(serverPlayer);

                    // Enviar paquete cinemático al jugador local
                    PacketDistributor.sendToPlayer(serverPlayer, new StartSpiritualResonanceCinematicPayload(player.getId(), cinematicTicks, 35));

                    // Sincronizar Aura visible y Aura opresora para todos los jugadores que observan
                    float auraPower = data.getAuraStat(player) * 2.0f;
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                            new SyncEntityAuraPayload(player.getId(), durationTicks, auraPower, auraColorRgb));
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                            new SyncOppressiveAuraPayload(player.getId(), cinematicTicks, 16.0f, auraPower, auraColorRgb));

                    // Efectos protectores de cinemática (Resistencia V para invulnerabilidad absoluta)
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, cinematicTicks + 10, 4, false, false, true));

                    // Recalcular atributos inmediatamente para duplicar vida, daño, velocidad, armadura
                    ModAttributesHandler.updatePlayerAttributes(serverPlayer);
                }

                // Efectos sonoros y de partículas de inicio
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.3f, 0.85f);
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.25f);
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.7f);

                serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
            }
        }

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("tenseicraft.spiritual_sword.resonance_form.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("§7Tipo: §dForma Suprema Universal (200 Afinidad)"),
                Component.literal("§eDuración: §f30 Segundos §8(Cooldown: 200s)"),
                Component.literal("§bEfecto: §a¡Duplica (x2) todas las estadísticas del jugador!"),
                Component.literal("§7Aura: §6Aura de Ki exaltada continua con cinemática de despertar")
        );
    }
}
