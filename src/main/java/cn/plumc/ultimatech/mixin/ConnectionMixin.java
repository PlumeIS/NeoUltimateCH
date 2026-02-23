package cn.plumc.ultimatech.mixin;

import cn.plumc.ultimatech.Lobby;
import cn.plumc.ultimatech.commands.DevelopCommands;
import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.info.StatusTags;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.provider.WallJumpProvider;
import cn.plumc.ultimatech.utils.ContainerUtil;
import cn.plumc.ultimatech.utils.TickUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.RejectedExecutionException;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Shadow private Channel channel;

    @Shadow
    private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {}

    @Shadow public abstract void disconnect(Component pMessage);

    @Shadow @Final private static Logger LOGGER;

    @Shadow private int receivedPackets;

    @Shadow private PacketListener packetListener;

    @Inject(method = "send*", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci){
        ServerPlayer serverPlayer = ultimateCH$getPlayer();
        logPacket(packet,"OUT:{}");
        if (packet instanceof ClientboundSetTimePacket && serverPlayer.getTags().contains(StatusTags.SKIP_TIME_SYNC_TAG)){
            ci.cancel();
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundMovePlayerPacket){
            ServerPlayer serverPlayer = ultimateCH$getPlayer();
            if (serverPlayer == null){return;}
            if (WallJumpProvider.isTouchingPlayer(serverPlayer)) {
                ci.cancel();
                if (this.channel.isOpen()) {
                    try {
                        ServerboundMovePlayerPacket movePlayerPacket = null;
                        if (packet instanceof ServerboundMovePlayerPacket.Pos p) {
                            movePlayerPacket = new ServerboundMovePlayerPacket.Pos(p.getX(0), p.getY(0), p.getZ(0), false);
                        }
                        if (packet instanceof ServerboundMovePlayerPacket.Rot p){
                            movePlayerPacket = new ServerboundMovePlayerPacket.Rot(p.getYRot(0), p.getXRot(0), false);
                        }
                        if (packet instanceof ServerboundMovePlayerPacket.PosRot p){
                            movePlayerPacket = new ServerboundMovePlayerPacket.PosRot(p.getX(0), p.getY(0), p.getZ(0), p.getYRot(0), p.getXRot(0), false);
                        }
                        if (packet instanceof ServerboundMovePlayerPacket.StatusOnly p){
                            movePlayerPacket = new ServerboundMovePlayerPacket.StatusOnly(false);
                        }
                        genericsFtw(movePlayerPacket, this.packetListener);
                    } catch (RunningOnDifferentThreadException runningondifferentthreadexception) {
                    } catch (RejectedExecutionException rejectedexecutionexception) {
                        this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
                    } catch (ClassCastException classcastexception) {
                        LOGGER.error("Received {} that couldn't be processed", packet.getClass(), classcastexception);
                        this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
                    }

                    ++this.receivedPackets;
                }
            }
        }

        if (packet instanceof ServerboundUseItemPacket){
            TickUtil.tickRun(()->{
                ServerPlayer player = ultimateCH$getPlayer();
                if (player == null){return;}
                for (Game game : Lobby.games.values()) {
                    TickUtil.tickRun(()-> {
                        if (game.getPlayerManager().getPlayers().contains(player)) {
                            game.getPlayerManager().onPlayerUseItem(player, player.getInventory().getSelected());
                        }
                        DevelopCommands.onPlayerUseItem(player, player.getInventory().getSelected());
                    });
                }
            });
        }

        if (packet instanceof ServerboundContainerClickPacket clickPacket){
            for (Game game : Lobby.games.values()) {
                Integer container = game.getStatus().playerSectionBoxIds.get(ultimateCH$getPlayer().getUUID());
                System.out.println(container);
                if (container != null && container == clickPacket.getContainerId()){
                    ItemStack item = clickPacket.getCarriedItem();
                    System.out.println(item);
                    if (!item.isEmpty()) TickUtil.tickRun(()-> {
                        game.getStatusSignal().onSectionPicked(ultimateCH$getPlayer(), item);
                        ultimateCH$getPlayer().containerMenu.broadcastFullState();
                    });

                }
                ContainerUtil.broadcastUpdate(game.getPlayerManager().getPlayers(), clickPacket.getContainerId(), clickPacket.getSlotNum(), ItemStack.EMPTY);
            }
        }

        logPacket(packet,"IN:{}");

        if (packet instanceof ServerboundContainerClosePacket closePacket){
            for (Game game : Lobby.games.values()) {
                Integer container = game.getStatus().playerSectionBoxIds.get(ultimateCH$getPlayer().getUUID());
                if (container != null && container == closePacket.getContainerId()){
                    TickUtil.tickRun(()-> game.getStatusSignal().onSectionClose(ultimateCH$getPlayer()));
                }
            }
        }
    }

    @Unique
    private ServerPlayer ultimateCH$getPlayer() {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player.connection.connection == (Connection)(Object)this) return player;
        }
        return null;
    }

    private void logPacket(Packet<?> packet, String io){
        if (!UCHInfos.DEBUG) return;
        if (packet instanceof ServerboundMovePlayerPacket || packet instanceof ClientboundMoveEntityPacket) return;
        if (packet instanceof ServerboundKeepAlivePacket) return;
        if (packet instanceof ServerboundChunkBatchReceivedPacket) return;
        LOGGER.info(io, packet.getClass().getSimpleName());
    }
}
