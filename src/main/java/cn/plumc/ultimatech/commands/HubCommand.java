package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class HubCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hub")
                .executes(commandContext -> {
                    ServerPlayer player = commandContext.getSource().getPlayer();
                    if (PlayerUtil.containsTag(player, StatusTags.DEVELOPER_TAG))
                        PlayerUtil.teleport(player, UCHInfos.DEVELOPER_POINT);
                    else PlayerUtil.teleport(player, UCHInfos.PLAYER_LOBBY_POINT);
                    return 1;
                })
                .then(Commands.literal("develop")
                        .requires(context -> context.getPlayer().getTags().contains(StatusTags.DEVELOPER_TAG))
                        .executes(commandContext -> {
                            ServerPlayer player = commandContext.getSource().getPlayer();
                            PlayerUtil.teleport(player, UCHInfos.DEVELOPER_POINT);
                            return 1;
                        })
                )
                .then(Commands.literal("map")
                        .requires(context -> context.getPlayer().getTags().contains(StatusTags.DEVELOPER_TAG))
                        .executes(CommandContext -> {
                            ServerPlayer player = CommandContext.getSource().getPlayer();
                            PlayerUtil.teleport(player, new Vec3(114513.0, 19.00, 19810.0));
                            return 1;
                        })
                )
        );
    }
}
