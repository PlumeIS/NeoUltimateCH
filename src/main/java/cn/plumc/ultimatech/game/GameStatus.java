package cn.plumc.ultimatech.game;

import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.utils.IntCounter;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class GameStatus {
    // 玩家集合
    public List<ServerPlayer> playings = new ArrayList<>();
    public List<ServerPlayer> losers = new ArrayList<>();
    public List<ServerPlayer> winners = new ArrayList<>();
    public LinkedList<ServerPlayer> putSectionPlayers = new LinkedList<>();

    // 玩家数据
    public HashMap<ServerPlayer, IntCounter> playerScore = new HashMap<>();
    public HashMap<ServerPlayer, IntCounter> playerKills = new HashMap<>();
    public HashMap<ServerPlayer, IntCounter> playerCoins = new HashMap<>();
    public HashMap<ServerPlayer, IntCounter> playerLoseRound = new HashMap<>();
    public HashMap<ServerPlayer, Integer> playerLoseRoundCopied = new HashMap<>();

    // 游戏
    public boolean gameStarting = false;

    // 游戏数据
    public final MapInfo mapInfo;
    public final Map map;
    public final int winScore = 100;
    public int round = 0;
    public final static int COMEBACK_ROUND = 3;

    // 回合数据
    public int minPlayerLoseRound = 0;
    public ServerPlayer firstWinPlayer = null;
    
    public HashMap<ServerPlayer, Section> roundSections = new HashMap<>();
    public final HashMap<UUID, Integer> playerSectionBoxIds = new HashMap<>();
    
    public boolean roundReady = false;
    public boolean roundRunning = false;
    public boolean roundCountdowning = false;

    public GameStatus(MapInfo mapInfo){
        this.mapInfo = mapInfo;
        this.map = mapInfo.map;
    }

    public ImmutableList<ServerPlayer> getRoundPlayings(){
        return ImmutableList.copyOf(playings);
    }

    public ImmutableList<ServerPlayer> getPlayings(){
        return isRoundRunning() ? ImmutableList.copyOf(playings) : ImmutableList.of();
    }

    public Map getMap(){
        return map;
    }

    public boolean isGameStarting(){
        return gameStarting;
    }

    public boolean isRoundRunning(){
        return roundRunning && !roundCountdowning;
    }
}
