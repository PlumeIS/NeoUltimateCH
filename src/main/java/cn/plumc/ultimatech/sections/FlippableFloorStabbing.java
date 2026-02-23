package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.*;
import cn.plumc.ultimatech.section.hit.BoxHit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

public class FlippableFloorStabbing extends Section {
    BoxHit.Relative topHit;
    BoxHit.Relative bottomHit;

    public FlippableFloorStabbing(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(2, 4), game);
        setProcess(SectionCounter.toTicks(4.0));
    }

    @Override
    public void init() {
        topHit = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0, 1, 0), new Vec3(1, 1.2, 1));
        bottomHit = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0, -0.2, 0), new Vec3(1, 0, 1));
        transform.applyRotationToRelativeHit(topHit);
        transform.applyRotationToRelativeHit(bottomHit);
    }

    @Override
    public void tickRun(int tickTime) {
        Entity entity = content.getContentEntity("uch.flippable_floor_stabbing");
        if (process.in(0.0, 1.2)) {
            killAll(topHit.detectPlayers(game));
        }
        if (process.at(1.2)) {
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            rotations.put(SectionRotation.Axis.Z, 180.0);
            transform.applyEntityRotation(entity, rotations, 0.8);
        }
        if (process.in(2.0, 3.2)){
            killAll(bottomHit.detectPlayers(game));
        }
        if (process.at(3.2)){
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            transform.applyEntityRotation(entity, rotations, 0.8);
        }
    }
}
