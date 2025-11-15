package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import net.minecraft.server.level.ServerPlayer;

public class Stair extends Section {
    public Stair(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(4, 7), game);
    }
}
