package cn.plumc.ultimatech.listeners;

import cn.plumc.ultimatech.info.StatusTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public class BlockListener {
    @SubscribeEvent
    public static void onPlaceBlockEvent(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getTags().contains(StatusTags.PUTTING_SECTION_TAG)){
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onBreakBlockEvent(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.getTags().contains(StatusTags.PUTTING_SECTION_TAG)) {
            event.setCanceled(true);
        }
    }
}
