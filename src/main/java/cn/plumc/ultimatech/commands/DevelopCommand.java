package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.utils.CommandUtil;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DevelopCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("develop")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (PlayerUtil.containsTag(player, StatusTags.DEVELOPER_TAG)) {
                                player.getTags().remove(StatusTags.DEVELOPER_TAG);
                                player.getTags().remove(StatusTags.NO_JUMP_TAG);
                                player.sendSystemMessage(Component.literal("DEVELOP: OFF"));
                            } else {
                                player.getTags().add(StatusTags.DEVELOPER_TAG);
                                player.getTags().add(StatusTags.NO_JUMP_TAG);
                                player.sendSystemMessage(Component.literal("DEVELOP: ON"));
                            }
                        }
                        return 1;
                    }
                ).then(Commands.literal("test")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        CommandSourceStack source = context.getSource();
                        List<String> commands = List.of("/game create roof",
                                                        "/game join @a",
                                                        "/section tick",
                                                        "/section add ultimatech.beam4",
                                                        "/section view 0",
                                                        "/hub map");
                        CommandUtil.runList(server, source, commands);
                        return 1;
                    })
                ).then(Commands.literal("test1")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        CommandSourceStack source = context.getSource();
                        List<String> commands = List.of("/game create roof",
                                "/game join @a",
                                "/game start");
                        CommandUtil.runList(server, source, commands);
                        return 1;
                    })
                ).then(Commands.literal("test2")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        CommandSourceStack source = context.getSource();
                        List<String> commands = List.of("/section add ultimatech.flippable_floor_stabbing",
                                "/section view 1",
                                "/section place 1");
                        CommandUtil.runList(server, source, commands);
                        return 1;
                    })
                ).then(Commands.literal("clear")
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            CommandSourceStack source = context.getSource();
                            if (source.getPlayer().position().distanceTo(UCHInfos.DEVELOPER_POINT) < 500) return 1;
                            List<String> commands = List.of("/kill @e[type=item_display, distance=..200]",
                                    "/kill @e[type=block_display, distance=..200]",
                                    "/kill @e[type=text_display, distance=..200]",
                                    "/fill 114499 3 19794 114541 29 19833 air replace minecraft:barrier",
                                    "/fill 114499 3 19794 114541 29 19833 air replace minecraft:structure_void",
                                    "/fill 114497 19 19794 114532 30 19830 air");
                            CommandUtil.runList(server, source, commands);
                            return 1;
                        })
                )
        );
    }

}
