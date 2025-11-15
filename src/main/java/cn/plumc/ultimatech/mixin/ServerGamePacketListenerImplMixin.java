package cn.plumc.ultimatech.mixin;

import cn.plumc.ultimatech.info.StatusTags;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.network.payload.ClientboundCustomSetTimePayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl{

    @Shadow public abstract ServerPlayer getPlayer();

    public ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie cookie) {
        super(server, connection, cookie);
    }

    @Override
    public void send(CustomPacketPayload payload) {
        ServerPlayer player = getPlayer();
        if (payload instanceof ClientboundCustomSetTimePayload && player.getTags().contains(StatusTags.SKIP_TIME_SYNC_TAG)) return;
        super.send(payload);
    }

    @Override
    public void send(Packet<?> packet) {
        ServerPlayer player = getPlayer();
        if (packet instanceof ClientboundSetTimePacket && player.getTags().contains(StatusTags.SKIP_TIME_SYNC_TAG)) return;
        super.send(packet);
    }
}
