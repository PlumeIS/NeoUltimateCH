package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.*;
import cn.plumc.ultimatech.section.hit.BoxHit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

public class Barrett extends Section {
    public Entity stick;
    public Vec3 rotatingCenter = new Vec3(-0.17, 0.46, 0.0);
    public BoxHit.Relative hit;
    public BoxHit.Relative maxHit;
    public Vec3 warningPos;
    public Vec3 start;
    public Vec3 end;
    public Vec3 detection;
    public boolean vfx = false;

    public Barrett(ServerPlayer player, Game game) {
        // 1.5s - 抬杆，警报 => 2.5s - 挥杆，出球，警报结束 => 3.5 - 回杆
        super(player, SectionLocation.get(3, 4), game);
        setProcess(SectionCounter.toTicks(3.5));
    }

    @Override
    public void init() {
        stick = content.getContentEntity("uch.barrett.stick");
        hit = new BoxHit.Relative(content.origin, new Vec3(0.1, 0, 1), new Vec3(0.9, 0.8, 1.8));
        maxHit = new BoxHit.Relative(content.origin, new Vec3(0.1, 0, 1), new Vec3(0.1, 0.8, 1.8));
        transform.applyRotationToRelativeHit(hit);
        transform.applyRotationToRelativeHit(maxHit);
        detection = transform.rotateVector(new Vec3(1.0, 0.0, 0.0));
        start = transform.rotateVector(new Vec3(0.5, 0.4, 1.4));
        end = transform.rotateVector(new Vec3(500.1, 0.4, 1.4));
        warningPos = transform.toAbsolute(transform.rotateVector(new Vec3(0.5, 2.5, 0.5)));
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.in(1.5, 2.4)) {
            double progress = process.progress(1.5, 2.4);
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            rotations.put(SectionRotation.Axis.Z, -45.0*progress);
            transform.applyEntityRotationWithCenter(stick, rotations, rotatingCenter,0.05);
            vfxWarning();
        }
        if (process.at(2.5)) {
            detectHit();
            killAll(maxHit.detectPlayers(game));
            vfx = true;
        }
        if (process.in(2.5, 2.6)) {
            double progress = process.progress(2.5, 2.6);
            if (vfx) vfx(progress);
        }
        if (process.in(2.4, 2.6)) {
            double progress = process.progress(2.4, 2.6);
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            rotations.put(SectionRotation.Axis.Z, -45+270*progress);
            transform.applyEntityRotationWithCenter(stick, rotations, rotatingCenter, 0.05);
        }
        if (process.in(2.6, 3.5)) {
            double progress = process.progress(2.6, 3.5);
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            rotations.put(SectionRotation.Axis.Z, 225-225*progress);
            transform.applyEntityRotationWithCenter(stick, rotations, rotatingCenter, 0.05);
        }
    }

    private void vfxWarning(){
        level.sendParticles(new DustParticleOptions(new Vec3(1, 0, 0).toVector3f(), 1.5f),
                warningPos.x, warningPos.y, warningPos.z,
                5, 0.25, 0.25, 0.25, 0
        );
    }

    private void vfx(double progress) {
        Vec3 startAbs = transform.toAbsolute(start);
        Vec3 endAbs = transform.toAbsolute(end);
        Vec3 pointA = startAbs.add(endAbs.subtract(startAbs).scale(Math.min(progress, 1.0)));
        Vec3 pointB = startAbs.add(endAbs.subtract(startAbs).scale(Math.min(progress+0.5, 1.0)));

        for (double i = 0; i <= 1; i += 0.002) {
            Vec3 point = pointA.add(pointB.subtract(pointA).scale(i));
            BlockPos pos = new BlockPos(Mth.floor(point.x), Mth.floor(point.y), Mth.floor(point.z));
            if (!level.getBlockState(pos).isAir()) {
                vfx = false;
                break;
            };
            level.sendParticles(new DustParticleOptions(new Vec3(0.0, 0.0, 0.0).toVector3f(), 1.2f),
                    point.x, point.y, point.z, 1,
                    0, 0, 0, 0);
        }
    }

    private void detectHit() {
        Vec3 point = transform.toAbsolute(hit.pos2);
        for (int i = 0; i < 500; i++) {
            BlockPos detectionPos = new BlockPos(
                    Mth.floor(point.add(detection).x),
                    Mth.floor(point.add(detection).y),
                    Mth.floor(point.add(detection).z)
            );
            if (!level.getBlockState(detectionPos).isAir()){
                break;
            }
            point = point.add(detection);
        }
        maxHit.setPos(hit.pos1, transform.toRelative(point));
    }
}
