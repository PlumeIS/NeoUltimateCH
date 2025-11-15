package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public class MagneticStep extends Section {
    public BoxHit.Relative triggerHit;
    public boolean triggered;

    public MagneticStep(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(2, 8), game);
        setProcess(0);
    }

    @Override
    public void init() {
        triggerHit = new BoxHit.Relative(content.origin, new Vec3(0.0, 1.0, 0.0), new Vec3(1.0, 1.2, 2.0));
        transform.applyRotationToRelativeHit(triggerHit);
    }

    @Override
    public void tickRun(int tickTime) {
        if (!triggered&&Objects.nonNull(triggerHit.detectPlayer(game))) {
            setProcess(SectionCounter.toTicks(0.5), -1, false);
            process.start();
            triggered = true;
        }
        if (process.at(0.5)) {
            content.blocks.forEach((pos, block) -> level.destroyBlock(pos, false));
        }
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        content.blocks.forEach((pos, block) -> level.setBlockAndUpdate(pos, block));
        setProcess(0);
    }
}
