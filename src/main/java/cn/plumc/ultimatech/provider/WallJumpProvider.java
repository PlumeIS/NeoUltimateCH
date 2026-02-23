package cn.plumc.ultimatech.provider;

import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.utils.IntCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cn.plumc.ultimatech.info.StatusTags.*;

public class WallJumpProvider {
    public static final Map<ServerPlayer, Double> playerLastY = new ConcurrentHashMap<>();

    public static final Map<ServerPlayer, IntCounter> shiftJumpingPlayers = new ConcurrentHashMap<>();

    public static final Map<ServerPlayer, Integer> shulkerJumpingPlayers = new ConcurrentHashMap<>();
    public static final Map<ServerPlayer, IntCounter> shulkerJumpTimer = new ConcurrentHashMap<>();

    public static final Map<ServerPlayer, Optional<BlockPos>> fakeBlockJumpingPlayers = new ConcurrentHashMap<>();
    public static final Map<ServerPlayer, IntCounter> fakeBlockJumpTimer = new ConcurrentHashMap<>();

    public static void speedFix(ServerPlayer player) {
        player.setOnGround(true);
        Vec3 movement = player.getDeltaMovement();

        Double lastY = playerLastY.get(player);
        if (lastY != null && (lastY - player.getY()) > 0.5) {
            player.setDeltaMovement(movement.x, 0.1, movement.z);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

    }

    public static void shiftJump(ServerPlayer player, ServerLevel level) {
        BlockPos pos = player.blockPosition();
        Vec3 vec = player.position();

        shiftJumpingPlayers.forEach((p, counter) -> counter.add());

        if (touchingWall(level, pos, vec) || touchingWall(level, pos.above(), vec)) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, -1, 10, false, false));
            if (!shiftJumpingPlayers.containsKey(player)) {
                if (player.isShiftKeyDown()) {
                    player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, -1, 20, false, false));
                    shiftJumpingPlayers.put(player, new IntCounter(0));
                }
            } else {
                IntCounter counter = shiftJumpingPlayers.get(player);
                if (counter != null && counter.get() > 6) {
                    player.removeEffect(MobEffects.LEVITATION);
                }
                if (!player.isShiftKeyDown() && counter != null && counter.get() > 6) {
                    shiftJumpingPlayers.remove(player);
                }
            }
        } else {
            player.removeEffect(MobEffects.LEVITATION);
            player.removeEffect(MobEffects.SLOW_FALLING);
            shiftJumpingPlayers.remove(player);
        }
    }

    public static void shulkerJump(ServerPlayer player, ServerLevel level) {
        Vec3 vec = player.position();
        BlockPos pos = player.blockPosition();
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;

        if (touchingWall(level, pos, vec) || touchingWall(level, pos.above(), vec)) {
            speedFix(player);
            if (!shulkerJumpingPlayers.containsKey(player)) {
                int pid = getEntityId();
                UUID uuid = UUID.randomUUID();

                player.connection.send(new ClientboundAddEntityPacket(pid, uuid, x, y - 1.5, z, 0, 0, EntityType.SHULKER, 0, Vec3.ZERO, 0));
                player.connection.send(new ClientboundUpdateMobEffectPacket(pid, new MobEffectInstance(MobEffects.INVISIBILITY, -1, 1, false, false), true));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 1, false, false));
                shulkerJumpingPlayers.put(player, pid);
            } else {
                Integer pid = shulkerJumpingPlayers.get(player);
                UUID uuid = UUID.randomUUID();
                double deltaMovement = 0;
                IntCounter counter = shulkerJumpTimer.get(player);
                if (counter != null && counter.get() >= 6) {
                    deltaMovement = -0.8;
                }
                player.connection.send(new ClientboundRemoveEntitiesPacket(pid));
                player.connection.send(new ClientboundAddEntityPacket(pid, uuid, x, y - 1.5 + deltaMovement, z, 0, 0, EntityType.SHULKER, 0, Vec3.ZERO, 0));

                if (Mth.equal(y - Math.floor(y), 0.0D)) {
                    shulkerJumpTimer.compute(player, (p, c) -> {
                        if (c == null) return new IntCounter(0);
                        c.add();
                        return c;
                    });
                } else {
                    shulkerJumpTimer.remove(player);
                }
            }
        } else {
            if (shulkerJumpingPlayers.containsKey(player)) {
                player.removeEffect(MobEffects.JUMP);
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 0, false, false));
                Integer pid = shulkerJumpingPlayers.get(player);
                player.connection.send(new ClientboundRemoveEntitiesPacket(pid));
                shulkerJumpingPlayers.remove(player);
            }
        }
    }

    public static void fakeBlockJump(ServerPlayer player, ServerLevel level) {
        Vec3 vec = player.position();
        BlockPos pos = player.blockPosition();
        double y = vec.y;

        if (touchingWall(level, pos, vec) || touchingWall(level, pos.above(), vec)) {
            speedFix(player);
            if (!fakeBlockJumpingPlayers.containsKey(player)) {
                if (level.getBlockState(pos.below()).is(Blocks.AIR)) {
                    player.connection.send(new ClientboundBlockUpdatePacket(pos.below(), UCHInfos.JUMPING_BLOCK));
                    fakeBlockJumpingPlayers.put(player, Optional.of(pos.below()));
                } else fakeBlockJumpingPlayers.put(player, Optional.empty());
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 1, false, false));
            } else {
                double deltaMovement = 0;
                IntCounter counter = fakeBlockJumpTimer.get(player);
                Optional<BlockPos> changed = fakeBlockJumpingPlayers.get(player);
                if (counter != null && counter.get() >= 6) {
                    deltaMovement = -0.8;
                }
                int blockY = Mth.floor(y - 1 + deltaMovement);
                BlockPos newPos = new BlockPos(pos.getX(), blockY, pos.getZ());
                changed.ifPresent(p -> player.connection.send(new ClientboundBlockUpdatePacket(p, UCHInfos.AIR)));
                if (level.getBlockState(newPos).is(Blocks.AIR)) {
                    player.connection.send(new ClientboundBlockUpdatePacket(newPos, UCHInfos.JUMPING_BLOCK));
                    fakeBlockJumpingPlayers.put(player, Optional.of(newPos));
                } else fakeBlockJumpingPlayers.put(player, Optional.empty());

                if (Mth.equal(y - Math.floor(y), 0.0D)) {
                    fakeBlockJumpTimer.compute(player, (p, c) -> {
                        if (c == null) return new IntCounter(0);
                        c.add();
                        return c;
                    });
                } else {
                    fakeBlockJumpTimer.remove(player);
                }
            }
        } else {
            if (fakeBlockJumpingPlayers.containsKey(player)) {
                player.removeEffect(MobEffects.JUMP);
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 0, false, false));
                if (Objects.nonNull(fakeBlockJumpingPlayers.get(player))){
                    Optional<BlockPos> changed = fakeBlockJumpingPlayers.get(player);
                    changed.ifPresent(p -> player.connection.send(new ClientboundBlockUpdatePacket(p, UCHInfos.AIR)));
                }
                fakeBlockJumpingPlayers.remove(player);
            }
        }
    }

    public static void tick(List<ServerPlayer> players, ServerLevel level) {
        for (ServerPlayer player : players) {
            if (player.getTags().contains(NO_JUMP_TAG)) continue;

            if (player.getTags().contains(SHIFT_JUMP_TAG)) {
                shiftJump(player, level);
            } else if (player.getTags().contains(SHULKER_JUMP_TAG)) {
                shulkerJump(player, level);
            } else {
                fakeBlockJump(player, level);
            }
            playerLastY.put(player, player.getY());
        }
    }

    public static boolean isTouchingPlayer(ServerPlayer player) {
        return shulkerJumpingPlayers.containsKey(player)||fakeBlockJumpingPlayers.containsKey(player);
    }

    private static boolean touchingWall(ServerLevel level, BlockPos pos, Vec3 vec) {
        if (level.getBlockState(pos).is(Blocks.STRUCTURE_VOID) ||
                level.getBlockState(pos.below()).is(Blocks.STRUCTURE_VOID)) return false;

        for (int x = -1; x <= 1; x++) {
            int bx = pos.getX() + x;
            double minX = bx - 0.4;
            double maxX = bx + 1.4;
            if (vec.x < minX || vec.x > maxX) continue;
            for (int z = -1; z <= 1; z++) {
                int bz = pos.getZ() + z;
                double minZ = bz - 0.4;
                double maxZ = bz + 1.4;
                if (vec.z < minZ || vec.z > maxZ) continue;
                BlockPos testing = new BlockPos(bx, pos.getY(), bz);
                BlockState state = level.getBlockState(testing);
                if (!state.isAir() && state.isCollisionShapeFullBlock(level, testing)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int getEntityId() {
        return UCHInfos.WALL_JUMPING_USING_ID;
    }
}