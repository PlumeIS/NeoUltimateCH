package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.utils.CommandUtil;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class DevelopCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("develop")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            if (PlayerUtil.containsTag(player, StatusTags.DEVELOPER_TAG)) {
                                player.getTags().remove(StatusTags.DEVELOPER_TAG);
                                player.getTags().remove(StatusTags.NO_JUMP_TAG);
                                player.sendSystemMessage(Component.literal("DEVELOP: OFF"));
                            } else {
                                player.getTags().add(StatusTags.DEVELOPER_TAG);
                                player.getTags().add(StatusTags.NO_JUMP_TAG);
                                player.sendSystemMessage(Component.literal("DEVELOP: ON"));
                            }
                        }
                        return 1;
                    }
                ).then(Commands.literal("test")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        CommandSourceStack source = context.getSource();
                        List<String> commands = List.of("/game create roof",
                                                        "/game join @a",
                                                        "/section tick",
                                                        "/section add ultimatech.beam4",
                                                        "/section view 0",
                                                        "/hub map");
                        CommandUtil.runList(server, source, commands);
                        return 1;
                    })
                ).then(Commands.literal("test1")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        CommandSourceStack source = context.getSource();
                        List<String> commands = List.of("/game create roof",
                                "/game join @a",
                                "/game start");
                        CommandUtil.runList(server, source, commands);
                        return 1;
                    })
                ).then(Commands.literal("test2")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        CommandSourceStack source = context.getSource();
                        List<String> commands = List.of("/section add ultimatech.flippable_floor_stabbing",
                                "/section view 1",
                                "/section place 1");
                        CommandUtil.runList(server, source, commands);
                        return 1;
                    })
                ).then(Commands.literal("clear")
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            CommandSourceStack source = context.getSource();
                            if (source.getPlayer().position().distanceTo(UCHInfos.DEVELOPER_POINT) < 500) return 1;
                            List<String> commands = List.of("/kill @e[type=item_display, distance=..200]",
                                    "/kill @e[type=block_display, distance=..200]",
                                    "/kill @e[type=text_display, distance=..200]",
                                    "/fill 114499 3 19794 114541 29 19833 air replace minecraft:barrier",
                                    "/fill 114499 3 19794 114541 29 19833 air replace minecraft:structure_void",
                                    "/fill 114497 19 19794 114532 30 19830 air");
                            CommandUtil.runList(server, source, commands);
                            return 1;
                        })
                ).then(Commands.literal("clear_map")
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            ServerLevel level = server.overworld();
                            BlockPos pos1 = new BlockPos(1809, -55, 1666);
                            BlockPos pos2 = new BlockPos(2209, -55, 2166);
                            BlockState air = Blocks.AIR.defaultBlockState();
                            long counter = 0;
                            long count = (long) (pos2.getX() - pos1.getX()) *(pos2.getZ()-pos1.getZ());
                            ArrayList<BlockPos> blockPos = new ArrayList<>();
                            for (int x = pos1.getX(); x <= pos2.getX(); x++) {
                                for (int z = pos1.getZ(); z <= pos2.getZ(); z++) {
                                    int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                                    for (int y = -55; y <= height; y++) {
                                        BlockPos pos = new BlockPos(x, y, z);
                                        BlockState state = level.getBlockState(pos);
                                        if (state.is(Blocks.BARRIER)||
                                        state.is(Blocks.STRUCTURE_VOID)||
                                        state.is(Blocks.SAND)||
                                        state.is(Blocks.GRAVEL)||
                                        state.is(BlockTags.LOGS)||
                                        state.is(BlockTags.LEAVES)) continue;
                                        if (!isOnAir(level, pos)) {blockPos.add(pos);}
                                    }
                                    counter++;
                                }
                                System.out.println("Counter: " + counter + "Max: " + count);
                            }
                            counter = 0;
                            long max = blockPos.size();
                            for (BlockPos pos : blockPos) {
                                level.setBlockAndUpdate(pos, air);
                                if ((counter%1000L)==0)System.out.println("Counter: " + counter + "Max: " + max);
                                counter++;
                            }
                            return 1;
                        })
                )
        );
    }

    private static boolean isOnAir(ServerLevel level, BlockPos pos){
        List<BlockPos> blocks = List.of(pos.above(), pos.below(), pos.north(), pos.east(), pos.south(), pos.west());
        for (BlockPos block : blocks) {
            BlockState state = level.getBlockState(block);
            if ((state.isAir()&&!state.is(Blocks.CAVE_AIR))||
                    !state.isCollisionShapeFullBlock(level, pos)||
            state.is(Blocks.BARRIER)||
            state.is(Blocks.STRUCTURE_VOID)||
            state.is(Blocks.SAND)||
            state.is(Blocks.GRAVEL)||
            state.is(BlockTags.LOGS)||
            state.is(BlockTags.LEAVES)) return true;
        }
        return false;
    }

}
