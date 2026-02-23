package cn.plumc.ultimatech.game.map.maps;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.section.SectionSerialization;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.List;

public class FragileBridge extends Map {
    public FragileBridge() {
        super(
                new Map.Region(
                        new BlockPos(5950, 46, 5985),
                        new BlockPos(6039, 82, 6024)
                ),
                new Map.Region(
                        new BlockPos(5960, 55, 6001),
                        new BlockPos(5964, 58, 6005)
                ),
                new Map.Region(
                        new BlockPos(6020, 65, 6005),
                        new BlockPos(6016, 68, 6001)
                ),
                46,
                100,
                2
        );
    }

    @Override
    public void startGame(List<ServerPlayer> players) {
        super.startGame(players);
        try {
            SectionSerialization.load(MapInfo.FragileBridge, Lobby.games.get(MapInfo.FragileBridge.id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isLose(ServerPlayer player) {
        return false;
    }
}
