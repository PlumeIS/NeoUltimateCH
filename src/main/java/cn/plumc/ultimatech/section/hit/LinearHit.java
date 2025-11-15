package cn.plumc.ultimatech.section.hit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class LinearHit extends Hit {
    public Vec3 start;
    public Vec3 end;
    public double radius;
    public double step;

    private final List<BoxHit> hits = new ArrayList<>();
    private final List<Vec3> hitPoints = new ArrayList<>();

    public LinearHit(Vec3 start, Vec3 end, double radius, double step) {
        this.start = start;
        this.end = end;
        this.radius = radius;
        this.step = step;
        calculate();
    }

    public void setStep(double step) {
        this.step = step;
        calculate();
    }

    public void setRadius(double radius) {
        this.radius = radius;
        calculate();
    }

    public void set(Vec3 start, Vec3 end){
        this.start = start;
        this.end = end;
        calculate();
    }

    private void calculate() {
        hitPoints.clear();
        hits.clear();

        Vec3 direction = end.subtract(start);
        double length = direction.length();
        if (length == 0) return;

        Vec3 stepDir = direction.normalize().scale(step);
        int steps = (int) Math.ceil(length / step);

        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(stepDir.scale(i));
            Vec3 point1 = new Vec3(pos.x - radius, pos.y - radius, pos.z - radius);
            Vec3 point2 = new Vec3(pos.x + radius, pos.y + radius, pos.z + radius);
            hitPoints.add(pos);
            hits.add(new BoxHit(point1, point2));
        }
    }

    public List<Vec3> getHitPoints() {
        return hitPoints;
    }

    @Override
    public boolean intersect(Hit other) {
        return hits.stream().anyMatch(hit -> other.intersectAABB(hit.getAABB()));
    }

    @Override
    public boolean intersectAABB(AABB aabb) {
        return hits.stream().anyMatch(hit -> aabb.intersects(hit.getAABB()));
    }

    @Override
    public boolean intersectPlayer(ServerPlayer player) {
        return intersect(new PlayerHit(player));
    }
}
