package cn.plumc.ultimatech.utils;

import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.SectionRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;

public class RotationUtil {
    public static Vec3 rotatePoint(Vec3 point, HashMap<SectionRotation.Axis, Double> angles) {
        return rotatePoint(point, new Vec3(0, 0, 0), angles);
    }

    public static Vec3 rotatePoint(Vec3 point, Vec3 origin, HashMap<SectionRotation.Axis, Double> angles) {
        double x = point.x() - origin.x();
        double y = point.y() - origin.y();
        double z = point.z() - origin.z();

        double[] roatedPoint = {x, y, z};

        roatedPoint = rotateX(roatedPoint, angles.get(SectionRotation.Axis.X));
        roatedPoint = rotateY(roatedPoint, angles.get(SectionRotation.Axis.Y));
        roatedPoint = rotateZ(roatedPoint, angles.get(SectionRotation.Axis.Z));

        double newX = roatedPoint[0] + origin.x();
        double newY = roatedPoint[1] + origin.y();
        double newZ = roatedPoint[2] + origin.z();

        return new Vec3(newX, newY, newZ);
    }

    public static BlockPos rotateBlock(BlockPos pos, HashMap<SectionRotation.Axis, Integer> axis) {
        return rotateBlock(pos, new Vec3(0, 0, 0), axis);
    }

    public static BlockPos rotateBlock(BlockPos pos, Vec3 origin, HashMap<SectionRotation.Axis, Integer> axis) {
        double x = pos.getX() + 0.5 - origin.x();
        double y = pos.getY() + 0.5 - origin.y();
        double z = pos.getZ() + 0.5 - origin.z();

        double[] point = {x, y, z};

        point = rotateX(point, axis.get(SectionRotation.Axis.X)*UCHInfos.SECTION_ROTATION_DEGREE);
        point = rotateY(point, axis.get(SectionRotation.Axis.Y)*UCHInfos.SECTION_ROTATION_DEGREE);
        point = rotateZ(point, axis.get(SectionRotation.Axis.Z)*UCHInfos.SECTION_ROTATION_DEGREE);

        int newX = Mth.floor(point[0] + origin.x());
        int newY = Mth.floor(point[1] + origin.y());
        int newZ = Mth.floor(point[2] + origin.z());

        return new BlockPos(newX, newY, newZ);
    }

    private static double[] rotateX(double[] point, double rotation) {
        double angle = Math.toRadians(rotation);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double y = point[1] * cos - point[2] * sin;
        double z = point[1] * sin + point[2] * cos;

        return new double[]{point[0], y, z};
    }

    private static double[] rotateY(double[] point, double rotation) {
        double angle = Math.toRadians(rotation);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = point[0] * cos + point[2] * sin;
        double z = -point[0] * sin + point[2] * cos;

        return new double[]{x, point[1], z};
    }

    private static double[] rotateZ(double[] point, double rotation) {
        double angle = Math.toRadians(rotation);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = point[0] * cos - point[1] * sin;
        double y = point[0] * sin + point[1] * cos;

        return new double[]{x, y, point[2]};
    }

    public static Quaternionf getRotation(HashMap<SectionRotation.Axis, Double> angles) {
        return toQuaternion(
                angles.get(SectionRotation.Axis.Y),
                angles.get(SectionRotation.Axis.Z),
                angles.get(SectionRotation.Axis.X));
    }

    public static Quaternionf getMCRotation(HashMap<SectionRotation.Axis, Double> angles) {
        return toQuaternion(
                angles.get(SectionRotation.Axis.X),
                angles.get(SectionRotation.Axis.Y),
                angles.get(SectionRotation.Axis.Z));
    }

    public static Quaternionf toQuaternion(double pitch, double yaw, double roll) {
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw);
        float rollRad = (float) Math.toRadians(roll);

        float cy = (float) Math.cos(yawRad * 0.5F);
        float sy = (float) Math.sin(yawRad * 0.5F);
        float cp = (float) Math.cos(pitchRad * 0.5F);
        float sp = (float) Math.sin(pitchRad * 0.5F);
        float cr = (float) Math.cos(rollRad * 0.5F);
        float sr = (float) Math.sin(rollRad * 0.5F);

        float w = cr * cp * cy + sr * sp * sy;
        float x = sr * cp * cy - cr * sp * sy;
        float y = cr * sp * cy + sr * cp * sy;
        float z = cr * cp * sy - sr * sp * cy;

        return new Quaternionf(x, y, z, w);
    }

    public static Vec3 rotateVector(Vec3 vector, Quaternionf rotation) {
        float x = rotation.x();
        float y = rotation.y();
        float z = rotation.z();
        float w = rotation.w();

        float xx = x * x;
        float xy = x * y;
        float xz = x * z;
        float xw = x * w;
        float yy = y * y;
        float yz = y * z;
        float yw = y * w;
        float zz = z * z;
        float zw = z * w;

        double newX = (1 - 2 * (yy + zz)) * vector.x() + 2 * (xy - zw) * vector.y() + 2 * (xz + yw) * vector.z();
        double newY = 2 * (xy + zw) * vector.x() + (1 - 2 * (xx + zz)) * vector.y() + 2 * (yz - xw) * vector.z();
        double newZ = 2 * (xz - yw) * vector.x() + 2 * (yz + xw) * vector.y() + (1 - 2 * (xx + yy)) * vector.z();

        return new Vec3(newX, newY, newZ);
    }
}
