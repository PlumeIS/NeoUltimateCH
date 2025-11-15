package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.info.UCHInfos;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BlockHit;
import cn.plumc.ultimatech.section.hit.Hit;
import cn.plumc.ultimatech.section.hit.PlayerHit;
import cn.plumc.ultimatech.utils.DisplayEntityUtil;
import cn.plumc.ultimatech.utils.ItemUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Objects;

public class Coin extends Section {
    private boolean picked = false;
    private ServerPlayer pickedPlayer = null;
    private boolean existing = true;

    public Coin(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(1, 0), game);
        setProcess(0);
    }

    @Override
    public void tickRun(int tickTime) {
        if (!checkCoinCanPick()) return;
        List<PlayerHit> playerHits = Hit.getPlayerHits(game);
        BlockHit coinHit = new BlockHit(content.getEntityPosition(UCHInfos.contentID("coin")));
        for (PlayerHit playerHit : playerHits) {
            if (coinHit.intersect(playerHit)&&checkCoinCanPick()) {
                picked = true;
                pickedPlayer = playerHit.getPlayer();
                setVisible(false);
                addCoin(pickedPlayer);
            }
        }
    }

    private boolean checkCoinCanPick(){
        if (!existing||picked) return false;
        Entity coin = content.getContentEntity(UCHInfos.contentID("coin"));
        EntityDataAccessor accessor = new EntityDataAccessor(coin);
        CompoundTag data = accessor.getData();
        return !data.getCompound("item").getString("id").equals("minecraft:air");
    }

    private void setVisible(boolean visible) {
        Entity coin = content.getContentEntity(UCHInfos.contentID("coin"));
        DisplayEntityUtil.setVisible(coin, "minecraft:activator_rail", visible);
    }

    private void addCoin(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(0);
        if (ItemStack.isSameItem(item, getCoinItem())){
            item.setCount(item.getCount() + 1);
            inventory.setItem(0, item);
        } else {
            inventory.add(getCoinItem());
        }
        inventory.armor.set(3, getCoinItem());
    }

    private static ItemStack getCoinItem(){
        ItemStack itemStack = new ItemStack(Items.ACTIVATOR_RAIL);

        MutableComponent name = Component.literal("金币")
                .withStyle(ChatFormatting.YELLOW)
                .withStyle(ChatFormatting.BOLD);

        MutableComponent lore = Component.literal("注意: 丢掉就丢掉了!")
                .withStyle(ChatFormatting.RED);

        ItemEnchantments.Mutable enchantment = ItemUtil.getItemEnchantment();
        enchantment.set(ItemUtil.getEnchantment(Enchantments.BINDING_CURSE), 1);

        itemStack.set(DataComponents.ENCHANTMENTS, enchantment.toImmutable());
        itemStack.set(DataComponents.ITEM_NAME, name);
        itemStack.set(DataComponents.LORE, new ItemLore(List.of(lore)));
        return itemStack;
    }

    @Override
    public void onPlayerWin(ServerPlayer player) {
        if (pickedPlayer.getUUID().equals(player.getUUID())) existing = false;
    }

    @Override
    public void onPlayerDeath(ServerPlayer player) {
        if (Objects.nonNull(pickedPlayer)) clearPlayerCoin(pickedPlayer);
        if (pickedPlayer!=null&&pickedPlayer.getUUID().equals(player.getUUID())) pickedPlayer = null;
    }

    @Override
    public void onRoundStart() {
        super.onRoundStart();
        if (Objects.nonNull(pickedPlayer)) clearPlayerCoin(pickedPlayer);
        if (existing) {
            pickedPlayer = null;
            picked = false;
            setVisible(true);
        };
    }

    public static void clearPlayerCoin(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++){
            ItemStack item = inventory.getItem(i);
            if (ItemStack.isSameItem(item, getCoinItem())){
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public static int getPlayerCoinCount(ServerPlayer player){
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++){
            ItemStack item = inventory.getItem(i);
            if (ItemStack.isSameItem(item, getCoinItem())){
                count+=item.getCount();
            }
        }
        return count;
    }
}
