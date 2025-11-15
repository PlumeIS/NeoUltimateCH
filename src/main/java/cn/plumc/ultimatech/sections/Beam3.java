package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import net.minecraft.server.level.ServerPlayer;

public class Beam3 extends Section {
    public Beam3(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(0, 2), game);
    }
}
