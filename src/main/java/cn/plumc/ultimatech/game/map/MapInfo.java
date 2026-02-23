package cn.plumc.ultimatech.game.map;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.LobbyRegion;
import cn.plumc.ultimatech.game.map.maps.*;

public enum MapInfo {
    ROOF("roof", "房顶", new Roof(), Map.Region.get(-1, 18, -4, 2 , 20, -2)),
    WATERFALL("waterfall", "瀑布", new Waterfall(), Map.Region.get(39, 10, 7, 36, 12, 11)),
    Wheatland("wheatland", "麦田", new Wheatland(), Map.Region.get(-32, 3, -35, -28, 5, -32)),
    FragileBridge("fragile_bridge", "碎碎桥", new FragileBridge(), Map.Region.get(-1, 8, -35, 3 ,10, -37)),
    ;

    public final String id;
    public final String name;
    public final Map map;
    public final Map.Region lobby;

    MapInfo(String id, String name, Map map, Map.Region lobby) {
        this.id = id;
        this.name = name;
        this.map = map;
        this.lobby = lobby;
    }

    public static void registerMaps() {
        for (MapInfo map : MapInfo.values()) {
            Lobby.regions.put("map."+map.id,
                    new LobbyRegion(
                            LobbyRegion.Type.GAME,
                            map.id,
                            map.lobby.pos1(),
                            map.lobby.pos2(),
                            true)
            );
        }
    }

    public static MapInfo getMap(String id) {
        for (MapInfo map : MapInfo.values()) {
            if (map.id.equals(id)) {
                return map;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
