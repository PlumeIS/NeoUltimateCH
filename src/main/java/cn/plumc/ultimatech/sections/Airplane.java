package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.*;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.DisplayEntityUtil;
import cn.plumc.ultimatech.utils.FixedLinkedList;
import cn.plumc.ultimatech.utils.TickUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Airplane extends Section {
    public Vec3 spawnPoint = new Vec3(1.0, 2.5, 1.0);
    public Vec3 velocity = new Vec3(0.0, 0.0, -0.075);
    public BoxHit.Relative blocks;

    public static class AirplaneEntity {
        private final Entity entity;
        private final BoxHit.Relative block;
        private Vec3 pos;
        private boolean crashed = false;

        public AirplaneEntity(Entity entity, BoxHit.Relative block, Vec3 pos) {
            this.entity = entity;
            this.block = block;
            this.pos = pos;
        }

        public void setPos(Vec3 pos) {
            this.pos = pos;
        }

        public Vec3 getPos() {
            return pos;
        }

        public void setCrashed(boolean crashed) {
            this.crashed = crashed;
        }

        public boolean isCrashed() {
            return crashed;
        }

        public Entity getEntity() {
            return entity;
        }

        public BoxHit.Relative getBlock() {
            return block;
        }
    }

    public FixedLinkedList<AirplaneEntity> airplanes = new FixedLinkedList<>(UCHInfos.SECTION_MAX_ENTITIES){
        @Override
        public AirplaneEntity removeFirst() {
            getFirst().getEntity().kill(level);
            return super.removeFirst();
        }
    };
    public List<BlockPos> changedBlocks = new ArrayList<>();

    public Airplane(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3, 8), game);
        setProcess(SectionCounter.toTicks(4.0));
    }

    @Override
    public void init() {
        spawnPoint = transform.rotateVector(spawnPoint);
        blocks = new BoxHit.Relative(content.origin, new Vec3(0.5, 3.5, 0), new Vec3(1.5, 3.5, 2));
        transform.applyRotationToRelativeHit(blocks);
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.at(4.0)) {
            Display.ItemDisplay itemDisplay = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
            itemDisplay.addTag("uch.airplane.airplane");
            DisplayEntityUtil.setItem(itemDisplay, "minecraft:crafter");
            DisplayEntityUtil.setScale(itemDisplay, 3.0);

            itemDisplay.setPos(transform.toAbsolute(spawnPoint));
            transform.applyEntityBaseRotation(itemDisplay, rotation.getRotations(), 0.0);
            airplanes.add(new AirplaneEntity(itemDisplay, new BoxHit.Relative(blocks.origin, blocks.pos1, blocks.pos2), new Vec3(0, 0, 0)));
            level.addFreshEntity(itemDisplay);
        }

        changedBlocks.forEach(blockPos -> level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState()));

        for (AirplaneEntity entry : ImmutableList.copyOf(airplanes)) {
            Entity entity = entry.getEntity();
            Vec3 translation = entry.getPos();
            BoxHit.Relative relative = entry.getBlock();

            AABB aabb = relative.getAABB();
            Vec3 rotatedVelocity = transform.rotateVector(velocity);
            relative.setPos(relative.pos1.add(rotatedVelocity), relative.pos2.add(rotatedVelocity));
            List<BlockPos> blocks = BlockUtil.getBlocksFromAABB(aabb);

            if (!entry.isCrashed() && testCrash(blocks)) {
                HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
                translation = translation.add(velocity.scale(-10));
                rotations.put(SectionRotation.Axis.X, -90.0);
                transform.moveEntityAbsolute(entity, translation, 0.0);
                transform.applyEntityRotation(entity, rotations, 0.05);
                entry.setCrashed(true);
                TickUtil.runAfterTick(() -> {
                    vfx(spawnPoint.add(transform.toAbsolute(transform.rotatePoint(spawnPoint.add(entry.getPos())))));
                    entity.kill(level);
                    airplanes.remove(entry);
                }, 1.0);
            }
            if (entry.isCrashed()) {
                translation = translation.add(new Vec3(0, -0.3, 0));

            } else {
                translation = translation.add(velocity);
                for (BlockPos pos : blocks) {
                    if (content.checkCanPlace(pos, level.getBlockState(pos))) {
                        level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
                        changedBlocks.add(pos);
                    }
                }
            }
            transform.moveEntityAbsolute(entity, translation, 0.05);
            entry.setPos(translation);
        }
    }

    private boolean testCrash(List<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            if (!content.blocks.containsKey(pos)&&!content.checkCanPlace(pos, level.getBlockState(pos))) {
                return true;
            }
        }
        return false;
    }

    private void vfx(Vec3 pos) {
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 20,
                1.0, 1.0, 1.0, 0);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 1.0f);
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        airplanes.forEach(airplane -> airplane.getEntity().kill(level));
        airplanes.clear();
        changedBlocks.forEach(blockPos -> level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState()));
    }

    @Override
    public void remove() {
        super.remove();
        changedBlocks.forEach(blockPos -> level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState()));
        airplanes.forEach(airplane -> airplane.getEntity().kill(level));
    }
}
