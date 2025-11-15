package cn.plumc.ultimatech.section;

import static cn.plumc.ultimatech.info.UCHInfos.*;

public record SectionLocation(int x, int y, int z, int l, int w, int h) {
    public SectionLocation add(int il, int iw, int ih){
        return new SectionLocation(x, y, z, l+il, w+iw, h+ih);
    }
    public static SectionLocation get(int row, int col){
        return new SectionLocation(
                SECTION_START_X + SECTION_BORDER * (row+1) + SECTION_SIZE * row,
                SECTION_START_Y,
                SECTION_START_Z + SECTION_BORDER * (col+1) + SECTION_SIZE * col,
                SECTION_SIZE, SECTION_SIZE, SECTION_SIZE);
    }
}
