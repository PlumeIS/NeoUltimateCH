package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.layer.LayerType;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

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
    public LayerType runningLayer = LayerType.MIDDLE;
    public boolean viewing = false;
    public boolean placed = false;
    public boolean removed = false;
    public boolean initialized = false;
    public boolean mapSection = false;


    public Section(ServerPlayer owner, SectionLocation location, Game game){
        this.owner = owner;
        this.location = location;
        this.game = game;

        this.server = owner.getServer();
        this.level = server.overworld();
        this.rotation = new SectionRotation(this);
        this.content = new SectionContent(this, location, level);
        this.transform = new MotionTransform(this);

        if (PlayerUtil.isMapHolder(owner)) mapSection = true;
    }

    public void setProcess(int ticks){
        if (removed) return;
        process = new SectionCounter(ticks, -1, true, this::tickRun, this::secondRun);
    }


    public void setProcess(int ticks, int seconds, boolean loop){
        if (removed) return;
        process = new SectionCounter(ticks, seconds, loop, this::tickRun, this::secondRun);
    }

    public void tick(){
        if (removed) return;
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
        removed = true;
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

    public void setOnMove(Section section){}

    public void onMove(Vec3i movement){}

    public boolean isStatic(){return true;}

    public LayerType getRunningLayer(){return runningLayer;}

    public void setRunningLayer(LayerType layer){
        if (getRunningLayer() == layer) return;
        content.changedBlocks.forEach(pos -> {
            Tuple<BlockState, Optional<BlockEntity>> block = content.manager.getLayer(runningLayer).remove(pos);
            content.manager.getLayer(layer).set(pos, block);
        });
        this.runningLayer = layer;
    }

    @Override
    public String toString() {
        SectionRegistry.SectionInfo info = SectionRegistry.instance.getSectionInfo(this.getClass());
        return "Section[type="+info.id()+" origin="+content.getOrigin()+"]";
    }
}
