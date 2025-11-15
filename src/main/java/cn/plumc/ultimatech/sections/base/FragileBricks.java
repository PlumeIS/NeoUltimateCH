package cn.plumc.ultimatech.sections.base;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BlockSurfaceHit;
import cn.plumc.ultimatech.section.hit.Hit;
import cn.plumc.ultimatech.section.hit.PlayerHit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class FragileBricks extends Section {
    public List<UUID> standings = new ArrayList<>();
    public boolean fullBreak = false;

    public FragileBricks(ServerPlayer owner, SectionLocation location, Game game) {
        super(owner, location, game);
        setProcess(0);
    }

    @Override
    public void tickRun(int tickTime) {
        if (fullBreak) return;
        HashMap<BlockPos, BlockState> blocks = content.blocks;
        List<PlayerHit> playerHits = Hit.getPlayerHits(game);
        playerLoop: for (PlayerHit playerHit : playerHits) {
            for (BlockPos blockPos : blocks.keySet()) {
                BlockSurfaceHit blockSurfaceHit = new BlockSurfaceHit(blockPos);
                if (blockSurfaceHit.intersect(playerHit)) {
                    if (!standings.contains(playerHit.getPlayer().getUUID())) {
                        boolean result = tryBreak((double) 1 / 3);
                        if (result || !fullBreak) tryBreak((double) 1 / 6);
                        standings.add(playerHit.getPlayer().getUUID());
                    }
                    break playerLoop;
                }
            }
            standings.remove(playerHit.getPlayer().getUUID());
        }
    }

    private boolean tryBreak(double chance) {
        Random random = new Random();
        double result = random.nextDouble();
        if (!(result < (chance))) return false;

        HashMap<BlockPos, BlockState> bricks = content.blocks;
        BlockState state = bricks.values().stream().toList().get(0);
        BlockState newState;
        if (state.getBlock() == Blocks.STONE_BRICKS) newState = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        else {
            newState = Blocks.AIR.defaultBlockState();
            fullBreak = true;
        };
        HashMap<BlockPos, BlockState> newBricks = new HashMap<>();
        vfx();
        for (java.util.Map.Entry<BlockPos, BlockState> entry: bricks.entrySet()){
            newBricks.put(entry.getKey(), newState);
            level.setBlockAndUpdate(entry.getKey(), newState);
        }
        content.blocks = newBricks;
        return true;
    }

    private void vfx(){
        HashMap<BlockPos, BlockState> blocks = content.blocks;
        for (BlockPos blockPos : blocks.keySet()) {
            Vec3 pos = new Vec3(blockPos).add(0.5);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()), pos.x, pos.y, pos.z, 50, 0.8, 0.8, 0.8, 1);
            level.playSound(null, blockPos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS);
        }
    }
}
