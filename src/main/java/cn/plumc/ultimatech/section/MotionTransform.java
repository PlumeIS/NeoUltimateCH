package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.section.hit.LinearHit;
import cn.plumc.ultimatech.utils.RotationUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.*;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MotionTransform {
    public Section parent;

    public MotionTransform(Section parent) {
        this.parent = parent;
    }

    public Display.ItemDisplay createItemDisplay(String item, Vec3 position, String tag, @Nullable HashMap<SectionRotation.Axis, Double> rotations){
        Display.ItemDisplay entity = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, parent.level);
        EntityDataAccessor accessor = new EntityDataAccessor(entity);
        CompoundTag data = accessor.getData();
        if (Objects.nonNull(rotations)) {
            Quaternionf q = RotationUtil.getRotation(rotations);
            CompoundTag transformation = data.getCompound("transformation");
            transformation.put("right_rotation", toFloatListTag(q));
            data.put("transformation", transformation);
        }
        CompoundTag itemTag = new CompoundTag();
        itemTag.putString("id", item);
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(tag));
        data.put("Tags", tags);
        data.put("item", itemTag);
        safeSetData(accessor, data);

        parent.level.addFreshEntity(entity);
        entity.moveTo(parent.content.origin);
        SectionContent.SectionEntities entities = new SectionContent.SectionEntities();
        entities.add(position, entity);
        parent.rotation.rotated(entities);
        parent.content.entities.add(position, entity);
        return entity;
    }

    public Vec3 toNonNegative(Vec3 vec) {
        return new Vec3(Math.abs(vec.x), Math.abs(vec.y), Math.abs(vec.z));
    }

    public Vec3 rotateVector(Vec3 vec){
        return RotationUtil.rotateVector(vec, RotationUtil.getRotation(parent.rotation.getRotations()));
    }

    public Vec3 rotatePoint(Vec3 point){
        return RotationUtil.rotatePoint(point, parent.rotation.getRotations());
    }

    public Vec3 toAbsolute(Vec3 vec){
        return parent.content.origin.add(vec);
    }

    public Vec3 toRelative(Vec3 vec){
        return vec.subtract(parent.content.origin);
    }

    public void applyRotationToRelativeHit(BoxHit.Relative hit){
        hit.setPos(
                RotationUtil.rotatePoint(hit.pos1, parent.rotation.getRotations()),
                RotationUtil.rotatePoint(hit.pos2, parent.rotation.getRotations())
        );
    }

    public void applyRotationToLinearHit(LinearHit hit){
        hit.set(
                toAbsolute(RotationUtil.rotatePoint(toRelative(hit.start), parent.rotation.getRotations())),
                toAbsolute(RotationUtil.rotatePoint(toRelative(hit.end), parent.rotation.getRotations()))
        );
    }

    public void moveEntityRelative(Entity entity, Vec3 offset, double duration){
        Vec3 base = parent.content.entities.positions.get(entity.getUUID());
        Quaternionf rotation = RotationUtil.getRotation(parent.rotation.getRotations());
        Vec3 target = RotationUtil.rotateVector(base.add(offset), rotation);

        updateEntityTransformation(entity, t -> t.put("translation", toFloatListTag(target)), duration);
    }

    public void moveEntitySelfRelative(Entity entity, Vec3 offset, double duration){
        AtomicReference<Vec3> add = new AtomicReference<>();
        updateEntityTransformation(entity, t -> {
            ListTag translation = t.getList("translation", Tag.TAG_FLOAT);
            add.set(new Vec3(translation.getFloat(0), translation.getFloat(1), translation.getFloat(2)).add(offset));
        }, 0.0);
        Quaternionf rotation = RotationUtil.getRotation(parent.rotation.getRotations());
        Vec3 target = RotationUtil.rotateVector(add.get(), rotation);

        updateEntityTransformation(entity, t -> t.put("translation", toFloatListTag(target)), duration);
    }

    public void moveEntityAbsolute(Entity entity, Vec3 abs, double duration){
        Quaternionf rotation = RotationUtil.getRotation(parent.rotation.getRotations());
        Vec3 target = RotationUtil.rotateVector(abs, rotation);

        updateEntityTransformation(entity, t -> t.put("translation", toFloatListTag(target)), duration);
    }

    public void applyEntityRotation(Entity entity, HashMap<SectionRotation.Axis, Double> rotations, double duration){
        if (rotations == null) return;

        Quaternionf q = RotationUtil.getRotation(rotations);
        updateEntityTransformation(entity, t -> t.put("right_rotation", toFloatListTag(q)), duration);
    }

    public void applyEntityBaseRotation(Entity entity, HashMap<SectionRotation.Axis, Double> rotations, double duration){
        if (rotations == null) return;

        Quaternionf q = RotationUtil.getRotation(rotations);
        updateEntityTransformation(entity, t -> t.put("left_rotation", toFloatListTag(q)), duration);
    }

    public void applyEntityRotationWithCenter(Entity entity, HashMap<SectionRotation.Axis, Double> rotations, Vec3 offsetCenter , double duration){
        Vec3 offset = offsetCenter.scale(-1);
        applyEntityRotation(entity, rotations, duration);
        moveEntityRelative(entity, offset.subtract(RotationUtil.rotateVector(offset, RotationUtil.getRotation(rotations))), duration);
    }

    public void applyEntityScale(Entity entity, Vec3 scale, double duration){
        Quaternionf q = RotationUtil.getMCRotation(parent.rotation.getRotations());
        Vec3 rotatedScale = RotationUtil.rotateVector(scale, q);

        Vec3 absScale = new Vec3(
                Mth.abs((float) rotatedScale.x),
                Mth.abs((float) rotatedScale.y),
                Mth.abs((float) rotatedScale.z)
        );

        updateEntityTransformation(entity, t -> t.put("scale", toFloatListTag(absScale)), duration);
    }

    public static HashMap<SectionRotation.Axis, Double> createZeroRotationMap(){
        HashMap<SectionRotation.Axis, Double> rotations = new HashMap<>();
        rotations.put(SectionRotation.Axis.X, 0.0);
        rotations.put(SectionRotation.Axis.Y, 0.0);
        rotations.put(SectionRotation.Axis.Z, 0.0);
        return rotations;
    }

    public List<Vec3> generateOutlinePoints(double step) {
        Vec3 origin = parent.content.origin;
        SectionRegistry.SectionInfo info = SectionRegistry.instance.getSectionInfo(parent.getClass());
        Vec3 end = new Vec3(info.size().width(), info.size().height(), info.size().length());
        Vec3 rotatedEnd = RotationUtil.rotatePoint(origin.add(end), origin, parent.rotation.getRotations());
        AABB aabb = new AABB(origin, rotatedEnd);
        Vec3 min = aabb.getMinPosition();
        Vec3 max = aabb.getMaxPosition();

        Vec3[] vertices = new Vec3[]{
                new Vec3(min.x, min.y, min.z),
                new Vec3(max.x, min.y, min.z),
                new Vec3(min.x, max.y, min.z),
                new Vec3(max.x, max.y, min.z),
                new Vec3(min.x, min.y, max.z),
                new Vec3(max.x, min.y, max.z),
                new Vec3(min.x, max.y, max.z),
                new Vec3(max.x, max.y, max.z)
        };
        List<Vec3> points = new ArrayList<>(Arrays.asList(vertices));
        int[][] edges = new int[][]{
                {0, 1}, {0, 2}, {0, 4},
                {1, 3}, {1, 5},
                {2, 3}, {2, 6},
                {3, 7},
                {4, 5}, {4, 6},
                {5, 7},
                {6, 7}
        };
        for (int[] edge : edges) {
            Vec3 a = vertices[edge[0]];
            Vec3 b = vertices[edge[1]];
            Vec3 dir = b.subtract(a);
            double len = dir.length();
            Vec3 unit = dir.scale(1.0 / len);

            for (double t = 0; t < len; t += step) {
                Vec3 p = a.add(unit.scale(t));
                points.add(p);
            }
            points.add(b);
        }
        return points;
    }

    private void updateEntityTransformation(Entity entity, TransformationEditor editor, double duration) {
        EntityDataAccessor accessor = new EntityDataAccessor(entity);
        CompoundTag data = accessor.getData();
        CompoundTag transformation = data.getCompound("transformation");

        editor.edit(transformation);

        data.putInt("start_interpolation", 0);
        data.putInt("interpolation_duration", SectionCounter.toTicks(duration));
        data.put("transformation", transformation);

        safeSetData(accessor, data);
    }

    private static ListTag toFloatListTag(Vec3 vec) {
        ListTag list = new ListTag();
        list.add(FloatTag.valueOf((float) vec.x));
        list.add(FloatTag.valueOf((float) vec.y));
        list.add(FloatTag.valueOf((float) vec.z));
        return list;
    }

    private static ListTag toFloatListTag(Quaternionf q) {
        ListTag list = new ListTag();
        list.add(FloatTag.valueOf(q.x()));
        list.add(FloatTag.valueOf(q.y()));
        list.add(FloatTag.valueOf(q.z()));
        list.add(FloatTag.valueOf(q.w()));
        return list;
    }

    private void safeSetData(EntityDataAccessor accessor, CompoundTag data) {
        try {
            accessor.setData(data);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException("Failed to set entity NBT data", e);
        }
    }

    @FunctionalInterface
    private interface TransformationEditor {
        void edit(CompoundTag transformation);
    }
}
