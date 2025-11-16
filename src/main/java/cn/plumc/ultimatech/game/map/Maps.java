package cn.plumc.ultimatech.game.map;

import cn.plumc.ultimatech.game.map.maps.Map;
import cn.plumc.ultimatech.game.map.maps.Waterfall;
import net.minecraft.core.BlockPos;

public enum Maps {
    ROOF("roof", "房顶", new Map(
            new BlockPos(114578, -41, 19825),
            new BlockPos(114498, 29, 19794),
            new BlockPos(114515, 18, 19811),
            new BlockPos(114512, 21, 19808),
            new BlockPos(114564, 19, 19811),
            new BlockPos(114561, 22, 19808),
            -10,
            13000,
            2
            )),
    WATERFALL("waterfall", "瀑布", new Waterfall());
    ;

    public final String id;
    public final String name;
    public final Map map;

    Maps(String id, String name, Map map) {
        this.id = id;
        this.name = name;
        this.map = map;
    }

    public static Maps getMap(String id) {
        for (Maps map : Maps.values()) {
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
