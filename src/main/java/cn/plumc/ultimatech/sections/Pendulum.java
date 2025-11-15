package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.*;
import cn.plumc.ultimatech.section.hit.PendulumHit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;

public class Pendulum extends Section {
    public final PendulumPhysical pendulum = new PendulumPhysical(5.0, 9.81, Math.toRadians(90), 0.0);
    public PendulumHit hit;

    public Pendulum(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(2, 5).add(0, 0, 2), game);
        setProcess(SectionCounter.toTicks(5.3));
    }

    @Override
    public void init() {
        hit = new PendulumHit(this);
    }

    @Override
    public void tickRun(int tickTime) {
        pendulum.step(0.05);
        double theta = pendulum.getTheta();

        List<Tuple<Vec3, Entity>> entities = content.entities.entities;

        HashMap<SectionRotation.Axis, Double> rotations = MotionTransform.createZeroRotationMap();
        rotations.put(SectionRotation.Axis.X, Math.toDegrees(theta));
        Vec3 center = content.getRelativeEntityPosition("uch.pendulum.frame");
        for (Tuple<Vec3, Entity> entityEntry : entities) {
            Vec3 pos = entityEntry.getA();
            Entity entity = entityEntry.getB();
            if (entity.getTags().contains("uch.pendulum.frame")) continue;

            double r = pos.distanceTo(center);
            double dy = r*Math.cos(theta);
            double dx = r*Math.sin(theta);

            transform.applyEntityRotation(entity, rotations, 0.05);
            transform.moveEntityAbsolute(entity, center.add(new Vec3(0, -dy, -dx)), 0.05);
        }

        killAll(hit.detectPlayers(game));
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        pendulum.reset();
    }

    public static class PendulumPhysical {
        private final double L;       // 摆长 (m)
        private final double g;       // 重力加速度 (m/s^2)
        private final double theta0;  // 初始角度 (rad)
        private final double omega0;  // 初始角速度 (rad/s)

        private double theta;         // 当前角度
        private double omega;         // 当前角速度

        public PendulumPhysical(double L, double g, double theta0, double omega0) {
            this.L = L;
            this.g = g;
            this.theta0 = theta0;
            this.omega0 = omega0;
            reset();
        }
        /** 重置为初始状态 */
        public void reset() {
            this.theta = theta0;
            this.omega = omega0;
        }

        /** 单步积分（dt 秒） */
        public void step(double dt) {
            double k1_theta = omega;
            double k1_omega = - (g / L) * Math.sin(theta);

            double k2_theta = omega + 0.5 * dt * k1_omega;
            double k2_omega = - (g / L) * Math.sin(theta + 0.5 * dt * k1_theta);

            double k3_theta = omega + 0.5 * dt * k2_omega;
            double k3_omega = - (g / L) * Math.sin(theta + 0.5 * dt * k2_theta);

            double k4_theta = omega + dt * k3_omega;
            double k4_omega = - (g / L) * Math.sin(theta + dt * k3_theta);

            theta += (dt / 6.0) * (k1_theta + 2*k2_theta + 2*k3_theta + k4_theta);
            omega += (dt / 6.0) * (k1_omega + 2*k2_omega + 2*k3_omega + k4_omega);
        }

        /** 获取当前角度 θ(t) */
        public double getTheta() {
            return theta;
        }

        /** X 坐标 */
        public double getX() {
            return L * Math.sin(theta);
        }

        /** Y 坐标 */
        public double getY() {
            return -L * Math.cos(theta);
        }
    }
}
