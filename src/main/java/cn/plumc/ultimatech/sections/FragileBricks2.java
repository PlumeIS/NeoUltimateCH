package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.sections.base.FragileBricks;
import net.minecraft.server.level.ServerPlayer;

public class FragileBricks2 extends FragileBricks {
    public FragileBricks2(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(1,2), game);
    }
}
