package cn.plumc.ultimatech.info;

import cn.plumc.ultimatech.UltimateCH;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static cn.plumc.ultimatech.utils.ItemUtil.getStyledItem;
import static cn.plumc.ultimatech.utils.ItemUtil.withTag;

public class UCHInfos {
    public static final boolean DEBUG = false;

    public static Path MINECRAFT_DIR;
    public static Path UCH_PATCH;
    public static Path CACHED_PATCH;
    public static Path CACHED_SKIN_PATCH;

    // Position
    public static final Vec3 PLAYER_LOBBY_POINT = new Vec3(0.50, 1.00, -16.5);
    public static final Vec3 DEVELOPER_POINT = new Vec3(1000.50, 1.00, 0.5);

    // Section
    public static final int SECTION_SIZE = 6;
    public static final int SECTION_START_X = 1000;
    public static final int SECTION_START_Y = 0;
    public static final int SECTION_START_Z = 0;
    public static final int SECTION_BORDER = 1;

    public static final List<EntityType<?>> TEMPLATE_ENTITIES = Arrays.asList(
            EntityType.BLOCK_DISPLAY,
            EntityType.ITEM_DISPLAY,
            EntityType.TEXT_DISPLAY
    );

    public static final int SECTION_VIEW_DISTANCE = 5;

    public static final int SECTION_ROTATION_DEGREE = 90;

    public static final int SECTION_MAX_ENTITIES = 100;

    public static final BlockState SECTION_NONPPLACABLE_BLOCK = Blocks.RED_STAINED_GLASS.defaultBlockState();
    public static final BlockState SECTION_DISCARDED_BLOCK = Blocks.YELLOW_STAINED_GLASS.defaultBlockState();

    public static final ItemStack SECTION_PLACE_ITEM = withTag(getStyledItem(new ItemStack(Items.WOODEN_SWORD),
            Component.literal(">").withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.YELLOW)
                    .append("放置建筑").withStyle(ChatFormatting.UNDERLINE)
                    .append("<")
    ), contentID("action_item.place"));
    public static final ItemStack SECTION_ROTATE_X_ITEM = withTag(getStyledItem(new ItemStack(Items.STONE_SWORD),
            Component.literal("绕X轴旋转").withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.YELLOW)
    ), contentID("action_item.rotate.x"));
    public static final ItemStack SECTION_ROTATE_Y_ITEM = withTag(getStyledItem(new ItemStack(Items.GOLDEN_SWORD),
            Component.literal("绕Y轴旋转").withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.YELLOW)
    ), contentID("action_item.rotate.y"));
    public static final ItemStack SECTION_ROTATE_Z_ITEM = withTag(getStyledItem(new ItemStack(Items.DIAMOND_SWORD),
            Component.literal("绕Z轴旋转").withStyle(ChatFormatting.UNDERLINE).withStyle(ChatFormatting.YELLOW)
    ), contentID("action_item.rotate.z"));

    public static String contentID(String name){
        return UltimateCH.CONTENT_ID + "." + name;
    }
    public static String id(String name){
        return UltimateCH.MOD_ID + "." + name;
    }
    public static String id(String category, String name){
        return UltimateCH.MOD_ID + "." + category + "." + name;
    }
}
