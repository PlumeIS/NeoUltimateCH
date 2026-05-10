package cn.plumc.ultimatech.commands;

import cn.plumc.ultimatech.provider.offset.OffsetProvider;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class OffsetCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(net.minecraft.commands.Commands.literal("offset")
                .executes(context -> {
                    net.minecraft.server.level.ServerPlayer player = context.getSource().getPlayerOrException();
                    OffsetProvider.INSTANCE.testing(player);
                    return 1;
                })
        );
    }

}
