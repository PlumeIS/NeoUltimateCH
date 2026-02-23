package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.section.hit.EntityHit;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Honeycomb extends Section {
    public final int BEE_COUNT = 20;

    public BoxHit.Relative triggerHit;
    public boolean triggered;
    public ServerPlayer target;
    public PlayerTeam beeTeam = new PlayerTeam(new Scoreboard(), "uch.bee.team"){
        @Override
        public @NotNull CollisionRule getCollisionRule() {
            return CollisionRule.NEVER;
        }
    };
    public List<Bee> bees = new ArrayList<>();

    public Honeycomb(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(2, 7), game);
        setProcess(0);
    }

    @Override
    public void init() {
        triggerHit = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0, 0, 0), new Vec3(2.0, 2.0, 2.0));
        transform.applyRotationToRelativeHit(triggerHit);
    }

    @Override
    public void tickRun(int tickTime) {
        if (triggered) {
            bees.forEach(bee -> killAll(new EntityHit(bee).detectPlayers(game)));
        };

        ServerPlayer target = triggerHit.detectPlayer(game);
        if (!triggered&&Objects.nonNull(target)) {
            triggered = true;
            this.target = target;
            setProcess(SectionCounter.toTicks(0.5), -1, false);
            process.start();
        }
        if (process.at(0.5)) {
            Vec3 spawnPoint = transform.toAbsolute(transform.rotatePoint(new Vec3(1.0, 1.0, 1.0)));
            vfx();
            for (int i = 0; i < BEE_COUNT; i++) {
                Bee bee = spawnBee(spawnPoint, this.target);
                level.addFreshEntity(bee);
                bees.add(bee);
            }
        }
    }

    private Bee spawnBee(Vec3 spawnPoint, ServerPlayer target) {
        Bee bee = new Bee(EntityType.BEE, level){
            @Override
            public @Nullable PlayerTeam getTeam() {
                return beeTeam;
            }

            @Override
            public boolean doHurtTarget(@NotNull Entity entity) {
                boolean result = super.doHurtTarget(entity);
                if (result && entity instanceof ServerPlayer player) Honeycomb.this.kill(player);
                bees.forEach(bee -> bee.kill());
                bees.clear();
                return result;
            }

            @Override
            public boolean onGround() {
                return true;
            }
        };
        bee.setAge(Integer.MIN_VALUE);
        bee.setPos(spawnPoint.x, spawnPoint.y, spawnPoint.z);
        bee.setTarget(target);
        bee.getAttribute(Attributes.FLYING_SPEED).setBaseValue(5);
        bee.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.5);
        return bee;
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        triggered = false;
        bees.forEach(bee -> bee.kill());
        bees.clear();
        setProcess(0);
    }

    private void vfx(){
        Vec3 center = triggerHit.getAABB().getCenter();
        level.sendParticles(
                new DustParticleOptions(new Vec3(1.0f, 0.8235f, 0.09411f).toVector3f(), 0.8f),
                center.x() + 0.5,
                center.y() + 0.5,
                center.z() + 0.5,
                10,
                2.25,
                2.25,
                2.25,
                1.0);
    }
}
