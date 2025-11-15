package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.sections.*;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

import static cn.plumc.ultimatech.info.UCHInfos.id;


public class SectionRegistry {
    public static SectionRegistry instance;

    public record SectionSize(int length, int width, int height){
        public String getString(){
            return length + "x" + width + "x" + height;
        }
        @Override
        public String toString() {
            return getString();
        }
        public static SectionSize get(int length, int width, int height){
            return new SectionSize(length, width, height);
        }
    };
    public enum SectionDifficulty {EASY, NORMAL, HARD}
    public record SectionInfo(int code, String id, String name, SectionSize size, SectionDifficulty difficulty, double weight, Class<? extends Section> section) {}

    private final Map<Integer, Class<? extends Section>> byCode = new HashMap<>();
    private final Map<String, Class<? extends Section>> byId = new HashMap<>();
    private final Map<String, Class<? extends Section>> byName = new HashMap<>();
    private final Map<String, SectionInfo> sectionInfos = new HashMap<>();

    public SectionRegistry() {
        instance = this;
        builtinRegistries();
    }

    private void builtinRegistries(){
        register(1, id("beam1"), "横木", SectionSize.get(1, 1, 1), SectionDifficulty.EASY, Beam1.class);
        register(2, id("beam2"), "横木", SectionSize.get(2, 1, 1), SectionDifficulty.EASY, Beam2.class);
        register(3, id("beam3"), "横木", SectionSize.get(3, 1, 1), SectionDifficulty.EASY, Beam3.class);
        register(4, id("beam4"), "横木", SectionSize.get(4, 1, 1), SectionDifficulty.EASY, Beam4.class);
        register(5, id("beam5"), "横木", SectionSize.get(5, 1, 1), SectionDifficulty.EASY, Beam5.class);
        register(6, id("beam6"), "横木", SectionSize.get(6, 1, 1), SectionDifficulty.EASY, Beam6.class);
        register(7, id("l_shaped_steel"), "L形框架", SectionSize.get(3, 1, 3), SectionDifficulty.EASY, LShapedSteel.class);
        register(8, id("barrel"), "铁桶", SectionSize.get(2, 2, 2), SectionDifficulty.EASY, Barrel.class);
        register(9, id("hay"), "草垛", SectionSize.get(3, 3, 3), SectionDifficulty.EASY, Hay.class);
        register(10, id("coin"), "金币", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, Coin.class);
        register(11, id("fragile_bricks1"), "碎碎砖", SectionSize.get(1, 1, 1), SectionDifficulty.EASY, FragileBricks1.class);
        register(12, id("fragile_bricks2"), "碎碎砖", SectionSize.get(3, 1, 2), SectionDifficulty.EASY, FragileBricks2.class);
        register(13, id("fragile_bricks3"), "碎碎砖", SectionSize.get(3, 1, 2), SectionDifficulty.EASY, FragileBricks3.class);
        register(14, id("steel_frame"), "悬浮钢架", SectionSize.get(1, 1, 5), SectionDifficulty.EASY, SteelFrame.class);
        register(15, id("ice1"), "冰面", SectionSize.get(1, 1, 1), SectionDifficulty.EASY, Ice1.class);
        register(16, id("ice2"), "冰面", SectionSize.get(2, 1, 1), SectionDifficulty.EASY, Ice2.class);
        register(17, id("ice3"), "冰面", SectionSize.get(3, 1, 1), SectionDifficulty.EASY, Ice3.class);
        register(18, id("sharp_step"), "尖刺陷阱", SectionSize.get(3, 1, 1), SectionDifficulty.NORMAL, SharpStep.class);
        register(19, id("wire_mesh3"), "地刺", SectionSize.get(3, 1, 1), SectionDifficulty.NORMAL, WireMesh3.class);
        register(20, id("wire_mesh2"), "地刺", SectionSize.get(2, 1, 1), SectionDifficulty.NORMAL, WireMesh2.class);
        register(21, id("wire_mesh1"), "地刺", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, WireMesh1.class);
        register(22, id("spiky_ball"), "刺球", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, SpikyBall.class);
        register(23, id("flippable_floor_stabbing"), "翻转地刺", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, FlippableFloorStabbing.class);
        register(24, id("pendulum"), "大摆锤", SectionSize.get(1, 1, 7), SectionDifficulty.HARD, Pendulum.class);
        register(25, id("fist"), "拳头", SectionSize.get(1, 1, 2), SectionDifficulty.NORMAL, Fist.class);
        register(26, id("honeycomb"), "蜂巢", SectionSize.get(2, 2, 2), SectionDifficulty.HARD, Honeycomb.class);
        register(27, id("magnetic_step"), "磁吸平台", SectionSize.get(2, 1, 1), SectionDifficulty.EASY, MagneticStep.class);
        register(28, id("flamethrower"), "喷火器", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, Flamethrower.class);
        register(29, id("pitcher"), "投球器", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, Pitcher.class);
        register(30, id("artillery"), "大炮", SectionSize.get(2, 2, 2), SectionDifficulty.HARD, Artillery.class);
        register(31, id("black_hole"), "黑洞", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, BlackHole.class);
        register(32, id("barrett"), "巴雷特", SectionSize.get(1, 1, 3), SectionDifficulty.HARD, Barrett.class);
        register(33, id("crossbow"), "弩", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, Crossbow.class);
        register(34, id("rocket_pack_pad"), "火箭背包平台", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, RocketPackPad.class);
        register(35, id("tyson"), "泰森", SectionSize.get(1, 1, 2), SectionDifficulty.NORMAL, Tyson.class);
        register(36, id("airplane"), "纸飞机", SectionSize.get(2, 2, 4), SectionDifficulty.NORMAL, Airplane.class);
        register(37, id("flying_saucer"), "飞碟", SectionSize.get(2, 2, 1), SectionDifficulty.NORMAL, FlyingSaucer.class);
        register(38, id("fire_hydrant"), "消防栓", SectionSize.get(1, 1, 2), SectionDifficulty.NORMAL, FireHydrant.class);
        register(39, id("fan"), "风扇", SectionSize.get(3, 3, 1), SectionDifficulty.NORMAL, Fan.class);
        register(40, id("automatic_door"), "自动门", SectionSize.get(1, 1, 4), SectionDifficulty.EASY, AutomaticDoor.class);
        register(41, id("movable_step1"), "移动平台", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, MovableStep1.class);
        register(42, id("movable_step2"), "移动平台", SectionSize.get(2, 1, 1), SectionDifficulty.NORMAL, MovableStep2.class);
        register(43, id("movable_step3"), "移动平台", SectionSize.get(3, 1, 1), SectionDifficulty.NORMAL, MovableStep3.class);
        register(44, id("stair"), "斜梯", SectionSize.get(4, 1, 4), SectionDifficulty.EASY, Stair.class);
        register(45, id("spring"), "弹簧", SectionSize.get(2, 2, 1), SectionDifficulty.NORMAL, Spring.class);
        register(46, id("chain_saw"), "链锯平台", SectionSize.get(6, 1, 1), SectionDifficulty.NORMAL, ChainSaw.class);
        register(47, id("rotating_saw_blade"), "旋转锯片", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, RotatingSawBlade.class);
        register(48, id("conveyer_belt"), "传送带", SectionSize.get(3, 1, 1), SectionDifficulty.NORMAL, ConveyerBelt.class);
        register(49, id("firecracker"), "炮竹", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, Firecracker.class);
        register(50, id("bomb"), "炸弹", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, Bomb.class);
        register(51, id("n_bomb"), "核弹", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, NBomb.class);
        register(52, id("rocket_pack"), "火箭背包", SectionSize.get(1, 1, 1), SectionDifficulty.HARD, RocketPack.class);
        register(53, id("honey"), "蜂蜜", SectionSize.get(1, 1, 1), SectionDifficulty.NORMAL, Honey.class);
    }

    public void register(int code, String id, String name, SectionSize size, Class<? extends Section> section) {
        SectionInfo info = new SectionInfo(code, id, name, size, SectionDifficulty.EASY, 1.0, section);
        register(info);
    }

    public void register(int code, String id, String name, SectionSize size, SectionDifficulty difficulty, Class<? extends Section> section){
        SectionInfo info = new SectionInfo(code, id, name, size, difficulty, 1.0, section);
        register(info);
    }

    public void register(int code, String id, String name, SectionSize size, SectionDifficulty difficulty, double weight, Class<? extends Section> section){
        SectionInfo info = new SectionInfo(code, id, name, size, difficulty, weight, section);
        register(info);
    }

    public void register(SectionInfo sectionInfo) {
        byCode.put(sectionInfo.code, sectionInfo.section);
        byId.put(sectionInfo.id, sectionInfo.section);
        byName.put(sectionInfo.name, sectionInfo.section);
        sectionInfos.put(sectionInfo.id, sectionInfo);
    }

    public Class<? extends Section> byCode(int code) {
        return byCode.get(code);
    }

    public Class<? extends Section> byId(String id) {
        return byId.get(id);
    }

    public Class<? extends Section> byName(String name) {
        return byName.get(name);
    }

    public SectionInfo getSectionInfo(String id) {
        return sectionInfos.get(id);
    }

    public SectionInfo getSectionInfo(Class<? extends Section> section) {
        for(SectionInfo info : sectionInfos.values()){
            if(info.section.equals(section)){
                return info;
            }
        }
        return null;
    }

    public String getId(Class<? extends Section> section) {
        for(SectionInfo info : sectionInfos.values()){
            if(info.section.equals(section)){
                return info.id;
            }
        }
        return null;
    }

    public Map<String, SectionInfo> getSectionInfos() {
        return ImmutableMap.copyOf(sectionInfos);
    }
}
