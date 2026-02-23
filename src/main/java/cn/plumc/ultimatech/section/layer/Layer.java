package cn.plumc.ultimatech.section.layer;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Layer {
    public LayerType type;
    public Layer upper;
    public Layer lower;
    public Map<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> blocks = new HashMap<>(){
        @Override
        public Tuple<BlockState, Optional<BlockEntity>> put(BlockPos key, Tuple<BlockState, Optional<BlockEntity>> value) {
            if (key == null || value == null) throw new NullPointerException();
            return super.put(key, value);
        }
    };
    public Layer(LayerType type){
        this.type = type;
    }

    public void set(BlockPos pos, Tuple<BlockState, Optional<BlockEntity>> block) {
        getLowest().get(pos);
        blocks.put(pos, block);
    }

    public void set(BlockPos pos, BlockState state){
        set(pos, new Tuple<>(state, Optional.empty()));
    }

    public Tuple<BlockState, Optional<BlockEntity>> get(BlockPos pos){
        if (has(pos)) return blocks.get(pos);
        else return lower.get(pos);
    }

    public Tuple<BlockState, Optional<BlockEntity>> remove(BlockPos pos){
        return blocks.remove(pos);
    }

    public void removeAll(){
        blocks.clear();
    }

    public boolean has(BlockPos pos){return blocks.containsKey(pos);}

    public Map<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> apply(){
        Layer layer = this;
        Set<BlockPos> changed = Sets.newHashSet();
        while(layer != null){
            changed.addAll(layer.blocks.keySet());
            layer = layer.lower;
        }
        Map<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> applied = new HashMap<>();
        for(BlockPos pos : changed){
            applied.put(pos, this.get(pos));
        }
        return applied;
    }

    public Map<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> reset(){
        return getLowest().blocks;
    }

    public void setUpper(Layer upper) {
        this.upper = upper;
    }

    public Layer getUppest(){
        Layer layer = this;
        while(layer.upper != null){
            layer = layer.upper;
        }
        return layer;
    }

    public Layer getLowest(){
        Layer layer = this;
        while(layer.lower != null){
            layer = layer.lower;
        }
        return layer;
    }

    public void setLower(Layer lower) {
        this.lower = lower;
    }
}
