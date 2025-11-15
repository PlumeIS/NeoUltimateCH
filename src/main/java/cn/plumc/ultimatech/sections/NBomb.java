package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.sections.base.Remover;
import net.minecraft.server.level.ServerPlayer;

public class NBomb extends Remover {
    public NBomb(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(5, 5), game);
    }
}
