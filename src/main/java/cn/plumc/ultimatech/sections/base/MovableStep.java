package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionContent;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.section.layer.LayerType;
import cn.plumc.ultimatech.utils.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;

public class MovableStep extends Configurable {
    public Vec3 position = new Vec3(0.0, 0.0, 0.0);
    public Vec3 movement = new Vec3(0.0, 0.0, 1.0);
    public BoxHit.Relative blocks;
    public BoxHit.Relative blocksCurrent;
    public List<Section> movers = new ArrayList<>();
    public List<BlockPos> topChanged = new ArrayList<>();


    public MovableStep(ServerPlayer player, SectionLocation location ,Game game) {
        super(player, location, game);
    }

    public void move(Vec3 velocity){
        SectionContent.SectionEntities entities = content.entities;
        for (Tuple<Vec3, Entity> entry : entities.entities) {
            Vec3 pos = entry.getA();
            Entity entity = entry.getB();
            transform.moveEntityAbsolute(entity, position.add(pos), 0.05);
        }

        for (BlockPos pos : BlockUtil.getBlocksFromAABB(blocksCurrent.getAABB())) {
            content.manager.getTopLayer().set(pos, Blocks.BARRIER.defaultBlockState());
            topChanged.add(pos);
        }

        movers.forEach((section) -> section.onMove(new Vec3i(Mth.floor(position.x), Mth.floor(position.y), Mth.floor(position.z))));

        Vec3 rotatedVector = transform.rotateVector(velocity);
        blocksCurrent.setPos(blocksCurrent.pos1.add(rotatedVector), blocksCurrent.pos2.add(rotatedVector));
        position = position.add(velocity);
    }

    public void clearChanged(){
        topChanged.forEach(pos -> content.manager.getTopLayer().remove(pos));
        topChanged.clear();
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        position = new Vec3(0.0, 0.0, 0.0);
        for (Tuple<Vec3, Entity> entry : content.entities.entities) {
            Vec3 pos = entry.getA();
            Entity entity = entry.getB();
            transform.moveEntityAbsolute(entity, position.add(pos), 0.05);
        }
        blocksCurrent.setPos(blocks.pos1.add(position), blocks.pos2.add(position));
        clearChanged();
        movers.forEach((section) -> section.onMove(new Vec3i(0, 0, 0)));
    }

    @Override
    public void remove() {
        super.remove();
        clearChanged();
    }

    @Override
    public void setOnMove(Section section) {
        movers.add(section);
    }

    @Override
    public boolean isStatic() {
        return false;
    }
}
