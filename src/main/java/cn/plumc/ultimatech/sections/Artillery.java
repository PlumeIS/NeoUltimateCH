package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class Artillery extends Section {
    public Vec3 motion;
    public Entity passenger;
    public BoxHit.Relative triggerHit;
    public HashMap<UUID, Long> passengerCooldown = new HashMap<>();

    public Artillery(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3, 2), game);
        setProcess(0);
    }

    @Override
    public void init() {
        motion = transform.rotateVector(new Vec3(0.0, 1.3, 3.0));
        triggerHit = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0.2, 0.0, 0.2), new Vec3(2.8, 2.8, 2.8));
        transform.applyRotationToRelativeHit(triggerHit);
    }

    @Override
    public void tickRun(int tickTime) {
        List<Entity> entities = triggerHit.detectEntities(level, entity -> {
            UUID uuid = entity.getUUID();
            return !passengerCooldown.containsKey(uuid)||
                    (System.currentTimeMillis()-passengerCooldown.get(uuid))>500L;
        });
        if (Objects.isNull(passenger) && !entities.isEmpty()) {
            passenger = entities.getFirst();
            playerOnly(player -> player.setGameMode(GameType.SPECTATOR));
            setProcess(SectionCounter.toTicks(1.0));
            process.start();
        }
        if (Objects.nonNull(passenger)) {
            if (!playerOnly(player -> PlayerUtil.teleport(player, triggerHit.getAABB().getCenter()))){
                passenger.setDeltaMovement(Vec3.ZERO);
                passenger.setPos(triggerHit.getAABB().getCenter());
                for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()){
                    serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(passenger));
                }
            }
            if (process.at(1.0)) {
                playerOnly(player -> player.setGameMode(GameType.ADVENTURE));
                passenger.setDeltaMovement(motion);
                for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()){
                    serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(passenger));
                }
                passengerCooldown.put(passenger.getUUID(), System.currentTimeMillis());
                passenger = null;
                setProcess(0);
                process.start();
            }
        }
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        passengerCooldown.clear();
    }

    private boolean playerOnly(Consumer<ServerPlayer> consumer) {
        if (passenger instanceof ServerPlayer player) {consumer.accept(player);return true;}
        return false;
    }
}
