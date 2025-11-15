package cn.plumc.ultimatech.section.hit;

import cn.plumc.ultimatech.sections.Pendulum;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PendulumHit extends Hit{
    private final Pendulum pendulum;
    private final BoxHit.Relative relative;

    public PendulumHit(Pendulum pendulum){
        this.pendulum = pendulum;
        relative = new BoxHit.Relative(pendulum.content.origin, new Vec3(0.0, 0.5, 0.5), new Vec3(1, 2.5, 2.5));
        pendulum.transform.applyRotationToRelativeHit(relative);
    }

    public void step(){
        Vec3 center = pendulum.content.getRelativeEntityPosition("uch.pendulum.frame");
        Vec3 position = center.add(new Vec3(0, pendulum.pendulum.getY(), -pendulum.pendulum.getX()));
        relative.setPos(new Vec3(0, position.y-1, position.z-1), new Vec3(1, position.y+1, position.z+1));
        pendulum.transform.applyRotationToRelativeHit(relative);
    }

    private boolean checkVertices(AABB aabb, Vec3 center, double distance){
        Vec3 min = aabb.getMinPosition();
        Vec3 max = aabb.getMaxPosition();
        Vec3[] vertices = new Vec3[]{
                new Vec3(min.x, min.y, min.z),
                new Vec3(max.x, min.y, min.z),
                new Vec3(min.x, max.y, min.z),
                new Vec3(max.x, max.y, min.z),
                new Vec3(min.x, min.y, max.z),
                new Vec3(max.x, min.y, max.z),
                new Vec3(min.x, max.y, max.z),
                new Vec3(max.x, max.y, max.z)
        };
        for (Vec3 vertex : vertices){
            if (vertex.distanceTo(center) <= distance) return true;
        }
        return false;
    }

    @Override
    public boolean intersect(Hit other) {
        // Inaccurate
        step();
        return other.intersectAABB(relative.getAABB());
    }

    @Override
    public boolean intersectAABB(AABB aabb) {
        step();
        return relative.intersectAABB(aabb)&&checkVertices(aabb, relative.getAABB().getCenter(), 2);
    }

    @Override
    public boolean intersectPlayer(ServerPlayer player) {
        step();
        return intersectAABB(new PlayerHit(player).getAABB());
    }
}
