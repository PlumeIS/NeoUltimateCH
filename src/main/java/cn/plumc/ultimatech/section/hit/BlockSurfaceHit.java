package cn.plumc.ultimatech.section.hit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BlockSurfaceHit extends Hit{
    BlockPos pos;
    List<AABB> aabbs;

    public BlockSurfaceHit(Vec3 pos) {
        this(new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z)));
    }

    public BlockSurfaceHit(BlockPos pos) {
        this.pos = pos;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        double offset = 1.0E-5F;
        this.aabbs = List.of(
                new AABB(x + offset, y + 1, z + offset, x + 1 - offset, y + 1.2 - offset, z + 1 - offset), // TOP
                new AABB(x + offset, y - 0.2 + offset, z + offset, x + 1 - offset, y, z + 1 - offset), // BOTTOM
                new AABB(x + offset, y + offset, z - 0.2 + offset, x + 1 - offset, y + 1 - offset, z), // LEFT
                new AABB(x + offset, y + offset, z + 1, x + 1 - offset, y + 1 - offset, z + 1.2 - offset), // RIGHT
                new AABB(x + 1, y + offset, z + offset, x + 1.2 - offset, y + 1 - offset, z + 1 - offset), // FRONT
                new AABB(x - 0.2 + offset, y + offset, z + offset, x, y + 1 - offset, z + 1 - offset) // BACK
        );
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public boolean intersect(Hit other) {
        for (AABB aabb : aabbs) {
            if (other.intersectAABB(aabb)) return true;
        }
        return false;
    }

    @Override
    public boolean intersectAABB(AABB aabb) {
        for (AABB bb : aabbs) {
            if (bb.intersects(aabb)) return true;
        }
        return false;
    }

    @Override
    public boolean intersectPlayer(ServerPlayer player) {
        PlayerHit hit = new PlayerHit(player);
        for (AABB bb : aabbs) {
            if (hit.intersectAABB(bb)) return true;
        }
        return false;
    }
}
