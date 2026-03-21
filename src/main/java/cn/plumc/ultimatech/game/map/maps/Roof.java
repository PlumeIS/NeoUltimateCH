package cn.plumc.ultimatech.game.map.maps;

import cn.plumc.ultimatech.game.map.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class Roof extends Map {
    public Roof() {
        super(
                new Map.Region(
                        new BlockPos(-8051, 44, -7899),
                        new BlockPos(-7968, 68, -7864)),
                new Map.Region(
                        new BlockPos(-8035, 58, -7882),
                        new BlockPos(-8032, 60, -7885)),
                new Map.Region(
                        new BlockPos(-7984, 58, -7885),
                        new BlockPos(-7987, 60, -7882)),
                -10,
                13000,
                2
        );
    }

    @Override
    public boolean isLose(ServerPlayer player) {
        return super.isLose(player) ||
                new Region(new BlockPos(114557, 7, 19817), new BlockPos(114557, 7, 19817)).inPos(player.position());
    }
}
