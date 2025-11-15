package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static cn.plumc.ultimatech.info.UCHInfos.*;

public class Remover extends Configurable {
    public Remover(ServerPlayer owner, SectionLocation location, Game game) {
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
                List<Section> byPos = game.getSectionManager().getSectionsByPos(worldPos);
                if (!byPos.isEmpty()) {
                    byPos.getFirst().content.blocks.forEach((pos, state) -> {
                        BlockEntity entity = level.getBlockEntity(worldPos);
                        content.worldRecoverCache.put(pos, new Tuple<>(state, Objects.nonNull(entity) ? Optional.of(entity) : Optional.empty()));
                        level.setBlockAndUpdate(pos, SECTION_NONPPLACABLE_BLOCK);
                    });
                } else {
                    level.setBlockAndUpdate(worldPos, SECTION_DISCARDED_BLOCK);
                }
            }
        }
    }

    @Override
    protected boolean checkCanPlace(BlockPos worldPos, BlockState worldBlockState) {
        List<Section> byPos = game.getSectionManager().getSectionsByPos(worldPos);
        return byPos.isEmpty();
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

            List<Section> byPos = game.getSectionManager().getSectionsByPos(worldPos);
            if (!byPos.isEmpty()) {
                byPos.getFirst().remove();
            } else {
                level.setBlockAndUpdate(worldPos, worldBlockState);
            }
        }
        Vec3 viewOrigin = PlayerUtil.getPlayerLooking(owner, SECTION_VIEW_DISTANCE);
        content.entities.entities.stream().map(Tuple::getA).toList().forEach(position -> {
            Vec3 pos = viewOrigin.add(position);
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 20,
                    1.0, 1.0, 1.0, 0);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 1.0f);
        });
        remove();
        return true;
    }
}
