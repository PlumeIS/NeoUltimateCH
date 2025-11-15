package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import net.minecraft.server.level.ServerPlayer;

public class Barrel extends Section {
    public Barrel(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(0, 7), game);
    }
}
