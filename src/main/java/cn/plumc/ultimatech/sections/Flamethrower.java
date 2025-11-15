package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.LinearHit;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Flamethrower extends Section {
    public LinearHit hit;

    public Flamethrower(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3,0), game);
        setProcess(SectionCounter.toTicks(3.0));
    }

    @Override
    public void init() {
        hit = new LinearHit(
                transform.toAbsolute(new Vec3(0.15, 0.95, 1.0)),
                transform.toAbsolute(new Vec3(0.15, 3.0, 3.0)),
                0.9,
                0.1);
        transform.applyRotationToLinearHit(hit);
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.in(0.0, 1.5)) {
            vfx(tickTime);
        }
        if (process.in(1.1, 1.5)) {
            killAll(hit.detectPlayers(game));
        }
    }

    private void vfx(int tick) {
        Vector3f color = new Vec3(1f, 0.34901960784f, 0f).toVector3f();
        double offset = 0;
        double d_offset = 0.01;
        for (int i = 0; i < hit.getHitPoints().size(); i++) {
            offset+=d_offset;
            Vec3 pos = hit.getHitPoints().get(i);
            if (i % 10 == 0 && tick % 15 == 0 ) {
                level.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 1, 0, 0, 0, 1);
            }
            if (i % 3 == 0) {
                level.sendParticles(new DustParticleOptions(color, 0.8f), pos.x, pos.y, pos.z, 1, offset, offset, offset, 1);
            }
        }
    }
}
