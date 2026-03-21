package cn.plumc.ultimatech.game.map.maps;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.section.MotionTransform;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.section.layer.Layer;
import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.IntCounter;
import cn.plumc.ultimatech.utils.PlayerUtil;
import cn.plumc.ultimatech.utils.TickUtil;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

public class OldMansion extends Map {
    public static final String BOTTOM_PLATE_TAG = "uch.map.elevator.trigger";
    public static final String ELEVATOR_BLOCK_TAG = "uch.map.elevator.block";

    public BoxHit triggerBottom = new BoxHit(new Vec3(209.00, 14.00, 809.00), new Vec3(211.00, 14.20, 811.00));
//    public BoxHit triggerTop = new BoxHit(new Vec3())

    public BoxHit elevatorBottom = new BoxHit(new Vec3(204.5, 29.50, 809.5), new Vec3(205.5, 29.5, 810.5));
    public BoxHit elevatorTop = new BoxHit(new Vec3(204.5, 33.50, 809.5), new Vec3(205.5, 33.5, 810.5));
    public List<BoxHit> upping = List.of(
            new BoxHit(new Vec3(204.00, 30.00, 809.00), new Vec3(206.0, 30.2, 811.00)),
            new BoxHit(new Vec3(204.00, 34.00, 809.00), new Vec3(206.0, 34.2, 811.00))
    );
    public double elevatorSpeed = 1.6;
    public double elevatorOffset = -0.4;

    public HashSet<BlockPos> changed = Sets.newHashSet();
    public Entity plateBottom;
    public HashSet<Entity> elevatorBlocks = new HashSet<>();

    public boolean running = false;
    public boolean pressed = false;
    public IntCounter plateTimer = new IntCounter();
    public IntCounter elevatorTimer = new IntCounter();


    public OldMansion() {
        super(
                new Map.Region(
                        new BlockPos(183, 9, 794),
                        new BlockPos(228, 44, 825)),
                new Map.Region(
                        new BlockPos(187, 10, 808),
                        new BlockPos(190, 12, 811)),
                new Map.Region(
                        new BlockPos(218, 27, 808),
                        new BlockPos(220, 29, 811)),
                -10,
                800,
                2
        );
    }

    @Override
    public void startGame(Game game, List<ServerPlayer> players) {
        super.startGame(game, players);
        ServerLevel level = game.getLevel();
        TickUtil.tickRun(()-> {
                    elevatorBlocks.clear();
                    level.getEntities().get(mapRegion.getAABB(), entity -> {
                        if (entity instanceof Display) {
                            if (entity.getTags().contains(BOTTOM_PLATE_TAG)) plateBottom = entity;
                            if (entity.getTags().contains(ELEVATOR_BLOCK_TAG)) elevatorBlocks.add(entity);
                        }
                    });
                }
        );
    }

    private void loadEntities(){
        elevatorBlocks.clear();
        ServerLevel level = currentGame.getLevel();
        level.getEntities().get(mapRegion.getAABB(), entity -> {
            if (entity instanceof Display) {
                if (entity.getTags().contains(BOTTOM_PLATE_TAG)) plateBottom = entity;
                if (entity.getTags().contains(ELEVATOR_BLOCK_TAG)) elevatorBlocks.add(entity);
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (triggerBottom.hasPlayer(currentGame)) {
            if (Objects.isNull(plateBottom)||elevatorBlocks.isEmpty()) loadEntities();
            if (!pressed) {
                pressed = true;
                plateTimer.set(10);
                Vec3 pressVec = new Vec3(0, -0.1, 0);
                MotionTransform.updateEntityTransformation(plateBottom, t -> t.put("translation", MotionTransform.toFloatListTag(pressVec)), 0.15);
                if (!running) {
                    running = true;
                    elevatorTimer.set(100 + Mth.floor(16.0/elevatorSpeed*20*2));
                    Vec3 target = new Vec3(0, -16, 0);
                    elevatorBlocks.forEach(entity -> MotionTransform.updateEntityTransformation(entity, t -> t.put("translation", MotionTransform.toFloatListTag(target)), 16.0/elevatorSpeed));
                }
            }
        } else {
            plateTimer.add(-1);
        }

        if (running) elevatorTimer.add(-1);

        if (running && elevatorTimer.get() >= 100 + Mth.floor(16.0/elevatorSpeed*20)) {
            elevatorOffset -= (elevatorSpeed/20.0);
        }
        if (running && elevatorTimer.get() <= Mth.floor(16.0/elevatorSpeed*20)) {
            elevatorOffset += (elevatorSpeed/20.0);
        }

        if (elevatorTimer.get() == Mth.floor(16.0/elevatorSpeed*20)) {
            elevatorBlocks.forEach(entity -> MotionTransform.updateEntityTransformation(entity, t -> t.put("translation", MotionTransform.toFloatListTag(Vec3.ZERO)), 16.0/elevatorSpeed));
        }
        if (plateTimer.get() == 5) {
            MotionTransform.updateEntityTransformation(plateBottom, t -> t.put("translation", MotionTransform.toFloatListTag(Vec3.ZERO)), 0.15);
        }

        if (plateTimer.get() == 0) {
            pressed = false;
        }
        if (elevatorTimer.get() == 0) {
            elevatorOffset = -0.4;
            if (running) {
                BoxHit bottom = upping.getFirst();
                for (ServerPlayer player : bottom.detectPlayers(currentGame)) {
                    Vec3 position = player.position().add(0, bottom.pos1.y - player.position().y + 0.01, 0);
                    PlayerUtil.teleport(player, position);
                }
            }
            running = false;

        }

        Layer layer = currentGame.getSectionManager().getMiddleLayer();
        double bottomOffset = elevatorOffset;
        double upOffset = elevatorOffset;
        if (running && elevatorTimer.get() <= Mth.floor(16.0/elevatorSpeed*20)) bottomOffset -= 0.4;
        if (running && elevatorTimer.get() <= Mth.floor(16.0/elevatorSpeed*20)) upOffset += 0.6;
        Vec3 topBlocks1 = elevatorTop.pos1.add(0, upOffset, 0);
        Vec3 topBlocks2 = elevatorTop.pos2.add(0, upOffset, 0);
        Vec3 bottomBlocks1 = elevatorBottom.pos1.add(0, bottomOffset, 0);
        Vec3 bottomBlocks2 = elevatorBottom.pos2.add(0, bottomOffset, 0);
        for (BlockPos changed: changed) {
            layer.remove(changed);
        }
        changed.clear();
        for (BlockPos pos: BlockUtil.getBlocksFromAABB(new AABB(bottomBlocks1, bottomBlocks2))){
            if (layer.has(pos)) continue;
            layer.set(pos, Blocks.BARRIER.defaultBlockState());
            changed.add(pos);
        }
        for (BlockPos pos: BlockUtil.getBlocksFromAABB(new AABB(topBlocks1, topBlocks2))){
            if (layer.has(pos)) continue;
            layer.set(pos, Blocks.BARRIER.defaultBlockState());
            changed.add(pos);
        }
        if (running && elevatorTimer.get() <= Mth.floor(16.0/elevatorSpeed*20)) {
            for (BoxHit hit : upping) {
                Vec3 pos1 = hit.pos1.add(0, elevatorOffset, 0);
                Vec3 pos2 = hit.pos2.add(0, elevatorOffset, 0);
                BoxHit boxHit = new BoxHit(pos1, pos2);
                for (ServerPlayer player : boxHit.detectPlayers(currentGame)) {
                    Vec3 movement = player.getDeltaMovement();
                    double dy = elevatorSpeed / 20.0;
                    double add = 0;
                    if (movement.y < dy) {
                        add = dy - movement.y;
                    }
                    Vec3 position = player.position().add(0, pos2.y - player.position().y + dy, 0);
                    if (player.position().y < pos1.y-0.01) PlayerUtil.teleport(player, position);
                    player.setDeltaMovement(movement.add(0, add, 0));
                    PlayerUtil.updateDeltaMovement(currentGame.getPlayerManager().getPlayers(),  player);
                }
            }
        }


    }

    @Override
    public void reset() {
        Layer layer = currentGame.getSectionManager().getMiddleLayer();
        for (BlockPos changed: changed) {
            layer.remove(changed);
        }
        changed.clear();
    }

    @Override
    public boolean isLose(ServerPlayer player) {
        return super.isLose(player);
    }
}
