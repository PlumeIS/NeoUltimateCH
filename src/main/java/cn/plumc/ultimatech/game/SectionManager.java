package cn.plumc.ultimatech.game;

import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionBuilder;
import cn.plumc.ultimatech.section.layer.Layer;
import cn.plumc.ultimatech.section.layer.LayerBlock;
import cn.plumc.ultimatech.section.layer.LayerType;
import cn.plumc.ultimatech.section.layer.WorldLayer;
import cn.plumc.ultimatech.sections.Coin;
import cn.plumc.ultimatech.sections.base.Alternative;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class SectionManager {
    private final Layer viewLayer;
    private final Layer topLayer;
    private final Layer middleLayer;
    private final WorldLayer worldLayer;
    public List<Section> sections = new ArrayList<>();
    public SectionBuilder builder;
    private final Game game;
    private final GameStatus status;


    public SectionManager(Game game, GameStatus status) {
        this.game = game;
        this.status = status;
        this.builder = new SectionBuilder(game);

        viewLayer = new Layer(LayerType.VIEW);
        topLayer = new Layer(LayerType.TOP);
        middleLayer = new Layer(LayerType.MIDDLE);
        worldLayer = new WorldLayer(game.getLevel());

        viewLayer.setLower(topLayer);
        topLayer.setUpper(viewLayer);
        topLayer.setLower(middleLayer);
        middleLayer.setUpper(topLayer);
        middleLayer.setLower(worldLayer);
        worldLayer.setUpper(middleLayer);
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

    public void removeSection(int index) {
        sections.remove(index);
    }

    public List<Section> getSections() {
        return sections;
    }

    public List<Section> getSectionsByPos(BlockPos pos, LayerType layer) {
        List<Section> find = new ArrayList<>();
        for (Section section : sections) {
            if (section.removed) continue;
            if (section.runningLayer != layer) continue;
            if (section.content.containsBlock(pos)) find.add(section);
        }
        sortSections(find);
        return find;
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

    public int getPlayerCoin(ServerPlayer player) {
        return Coin.getPlayerCoinCount(player);
    }

    public void sectionTick() {
        ImmutableList.copyOf(sections).forEach(Section::tick);
    }

    public void applyBlockLayer() {
        Map<Long, LayerBlock> changes = getViewLayer().apply();

        if (changes.isEmpty()) return;
        var level = game.getLevel();

        for (Map.Entry<Long, LayerBlock> entry : changes.entrySet()) {
            long key = entry.getKey();
            LayerBlock block = entry.getValue();
            BlockPos pos = BlockPos.of(key);
            if (block == null) {
                level.removeBlock(pos, false);
                continue;
            }
            level.setBlock(pos, block.state, 2);
            if (block.entity != null) {
                level.setBlockEntity(block.entity);
            }
        }
    }

    public void sectionSecond() {
    }

    public Section shouldPlayerLose(ServerPlayer player) {
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

    public void destroy() {
        removeAll();
        for (Section section : ImmutableList.copyOf(getSections())) {
            removeSection(section);
        }
        getViewLayer().removeAll();
        getTopLayer().removeAll();
        getMiddleLayer().removeAll();
        applyBlockLayer();
    }

    public Layer getLayer(LayerType layer) {
        return switch (layer) {
            case VIEW -> getViewLayer();
            case TOP -> getTopLayer();
            case MIDDLE -> getMiddleLayer();
            case BOTTOM -> getWorldLayer();
        };
    }

    public Layer getViewLayer() {
        return viewLayer;
    }

    public Layer getTopLayer() {
        return topLayer;
    }

    public Layer getMiddleLayer() {
        return middleLayer;
    }

    public WorldLayer getWorldLayer() {
        return worldLayer;
    }
}
