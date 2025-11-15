package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ChainSaw extends Section {
    public Vec3 end;
    public Vec3 hitStart;
    public Vec3 hitEnd;

    public ChainSaw(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(5, 0), game);
        setProcess(SectionCounter.toTicks(4.0));
    }

    @Override
    public void init() {
        end = transform.rotatePoint(new Vec3(0, 0, 5));
        hitStart = transform.rotatePoint(new Vec3(0, 1, 0));
        hitEnd = transform.rotatePoint(new Vec3(1, 2, 1));
    }

    @Override
    public void tickRun(int tickTime) {
        Entity sawEntity = content.getContentEntity("uch.chain_saw.saw");
        Vec3 movement;
        if (process.in(0.0, 2.0)) movement = end.scale(process.progress(0.0, 2.0));
        else movement = end.scale(1.0-process.progress(2.0, 4.0));

        transform.moveEntityRelative(sawEntity, movement, 0.05);
        killAll(new BoxHit.Relative(content.origin, hitStart.add(movement), hitEnd.add(movement)).detectPlayers(game));
    }
}
