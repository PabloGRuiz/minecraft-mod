package com.pablo.rpglevels.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.GreekGod;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.events.ModAttributesHandler;
import com.pablo.rpglevels.items.AdventurerCardItem;
import com.pablo.rpglevels.items.ModItems;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
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
        // 1. Configurar Progreso RPG: Rango A de Aventurero, Nivel 90 y 89 Puntos repartidos equitativamente
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            // Asegurar estadísticas base de nacimiento
            data.rollBirthStatsIfNeeded(player.getRandom());

            // Desbloqueo divino
            data.setDivineBlessingUnlocked(true);
            if (data.getPatronGodId().isEmpty()) {
                data.setPatronGodId(GreekGod.ARES.getId());
            }

            // Establecer Rango A de Aventurero (Tope de aventurero / Midgame)
            data.setRank(AdventurerRank.A);
            data.setGuildRegistered(true);
            data.setReceivedAdventurerCard(true);

            // Nivel RPG 90
            data.setRpgLevel(90);
            data.setRpgXp(0);
            data.resetAllocatedPoints();
            data.setBonusPendingPoints(0);

            // Repartir 89 puntos de forma equitativa entre los 4 atributos RPG (22 c/u + 1 a Fortaleza)
            data.setLifePoints(StatType.FORTITUDE, 23);
            data.setLifePoints(StatType.DEXTERITY, 22);
            data.setLifePoints(StatType.SPIRIT, 22);
            data.setLifePoints(StatType.LUCK, 22);

            // Actualizar y sincronizar atributos y maná
            ModAttributesHandler.updatePlayerAttributes(player);
            data.setCurrentMana(player, data.getMaxMana(player));
            data.setLastReleasedManaCap(data.getMaxMana(player));
            ModNetworking.syncToPlayer(player);
            ModNetworking.syncMana(player);
        }

        // Sincronizar nivel de experiencia de Minecraft a 90
        player.setExperienceLevels(Math.max(player.experienceLevel, 90));

        // 2. Armadura de Diamante completa y Escudo
        equipOrGive(player, EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        equipOrGive(player, EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        equipOrGive(player, EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        equipOrGive(player, EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        equipOrGive(player, EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));

        // 3. Entregar Zanpakuto Senbonzakura (Afinidad 200/200, Primera Liberación Despertada y Activa con los 3 Hechizos)
        ItemStack kitSword = new ItemStack(ModItems.SPIRITUAL_SWORD.get());
        kitSword.set(DataComponents.CUSTOM_NAME, Component.literal("Senbonzakura"));
        SpiritualSwordItem.bindSoulVassal(kitSword, player);
        SpiritualSwordItem.setBladeStyle(kitSword, SpiritualSwordItem.BladeStyle.PHANTOM);
        SpiritualSwordItem.setBladeElement(kitSword, SpiritualSwordItem.BladeElement.NONE);
        SpiritualSwordItem.setFirstReleaseUnlocked(kitSword, true);
        SpiritualSwordItem.setTrialReady(kitSword, true);
        SpiritualSwordItem.setAffinity(kitSword, 200);
        SpiritualSwordItem.setFirstRelease(kitSword, true);
        SpiritualSwordItem.syncAwakenedSpell(kitSword);
        equipOrGive(player, EquipmentSlot.MAINHAND, kitSword);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
        );

        // 4. Yunque
        giveItem(player, new ItemStack(Items.ANVIL));

        // 5. Comida y sustento
        giveItem(player, new ItemStack(Items.COOKED_BEEF, 64));
        giveItem(player, new ItemStack(Items.GOLDEN_APPLE, 16));

        // 6. Huevos de Bandidos y Jefes para pruebas de combate
        giveItem(player, new ItemStack(ModItems.BANDIT_SPAWN_EGG.get(), 16));
        giveItem(player, new ItemStack(ModItems.ALICE_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.GUILD_MASTER_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.WIND_MASTER_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.UNDEAD_ADVENTURER_SPAWN_EGG.get(), 2));
        giveItem(player, new ItemStack(ModItems.ADVENTURER_SPAWN_EGG.get(), 2));

        // 7. Botellas de Gran Experiencia (10x XP cada una)
        giveItem(player, new ItemStack(ModItems.HIGH_EXPERIENCE_BOTTLE.get(), 16));

        // 8. Grimorio del Segador Espiritual y Tarjeta de Aventurero (Rango A, Nivel 90)
        giveItem(player, new ItemStack(ModItems.SPIRITUAL_SPELLBOOK.get()));
        giveItem(player, AdventurerCardItem.createPlayerCard(player, player.getData(ModAttachments.PLAYER_RPG_DATA)));

        // Efectos de sonido y retroalimentación épicos
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.5F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("§a§l✦ [RPG KIT DE PRUEBAS ENTREGADO] ✦"));
        player.sendSystemMessage(Component.literal("§d• Zanpakuto: Senbonzakura §e(Afinidad 200/200 Máxima, Shikai Despertado y Activo)"));
        player.sendSystemMessage(Component.literal("§6• Rango de Aventurero: §e[RANGO A] §7(Tope de aventurero y umbral al Midgame)"));
        player.sendSystemMessage(Component.literal("§e• Nivel RPG 90: §f89 Puntos repartidos equitativamente (Fortaleza: 23, Destreza: 22, Espíritu: 22, Suerte: 22)"));
        player.sendSystemMessage(Component.literal("§b• Armadura de Diamante completa y Escudo"));
        player.sendSystemMessage(Component.literal("§6• 64 Ternera cocida y 16 Manzanas doradas"));
        player.sendSystemMessage(Component.literal("§c• 16 Huevos de Bandido y Huevos de Jefes"));
        player.sendSystemMessage(Component.literal("§a• 16 Botellas de Gran Experiencia §8(10x XP)"));
        player.sendSystemMessage(Component.literal("§5• Grimorio del Segador Espiritual + Tarjeta de Aventurero Rango A"));
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

