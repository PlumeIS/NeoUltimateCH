package cn.plumc.ultimatech.listeners;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.provider.WallJumpProvider;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class PlayerListener {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        PlayerUtil.cachePlayerTexture(player);
        WallJumpProvider.createWallJumpEntities(player, player.position());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        WallJumpProvider.createWallJumpEntities(player, player.position());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Lobby.games.values().forEach(game -> {
            game.getPlayerManager().leave((ServerPlayer) event.getEntity());
        });
    }
}
