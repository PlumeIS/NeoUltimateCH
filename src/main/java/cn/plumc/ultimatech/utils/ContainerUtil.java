package cn.plumc.ultimatech.utils;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ContainerUtil {
    public static void broadcastUpdate(List<ServerPlayer> players, int containerId, int slot, ItemStack stack) {
        if (slot == -999) return;
        ClientboundContainerSetSlotPacket packet = new ClientboundContainerSetSlotPacket(containerId, 0, slot, stack);
        players.forEach(player -> player.connection.send(packet));
    }
}
