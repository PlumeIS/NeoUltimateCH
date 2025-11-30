package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionBox;
import cn.plumc.ultimatech.section.SectionRegistry;
import cn.plumc.ultimatech.section.SectionRotation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public class SectionCommand {
    public static SectionBox sectionBox;
    public static int tickId = -1;
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("section")
                .then(Commands.literal("add")
                        .then(Commands.argument("section", StringArgumentType.string())
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
                                .executes(SectionCommand::addSection)
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("section", IntegerArgumentType.integer())
                                .executes(SectionCommand::removeSection)
                        )
                )
                .then(Commands.literal("list")
                        .executes(SectionCommand::listSection)
                )
                .then(Commands.literal("view")
                        .then(Commands.argument("section", IntegerArgumentType.integer())
                                .executes(SectionCommand::viewSection)
                        )
                )
                .then(Commands.literal("place")
                        .then(Commands.argument("section", IntegerArgumentType.integer())
                                .executes(SectionCommand::placeSection)
                        )
                )
                .then(Commands.literal("rotate")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(SectionCommand::rotateSection)
                                        )
                                )
                        )
                        .then(Commands.literal("reset")
                                .executes(SectionCommand::rotateResetSection)
                        )
                )
                .then(Commands.literal("box")
                        .executes(commandContext -> {
                            try {
                                if (sectionBox==null) sectionBox = new SectionBox(1, 9);
                                commandContext.getSource().getPlayer().openMenu(sectionBox);
                                return 1;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return 0;
                        })
                )
        );
    }

    public static int addSection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            Game game = getGameByPlayer(ctx.getSource().getPlayer());
            String sectionId = StringArgumentType.getString(ctx, "section");
            Section section = game.getSectionManager().buildSection(sectionId, ctx.getSource().getPlayer());
            game.getStatus().roundSections.put(ctx.getSource().getPlayer(), section);
        } catch (Exception e){
            e.printStackTrace();
        }
        return 1;
    }

    public static int removeSection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            Game game = getGameByPlayer(ctx.getSource().getPlayer());
            int index = IntegerArgumentType.getInteger(ctx, "section");
            game.getSectionManager().getSections().get(index).remove();
            game.getSectionManager().removeSection(index);
        } catch (Exception e){
            e.printStackTrace();
        }
        return 1;
    }

    public static int listSection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Game game = getGameByPlayer(ctx.getSource().getPlayer());
        for (int i = 0; i < game.getSectionManager().getSections().size(); i++) {
            final int index = i;
            Section section = game.getSectionManager().getSections().get(i);
            ctx.getSource().sendSuccess(()->Component.literal(index+" : "+ SectionRegistry.instance.getSectionInfo(section.getClass())), false);
        }
        return 1;
    }

    public static int viewSection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            Game game = getGameByPlayer(ctx.getSource().getPlayer());
            int index = IntegerArgumentType.getInteger(ctx, "section");
            game.getSectionManager().getSections().get(index).view();
        } catch (Exception e){
                e.printStackTrace();
            }
        return 1;
    }

    public static int placeSection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        try {
            Game game = getGameByPlayer(ctx.getSource().getPlayer());
            int index = IntegerArgumentType.getInteger(ctx, "section");
            Section section = game.getSectionManager().getSections().get(index);
            section.place();
            if (Objects.nonNull(section.process))section.process.start();
        } catch (Exception e){
            e.printStackTrace();
        }
        return 1;
    }

    public static int rotateSection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Game game = getGameByPlayer(ctx.getSource().getPlayer());
        int index = IntegerArgumentType.getInteger(ctx, "section");
        Section section = game.getSectionManager().getSections().get(index);
        section.rotation.set(SectionRotation.Axis.X, IntegerArgumentType.getInteger(ctx, "x"));
        section.rotation.set(SectionRotation.Axis.Y, IntegerArgumentType.getInteger(ctx, "y"));
        section.rotation.set(SectionRotation.Axis.Z, IntegerArgumentType.getInteger(ctx, "z"));
        return 1;
    }


    public static int rotateResetSection(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Game game = getGameByPlayer(ctx.getSource().getPlayer());
        int index = IntegerArgumentType.getInteger(ctx, "section");
        Section section = game.getSectionManager().getSections().get(index);
        section.rotation.set(SectionRotation.Axis.X, 0);
        section.rotation.set(SectionRotation.Axis.Y, 0);
        section.rotation.set(SectionRotation.Axis.Z, 0);
        return 1;
    }
    
    public static Game getGameByPlayer(ServerPlayer player) {
        for (Game game : Lobby.games.values()) {
            if (game.getPlayerManager().getPlayers().contains(player)) {return game;}
        }
        return null;
    }
}
