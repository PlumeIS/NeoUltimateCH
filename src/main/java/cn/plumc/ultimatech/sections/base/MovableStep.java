package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionContent;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MovableStep extends Configurable {
    public Vec3 position = new Vec3(0.0, 0.0, 0.0);
    public Vec3 movement = new Vec3(0.0, 0.0, 1.0);
    public BoxHit.Relative blocks;
    public BoxHit.Relative blocksCurrent;
    public HashMap<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> changed = new HashMap<>();

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
            changed.put(pos, game.getSectionManager().getRecoverBlockOrWorldBlock(pos));
            level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
        }

        Vec3 rotatedVector = transform.rotateVector(velocity);
        blocksCurrent.setPos(blocksCurrent.pos1.add(rotatedVector), blocksCurrent.pos2.add(rotatedVector));
        position = position.add(velocity);
    }

    public void clearChanged(){
        for (Map.Entry<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> entry : changed.entrySet()) {
            Optional<BlockEntity> blockEntity = entry.getValue().getB();
            level.setBlockAndUpdate(entry.getKey(), entry.getValue().getA());
            blockEntity.ifPresent(entity -> level.setBlockEntity(entity));
        }
        changed.clear();
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
    }

    @Override
    public void remove() {
        super.remove();
        clearChanged();
    }
}
