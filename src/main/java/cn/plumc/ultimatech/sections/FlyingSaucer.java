package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.MotionTransform;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.SectionRotation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.sections.base.MovableStep;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

import static cn.plumc.ultimatech.info.UCHInfos.SECTION_VIEW_DISTANCE;

public class FlyingSaucer extends MovableStep {
    public Vec3 velocity = new Vec3(0.0, 0.05, 0.0);
    public Vec3 movement = new Vec3(0.0, 1.0, 0.0);


    public FlyingSaucer(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(4, 0), game);
        setProcess(SectionCounter.toTicks(10.0));
    }

    @Override
    public void handleView() {
        super.handleView();
        Vec3 viewOrigin = PlayerUtil.getPlayerLooking(owner, SECTION_VIEW_DISTANCE);
        vfx(viewOrigin);
    }

    @Override
    public void init() {
        blocks = new BoxHit.Relative(content.origin, new Vec3(0.5, 0.5, 0.5), new Vec3(1.5, 0.5, 1.5));
        transform.applyRotationToRelativeHit(blocks);
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.at(10.0)) {
            position = new Vec3(0.0, 0.0, 0.0);
            blocks = new BoxHit.Relative(content.origin, new Vec3(0.5, 0.5, 0.5), new Vec3(1.5, 0.5, 1.5));
            transform.applyRotationToRelativeHit(blocks);
            return;
        };
        Vec3 currentVelocity = velocity;
        if (process.in(5.0, 10.0)) {
            currentVelocity = velocity.scale(-1);
        }

        clearChanged();
        move(currentVelocity);

        double fanProgress = process.progress(0.0, 0.5);
        HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
        rotations.put(SectionRotation.Axis.Y, -360.0*fanProgress);
        Entity entity = content.getContentEntity("uch.flying_saucer.fan");
        transform.applyEntityRotationWithCenter(
                entity,
                rotations,
                new Vec3(0.5, 0.0, 0.5),
                0.05
        );
        transform.moveEntitySelfRelative(entity, position, 0.05);
    }

    public void vfx(Vec3 origin) {
        for (int i = 0; i < 6; i++) {
            Vec3 pos = origin.add(transform.rotateVector(new Vec3(1.0, 1.0, 1.0))).add(transform.rotateVector(movement.scale(i)));
            if (i == 0 || i == 5) {
                level.sendParticles(new DustParticleOptions(new Vec3(0.39, 1.0, 0.0).toVector3f(), 1.5f),
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            } else {
                level.sendParticles(new DustParticleOptions(new Vec3(0.39, 1.0, 0.0).toVector3f(), 0.8f),
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }
}
