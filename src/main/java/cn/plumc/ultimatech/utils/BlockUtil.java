package cn.plumc.ultimatech.utils;

import cn.plumc.ultimatech.section.SectionRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class BlockUtil {
    public static boolean isMultiBlock(ServerLevel world, BlockPos pos){
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BedBlock) return true;
        if (block instanceof DoorBlock) return true;
        return false;
    }

    public static List<BlockPos> getBlocksFromAABB(AABB aabb) {
        List<BlockPos> blocks = new ArrayList<>();

        int minX = (int) Math.floor(aabb.minX);
        int maxX = (int) Math.floor(aabb.maxX);
        int minY = (int) Math.floor(aabb.minY);
        int maxY = (int) Math.floor(aabb.maxY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxZ = (int) Math.floor(aabb.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }

        return blocks;
    }

    public static BlockPos toBlockPos(Vec3 pos) {
        return new BlockPos(Mth.floor(pos.x), Mth.floor(pos.y), Mth.floor(pos.z));
    }

    public static Vec3 toVec3(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    public static Vec3 getRandomPointInAABB(AABB aabb) {
        Random random = new Random();
        double x = random.nextDouble() * (aabb.maxX - aabb.minX) + aabb.minX;
        double y = random.nextDouble() * (aabb.maxY - aabb.minY) + aabb.minY;
        double z = random.nextDouble() * (aabb.maxZ - aabb.minZ) + aabb.minZ;
        return new Vec3(x, y, z);
    }

    public static List<Vec3> generateOutlinePoints(double step, AABB aabb) {
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
        List<Vec3> points = new ArrayList<>(Arrays.asList(vertices));
        int[][] edges = new int[][]{
                {0, 1}, {0, 2}, {0, 4},
                {1, 3}, {1, 5},
                {2, 3}, {2, 6},
                {3, 7},
                {4, 5}, {4, 6},
                {5, 7},
                {6, 7}
        };
        for (int[] edge : edges) {
            Vec3 a = vertices[edge[0]];
            Vec3 b = vertices[edge[1]];
            Vec3 dir = b.subtract(a);
            double len = dir.length();
            Vec3 unit = dir.scale(1.0 / len);

            for (double t = 0; t < len; t += step) {
                Vec3 p = a.add(unit.scale(t));
                points.add(p);
            }
            points.add(b);
        }
        return points;
    }
}
