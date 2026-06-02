package cn.plumc.ultimatech.provider.offset;

import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.SectionSerialization;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static cn.plumc.ultimatech.info.UCHInfos.OFFSETS_PATCH;

public class OffsetProvider {
    public static OffsetProvider INSTANCE;

    public static Vec3 TEST_START_POINT = new Vec3(195.7, 121.0, -186.5);
    public static int TEST_HEIGHT = 120;
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class OffsetTesting {
        public static final double BASE_OFFSET = -2.975;
        public static final double TARGET_SPEED = 1.8;

        private final long startTime;

        public final OffsetResult result;
        private final ServerPlayer player;
        private final SpeedProvider speedProvider;
        private final SlippingTest slippingTest;
        private final DropTest dropTest;

        private Shulker shulker;
        private ArmorStand stand;

        private boolean initialized = false;
        public boolean finished = false;
        public boolean cancel = false;

        public OffsetTesting(ServerPlayer player, int ping) {
            this.player = player;
            this.speedProvider = new SpeedProvider(player);
            this.result = new OffsetResult(ping);

            this.slippingTest = new SlippingTest(player, TARGET_SPEED);
            this.dropTest = new DropTest(player);
            this.startTime = System.currentTimeMillis();
        }

        public void initialize(MinecraftServer server){
            ServerLevel level = server.overworld();
            player.addTag(StatusTags.OFFSET_TESTING_TAG);
            player.addTag(StatusTags.NO_JUMP_TAG);
            PlayerUtil.teleport(player, TEST_START_POINT);
            if (initialized) return;

            shulker = new Shulker(EntityType.SHULKER, level);
            stand = new ArmorStand(EntityType.ARMOR_STAND, level);

            shulker.setPos(TEST_START_POINT);
            stand.setPos(TEST_START_POINT);

            shulker.setNoGravity(true);
            stand.setNoGravity(true);

            shulker.setNoAi(true);
            shulker.setInvulnerable(true);
            stand.setInvulnerable(true);

            level.addFreshEntity(shulker);
            level.addFreshEntity(stand);
            shulker.startRiding(stand);

            initialized = true;
        }

        public void tick() {
            if (finished) return;
            speedProvider.tick();
            setInventory();

            Vec3 position = player.position();
            if (position.y < TEST_START_POINT.y - TEST_HEIGHT) {
                PlayerUtil.teleport(player, TEST_START_POINT);
            }

            if (!slippingTest.checked) slippingTest.test(speedProvider, stand);
            else if (!dropTest.checked) dropTest.test(stand, slippingTest.offset);
            else end(false);
        }

        public void setInventory(){
            Inventory inventory = player.getInventory();
            inventory.clearContent();
            inventory.setItem(8, UCHInfos.OFFSET_TESTING_CANCEL);
        }

        public void end(boolean cancel){
            this.cancel = cancel;
            double time = (System.currentTimeMillis() - startTime) / 1000.0;
            if (!cancel) player.sendSystemMessage(Component.literal("测试已完成，用时：%.2fs".formatted(time)));
            else player.sendSystemMessage(Component.literal("测试已取消"));
            finished = true;
            shulker.kill();
            stand.kill();
            player.removeTag(StatusTags.OFFSET_TESTING_TAG);
            player.removeTag(StatusTags.NO_JUMP_TAG);
            PlayerUtil.teleport(player, UCHInfos.PLAYER_LOBBY_POINT);
            if (!cancel) {
                slippingTest.save(result);
                dropTest.save(result);
                INSTANCE.byLatency.put(result.ping, result);
                INSTANCE.byPlayer.put(player.getUUID(), result);
                INSTANCE.save(player.getUUID(), result);
                System.out.println(result);
            }
        }
    }

    public MinecraftServer server;
    public final HashMap<Integer, OffsetResult> byLatency = new HashMap<>(){
        @Override
        public OffsetResult getOrDefault(Object key, @Nullable OffsetResult defaultValue) {
            if (!(key instanceof Integer latency)) {
                return defaultValue;
            }

            if (isEmpty()) {
                return defaultValue;
            }

            Map.Entry<Integer, OffsetResult> closestEntry = null;
            int minDiff = Integer.MAX_VALUE;

            for (Map.Entry<Integer, OffsetResult> entry : entrySet()) {
                int diff = Math.abs(entry.getKey() - latency);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestEntry = entry;
                }
            }

            if (minDiff < 5) {
                return closestEntry.getValue();
            }

            return defaultValue;
        }
    };
    public final HashMap<UUID, OffsetResult> byPlayer = new HashMap<>();
    public final HashMap<UUID, OffsetTesting> testing = new HashMap<>();
    public OffsetResult defaultOffset;

    public OffsetProvider(MinecraftServer server){
        INSTANCE = this;
        this.server = server;
        load();
    }

    public void load() {
        Path defaultPath = OFFSETS_PATCH.resolve("default_offset.json");
        if (!defaultPath.toFile().exists()) {
            try (InputStream in = SectionSerialization.class
                    .getClassLoader()
                    .getResourceAsStream("default_offset.json")) {
                if (in != null) {
                    try (OutputStream out = new FileOutputStream(defaultPath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                } else {
                     System.err.println("Could not find default_offset.json in resources.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try (Stream<Path> paths = Files.walk(OFFSETS_PATCH)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".json"))
                 .forEach(path -> {
                     try (Reader reader = Files.newBufferedReader(path)) {
                         OffsetResult result = GSON.fromJson(reader, OffsetResult.class);
                         if (result != null) {
                             String fileName = path.getFileName().toString();
                             if (fileName.equals("default_offset.json")) {
                                 defaultOffset = result;
                             } else {
                                 UUID uuid = UUID.fromString(fileName.substring(0, fileName.lastIndexOf('.')));
                                 byPlayer.put(uuid, result);
                                 byLatency.put(result.ping, result);
                             }
                         }
                     } catch (Exception e) {
                         System.err.println("Failed to load offset result from " + path + ": " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            System.err.println("Failed to read offset directory: " + e.getMessage());
        }
    }

    public void save(UUID player, OffsetResult result) {
        Path savePatch = OFFSETS_PATCH.resolve("%s.json".formatted(player.toString()));
        
        try {
            try (Writer writer = Files.newBufferedWriter(savePatch)) {
                GSON.toJson(result, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save offset result for " + player + ": " + e.getMessage());
        }
    }

    public OffsetResult get(ServerPlayer player) {
        int latency = player.connection.latency();
        return byPlayer.getOrDefault(player.getUUID(),
                byLatency.getOrDefault(latency, defaultOffset));
    }
    
    public void testing(ServerPlayer player) {
        if (testing.containsKey(player.getUUID()) && !testing.get(player.getUUID()).finished) return;
        int ping = player.connection.latency();
        OffsetTesting test = new OffsetTesting(player, ping);
        test.initialize(server);
        testing.put(player.getUUID(), test);
    }

    public void interact(ServerPlayer player, ItemStack item) {
        OffsetTesting test = testing.get(player.getUUID());
        if (test == null || test.finished) return;
        if (item.isEmpty()) return;
        if (ItemStack.isSameItem(item, UCHInfos.OFFSET_TESTING_CANCEL)) testing.get(player.getUUID()).end(true);
    }

    public void tick(){
        testing.values().forEach(OffsetTesting::tick);
    }
}