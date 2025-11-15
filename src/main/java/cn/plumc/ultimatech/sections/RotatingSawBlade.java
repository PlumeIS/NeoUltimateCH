package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.*;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.RotationUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

public class RotatingSawBlade extends Section {
    public Vec3 rotatingCenter = new Vec3(0.5, 1.0, 0.0);
    public Vec3 absRotatingCenter;
    public Vec3 hitCenter;
    public Vec3 hitStart;
    public Vec3 hitEnd;

    public RotatingSawBlade(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(5, 1), game);
        setProcess(SectionCounter.toTicks(5.0));
    }

    @Override
    public void init() {
        hitCenter = transform.toAbsolute(transform.rotatePoint(new Vec3(0.5, 2.5, 1.0)));
        absRotatingCenter = transform.toAbsolute(transform.rotatePoint(new Vec3(1, 0.5, 0.5)));
        hitStart = transform.rotatePoint(new Vec3(-0.6, -0.4, -0.4));
        hitEnd = transform.rotatePoint(new Vec3(0.6, 0.4, 0.4));
    }

    @Override
    public void tickRun(int tickTime) {
        double progress = process.progress(0.0, 5.0);
        Entity sawEntity = content.getContentEntity("uch.rotating_saw_blade.saw");
        HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
        rotations.put(SectionRotation.Axis.X, 360.0 * progress);
        transform.applyEntityRotationWithCenter(sawEntity, rotations, rotatingCenter, 0.05);
        Vec3 hitPoint = RotationUtil.rotatePoint(hitCenter, absRotatingCenter, rotations);
        killAll(new BoxHit.Relative(hitPoint, hitStart, hitEnd).detectPlayers(game));
    }
}
