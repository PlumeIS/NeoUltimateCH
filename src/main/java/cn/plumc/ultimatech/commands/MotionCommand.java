package cn.plumc.ultimatech.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class MotionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("motion").requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.literal("add")
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(MotionCommand::add)
                                                ).executes(MotionCommand::helper)
                                        ).executes(MotionCommand::helper)
                                ).executes(MotionCommand::helper)
                        )
                        .then(Commands.literal("set")
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(MotionCommand::set)
                                                ).executes(MotionCommand::helper)
                                        ).executes(MotionCommand::helper)
                                ).executes(MotionCommand::helper)
                        )
                        .then(Commands.literal("scale")
                                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(MotionCommand::scale)
                                                ).executes(MotionCommand::helper)
                                        ).executes(MotionCommand::helper)
                                ).executes(MotionCommand::helper)
                        )
                        .then(Commands.literal("facing")
                                .then(Commands.argument("i", DoubleArgumentType.doubleArg())
                                        .executes(MotionCommand::facing)
                                ).executes(MotionCommand::helper)
                        )
                        .executes(MotionCommand::helper)
                )
                .executes(MotionCommand::helper)
        );
    }

    public static int add(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
        double x = DoubleArgumentType.getDouble(context, "x");
        double y = DoubleArgumentType.getDouble(context, "y");
        double z = DoubleArgumentType.getDouble(context, "z");
        if (entities.isEmpty()) return 0;
        for (Entity entity : entities) {
            Vec3 movement = entity.getDeltaMovement();
            setMotion(entity, movement.add(x, y, z)) ;
        }
        return 1;
    }

    public static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
        double x = DoubleArgumentType.getDouble(context, "x");
        double y = DoubleArgumentType.getDouble(context, "y");
        double z = DoubleArgumentType.getDouble(context, "z");
        if (entities.isEmpty()) return 0;
        for (Entity entity : entities) {
            setMotion(entity, new Vec3(x, y, z)) ;
        }
        return 1;
    }

    public static int scale(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
        double x = DoubleArgumentType.getDouble(context, "x");
        double y = DoubleArgumentType.getDouble(context, "y");
        double z = DoubleArgumentType.getDouble(context, "z");
        if (entities.isEmpty()) return 0;
        for (Entity entity : entities) {
            Vec3 movement = entity.getDeltaMovement();
            setMotion(entity, movement.multiply(x, y, z)) ;
        }
        return 1;
    }

    public static int facing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(context, "targets");
        double i = DoubleArgumentType.getDouble(context, "i");
        for (Entity entity : entities) {
            if (!(entity instanceof ServerPlayer player)) continue;
            float playerPitch = player.getCamera().getXRot(); // 玩家的俯仰角
            float playerYaw = player.getCamera().getYRot(); // 玩家的偏航角

            double pitchRadians = Math.toRadians(playerPitch);
            double yawRadians = Math.toRadians(playerYaw);

            double directionX = -Math.sin(yawRadians) * Math.cos(pitchRadians);
            double directionY = -Math.sin(pitchRadians);
            double directionZ = Math.cos(yawRadians) * Math.cos(pitchRadians);
            Vec3 facing = new Vec3(directionX, directionY, directionZ).normalize();
            Vec3 movement = player.getDeltaMovement();
            return setMotion(player, movement.add(facing.scale(i))) ;
        }
        return 0;
    }

    private static int setMotion(Entity entity, Vec3 motion){
        entity.setDeltaMovement(motion);
        for (ServerPlayer player : entity.getServer().getPlayerList().getPlayers()){
            player.connection.send(new ClientboundSetEntityMotionPacket(entity));
        }
        return 1;
    }

    public static int helper(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("用法: /motion <targets> [add/set/scale/facing] <x> <y> <z>"));
        return 0;
    }
}
