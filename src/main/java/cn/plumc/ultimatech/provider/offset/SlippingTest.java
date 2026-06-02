package cn.plumc.ultimatech.provider.offset;

import cn.plumc.ultimatech.utils.PlayerUtil;
import cn.plumc.ultimatech.utils.TickUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import static cn.plumc.ultimatech.provider.offset.OffsetProvider.OffsetTesting.BASE_OFFSET;
import static cn.plumc.ultimatech.provider.offset.OffsetProvider.TEST_START_POINT;

public class SlippingTest {
    private final ServerPlayer player;
    private final double targetSpeed;

    public boolean findingSpeed = true;
    public boolean checkingSpeed = false;
    public boolean checking = false;

    public double offset;
    public boolean checked = false;

    private double kp = 0.05; // 比例系数
    private double ki = 0.01; // 积分系数
    private double kd = 0.02; // 微分系数
    
    private double integral = 0;
    private double previousError = 0;
    
    public SlippingTest(ServerPlayer player, double targetSpeed) {
        this.player = player;
        this.targetSpeed = targetSpeed;
    }

    public void save(OffsetResult result) {
        result.slippingOffset = offset;
    }

    public void test(SpeedProvider speedProvider, ArmorStand stand){
        Vec3 position = player.position();

        Vec3 offsetPos = position.add(0, BASE_OFFSET + offset, 0);
        stand.teleportTo(offsetPos.x, offsetPos.y, offsetPos.z);

        MutableComponent actionBar = Component.literal("偏移量: %.3f 速度: %.2f".formatted(offset, speedProvider.getSmoothSpeed()));
        player.connection.send(new ClientboundSetActionBarTextPacket(actionBar));

        if (findingSpeed) {
            double currentSpeed = speedProvider.getCurrentSpeed();
            double error = targetSpeed - currentSpeed;

            if (currentSpeed < targetSpeed) offset = offset - 0.01;
            if (currentSpeed >= targetSpeed) {
                checkingSpeed = true;
                findingSpeed = false;

                integral = 0;
                previousError = error;
            }
        }

        if (checkingSpeed) {
            if (!checking) {
                PlayerUtil.teleport(player, TEST_START_POINT);
                speedProvider.reset();
                TickUtil.runAfterTick(() -> speedProvider.test(5), 1);
                checking = true;
            } else {
                if (speedProvider.finished()) {
                    Double testSpeed = speedProvider.getTestSpeed();
                    player.sendSystemMessage(Component.literal("测试速度: %.2f".formatted(testSpeed)));
                    
                    double error = testSpeed - targetSpeed;
                    
                    if (Math.abs(error) <= 0.03) {
                        player.sendSystemMessage(Component.literal("测试通过"));
                        checkingSpeed = false;
                        checked = true;
                    } else {
                        integral += error;

                        if (integral > 10) integral = 10;
                        if (integral < -10) integral = -10;
                        
                        double derivative = error - previousError;
                        double output = kp * error + ki * integral + kd * derivative;
                        offset += output;
                        
                        previousError = error;
                    }
                    checking = false;
                }
            }
        }
    }
}