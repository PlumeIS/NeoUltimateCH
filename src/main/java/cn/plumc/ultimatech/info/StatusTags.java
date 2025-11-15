package cn.plumc.ultimatech.info;

import static cn.plumc.ultimatech.info.UCHInfos.id;

public class StatusTags {
    public static final String DEVELOPER_TAG = id("developer");

    public static final String NO_JUMP_TAG = id("att","no_jump");
    public static final String BOAT_JUMP_TAG = id("att","boat_jump");
    public static final String SHIFT_JUMP_TAG = id("att","shift_jump");
    public static final String SHULKER_JUMP_TAG = id("att","shulker_jump");

    // Game process
    public final static String PICKED_SECTION_TAG = id("game.round", "section_picked");
    public final static String PUTTING_SECTION_TAG = id("game.round", "section_putting");
    public final static String PUTTED_SECTION_TAG = id("game.round", "section_putted");

    public final static String SKIP_TIME_SYNC_TAG = id("game", "skip_time_sync");
}
