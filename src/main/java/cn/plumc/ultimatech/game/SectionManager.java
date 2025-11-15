package cn.plumc.ultimatech.game;

import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionBuilder;
import cn.plumc.ultimatech.sections.Coin;
import cn.plumc.ultimatech.sections.base.Alternative;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class SectionManager {
    public List<Section> sections = new ArrayList<>();
    public SectionBuilder builder;
    private Game game;
    private GameStatus status;

    public SectionManager(Game game, GameStatus status) {
        this.game = game;
        this.status = status;
        this.builder = new SectionBuilder(game);
    }

    public Section buildSection(String id, ServerPlayer player) {
        Section section;
        addSection(section = builder.build(id, player));
        return section;
    }

    public void viewRoundSection(ServerPlayer player) {
        status.roundSections.get(player).view();
    }

    public void addSection(Section section) {
        sections.add(section);
    }

    public void removeSection(Section section) {
        sections.remove(section);
    }

    public void removeSection(int index){sections.remove(index);}

    public List<Section> getSections(){return sections;}

    public List<Section> getSectionsByPos(BlockPos pos) {
        List<Section> find = new ArrayList<>();
        for (Section section : sections) {
            if (section.content.containsBlock(pos)) find.add(section);
        }
        sortSections(find);
        return find;
    }

    public Tuple<BlockState, Optional<BlockEntity>> getRecoverBlockOrWorldBlock(BlockPos pos) {
        for (Section section : sections) {
            if (section.placed) continue;
            if (section.content.worldRecoverCache.containsKey(pos)){
                return section.content.worldRecoverCache.get(pos);
            }
        }
        BlockState worldBlockState = game.getLevel().getBlockState(pos);
        BlockEntity blockEntity = game.getLevel().getBlockEntity(pos);
        return new Tuple<>(worldBlockState, Objects.nonNull(blockEntity) ? Optional.of(blockEntity) : Optional.empty());
    }

    private void sortSections(List<Section> find) {
        find.sort((o1, o2) -> {
            if (o1 instanceof Alternative && o2 instanceof Alternative) {
                return 0;
            } else if (o1 instanceof Alternative) {
                return 1;
            } else if (o2 instanceof Alternative) {
                return -1;
            } else {
                return 0;
            }
        });
    }

    public int getPlayerCoin(ServerPlayer player){
        return Coin.getPlayerCoinCount(player);
    }

    public void sectionTick(){
        sections.forEach(Section::tick);
    }

    public void sectionSecond(){
    }

    public void announcePlace(Section placed){
        HashMap<BlockPos, BlockState> blocks = placed.content.blocks;
        for (Section section : sections) {
            if (section.placed) continue;
            for (Map.Entry<BlockPos, BlockState> block : blocks.entrySet()) {
                if (section.content.worldRecoverCache.containsKey(block.getKey())){
                    section.content.worldRecoverCache.put(block.getKey(), new Tuple<>(block.getValue(), Optional.empty()));
                }
            }
        }
    }

    public Section shouldPlayerLose(ServerPlayer player){
        for (Section section : sections) {
            if (section.killed.contains(player)) {
                section.killed.remove(player);
                return section;
            }
        }
        return null;
    }

    public void removeAll() {
        sortSections(sections);
        sections.forEach(Section::remove);
    }
}
