package cn.plumc.ultimatech.section.hit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityHit extends Hit{
    private final Entity entity;

    public EntityHit(Entity entity) {
        this.entity = entity;
    }

    private AABB getAABB() {
        Vec3 position = entity.position();
        double x = position.x;
        double y = position.y;
        double z = position.z;
        float bbHeight = entity.getBbHeight();
        float bbWidth = entity.getBbWidth();
        return new AABB(x-bbWidth/2, y, z-bbWidth/2, x+bbWidth, y+bbHeight, z+bbWidth);
    }

    @Override
    public boolean intersect(Hit other) {
        return other.intersectAABB(getAABB());
    }

    @Override
    public boolean intersectAABB(AABB aabb) {
        return getAABB().intersects(aabb);
    }

    @Override
    public boolean intersectPlayer(ServerPlayer player) {
        return new PlayerHit(player).intersectAABB(getAABB());
    }
}
