package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class FireHydrant extends Section {
    public Vec3 startPos;
    public Vec3 pushMovement;
    public Vec3 effectMovement;
    public Vec3 deltaMovement;
    public Vec3 endMovement;
    public Vec3 waterMovement;
    public Vec3 waterBlock;
    public boolean setBlocked = false;
    public BoxHit.Relative hit;
    private List<Tuple<Vec3, DustParticleOptions>> vfxPositions = new ArrayList<>();

    public FireHydrant(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(4, 1), game);
        setProcess(SectionCounter.toTicks(2.0));
    }

    @Override
    public void init() {
        startPos = transform.rotatePoint(new Vec3(0.5, 1.5, 0.5));
        pushMovement = transform.rotateVector(new Vec3(0.0, 1.2, 0.0));
        effectMovement = transform.rotateVector(new Vec3(1.0, 0.0, 1.0));
        deltaMovement = transform.rotateVector(new Vec3(0.04, 0.04, 0.04));
        endMovement = transform.rotateVector(new Vec3(0.2, 0.05, 0.2));
        waterMovement = transform.rotateVector(new Vec3(0.0, 3.5, 0.0));
        waterBlock = transform.toAbsolute(transform.rotatePoint(new Vec3(0.5, 4.5, 0.5)));
        hit = new BoxHit.Relative(content.origin, new Vec3(0, 1.5, 0), new Vec3(1, 5, 1));
        transform.applyRotationToRelativeHit(hit);
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.at(0.0)) {
            if (setBlocked) level.setBlockAndUpdate(BlockUtil.toBlockPos(waterBlock), Blocks.AIR.defaultBlockState());
            setBlocked = false;
        }
        if (process.at(0.2)) {
            vfxPositions.clear();
        }
        if (process.in(0.2, 2.0)) {
            List<ServerPlayer> players = hit.detectPlayers(game);
            for (ServerPlayer player : players) {
                double effect = Math.max(Math.min(1 - (player.position().distanceTo(transform.toAbsolute(startPos)) / 3.5), 0.8), 0.3);
                Vec3 movement = pushMovement.scale(effect).add(player.getDeltaMovement().multiply(effectMovement));
                player.setDeltaMovement(movement);
                PlayerUtil.updateDeltaMovement(server.getPlayerList().getPlayers(), player);
            }
            vfx(process.progress(0.2, 2.0));
        }
        if (process.at(1.0)) {
            BlockPos pos = BlockUtil.toBlockPos(waterBlock);
            if (content.checkCanPlace(pos, level.getBlockState(pos))) {
                level.setBlockAndUpdate(pos, Blocks.BARRIER.defaultBlockState());
                setBlocked = true;
            }
        }
    }

    private void vfx(double progress) {
        if (progress <= 0.75) {
            progress*=(1.0/0.75);
            Vec3 pos = transform.toAbsolute(startPos.add(waterMovement.scale(progress)));
            DustParticleOptions dustParticleOptions = new DustParticleOptions(ARGB.color(new Vec3(0, 0.87058823529f, 1)), 0.6f);
            vfxPositions.add(new Tuple<>(pos, dustParticleOptions));
            level.sendParticles(dustParticleOptions, pos.x, pos.y, pos.z, 12,
                    Math.abs(endMovement.x), Math.abs(endMovement.y), Math.abs(endMovement.z), 0);
        } if (progress >= 0.75) {
            Vec3 pos = transform.toAbsolute(startPos.add(waterMovement));
            DustParticleOptions dustParticleOptions = new DustParticleOptions(ARGB.color(new Vec3(0, 0.87058823529f, 1)), 0.8f);
            level.sendParticles(dustParticleOptions, pos.x, pos.y, pos.z, 12,
                    Math.abs(endMovement.x), Math.abs(endMovement.y), Math.abs(endMovement.z), 0);
        }
        for (Tuple<Vec3, DustParticleOptions> entry : vfxPositions) {
            Vec3 pos = entry.getA();
            DustParticleOptions dustParticleOptions = entry.getB();
            level.sendParticles(dustParticleOptions, pos.x, pos.y, pos.z, 6,
                    Math.abs(deltaMovement.x), Math.abs(deltaMovement.y), Math.abs(deltaMovement.z), 0);
        }
    }
}
