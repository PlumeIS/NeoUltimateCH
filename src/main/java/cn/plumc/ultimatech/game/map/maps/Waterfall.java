package cn.plumc.ultimatech.game.map.maps;

import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;

public class Waterfall extends Map {
    public Waterfall() {
        super(
                new Map.Region(
                        new BlockPos(1991, 70, 1984),
                        new BlockPos(2020, 144, 2060)),
                new Map.Region(
                        new BlockPos(2008, 97, 2042),
                        new BlockPos(2002, 100, 2037)),
                new Map.Region(
                        new BlockPos(2009, 128, 2047),
                        new BlockPos(2002, 134, 2040)),
                69,
                1000,
                2
        );
    }

    @Override
    public void tick() {
        List<ServerPlayer> players = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
        players.forEach(player -> {
            if (player.isInWater()&&PlayerUtil.inPlayerInPlaying(player)) {
                player.setDeltaMovement(player.getDeltaMovement().add(0, -0.8, 0));
                PlayerUtil.updateDeltaMovement(players, player);
            }
        });
    }
}
