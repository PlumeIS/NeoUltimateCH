package cn.plumc.ultimatech.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static cn.plumc.ultimatech.info.StatusTags.PICKED_SECTION_TAG;
import static cn.plumc.ultimatech.info.StatusTags.PUTTED_SECTION_TAG;

public class GameUtil {
    private Game game;

    public GameUtil(Game game) {
        this.game = game;
    }

    public boolean checkAllPlayerPicked() {
        for (ServerPlayer player : game.getStatus().getRoundPlayings()){
            if (!player.getTags().contains(PICKED_SECTION_TAG)){
                return false;
            }
        }
        return true;
    }
    public boolean checkAllPlayerPutted() {
        for (ServerPlayer player : game.getStatus().getRoundPlayings()){
            if (!player.getTags().contains(PUTTED_SECTION_TAG)){
                return false;
            }
        }
        return true;
    }

    public void broadcast(String text){
        for (ServerPlayer player : game.getPlayerManager().getPlayers()){
            player.sendSystemMessage(Component.literal(text));
        }
    }
}
