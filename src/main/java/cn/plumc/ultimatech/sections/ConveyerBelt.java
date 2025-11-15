package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.FixedLinkedList;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;

public class ConveyerBelt extends Section {
    public BoxHit.Relative topHit;
    public BoxHit.Relative bottomHit;
    public Vec3 velocitySampling;
    public Vec3 topSpeed;
    public Vec3 bottomSpeed;
    public Vec3 acceleration;
    public FixedLinkedList<Tuple<DustParticleOptions, Vec3>> topTrackingParticles = new FixedLinkedList<>(1000);
    public FixedLinkedList<Tuple<DustParticleOptions, Vec3>> bottomTrackingParticles = new FixedLinkedList<>(1000);

    public ConveyerBelt(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(5, 2), game);
        setProcess(5);
    }

    @Override
    public void init() {
        topHit = new BoxHit.Relative(content.origin, new Vec3(0, 1, 0), new Vec3(3, 1.2, 1));
        bottomHit = new BoxHit.Relative(content.origin, new Vec3(0, 0, 0), new Vec3(3, -0.2, 1));
        transform.applyRotationToRelativeHit(topHit);
        transform.applyRotationToRelativeHit(bottomHit);
        velocitySampling = transform.toNonNegative(transform.rotateVector(new Vec3(1, 0, 0)));
        topSpeed = transform.rotateVector(new Vec3(0.2, 0, 0));
        bottomSpeed = transform.rotateVector(new Vec3(-0.2, 0, 0));
        acceleration = transform.rotateVector(new Vec3(1.3, 0, 0));
    }

    @Override
    public void tickRun(int tickTime) {
        friction(topHit, topSpeed, acceleration);
        friction(bottomHit, bottomSpeed, acceleration);
        vfx(topHit, topSpeed, topTrackingParticles, tickTime == 0);
        vfx(bottomHit, bottomSpeed, bottomTrackingParticles, tickTime == 0);
    }

    private void friction(BoxHit.Relative hit, Vec3 speed, Vec3 acceleration) {
        hit.detectPlayers(game).forEach(player -> {
            Vec3 speedVec = player.getDeltaMovement();
            Vec3 movement = speedVec.multiply(velocitySampling);
            Vec3 delta = speed.subtract(movement);
            double deltaSpeed = delta.dot(velocitySampling);
            Vec3 addedSpeed;
            if (deltaSpeed > 0) {
                Vec3 added = acceleration.scale(0.05);
                if (added.dot(velocitySampling) > deltaSpeed) addedSpeed = speedVec.add(velocitySampling.scale(deltaSpeed));
                else addedSpeed = speedVec.add(added);
            } else {
                Vec3 added = acceleration.scale(-0.05);
                if (added.dot(velocitySampling) < deltaSpeed) addedSpeed = speedVec.add(velocitySampling.scale(deltaSpeed));
                else addedSpeed = speedVec.add(added);
            }

            System.out.println(addedSpeed);
            player.setDeltaMovement(addedSpeed);
            PlayerUtil.updateDeltaMovement(game.getPlayerManager().getPlayers(), player);
        });
    }

    private void vfx(BoxHit.Relative hit, Vec3 speed,
                     FixedLinkedList<Tuple<DustParticleOptions, Vec3>> tracking,
                     boolean create){
        if (create) {
            Vec3 point = BlockUtil.getRandomPointInAABB(hit.getAABB());
            DustParticleOptions dust = new DustParticleOptions(ARGB.color(new Vec3(1, 1, 1)), 0.8f);
            tracking.add(new Tuple<>(dust, point));
        }

        for (Tuple<DustParticleOptions, Vec3> entry : ImmutableList.copyOf(tracking)) {
            Vec3 pos = entry.getB();
            if (!hit.getAABB().contains(pos.add(speed.scale(0.05)))){
                tracking.remove(entry);
            }
            entry.setB(pos.add(speed.scale(0.05)));
            level.sendParticles(entry.getA(), pos.x, pos.y, pos.z, 1,
                    0, 0, 0, 0);
        }
    }
}
