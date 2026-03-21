package cn.plumc.ultimatech.provider;

import cn.plumc.ultimatech.game.map.Map;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.SectionSerialization;
import com.google.gson.*;
import net.minecraft.core.BlockPos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;

public class FallingBlockPreventProvider {
    public static FallingBlockPreventProvider instance;
    private boolean loaded = false;
    private final HashSet<Map.Region> preventRegions = new HashSet<>();

    public FallingBlockPreventProvider() {
        instance = this;
    }

    public void load() {
        String resource = "falling_prevents.json";
        Path jsonPath = UCHInfos.UCH_PATCH.resolve(resource);

        if (!jsonPath.toFile().exists()) {
            try (InputStream in = SectionSerialization.class
                    .getClassLoader()
                    .getResourceAsStream(resource);
                 OutputStream out = new FileOutputStream(jsonPath.toFile())) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (Reader fileReader = new InputStreamReader(
                new FileInputStream(jsonPath.toFile()), StandardCharsets.UTF_8)) {
            JsonArray objects = JsonParser.parseReader(fileReader).getAsJsonArray();
            for (JsonElement element : objects) {
                JsonObject object = element.getAsJsonObject();
                JsonArray array1 = object.getAsJsonArray("pos1");
                JsonArray array2 = object.getAsJsonArray("pos2");
                BlockPos pos1 = new BlockPos(array1.get(0).getAsInt(), array1.get(1).getAsInt(), array1.get(2).getAsInt());
                BlockPos pos2 = new BlockPos(array2.get(0).getAsInt(), array2.get(1).getAsInt(), array2.get(2).getAsInt());
                preventRegions.add(new Map.Region(pos1, pos2));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        loaded = true;
    }

    public boolean isPrevent(BlockPos pos){
        for(Map.Region region : preventRegions){
            if (region.inPos(pos)) return true;
        }
        return !loaded;
    }
}
