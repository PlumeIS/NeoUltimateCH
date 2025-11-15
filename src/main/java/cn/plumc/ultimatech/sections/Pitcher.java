package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.utils.FixedLinkedList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class Pitcher extends Section {
    public Vec3 motion;
    public Vec3 spawnPoint;

    public FixedLinkedList<Snowball> snowballs = new FixedLinkedList<>(UCHInfos.SECTION_MAX_ENTITIES){
        @Override
        public Snowball removeFirst() {
            getFirst().kill();
            return super.removeFirst();
        }
    };

    public Pitcher(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3, 1), game);
        setProcess(SectionCounter.toTicks(1.5));
    }

    @Override
    public void init() {
        motion = transform.rotateVector(new Vec3(0.0, 0.515, 0.355));
        spawnPoint = transform.toAbsolute(transform.rotateVector(new Vec3(0.5, 1.0, 1.0)));
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.at(1.5)) {
            Snowball snowball = new Snowball(EntityType.SNOWBALL, level){
                @Override
                protected void onHitEntity(EntityHitResult result) {
                    if (result.getEntity() instanceof ServerPlayer player) Pitcher.this.kill(player);
                }
            };
            snowball.addTag("uch.pitcher.snowball");
            snowball.setDeltaMovement(motion);
            snowball.setPos(spawnPoint);
            level.addFreshEntity(snowball);
            snowballs.add(snowball);
        }
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        snowballs.forEach(snowball -> snowball.kill());
        snowballs.clear();
    }
}
