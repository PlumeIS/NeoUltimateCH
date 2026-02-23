package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.layer.LayerType;
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
    public HashMap<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> replacements = new HashMap<>();

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
            Tuple<BlockState, Optional<BlockEntity>> block = content.manager.getTopLayer().get(worldPos);
            if (!BlockUtil.isMultiBlock(level, worldPos)) {
                content.changedBlocks.add(worldPos);
                content.manager.getViewLayer().set(worldPos, checkCanPlace(worldPos, block.getA()) ? blockEntry.getValue() : SECTION_DISCARDED_BLOCK);
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
            Tuple<BlockState, Optional<BlockEntity>> block = content.manager.getTopLayer().get(worldPos);
            if (!BlockUtil.isMultiBlock(level, worldPos)&&!checkCanPlace(worldPos, block.getA())) {
                content.manager.getViewLayer().remove(worldPos);
                content.changedBlocks.remove(worldPos);
            } else if (!BlockUtil.isMultiBlock(level, worldPos)) {
                replacements.put(worldPos, block);
            }
        }
        if (!replacements.isEmpty()) {
            content.changedBlocks.forEach(pos -> {
                Tuple<BlockState, Optional<BlockEntity>> block = content.manager.getViewLayer().remove(pos);
                content.manager.getLayer(getRunningLayer()).set(pos, block);
            });
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

    @Override
    public LayerType getRunningLayer() {
        return LayerType.TOP;
    }
}
