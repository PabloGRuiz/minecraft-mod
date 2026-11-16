package com.pablo.rpglevels.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.events.ModAttributesHandler;
import com.pablo.rpglevels.items.AdventurerCardItem;
import com.pablo.rpglevels.items.ModItems;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class RpgKitCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), "rpgkit");
        register(event.getDispatcher(), "kit");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal(commandName)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    giveTestKit(player);
                    return 1;
                });

        dispatcher.register(cmd);
    }

    public static void giveTestKit(ServerPlayer player) {
        // 1. Armadura de Diamante completa y Escudo
        equipOrGive(player, EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        equipOrGive(player, EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        equipOrGive(player, EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        equipOrGive(player, EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        equipOrGive(player, EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        // 2. Espada Espiritual vinculada y lista para probar inmediatamente Primera Liberación y hechizos (150 de Afinidad: ambas técnicas desbloqueadas)
        ItemStack kitSword = new ItemStack(ModItems.SPIRITUAL_SWORD.get());
        com.pablo.rpglevels.items.SpiritualSwordItem.bindSoulVassal(kitSword, player);
        com.pablo.rpglevels.items.SpiritualSwordItem.setFirstReleaseUnlocked(kitSword, true);
        com.pablo.rpglevels.items.SpiritualSwordItem.setAffinity(kitSword, 150);
        com.pablo.rpglevels.items.SpiritualSwordItem.syncAwakenedSpell(kitSword);
        equipOrGive(player, EquipmentSlot.MAINHAND, kitSword);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
        );

        // 3. Yunque para nombrar la espada al instante
        giveItem(player, new ItemStack(Items.ANVIL));

        // 4. Comida y sustento
        giveItem(player, new ItemStack(Items.COOKED_BEEF, 64));
        giveItem(player, new ItemStack(Items.GOLDEN_APPLE, 16));

        // 5. Huevos de Bandidos y Jefes para pruebas de combate
        giveItem(player, new ItemStack(ModItems.BANDIT_SPAWN_EGG.get(), 16));
        giveItem(player, new ItemStack(ModItems.ALICE_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.GUILD_MASTER_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.WIND_MASTER_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.UNDEAD_ADVENTURER_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.ADVENTURER_SPAWN_EGG.get(), 2));

        // 6. Botellas de Gran Experiencia (10x XP cada una)
        giveItem(player, new ItemStack(ModItems.HIGH_EXPERIENCE_BOTTLE.get(), 16));

        // 7. Grimorio del Segador Espiritual y Tarjeta de Aventurero
        giveItem(player, new ItemStack(ModItems.SPIRITUAL_SPELLBOOK.get()));
        giveItem(player, AdventurerCardItem.createPlayerCard(player, player.getData(ModAttachments.PLAYER_RPG_DATA)));

        // 8. Incrementar +15 puntos a todos los atributos RPG
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            for (StatType type : StatType.values()) {
                data.addPermanentPoints(type, 15);
            }
            ModAttributesHandler.updatePlayerAttributes(player);
            ModNetworking.syncToPlayer(player);
        }

        // Efectos de sonido y retroalimentación
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.5F);

        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§a§l✦ [RPG KIT DE PRUEBAS ENTREGADO] ✦"));
        player.sendSystemMessage(Component.literal("§d• Espada Espiritual + Yunque §7(Lista para nombrar y forjar el vínculo)"));
        player.sendSystemMessage(Component.literal("§e• +15 Puntos a todos los Atributos §7(Fortaleza, Destreza, Espíritu, Suerte)"));
        player.sendSystemMessage(Component.literal("§b• Armadura de Diamante completa y Escudo"));
        player.sendSystemMessage(Component.literal("§6• 64 Ternera cocida y 16 Manzanas doradas"));
        player.sendSystemMessage(Component.literal("§c• 16 Huevos de Bandido y Huevos de Jefes"));
        player.sendSystemMessage(Component.literal("§a• 16 Botellas de Gran Experiencia §8(10x XP)"));
        player.sendSystemMessage(Component.literal("§5• Grimorio del Segador Espiritual"));
        player.sendSystemMessage(Component.empty());
    }

    private static void equipOrGive(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, stack);
        } else {
            giveItem(player, stack);
        }
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        boolean added = player.getInventory().add(stack);
        if (!added || !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }
}
