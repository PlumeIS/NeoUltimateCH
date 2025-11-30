package cn.plumc.ultimatech.utils;

import cn.plumc.ultimatech.UltimateCH;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.hit.PlayerHit;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;

public class PlayerUtil {
    public static HashMap<UUID, PlayerTexture> textureCache = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static class PlayerTexture{
        public ServerPlayer getPlayer() {
            return player;
        }

        public boolean isCached() {
            return cached;
        }

        public static class WeightedSkinColorRandom {
            private final TreeMap<Integer, Integer> weightColorMap = new TreeMap<>();

            public WeightedSkinColorRandom(HashMap<Integer, Integer> colorMap) {
                int cumulativeWeight = 0;
                for (Map.Entry<Integer, Integer> entry : colorMap.entrySet()) {
                    cumulativeWeight += entry.getValue();
                    weightColorMap.put(cumulativeWeight, entry.getKey());
                }
            }

            public Integer getRandomColor() {
                Integer randomWeight = (int) (weightColorMap.lastKey() * Math.random());
                return weightColorMap.higherEntry(randomWeight).getValue();
            }
        }

        private boolean loaded = false;
        private boolean cached = false;

        private final ServerPlayer player;
        private final UUID playerUUID;
        private String username;

        private Path skin;

        private BufferedImage skinImage;
        private WeightedSkinColorRandom colorRandom;

        public static final String UUID_API = "https://api.mojang.com/users/profiles/minecraft/%s";
        public static final String TEXTURE_API = "https://sessionserver.mojang.com/session/minecraft/profile/%s";

        public static final HttpHelper httpHelper = new HttpHelper();
        public static final Gson gson = new Gson();

        public PlayerTexture(ServerPlayer player){
            this.username = player.getGameProfile().getName().toLowerCase();
            this.player = player;
            this.playerUUID = player.getUUID();

            new Thread(() -> {
                try {
                    load();
                    if (loaded) UltimateCH.LOGGER.info("[PlayerUtil] Loaded player ("+playerUUID+"): "+username);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        public int[] getSkinRGBA(int x, int y) {
            if (!loaded) return null;
            int argb = skinImage.getRGB(x, y);
            int alpha = (argb >> 24) & 0xFF;
            int red   = (argb >> 16) & 0xFF;
            int green = (argb >> 8) & 0xFF;
            int blue  = argb & 0xFF;
            return new int[]{red, green, blue, alpha};
        }

        public HashMap<Integer, Integer> calculateSkinColor(boolean ignoreTransparent){
            if (!loaded) return new HashMap<>();
            HashMap<Integer, Integer> colorMap = new HashMap<>();

            int width = skinImage.getWidth();
            int height = skinImage.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = skinImage.getRGB(x, y);

                    int alpha = (argb >> 24) & 0xFF;
                    if (ignoreTransparent && alpha < 255) continue;

                    colorMap.merge(argb, 1, Integer::sum);
                }
            }

            return colorMap;
        }

        public WeightedSkinColorRandom getColorRandom(){
            if (!loaded) return null;
            if (colorRandom != null) return colorRandom;
            this.colorRandom = new WeightedSkinColorRandom(calculateSkinColor(true));
            return colorRandom;
        }

        private void load() throws IOException {
            this.skin = UCHInfos.CACHED_SKIN_PATCH.resolve(username + ".png");
            if (Files.exists(skin)) {
                loadSkin();
                loaded = true;
                cached = true;
            } else {
                download();
                cached = false;
            }
        }

        private void download() throws IOException {
            // get uuid from mojang
            String uuidResponse = httpHelper.sendGet(String.format(UUID_API, username), null);
            JsonObject uuidJson = gson.fromJson(uuidResponse, JsonObject.class);
            String textureUUID;
            if (uuidJson.has("errorMessage")) {
                this.username = "uch_no_texture";
                textureUUID = UUID.nameUUIDFromBytes(username.getBytes()).toString();
                createNullSkin();
                load();
            }
            textureUUID = uuidJson.get("id").getAsString();

            // get texture from mojang
            String textureResponse = httpHelper.sendGet(String.format(TEXTURE_API, textureUUID), null);
            JsonObject textureJson = gson.fromJson(textureResponse, JsonObject.class);
            JsonArray properties = textureJson.getAsJsonArray("properties");
            String base64Texture = properties.get(0).getAsJsonObject().get("value").getAsString();
            Base64.Decoder decoder = Base64.getDecoder();
            String textureValue = new String(decoder.decode(base64Texture), StandardCharsets.UTF_8);
            JsonObject textureValueJson = gson.fromJson(textureValue, JsonObject.class);
            String url = textureValueJson.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();

            // download skin and save
            try (InputStream in = new URI(url).toURL().openStream()) {
                Files.copy(in, this.skin, StandardCopyOption.REPLACE_EXISTING);
                loadSkin();
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        private void loadSkin() throws IOException {
            skinImage = ImageIO.read(skin.toFile());
            loaded = true;
        }

        private void createNullSkin() throws IOException {
            if (Files.exists(UCHInfos.CACHED_SKIN_PATCH.resolve( "uch_no_texture.png"))) return;
            int width = 64;
            int height = 64;

            BufferedImage skin = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int black = new Color(0, 0, 0, 255).getRGB();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    skin.setRGB(x, y, black);
                }
            }

            ImageIO.write(skin, "png", UCHInfos.CACHED_SKIN_PATCH.resolve( "uch_no_texture.png").toFile());
        }

        public boolean isLoaded() {
            return loaded;
        }
    }

    public static void cachePlayerTexture(ServerPlayer player){
        textureCache.put(player.getUUID(), new PlayerTexture(player));
    }

    public static Vec3 getRandomPointInPlayerAABB(ServerPlayer player){
        AABB aabb = new PlayerHit(player).getAABB();
        double x = RANDOM.nextDouble() * (aabb.maxX - aabb.minX) + aabb.minX;
        double y = RANDOM.nextDouble() * (aabb.maxY - aabb.minY) + aabb.minY;
        double z = RANDOM.nextDouble() * (aabb.maxZ - aabb.minZ) + aabb.minZ;
        return new Vec3(x, y, z);
    }

    public static Vec3 getPlayerLooking(ServerPlayer player, int distance) {
        Vec3 eyePosition = player.getEyePosition();
        double playerX = eyePosition.x;
        double playerY = eyePosition.y;
        double playerZ = eyePosition.z;

        float playerPitch = player.getCamera().getXRot(); // 玩家的俯仰角
        float playerYaw = player.getCamera().getYRot(); // 玩家的偏航角

        double pitchRadians = Math.toRadians(playerPitch);
        double yawRadians = Math.toRadians(playerYaw);

        double directionX = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double directionY = -Math.sin(pitchRadians);
        double directionZ = Math.cos(yawRadians) * Math.cos(pitchRadians);

        double targetX = Math.floor(playerX + directionX * distance);
        double targetY = Math.floor(playerY + directionY * distance);
        double targetZ = Math.floor(playerZ + directionZ * distance);

        return new Vec3(targetX, targetY, targetZ);
    }


    public static void teleport(Player player, Vec3 location){
        player.teleportTo(location.x, location.y, location.z);
    }

    public static boolean containsTag(Player player, String tag){
        return player.getTags().contains(tag);
    }

    public static void updateDeltaMovement(List<ServerPlayer> players, Entity entity){
        players.forEach(serverPlayer -> serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(entity)));
    }

    public static ClientboundContainerSetSlotPacket createInventoryUpdatePacket(ServerPlayer player, int slot){
        return new ClientboundContainerSetSlotPacket(-2, 0, slot, player.getInventory().getItem(slot));
    }
}
