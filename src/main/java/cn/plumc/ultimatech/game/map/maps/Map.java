package cn.plumc.ultimatech.game.map.maps;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

public class Map {
    public BlockPos mapPos1;
    public BlockPos mapPos2;
    public BlockPos startPos1;
    public BlockPos startPos2;
    public BlockPos winPos1;
    public BlockPos winPos2;
    public int deathHeight;
    public int dayTime;

    public Map(BlockPos mapPos1, BlockPos mapPos2, BlockPos startPos1, BlockPos startPos2, BlockPos winPos1, BlockPos winPos2, int deathHeight, int dayTime){
        this.mapPos1 = mapPos1;
        this.mapPos2 = mapPos2;
        this.startPos1 = startPos1;
        this.startPos2 = startPos2;
        this.winPos1 = winPos1;
        this.winPos2 = winPos2;
        this.deathHeight = deathHeight;
        this.dayTime = dayTime;
    }

    public Vec3 getAStartPos(){
        int x1 = Math.min(startPos1.getX(), startPos2.getX());
        int z1 = Math.min(startPos1.getZ(), startPos2.getZ());
        int x2 = Math.max(startPos1.getX(), startPos2.getX());
        int z2 = Math.max(startPos1.getZ(), startPos2.getZ());
        Random random = new Random();
        return new Vec3(random.nextDouble(x1, x2), startPos1.getY()+0.5, random.nextDouble(z1, z2));
    }

    public void terminateGame(List<ServerPlayer> players){};

    public boolean isWin(ServerPlayer player) {
        Vec3 position = player.position();
        return inPos(position, winPos1, winPos2);
    }

    public boolean isLose(ServerPlayer player) {
        return player.position().y < deathHeight;
    }

    public void startGame(List<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            Vec3 pos = getAStartPos();
            player.teleportTo(pos.x, startPos1.getY()+1.5, pos.z);
        }
    }

    public boolean canPlace(BlockPos pos) {
        if (!inPos(pos, mapPos1, mapPos2)) return false;
        if (inPos(pos, startPos1, startPos2)) return false;
        if (inPos(pos, winPos1, winPos2)) return false;
        return true;
    }

    public static boolean inPos(BlockPos testPos, BlockPos pos1, BlockPos pos2) {
        int x1 = Math.min(pos1.getX(), pos2.getX());
        int y1 = Math.min(pos1.getY(), pos2.getY());
        int z1 = Math.min(pos1.getZ(), pos2.getZ());
        int x2 = Math.max(pos1.getX(), pos2.getX());
        int y2 = Math.max(pos1.getY(), pos2.getY());
        int z2 = Math.max(pos1.getZ(), pos2.getZ());
        return x1 <= testPos.getX() && y1 <= testPos.getY() && z1 <= testPos.getZ() &&
                x2 >= testPos.getX() && y2 >= testPos.getY() && z2 >= testPos.getZ();
    }

    public static boolean inPos(Vec3 testPos, BlockPos pos1, BlockPos pos2) {
        int x1 = Math.min(pos1.getX(), pos2.getX());
        int y1 = Math.min(pos1.getY(), pos2.getY());
        int z1 = Math.min(pos1.getZ(), pos2.getZ());
        int x2 = Math.max(pos1.getX(), pos2.getX());
        int y2 = Math.max(pos1.getY(), pos2.getY());
        int z2 = Math.max(pos1.getZ(), pos2.getZ());
        return x1 <= testPos.x && y1 <= testPos.y && z1 <= testPos.z &&
                x2+1 >= testPos.x && y2+1 >= testPos.y && z2+1 >= testPos.z+1;
    }

    public void tick(){};
    public void second(){};
    public void render(){};
    public void reset(){}
}
