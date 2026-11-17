package com.pablo.rpglevels.compat.irons;

import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.events.MagicAdvancementEvents;
import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public class IronsSpellsCompat {

    private static final ResourceLocation MOD_SPIRIT_MAX_MANA =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_spirit_max_mana");
    private static final ResourceLocation MOD_SPIRIT_MANA_REGEN =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_spirit_mana_regen");
    private static final ResourceLocation MOD_SPIRIT_GENERAL_SPELL_POWER =
            ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "stat_spirit_general_spell_power");

    private static Boolean ironsLoaded = null;

    public static boolean isLoaded() {
        if (ironsLoaded == null) {
            ironsLoaded = ModList.get().isLoaded("irons_spellbooks");
        }
        return ironsLoaded;
    }

    public static void init() {
        if (isLoaded()) {
            RpgLevelsMod.LOGGER.info("RPG Levels: Detectado Iron's Spells 'n Spellbooks. Registrando enlace de Afinidad Mágica y Maná...");
            NeoForge.EVENT_BUS.register(new IronsSpellsEvents());
        }
    }

    public static float getIronsMana(Player player) {
        if (!isLoaded() || player == null) return -1.0f;
        try {
            if (player.level().isClientSide()) {
                return IronsSpellsClientCompat.getClientMana();
            } else {
                MagicData magicData = MagicData.getPlayerMagicData(player);
                return magicData != null ? magicData.getMana() : -1.0f;
            }
        } catch (Throwable t) {
            return -1.0f;
        }
    }

    public static float getIronsMaxMana(Player player) {
        if (!isLoaded() || player == null) return -1.0f;
        try {
            if (player.level().isClientSide()) {
                return IronsSpellsClientCompat.getClientMaxMana(player);
            } else {
                if (player.getAttributes().hasAttribute(AttributeRegistry.MAX_MANA)) {
                    return (float) player.getAttributeValue(AttributeRegistry.MAX_MANA);
                }
            }
        } catch (Throwable t) {
        }
        return -1.0f;
    }

    public static boolean consumeIronsMana(Player player, float amount) {
        if (!isLoaded() || player == null || player.level().isClientSide()) return false;
        try {
            MagicData magicData = MagicData.getPlayerMagicData(player);
            if (magicData != null && magicData.getMana() >= amount) {
                magicData.addMana(-amount);
                if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new SyncManaPacket(magicData));
                }
                return true;
            }
        } catch (Throwable t) {
        }
        return false;
    }

    public static void setIronsMana(Player player, float mana) {
        if (!isLoaded() || player == null || player.level().isClientSide()) return;
        try {
            MagicData magicData = MagicData.getPlayerMagicData(player);
            if (magicData != null) {
                magicData.setMana(mana);
                if (player instanceof ServerPlayer sp) {
                    PacketDistributor.sendToPlayer(sp, new SyncManaPacket(magicData));
                }
            }
        } catch (Throwable t) {
        }
    }

    public static void applySpiritModifiers(Player player, int spirit, int channelTier, boolean spiritSurge) {
        if (!isLoaded() || player == null || player.level().isClientSide()) return;
        try {
            AttributeInstance maxManaInst = player.getAttribute(AttributeRegistry.MAX_MANA);
            if (maxManaInst != null && Math.abs(maxManaInst.getBaseValue() - 100.0) > 0.01) {
                maxManaInst.setBaseValue(100.0);
            }
            // En Espíritu = 5 y Tier = 1, el maná base estándar es 100 (0 de bono). +/- 10 PM por punto respecto a 5.
            double bonusMax = Math.max(-80.0, ((spirit - 5) * 10.0) + ((channelTier - 1) * 25.0));
            double bonusRegen = Math.max(0.0, (spirit - 5) * 0.05);
            if (spiritSurge) {
                bonusRegen = (bonusRegen + 0.25) * 2.0;
            }
            if (PlayerRpgData.isHoldingReleasedSpiritualSword(player)) {
                // Durante la Primera Liberación activa: el maná NO se regenera pasivamente (multiplicador total -1.0 anula el 100% del maná regen)
                applyModifier(player, AttributeRegistry.MANA_REGEN, MOD_SPIRIT_MANA_REGEN, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            } else {
                applyModifier(player, AttributeRegistry.MANA_REGEN, MOD_SPIRIT_MANA_REGEN, bonusRegen, AttributeModifier.Operation.ADD_VALUE);
            }

            applyModifier(player, AttributeRegistry.MAX_MANA, MOD_SPIRIT_MAX_MANA, bonusMax, AttributeModifier.Operation.ADD_VALUE);

            double bonusSpellPower = (spirit - 5) * 0.015; // +/- 1.5% Poder de Hechizo general en Iron's Spells respecto a base 5
            applyModifier(player, AttributeRegistry.SPELL_POWER, MOD_SPIRIT_GENERAL_SPELL_POWER, bonusSpellPower, AttributeModifier.Operation.ADD_VALUE);
        } catch (Throwable t) {
            RpgLevelsMod.LOGGER.error("Error aplicando modificadores de espíritu para Iron's Spells: ", t);
        }
    }

    private static void applyModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double value, AttributeModifier.Operation op) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            if (Math.abs(existing.amount() - value) < 0.0001 && existing.operation() == op) {
                return; // Ya está configurado con este valor exacto; evita churn de atributos
            }
            instance.removeModifier(id);
        }
        if (Math.abs(value) > 0.0001) {
            AttributeModifier modifier = new AttributeModifier(id, value, op);
            instance.addTransientModifier(modifier);
        }
    }

    public static void ensureSpiritualSpellbookUnlocked(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SPELLBOOK.get())) return;
        io.redspace.ironsspellbooks.api.spells.ISpellContainer container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(stack);
        if (container == null) return;

        boolean hasLocked = false;
        for (io.redspace.ironsspellbooks.api.spells.SpellSlot slot : container.getAllSpells()) {
            if (slot != null && slot.isLocked()) {
                hasLocked = true;
                break;
            }
        }

        if (hasLocked) {
            io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable mutable = container.mutableCopy();
            for (int i = 0; i < container.getMaxSpellCount(); i++) {
                io.redspace.ironsspellbooks.api.spells.SpellData data = container.getSpellAtIndex(i);
                if (data != null && data != io.redspace.ironsspellbooks.api.spells.SpellData.EMPTY && data.isLocked()) {
                    mutable.removeSpellAtIndex(i);
                    mutable.addSpellAtIndex(data.getSpell(), data.getLevel(), i, false);
                }
            }
            io.redspace.ironsspellbooks.api.spells.ISpellContainer.set(stack, mutable.toImmutable());
        }
    }

    public static class IronsSpellsEvents {
        @SubscribeEvent
        public void onSpellPreCast(SpellPreCastEvent event) {
            Player player = event.getEntity();
            if (player == null) return;

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null || !data.hasSkill(MagicAdvancementEvents.SKILL_ID)) {
                event.setCanceled(true);
                player.displayClientMessage(
                        Component.literal("§5§k??? §c¡Los sellos arcanos no responden a tu espíritu sin [Afinidad Mágica]! §5§k???")
                                .withStyle(ChatFormatting.RED),
                        true
                );
                player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.PLAYERS,
                        1.0f,
                        0.6f
                );
            }
        }

        @SubscribeEvent
        public void onContainerOpen(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event) {
            Player player = event.getEntity();
            if (player == null || player.level().isClientSide()) return;
            if (event.getContainer() instanceof InscriptionTableMenu menu) {
                ensureSpiritualSpellbookUnlocked(menu.getSpellBookSlot().getItem());
                for (ItemStack item : player.getInventory().items) {
                    ensureSpiritualSpellbookUnlocked(item);
                }
            }
        }

        @SubscribeEvent
        public void onInscribeSpell(InscribeSpellEvent event) {
            Player player = event.getEntity();
            if (player == null || event.getSpellData() == null || event.getSpellData().getSpell() == null) return;

            if (player.containerMenu instanceof InscriptionTableMenu menu) {
                ItemStack bookSlotItem = menu.getSpellBookSlot().getItem();

                // 1. Desbloqueo preventivo automático del Grimorio del Segador para permitir intercambios
                if (bookSlotItem.is(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SPELLBOOK.get())) {
                    ensureSpiritualSpellbookUnlocked(bookSlotItem);
                }

                // 2. La Espada Espiritual NUNCA admite grabados manuales externos: su técnica es innata y eterna
                if (bookSlotItem.is(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SWORD.get())) {
                    event.setCanceled(true);
                    player.displayClientMessage(
                            Component.literal("§c[✦] ¡La Espada Espiritual alberga un alma única! Sus habilidades despertadas son innatas y no pueden ser alteradas en la mesa de inscripción.")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                    player.level().playSound(
                            null,
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK,
                            SoundSource.PLAYERS,
                            1.0f,
                            0.8f
                    );
                    return;
                }

                // 3. Restricción: Las Técnicas Espirituales NO pueden grabarse en libros arcanos comunes
                if (event.getSpellData().getSpell().getSchoolType() == com.pablo.rpglevels.magic.spiritual.SpiritualSchool.SPIRITUAL.get()) {
                    boolean isGrimorio = bookSlotItem.is(com.pablo.rpglevels.items.ModItems.SPIRITUAL_SPELLBOOK.get());
                    if (!isGrimorio) {
                        event.setCanceled(true);
                        player.displayClientMessage(
                                Component.literal("§c[✦] ¡Las Técnicas Espirituales son artes marciales del alma! Solo pueden grabarse en el Grimorio del Segador (3 ranuras).")
                                        .withStyle(ChatFormatting.RED),
                                true
                        );
                        player.level().playSound(
                                null,
                                player.getX(), player.getY(), player.getZ(),
                                SoundEvents.SHIELD_BLOCK,
                                SoundSource.PLAYERS,
                                1.0f,
                                0.8f
                        );
                    }
                }
            }
        }
    }
}
