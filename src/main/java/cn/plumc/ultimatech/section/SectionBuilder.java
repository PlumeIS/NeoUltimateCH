package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.utils.ItemUtil;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.Unbreakable;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class SectionBuilder {
    private Game game;

    public SectionBuilder(Game game){
        this.game = game;
    }

    public Section build(String id, ServerPlayer player){
        try {
            return SectionRegistry.instance.byId(id).getConstructor(ServerPlayer.class, Game.class).newInstance(player, game);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Section build(String id){
        FakePlayer mapPlayer = new FakePlayer(game.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(UCHInfos.MAP_HOLDER.getBytes(StandardCharsets.UTF_8)), UCHInfos.MAP_HOLDER));
        return build(id, mapPlayer);
    }

    public static ItemStack buildItem(String id){
        SectionRegistry.SectionInfo info = SectionRegistry.instance.getSectionInfo(id);
        ItemStack itemStack = new ItemStack(Items.PUFFERFISH_BUCKET);

        ItemUtil.withTag(itemStack, info.id());

        itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(info.code()));
        itemStack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        itemStack.set(DataComponents.ITEM_NAME, Component.literal("")
                .append(info.name()).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GREEN)
                .append(" - ").withStyle(ChatFormatting.GRAY)
                .append(info.size().getString()).withStyle(ChatFormatting.YELLOW)
        );
        return itemStack;
    }
}
