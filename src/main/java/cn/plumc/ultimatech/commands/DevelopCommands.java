package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.SectionManager;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionRegistry;
import cn.plumc.ultimatech.section.SectionRotation;
import cn.plumc.ultimatech.section.SectionSerialization;
import cn.plumc.ultimatech.utils.CommandUtil;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

import static cn.plumc.ultimatech.info.UCHInfos.*;
import static cn.plumc.ultimatech.info.UCHInfos.SECTION_ROTATE_Z_ITEM;

public class DevelopCommands {
    private static boolean recording = false;
    private static String mapId = null;
    private static final List<ServerPlayer> recordingPlayers = new ArrayList<>();
    private static final List<Section> recordingSections = new ArrayList<>();
    private static final HashMap<UUID, String> selections  = new HashMap<>();
    private static final HashMap<UUID, Section> placings =  new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        registerDevelop(dispatcher);
        registerFlySpeed(dispatcher);
    }

    public static void registerDevelop(CommandDispatcher<CommandSourceStack> dispatcher) {
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
                    ).then(Commands.literal("map")
                        .then(Commands.literal("test")
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
                        ).then(Commands.literal("build_hay")
                            .executes(context -> {
                                    MinecraftServer server = context.getSource().getServer();
                                    ServerLevel level = server.overworld();
                                    BlockPos pos1 = new BlockPos(3889, 0, 3877);
                                    BlockPos pos2 = new BlockPos(4139, 0, 4109);
                                    int height = 20;
                                    BlockState dirt = Blocks.DIRT.defaultBlockState();
                                    BlockState wheat = Blocks.WHEAT.defaultBlockState();
                                    wheat.setValue(BlockStateProperties.AGE_7, 7);
                                    BlockState farmland = Blocks.FARMLAND.defaultBlockState();
                                    Random rand = new Random();
                                    for (int x = pos1.getX(); x <= pos2.getX(); x++) {
                                        for (int z = pos1.getZ(); z <= pos2.getZ(); z++) {
                                            for (int y = 0; y <= height; y++) {
                                                BlockPos pos = new BlockPos(x, y, z);
                                                BlockState state = level.getBlockState(pos.above());
                                                if ((state.isAir()||state.is(Blocks.WHEAT))) {
                                                    if ((rand.nextDouble() < 0.01)) break;
                                                    level.setBlockAndUpdate(pos, farmland);
                                                    level.setBlockAndUpdate(pos.above(), wheat);
                                                    break;
                                                } else {
                                                    level.setBlockAndUpdate(pos, dirt);
                                                }
                                            }
                                        }
                                    }
                                    return 1;
                                })
                        ).then(Commands.literal("grass_handle")
                            .executes(context -> {
                                        MinecraftServer server = context.getSource().getServer();
                                        ServerLevel level = server.overworld();
                                        BlockPos pos1 = new BlockPos(69, 10, 680);
                                        BlockPos pos2 = new BlockPos(340, 10, 942);
                                        int y = 11;
                                        BlockState tallGrass = Blocks.TALL_GRASS.defaultBlockState();
                                        tallGrass.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                                        for (int x = pos1.getX(); x <= pos2.getX(); x++) {
                                            for (int z = pos1.getZ(); z <= pos2.getZ(); z++) {
                                                if (!level.getBlockState(new BlockPos(x, y-1, z)).is(Blocks.TALL_GRASS)) {continue;}
                                                level.setBlockAndUpdate(new BlockPos(x, y, z), tallGrass);
                                            }
                                        }
                                        return 1;
                                    })
                        )
                    ).then(Commands.literal("start_record")
                                .then(Commands.argument("map", StringArgumentType.string())
                                        .executes((context) -> {
                                            mapId = context.getArgument("map", String.class);
                                            recording = true;
                                            recordingPlayers.add(context.getSource().getPlayer());
                                            context.getSource().sendSuccess(()->Component.literal("为你开启记录。"), false);
                                            return 1;
                                        })
                                )
                    ).then(Commands.literal("stop_record")
                            .executes((context) -> {
                                recordingPlayers.remove(context.getSource().getPlayer());
                                Section section;
                                if (Objects.nonNull(section = placings.get(context.getSource().getPlayer().getUUID()))) {
                                    if (!section.place()) {
                                        context.getSource().sendFailure(Component.literal("请确保当前Section能被放置后重试"));
                                        return 0;
                                    }
                                    section.remove();
                                }
                                context.getSource().sendSuccess(()->Component.literal("为你停止记录。"), false);
                                return 1;
                            })
                    ).then(Commands.literal("end_record")
                            .then(Commands.argument("map", StringArgumentType.string())
                                    .executes(DevelopCommands::endRecord)
                            )
                    ).then(Commands.literal("undo")
                                .executes(context -> {
                                    recordingSections.getLast().remove();
                                    recordingSections.removeLast();
                                    context.getSource().sendSuccess(()->Component.literal("已撤回"), false);
                                    return 1;
                                })
                    ).then(Commands.literal("select")
                                .then(Commands.argument("selection", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            try {
                                                String string = StringArgumentType.getString(context, "section");
                                                SectionRegistry.instance.getSectionInfos().keySet().forEach(id -> {
                                                    if (id.contains(string)) builder.suggest(id);}
                                                );
                                            } catch (IllegalArgumentException ignored) {
                                                SectionRegistry.instance.getSectionInfos().keySet().forEach(builder::suggest);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes((context) -> {
                                            String id = StringArgumentType.getString(context, "selection");
                                            selections.put(context.getSource().getPlayer().getUUID(), id);
                                            Section section;
                                            if (Objects.nonNull(section = placings.get(context.getSource().getPlayer().getUUID()))) {
                                                if (!section.place()) {
                                                    context.getSource().sendFailure(Component.literal("请确保当前Section能被放置后重试"));
                                                    return 0;
                                                }
                                                section.remove();
                                            }
                                            context.getSource().sendSuccess(()->Component.literal("已选择: "+id), false);
                                            return 1;
                                        })
                                )
                        )
        );
    }

    private static int endRecord(CommandContext<CommandSourceStack> context) {
        String map = StringArgumentType.getString(context, "map");
        MapInfo mapInfo = MapInfo.getMap(map);
        try {
            SectionSerialization.save(mapInfo, recordingSections);
            recordingSections.clear();
            recordingPlayers.clear();
//            for (Section section : placings.values()) {
//                if (!section.place()) {
//                    context.getSource().sendFailure(Component.literal("请确保当前全部玩家Section能被放置后重试"));
//                    return 0;
//                }
//                section.remove();
//            }
            placings.clear();
            recording = false;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
        }
        return 1;
    }

    public static void registerFlySpeed(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("flyspeed")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0f))
                                .executes((context) -> {
                                    Float value = FloatArgumentType.getFloat(context, "value");
                                    ServerPlayer player = context.getSource().getPlayer();
                                    player.getAbilities().setFlyingSpeed(0.05F*value);
                                    player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
                                    context.getSource().sendSuccess(() -> Component.literal("飞行速度已设置。"),  false);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("reset")
                                .executes((context) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    player.getAbilities().setFlyingSpeed(0.05F);
                                    player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
                                    context.getSource().sendSuccess(() -> Component.literal("飞行速度已设置。"),  false);
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

    public static void tick() {
        if (recording) {
            Game game = Lobby.games.get(mapId);
            SectionManager sectionManager = game.getSectionManager();
            for (ServerPlayer player : recordingPlayers) {
                String id;
                if (Objects.nonNull(id = selections.get(player.getUUID()))) {
                    Section section;
                    if (Objects.nonNull(section = placings.get(player.getUUID()))) {
                        if (section.placed) {
                            recordingSections.add(section);
                            placings.remove(player.getUUID());
                            Section newSection = sectionManager.buildSection(id, player);
                            newSection.rotation.set(SectionRotation.Axis.X, section.rotation.getX());
                            newSection.rotation.set(SectionRotation.Axis.Y, section.rotation.getY());
                            newSection.rotation.set(SectionRotation.Axis.Z, section.rotation.getZ());
                            newSection.view();
                            placings.put(player.getUUID(), newSection);
                        }
                    } else {
                        section = sectionManager.buildSection(id, player);
                        section.view();
                        placings.put(player.getUUID(), section);
                    }
                }
            }
        }
    }

    public static void onPlayerUseItem(ServerPlayer player, ItemStack itemStack) {
        Section section;
        if (recording && Objects.nonNull(section = placings.get(player.getUUID()))) {
            if (ItemStack.isSameItem(itemStack, SECTION_PLACE_ITEM)) {
                section.place();
            } else if (ItemStack.isSameItem(itemStack, SECTION_ROTATE_X_ITEM)) {
                section.rotation.rotate(SectionRotation.Axis.X, SectionRotation.RotationHandle.CLOCKWISE);
            } else if (ItemStack.isSameItem(itemStack, SECTION_ROTATE_Y_ITEM)) {
                section.rotation.rotate(SectionRotation.Axis.Y, SectionRotation.RotationHandle.CLOCKWISE);
            } else if (ItemStack.isSameItem(itemStack, SECTION_ROTATE_Z_ITEM)) {
                section.rotation.rotate(SectionRotation.Axis.Z, SectionRotation.RotationHandle.CLOCKWISE);
            }
        }
    }
}
