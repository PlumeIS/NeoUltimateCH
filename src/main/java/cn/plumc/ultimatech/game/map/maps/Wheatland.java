package cn.plumc.ultimatech.game.map.maps;

import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.section.MotionTransform;
import cn.plumc.ultimatech.section.SectionRotation;
import cn.plumc.ultimatech.utils.IntCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Objects;

public class Wheatland extends Map {
    public ServerLevel level;
    public Entity machine;
    public IntCounter tickCounter = new IntCounter();
    public HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
    public MotionTransform transform = new MotionTransform(null);

    public Wheatland() {
        super(
                new Map.Region(
                        new BlockPos(3985, 3, 4024),
                        new BlockPos(4028, 29, 3968)),
                new Map.Region(
                        new BlockPos(4005, 13, 3983),
                        new BlockPos(4007, 15, 3985)),
                new Map.Region(
                        new BlockPos(4005, 19, 4015),
                        new BlockPos(4007, 21, 4017)),
                Integer.MIN_VALUE,
                6000,
                2);
    }

    @Override
    public void tick() {
        tickCounter.add();
        if (Objects.isNull(level)) level = ServerLifecycleHooks.getCurrentServer().overworld();
        if (Objects.isNull(machine)) {
            level.getEntities().get(new AABB(4005, 5, 4007, 4008, 8.00, 4011), (entity) -> {
                if (entity.getTags().contains("uch.map.wheatland.machine")) machine = entity;
            });
        }

        if (tickCounter.get() % 5 == 0) {
            rotations.put(SectionRotation.Axis.Z, rotations.get(SectionRotation.Axis.Z) + 60);
            transform.applyEntityRotation(machine, rotations, 0.25);
        }
    }

    @Override
    public boolean isLose(ServerPlayer player) {
        return super.isLose(player) ||
                new Region(new BlockPos(4005, 5, 4007), new BlockPos(4007, 7, 4010)).inPos(player.position());
    }
}
