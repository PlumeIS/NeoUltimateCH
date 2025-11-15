package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.UltimateCH;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Objects;

public class GameCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("game")
                .then(Commands.literal("create")
                        .then(Commands.argument("map", StringArgumentType.string())
                                .executes(commandContext -> {
                                    try {
                                        if (Objects.nonNull(UltimateCH.game)) UltimateCH.game.destroy();
                                        UltimateCH.game = new Game(Maps.getMap(StringArgumentType.getString(commandContext, "map")).map);
                                        return 1;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    return 0;
                                })
                        )
                )
                .then(Commands.literal("join")
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(commandContext -> {
                                    if (UltimateCH.game != null) {
                                        Collection<ServerPlayer> players = EntityArgument.getPlayers(commandContext, "player");
                                        players.forEach(serverPlayer -> UltimateCH.game.getPlayerManager().join(serverPlayer));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("leave")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(commandContext -> {
                                    if (UltimateCH.game != null) {
                                        UltimateCH.game.getPlayerManager().leave(EntityArgument.getPlayer(commandContext, "player"));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("start")
                        .executes(commandContext -> {
                            if (UltimateCH.game != null) {
                                UltimateCH.game.gameStart();
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("end")
                        .executes(commandContext -> {
                            if (UltimateCH.game != null) {
                                UltimateCH.game.gameEnd(commandContext.getSource().getPlayer());
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("list")
                        .executes(commandContext -> {
                            commandContext.getSource().getPlayer().sendSystemMessage(Component.literal("玩家列表:"));
                            for (ServerPlayer player : UltimateCH.game.getPlayerManager().getPlayers()) {
                                commandContext.getSource().getPlayer().sendSystemMessage(Component.literal(player.getGameProfile().getName()));
                            }
                            return 1;
                        })
                )
        );
    }
}
