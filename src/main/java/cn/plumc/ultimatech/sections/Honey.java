package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.layer.LayerType;
import cn.plumc.ultimatech.sections.base.Alternative;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class Honey extends Alternative {
    public Section mover;
    public List<Section> movements = new ArrayList<>();

    public Honey(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(5, 7), game);
    }

    @Override
    public void onRoundRunning() {
        super.onRoundRunning();
        movements.clear();
        BlockPos pos = new BlockPos(Mth.floor(content.origin.x), Mth.floor(content.origin.y), Mth.floor(content.origin.z));
        List<Section> middles = content.manager.getSectionsByPos(pos, LayerType.MIDDLE);
        List<Section> list = middles.stream().filter(section -> section != this).toList();
        if (!list.isEmpty()) {
            mover = list.get(0);
            mover.setOnMove(this);
        }
        for (Direction direction : Direction.values()) {
            BlockPos relative = pos.relative(direction);
            List<Section> sections = game.getSectionManager().getSectionsByPos(relative, LayerType.MIDDLE);
            if (!sections.isEmpty()) {
                movements.addAll(sections.stream().filter(Section::isStatic).toList());
            }
        }
    }

    @Override
    public void onRoundEnd() {
        movements.forEach((section) -> section.setRunningLayer(LayerType.MIDDLE));
    }

    @Override
    public void onMove(Vec3i movement) {
        content.move(movement.getX(), movement.getY(), movement.getZ());
        movements.forEach((section) -> section.content.move(movement.getX(), movement.getY(), movement.getZ()));
    }
}
