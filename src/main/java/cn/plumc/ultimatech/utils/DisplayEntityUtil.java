package cn.plumc.ultimatech.utils;

import cn.plumc.ultimatech.section.SectionCounter;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Entity;

public class DisplayEntityUtil {
    public static void setVisible(Entity entity, String origin ,boolean visible) {
        EntityDataAccessor accessor = new EntityDataAccessor(entity);
        CompoundTag data = accessor.getData();
        CompoundTag item = new CompoundTag();
        if (visible) item.put("id", StringTag.valueOf(origin));
        else item.put("id", StringTag.valueOf("minecraft:air"));
        data.put("item", item);
        try {
            accessor.setData(data);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setItem(Entity entity, String itemId){
        EntityDataAccessor accessor = new EntityDataAccessor(entity);
        CompoundTag data = accessor.getData();
        CompoundTag item = new CompoundTag();
        item.put("id", StringTag.valueOf(itemId));
        data.put("item", item);
        try {
            accessor.setData(data);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setScale(Entity entity, double scale){
        EntityDataAccessor accessor = new EntityDataAccessor(entity);
        CompoundTag data = accessor.getData();
        ListTag scales = new ListTag();
        scales.add(FloatTag.valueOf((float) scale));
        scales.add(FloatTag.valueOf((float) scale));
        scales.add(FloatTag.valueOf((float) scale));
        data.getCompound("transformation").put("scale", scales);
        try {
            accessor.setData(data);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setAnima(Entity entity, double duration){
        EntityDataAccessor accessor = new EntityDataAccessor(entity);
        CompoundTag data = accessor.getData();
        data.putInt("start_interpolation", 0);
        data.putInt("interpolation_duration", SectionCounter.toTicks(duration));
        try {
            accessor.setData(data);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
