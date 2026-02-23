package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.ColorUtil;
import cn.plumc.ultimatech.utils.FixedLinkedList;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BlackHole extends Section {
    public static final double BLACK_HOLE_MASS = 3.0e9;
    public static final double PLAYER_MASS = 70;

    public BoxHit.Relative hit;
    public BoxHit.Relative detectionRange;
    public Vec3 center;
    public BlockHolePhysical physical;

    public FixedLinkedList<Tuple<DustParticleOptions, Vec3>> trackingParticles = new FixedLinkedList<>(2000);

    public BlackHole(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3, 3), game);
        setProcess(1);
    }

    @Override
    public void init() {
        center = new Vec3(0.5, 0.5, 0.5);
        transform.rotateVector(center);
        physical = new BlockHolePhysical(transform.toAbsolute(center), BLACK_HOLE_MASS);
        hit = new BoxHit.Relative(()->content.getOrigin(), new Vec3(-0.2, -0.2, -0.2), new Vec3(1.2, 1.2, 1.2));
        detectionRange = new BoxHit.Relative(()->content.getOrigin(), new Vec3(-4.0, -4.0, -4.0), new Vec3(5.0, 5.0, 5.0));
        transform.applyRotationToRelativeHit(hit);
    }

    @Override
    public void tickRun(int tickTime) {
        List<Entity> entities = detectionRange.detectEntities(level, entity -> {
            if (entity instanceof ServerPlayer player) {
                return game.getStatus().getPlayings().stream().map(Entity::getUUID).anyMatch(uuid->uuid.equals(player.getUUID()));
            }
            else return true;
        });
        for (Entity entity : entities) {
            vfx(entity, tickTime);
            Vec3 deltaVelocity = physical.getDeltaVelocity(entity.position(), PLAYER_MASS, 0.05);
            entity.setDeltaMovement(entity.getDeltaMovement().add(deltaVelocity));
            PlayerUtil.updateDeltaMovement(server.getPlayerList().getPlayers(), entity);
        }
        vfx(null, tickTime);
        killAll(hit.detectPlayers(game));
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        trackingParticles.clear();
    }

    private void vfx(Entity entity, int tick) {
        if (entity instanceof ServerPlayer player) {
            PlayerUtil.PlayerTexture texture = PlayerUtil.textureCache.get(player.getUUID());
            if (texture.isLoaded()) {
                Vec3 point = PlayerUtil.getRandomPointInPlayerAABB(player);
                Integer randomColor = texture.getColorRandom().getRandomColor();
                DustParticleOptions dust = new DustParticleOptions(ColorUtil.toColor(randomColor).mul(1f/255f), 0.6f);
                level.sendParticles(dust, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                trackingParticles.add(new Tuple<>(dust, point));
            }
        }
        if (tick == 1) {
            for (Tuple<DustParticleOptions, Vec3> trackingParticle : ImmutableList.copyOf(trackingParticles)) {
                if (trackingParticle.getB().distanceTo(transform.toAbsolute(center)) < 0.5) {
                    trackingParticles.remove(trackingParticle);
                }
                Vec3 delta = transform.toAbsolute(center).subtract(trackingParticle.getB()).normalize().scale(0.1);
                Vec3 point = trackingParticle.getB().add(delta);
                trackingParticle.setB(point);
                level.sendParticles(trackingParticle.getA(), point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static class BlockHolePhysical{
        private static final double G = 6.67430e-11;
        private final Vec3 center;
        private final double M;

        public BlockHolePhysical(Vec3 center, double M){
            this.center = center;
            this.M = M;
        }

        public double getAcceleration(Vec3 pos, double m){
            double r = pos.distanceTo(center);
            return G * (M*m)/(Math.pow(r, 2));
        }

        public Vec3 getAccelerationVec(Vec3 pos, double m){
            return center.subtract(pos).normalize().scale(getAcceleration(pos, m));
        }

        public Vec3 getDeltaVelocity(Vec3 pos, double m, double t){
            Vec3 accelerationVec = getAccelerationVec(pos, m);
            return accelerationVec.scale(t);
        }
    }
}
