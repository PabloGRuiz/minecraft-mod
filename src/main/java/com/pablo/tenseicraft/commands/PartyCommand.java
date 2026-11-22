package com.pablo.tenseicraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pablo.tenseicraft.TenseiCraftMod;
import com.pablo.tenseicraft.network.ModNetworking;
import com.pablo.tenseicraft.party.Party;
import com.pablo.tenseicraft.party.PartyManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;

@EventBusSubscriber(modid = TenseiCraftMod.MOD_ID)
public class PartyCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), "party");
        register(event.getDispatcher(), "p");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, String rootName) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal(rootName);

        // /party (sin argumentos: abre la interfaz gráfica de grupo)
        cmd.executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ModNetworking.openPartyScreen(player);
            return 1;
        });

        // /party gui
        cmd.then(Commands.literal("gui").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ModNetworking.openPartyScreen(player);
            return 1;
        }));

        // /party create
        cmd.then(Commands.literal("create").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            PartyManager.createParty(player);
            return 1;
        }));

        // /party invite <target>
        cmd.then(Commands.literal("invite")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            PartyManager.invitePlayer(player, target);
                            return 1;
                        })
                )
        );

        // /party accept
        cmd.then(Commands.literal("accept").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            PartyManager.acceptInvite(player);
            return 1;
        }));

        // /party leave
        cmd.then(Commands.literal("leave").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            PartyManager.leaveParty(player);
            return 1;
        }));

        // /party disband
        cmd.then(Commands.literal("disband").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            PartyManager.disbandParty(player);
            return 1;
        }));

        // /party kick <target>
        cmd.then(Commands.literal("kick")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            PartyManager.kickMember(player, target.getUUID());
                            return 1;
                        })
                )
        );

        // /party info
        cmd.then(Commands.literal("info").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            Party party = PartyManager.getParty(player.getUUID());
            if (party == null) {
                player.sendSystemMessage(Component.literal("§cNo formas parte de ningún grupo de aventura. Usa §6/party create §co abre la interfaz con §6/party§c."));
                return 0;
            }

            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§6« ✦ GRUPO DE AVENTURA: " + party.getLeaderName() + " »").withStyle(ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("§7Miembros (" + party.getMemberCount() + "/" + Party.MAX_MEMBERS + "):"));
            for (UUID memberUUID : party.getMemberUUIDs()) {
                String name = party.getMemberNames().getOrDefault(memberUUID, "Desconocido");
                boolean isLead = party.isLeader(memberUUID);
                player.sendSystemMessage(Component.literal("  " + (isLead ? "§6★ [Líder] " : "§7- ") + "§b" + name));
            }
            player.sendSystemMessage(Component.empty());
            return 1;
        }));

        dispatcher.register(cmd);
    }
}

