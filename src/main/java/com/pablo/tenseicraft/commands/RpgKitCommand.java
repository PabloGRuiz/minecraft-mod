package com.pablo.tenseicraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.data.AdventurerRank;
import com.pablo.tenseicraft.data.GreekGod;
import com.pablo.tenseicraft.data.ModAttachments;
import com.pablo.tenseicraft.data.PlayerRpgData;
import com.pablo.tenseicraft.data.StatType;
import com.pablo.tenseicraft.events.ModAttributesHandler;
import com.pablo.tenseicraft.items.AdventurerCardItem;
import com.pablo.tenseicraft.items.ModItems;
import com.pablo.tenseicraft.items.SpiritualSwordItem;
import com.pablo.tenseicraft.network.ModNetworking;
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

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class RpgKitCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), "tenseikit");
        register(event.getDispatcher(), "tckit");
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
        // 1. Configurar Progreso RPG: Rango C de Aventurero, Nivel 45 (mínimo de Rango C) y puntos libres sin asignar
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            // Asegurar estadísticas base de nacimiento (1 a 10)
            data.rollBirthStatsIfNeeded(player.getRandom());

            // Desbloqueo divino con Dios Ares por defecto
            data.setDivineBlessingUnlocked(true);
            if (data.getPatronGodId().isEmpty()) {
                data.setPatronGodId(GreekGod.ARES.getId());
            }

            // Establecer Rango C de Aventurero
            data.setRank(AdventurerRank.C);
            data.setGuildRegistered(true);
            data.setReceivedAdventurerCard(true);

            // Nivel RPG 45 (mínimo para estar en Rango C)
            data.setRpgLevel(45);
            data.setRpgXp(0);

            // NO asignar puntos automáticamente: dejar los 44 puntos libres para que el jugador los asigne
            data.resetAllocatedPoints();
            data.setLifePoints(StatType.FORTITUDE, 0);
            data.setLifePoints(StatType.DEXTERITY, 0);
            data.setLifePoints(StatType.SPIRIT, 0);
            data.setLifePoints(StatType.LUCK, 0);
            data.setBonusPendingPoints(0);

            // Actualizar y sincronizar atributos y maná
            ModAttributesHandler.updatePlayerAttributes(player);
            data.setCurrentMana(player, data.getMaxMana(player));
            data.setLastReleasedManaCap(data.getMaxMana(player));
            ModNetworking.syncToPlayer(player);
            ModNetworking.syncMana(player);
        }

        // Sincronizar nivel de experiencia de Minecraft al nivel 45
        player.setExperienceLevels(Math.max(player.experienceLevel, 45));

        // 2. Armadura completa de Hierro y Escudo
        equipOrGive(player, EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        equipOrGive(player, EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        equipOrGive(player, EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        equipOrGive(player, EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        equipOrGive(player, EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        // 3. Entregar Espada Espiritual vinculada con Afinidad al máximo (200/200), lista para despertar pero SIN NOMBRE
        ItemStack kitSword = new ItemStack(ModItems.SPIRITUAL_SWORD.get());
        SpiritualSwordItem.bindSoulVassal(kitSword, player);
        SpiritualSwordItem.setBladeStyle(kitSword, SpiritualSwordItem.BladeStyle.DUELIST);
        SpiritualSwordItem.setBladeElement(kitSword, SpiritualSwordItem.BladeElement.NONE);
        SpiritualSwordItem.setTrialReady(kitSword, true);
        SpiritualSwordItem.setFirstReleaseUnlocked(kitSword, false); // Listo para el rito de nombramiento en el yunque
        SpiritualSwordItem.setAffinity(kitSword, 200); // Afinidad máxima lista para el despertar
        SpiritualSwordItem.setFirstRelease(kitSword, false);
        equipOrGive(player, EquipmentSlot.MAINHAND, kitSword);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
        );

        // 4. Yunque para bautizar y despertar la Espada Espiritual
        giveItem(player, new ItemStack(Items.ANVIL));

        // 5. Comida y manzanas doradas
        giveItem(player, new ItemStack(Items.COOKED_BEEF, 64));
        giveItem(player, new ItemStack(Items.GOLDEN_APPLE, 16));

        // 6. Huevos de Spawn: SOLO Alice (2) y 10 Hoglins
        giveItem(player, new ItemStack(ModItems.ALICE_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(Items.HOGLIN_SPAWN_EGG, 10));

        // 7. Tarjeta de Aventurero (Rango C, Nivel 45) y Grimorio Espiritual
        giveItem(player, new ItemStack(ModItems.SPIRITUAL_SPELLBOOK.get()));
        giveItem(player, AdventurerCardItem.createPlayerCard(player, player.getData(ModAttachments.PLAYER_RPG_DATA)));

        // Efectos sonoros y retroalimentación en chat
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);

        int pendingPoints = (data != null) ? data.getPendingPointsCount() : 44;
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§a§l✦ [TENSEICRAFT KIT DE PRUEBAS ENTREGADO] ✦"));
        player.sendSystemMessage(Component.literal("§6• Rango de Aventurero: §9[RANGO C] §7(Nivel mínimo requerido: 45)"));
        player.sendSystemMessage(Component.literal("§e• Puntos sin asignar: §a" + pendingPoints + " puntos libres §7(Usa §f/tc stats §7o pulsa §f[K] §7para distribuirlos a tu gusto)"));
        player.sendSystemMessage(Component.literal("§b• Armadura: §fSet completo de Hierro + Escudo"));
        player.sendSystemMessage(Component.literal("§d• Espada Espiritual: §eAfinidad Máxima (200/200) §flista para nombrar en el yunque y despertar"));
        player.sendSystemMessage(Component.literal("§c• Huevos: §f2 de Alice y 10 Hoglins salvajes"));
        player.sendSystemMessage(Component.literal("§6• Provisiones: §f64 Ternera cocida y 16 Manzanas doradas"));
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
