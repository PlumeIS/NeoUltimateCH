package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Fist extends Section {
    public BoxHit.Relative topCheck;
    public BoxHit.Relative bottomCheck;
    public BoxHit.Relative backCheck;
    public BoxHit.Relative hit;

    public int activators = 0;
    public boolean active = false;
    public boolean spring = false;
    public boolean backing = false;

    public Vec3 springScale = new Vec3(0.5, 0.5, 4.0);
    public Vec3 springMovement = new Vec3(0.0, -0.5, -1.1);
    public Vec3 fistMovement = new Vec3(0.0, 0.0, 0.9);

    public Fist(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(2, 6), game);
        setProcess(0);
    }

    @Override
    public void init() {
        topCheck = new BoxHit.Relative(content.origin, new Vec3(0.0, 1.0, 0.0), new Vec3(1.0, 1.2, 1.0));
        bottomCheck = new BoxHit.Relative(content.origin, new Vec3(0.0, 0.0, 0.0), new Vec3(1.0, -0.2, 1.0));
        backCheck = new BoxHit.Relative(content.origin, new Vec3(0.0, 0.0, -0.2), new Vec3(1.0, 1.0, 0.0));
        hit = new BoxHit.Relative(content.origin, new Vec3(0.0, 0.0, 1.0), new Vec3(1.0, 1.0, 3.0));
        transform.applyRotationToRelativeHit(topCheck);
        transform.applyRotationToRelativeHit(bottomCheck);
        transform.applyRotationToRelativeHit(backCheck);
        transform.applyRotationToRelativeHit(hit);
    }

    @Override
    public void tickRun(int tickTime) {
        activators = topCheck.detectPlayers(game).size()+bottomCheck.detectPlayers(game).size()+backCheck.detectPlayers(game).size();
        if (!active) {
            if (activators > 0) {
                active = true;
                spring = true;
                setProcess(Integer.MAX_VALUE);
                process.start();
                vfx(false, true, false);
                return;
            }
        }

        Entity springEntity = content.getContentEntity("uch.fist.spring");
        Entity fistEntity = content.getContentEntity("uch.fist.fist");
        if (active&&spring&&!backing) {
            if (process.at(0.1)) {
                transform.applyEntityScale(springEntity, springScale, 0.1);
                transform.moveEntityRelative(springEntity, springMovement, 0.1);
                transform.moveEntityRelative(fistEntity, fistMovement, 0.1);
                vfx(true, false, false);
            }
            if (process.in(0.1, 0.3)) {
                killAll(hit.detectPlayers(game));
            }
            if (process.at(0.3)) {
                spring = false;
            }
        }

        if (active&&!spring&&!backing) {
            if (activators == 0) {
                backing = true;
                setProcess(Integer.MAX_VALUE);
                process.start();
                vfx(false, false, true);
                return;
            }
        }
        if (active&&backing) {
            if (process.at(0.1)) {
                transform.applyEntityScale(springEntity, new Vec3(1.0, 1.0, 1.0), 0.1);
                transform.moveEntityRelative(springEntity, springMovement.scale(0.0), 0.1);
                transform.moveEntityRelative(fistEntity, fistMovement.scale(0.0), 0.1);
            }
            if (process.at(0.2)) {
                active = false;
                backing = false;
            }
        }
    }

    public void vfx(boolean spring, boolean pushing, boolean releasing){
        if (spring) {
            Vec3 explodePoint = new Vec3(0.5, 0.5, 2.5);
            Vec3 point = transform.toAbsolute(transform.rotatePoint(explodePoint));
            level.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z,
                    1, 0, 0, 0, 0);
            level.playSound(null, point.x, point.y, point.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        Vec3 soundPoint = transform.toAbsolute(transform.rotatePoint(new Vec3(0.5, 0.5, 0.5)));
        if (pushing) {
            level.playSound(null, soundPoint.x, soundPoint.y, soundPoint.z,
                    SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        if (releasing) {
            level.playSound(null, soundPoint.x, soundPoint.y, soundPoint.z,
                    SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
