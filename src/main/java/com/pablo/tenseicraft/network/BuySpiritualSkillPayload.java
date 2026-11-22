package com.pablo.tenseicraft.network;

import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.magic.SpiritualTechnique;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BuySpiritualSkillPayload(String skillId) implements CustomPacketPayload {

    public static final Type<BuySpiritualSkillPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TenseiCraftMod.MOD_ID, "buy_spiritual_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuySpiritualSkillPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    BuySpiritualSkillPayload::skillId,
                    BuySpiritualSkillPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuySpiritualSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data == null) return;

            SpiritualTechnique tech = SpiritualTechnique.byId(payload.skillId());
            if (tech == null || !tech.isShopPurchasable()) return;

            if (data.hasSkill(tech.getId())) {
                player.sendSystemMessage(Component.literal("§eYa has dominado esta técnica espiritual."));
                return;
            }

            int emeraldCost = tech.getEmeraldCost();
            int emeraldsCount = 0;
            for (ItemStack item : player.getInventory().items) {
                if (item.is(Items.EMERALD)) {
                    emeraldsCount += item.getCount();
                }
            }

            if (emeraldsCount < emeraldCost) {
                player.sendSystemMessage(Component.literal("§c[Alice] No tienes suficientes esmeraldas. Requiere " + emeraldCost + " esmeraldas (Tienes: " + emeraldsCount + ")."));
                player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
                return;
            }

            // Deduct emeralds
            int toRemove = emeraldCost;
            for (ItemStack item : player.getInventory().items) {
                if (item.is(Items.EMERALD)) {
                    int take = Math.min(toRemove, item.getCount());
                    item.shrink(take);
                    toRemove -= take;
                    if (toRemove <= 0) break;
                }
            }

            // Unlock skill
            data.unlockSkill(tech.getId());
            ModNetworking.syncToPlayer(player);

            // Deliver Iron's Spells Official Scroll
            io.redspace.ironsspellbooks.api.spells.AbstractSpell spell = switch (tech.getId()) {
                case "physical_boost" -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.PHYSICAL_BOOST.get();
                case "low_healing" -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.LOW_HEALING.get();
                case "low_paralize" -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.LOW_PARALIZE.get();
                case "low_purification" -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.LOW_PURIFICATION.get();
                case "elemental_ball" -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.ELEMENTAL_BALL.get();
                case "channel_magic" -> com.pablo.tenseicraft.magic.spiritual.SpiritualSpells.MAGIC_AFFINITY.get();
                default -> null;
            };

            if (spell != null) {
                ItemStack scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
                io.redspace.ironsspellbooks.api.spells.ISpellContainer.createScrollContainer(spell, 1, scroll);
                if (!player.addItem(scroll)) {
                    player.drop(scroll, false);
                }

                // Sintonización automática en el Grimorio del Segador si el jugador lo porta
                ItemStack bookStack = ItemStack.EMPTY;
                for (ItemStack item : player.getInventory().items) {
                    if (item.is(com.pablo.tenseicraft.items.ModItems.SPIRITUAL_SPELLBOOK.get())) {
                        bookStack = item;
                        break;
                    }
                }
                if (bookStack.isEmpty() && player.getOffhandItem().is(com.pablo.tenseicraft.items.ModItems.SPIRITUAL_SPELLBOOK.get())) {
                    bookStack = player.getOffhandItem();
                }
                if (bookStack.isEmpty() && net.neoforged.fml.ModList.get().isLoaded("curios")) {
                    bookStack = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                            .map(inv -> inv.findFirstCurio(com.pablo.tenseicraft.items.ModItems.SPIRITUAL_SPELLBOOK.get()))
                            .filter(java.util.Optional::isPresent)
                            .map(opt -> opt.get().stack())
                            .orElse(ItemStack.EMPTY);
                }

                if (!bookStack.isEmpty()) {
                    com.pablo.tenseicraft.compat.irons.IronsSpellsCompat.ensureSpiritualSpellbookUnlocked(bookStack);
                    io.redspace.ironsspellbooks.api.spells.ISpellContainer container = io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(bookStack);
                    if (container != null && container.getIndexForSpell(spell) == -1 && container.getNextAvailableIndex() < container.getMaxSpellCount()) {
                        io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable mutable = container.mutableCopy();
                        if (mutable.addSpell(spell, 1, false)) {
                            io.redspace.ironsspellbooks.api.spells.ISpellContainer.set(bookStack, mutable.toImmutable());
                            player.sendSystemMessage(Component.literal("§b⚡ [Grimorio]: ¡La técnica se ha sintonizado en una ranura de acceso rápido [R]!"));
                        }
                    }
                }
            }

            // Celebration sound and particles
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.4f);
            player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT,
                    player.getX(), player.getY() + 1.2, player.getZ(), 40, 0.5, 0.6, 0.5, 0.2);

            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§d[Alice]: §a«¡Excelente disciplina! Has dominado la §e" + tech.getDisplayName() + "§a. He imbuido un pergamino místico para tu espada o grimorio.»"));
            player.sendSystemMessage(Component.literal("§7Secuencia sagrada: §f[ALT IZQ] + §b" + tech.getSequenceDisplay() + " §7| Coste: §b" + (int)tech.getManaCost() + " MP"));
            player.sendSystemMessage(Component.literal("§5✦ Se ha añadido un pergamino espiritual a tu inventario para engarzar en tu Espada Espiritual o Grimorio."));
            player.sendSystemMessage(Component.empty());
        });
    }
}

