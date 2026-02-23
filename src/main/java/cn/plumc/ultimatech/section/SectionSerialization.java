package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.info.UCHInfos;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class SectionSerialization {
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void save(MapInfo mapInfo, List<Section> sections) throws IOException {
        String holderId = UUID.nameUUIDFromBytes(
                UCHInfos.MAP_HOLDER.getBytes(StandardCharsets.UTF_8)
        ).toString();

        Path jsonPath = UCHInfos.UCH_PATCH.resolve("mapSection_" + mapInfo.id + ".json");
        JsonArray array = new JsonArray();

        for (Section section : sections) {
            JsonObject serialized = serialize(section, holderId);
            array.add(serialized);
        }

        try (Writer fileWriter = new OutputStreamWriter(
                new FileOutputStream(jsonPath.toFile()), StandardCharsets.UTF_8);
             JsonWriter jsonWriter = new JsonWriter(fileWriter)) {
            gson.toJson(array, jsonWriter);
        } catch (JsonIOException e) {
            throw new IOException("Failed to write JSON to file: " + jsonPath, e);
        }
    }

    public static void load(MapInfo mapInfo, Game game) throws IOException {
        String resource = "mapSection_" + mapInfo.id + ".json";
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
            }
        }

        try (Reader fileReader = new InputStreamReader(
                new FileInputStream(jsonPath.toFile()), StandardCharsets.UTF_8)) {
            JsonArray objects = JsonParser.parseReader(fileReader).getAsJsonArray();
            for (JsonElement element : objects) {
                JsonObject object = element.getAsJsonObject();
                deserialize(object, game);
            }
        } catch (FileNotFoundException e) {
            throw new IOException("Map section file not found: " + jsonPath, e);
        } catch (JsonParseException | NullPointerException e) {
            throw new IOException("Failed to parse JSON from file: " + jsonPath, e);
        }
    }

    public static JsonObject serialize(Section section, @Nullable String owner) {
        JsonObject object = new JsonObject();
        SectionRegistry.SectionInfo info = SectionRegistry.instance.getSectionInfo(section.getClass());

        object.addProperty("type", info.id());
        object.addProperty("owner", Objects.nonNull(owner) ? owner : section.owner.getUUID().toString());

        JsonObject origin = new JsonObject();
        Vec3 originVec = section.content.origin;
        origin.addProperty("x", originVec.x);
        origin.addProperty("y", originVec.y);
        origin.addProperty("z", originVec.z);
        object.add("origin",  origin);

        JsonObject rotation = new JsonObject();
        rotation.addProperty("x", section.rotation.getX());
        rotation.addProperty("y", section.rotation.getY());
        rotation.addProperty("z", section.rotation.getZ());
        object.add("rotation", rotation);

        return object;
    }

    public static void deserialize(JsonObject object, Game game) {
        String id = object.get("type").getAsString();

        Section section = game.getSectionManager().builder.build(id);
        game.getSectionManager().addSection(section);

        JsonObject origin = object.getAsJsonObject("origin");
        Vec3 sectionOrigin = new Vec3(origin.get("x").getAsDouble(), origin.get("y").getAsDouble(), origin.get("z").getAsDouble());
        section.content.setMapOrigin(sectionOrigin);

        JsonObject rotation = object.getAsJsonObject("rotation");
        section.rotation.set(SectionRotation.Axis.X, rotation.get("x").getAsDouble());
        section.rotation.set(SectionRotation.Axis.Y, rotation.get("y").getAsDouble());
        section.rotation.set(SectionRotation.Axis.Z, rotation.get("z").getAsDouble());

        section.view();
        section.tick();
        section.place();
    }
}
