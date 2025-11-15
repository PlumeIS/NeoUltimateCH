package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Spring extends Section {
    public BoxHit.Relative triggerHit;
    public Vec3 springMotion;
    public Vec3 baseMotion;
    public boolean triggered = false;
    public Vec3 baseSpringScale = new Vec3(1.0, 1.2, 1.0);
    public Vec3 springScale = new Vec3(1.0, 1.5, 1.0);
    public Vec3 springMovement;
    public Vec3 boardMovement;


    public Spring(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(4, 8), game);
        setProcess(0);
    }

    @Override
    public void init() {
        triggerHit = new BoxHit.Relative(content.origin, new Vec3(0.0, 1.0, 0.0), new Vec3(2.0, 1.4, 2.0));
        transform.applyRotationToRelativeHit(triggerHit);
        springMotion = transform.rotateVector(new Vec3(0.0, 1.0, 0.0));
        baseMotion = transform.toNonNegative(transform.rotateVector(new Vec3(1.0, 0.0, 1.0)));
        springMovement = transform.rotatePoint(new Vec3(0.0, 0.2, 0.0));
        boardMovement = transform.rotatePoint(new Vec3(0.0, 0.4, 0.0));
    }

    @Override
    public void tickRun(int tickTime) {
        triggerHit.detectPlayers(game).forEach(player -> {
            player.setDeltaMovement(player.getDeltaMovement().multiply(baseMotion).add(springMotion));
            PlayerUtil.updateDeltaMovement(game.getPlayerManager().getPlayers(), player);
            if (!triggered) {
                triggered = true;
                setProcess(SectionCounter.toTicks(0.3));
                process.start();
            }
        });
        if (triggered) {
            Entity spring = content.getContentEntity("uch.spring.spring");
            Entity board = content.getContentEntity("uch.spring.springboard");
            if (process.at(0.0)) {
                transform.applyEntityScale(spring, springScale, 0.15);
                transform.moveEntityRelative(spring, springMovement, 0.15);
                transform.moveEntityRelative(board, boardMovement, 0.15);
            }
            if (process.at(0.15)) {
                transform.applyEntityScale(spring, baseSpringScale, 0.15);
                transform.moveEntityRelative(spring, Vec3.ZERO, 0.15);
                transform.moveEntityRelative(board, Vec3.ZERO, 0.15);
            }
            if (process.at(0.3)) {
                triggered = false;
                setProcess(0);
                process.start();
            }
        }
    }
}
