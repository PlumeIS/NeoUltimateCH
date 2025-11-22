package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.*;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.FixedLinkedList;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.util.Tuple;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;

public class Fan extends Section {
    public Vec3 center;
    public Vec3 pushMovement;
    public Vec3 effectMovement;
    public Vec3 deltaMovement;
    public BoxHit.Relative hit;
    public BoxHit.Relative particleHit;
    public FixedLinkedList<Tuple<DustParticleOptions, Vec3>> trackingParticles = new FixedLinkedList<>(2000);

    public Fan(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(4, 2), game);
        setProcess(SectionCounter.toTicks(1.2));
    }

    @Override
    public void init() {
        center = transform.rotatePoint(new Vec3(1.5, 1, 1.5));
        pushMovement = transform.rotateVector(new Vec3(0, 1.2, 0));
        effectMovement = transform.rotateVector(new Vec3(1.0, 0.0, 1.0));
        effectMovement = new Vec3(Math.abs(effectMovement.x), Math.abs(effectMovement.y), Math.abs(effectMovement.z));
        deltaMovement = transform.toNonNegative(transform.rotateVector(new Vec3(0, 0.2, 0)));
        hit = new BoxHit.Relative(content.origin, new Vec3(0, 1, 0), new Vec3(3, 8, 3));
        particleHit = new BoxHit.Relative(content.origin, new Vec3(0, 1, 0), new Vec3(3, 3.5, 3));
        transform.applyRotationToRelativeHit(hit);
        transform.applyRotationToRelativeHit(particleHit);
    }

    @Override
    public void tickRun(int tickTime) {
        double progress = process.progress(0, 1.2);
        HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
        rotations.put(SectionRotation.Axis.Y, progress*360);
        transform.applyEntityRotation(content.getContentEntity("uch.fan.fan"), rotations, 0.05);

        List<ServerPlayer> players = hit.detectPlayers(game);
        for (ServerPlayer player : players) {
            ClipContext clipContext = new ClipContext(
                    transform.toAbsolute(center),
                    player.position(),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            );
            BlockHitResult result = level.clip(clipContext);
            if (!(result.getType() == HitResult.Type.MISS)) continue;
            double effect = Math.max(Math.min(1 - (player.position().distanceTo(transform.toAbsolute(center)) / 7.0), 0.8), 0.3);
            Vec3 movement = pushMovement.scale(effect).add(player.getDeltaMovement().multiply(effectMovement).scale(1.01));
            player.setDeltaMovement(movement);
            PlayerUtil.updateDeltaMovement(server.getPlayerList().getPlayers(), player);
        }

        vfx();
    }

    private void vfx(){
        Vec3 point = BlockUtil.getRandomPointInAABB(particleHit.getAABB());
        DustParticleOptions dust = new DustParticleOptions(new Vec3(1, 1, 1).toVector3f(), 1);
        trackingParticles.add(new Tuple<>(dust, point));

        for (Tuple<DustParticleOptions, Vec3> trackingParticle : ImmutableList.copyOf(trackingParticles)) {
            Vec3 pos = trackingParticle.getB();
            if (!hit.getAABB().contains(pos.add(deltaMovement))){
                trackingParticles.remove(trackingParticle);
            }
            BlockPos blockPos = BlockUtil.toBlockPos(pos);
            if (!content.checkCanPlace(blockPos, level.getBlockState(blockPos))){
                trackingParticles.remove(trackingParticle);
            }
            trackingParticle.setB(pos.add(deltaMovement));
            level.sendParticles(trackingParticle.getA(), pos.x, pos.y, pos.z, 1,
                    0, 0, 0, 0);
        }
    }
}
