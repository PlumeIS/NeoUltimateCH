package cn.plumc.ultimatech;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.section.hit.PlayerHit;
import cn.plumc.ultimatech.utils.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class LobbyRegion {
        public enum Type{
            GAME, OTHER
        }

        public Type type;
        public String name;
        public AABB aabb;
        public BlockPos pos1;
        public BlockPos pos2;
        public boolean visible;
        public int counter;

        public LobbyRegion(Type type, String name , BlockPos pos1, BlockPos pos2, boolean visible) {
            this.type = type;
            this.name = name;
            this.pos1 = pos1;
            this.pos2 = pos2;
            int min_x = Math.min(pos1.getX(), pos2.getX());
            int min_y = Math.min(pos1.getY(), pos2.getY());
            int min_z = Math.min(pos1.getZ(), pos2.getZ());
            int max_x = Math.max(pos1.getX(), pos2.getX())+1;
            int max_y = Math.max(pos1.getY(), pos2.getY())+1;
            int max_z = Math.max(pos1.getZ(), pos2.getZ())+1;
            this.aabb = new AABB(min_x, min_y, min_z, max_x, max_y, max_z);
            this.visible = visible;
            this.counter = 0;
        }

        public List<ServerPlayer> getRegionPlayers(MinecraftServer server) {
            return server.overworld().getPlayers(player -> new PlayerHit(player).intersectAABB(this.aabb)&&!player.isSpectator());
        }

        public Vector3f getRegionColor(int playerCount){
            if (playerCount == 0) return new Vector3f(0.59f,0.59f,0.59f);
            if (playerCount == 1) return new Vector3f(1f,1f,0.59f);
            else return new Vector3f(0.59f,1.0f,0.59f);
        }

        public void render(int playerCount, MinecraftServer server) {
            if (!visible) return;
            Vec3 min = aabb.getMinPosition().add(0.08, 0.08, 0.08);
            Vec3 max = aabb.getMaxPosition().add(-0.08, -0.08, -0.08);
            List<Vec3> points = BlockUtil.generateOutlinePoints(0.2, new AABB(min, max), 0.3);
            Vector3f color = getRegionColor(playerCount);
            ServerLevel level = server.overworld();
            points.forEach(point -> level.sendParticles(new DustParticleOptions(color, 0.5f),
                    point.x, point.y, point.z, 1,
                    0, 0, 0, 0
            ));
        }

        public static void tick(MinecraftServer server) {
            for (LobbyRegion lobbyRegion : Lobby.regions.values()) {
                List<ServerPlayer> regionPlayers = lobbyRegion.getRegionPlayers(server);
                lobbyRegion.handleRegion(regionPlayers, server);
            }
        }
        private void handleRegion(List<ServerPlayer> regionPlayers, MinecraftServer server) {
            if (type == LobbyRegion.Type.GAME) {
                handleGame(regionPlayers);
            }
            if (!regionPlayers.isEmpty()) render(regionPlayers.size(), server);
        }

        private void handleGame(List<ServerPlayer> regionPlayers) {
            if (regionPlayers.isEmpty()) {
                counter = 0;
                return;
            }
            MapInfo mapInfo = MapInfo.getMap(name);
            int minStartPlayer = mapInfo.map.minStartPlayer;
            boolean gaming = Lobby.games.containsKey(name);
            regionPlayers.forEach(player -> {
                MutableComponent text = Component.literal("地图:")
                        .append(mapInfo.name)
                        .append(" 玩家:")
                        .append("%d/%d".formatted(regionPlayers.size(), minStartPlayer))
                        .append(" 进度:")
                        .append(gaming ? "游玩中" : "未开启");
                player.connection.send(new ClientboundSetActionBarTextPacket(text));
            });
            if (!gaming) {
                if (regionPlayers.size()>=minStartPlayer) {
                    int time = 15 - (counter/20);
                    if (counter%20==0) {
                        regionPlayers.forEach(player -> {
                            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.valueOf(time))));
                        });
                    }
                    if (counter == 15*20) {
                        Game game = new Game(mapInfo);
                        Lobby.games.put(name, game);
                        regionPlayers.forEach(game.getPlayerManager()::join);
                        game.gameStart();
                    }
                    counter++;
                } else {
                    counter = 0;
                }
            }
        }
    }