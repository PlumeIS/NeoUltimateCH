package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.sections.base.MovableStep;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import static cn.plumc.ultimatech.info.UCHInfos.SECTION_VIEW_DISTANCE;

public class MovableStep1 extends MovableStep {
    public Vec3 velocity = new Vec3(0.0, 0.0707, 0.0707);
    public Vec3 movement = new Vec3(0.0, 1.0, 1.0);

    public MovableStep1(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(4, 4), game);
        setProcess(SectionCounter.toTicks(8.5));
    }

    @Override
    public void init() {
        blocks = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0.5, 0.5, 0.5), new Vec3(0.5, 0.5, 0.5));
        transform.applyRotationToRelativeHit(blocks);
        blocksCurrent = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0.5, 0.5, 0.5), new Vec3(0.5, 0.5, 0.5));
        transform.applyRotationToRelativeHit(blocksCurrent);
    }

    @Override
    public void handleView() {
        super.handleView();
        if (!mapSection) {
            Vec3 viewOrigin = PlayerUtil.getPlayerLooking(owner, SECTION_VIEW_DISTANCE);
            vfx(viewOrigin);
        }
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.at(8.50)) {
            position = new Vec3(0.0, 0.0, 0.0);
            blocksCurrent = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0.5, 0.5, 0.5), new Vec3(0.5, 0.5, 0.5));
            transform.applyRotationToRelativeHit(blocksCurrent);
            return;
        };
        Vec3 currentVelocity = velocity;
        if (process.in(4.25, 8.50)) {
            currentVelocity = velocity.scale(-1);
        }

        clearChanged();
        move(currentVelocity);

        vfx(content.getOrigin());

    }

    public void vfx(Vec3 origin) {
        for (int i = 0; i < 7; i++) {
            Vec3 pos = origin.add(transform.rotateVector(new Vec3(0.5, 0.5, 0.5))).add(transform.rotateVector(movement.scale(i)));
            if (i == 0 || i == 6) {
                level.sendParticles(new DustParticleOptions(new Vec3(0.3, 0.3, 0.3).toVector3f(), 1.5f),
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            } else {
                level.sendParticles(new DustParticleOptions(new Vec3(0.5, 0.5, 0.5).toVector3f(), 0.8f),
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }
}
