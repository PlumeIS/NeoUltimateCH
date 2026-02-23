package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BlockHit;
import cn.plumc.ultimatech.utils.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

import static cn.plumc.ultimatech.info.UCHInfos.SECTION_DISCARDED_BLOCK;

public class WireMesh extends Configurable {
    public WireMesh(ServerPlayer owner, SectionLocation location, Game game) {
        super(owner, location, game);
        setProcess(0);
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
                content.manager.getMiddleLayer().set(worldPos, checkCanPlace(worldPos, worldBlockState) ? blockEntry.getValue() : SECTION_DISCARDED_BLOCK);
            }
        }
    }

    @Override
    protected boolean checkCanPlace(BlockPos worldPos, BlockState worldBlockState) {
        return !getFacing(worldPos).isEmpty() &&
                worldBlockState.isAir() &&
                game.getStatus().map.canPlace(worldPos);
    }

    private List<BlockState> getFacing(BlockPos pos){
        List<BlockState> faceBlocks = new ArrayList<>();
        faceBlocks.add(getWorldBlockState(pos.above()));
        faceBlocks.add(getWorldBlockState(pos.below()));
        faceBlocks.add(getWorldBlockState(pos.north()));
        faceBlocks.add(getWorldBlockState(pos.east()));
        faceBlocks.add(getWorldBlockState(pos.south()));
        faceBlocks.add(getWorldBlockState(pos.west()));
        return faceBlocks.stream().filter(blockState -> !blockState.isAir()).toList();
    }

    private BlockState getWorldBlockState(BlockPos pos){
        return content.manager.getMiddleLayer().get(pos).getA();
    }

    @Override
    protected boolean handlePlaceBlock(BlockPos blockOrigin) {
        for (Map.Entry<BlockPos, BlockState> blockEntry : rotation.rotated(content.templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            if (!BlockUtil.isMultiBlock(level, worldPos)) {
                content.changedBlocks.add(worldPos);
            }
        }
        return !content.changedBlocks.isEmpty();
    }

    @Override
    public void tickRun(int tickTime) {
        content.getBlocks().keySet().forEach(pos->killAll(new BlockHit(pos).detectPlayers(game)));
    }
}
