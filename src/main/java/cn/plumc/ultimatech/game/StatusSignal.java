package cn.plumc.ultimatech.game;

import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.utils.TickUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import static cn.plumc.ultimatech.info.StatusTags.PICKED_SECTION_TAG;
import static cn.plumc.ultimatech.info.StatusTags.PUTTED_SECTION_TAG;

public class StatusSignal {
    public Game game;

    public StatusSignal(Game game) {
        this.game = game;
    }

    public void onSectionPicked(ServerPlayer player, ItemStack stack) {
        ListTag tags = (ListTag)stack.get(DataComponents.CUSTOM_DATA).copyTag().get("Tags");
        String id = tags.getString(0);
        Section section = game.getSectionManager().buildSection(id, player);
        game.getStatus().roundSections.put(player, section);

        TickUtil.tickRun(()->{
            player.addTag(PICKED_SECTION_TAG);
            player.closeContainer();
            player.getInventory().clearContent();
            player.setGameMode(GameType.SPECTATOR);
        });
    }

    public void onSectionClose(ServerPlayer player){
        if (!player.getTags().contains(PICKED_SECTION_TAG)){
            player.addTag(PICKED_SECTION_TAG);
            player.addTag(PUTTED_SECTION_TAG);
        }
        player.sendSystemMessage(Component.literal("你放弃了本轮的道具放置"));
    }

    public void onPlayerWin(ServerPlayer player) {
        game.getSectionManager().getSections().forEach(section -> section.onPlayerWin(player));
    }

    public void onPlayerDeath(ServerPlayer player) {
        game.getSectionManager().getSections().forEach(section -> section.onPlayerDeath(player));
    }

    public void onRoundStart() {
        game.getSectionManager().getSections().forEach(Section::onRoundStart);
    }

    public void onRoundEnd() {
        game.getSectionManager().getSections().forEach(Section::onRoundEnd);
    }

    public void onRoundRunning() {
        game.getSectionManager().getSections().forEach(Section::onRoundRunning);
    }
}
