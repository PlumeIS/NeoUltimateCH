package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static cn.plumc.ultimatech.info.UCHInfos.*;

public class Configurable extends Section {
    public Configurable(ServerPlayer owner, SectionLocation location, Game game) {
        super(owner, location, game);
    }

    @Override
    public void handleView() {
        content.setSectionViewInventory(owner);
        Vec3 viewOrigin = PlayerUtil.getPlayerLooking(owner, SECTION_VIEW_DISTANCE);
        BlockPos blockOrigin = new BlockPos(Mth.floor(viewOrigin.x), Mth.floor(viewOrigin.y), Mth.floor(viewOrigin.z));

        handleViewRecover(blockOrigin);
        handleViewBlock(blockOrigin);
        handleViewEntity(blockOrigin);
        handleViewDebug(blockOrigin);
    }

    protected void handleViewRecover(BlockPos blockOrigin){
        // recover
        for (Map.Entry<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> recoverEntry : content.worldRecoverCache.entrySet()) {
            level.setBlockAndUpdate(recoverEntry.getKey(), recoverEntry.getValue().getA());
            Optional<BlockEntity> blockEntity = recoverEntry.getValue().getB();
            blockEntity.ifPresent(entity -> level.setBlockEntity(entity));
        }
        content.worldRecoverCache.clear();
    }

    protected void handleViewBlock(BlockPos blockOrigin){
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
                level.setBlockAndUpdate(worldPos, checkCanPlace(worldPos, worldBlockState) ? blockEntry.getValue() : SECTION_NONPPLACABLE_BLOCK);
            }
        }
    }

    protected void handleViewEntity(BlockPos blockOrigin){        // entities
        rotation.rotated(content.entities);
        for (Entity entityEntry : content.entities.entities.stream().map(Tuple::getB).toList()) {
            entityEntry.moveTo(blockOrigin.getX(), blockOrigin.getY(), blockOrigin.getZ());
        }}

    protected void handleViewDebug(BlockPos blockOrigin){
        if (DEBUG) content.handleViewDebug();
    }

    protected boolean checkCanPlace(BlockPos worldPos, BlockState worldBlockState) {
        return worldBlockState.isAir() && game.getStatus().map.canPlace(worldPos);
    }

    @Override
    public boolean place() {
        if (placed&&!viewing) return false;
        Vec3 viewOrigin = PlayerUtil.getPlayerLooking(owner, SECTION_VIEW_DISTANCE);
        BlockPos blockOrigin = new BlockPos(Mth.floor(viewOrigin.x), Mth.floor(viewOrigin.y), Mth.floor(viewOrigin.z));
        if (!handlePlaceBlock(blockOrigin)) return false;
        content.origin = viewOrigin;
        placed = true;
        viewing = false;
        init();
        initialized = true;
        return true;
    }

    protected boolean handlePlaceBlock(BlockPos blockOrigin){
        for (Map.Entry<BlockPos, BlockState> blockEntry : rotation.rotated(content.templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            BlockState worldBlockState;
            if (content.worldRecoverCache.containsKey(worldPos)) worldBlockState = content.worldRecoverCache.get(worldPos).getA();
            else worldBlockState = level.getBlockState(worldPos);
            content.blocks.put(worldPos, blockEntry.getValue());
            if (BlockUtil.isMultiBlock(level, worldPos)||!checkCanPlace(worldPos, worldBlockState)){
                content.blocks.clear();
                return false;
            }
        }
        return true;
    }
}
