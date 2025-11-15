package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.game.Game;
import com.google.common.collect.ImmutableList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Section {
    public ServerPlayer owner;
    public Game game;
    public MinecraftServer server;
    public ServerLevel level;

    public SectionLocation location;
    public SectionRotation rotation;
    public SectionContent content;
    public SectionCounter process;
    public MotionTransform transform;

    public List<ServerPlayer> killed = new ArrayList<>();

    //status
    public boolean viewing = false;
    public boolean placed = false;
    public boolean initialized = false;

    public Section(ServerPlayer owner, SectionLocation location, Game game){
        this.owner = owner;
        this.location = location;
        this.game = game;

        this.server = owner.getServer();
        this.level = server.overworld();
        this.rotation = new SectionRotation(this);
        this.content = new SectionContent(this, location, level);
        this.transform = new MotionTransform(this);
    }

    public void setProcess(int ticks){
        process = new SectionCounter(ticks, -1, true, this::tickRun, this::secondRun);
    }


    public void setProcess(int ticks, int seconds, boolean loop){
        process = new SectionCounter(ticks, seconds, loop, this::tickRun, this::secondRun);
    }

    public void tick(){
        if (!placed&&viewing) handleView();
        if (Objects.nonNull(process)&&initialized) {process.tick();}
    }

    public void view(){
        if (!placed){
            content.createViewEntities();
            viewing = true;
        }
    }

    public void handleView(){
        content.handleView();
    }

    public boolean place(){
        boolean result = content.handlePlace();
        if (result) {
            init();
            initialized = true;
        };
        return result;
    }

    public void remove(){
        if (Objects.nonNull(process)) process.stop();
        content.remove();
    }

    public void init(){}

    public void tickRun(int tickTime){}

    public void secondRun(int secondTime){}

    public void onPlayerWin(ServerPlayer player) {}

    public void onPlayerDeath(ServerPlayer player) {}

    public void onRoundStart() {}

    public void onRoundEnd() {
        if (Objects.nonNull(process)) {
            process.stop();
            process.reset();
        }
        killed.clear();
    }

    public void killAll(List<ServerPlayer> players){
        players.forEach(this::kill);
    }

    public void kill(ServerPlayer player){
        ImmutableList<ServerPlayer> playings = game.getStatus().getPlayings();
        if (!killed.contains(player)&&playings.contains(player)) killed.add(player);
    }

    public void onRoundRunning() {
        if (Objects.nonNull(process)) process.start();
    }
}
