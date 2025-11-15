package cn.plumc.ultimatech.section.hit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class BoxHit extends Hit{
    public Vec3 pos1;
    public Vec3 pos2;
    AABB aabb;

    public BoxHit(Vec3 pos1, Vec3 pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.aabb = new AABB(pos1, pos2);
    }

    public AABB getAABB() {
        return aabb;
    }

    public List<Entity> detectEntities(ServerLevel level) {
        List<Entity> entities = new ArrayList<>();
        AABB aabb = this.getAABB();
        level.getEntities().get(aabb, entities::add);
        return entities;
    }

    public List<Entity> detectEntities(ServerLevel level, Predicate<Entity> test) {
        List<Entity> entities = new ArrayList<>();
        AABB aabb = this.getAABB();
        level.getEntities().get(getAABB(), entity -> {if(test.test(entity)) entities.add(entity);});
        return entities;
    }

    @Override
    public boolean intersect(Hit other) {
        return other.intersectAABB(aabb);
    }

    @Override
    public boolean intersectAABB(AABB aabb) {
        return this.aabb.intersects(aabb);
    }

    @Override
    public boolean intersectPlayer(ServerPlayer player) {
        return false;
    }

    public static class Relative extends BoxHit {
        public Vec3 origin;

        public Relative(Vec3 origin, Vec3 pos1, Vec3 pos2) {
            super(pos1, pos2);
            this.origin = origin;
            setOrigin(origin);
        }

        public void setPos(Vec3 pos1, Vec3 pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
            setOrigin(origin);
        }

        public void setOrigin(Vec3 pos) {
            this.origin = pos;
            this.aabb = new AABB(pos1.add(pos), pos2.add(pos));
        }

        @Override
        public String toString() {
            return "Relative{" +
                    "origin=" + origin +
                    ", pos1=" + pos1 +
                    ", pos2=" + pos2 +
                    ", aabb=" + aabb +
                    '}';
        }
    }
}
