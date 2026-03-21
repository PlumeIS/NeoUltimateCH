package cn.plumc.ultimatech.section.layer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Layer {
    public final LayerType type;
    protected final Map<Long, LayerBlock> blocks;
    protected final Set<Long> dirty;
    protected Layer upper;
    protected Layer lower;

    public Layer(LayerType type) {
        this.type = type;

        this.blocks = new HashMap<>(1024, 0.75f);
        this.dirty = new HashSet<>(256, 0.75f);
    }


    protected static long key(BlockPos pos) {
        return pos.asLong();
    }

    private static long key(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }


    public void set(BlockPos pos, BlockState state, BlockEntity entity) {
        getLowest().read(pos);
        long k = key(pos);

        LayerBlock block = blocks.get(k);
        if (block == null) {
            blocks.put(k, new LayerBlock(state, entity));
        } else {
            block.set(state, entity);
        }

        dirty.add(k);
        Layer layer = this;
        while (layer.upper != null) {
            layer = layer.upper;
            if (layer.has(k)) layer.dirty.add(k);
        }
    }

    public void set(BlockPos pos, BlockState state) {
        set(pos, state, null);
    }

    public void set(BlockPos pos, LayerBlock block) {
        set(pos, block.state, block.entity);
    }


    public LayerBlock remove(BlockPos pos) {
        long k = key(pos);
        return remove(k);
    }

    public LayerBlock remove(long k) {
        LayerBlock removed;
        if ((removed = blocks.remove(k)) != null) {
            dirty.add(k);
        }
        Layer layer = this;
        while (layer.lower != null) {
            layer = layer.lower;
            if (layer.has(k)) layer.dirty.add(k);
        }
        return removed;
    }

    public boolean has(BlockPos pos) {
        return blocks.containsKey(key(pos));
    }

    public boolean has(long k) {
        return blocks.containsKey(k);
    }


    public LayerBlock get(BlockPos pos) {
        if (has(pos)) return blocks.get(key(pos));
        return lower.get(pos);
    }


    public Map<Long, LayerBlock> apply() {
        Layer layer = getLowest();
        Map<Long, LayerBlock> result = new HashMap<>();
        while (layer != this) {
            for (long k : layer.dirty) {
                LayerBlock block = getByKey(k);
                if (block != null)
                    result.put(k, block);
            }
            layer.dirty.clear();
            layer = layer.upper;
        }
        return result;
    }

    private LayerBlock getByKey(long k) {
        Layer layer = this;
        while (layer != null) {
            LayerBlock block = layer.blocks.get(k);
            if (block != null) return block;
            layer = layer.lower;
        }
        return null;
    }

    public Map<Long, LayerBlock> reset() {
        return getLowest().blocks;
    }

    public Layer getUpper() {
        return upper;
    }

    public void setUpper(Layer upper) {
        this.upper = upper;
    }

    public Layer getLower() {
        return lower;
    }

    public void setLower(Layer lower) {
        this.lower = lower;
    }

    public WorldLayer getLowest() {
        Layer layer = this;
        while (layer.lower != null)
            layer = layer.lower;
        return (WorldLayer) layer;
    }

    public Layer getUppest() {
        Layer layer = this;
        while (layer.upper != null)
            layer = layer.upper;
        return layer;
    }


    public void removeAll() {
        new HashSet<>(blocks.keySet()).forEach(this::remove);
    }

    public int size() {
        return blocks.size();
    }
}