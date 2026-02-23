package cn.plumc.ultimatech.game.map.maps;

import cn.plumc.ultimatech.game.map.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class Roof extends Map {
    public Roof() {
        super(
                new Map.Region(
                        new BlockPos(114578, -41, 19825),
                        new BlockPos(114498, 29, 19794)),
                new Map.Region(
                        new BlockPos(114515, 18, 19811),
                        new BlockPos(114512, 21, 19808)),
                new Map.Region(
                        new BlockPos(114564, 19, 19811),
                        new BlockPos(114561, 22, 19808)),
                -10,
                13000,
                2
        );
    }

    @Override
    public boolean isLose(ServerPlayer player) {
        return super.isLose(player)||
                new Region(new BlockPos(114557,7,19817), new BlockPos(114557,7,19817)).inPos(player.position());
    }
}
