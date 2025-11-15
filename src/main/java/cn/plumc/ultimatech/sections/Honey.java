package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.sections.base.Alternative;
import net.minecraft.server.level.ServerPlayer;

public class Honey extends Alternative {
    public Honey(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(5, 7), game);
    }
}
