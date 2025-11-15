package cn.plumc.ultimatech.listeners;

import cn.plumc.ultimatech.UltimateCH;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class PlayerListener {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event){
        PlayerUtil.cachePlayerTexture((ServerPlayer) event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event){
        if (UltimateCH.game!=null){
            UltimateCH.game.getPlayerManager().leave((ServerPlayer) event.getEntity());
        }
    }
}
