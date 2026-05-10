package cn.plumc.ultimatech.game.map.maps;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.section.MotionTransform;
import cn.plumc.ultimatech.utils.DisplayEntityUtil;
import cn.plumc.ultimatech.utils.IntCounter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.*;

public class Island extends Map {
    private final long dayTime;
    private final long nightTime;
    private boolean inDay;
    private final IntCounter cycleCounter;
    private List<Entity> tentacleEntities = new ArrayList<>();
    private List<Tentacle> tentacles = new ArrayList<>();

    public Island() {
        super(
                new Map.Region(
                        new BlockPos(7926 ,30 ,7969),
                        new BlockPos(7981 ,99 ,8071)),
                new Map.Region(
                        new BlockPos(7950 ,79 ,8042),
                        new BlockPos(7953 ,82 ,8045)),
                new Map.Region(
                        new BlockPos(7951 ,75 ,7990),
                        new BlockPos(7954 ,78 ,7993)),
                -10,
                6000,
                2
        );
        this.dayTime = 6000;
        this.nightTime = 18000;
        this.inDay = true;
        this.cycleCounter = new IntCounter();
    }

    @Override
    public void startGame(Game game, List<ServerPlayer> players) {
        super.startGame(game, players);
        this.inDay = true;
        this.cycleCounter.set(20*5*60);
        changeTime(this.dayTime);
        tentacleEntities.clear();
        currentGame.getLevel().getEntities().get(
                mapRegion.getAABB(), entity -> tentacleEntities.add(entity)
        );
        initTentacles();
    }

    @Override
    public void reset() {
        tentacles.forEach(Tentacle::reset);
    }

    @Override
    public void tick() {
        if (currentGame.getStatus().isRoundRunning()) {
            cycleCounter.add();
            if (cycleCounter.get() > 20*5*60) {
                this.inDay = !inDay;
                if (inDay) changeTime(dayTime);
                else changeTime(nightTime);
                cycleCounter.set(0);
            }
            if (!inDay) {
                if (isTentacleTriggered()) {
                    for (Tentacle t : tentacles) {
                        t.trigger();
                    }
                }
            }
            
            for (Tentacle t : tentacles) {
                t.tick();
            }
        }
    }

    private void initTentacles() {
        tentacles.clear();
        java.util.Map<Integer, List<Entity>> tentacleMap = new HashMap<>();
        for (Entity entity : tentacleEntities) {
            if (!(entity instanceof Display.ItemDisplay)) continue;
            int tentacleId = -1;
            for (String tag : entity.getTags()) {
                if (tag.startsWith("tentacle:")) {
                    try {
                        tentacleId = Integer.parseInt(tag.substring(9));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (tentacleId != -1) {
                tentacleMap.computeIfAbsent(tentacleId, k -> new ArrayList<>()).add(entity);
            }
        }

        for (java.util.Map.Entry<Integer, List<Entity>> entry : tentacleMap.entrySet()) {
            tentacles.add(new Tentacle(entry.getKey(), entry.getValue()));
        }
        for (Tentacle t : tentacles) {
            t.hide();
        }
    }

    private boolean isTentacleTriggered() {
//        for (ServerPlayer player : currentGame.getPlayerManager().getPlayers()) {
//            if (!player.isAlive()) continue;
//            if (player.position().distanceTo(tentacle.originalPositions.get(0)) < 16) {
//                return true;
//            }
//        }
//        return false;
        return true;
    }

    private class Tentacle {
        public final int id;
        public final List<Entity> nodes = new ArrayList<>();
        public final List<Double> lengths = new ArrayList<>();
        public final List<Vec3> originalPositions = new ArrayList<>();
        public final List<String> items = new ArrayList<>();
        
        public int state = 0; // 0: IDLE, 1: EMERGING, 2: WAVING, 3: SINKING
        public int tickCounter = 0;
        
        // Random offsets for each tentacle to make them desynchronized
        private final double randomPhaseX;
        private final double randomPhaseZ;
        private final double randomSpeedX;
        private final double randomSpeedZ;
        
        public Tentacle(int id, List<Entity> unsortedNodes) {
            this.id = id;
            
            // Initialize random parameters based on ID for consistent but varied movement
            Random rand = new Random(id * 314159L); 
            this.randomPhaseX = rand.nextDouble() * Math.PI * 2;
            this.randomPhaseZ = rand.nextDouble() * Math.PI * 2;
            this.randomSpeedX = 0.8 + rand.nextDouble() * 0.4; // 0.8 to 1.2 multiplier
            this.randomSpeedZ = 0.8 + rand.nextDouble() * 0.4; // 0.8 to 1.2 multiplier
            
            java.util.Map<String, Entity> byThis = new HashMap<>();
            String firstId = null;
            for (Entity e : unsortedNodes) {
                String thisId = null;
                String lastId = null;
                for (String tag : e.getTags()) {
                    if (tag.startsWith("node:")) thisId = tag.substring(5);
                    if (tag.startsWith("node.last:")) lastId = tag.substring(10);
                }
                if (thisId != null) byThis.put(thisId, e);
                if (lastId == null || lastId.equals("null")) {
                    firstId = thisId;
                }
            }
            
            if (firstId == null) {
                Set<String> allNexts = new HashSet<>();
                for (Entity e : unsortedNodes) {
                    for (String tag : e.getTags()) {
                        if (tag.startsWith("node.next:")) allNexts.add(tag.substring(10));
                    }
                }
                for (String thisId : byThis.keySet()) {
                    if (!allNexts.contains(thisId)) {
                        firstId = thisId;
                        break;
                    }
                }
            }

            Entity current = byThis.get(firstId);
            while (current != null) {
                this.nodes.add(current);
                this.originalPositions.add(current.position());
                CompoundTag data = new EntityDataAccessor(current).getData();
                CompoundTag itemTag = data.getCompound("item");
                this.items.add(itemTag.getString("id"));
                
                String nextId = null;
                for (String tag : current.getTags()) {
                    if (tag.startsWith("node.next:")) nextId = tag.substring(10);
                }
                if (nextId == null || nextId.equals("null")) break;
                current = byThis.get(nextId);
            }
            
            if (nodes.size() > 1 && originalPositions.get(0).y > originalPositions.get(nodes.size() - 1).y) {
                Collections.reverse(nodes);
                Collections.reverse(originalPositions);
                Collections.reverse(items);
            }
            
            for (int i = 0; i < nodes.size(); i++) {
                if (i == 0) lengths.add(0.0);
                else lengths.add(originalPositions.get(i).distanceTo(originalPositions.get(i-1)));
            }
            System.out.println(nodes);
        }
        
        public void trigger() {
            if (state == 0) {
                state = 1;
                tickCounter = 0;
                for (int i = 0; i < nodes.size(); i++) {
                    DisplayEntityUtil.setVisible(nodes.get(i), items.get(i), true);
                }
            }
        }

        public void reset() {
            show();
            for (Entity node : nodes) {
                MotionTransform.updateEntityTransformation(node, t -> {
                    t.put("translation", MotionTransform.toFloatListTag(Vec3.ZERO));
                    t.put("left_rotation", MotionTransform.toFloatListTag(new Quaternionf()));
                }, 0.05); // Kept 0.05s interpolation as requested
            }
        }

        public void hide() {
            for (int i = 0; i < nodes.size(); i++) {
                DisplayEntityUtil.setVisible(nodes.get(i), items.get(i), false);
            }
        }

        public void show(){
            for (int i = 0; i < nodes.size(); i++) {
                DisplayEntityUtil.setVisible(nodes.get(i), items.get(i), true);
            }
        }
        
        public void tick() {
            if (state == 0) return;
            
            tickCounter++;
            
            int EMERGE_TICKS = 60;
            int WAVE_TICKS = 400;
            int SINK_TICKS = 60;
            
            double emergeProgress = 1.0;
            if (state == 1) {
                emergeProgress = (double) tickCounter / EMERGE_TICKS;
                if (tickCounter >= EMERGE_TICKS) {
                    state = 2;
                    tickCounter = 0;
                }
            } else if (state == 2) {
                emergeProgress = 1.0;
                if (tickCounter >= WAVE_TICKS) {
                    state = 3;
                    tickCounter = 0;
                }
            } else if (state == 3) {
                emergeProgress = 1.0 - (double) tickCounter / SINK_TICKS;
                if (tickCounter >= SINK_TICKS) {
                    state = 0;
                    tickCounter = 0;
                    hide();
                    return;
                }
            }

            int N = nodes.size();
            if (N == 0) return;
            
            double H = Math.abs(originalPositions.get(N-1).y - originalPositions.get(0).y);
            double dropHeight = H + 5.0; // Drop 5 blocks below base
            
            Vec3 curBasePos = originalPositions.get(0).add(0, (emergeProgress - 1.0) * dropHeight, 0);
            Vec3 pos = curBasePos;
            
            double time = state == 1 ? tickCounter : (state == 2 ? EMERGE_TICKS + tickCounter : EMERGE_TICKS + WAVE_TICKS + tickCounter);


            for (int i = 0; i < N; i++) {
                Entity node = nodes.get(i);
                
                // Exponential-like scaling for angle so the tip waves much more than the base
                double relativePos = i / (double) Math.max(1, N - 1); // 0.0 at base, 1.0 at tip
                double maxAngle = 1.5 * Math.pow(relativePos, 1.5); // increased amplitude and non-linear scaling
                
                // Decrease wave speed and increase frequency to make it look more like organic waving
                double waveSpeed = 0.08;
                double waveK = 0.3;      
                
                // Use different frequencies and random offsets for X and Z to avoid purely circular/synchronized motion
                double thetaX = maxAngle * Math.sin(waveSpeed * this.randomSpeedX * time - waveK * i + this.randomPhaseX);
                double thetaZ = maxAngle * Math.cos(waveSpeed * 1.3 * this.randomSpeedZ * time - waveK * 1.2 * i + this.randomPhaseZ); // 1.3 is to make the frequency of X and Z unaligned

                org.joml.Quaternionf q = new org.joml.Quaternionf().rotateXYZ((float)thetaX, 0, (float)thetaZ);

                org.joml.Vector3f dir = new org.joml.Vector3f(0, 1, 0);
                q.transform(dir);

                if (i > 0) {
                    pos = pos.add(dir.x() * lengths.get(i), dir.y() * lengths.get(i), dir.z() * lengths.get(i));
                }
                
                Vec3 offset = pos.subtract(node.position());

                MotionTransform.updateEntityTransformation(node, t -> {
                    t.put("translation", MotionTransform.toFloatListTag(offset));
                    t.put("left_rotation", MotionTransform.toFloatListTag(q));
                }, 0.05); // Kept 0.05s interpolation as requested
            }
        }
    }
}
