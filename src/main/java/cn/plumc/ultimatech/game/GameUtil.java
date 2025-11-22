package cn.plumc.ultimatech.game;

import cn.plumc.ultimatech.utils.TickUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
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

    public void broadcastTitle(String text){
        ClientboundSetTitleTextPacket packet = new ClientboundSetTitleTextPacket(Component.literal(text));
        for (ServerPlayer player : game.getPlayerManager().getPlayers()){
            player.connection.send(packet);
        }
    }

    public void broadcastSubTitle(String subtitle){
        ClientboundSetSubtitleTextPacket packet = new ClientboundSetSubtitleTextPacket(Component.literal(subtitle));
        for (ServerPlayer player : game.getPlayerManager().getPlayers()){
            player.connection.send(packet);
        }
    }

    public void countdown(int time, boolean broadcast, boolean title , String message ,Runnable callback){
        if (broadcast) {
            for (int i = time; i > 0; i--) {
                final int seconds = i;
                if (title) TickUtil.runAfterTick(() -> broadcastTitle(message.formatted(seconds)), time - i);
                else TickUtil.runAfterTick(() -> broadcast(message.formatted(seconds)), time - i);
            }
        }

        TickUtil.runAfterTick(callback, time);
    }

    public void countdown(int time, boolean broadcast, Runnable callback){
        if (broadcast) {
            for (int i = time; i > 0; i--) {
                final int seconds = i;
                TickUtil.runAfterTick(() -> broadcast("游戏将在%d秒后开始".formatted(seconds)), time - i);
            }
        }

        TickUtil.runAfterTick(callback, time);
    }
}
