package cn.plumc.ultimatech.listeners;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.layer.LayerType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

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
        if (player.getTags().contains("develop.test")){
            event.setCanceled(true);
            BlockPos pos = event.getPos();
            Game game = Lobby.games.get(MapInfo.FragileBridge.id);
            List<Section> sectionsByPos = game.getSectionManager().getSectionsByPos(pos, LayerType.MIDDLE);
            if (sectionsByPos.isEmpty()) return;
            player.sendSystemMessage(Component.literal(sectionsByPos.get(0).content.origin.toString()));
        }
    }

    @SubscribeEvent
    public static void onFarmlandTrampleEvent(BlockEvent.FarmlandTrampleEvent event) {
        event.setCanceled(true);
    }
}
