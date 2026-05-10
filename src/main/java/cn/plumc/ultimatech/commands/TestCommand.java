package cn.plumc.ultimatech.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;

import java.util.HashMap;
import java.util.UUID;

public class TestCommand {
    public static HashMap<UUID, Pig> temp = new HashMap<>();
    public static final String TAG = "test_aaaaaa";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("uchtest")
                .executes((context) -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    Pig pig = new Pig(EntityType.PIG, context.getSource().getLevel());
                    pig.setNoAi(true);
                    pig.setInvisible(true);
                    pig.setPos(player.position());
                    context.getSource().getLevel().addFreshEntity(pig);
                    player.addTag(TAG);
                    player.startRiding(pig, true);
                    temp.put(player.getUUID(), pig);
                    return 1;
                })
        );
    }

}
