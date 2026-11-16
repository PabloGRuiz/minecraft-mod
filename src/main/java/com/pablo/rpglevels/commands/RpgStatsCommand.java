package com.pablo.rpglevels.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pablo.rpglevels.RpgLevelsMod;
import com.pablo.rpglevels.data.AdventurerRank;
import com.pablo.rpglevels.data.ModAttachments;
import com.pablo.rpglevels.data.PlayerRpgData;
import com.pablo.rpglevels.data.StatType;
import com.pablo.rpglevels.data.ThreatLevel;
import com.pablo.rpglevels.events.LevelCheckEvents;
import com.pablo.rpglevels.events.ModAttributesHandler;
import com.pablo.rpglevels.items.SpiritualSwordItem;
import com.pablo.rpglevels.network.ModNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.pablo.rpglevels.data.AuraStatHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = RpgLevelsMod.MOD_ID)
public class RpgStatsCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), "rpgstats");
        register(event.getDispatcher(), "rpglevels");
        register(event.getDispatcher(), "rpg");

        // Registrar atajos directos en el comando /locate de Minecraft para autocompletar la Casa del Gremio
        try {
            var locateNode = event.getDispatcher().getRoot().getChild("locate");
            if (locateNode != null) {
                locateNode.addChild(Commands.literal("guild")
                        .requires(s -> s.hasPermission(2))
                        .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))
                        .build());
                locateNode.addChild(Commands.literal("guild_house")
                        .requires(s -> s.hasPermission(2))
                        .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))
                        .build());
                locateNode.addChild(Commands.literal("gremio")
                        .requires(s -> s.hasPermission(2))
                        .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))
                        .build());

                var structNode = locateNode.getChild("structure");
                if (structNode != null) {
                    structNode.addChild(Commands.literal("guild")
                            .requires(s -> s.hasPermission(2))
                            .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))
                            .build());
                    structNode.addChild(Commands.literal("guild_house")
                            .requires(s -> s.hasPermission(2))
                            .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))
                            .build());
                    structNode.addChild(Commands.literal("gremio")
                            .requires(s -> s.hasPermission(2))
                            .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))
                            .build());
                }
            }
        } catch (Throwable t) {
            RpgLevelsMod.LOGGER.warn("Failed to inject /locate shortcuts: {}", t.getMessage());
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, String rootCommand) {
        LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal(rootCommand)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return executeInfo(player);
                });

        // ==========================================
        // 1. CORE PLAYER ACTIONS (Root shortcuts)
        // ==========================================

        // /<root> info -> View stats sheet
        cmd.then(Commands.literal("info")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return executeInfo(player);
                }));

        // /<root> choose <stat> [milestone] -> Allocate milestone attribute point
        cmd.then(Commands.literal("choose")
                .then(Commands.argument("stat", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"fortaleza", "destreza", "espiritu", "suerte"}, b))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String statName = StringArgumentType.getString(context, "stat");
                            return executeChoose(player, statName);
                        })
                        .then(Commands.argument("milestone", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String statName = StringArgumentType.getString(context, "stat");
                                    return executeChoose(player, statName);
                                }))));

        // /<root> ascend -> Perform rank ascension check & promotion
        cmd.then(Commands.literal("ascend")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return executeAscend(player);
                }));

        // /<root> help -> Interactive command manual
        cmd.then(Commands.literal("help")
                .executes(context -> executeHelp(context.getSource())));

        // ==========================================
        // 2. STATS CATEGORY: /<root> stats <info|add|set|reset>
        // ==========================================
        LiteralArgumentBuilder<CommandSourceStack> statsNode = Commands.literal("stats")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return executeInfo(player);
                })
                .then(Commands.literal("info")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return executeInfo(player);
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    return executeInfo(target);
                                })))
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"fortaleza", "destreza", "espiritu", "suerte"}, b))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String statName = StringArgumentType.getString(context, "stat");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            return executeAddStats(context.getSource(), Collections.singleton(player), statName, amount);
                                        })
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> {
                                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                                    String statName = StringArgumentType.getString(context, "stat");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    return executeAddStats(context.getSource(), players, statName, amount);
                                                })))))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("stat", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"fortaleza", "destreza", "espiritu", "suerte"}, b))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String statName = StringArgumentType.getString(context, "stat");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            return executeSetStat(context.getSource(), Collections.singleton(player), statName, amount);
                                        })
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(context -> {
                                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                                    String statName = StringArgumentType.getString(context, "stat");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    return executeSetStat(context.getSource(), players, statName, amount);
                                                })))))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return executeReset(context.getSource(), Collections.singleton(player));
                        })
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                    return executeReset(context.getSource(), players);
                                })));
        cmd.then(statsNode);

        // ==========================================
        // 3. POINTS CATEGORY: /<root> points <add|set|<amount>>
        // ==========================================
        LiteralArgumentBuilder<CommandSourceStack> pointsNode = Commands.literal("points")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    return executeAddPoints(context.getSource(), Collections.singleton(player), amount);
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            return executeAddPoints(context.getSource(), players, amount);
                                        }))))
                .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                    return executeSetPoints(context.getSource(), Collections.singleton(player), amount);
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                            int amount = IntegerArgumentType.getInteger(context, "amount");
                                            return executeSetPoints(context.getSource(), players, amount);
                                        }))))
                // Direct shortcut: /<root> points <amount>
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int amount = IntegerArgumentType.getInteger(context, "amount");
                            return executeAddPoints(context.getSource(), Collections.singleton(player), amount);
                        }));
        cmd.then(pointsNode);

        // ==========================================
        // 4. SKILLS CATEGORY: /<root> skills <add|reset>
        // ==========================================
        LiteralArgumentBuilder<CommandSourceStack> skillsNode = Commands.literal("skills")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amt = IntegerArgumentType.getInteger(context, "amount");
                                    PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                                    if (data != null) {
                                        data.addPhysicalSkillPoints(amt);
                                        ModNetworking.syncToPlayer(player);
                                        player.sendSystemMessage(Component.literal("§a+ " + amt + " Puntos de Habilidad Física otorgados. Total: " + data.getPhysicalSkillPoints()));
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            int amt = IntegerArgumentType.getInteger(context, "amount");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                            for (ServerPlayer target : players) {
                                                PlayerRpgData data = target.getData(ModAttachments.PLAYER_RPG_DATA);
                                                if (data != null) {
                                                    data.addPhysicalSkillPoints(amt);
                                                    ModNetworking.syncToPlayer(target);
                                                    target.sendSystemMessage(Component.literal("§a+ " + amt + " Puntos de Habilidad Física otorgados. Total: " + data.getPhysicalSkillPoints()));
                                                }
                                            }
                                            return players.size();
                                        }))))
                .then(Commands.literal("reset")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                            if (data != null) {
                                data.resetPhysicalSkills();
                                ModAttributesHandler.updatePlayerAttributes(player);
                                ModNetworking.syncToPlayer(player);
                                player.sendSystemMessage(Component.literal("§eÁrbol de Habilidades Físicas reiniciado. Puntos reembolsados: " + data.getPhysicalSkillPoints()));
                            }
                            return 1;
                        })
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                    for (ServerPlayer target : players) {
                                        PlayerRpgData data = target.getData(ModAttachments.PLAYER_RPG_DATA);
                                        if (data != null) {
                                            data.resetPhysicalSkills();
                                            ModAttributesHandler.updatePlayerAttributes(target);
                                            ModNetworking.syncToPlayer(target);
                                            target.sendSystemMessage(Component.literal("§eÁrbol de Habilidades Físicas reiniciado. Puntos reembolsados: " + data.getPhysicalSkillPoints()));
                                        }
                                    }
                                    return players.size();
                                })))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"physical_boost", "low_healing", "low_paralize", "low_purification", "elemental_ball", "channel_magic", "fast_healing", "all"}, b))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String skill = StringArgumentType.getString(context, "skill");
                                    PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                                    if (data != null) {
                                        if ("all".equalsIgnoreCase(skill)) {
                                            for (com.pablo.rpglevels.magic.SpiritualTechnique tech : com.pablo.rpglevels.magic.SpiritualTechnique.values()) {
                                                data.unlockSkill(tech.getId());
                                            }
                                            data.unlockSkill("magic_affinity");
                                            ModNetworking.syncToPlayer(player);
                                            player.sendSystemMessage(Component.literal("§a¡Todas las 7 Técnicas Espirituales desbloqueadas!"));
                                        } else {
                                            data.unlockSkill(skill);
                                            data.unlockSkill("magic_affinity");
                                            ModNetworking.syncToPlayer(player);
                                            player.sendSystemMessage(Component.literal("§aTécnica " + skill + " desbloqueada."));
                                        }
                                    }
                                    return 1;
                                })));
        cmd.then(skillsNode);

        // ==========================================
        // 5. RANK CATEGORY: /<root> rank <set|ascend|<rank>>
        // ==========================================
        LiteralArgumentBuilder<CommandSourceStack> rankNode = Commands.literal("rank")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"F", "E", "D", "C", "B", "A"}, b))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String rankName = StringArgumentType.getString(context, "rank");
                                    return executeSetRank(context.getSource(), Collections.singleton(player), rankName);
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
                                            String rankName = StringArgumentType.getString(context, "rank");
                                            return executeSetRank(context.getSource(), players, rankName);
                                        }))))
                .then(Commands.literal("ascend")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            return executeAscend(player);
                        }))
                // Direct shortcut: /<root> rank <F..A>
                .then(Commands.argument("rank", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"F", "E", "D", "C", "B", "A"}, b))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String rankName = StringArgumentType.getString(context, "rank");
                            return executeSetRank(context.getSource(), Collections.singleton(player), rankName);
                        }));
        cmd.then(rankNode);

        // ==========================================
        // 6. REPUTATION CATEGORY: /<root> reputation <get|add|set>
        // ==========================================
        LiteralArgumentBuilder<CommandSourceStack> repNode = Commands.literal("reputation")
                .then(Commands.literal("get")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
                            if (data != null) {
                                context.getSource().sendSuccess(() -> Component.literal("§eTu reputación actual es: ")
                                        .append(data.getReputationTier().getDisplayName())
                                        .append(Component.literal(" §7(" + data.getReputationScore() + "/100)")), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("target", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    PlayerRpgData data = target.getData(ModAttachments.PLAYER_RPG_DATA);
                                    if (data != null) {
                                        context.getSource().sendSuccess(() -> Component.literal("§eReputación de " + target.getName().getString() + ": ")
                                                .append(data.getReputationTier().getDisplayName())
                                                .append(Component.literal(" §7(" + data.getReputationScore() + "/100)")), false);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("add")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amt = IntegerArgumentType.getInteger(context, "amount");
                                    return executeAddReputation(context.getSource(), Collections.singleton(player), amt);
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            int amt = IntegerArgumentType.getInteger(context, "amount");
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                            return executeAddReputation(context.getSource(), targets, amt);
                                        }))))
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(-100, 100))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int amt = IntegerArgumentType.getInteger(context, "amount");
                                    return executeSetReputation(context.getSource(), Collections.singleton(player), amt);
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> {
                                            int amt = IntegerArgumentType.getInteger(context, "amount");
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
                                            return executeSetReputation(context.getSource(), targets, amt);
                                        }))));
        cmd.then(repNode);

        // ==========================================
        // 7. SWORD CATEGORY: /<root> sword ...
        // ==========================================
        cmd.then(buildSwordSubcommand("sword"));

        // ==========================================
        // 8. WORLD / DEBUG CATEGORY: /<root> world <rift|alice|ambush|trial|irregular|kit>
        // ==========================================
        LiteralArgumentBuilder<CommandSourceStack> worldNode = Commands.literal("world")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("rift")
                        .executes(context -> executeToggleRift(context.getSource().getPlayerOrException()))
                        .then(Commands.literal("enter").executes(context -> {
                            com.pablo.rpglevels.world.dimension.AbyssTeleportHelper.enterAbyssRift(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                        .then(Commands.literal("exit").executes(context -> {
                            com.pablo.rpglevels.world.dimension.AbyssTeleportHelper.exitAbyssRift(context.getSource().getPlayerOrException());
                            return 1;
                        })))
                .then(Commands.literal("alice")
                        .executes(context -> executeAliceSpawn(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.literal("spawn").executes(context -> executeAliceSpawn(context.getSource(), context.getSource().getPlayerOrException()))))
                .then(Commands.literal("ambush")
                        .executes(context -> executeAmbush(context.getSource(), context.getSource().getPlayerOrException(), false))
                        .then(Commands.literal("revenge")
                                .executes(context -> executeAmbush(context.getSource(), context.getSource().getPlayerOrException(), true))))
                .then(Commands.literal("irregular")
                        .executes(context -> executeIrregular(context.getSource(), context.getSource().getPlayerOrException())))
                .then(Commands.literal("kit")
                        .executes(context -> {
                            RpgKitCommand.giveTestKit(context.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("trial")
                        .then(Commands.literal("war")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    com.pablo.rpglevels.world.trial.WarGodTrialManager.startTrial(player);
                                    return 1;
                                })))
                .then(Commands.literal("guild")
                        .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException())));
        cmd.then(worldNode);

        // ==========================================
        // 9. DIRECT SHORTCUTS (Convenience & Backwards Compatibility)
        // ==========================================
        cmd.then(Commands.literal("kit")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    RpgKitCommand.giveTestKit(context.getSource().getPlayerOrException());
                    return 1;
                }));

        cmd.then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeReset(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException())))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> executeReset(context.getSource(), EntityArgument.getPlayers(context, "targets")))));

        cmd.then(Commands.literal("affinity")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeSetAffinity(context.getSource().getPlayerOrException(), 100, false))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0, 200))
                        .executes(context -> executeSetAffinity(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "amount"), false)))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 200))
                                .executes(context -> executeSetAffinity(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "amount"), true)))));

        cmd.then(Commands.literal("rift")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeToggleRift(context.getSource().getPlayerOrException())));

        cmd.then(Commands.literal("alice")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeAliceSpawn(context.getSource(), context.getSource().getPlayerOrException())));

        cmd.then(Commands.literal("ambush")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeAmbush(context.getSource(), context.getSource().getPlayerOrException(), false))
                .then(Commands.literal("revenge")
                        .executes(context -> executeAmbush(context.getSource(), context.getSource().getPlayerOrException(), true))));

        cmd.then(Commands.literal("irregular")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeIrregular(context.getSource(), context.getSource().getPlayerOrException())));

        cmd.then(Commands.literal("locate")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("guild")
                        .executes(context -> executeLocateGuild(context.getSource(), context.getSource().getPlayerOrException()))));

        cmd.then(Commands.literal("aura")
                .executes(context -> executeAuraCheck(context.getSource(), null))
                .then(Commands.argument("target", EntityArgument.entity())
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> executeAuraCheck(context.getSource(), EntityArgument.getEntity(context, "target")))));

        dispatcher.register(cmd);
    }

    private static final List<String> SWORD_STYLE_SUGGESTIONS = List.of("duelist", "frenzy", "bulwark", "phantom", "duelista", "frenesi", "baluarte", "fantasma");
    private static final List<String> SWORD_ELEMENT_SUGGESTIONS = List.of("ignis", "fulgur", "glacies", "umbra");
    private static final List<String> SWORD_RELEASE_SUGGESTIONS = List.of("slash", "vortex", "step", "overdrive", "corte", "vortice", "paso", "trance");
    private static final List<String> SWORD_RESONANCE_SUGGESTIONS = List.of("spirit", "fortitude", "dexterity", "luck", "espiritu", "fortaleza", "destreza", "suerte");

    private static LiteralArgumentBuilder<CommandSourceStack> buildSwordSubcommand(String literalName) {
        return Commands.literal(literalName)
                .requires(source -> source.hasPermission(2))
                // /rpg sword -> shows held or inventory sword info
                .executes(ctx -> executeSwordInfo(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("info")
                        .executes(ctx -> executeSwordInfo(ctx.getSource().getPlayerOrException())))
                // /rpg sword affinity [amount|add <amount>]
                .then(Commands.literal("affinity")
                        .executes(ctx -> executeSetAffinity(ctx.getSource().getPlayerOrException(), 100, false))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 200))
                                .executes(ctx -> executeSetAffinity(
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "amount"),
                                        false)))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 200))
                                        .executes(ctx -> executeSetAffinity(
                                                ctx.getSource().getPlayerOrException(),
                                                IntegerArgumentType.getInteger(ctx, "amount"),
                                                true)))))
                // /rpg sword toggle -> toggle First Release active state
                .then(Commands.literal("toggle")
                        .executes(ctx -> executeToggleHeldSword(ctx.getSource().getPlayerOrException())))
                // /rpg sword first_release <bool>
                .then(Commands.literal("first_release")
                        .then(Commands.argument("active", BoolArgumentType.bool())
                                .executes(ctx -> executeToggleFirstRelease(
                                        ctx.getSource().getPlayerOrException(),
                                        BoolArgumentType.getBool(ctx, "active")))))
                // /rpg sword release <technique>
                .then(Commands.literal("release")
                        .then(Commands.argument("release", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_RELEASE_SUGGESTIONS, builder))
                                .executes(ctx -> executeReleaseSword(
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "release")))))
                // /rpg sword element <element>
                .then(Commands.literal("element")
                        .then(Commands.argument("element", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_ELEMENT_SUGGESTIONS, builder))
                                .executes(ctx -> executeImbueSword(
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "element")))))
                // /rpg sword style <style>
                .then(Commands.literal("style")
                        .then(Commands.argument("style", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_STYLE_SUGGESTIONS, builder))
                                .executes(ctx -> executeStyleSword(
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "style")))))
                // /rpg sword variant [1|2]
                .then(Commands.literal("variant")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int cur = SpiritualSwordItem.getBladeVariant(p.getMainHandItem());
                            return executeVariantSword(p, (cur == 0 ? 2 : 1));
                        })
                        .then(Commands.argument("number", IntegerArgumentType.integer(1, 2))
                                .executes(ctx -> executeVariantSword(
                                        ctx.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(ctx, "number")))))
                // /rpg sword resonance <resonance>
                .then(Commands.literal("resonance")
                        .then(Commands.argument("resonance", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_RESONANCE_SUGGESTIONS, builder))
                                .executes(ctx -> executeResonanceSword(
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "resonance")))))
                // /rpg sword give <style> [element] [release] [resonance]
                .then(Commands.literal("give")
                        .then(Commands.argument("style", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_STYLE_SUGGESTIONS, builder))
                                .executes(ctx -> executeGiveSword(
                                        ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "style"),
                                        "ignis", "slash", "spirit"))
                                .then(Commands.argument("element", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_ELEMENT_SUGGESTIONS, builder))
                                        .executes(ctx -> executeGiveSword(
                                                ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "style"),
                                                StringArgumentType.getString(ctx, "element"),
                                                "slash", "spirit"))
                                        .then(Commands.argument("release", StringArgumentType.word())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_RELEASE_SUGGESTIONS, builder))
                                                .executes(ctx -> executeGiveSword(
                                                        ctx.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(ctx, "style"),
                                                        StringArgumentType.getString(ctx, "element"),
                                                        StringArgumentType.getString(ctx, "release"),
                                                        "spirit"))
                                                .then(Commands.argument("resonance", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(SWORD_RESONANCE_SUGGESTIONS, builder))
                                                        .executes(ctx -> executeGiveSword(
                                                                ctx.getSource().getPlayerOrException(),
                                                                StringArgumentType.getString(ctx, "style"),
                                                                StringArgumentType.getString(ctx, "element"),
                                                                StringArgumentType.getString(ctx, "release"),
                                                                StringArgumentType.getString(ctx, "resonance"))))))))
                // /rpg sword all
                .then(Commands.literal("all")
                        .executes(ctx -> executeGiveAllSwords(ctx.getSource().getPlayerOrException())))
                // /rpg sword reroll -> reset and reroll random soul traits for this world
                .then(Commands.literal("reroll")
                        .executes(ctx -> executeRerollSword(ctx.getSource().getPlayerOrException())));
    }

    private static int executeSwordInfo(ServerPlayer player) {
        ItemStack sword = findHeldOrInventorySpiritualSword(player);
        if (sword.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo sostienes ni tienes en tu inventario una Espada Espiritual."));
            return 0;
        }
        SpiritualSwordItem.BladeStyle style = SpiritualSwordItem.getBladeStyle(sword);
        SpiritualSwordItem.BladeElement elem = SpiritualSwordItem.getBladeElement(sword);
        SpiritualSwordItem.BladeRelease rel = SpiritualSwordItem.getBladeRelease(sword);
        SpiritualSwordItem.BladeResonance res = SpiritualSwordItem.getBladeResonance(sword);
        int affinity = SpiritualSwordItem.getAffinity(sword);
        boolean released = SpiritualSwordItem.isFirstRelease(sword);
        boolean unlocked = SpiritualSwordItem.isFirstReleaseUnlocked(sword);

        player.sendSystemMessage(Component.literal("§6═════════ [ Estado de tu Espada Espiritual ] ═════════"));
        player.sendSystemMessage(Component.literal("§7• Nombre: ").append(sword.getHoverName()));
        player.sendSystemMessage(Component.literal("§7• Afinidad: §e" + affinity + " / 100 " + (affinity >= 100 ? "§a(¡Máxima alcanzada!)" : "")));
        player.sendSystemMessage(Component.literal("§7• Estilo de Combate: ").append(Component.translatable(style.getTranslationKey()).withStyle(style.getColor(), ChatFormatting.BOLD)));
        int variant = SpiritualSwordItem.getBladeVariant(sword);
        String variantStr = (variant == 1) ? "§6Variante II (Alternativa)" : "§bVariante I (Clásica)";
        player.sendSystemMessage(Component.literal("§7• Aspecto Visual: " + variantStr));
        player.sendSystemMessage(Component.literal("§7• Imbuición Elemental: ").append(Component.translatable(elem.getTranslationKey()).withStyle(elem.getColor(), ChatFormatting.BOLD)));
        player.sendSystemMessage(Component.literal("§7• Resonancia de Atributo: ").append(Component.translatable(res.getTranslationKey()).withStyle(res.getColor(), ChatFormatting.BOLD)));
        player.sendSystemMessage(Component.literal("§7• 1ª Liberación: ").append(Component.translatable(rel.getTranslationKey()).withStyle(rel.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(unlocked ? " §a[Superada y Desbloqueada]" : " §c[Bloqueada - Desafía a tu Espíritu]"))
                .append(Component.literal(released ? " §e(Estado: Activa)" : " §7(Estado: Inactiva)")));
        player.sendSystemMessage(Component.literal("§6═══════════════════════════════════════════════════"));
        return 1;
    }

    private static int executeToggleHeldSword(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal."));
            return 0;
        }
        boolean current = SpiritualSwordItem.isFirstRelease(held);
        return executeToggleFirstRelease(player, !current);
    }

    private static int executeToggleRift(ServerPlayer player) {
        if (com.pablo.rpglevels.world.dimension.ModDimensions.isAbyssRift(player.level())) {
            com.pablo.rpglevels.world.dimension.AbyssTeleportHelper.exitAbyssRift(player);
        } else {
            com.pablo.rpglevels.world.dimension.AbyssTeleportHelper.enterAbyssRift(player);
        }
        return 1;
    }

    private static int executeAliceSpawn(CommandSourceStack source, ServerPlayer player) {
        boolean spawned = com.pablo.rpglevels.events.AliceVisitingSpawner.trySpawnAliceNearPlayer(player, true);
        if (spawned) {
            source.sendSuccess(() -> Component.literal("§a[Alice] ¡Alice ha llegado exitosamente cerca de tu ubicación!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[Alice] No se encontró una posición segura para que Alice aparezca. Intenta en un área despejada."));
            return 0;
        }
    }

    private static int executeAmbush(CommandSourceStack source, ServerPlayer player, boolean isRevenge) {
        boolean started = com.pablo.rpglevels.mobs.BanditAmbushManager.triggerAmbush(player, isRevenge, isRevenge ? 5 : 0);
        if (started) {
            source.sendSuccess(() -> Component.literal(isRevenge ? "§4¡Emboscada de Venganza Nemesis iniciada con éxito!" : "§a¡Emboscada de bandidos iniciada con éxito!"), true);
        }
        return started ? 1 : 0;
    }

    private static int executeGiveSword(ServerPlayer player, String styleStr, String elemStr, String releaseStr, String resonanceStr) {
        SpiritualSwordItem.BladeStyle style = SpiritualSwordItem.BladeStyle.byId(styleStr);
        if (style == null) {
            player.sendSystemMessage(Component.literal("§cEstilo inválido. Opciones: §eduelist, frenzy, bulwark, phantom§c."));
            return 0;
        }
        SpiritualSwordItem.BladeElement element = SpiritualSwordItem.BladeElement.byId(elemStr);
        if (element == null) {
            player.sendSystemMessage(Component.literal("§cElemento inválido. Opciones: §eignis, fulgur, glacies, umbra§c."));
            return 0;
        }
        SpiritualSwordItem.BladeRelease release = SpiritualSwordItem.BladeRelease.byId(releaseStr);
        if (release == null) {
            player.sendSystemMessage(Component.literal("§cTécnica de 1ª Liberación inválida. Opciones: §eslash (corte), vortex (vortice), step (paso), overdrive (trance)§c."));
            return 0;
        }
        SpiritualSwordItem.BladeResonance resonance = SpiritualSwordItem.BladeResonance.byId(resonanceStr);
        if (resonance == null) {
            player.sendSystemMessage(Component.literal("§cResonancia inválida. Opciones: §espirit, fortitude, dexterity, luck§c."));
            return 0;
        }

        ItemStack sword = SpiritualSwordItem.createCustomSword(player, style, element, release, resonance);
        if (!player.getInventory().add(sword)) {
            player.drop(sword, false);
        }

        SpiritualSwordItem.spawnElementalImpactBurst(player.serverLevel(), player, element);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §aRecibiste: ").append(sword.getHoverName())
                .append(Component.literal(" §7| 1ª Liberación: ")).append(Component.translatable(release.getTranslationKey()).withStyle(release.getColor(), ChatFormatting.BOLD)));
        return 1;
    }

    private static int executeToggleFirstRelease(ServerPlayer player, boolean active) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal."));
            return 0;
        }
        SpiritualSwordItem.setFirstRelease(held, active);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.2f);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §ePrimera Liberación: " + (active ? "§aActivada" : "§cDesactivada")));
        return 1;
    }

    private static int executeReleaseSword(ServerPlayer player, String releaseStr) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal."));
            return 0;
        }
        SpiritualSwordItem.BladeRelease release = SpiritualSwordItem.BladeRelease.byId(releaseStr);
        if (release == null) {
            player.sendSystemMessage(Component.literal("§cTécnica de 1ª Liberación inválida. Opciones: §eslash (corte), vortex (vortice), step (paso), overdrive (trance)§c."));
            return 0;
        }
        SpiritualSwordItem.setBladeRelease(held, release);
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            data.setSoulBladeRelease(release.getId());
        }
        player.playNotifySound(SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9f, 1.4f);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §eTécnica de 1ª Liberación cambiada a: ")
                .append(Component.translatable(release.getTranslationKey()).withStyle(release.getColor(), ChatFormatting.BOLD))
                .append(Component.literal(" §7(Usa Agachado + Clic Derecho)")));
        return 1;
    }

    private static int executeImbueSword(ServerPlayer player, String elemStr) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal."));
            return 0;
        }
        SpiritualSwordItem.BladeElement element = SpiritualSwordItem.BladeElement.byId(elemStr);
        if (element == null) {
            player.sendSystemMessage(Component.literal("§cElemento inválido. Opciones: §eignis, fulgur, glacies, umbra§c."));
            return 0;
        }
        SpiritualSwordItem.setBladeElement(held, element);
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            data.setSoulBladeElement(element.getId());
        }
        SpiritualSwordItem.spawnElementalImpactBurst(player.serverLevel(), player, element);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §eElemento imbuido: ")
                .append(Component.translatable(element.getTranslationKey()).withStyle(element.getColor(), ChatFormatting.BOLD)));
        return 1;
    }

    private static int executeStyleSword(ServerPlayer player, String styleStr) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal."));
            return 0;
        }
        SpiritualSwordItem.BladeStyle style = SpiritualSwordItem.BladeStyle.byId(styleStr);
        if (style == null) {
            player.sendSystemMessage(Component.literal("§cEstilo inválido. Opciones: §eduelist, frenzy, bulwark, phantom§c."));
            return 0;
        }
        SpiritualSwordItem.setBladeStyle(held, style);
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            data.setSoulBladeStyle(style.getId());
        }
        player.playNotifySound(SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7f, 1.4f);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §eEstilo cambiado a: ")
                .append(Component.translatable(style.getTranslationKey()).withStyle(style.getColor(), ChatFormatting.BOLD)));
        return 1;
    }

    private static int executeVariantSword(ServerPlayer player, int variant) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal."));
            return 0;
        }
        int safe = (variant == 2) ? 1 : 0;
        SpiritualSwordItem.setBladeVariant(held, safe);
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            data.setSoulBladeVariant(safe);
        }
        player.playNotifySound(SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        String variantStr = (safe == 1) ? "§6Variante II (Alternativa)" : "§bVariante I (Clásica)";
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §eAspecto de piel/forma cambiado a: " + variantStr));
        return 1;
    }

    private static int executeResonanceSword(ServerPlayer player, String resStr) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal."));
            return 0;
        }
        SpiritualSwordItem.BladeResonance resonance = SpiritualSwordItem.BladeResonance.byId(resStr);
        if (resonance == null) {
            player.sendSystemMessage(Component.literal("§cResonancia inválida. Opciones: §espirit, fortitude, dexterity, luck§c."));
            return 0;
        }
        SpiritualSwordItem.setBladeResonance(held, resonance);
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            data.setSoulBladeResonance(resonance.getId());
        }
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §eResonancia cambiada a: ")
                .append(Component.translatable(resonance.getTranslationKey()).withStyle(resonance.getColor(), ChatFormatting.BOLD)));
        return 1;
    }

    private static int executeRerollSword(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof SpiritualSwordItem)) {
            player.sendSystemMessage(Component.literal("§cDebes sostener una §bEspada Espiritual §cen tu mano principal para reinicializarla."));
            return 0;
        }
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data != null) {
            data.resetSoulBladeTraits();
        }
        SpiritualSwordItem.imprintSoulBlade(held, player);
        player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §a¡Has reiniciado y vuelto a forjar aleatoriamente el alma de tu espada para esta partida!"));
        return 1;
    }

    private static int executeGiveAllSwords(ServerPlayer player) {
        // Variante 1 (Clásicas)
        ItemStack s1 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.DUELIST, SpiritualSwordItem.BladeElement.IGNIS,
                SpiritualSwordItem.BladeRelease.SLASH, SpiritualSwordItem.BladeResonance.SPIRIT, 0);
        ItemStack s2 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.FRENZY, SpiritualSwordItem.BladeElement.FULGUR,
                SpiritualSwordItem.BladeRelease.STEP, SpiritualSwordItem.BladeResonance.DEXTERITY, 0);
        ItemStack s3 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.BULWARK, SpiritualSwordItem.BladeElement.GLACIES,
                SpiritualSwordItem.BladeRelease.VORTEX, SpiritualSwordItem.BladeResonance.FORTITUDE, 0);
        ItemStack s4 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.PHANTOM, SpiritualSwordItem.BladeElement.UMBRA,
                SpiritualSwordItem.BladeRelease.OVERDRIVE, SpiritualSwordItem.BladeResonance.LUCK, 0);

        // Variante 2 (Alternativas)
        ItemStack s1_v2 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.DUELIST, SpiritualSwordItem.BladeElement.IGNIS,
                SpiritualSwordItem.BladeRelease.SLASH, SpiritualSwordItem.BladeResonance.SPIRIT, 1);
        ItemStack s2_v2 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.FRENZY, SpiritualSwordItem.BladeElement.FULGUR,
                SpiritualSwordItem.BladeRelease.STEP, SpiritualSwordItem.BladeResonance.DEXTERITY, 1);
        ItemStack s3_v2 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.BULWARK, SpiritualSwordItem.BladeElement.GLACIES,
                SpiritualSwordItem.BladeRelease.VORTEX, SpiritualSwordItem.BladeResonance.FORTITUDE, 1);
        ItemStack s4_v2 = SpiritualSwordItem.createCustomSword(player,
                SpiritualSwordItem.BladeStyle.PHANTOM, SpiritualSwordItem.BladeElement.UMBRA,
                SpiritualSwordItem.BladeRelease.OVERDRIVE, SpiritualSwordItem.BladeResonance.LUCK, 1);

        for (ItemStack stack : List.of(s1, s2, s3, s4, s1_v2, s2_v2, s3_v2, s4_v2)) {
            SpiritualSwordItem.setFirstRelease(stack, true);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
        player.sendSystemMessage(Component.literal("§b§l[Espada Espiritual] §aSe te han entregado las §e8 Espadas Espirituales §a(4 estilos en sus Variantes Clásica y Alternativa)§a."));
        return 1;
    }

    private static ItemStack findHeldOrInventorySpiritualSword(ServerPlayer player) {
        if (player.getMainHandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().getItem() instanceof SpiritualSwordItem) {
            return player.getOffhandItem();
        }
        for (ItemStack item : player.getInventory().items) {
            if (item.getItem() instanceof SpiritualSwordItem) {
                return item;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int executeSetAffinity(ServerPlayer player, int amount, boolean isAdd) {
        ItemStack sword = findHeldOrInventorySpiritualSword(player);
        if (sword.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo tienes ninguna Espada Espiritual en tu mano ni en tu inventario."));
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1.0f);
            return 0;
        }

        if (!SpiritualSwordItem.isSoulBound(sword)) {
            SpiritualSwordItem.bindSoulVassal(sword, player);
            player.sendSystemMessage(Component.literal("§d✦ Espada vinculada automáticamente a tu alma."));
        }

        int current = SpiritualSwordItem.getAffinity(sword);
        int maxAffinity = SpiritualSwordItem.getMaxAffinity(sword);
        int newAffinity = isAdd ? Math.min(maxAffinity, Math.max(0, current + amount)) : Math.min(maxAffinity, Math.max(0, amount));
        SpiritualSwordItem.setAffinity(sword, newAffinity);
        SpiritualSwordItem.syncAwakenedSpell(sword);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player,
                new io.redspace.ironsspellbooks.network.EquipmentChangedPacket()
        );

        if (newAffinity >= 200) {
            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§d✦ §l[ESPADA ESPIRITUAL] §6¡Has dominado por completo tu Primera Liberación (200/200)!"));
            player.sendSystemMessage(Component.literal("§eEl drenaje espiritual ahora es mínimo (0.8 MP/s)."));
            player.sendSystemMessage(Component.empty());
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else if (newAffinity >= 100 && !SpiritualSwordItem.isFirstReleaseUnlocked(sword)) {
            player.sendSystemMessage(Component.empty());
            player.sendSystemMessage(Component.literal("§d✦ §l[ESPADA ESPIRITUAL] §f¡Afinidad máxima alcanzada (100/100)!"));
            player.sendSystemMessage(Component.literal("§eLos efectos elementales están completamente despiertos. Consulta con §bAlice §epara desafiar al Espíritu de tu Espada."));
            player.sendSystemMessage(Component.empty());

            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.6, 0.4, 0.15);
        } else {
            player.sendSystemMessage(Component.literal("§aAfinidad de la Espada Espiritual establecida en: §e" + newAffinity + " / " + maxAffinity));
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        }

        return 1;
    }

    private static int executeChoose(ServerPlayer player, String statName) {
        StatType statType = StatType.fromString(statName);
        if (statType == null) {
            player.sendSystemMessage(Component.literal("§cEstadística inválida. Opciones: fortaleza (fortitude), destreza (dexterity), espiritu (spirit), suerte (luck)."));
            return 0;
        }

        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return 0;
        }

        if (data.getPendingPointsCount(player.experienceLevel) < 1) {
            player.sendSystemMessage(Component.literal("§cNo tienes puntos de atributos pendientes por asignar."));
            return 0;
        }

        // Allocate 1 point
        boolean success = data.allocatePoint(statType, player.experienceLevel);
        if (!success) {
            player.sendSystemMessage(Component.literal("§cNo tienes suficientes puntos pendientes."));
            return 0;
        }

        // Refresh player attributes dynamically
        ModAttributesHandler.updatePlayerAttributes(player);
        ModNetworking.syncToPlayer(player);
        ModNetworking.syncMana(player);

        // Feedback
        int pendingRemaining = data.getPendingPointsCount(player.experienceLevel);
        Component statLabel = Component.translatable(statType.getTranslationKey());
        player.sendSystemMessage(Component.literal("§a+1 a ").append(statLabel).append(Component.literal(". §e(Puntos pendientes restantes: §a" + pendingRemaining + "§e)")));
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.85f, 1.2f);

        return 1;
    }

    private static int executeInfo(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return 0;
        }

        int currentLvl = player.experienceLevel;
        player.sendSystemMessage(Component.literal("§6=== [Tus Estadísticas RPG] ==="));
        if (data.isGuildRegistered()) {
            if (data.isCriminal()) {
                ThreatLevel threat = ThreatLevel.getThreatForPlayer(player, data);
                player.sendSystemMessage(Component.literal("§cEstado: §4[Fugitivo/Criminal] §7| ")
                        .append(threat.getBadge()).append(Component.literal(" §e| Nivel XP: §f" + currentLvl)));
            } else {
                player.sendSystemMessage(Component.literal("§eRango: ").append(data.getRank().getBadge())
                        .append(Component.literal(" §7(Multiplicador Rep: x" + data.getRank().getReputationMultiplier() + ") §e| Nivel XP: §f" + currentLvl)));
            }
        } else {
            if (data.isCriminal()) {
                ThreatLevel threat = ThreatLevel.getThreatForPlayer(player, data);
                player.sendSystemMessage(Component.literal("§cEstado: §4[Criminal No Registrado] §7| ")
                        .append(threat.getBadge()).append(Component.literal(" §e| Nivel XP: §f" + currentLvl)));
            } else {
                player.sendSystemMessage(Component.literal("§7Estado: §f[Civil No Registrado en el Gremio] §e| Nivel XP: §f" + currentLvl));
            }
        }

        for (StatType stat : StatType.values()) {
            int totalPoints = data.getTotalStatPoints(player, stat);
            int permPoints = data.getPermanentPoints(stat);
            int activeLifePoints = data.getActiveMilestonePoints(stat, currentLvl);
            int curiosBonus = data.getCuriosBonus(player, stat);
            Component statName = Component.translatable(stat.getTranslationKey());

            String bonusDesc = switch (stat) {
                case FORTITUDE -> "+" + (totalPoints * 2) + " HP, +" + String.format("%.1f", totalPoints * 0.5) + " Armadura, " + String.format("%.1f", Math.min(70.0, totalPoints * 0.7)) + "% Desvío";
                case DEXTERITY -> "+" + (totalPoints * 4) + "% Vel. Ataque, +" + (totalPoints * 4) + "% Daño Rango, +" + (totalPoints * 2) + "% Vel";
                case SPIRIT -> "+" + String.format("%.1f", totalPoints * 0.1) + "% Multiplicador XP Extra";
                case LUCK -> "+" + (totalPoints * 2) + "% Prob. Crítico, +" + (totalPoints * 3) + "% Botín Drops";
            };

            String detail = " §7(Perm: +" + permPoints + " | Nivel: +" + activeLifePoints + (curiosBonus > 0 ? " | Curios: +" + curiosBonus : "") + ")";
            player.sendSystemMessage(Component.literal(" " + stat.getIcon() + " ").append(statName).append(Component.literal(": §a" + totalPoints + " pts §e[" + bonusDesc + "]")).append(Component.literal(detail)));
        }

        int allocated = data.getAllocatedPointsCount();
        int maxEarned = data.getTotalEarnedPoints(currentLvl);
        int pending = data.getPendingPointsCount(currentLvl);
        player.sendSystemMessage(Component.literal("§7Puntos de Atributos (Vida Actual): §a" + allocated + " §7asignados / §e" + maxEarned + " §7desbloqueados (§b" + pending + " pendientes§7)"));
        player.sendSystemMessage(Component.literal("§7Puntos de Habilidad Física: §6" + data.getPhysicalSkillPoints() + " disponibles §7| Habilidades aprendidas: §f" + data.getUnlockedSkills().size()));

        com.pablo.rpglevels.data.ReputationTier repTier = data.getReputationTier();
        String repScoreStr = (data.getReputationScore() > 0 ? "+" : "") + data.getReputationScore();
        player.sendSystemMessage(Component.literal("§eMoralidad & Reputación: ").append(repTier.getDisplayName())
                .append(Component.literal(" §7(" + repScoreStr + "/100)")));

        return 1;
    }

    private static int executeAscend(ServerPlayer player) {
        PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
        if (data == null) {
            return 0;
        }

        if (!data.isGuildRegistered()) {
            player.sendSystemMessage(Component.literal("§cDebes inscribirte en el Gremio de Aventureros con un Guild Master en una aldea antes de poder ascender de rango."));
            return 0;
        }

        if (data.getRank().isMaxRank()) {
            player.sendSystemMessage(Component.translatable("rpglevels.ascension.max_rank"));
            return 0;
        }

        // Verify Guild Master proximity
        var nearbyMasters = player.serverLevel().getEntitiesOfClass(
                com.pablo.rpglevels.entities.GuildMasterNpcEntity.class,
                player.getBoundingBox().inflate(8.0)
        );
        if (nearbyMasters.isEmpty()) {
            player.sendSystemMessage(Component.literal("§c¡Para ascender de rango debes presentarte en persona ante un Maestro del Gremio en cualquier aldea!"));
            return 0;
        }

        int reqLevel = data.getRank().getRequiredAscensionLevel();
        if (player.experienceLevel < reqLevel) {
            player.sendSystemMessage(Component.literal("§cRequieres Nivel " + reqLevel + " de XP para ascender al siguiente rango (Nivel actual: " + player.experienceLevel + ")."));
            return 0;
        }

        // Perform ascension
        boolean success = data.performAscension();
        if (!success) {
            return 0;
        }

        // Reset experience to 0
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);

        // Update attributes and sync
        ModAttributesHandler.updatePlayerAttributes(player);
        ModNetworking.syncToPlayer(player);

        // Visual & Sound effects
        var level = player.serverLevel();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 0.5, 0.5, 0.2);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 0.5, player.getZ(), 40, 0.5, 0.3, 0.5, 0.1);
        player.playNotifySound(SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);

        // Announcement
        Component rankBadge = data.getRank().getBadge();
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.translatable("rpglevels.ascension.success.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.translatable("rpglevels.ascension.success.desc", rankBadge).withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.empty());

        return 1;
    }

    private static int executeReset(CommandSourceStack source, Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.resetProgress();
                ModAttributesHandler.updatePlayerAttributes(player);
                ModNetworking.syncToPlayer(player);

                player.sendSystemMessage(Component.literal("§c§l[RPG Levels] §eTu progreso RPG (Rango, Hitos y Stats) ha sido reiniciado a cero."));
                player.playNotifySound(SoundEvents.ANVIL_BREAK, SoundSource.PLAYERS, 0.7f, 1.0f);
            }
        }
        source.sendSuccess(() -> Component.literal("§aProgreso RPG reiniciado exitosamente para " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeSetRank(CommandSourceStack source, Collection<ServerPlayer> targets, String rankName) {
        AdventurerRank targetRank = null;
        for (AdventurerRank r : AdventurerRank.values()) {
            if (r.getName().equalsIgnoreCase(rankName)) {
                targetRank = r;
                break;
            }
        }

        if (targetRank == null) {
            source.sendFailure(Component.literal("§cRango inválido. Rangos disponibles: F, E, D, C, B, A"));
            return 0;
        }

        final AdventurerRank finalRank = targetRank;
        for (ServerPlayer player : targets) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.setRank(finalRank);
                data.setGuildRegistered(true);
                ModNetworking.syncToPlayer(player);
                player.sendSystemMessage(Component.literal("§6§l[RPG Levels] §eTu rango de aventurero ha sido establecido en: ").append(finalRank.getBadge()));
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
        source.sendSuccess(() -> Component.literal("§aRango establecido en " + finalRank.getName() + " para " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeAddStats(CommandSourceStack source, Collection<ServerPlayer> targets, String statName, int amount) {
        StatType statType = StatType.fromString(statName);
        if (statType == null) {
            source.sendFailure(Component.literal("§cEstadística inválida. Opciones: fortaleza (fortitude), destreza (dexterity), espiritu (spirit), suerte (luck)"));
            return 0;
        }

        for (ServerPlayer player : targets) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.addPermanentPoints(statType, amount);
                ModAttributesHandler.updatePlayerAttributes(player);
                ModNetworking.syncToPlayer(player);
                player.sendSystemMessage(Component.literal("§6§l[RPG Levels] §aHas recibido +" + amount + " puntos permanentes en " + statType.getId() + "."));
            }
        }
        source.sendSuccess(() -> Component.literal("§aAñadidos +" + amount + " puntos en " + statType.getId() + " a " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeSetStat(CommandSourceStack source, Collection<ServerPlayer> targets, String statName, int amount) {
        StatType statType = StatType.fromString(statName);
        if (statType == null) {
            source.sendFailure(Component.literal("§cEstadística inválida. Opciones: fortaleza (fortitude), destreza (dexterity), espiritu (spirit), suerte (luck)"));
            return 0;
        }

        for (ServerPlayer player : targets) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.setPermanentPoints(statType, amount);
                ModAttributesHandler.updatePlayerAttributes(player);
                ModNetworking.syncToPlayer(player);
                player.sendSystemMessage(Component.literal("§6§l[RPG Levels] §aTu estadística permanente en " + statType.getId() + " ha sido establecida en " + amount + "."));
            }
        }
        source.sendSuccess(() -> Component.literal("§aEstadística " + statType.getId() + " establecida en " + amount + " para " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeAddPoints(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.addBonusPendingPoints(amount);
                ModNetworking.syncToPlayer(player);
                int pending = data.getPendingPointsCount(player.experienceLevel);
                player.sendSystemMessage(Component.literal("§6§l[RPG Levels] §aHas recibido +" + amount + " puntos de atributos por asignar. (Pendientes: §e" + pending + "§a)."));
                player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 1.3f);
            }
        }
        source.sendSuccess(() -> Component.literal("§aAñadidos +" + amount + " puntos pendientes para " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeSetPoints(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.setBonusPendingPoints(amount);
                ModNetworking.syncToPlayer(player);
                int pending = data.getPendingPointsCount(player.experienceLevel);
                player.sendSystemMessage(Component.literal("§6§l[RPG Levels] §aTus puntos de atributos pendientes han sido establecidos en: §e" + pending + "§a."));
            }
        }
        source.sendSuccess(() -> Component.literal("§aPuntos pendientes establecidos en " + amount + " para " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeSetReputation(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            PlayerRpgData data = player.getData(ModAttachments.PLAYER_RPG_DATA);
            if (data != null) {
                data.setReputationScore(amount);
                ModNetworking.syncToPlayer(player);
                player.sendSystemMessage(Component.literal("§6§l[RPG Levels] §eTu reputación ha sido fijada en: ")
                        .append(data.getReputationTier().getDisplayName())
                        .append(Component.literal(" §7(" + amount + "/100)")));
            }
        }
        source.sendSuccess(() -> Component.literal("§aReputación establecida en " + amount + " para " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeAddReputation(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
        for (ServerPlayer player : targets) {
            com.pablo.rpglevels.events.ReputationEvents.modifyReputation(player, amount, "rpglevels.reputation.reason.admin_grant");
        }
        source.sendSuccess(() -> Component.literal("§aAñadidos " + amount + " de reputación a " + targets.size() + " jugador(es)."), true);
        return targets.size();
    }

    private static int executeIrregular(CommandSourceStack source, ServerPlayer player) {
        net.minecraft.world.phys.Vec3 eyePos = player.getEyePosition();
        net.minecraft.world.phys.Vec3 viewVec = player.getViewVector(1.0f);
        net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(8.0);
        List<net.minecraft.world.entity.Mob> nearbyMobs = player.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, net.minecraft.world.entity.LivingEntity::isAlive);
        net.minecraft.world.entity.Mob targetMob = null;
        double bestDot = 0.5;
        for (net.minecraft.world.entity.Mob mob : nearbyMobs) {
            net.minecraft.world.phys.Vec3 toMob = mob.position().subtract(eyePos).normalize();
            double dot = viewVec.dot(toMob);
            if (dot > bestDot) {
                bestDot = dot;
                targetMob = mob;
            }
        }

        if (targetMob != null) {
            com.pablo.rpglevels.mobs.IrregularMobHandler.makeIrregular(targetMob);
            net.minecraft.world.entity.Mob finalMob = targetMob;
            source.sendSuccess(() -> Component.literal("§a¡" + finalMob.getName().getString() + " ha sido transformado en Monstruo Irregular!"), true);
            return 1;
        }

        // Otherwise spawn an Irregular Zombie directly in front of the player
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        net.minecraft.world.entity.monster.Zombie zombie = new net.minecraft.world.entity.monster.Zombie(level);
        net.minecraft.world.phys.Vec3 spawnPos = eyePos.add(viewVec.scale(3.0));
        zombie.moveTo(spawnPos.x, spawnPos.y, spawnPos.z, player.getYRot(), 0.0f);
        com.pablo.rpglevels.mobs.IrregularMobHandler.makeIrregular(zombie);
        level.addFreshEntity(zombie);

        source.sendSuccess(() -> Component.literal("§a¡Zombie Irregular generado frente a ti!"), true);
        return 1;
    }

    private static int executeLocateGuild(CommandSourceStack source, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        try {
            var structReg = level.registryAccess().registry(Registries.STRUCTURE);
            if (structReg.isEmpty()) {
                source.sendFailure(Component.literal("§cRegistro de estructuras no disponible."));
                return 0;
            }

            ResourceLocation guildRl = ResourceLocation.fromNamespaceAndPath(RpgLevelsMod.MOD_ID, "guild_house");
            var guildKey = ResourceKey.create(Registries.STRUCTURE, guildRl);
            var holderOpt = structReg.get().getHolder(guildKey);

            if (holderOpt.isEmpty()) {
                source.sendFailure(Component.literal("§cEstructura rpglevels:guild_house no encontrada en el registro."));
                return 0;
            }

            HolderSet<Structure> holderSet = HolderSet.direct(holderOpt.get());
            com.mojang.datafixers.util.Pair<BlockPos, Holder<Structure>> result =
                    level.getChunkSource().getGenerator().findNearestMapStructure(level, holderSet, player.blockPosition(), 100, false);

            if (result == null || result.getFirst() == null) {
                source.sendFailure(Component.literal("§cNo se encontró ninguna Casa del Gremio (" + guildRl + ") en un radio de 100 chunks."));
                return 0;
            }

            BlockPos pos = result.getFirst();
            int distance = (int) Math.round(Math.sqrt(player.blockPosition().distSqr(pos)));

            Component tpComponent = Component.literal("§e[" + pos.getX() + ", ~, " + pos.getZ() + "]")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " ~ " + pos.getZ()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§aClic para sugerir comando de teletransporte"))));

            source.sendSuccess(() -> Component.literal("§6§l[Gremio] §a¡Casa del Gremio encontrada en ")
                    .append(tpComponent)
                    .append(Component.literal(" §7(a §e" + distance + " §7bloques de distancia)!")), false);
            source.sendSuccess(() -> Component.literal("§7Nota: También puedes usar el comando vanilla: §b/locate structure rpglevels:guild_house"), false);
            return 1;
        } catch (Throwable t) {
            RpgLevelsMod.LOGGER.error("Error al localizar Casa del Gremio: ", t);
            source.sendFailure(Component.literal("§cError al buscar la Casa del Gremio: " + t.getMessage()));
            return 0;
        }
    }

    private static int executeHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "§6═══════════ [ SISTEMA DE COMANDOS RPG ] ═══════════\n" +
                "§e/rpg §7o §e/rpg info §f- Ver tu hoja de estadísticas RPG\n" +
                "§e/rpg choose <fortaleza|destreza|espiritu|suerte> §f- Asignar punto de atributo\n" +
                "§e/rpg ascend §f- Solicitar ascensión de rango ante el Gremio\n" +
                "\n§6[Categorías Unificadas]:\n" +
                "§b/rpg stats <info|add|set|reset> §f- Gestión de atributos y niveles\n" +
                "§b/rpg points <add|set|<cant>> §f- Puntos pendientes de asignar\n" +
                "§b/rpg skills <add|reset> §f- Árbol de habilidades físicas y acrobacias\n" +
                "§b/rpg rank <set|ascend|<rango>> §f- Rango de aventurero (F a A)\n" +
                "§b/rpg reputation <get|add|set> §f- Reputación y moralidad (-100 a +100)\n" +
                "§b/rpg sword <info|affinity|toggle|release|element|style|resonance|give|all> §f- Espada Espiritual\n" +
                "§b/rpg world <rift|alice|ambush|trial|irregular|kit> §f- Eventos de mundo y pruebas\n" +
                "\n§7Atajos directos: §8/rpg points <cant>, /rpg rank <F..A>, /rpg affinity <0..100>, /rpg kit, /rpg aura"
        ), false);
        return 1;
    }

    private static int executeAuraCheck(CommandSourceStack source, Entity explicitTarget) {
        ServerPlayer player = null;
        try {
            player = source.getPlayerOrException();
        } catch (Exception ignored) {}

        LivingEntity target = null;
        if (explicitTarget instanceof LivingEntity living) {
            target = living;
        } else if (player != null) {
            // Raycast what the player is looking at (up to 32 blocks)
            target = findTargetedEntity(player, 32.0);
            if (target == null) {
                target = player; // Si no mira a ninguna criatura, analiza su propia aura
            }
        }

        if (target == null) {
            source.sendFailure(Component.literal("§cNo se encontró ninguna criatura viva para analizar su aura."));
            return 0;
        }

        AuraStatHelper.AuraBreakdown b = AuraStatHelper.getAuraBreakdown(target);
        String name = target.getDisplayName().getString();
        String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();

        LivingEntity finalTarget = target;
        source.sendSuccess(() -> Component.literal("§5========================================"), false);
        source.sendSuccess(() -> Component.literal("§d✦ ANÁLISIS DE AURA ESPIRITUAL ✦").withStyle(ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("§fEntidad: §e" + name + " §7(ID: " + typeId + ")"), false);
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "§6• Fortaleza Eq.:   §f%.1f §7(HP: %.0f/%.0f | KB: %.2f)", b.fortitude(), finalTarget.getHealth(), finalTarget.getMaxHealth(), finalTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE))), false);
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "§a• Destreza Eq.:    §f%.1f §7(Velocidad: %.3f)", b.dexterity(), finalTarget.getAttributeValue(Attributes.MOVEMENT_SPEED))), false);
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "§d• Espíritu Eq.:    §f%.1f §7(Poder Místico y Amenaza)", b.spirit())), false);
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "§b• Suerte/Def. Eq.: §f%.1f §7(Armadura: %.1f | Dureza: %.1f)", b.luck(), (double) finalTarget.getArmorValue(), finalTarget.getAttributeValue(Attributes.ARMOR_TOUGHNESS))), false);
        if (b.presenceBonus() > 0.0f) {
            source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "§e• Masa/Presencia:  §f+%.1f §7(Volumen físico colosal)", b.presenceBonus())), false);
        }
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "§5★ AURA TOTAL: §d§l%.1f §5puntos de alma", b.totalAura())), false);

        if (player != null && target != player) {
            PlayerRpgData pData = player.getData(ModAttachments.PLAYER_RPG_DATA);
            float playerAura = (pData != null) ? pData.getAuraStat(player) : 1.0f;
            float diff = playerAura - b.totalAura();
            String effectDesc;
            if (diff <= 0) {
                effectDesc = "§aResiste completamente (Aura superior o igual a la tuya)";
            } else if (diff < 15.0f) {
                effectDesc = "§eLentitud I + 1.5 Daño/s (Presión leve)";
            } else if (diff < 35.0f) {
                effectDesc = "§6Lentitud II + 3.0 Daño/s (Presión notable)";
            } else if (diff < 60.0f) {
                effectDesc = "§cLentitud III + Debilidad I + 5.0 Daño/s (Presión aplastante)";
            } else {
                effectDesc = "§4§lLentitud IV + Debilidad II + Ceguera + 8.0 Daño/s (Colapso Sensorial)";
            }
            source.sendSuccess(() -> Component.literal("§7----------------------------------------"), false);
            source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "§fTu Aura: §b%.1f §7| Diferencia: §eΔ = %+.1f", playerAura, diff)), false);
            source.sendSuccess(() -> Component.literal("§5✦ Reacción a tu Aura Opresiva: " + effectDesc), false);
        }
        source.sendSuccess(() -> Component.literal("§5========================================"), false);

        return 1;
    }

    private static LivingEntity findTargetedEntity(ServerPlayer player, double maxDist) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(viewVec.scale(maxDist));
        AABB box = player.getBoundingBox().expandTowards(viewVec.scale(maxDist)).inflate(2.0);
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player.level(), player, eyePos, reachVec, box,
                e -> e instanceof LivingEntity && !e.isSpectator()
        );
        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
