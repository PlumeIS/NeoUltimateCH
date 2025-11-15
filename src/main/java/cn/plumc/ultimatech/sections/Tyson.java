package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.*;
import cn.plumc.ultimatech.section.hit.BoxHit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

public class Tyson extends Section {
    public BoxHit.Relative triggerFront;
    public BoxHit.Relative triggerBack;
    public boolean triggered;
    public Vec3 scale = new Vec3(0.8, 1.5, 0.8);
    public boolean back = false;


    public Tyson(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3, 7), game);
        setProcess(0);
    }

    @Override
    public void init() {
        triggerFront = new BoxHit.Relative(content.origin, new Vec3(-0.5, 1, -0.5), new Vec3(0.5, 3, 0.5));
        triggerBack = new BoxHit.Relative(content.origin, new Vec3(0.5, 1, 0.5), new Vec3(1.5, 3, 1.5));
        transform.applyRotationToRelativeHit(triggerFront);
        transform.applyRotationToRelativeHit(triggerBack);
        scale = transform.rotateVector(scale);
    }

    @Override
    public void tickRun(int tickTime) {
        if (!triggered&&(triggerFront.hasPlayer(game)||triggerBack.hasPlayer(game))) {
            if (triggerBack.hasPlayer(game)) back = true;

            triggered = true;
            setProcess(SectionCounter.toTicks(1.2));
            process.start();
            return;
        }
        Entity tyson = content.getContentEntity("uch.tyson");
        if (triggered&&process.at(0.0)) {
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            if (back) rotations.put(SectionRotation.Axis.Y, 180.0);
            transform.applyEntityRotation(tyson, rotations, 0.1);
        }
        if (triggered&&process.at(0.1)) {
            transform.applyEntityScale(tyson, scale, 0.2);
        }
        if (triggered&&process.at(0.3)) {
            transform.applyEntityScale(tyson, new Vec3(1.0, 1.0, 1.0), 0.3);
        }
        if (triggered&&process.at(0.6)) {
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            if (back) rotations.put(SectionRotation.Axis.Y, 300.0);
            else rotations.put(SectionRotation.Axis.Y, 120.0);
            transform.applyEntityRotation(tyson, rotations, 0.2);
        }
        if (triggered&&process.at(0.8)) {
            HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
            if (back) rotations.put(SectionRotation.Axis.Y, 180.0);
            transform.applyEntityRotation(tyson, rotations, 0.2);
        }
        if (triggered&&process.at(1.0)) {
            if (back) killAll(triggerBack.detectPlayers(game));
            else killAll(triggerFront.detectPlayers(game));
        }
        if (triggered&&process.at(1.2)) {
            triggered = false;
            back = false;
            setProcess(0);
            process.start();
        }
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        setProcess(0);
    }
}
