package cn.plumc.ultimatech;

import cn.plumc.ultimatech.commands.DevelopCommands;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;

public class Lobby {
    public static HashMap<String, Game> games = new HashMap<>();
    public static HashMap<String, LobbyRegion> regions = new HashMap<>();

    public static void tick(MinecraftServer server) {
        LobbyRegion.tick(server);
        DevelopCommands.tick();
        Map.Region secretRegion = new Map.Region(new BlockPos(1, -20, -2), new BlockPos(0, -20, -3));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (secretRegion.inPos(player.position())&&!player.isSpectator()) {
                PlayerUtil.teleport(player, UCHInfos.PLAYER_LOBBY_POINT);
            }
        }
    }
    

    public static void onGameEnd(Game game) {
        games.remove(game.getStatus().mapInfo.id);
    }
}
