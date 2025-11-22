package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.UltimateCH;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.Maps;
import cn.plumc.ultimatech.section.SectionRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class GameCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("game")
                .then(Commands.literal("create")
                        .then(Commands.argument("map", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    try {
                                        String string = StringArgumentType.getString(context, "map");
                                        Arrays.stream(Maps.values()).forEach(map -> {
                                            if (map.id.contains(string)) builder.suggest(map.id);}
                                        );
                                    } catch (IllegalArgumentException ignored) {
                                        Arrays.stream(Maps.values()).map(Maps::getId).forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(commandContext -> {
                                    try {
                                        Maps mapInfo = Maps.getMap(StringArgumentType.getString(commandContext, "map"));
                                        Optional.ofNullable(Lobby.games.remove(mapInfo.id)).ifPresent(Game::destroy);
                                        Lobby.games.put(mapInfo.id, new Game(mapInfo));
                                        return 1;
                                    } catch (Exception e) {
                                        UltimateCH.LOGGER.error("error creating game", e);
                                    }
                                    return 0;
                                })
                        )
                )
                .then(Commands.literal("join")
                        .then(Commands.argument("map", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    try {
                                        String string = StringArgumentType.getString(context, "map");
                                        Arrays.stream(Maps.values()).forEach(map -> {
                                            if (map.id.contains(string)) builder.suggest(map.id);}
                                        );
                                    } catch (IllegalArgumentException ignored) {
                                        Arrays.stream(Maps.values()).map(Maps::getId).forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> {
                                            Maps mapInfo = Maps.getMap(StringArgumentType.getString(commandContext, "map"));
                                            Game game = Lobby.games.get(mapInfo.id);
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(commandContext, "player");
                                            players.forEach(serverPlayer -> game.getPlayerManager().join(serverPlayer));
                                            return 1;
                                        })
                                )
                        )

                )
                .then(Commands.literal("leave")
                        .then(Commands.argument("map", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    try {
                                        String string = StringArgumentType.getString(context, "map");
                                        Arrays.stream(Maps.values()).forEach(map -> {
                                            if (map.id.contains(string)) builder.suggest(map.id);}
                                        );
                                    } catch (IllegalArgumentException ignored) {
                                        Arrays.stream(Maps.values()).map(Maps::getId).forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(commandContext -> {
                                            Maps mapInfo = Maps.getMap(StringArgumentType.getString(commandContext, "map"));
                                            Game game = Lobby.games.get(mapInfo.id);
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(commandContext, "player");
                                            players.forEach(serverPlayer -> game.getPlayerManager().leave(serverPlayer));
                                            return 1;
                                        })
                                )
                        )

                )
                .then(Commands.literal("start")
                        .then(Commands.argument("map", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    try {
                                        String string = StringArgumentType.getString(context, "map");
                                        Arrays.stream(Maps.values()).forEach(map -> {
                                            if (map.id.contains(string)) builder.suggest(map.id);}
                                        );
                                    } catch (IllegalArgumentException ignored) {
                                        Arrays.stream(Maps.values()).map(Maps::getId).forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(commandContext -> {
                                    Maps mapInfo = Maps.getMap(StringArgumentType.getString(commandContext, "map"));
                                    Lobby.games.get(mapInfo.id).gameStart();
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("end")
                        .then(Commands.argument("map", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    try {
                                        String string = StringArgumentType.getString(context, "map");
                                        Arrays.stream(Maps.values()).forEach(map -> {
                                            if (map.id.contains(string)) builder.suggest(map.id);}
                                        );
                                    } catch (IllegalArgumentException ignored) {
                                        Arrays.stream(Maps.values()).map(Maps::getId).forEach(builder::suggest);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(commandContext -> {
                                    Maps mapInfo = Maps.getMap(StringArgumentType.getString(commandContext, "map"));
                                    Lobby.games.get(mapInfo.id).gameEnd(commandContext.getSource().getPlayer());
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("list")
                        .executes(commandContext -> {
                            commandContext.getSource().getPlayer().sendSystemMessage(Component.literal("正在运行的游戏:"));
                            Lobby.games.values().forEach(game -> {
                                commandContext.getSource().getPlayer().sendSystemMessage(Component.literal(game.getStatus().mapInfo.name));
                            });
                            return 1;
                        })
                )
        );
    }
}
