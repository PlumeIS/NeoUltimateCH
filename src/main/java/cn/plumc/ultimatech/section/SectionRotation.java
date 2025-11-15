package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.utils.RotationUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

public class SectionRotation {
    private Section parent;

    public enum Axis{X, Y, Z}
    public enum RotationHandle{CLOCKWISE, COUNTERCLOCKWISE, NONE}

    private double x;
    private double y;
    private double z;

    private final HashMap<Axis, Double> rotations = new HashMap<>();
    private final HashMap<Axis, Integer> rotationStatus =  new HashMap<>();

    public SectionRotation(Section parent){
        this.parent = parent;
        rotations.put(Axis.X, 0.0);
        rotations.put(Axis.Y, 0.0);
        rotations.put(Axis.Z, 0.0);
        normalize();
    }

    public void set(Axis axis, double value){
        rotations.put(axis, value);
        normalize();
    }

    public void add(Axis axis, double value){
        rotations.put(axis, rotations.get(axis) + value);
        normalize();
    }

    private void normalize(){
        for(Axis axis : Axis.values()){
            while (rotations.get(axis) > 360){
                rotations.put(axis, rotations.get(axis) - 360);
            }
            switch (axis){
                case X: x = rotations.get(axis); break;
                case Y: y = rotations.get(axis); break;
                case Z: z = rotations.get(axis); break;
            }
        }
        for(Axis axis : Axis.values()){
            rotationStatus.put(axis, Mth.floor(rotations.get(axis)/UCHInfos.SECTION_ROTATION_DEGREE));
        }
    }

    public void rotate(Axis axis, RotationHandle handle){
        if (handle.equals(RotationHandle.CLOCKWISE)){
            rotations.put(axis, rotations.get(axis) - UCHInfos.SECTION_ROTATION_DEGREE);
        } else if (handle.equals(RotationHandle.COUNTERCLOCKWISE)) {
            rotations.put(axis, rotations.get(axis) + UCHInfos.SECTION_ROTATION_DEGREE);
        }

        normalize();
    }

    public Map<BlockPos, BlockState> rotated(Map<BlockPos, BlockState> blocks) {
        HashMap<BlockPos, BlockState> rotated = new HashMap<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockState state = entry.getValue();
            Map<Property<?>, Comparable<?>> properties = state.getValues();
            if (properties.containsKey(BlockStateProperties.AXIS)) state = rotatedAxis(state);
            if (properties.containsKey(BlockStateProperties.FACING)) state = rotatedFace(state);
            if (properties.containsKey(BlockStateProperties.HORIZONTAL_FACING)) state = rotatedFace(state);
            rotated.put(RotationUtil.rotateBlock(entry.getKey(), rotationStatus), state);
        }
        return rotated;
    }

    private BlockState rotatedAxis(BlockState state){
        Map<Property<?>, Comparable<?>> properties = state.getValues();
        if (!properties.containsKey(BlockStateProperties.AXIS)) return state;

        Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
        BlockPos testPos = switch (axis) {
            case X -> new BlockPos(1, 0, 0);
            case Y -> new BlockPos(0, 1, 0);
            case Z -> new BlockPos(0, 0, 1);
        };

        BlockPos rotatedTestPos = RotationUtil.rotateBlock(testPos, new Vec3(0.5, 0.5, 0.5), getRotationStatus());
        Direction.Axis rotatedAxis = Direction.Axis.X;
        if (rotatedTestPos.getZ()!=0) rotatedAxis = Direction.Axis.Z;
        else if (rotatedTestPos.getY()!=0) rotatedAxis = Direction.Axis.Y;
        return state.setValue(BlockStateProperties.AXIS, rotatedAxis);
    }

    private BlockState rotatedFace(BlockState state){
        Map<Property<?>, Comparable<?>> properties = state.getValues();
        if (!properties.containsKey(BlockStateProperties.FACING)) return state;

        HashMap<BlockPos, Direction> facings = new HashMap<>();
        facings.put(new BlockPos(0, 1, 0), Direction.UP);
        facings.put(new BlockPos(0, -1, 0), Direction.DOWN);
        facings.put(new BlockPos(0, 0, -1), Direction.NORTH);
        facings.put(new BlockPos(0, 0, 1), Direction.SOUTH);
        facings.put(new BlockPos(1, 0, 0), Direction.WEST);
        facings.put(new BlockPos(-1, 0, 0), Direction.EAST);

        Direction facing = state.getValue(BlockStateProperties.FACING);
        BlockPos testPos = new BlockPos(0, 0, -1);
        for (Map.Entry<BlockPos, Direction> entry : facings.entrySet()) {
            if (facing == entry.getValue()) testPos = entry.getKey();
            break;
        }

        BlockPos rotatedTestPos = RotationUtil.rotateBlock(testPos, new Vec3(0.5, 0.5, 0.5), getRotationStatus());
        Direction rotatedFacing = facings.get(rotatedTestPos);
        return state.setValue(BlockStateProperties.FACING, rotatedFacing);
    }

    public void rotated(SectionContent.SectionEntities entities) {
        for (Tuple<Vec3, Entity> entityEntry : entities.entities) {
            Vec3 defaultTranslation = entityEntry.getA();
            Entity entity = entityEntry.getB();

            EntityDataAccessor accessor = new EntityDataAccessor(entity);
            Quaternionf quaternion = RotationUtil.getRotation(rotations);
            CompoundTag data = accessor.getData();
            CompoundTag transformation = data.getCompound("transformation");
            ListTag left_rotation = new ListTag();
            left_rotation.add(FloatTag.valueOf(quaternion.x()));
            left_rotation.add(FloatTag.valueOf(quaternion.y()));
            left_rotation.add(FloatTag.valueOf(quaternion.z()));
            left_rotation.add(FloatTag.valueOf(quaternion.w()));
            transformation.put("left_rotation", left_rotation);

            Vec3 rotatedTranslation = RotationUtil.rotateVector(defaultTranslation, RotationUtil.getRotation(rotations));
            ListTag newTranslation = new ListTag();
            newTranslation.add(FloatTag.valueOf((float) rotatedTranslation.x));
            newTranslation.add(FloatTag.valueOf((float) rotatedTranslation.y));
            newTranslation.add(FloatTag.valueOf((float) rotatedTranslation.z));
            transformation.put("translation", newTranslation);

            data.put("transformation", transformation);
            try {
                accessor.setData(data);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public HashMap<Axis, Double> getRotations() {
        return rotations;
    }

    public HashMap<Axis, Integer> getRotationStatus() {
        return rotationStatus;
    }
}
