package cn.plumc.ultimatech;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.game.map.MapInfo;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.provider.EffectProvider;
import cn.plumc.ultimatech.provider.FallingBlockPreventProvider;
import cn.plumc.ultimatech.provider.offset.OffsetProvider;
import cn.plumc.ultimatech.provider.WallJumpProvider;
import cn.plumc.ultimatech.section.SectionRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;


@Mod(UltimateCH.MOD_ID)
public class UltimateCH {
    public static UltimateCH INSTANCE;
    public static final String MOD_ID = "ultimatech";
    public static final String CONTENT_ID = "uch";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltimateCH(IEventBus modEventBus, ModContainer modContainer) {
        INSTANCE = this;
    }

    public void initialize(MinecraftServer server) {
        if (UCHInfos.DEBUG) server.getPlayerList().op(server.getProfileCache().get("Dev").get());
        new SectionRegistry();
        new OffsetProvider(server);
        MapInfo.registerMaps();
        new FallingBlockPreventProvider().load();
    }

    public void tick(){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Lobby.tick(server);
        WallJumpProvider.tick(server.getPlayerList().getPlayers(), server.overworld());
        OffsetProvider.INSTANCE.tick();
        for (Game game : Lobby.games.values()) {
            try {
                game.tick();
            } catch (Exception e) {
                for (ServerPlayer player : game.getPlayerManager().getPlayers()) {
                    if (player.hasPermissions(4))
                        player.sendSystemMessage(Component.literal("游戏发生错误: %s".formatted(e.getMessage())));
                }
                LOGGER.error("ERROR During gaming: ", e);
                game.gameEnd(null);
            }
        }
    }

    public void second(){
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        EffectProvider.second(server.getPlayerList().getPlayers());
        for (Game game : Lobby.games.values()) {
            try {
                game.second();
            } catch (Exception e) {
                for (ServerPlayer player : game.getPlayerManager().getPlayers()) {
                    if (player.hasPermissions(4))
                        player.sendSystemMessage(Component.literal("游戏发生错误: %s".formatted(e.getMessage())));
                }
                LOGGER.error("ERROR During gaming: ", e);
                game.gameEnd(null);
            }
        }
    }
}
