package cn.plumc.ultimatech.section.layer;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class LayerBlock {

    public BlockState state;
    public BlockEntity entity;

    public LayerBlock(BlockState state, BlockEntity entity) {
        this.state = state;
        this.entity = entity;
    }

    public void set(BlockState state, BlockEntity entity) {
        this.state = state;
        this.entity = entity;
    }
}