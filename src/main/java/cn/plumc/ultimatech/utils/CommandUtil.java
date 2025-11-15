package cn.plumc.ultimatech.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class CommandUtil {
    public static void runConsole(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    public static void runPlayer(MinecraftServer server, CommandSourceStack source, String command) {
        server.getCommands().performPrefixedCommand(source, command);
    }

    public static void runList(MinecraftServer server, CommandSourceStack source, List<String> commands) {
        for (String command : commands) {
            server.getCommands().performPrefixedCommand(source, command);
        }
    }
}
