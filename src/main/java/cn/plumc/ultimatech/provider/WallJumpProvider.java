package cn.plumc.ultimatech.provider;

import cn.plumc.ultimatech.utils.IntCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static cn.plumc.ultimatech.info.StatusTags.*;

public class WallJumpProvider {
    public static final Map<ServerPlayer, Integer> entityJumpPlayers = new ConcurrentHashMap<>();
    public static final Map<ServerPlayer, IntCounter> shulkerJumpPlayers = new ConcurrentHashMap<>();
    public static final Map<ServerPlayer, IntCounter> shiftJumpingPlayers = new ConcurrentHashMap<>();
    public static final Map<ServerPlayer, Double> playerLastY = new ConcurrentHashMap<>();
    public static boolean skipTick = false;

    // boatJump kept commented out as in original
    /*
    public static void boatJump(ServerPlayer player, ServerLevel world) { ... }
    */

    public static void shiftJump(ServerPlayer player, ServerLevel world) {
        final double x = player.getX();
        final double y0 = player.getY();
        final double z = player.getZ();
        final double y1 = y0 + 1;

        // increment all shift counters
        shiftJumpingPlayers.forEach((p, counter) -> counter.add());

        if (touchBlock(world, x, y0, z) || touchBlock(world, x, y1, z)) {
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

    public static void shulkerJump(ServerPlayer player, ServerLevel world) {
        final double x = player.getX();
        final double y0 = player.getY();
        final double z = player.getZ();
        final double y1 = y0 + 1;

        if (touchBlock(world, x, y0, z) || touchBlock(world, x, y1, z)) {
            // avoid structure void surface (keeps original behavior)
            if (world.getBlockState(new BlockPos((int) x, (int) y0, (int) z)).is(Blocks.STRUCTURE_VOID)) return;

            player.setOnGround(true);
            Vec3 movement = player.getDeltaMovement();

            // if last Y exists and player dropped significantly since last tick, nudge vertical motion
            Double lastY = playerLastY.get(player);
            if (lastY != null && (lastY - player.getY()) > 0.5) {
                player.setDeltaMovement(movement.x, 0.1, movement.z);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }

            if (!entityJumpPlayers.containsKey(player)) {
                int pid = generateUniqueEntityId();
                UUID uuid = UUID.randomUUID();

                player.connection.send(new ClientboundAddEntityPacket(pid, uuid, x, y0 - 1.5, z, 0, 0, EntityType.SHULKER, 0, Vec3.ZERO, 0));
                player.connection.send(new ClientboundUpdateMobEffectPacket(pid, new MobEffectInstance(MobEffects.INVISIBILITY, -1, 1, false, false), true));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 1, false, false));
                entityJumpPlayers.put(player, pid);
            } else {
                Integer pid = entityJumpPlayers.get(player);
                UUID uuid = UUID.randomUUID();
                int offset = 0;
                IntCounter counter = shulkerJumpPlayers.get(player);
                if (counter != null && counter.get() >= 6) {
                    offset = -1;
                }
                if (counter != null && counter.get() > 8) {
                    shulkerJumpPlayers.remove(player);
                }

                // alternate between pid+1 and pid-1 as original
                if (pid % 2 == 0) {
                    player.connection.send(new ClientboundAddEntityPacket(pid + 1, uuid, x, y0 - 1.5 + offset, z, 0, 0, EntityType.SHULKER, 0, Vec3.ZERO, 0));
                    player.connection.send(new ClientboundRemoveEntitiesPacket(pid));
                    player.connection.send(new ClientboundUpdateMobEffectPacket(pid + 1, new MobEffectInstance(MobEffects.INVISIBILITY, -1, 1, false, false), true));
                    entityJumpPlayers.put(player, pid + 1);
                } else {
                    player.connection.send(new ClientboundAddEntityPacket(pid - 1, uuid, x, y0 - 1.5 + offset, z, 0, 0, EntityType.SHULKER, 0, Vec3.ZERO, 0));
                    player.connection.send(new ClientboundRemoveEntitiesPacket(pid));
                    player.connection.send(new ClientboundUpdateMobEffectPacket(pid - 1, new MobEffectInstance(MobEffects.INVISIBILITY, -1, 1, false, false), true));
                    entityJumpPlayers.put(player, pid - 1);
                }

                // handle shulkerJumpPlayers counters when player exactly on integer Y
                if (Mth.equal(y0 - Math.floor(y0), 0.0D)) {
                    shulkerJumpPlayers.compute(player, (p, c) -> {
                        if (c == null) return new IntCounter(0);
                        c.add();
                        return c;
                    });
                } else {
                    shulkerJumpPlayers.remove(player);
                }
            }
        } else {
            if (entityJumpPlayers.containsKey(player)) {
                player.removeEffect(MobEffects.JUMP);
                player.removeEffect(MobEffects.SLOW_FALLING);
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 0, false, false));
                Integer pid = entityJumpPlayers.get(player);
                player.connection.send(new ClientboundRemoveEntitiesPacket(pid - 2, pid - 1, pid, pid + 1, pid + 2));
                entityJumpPlayers.remove(player);
            }
        }
    }

    public static void tick(List<ServerPlayer> players, ServerLevel world) {
        for (ServerPlayer player : players) {
            if (player.getTags().contains(NO_JUMP_TAG)) continue;

            if (player.getTags().contains(BOAT_JUMP_TAG)) {
                // boatJump commented as original
                // boatJump(player, world);
            } else if (player.getTags().contains(SHIFT_JUMP_TAG)) {
                shiftJump(player, world);
            } else {
                shulkerJump(player, world);
            }
            playerLastY.put(player, player.getY());
        }
    }

    public static boolean isTouchingPlayer(ServerPlayer player) {
        return entityJumpPlayers.containsKey(player);
    }

    /**
     * Checks a 3x3 area at given floor coordinates to determine if any full collision block is
     * within ~0.4 margin of the exact x/z position. Mirrors original method semantics.
     */
    private static boolean touchBlock(ServerLevel world, double ix, double iy, double iz) {
        final int blockX = (int) Math.floor(ix);
        final int blockY = (int) Math.floor(iy);
        final int blockZ = (int) Math.floor(iz);

        for (int x = -1; x <= 1; x++) {
            int bx = blockX + x;
            double minX = bx - 0.4;
            double maxX = bx + 1.4;
            if (ix < minX || ix > maxX) continue; // small early skip if x outside possible range
            for (int z = -1; z <= 1; z++) {
                int bz = blockZ + z;
                double minZ = bz - 0.4;
                double maxZ = bz + 1.4;
                if (iz < minZ || iz > maxZ) continue; // early skip if z outside
                BlockPos pos = new BlockPos(bx, blockY, bz);
                BlockState state = world.getBlockState(pos);
                if (!state.isAir() && state.isCollisionShapeFullBlock(world, pos)) {
                    // both x and z already checked for bounds
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Generate an entity ID similar to original logic: pick an int in [i, i+1000)
     * that is not currently used by entityJumpPlayers' values or +/-1/2 neighbors.
     * Keeps the original uniqueness check behavior.
     */
    private static int generateUniqueEntityId() {
        final int i = net.minecraft.world.entity.Entity.ENTITY_COUNTER.get() * 2;
        // Keep original range: i .. i+999
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int end = i + 1000;
        int attempts = 0;
        while (true) {
            int pid = rnd.nextInt(i, end);
            // ensure pid and its neighbors are not present as values
            boolean conflict = entityJumpPlayers.containsValue(pid)
                    || entityJumpPlayers.containsValue(pid + 1)
                    || entityJumpPlayers.containsValue(pid - 1)
                    || entityJumpPlayers.containsValue(pid + 2)
                    || entityJumpPlayers.containsValue(pid - 2);
            if (!conflict) return pid;
            // fallback to sequential search after many attempts
            if (++attempts > 2000) {
                for (int cand = i; cand < end; cand++) {
                    conflict = entityJumpPlayers.containsValue(cand)
                            || entityJumpPlayers.containsValue(cand + 1)
                            || entityJumpPlayers.containsValue(cand - 1)
                            || entityJumpPlayers.containsValue(cand + 2)
                            || entityJumpPlayers.containsValue(cand - 2);
                    if (!conflict) return cand;
                }
            }
        }
    }
}