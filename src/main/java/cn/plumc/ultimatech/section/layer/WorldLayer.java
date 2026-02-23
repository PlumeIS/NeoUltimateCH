package cn.plumc.ultimatech.section.layer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class WorldLayer extends Layer{
    public ServerLevel level;

    public WorldLayer(ServerLevel level){
        super(LayerType.BOTTOM);
        this.level = level;

    }

    public void read(BlockPos pos) {
        if (!has(pos)) {
            BlockState state = level.getBlockState(pos);
            BlockEntity entity = level.getBlockEntity(pos);
            Tuple<BlockState, Optional<BlockEntity>> block = new Tuple<>(state, Optional.ofNullable(entity));
            blocks.put(pos, block);
        }
    }

    @Override
    public Tuple<BlockState, Optional<BlockEntity>> get(BlockPos pos) {
        read(pos);
        return blocks.get(pos);
    }
}
