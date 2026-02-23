package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.layer.LayerType;
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
            if (!BlockUtil.isMultiBlock(level, worldPos)) {
                List<Section> tops = content.manager.getSectionsByPos(worldPos, LayerType.TOP);
                List<Section> middles = content.manager.getSectionsByPos(worldPos, LayerType.MIDDLE);
                tops.addAll(middles);
                if (!tops.isEmpty()) {
                    tops.getFirst().content.changedBlocks.forEach(pos -> {
                        content.manager.getViewLayer().set(pos, SECTION_NONPPLACABLE_BLOCK);
                        content.changedBlocks.add(pos);
                    });
                } else {
                    content.manager.getViewLayer().set(worldPos, SECTION_DISCARDED_BLOCK);
                    content.changedBlocks.add(worldPos);
                }
            }
        }
    }

    @Override
    protected boolean checkCanPlace(BlockPos worldPos, BlockState worldBlockState) {
        List<Section> tops = content.manager.getSectionsByPos(worldPos, LayerType.TOP);
        List<Section> middles = content.manager.getSectionsByPos(worldPos, LayerType.MIDDLE);
        return tops.isEmpty() && middles.isEmpty();
    }

    @Override
    protected boolean handlePlaceBlock(BlockPos blockOrigin) {
        for (Map.Entry<BlockPos, BlockState> blockEntry : rotation.rotated(content.templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            List<Section> tops = content.manager.getSectionsByPos(worldPos, LayerType.TOP);
            List<Section> middles = content.manager.getSectionsByPos(worldPos, LayerType.MIDDLE);
            tops.addAll(middles);
            if (!tops.isEmpty()) {
                tops.getFirst().remove();
            }
        }
        Vec3 viewOrigin = mapSection ? content.mapOrigin : PlayerUtil.getPlayerLooking(owner, SECTION_VIEW_DISTANCE);
        content.entities.entities.stream().map(Tuple::getA).toList().forEach(position -> {
            Vec3 pos = viewOrigin.add(position);
            level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 20,
                    1.0, 1.0, 1.0, 0);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 1.0f);
        });
        remove();
        return true;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public LayerType getRunningLayer() {
        return LayerType.VIEW;
    }
}
