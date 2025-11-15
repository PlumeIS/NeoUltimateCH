package cn.plumc.ultimatech.section;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class SectionBox implements MenuProvider, Container {
    private List<ItemStack> items = new ArrayList<>();
    private List<SectionRegistry.SectionInfo> sections = new ArrayList<>();

    private static class WeightedSectionRandom {
        private final TreeMap<Double, SectionRegistry.SectionInfo> weightedSectionMap = new TreeMap<>();

        public WeightedSectionRandom(List<SectionRegistry.SectionInfo> sections) {
            double cumulativeWeight = 0;
            for (SectionRegistry.SectionInfo section : sections) {
                cumulativeWeight += section.weight();
                weightedSectionMap.put(cumulativeWeight, section);
            }
        }

        public SectionRegistry.SectionInfo getRandomSection() {
            double randomWeight = weightedSectionMap.lastKey() * Math.random();
            return weightedSectionMap.higherEntry(randomWeight).getValue();
        }
    }

    public SectionBox(int round, int playerCount){
        for(int i = 0; i < getContainerSize()+1; i++){
            items.add(ItemStack.EMPTY);
        }

        java.util.Map<String, SectionRegistry.SectionInfo> sectionInfos = SectionRegistry.instance.getSectionInfos();

        List<SectionRegistry.SectionInfo> easySections = new ArrayList<>();
        List<SectionRegistry.SectionInfo> normalSections = new ArrayList<>();
        List<SectionRegistry.SectionInfo> hardSections = new ArrayList<>();

        for (java.util.Map.Entry<String, SectionRegistry.SectionInfo> sectionEntry : sectionInfos.entrySet()) {
            SectionRegistry.SectionInfo info = sectionEntry.getValue();
            if (info.difficulty() == SectionRegistry.SectionDifficulty.EASY){
                easySections.add(info);
            } else if (info.difficulty() == SectionRegistry.SectionDifficulty.NORMAL){
                normalSections.add(info);
            } else if (info.difficulty() == SectionRegistry.SectionDifficulty.HARD){
                hardSections.add(info);
            }
        }

        WeightedSectionRandom easySectionRandom = new WeightedSectionRandom(easySections);
        WeightedSectionRandom normalSectionRandom = new WeightedSectionRandom(normalSections);
        WeightedSectionRandom hardSectionRandom = new WeightedSectionRandom(hardSections);

        double easyWeight;
        double normalWeight;
        double hardWeight;
        if (round <= 2){
            easyWeight = 0.8;
            normalWeight = 0.2;
            hardWeight = 0;
        } else if (round <= 6){
            easyWeight = 0.5;
            normalWeight = 0.3;
            hardWeight = 0.2;
        } else {
            easyWeight = 0.3;
            normalWeight = 0.4;
            hardWeight = 0.3;
        }

        List<Integer> sectionSlots = new ArrayList<>();
        int sectionCount = (playerCount*3/2) +1;
        Random random = new Random();
        while (sectionSlots.size() < sectionCount){
            int slot = random.nextInt(0, getContainerSize());
            if (!sectionSlots.contains(slot)){
                sectionSlots.add(slot);
            }
        }

        Random sectionRandom = new Random();
        for (int slot : sectionSlots){
            double result = sectionRandom.nextDouble();
                SectionRegistry.SectionInfo section;
                if (result <= (easyWeight)) {
                    section = easySectionRandom.getRandomSection();
                } else if (result <= easyWeight+normalWeight) {
                    section = normalSectionRandom.getRandomSection();
                } else {
                    section = hardSectionRandom.getRandomSection();
                }
                sections.add(section);
                items.set(slot, SectionBuilder.buildItem(section.id()));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("SectionBox");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return ChestMenu.sixRows(i, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int i) {
        return items.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int i1) {
        return items.set(i, ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return items.set(i, ItemStack.EMPTY);
    }

    @Override
    public void setItem(int i, ItemStack itemStack) {
        items.set(i, itemStack);
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}