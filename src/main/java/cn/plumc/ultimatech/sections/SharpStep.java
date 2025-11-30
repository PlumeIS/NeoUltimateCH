package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.DisplayEntityUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

public class SharpStep extends Section {
    BoxHit.Relative triggerHit;
    BoxHit.Relative deathHit;
    boolean triggered = false;
    boolean initialized = false;

    public SharpStep(ServerPlayer player, Game game) {
        // 0.8 start 1.0 damage 2.8 end damage 3.2 end (64ticks)
        super(player, SectionLocation.get(1, 8), game);
        setProcess(0);
    }

    public void init(){
        if (initialized) return;
        transform.createItemDisplay("minecraft:purple_carpet", new Vec3(0.5, 0.5, 0.5), UCHInfos.contentID("section.control.spike"), null);
        transform.createItemDisplay("minecraft:purple_carpet", new Vec3(0.5, 0.5, 1.5), UCHInfos.contentID("section.control.spike"), null);
        transform.createItemDisplay("minecraft:purple_carpet", new Vec3(0.5, 0.5, 2.5), UCHInfos.contentID("section.control.spike"), null);
        triggerHit = new BoxHit.Relative(content.origin, new Vec3(0, 1, 0), new Vec3(1, 1.2, 3));
        deathHit = new BoxHit.Relative(content.origin, new Vec3(0, 1, 0), new Vec3(1, 1.6, 3));
        transform.applyRotationToRelativeHit(triggerHit);
        transform.applyRotationToRelativeHit(deathHit);
        initialized = true;
        setSpikeVisible(false);
    }

    @Override
    public void tickRun(int tickTime) {
        if (!initialized) return;
        if (!triggered) {
            if (Objects.nonNull(triggerHit.detectPlayer(game))) {
                triggered = true;
                setProcess(SectionCounter.toTicks(3.2));
                process.start();
            }
        }
        if (!triggered) return;

        List<Entity> spikes = content.getContentEntities(UCHInfos.contentID("section.control.spike"));
        if (process.at(0)) {
            setSpikeVisible(true);
            vfx(new Vec3(1, 0, 0));
        }
        if (process.at(0.8)){
            spikes.forEach(entity -> transform.moveEntityRelative(entity, new Vec3(0, 1, 0), 0.2));
        }
        if (process.in(1.0, 2.8)) {
            killAll(deathHit.detectPlayers(game));
        }
        if (process.at(2.8)) {
            vfx(new Vec3(0, 1, 0));
            spikes.forEach(entity -> transform.moveEntityRelative(entity, new Vec3(0, 0, 0), 0.4));
        }
        if (process.at(3.2)) {
            setSpikeVisible(false);
            setProcess(0);
            process.start();
            triggered = false;
        }
    }

    private void setSpikeVisible(boolean visible){
        List<Entity> entities = content.getContentEntities(UCHInfos.contentID("section.control.spike"));
        for (Entity entity : entities) {
            DisplayEntityUtil.setVisible(entity, "minecraft:purple_carpet", visible);
        }
    }

    @Override
    public void onRoundEnd() {
        List<Entity> spikes = content.getContentEntities(UCHInfos.contentID("section.control.spike"));
        spikes.forEach(entity -> transform.moveEntityRelative(entity, new Vec3(0, 0, 0), 0.4));
        setSpikeVisible(false);
        setProcess(0);
        process.stop();
        triggered = false;
    }

    private void vfx(Vec3 color){
        List<Vec3> boxPoints = transform.generateOutlinePoints(0.2, 0.2);
        for (Vec3 boxPoint : boxPoints) {
            level.sendParticles(new DustParticleOptions(color.toVector3f(), 1), boxPoint.x, boxPoint.y, boxPoint.z, 1, 0, 0, 0, 0);
        }
    }
}
