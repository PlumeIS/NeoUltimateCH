package cn.plumc.ultimatech.provider.offset;

import cn.plumc.ultimatech.utils.PlayerUtil;
import cn.plumc.ultimatech.utils.TickUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

import static cn.plumc.ultimatech.provider.offset.OffsetProvider.OffsetTesting.BASE_OFFSET;
import static cn.plumc.ultimatech.provider.offset.OffsetProvider.TEST_START_POINT;

public class DropTest {
    private enum TestSpeed {
        POINT1(0.8),
        POINT2(1.5),
        POINT3(3.0),
        POINT4(5.0),
        POINT5(8.0),
        POINT6(12.0),
        POINT7(18.0),
        POINT8(25.0),
        POINT9(30.0);

        public final double speed;

        TestSpeed(double speed) {
            this.speed = speed;
        }

        public static TestSpeed getDefault() {return POINT1;}

        public TestSpeed next() {
            return switch (this) {
                case POINT1 -> POINT2;
                case POINT2 -> POINT3;
                case POINT3 -> POINT4;
                case POINT4 -> POINT5;
                case POINT5 -> POINT6;
                case POINT6 -> POINT7;
                case POINT7 -> POINT8;
                case POINT8 -> POINT9;
                default -> null;
            };
        }
    }

    private final ServerPlayer player;

    public TestSpeed testSpeed;
    public boolean testing = false;
    public boolean checking = false;
    public double checkedSpeed = 0;
    public boolean checkResult = false;
    public boolean checkFinished = false;
    public Double offset;

    public HashMap<Double, Double> result = new HashMap<>();
    public boolean checked = false;

    public DropTest(ServerPlayer player) {
        this.player = player;
        this.testSpeed = TestSpeed.POINT1;

    }

    public void save(OffsetResult result) {
        result.droppingOffset.putAll(this.result);
    }

    public void test(ArmorStand stand, double slippingOffset){
        if (checked) return;
        if (offset==null) offset = slippingOffset;
        Vec3 deltaMovement = player.getDeltaMovement();
        double speed = -deltaMovement.y * 20;
        MutableComponent actionBar = Component.literal("偏移量: %.3f 目标速度: %.2f".formatted(offset, testSpeed.speed));
        player.connection.send(new ClientboundSetActionBarTextPacket(actionBar));

        if (!testing) {
            Vec3 waitingPos = TEST_START_POINT.add(0, -100, 0);
            stand.teleportTo(waitingPos.x, waitingPos.y, waitingPos.z);
            PlayerUtil.teleport(player, TEST_START_POINT);
            TickUtil.tickRun(() -> player.setDeltaMovement(0, testSpeed.speed/20 - 0.1, 0));
            testing = true;
        } else {
            if (!checking && speed > testSpeed.speed) {
                checkedSpeed = speed;
                Vec3 position = player.position();
                Vec3 offsetPos = position.add(0, BASE_OFFSET + this.offset, 0);
                stand.teleportTo(offsetPos.x, offsetPos.y, offsetPos.z);
                checking = true;
                checkFinished = false;
                TickUtil.runAfterTick(() -> {
                    checkResult = !((position.y - player.position().y + this.offset) > 1);
                    checkFinished = true;
                }, 1);
            }
            if (checking && checkFinished) {
                if (checkResult) {
                    player.sendSystemMessage(Component.literal("在速度 %.2f 下完成测试".formatted(testSpeed.speed)));
                    result.put(checkedSpeed, offset);
                    testSpeed = testSpeed.next();
                    testing = false;
                    checking = false;
                    checkFinished = false;
                } else {
                    player.sendSystemMessage(Component.literal("测试失败"));
                    offset -= 0.01;
                    testing = false;
                    checking = false;
                    checkFinished = false;
                }
            }
        }
        if (testSpeed == null) {
            checked = true;
        }
    }
}
