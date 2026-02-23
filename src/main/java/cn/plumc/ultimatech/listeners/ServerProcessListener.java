package cn.plumc.ultimatech.listeners;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.UltimateCH;
import cn.plumc.ultimatech.commands.*;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.section.SectionRegistry;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.io.IOException;
import java.nio.file.Files;

import static cn.plumc.ultimatech.info.UCHInfos.*;

@EventBusSubscriber
public class ServerProcessListener {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        GameCommand.register(event.getDispatcher());
        SectionCommand.register(event.getDispatcher());
        HubCommand.register(event.getDispatcher());
        DevelopCommands.register(event.getDispatcher());
        MotionCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        server.getPlayerList().op(server.getProfileCache().get("Dev").get());
        new SectionRegistry();
        MapInfo.registerMaps();
        createPatch(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Lobby.games.values().forEach(Game::destroy);
    }

    private static void createPatch(MinecraftServer server) {
        MINECRAFT_DIR = server.getServerDirectory().normalize();
        UCH_PATCH = MINECRAFT_DIR.resolve(UltimateCH.MOD_ID);
        CACHED_PATCH = UCH_PATCH.resolve("cache");
        CACHED_SKIN_PATCH = CACHED_PATCH.resolve("skin");
        try {
            Files.createDirectories(UCH_PATCH);
            Files.createDirectories(CACHED_PATCH);
            Files.createDirectories(CACHED_SKIN_PATCH);
        } catch(IOException e) {
            throw new RuntimeException("Couldn't create .minecraft/wurst folder.", e);
        }
    }
}
