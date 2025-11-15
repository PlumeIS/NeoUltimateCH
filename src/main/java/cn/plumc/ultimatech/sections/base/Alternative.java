package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.utils.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static cn.plumc.ultimatech.info.UCHInfos.SECTION_DISCARDED_BLOCK;

public class Alternative extends Configurable {
    HashMap<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> replacements = new HashMap<>();

    public Alternative(ServerPlayer owner, SectionLocation location, Game game) {
        super(owner, location, game);
    }

    @Override
    protected void handleViewBlock(BlockPos blockOrigin) {
        // block
        for (Map.Entry<BlockPos, BlockState> blockEntry : rotation.rotated(content.templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            BlockState worldBlockState = level.getBlockState(worldPos);
            BlockEntity blockEntity = level.getBlockEntity(worldPos);
            if (!BlockUtil.isMultiBlock(level, worldPos)) {
                content.worldRecoverCache.put(worldPos, new Tuple<>(worldBlockState, Objects.nonNull(blockEntity) ? Optional.of(blockEntity) : Optional.empty()));
                level.setBlockAndUpdate(worldPos, checkCanPlace(worldPos, worldBlockState) ? blockEntry.getValue() : SECTION_DISCARDED_BLOCK);
            }
        }
    }

    protected boolean checkCanPlace(BlockPos worldPos, BlockState worldBlockState) {
        return !worldBlockState.isAir() && game.getStatus().map.canPlace(worldPos);
    }

    @Override
    protected boolean handlePlaceBlock(BlockPos blockOrigin) {
        for (Map.Entry<BlockPos, BlockState> blockEntry : rotation.rotated(content.templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            BlockState worldBlockState;
            if (content.worldRecoverCache.containsKey(worldPos)) worldBlockState = content.worldRecoverCache.get(worldPos).getA();
            else worldBlockState = level.getBlockState(worldPos);
            if (!BlockUtil.isMultiBlock(level, worldPos)&&!checkCanPlace(worldPos, worldBlockState)) {
                level.removeBlock(worldPos, false);
            } else if (!BlockUtil.isMultiBlock(level, worldPos)) {
                replacements.put(worldPos, content.worldRecoverCache.get(worldPos));
                content.blocks.put(worldPos, blockEntry.getValue());
            }
        }
        return !replacements.isEmpty();
    }

    @Override
    public void remove() {
        for (Map.Entry<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> entry : replacements.entrySet()) {
            level.setBlockAndUpdate(entry.getKey(), entry.getValue().getA());
            if (entry.getValue().getB().isPresent()) level.setBlockEntity(entry.getValue().getB().get());
        }
    }
}
