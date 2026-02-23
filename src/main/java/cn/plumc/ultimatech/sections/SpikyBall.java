package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BlockSurfaceHit;
import net.minecraft.server.level.ServerPlayer;

public class SpikyBall extends Section {
    public SpikyBall(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(2, 3), game);
        setProcess(0);
    }

    @Override
    public void tickRun(int tickTime) {
        killAll(new BlockSurfaceHit(content.getOrigin()).detectPlayers(game));
    }
}
