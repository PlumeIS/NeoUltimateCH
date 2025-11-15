package cn.plumc.ultimatech.section.hit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlayerHit extends Hit{
    private final ServerPlayer player;

    public PlayerHit(ServerPlayer player) {
        this.player = player;
    }

    public AABB getAABB() {
        Vec3 position = player.position();
        double x = position.x;
        double y = position.y;
        double z = position.z;
        AABB aabb;
        if (!player.isShiftKeyDown()) {
            aabb = new AABB(x-0.3, y, z-0.3, x+0.3, y+1.8, z+0.3);
        } else {
            aabb = new AABB(x-0.3, y, z+0.3, x+0.3, y+1.0, z+0.3);
        }
        return aabb;
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
        return getAABB().intersects(new PlayerHit(player).getAABB());
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
