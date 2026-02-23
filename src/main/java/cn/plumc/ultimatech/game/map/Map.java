package cn.plumc.ultimatech.game.map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class Map {
    public Region mapRegion;
    public Region startRegion;
    public Region winRegion;
    public int deathHeight;
    public int dayTime;
    public int minStartPlayer;

    public Map(Region mapRegion, Region startRegion, Region winRegion, int deathHeight, int dayTime, int minStartPlayer) {
        this.mapRegion = mapRegion;
        this.startRegion = startRegion;
        this.winRegion = winRegion;
        this.deathHeight = deathHeight;
        this.dayTime = dayTime;
        this.minStartPlayer = minStartPlayer;
    }

    public Map(BlockPos mapPos1, BlockPos mapPos2, BlockPos startPos1, BlockPos startPos2, BlockPos winPos1, BlockPos winPos2, int deathHeight, int dayTime, int minStartPlayer) {
        this.mapRegion = new Region(mapPos1, mapPos2);
        this.startRegion = new Region(startPos1, startPos2);
        this.winRegion = new Region(winPos1, winPos2);
        this.deathHeight = deathHeight;
        this.dayTime = dayTime;
        this.minStartPlayer = minStartPlayer;
    }

    public Vec3 getAStartPos() {
        BlockPos[] bounds = startRegion.enclose();
        BlockPos min = bounds[0], max = bounds[1];
        Random random = new Random();
        return new Vec3(random.nextDouble(min.getX(), max.getX() + 1), min.getY() + 0.5, random.nextDouble(min.getZ(), max.getZ() + 1));
    }

    public void terminateGame(List<ServerPlayer> players) {
    }

    public boolean isWin(ServerPlayer player) {
        Vec3 position = player.position();
        return winRegion.inPos(position);
    }

    ;

    public boolean isLose(ServerPlayer player) {
        return player.position().y < deathHeight;
    }

    public void startGame(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            Vec3 pos = getAStartPos();
            player.teleportTo(pos.x, startRegion.getMin().getY() + 1.5, pos.z);
        }
    }

    public boolean canPlace(BlockPos pos) {
        if (!mapRegion.inPos(pos)) return false;
        if (startRegion.inPos(pos)) return false;
        if (winRegion.inPos(pos)) return false;
        return true;
    }

    public void tick() {
    }

    public void second() {
    }

    ;

    public void reset() {
    }

    ;

    public record Region(BlockPos pos1, BlockPos pos2) {
        public static Region get(int x1, int y1, int z1, int x2, int y2, int z2) {
            return new Region(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
        }

        public static boolean inPos(BlockPos test, Region region) {
            return region.inPos(test);
        }

        public BlockPos[] enclose() {
            int minX = Math.min(pos1.getX(), pos2.getX());
            int minY = Math.min(pos1.getY(), pos2.getY());
            int minZ = Math.min(pos1.getZ(), pos2.getZ());
            int maxX = Math.max(pos1.getX(), pos2.getX());
            int maxY = Math.max(pos1.getY(), pos2.getY());
            int maxZ = Math.max(pos1.getZ(), pos2.getZ());
            BlockPos min = new BlockPos(minX, minY, minZ);
            BlockPos max = new BlockPos(maxX, maxY, maxZ);
            return new BlockPos[]{min, max};
        }

        public BlockPos getMin() {
            return enclose()[0];
        }

        public BlockPos getMax() {
            return enclose()[1];
        }

        public boolean inPos(BlockPos test) {
            BlockPos[] bounds = enclose();
            BlockPos min = bounds[0], max = bounds[1];
            return min.getX() <= test.getX() && min.getY() <= test.getY() && min.getZ() <= test.getZ() &&
                    max.getX() >= test.getX() && max.getY() >= test.getY() && max.getZ() >= test.getZ();
        }

        public boolean inPos(Vec3 test) {
            BlockPos[] bounds = enclose();
            BlockPos min = bounds[0], max = bounds[1];
            return min.getX() <= test.x && min.getY() <= test.y && min.getZ() <= test.z &&
                    max.getX() + 1 >= test.x && max.getY() + 1 >= test.y && max.getZ() + 1 >= test.z;
        }
    }
}
