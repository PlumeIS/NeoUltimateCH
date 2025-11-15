package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.sections.base.Alternative;
import net.minecraft.server.level.ServerPlayer;

public class Ice2 extends Alternative {
    public Ice2(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(1, 6), game);
    }
}
