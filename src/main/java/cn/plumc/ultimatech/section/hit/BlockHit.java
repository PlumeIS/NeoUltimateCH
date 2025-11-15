package cn.plumc.ultimatech.section.hit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockHit extends Hit{
    public final BlockPos pos;
    private final AABB aabb;

    public BlockHit(BlockPos pos) {
        this.pos = pos;
        this.aabb = new AABB(pos);
    }

    public BlockHit(Vec3 pos) {
        BlockPos blockPos = new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
        this.pos = blockPos;
        this.aabb = new AABB(blockPos);
    }

    public AABB getAABB() {
        return aabb;
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
}
