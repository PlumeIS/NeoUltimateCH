package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.MotionTransform;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.SectionRotation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutomaticDoor extends Section {
    public Vec3 center;

    public BoxHit.Relative front;
    public BoxHit.Relative back;
    public BoxHit.Relative left;
    public BoxHit.Relative right;

    public boolean open;

    public AutomaticDoor(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(4, 3), game);
        setProcess(0);
    }

    @Override
    public void init() {
        center = transform.toAbsolute(transform.rotatePoint(new Vec3(0.5, 1, 0.5)));
        front = new BoxHit.Relative(()->content.getOrigin(), new Vec3(-0.2, 0.0, 0.0), new Vec3(0.0, 3.0, 1.0));
        back = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0.0, 0.0, 0.0), new Vec3(1.2, 3.0, 1.0));
        left = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0.0, 0.0, -0.2), new Vec3(1.0, 3.0, 0.0));
        right = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 3.0, 1.2));
        transform.applyRotationToRelativeHit(front);
        transform.applyRotationToRelativeHit(back);
        transform.applyRotationToRelativeHit(left);
        transform.applyRotationToRelativeHit(right);
    }

    @Override
    public void tickRun(int tickTime) {
        if (front.hasPlayer(game)||back.hasPlayer(game)) {
            setBlocking(false);
            if (open){
                openDoor(false);
                open = false;
                vfx();
            }
        }
        else if (left.hasPlayer(game)||right.hasPlayer(game)) {
            setBlocking(false);
            if (!open) {
                openDoor(true);
                open = true;
                vfx();
            }
        }
        else {
            setBlocking(true);
            if (!open) {
                openDoor(true);
                open = true;
                vfx();
            }
        }
    }

    private void openDoor(boolean open) {
        List<Entity> entities = content.getContentEntities("uch.automatic_door");
        for (Entity entity : entities) {
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            if (open) {
                rotations.put(SectionRotation.Axis.Y, 90.0);
            }
            transform.applyEntityRotationWithCenter(entity, rotations, new Vec3(-0.09375, 0, -0.90625), 0.1);
        }
    }

    private void setBlocking(boolean blocking){
        for (Map.Entry<BlockPos, BlockState> entry : content.getBlocks().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            if (state.getBlock() == Blocks.BARRIER && !blocking) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else {
                level.setBlockAndUpdate(pos, state);
            }
        }
    }

    private void vfx(){
        if (open) level.playSound(null, center.x, center.y, center.z, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS);
        else level.playSound(null, center.x, center.y, center.z, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS);
    }
}
