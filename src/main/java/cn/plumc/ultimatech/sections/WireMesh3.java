package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.sections.base.WireMesh;
import net.minecraft.server.level.ServerPlayer;

public class WireMesh3 extends WireMesh {
    public WireMesh3(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(2, 0), game);
    }
}
