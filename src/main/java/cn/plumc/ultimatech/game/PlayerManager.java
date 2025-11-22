package cn.plumc.ultimatech.game;

import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionRotation;
import cn.plumc.ultimatech.utils.TickUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;

import static cn.plumc.ultimatech.info.StatusTags.*;
import static cn.plumc.ultimatech.info.UCHInfos.*;

public class PlayerManager {
    private final List<ServerPlayer> players = new ArrayList<>();

    private Game game;
    private GameStatus status;

    public PlayerManager(Game game, GameStatus status) {
        this.game = game;
        this.status = status;
    }

    public void setMapTime(){
        ClientboundSetTimePacket packet = new ClientboundSetTimePacket(game.getLevel().getGameTime(), game.getStatus().map.dayTime, false);
        players.forEach(player -> {
            player.getTags().remove(StatusTags.SKIP_TIME_SYNC_TAG);
            player.connection.send(packet);
            player.getTags().add(SKIP_TIME_SYNC_TAG);
        });
    }

    public void resetMapTime(){
        ServerLevel level = game.getLevel();
        ClientboundSetTimePacket packet = new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(), level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));
        players.forEach(player -> {
            player.getTags().remove(StatusTags.SKIP_TIME_SYNC_TAG);
            player.connection.send(packet);
        });
    }

    public void join(ServerPlayer player) {
        players.add(player);
    }

    public void leave(ServerPlayer player) {
        players.remove(player);
        this.status.playings.remove(player);
        this.status.winners.remove(player);
        this.status.losers.remove(player);
        this.status.roundSections.remove(player).remove();
        this.status.putSectionPlayers.remove(player);
        this.status.playerScore.remove(player);
        this.status.playerKills.remove(player);
        this.status.playerCoins.remove(player);
        this.status.playerLoseRound.remove(player);
        this.status.playerLoseRoundCopied.remove(player);
        if (this.status.firstWinPlayer == player) this.status.firstWinPlayer = null;

    }

    public String settledPlayer(ServerPlayer player){
        String message = "§7失败(+0)";

        if (status.losers.isEmpty()){
            message = "§7太简单了!(金币除外)";
            int coin = game.getSectionManager().getPlayerCoin(player);
            if (coin > 0){
                message += "§e金币(+12*%d)".formatted(coin);
                status.playerScore.get(player).add(12);
            }
            return message;
        }

        if (status.winners.contains(player)){
            message = "§9胜利(+20)";
            status.playerScore.get(player).add(20);
            if (status.playerLoseRoundCopied.get(player) - this.status.minPlayerLoseRound >= status.COMEBACK_ROUND){
                message += "§5翻盘(+16)";
                status.playerScore.get(player).add(16);
            }
            status.playerLoseRound.get(player).set(0);
        } else {
            status.playerLoseRound.get(player).add(1);
        }

        if (player == status.firstWinPlayer){
            if (status.winners.size()==1){
                message += "§b独行(+12)";
                status.playerScore.get(player).add(12);
            }else {
                message += "§a第一(+4)";
                status.playerScore.get(player).add(4);
            }
        }
        status.firstWinPlayer = null;

        if (status.playerKills.get(player).get()>0){
            message += "§6陷阱(+4*%d)".formatted(status.playerKills.get(player).get());
            status.playerScore.get(player).add(status.playerKills.get(player).get()*4);
        }

        int coin = game.getSectionManager().getPlayerCoin(player);
        if (coin > 0){
            message += "§e金币(+12*%d)".formatted(coin);
            status.playerScore.get(player).add(12);
        }

        return message;
    }

    public void clearTags(){
        for (ServerPlayer player : ImmutableList.copyOf(players)) {
            player.removeTag(PICKED_SECTION_TAG);
            player.removeTag(PUTTING_SECTION_TAG);
            player.removeTag(PUTTED_SECTION_TAG);
        }
    }

    public void onPlayerUseItem(ServerPlayer player, ItemStack itemStack) {
        SectionManager sectionManager = game.getSectionManager();
        if (ItemStack.isSameItem(itemStack, SECTION_PLACE_ITEM)){
            for (Section section : game.getStatus().roundSections.values()){
                if (section.owner.getUUID().equals(player.getUUID())&&!section.placed){
                    if (section.place()){
                        player.addTag(PUTTED_SECTION_TAG);
                        player.removeTag(PUTTING_SECTION_TAG);
                        TickUtil.tickRun(()->{
                            player.getInventory().clearContent();
                            player.setGameMode(GameType.SPECTATOR);
                        });
                    }
                }
            }
        } else if (ItemStack.isSameItem(itemStack, SECTION_ROTATE_X_ITEM)){
            for (Section section : game.getStatus().roundSections.values()){
                if (section.owner.getUUID().equals(player.getUUID())&&!section.placed){
                    section.rotation.rotate(SectionRotation.Axis.X, SectionRotation.RotationHandle.CLOCKWISE);
                }
            }
        } else if (ItemStack.isSameItem(itemStack, SECTION_ROTATE_Y_ITEM)){
            for (Section section : game.getStatus().roundSections.values()){
                if (section.owner.getUUID().equals(player.getUUID())&&!section.placed){
                    section.rotation.rotate(SectionRotation.Axis.Y, SectionRotation.RotationHandle.CLOCKWISE);
                }
            }
        } else if (ItemStack.isSameItem(itemStack, SECTION_ROTATE_Z_ITEM)){
            for (Section section : game.getStatus().roundSections.values()){
                if (section.owner.getUUID().equals(player.getUUID())&&!section.placed){
                    section.rotation.rotate(SectionRotation.Axis.Z, SectionRotation.RotationHandle.CLOCKWISE);
                }
            }
        }
    }

    public ImmutableList<ServerPlayer> getPlayers(){
        return ImmutableList.copyOf(players);
    }
}
