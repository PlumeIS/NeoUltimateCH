package cn.plumc.ultimatech.section.hit;

import cn.plumc.ultimatech.game.Game;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public abstract class Hit {
    public abstract boolean intersect(Hit other);
    public abstract boolean intersectAABB(AABB aabb);
    public abstract boolean intersectPlayer(ServerPlayer player);

    public ServerPlayer detectPlayer(Game game) {
        for (PlayerHit playerHit : getPlayerHits(game)) {
            if (playerHit.intersect(this)) return playerHit.getPlayer();
        }
        return null;
    };

    public List<ServerPlayer> detectPlayers(Game game) {
        List<ServerPlayer> players = new ArrayList<>();
        for (PlayerHit playerHit : getPlayerHits(game)) {
            if (playerHit.intersect(this)) players.add(playerHit.getPlayer());
        }
        return players;
    }

    public boolean hasPlayer(Game game) {
        return detectPlayer(game) != null;
    }

    public static List<PlayerHit> getPlayerHits(Game game) {
        List<PlayerHit> hits = new ArrayList<>();
        game.getStatus().getPlayings().forEach(player -> hits.add(new PlayerHit(player)));
        return hits;
    }
}
