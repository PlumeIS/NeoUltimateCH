package cn.plumc.ultimatech.game;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.game.map.Maps;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionBox;
import cn.plumc.ultimatech.utils.IntCounter;
import cn.plumc.ultimatech.utils.PlayerUtil;
import cn.plumc.ultimatech.utils.TickUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

import static cn.plumc.ultimatech.info.StatusTags.PUTTED_SECTION_TAG;
import static cn.plumc.ultimatech.info.StatusTags.PUTTING_SECTION_TAG;

public class Game {
    private final MinecraftServer server;
    private final ServerLevel level;

    private final GameStatus status;
    private final GameUtil util;
    private final PlayerManager playerManager;
    private final SectionManager sectionManager;
    private final StatusSignal statusSignal;

    public Game(Maps mapInfo) {
        this.status = new GameStatus(mapInfo);
        this.util = new GameUtil(this);
        this.playerManager = new PlayerManager(this, status);
        this.sectionManager = new SectionManager(this, status);
        this.statusSignal = new StatusSignal(this);

        this.server = ServerLifecycleHooks.getCurrentServer();
        this.level = server.getLevel(Level.OVERWORLD);
    }

    public void tick(){
        if (!status.gameStarting) return;

        sectionManager.sectionTick();

        if (!status.roundReady) {
            checkRoundReady();
        }

        if (!status.roundRunning) {
            handleRoundStart();
        }

        if (status.roundRunning && !status.roundCountdowning) {
            handleRoundRunning();
        }
    }

    public void second(){
        status.map.second();
        sectionManager.sectionSecond();
    }

    private void checkRoundReady() {
        if (status.putSectionPlayers.isEmpty() && util.checkAllPlayerPicked() && !util.checkAllPlayerPutted()) {
            status.putSectionPlayers.clear();
            status.putSectionPlayers.addAll(status.playings.stream()
                    .filter(player -> !player.getTags().contains(PUTTED_SECTION_TAG))
                    .toList());
            Collections.shuffle(status.putSectionPlayers);
            status.roundReady = true;
        }
    }

    private void handleRoundStart() {
        if (!status.putSectionPlayers.isEmpty()) {
            startPlayerSectionPlacement();
        }

        if (util.checkAllPlayerPutted()) {
            startCountdown();
        }
    }

    private void startPlayerSectionPlacement() {
        ServerPlayer player = status.putSectionPlayers.poll();

        if (player != null) {
            player.addTag(PUTTING_SECTION_TAG);
            TickUtil.tickRun(() -> {
                Vec3 pos = status.map.getAStartPos();
                player.teleportTo(pos.x, pos.y, pos.z);
                player.setGameMode(GameType.CREATIVE);
            });
            sectionManager.viewRoundSection(player);
        }
    }

    private void startCountdown() {
        status.roundRunning = true;
        status.roundCountdowning = true;

        util.countdown(5, true, () -> {
            util.broadcast("游戏开始");
            status.playings.forEach(player -> player.setGameMode(GameType.ADVENTURE));

            status.playings.forEach(serverPlayer ->
                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20*20, 254, false, false)));

            TickUtil.runAfterTick(() -> util.broadcast("现在pvp已开放!"), 20);

            status.map.startGame(new ArrayList<>(playerManager.getPlayers()));
            status.roundCountdowning = false;
            statusSignal.onRoundRunning();
        });
    }

    private void handleRoundRunning() {
        status.map.tick();

        Iterator<ServerPlayer> iterator = status.playings.iterator();
        while (iterator.hasNext()) {
            ServerPlayer player = iterator.next();

            Boolean playerWinning;
            if (Objects.nonNull(playerWinning=checkPlayerStatus(player))) {
                iterator.remove();
                handlePlayerElimination(player, playerWinning.booleanValue());
            }
        }

        if (status.playings.isEmpty()) {
            roundEnd();
        }
    }

    private Boolean checkPlayerStatus(ServerPlayer player) {
        if (status.map.isLose(player)) {
            status.losers.add(player);
            return false;
        }

        if (status.map.isWin(player)) {
            status.winners.add(player);
            if (status.firstWinPlayer == null) {
                status.firstWinPlayer = player;
            }
            return true;
        }

        Section section;
        if (Objects.nonNull(section=sectionManager.shouldPlayerLose(player))) {
            status.losers.add(player);
            status.playerKills.get(section.owner).add();
            return false;
        }

        return null;
    }

    private void handlePlayerElimination(ServerPlayer player, boolean playerWinning) {
        TickUtil.tickRun(() -> player.setGameMode(GameType.SPECTATOR));

        if (playerWinning) statusSignal.onPlayerWin(player);
        else statusSignal.onPlayerDeath(player);
    }


    public void roundStart(){
        status.roundSections.clear();
        status.roundRunning = false;
        status.roundReady = false;
        status.round++;

        status.playings.clear();
        status.playings.addAll(playerManager.getPlayers());

        status.playerKills.clear();
        playerManager.getPlayers().forEach(player -> status.playerKills.put(player, new IntCounter(0)));

        status.playerSectionBoxIds.clear();
        SectionBox sectionBox = new SectionBox(status.round, status.playings.size());
        playerManager.getPlayers().forEach(player ->
                status.playerSectionBoxIds.put(player.getUUID(), player.openMenu(sectionBox).orElse(-3))
        );
        statusSignal.onRoundStart();
    }

    public void roundEnd(){
        statusSignal.onRoundEnd();
        playerManager.clearTags();
        util.broadcast("=========回合结算=========");

        status.minPlayerLoseRound = playerManager.getPlayers().stream()
                .mapToInt(player -> status.playerLoseRound.get(player).get())
                .min()
                .orElse(Integer.MAX_VALUE);

        status.playerLoseRoundCopied.clear();
        playerManager.getPlayers().forEach(player ->
                status.playerLoseRoundCopied.put(player, status.playerLoseRound.get(player).get())
        );

        playerManager.getPlayers().forEach(player -> {
            String message = playerManager.settledPlayer(player);
            util.broadcast(player.getGameProfile().getName() + ": " + message +
                    "§r " + status.playerScore.get(player).get() + ":" + status.winScore);
        });

        Optional<Tuple<ServerPlayer, Integer>> winnerEntry = playerManager.getPlayers().stream()
                .map(player -> new Tuple<>(player, status.playerScore.get(player).get()))
                .filter(entry -> entry.getB() >= status.winScore)
                .max(Comparator.comparingInt(Tuple::getB));

        if (winnerEntry.isPresent()) {
            gameEnd(winnerEntry.get().getA());
            return;
        }

        status.winners.clear();
        status.losers.clear();
        TickUtil.cancelDelayTask();
        roundStart();
    }

    public void gameStart(){
        playerManager.clearTags();
        playerManager.setMapTime();
        for (ServerPlayer player : playerManager.getPlayers()) {
            status.playerScore.put(player, new IntCounter(0));
            status.playerLoseRound.put(player, new IntCounter(0));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, -1, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.HEAL, -1, 25, false, false));
            server.getGameRules().getRule(GameRules.RULE_FALL_DAMAGE).set(false, server);
        }
        status.map.startGame(playerManager.getPlayers());
        util.broadcastSubTitle("请等待地图加载...");
        util.countdown(10, true, true, "%d",() -> {
            roundStart();
            status.gameStarting = true;
        });
    }

    public void gameEnd(ServerPlayer winner){
        if (winner!=null){
            util.broadcast("=========游戏结束=========");
            util.broadcast("胜利者: %s".formatted(winner.getGameProfile().getName()));
        } else {
            util.broadcast("=========游戏中断=========");
        }
        for (ServerPlayer player : playerManager.getPlayers()) {
            util.broadcast("%s: %s".formatted(player.getGameProfile().getName(), status.playerScore.getOrDefault(player, new IntCounter()).get()));
            PlayerUtil.teleport(player, UCHInfos.PLAYER_LOBBY_POINT);
        }
        playerManager.resetMapTime();
        status.gameStarting = false;
        status.roundReady = false;
        destroy();
        Lobby.onGameEnd(this);
        TickUtil.cancelDelayTask();
    }

    public void destroy(){
        sectionManager.removeAll();
        for (Section section : ImmutableList.copyOf(sectionManager.getSections())) {
            section.remove();
            sectionManager.removeSection(section);
        }
        status.map.reset();
        playerManager.clearTags();
    }

    public ServerLevel getLevel(){return level;}

    public GameStatus getStatus() {
        return status;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public SectionManager getSectionManager() {
        return sectionManager;
    }

    public StatusSignal getStatusSignal() {
        return statusSignal;
    }
}
