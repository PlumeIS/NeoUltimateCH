package cn.plumc.ultimatech.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Objects;

public class ItemUtil {
    public static Holder<Enchantment> getEnchantment(ResourceKey<Enchantment> enchantment) {
        HolderLookup.RegistryLookup<Enchantment> enchantmentRegistry = ServerLifecycleHooks.getCurrentServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return enchantmentRegistry.getOrThrow(enchantment);
    }

    public static ItemEnchantments.Mutable getItemEnchantment() {
        return new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    }

    public static ItemStack getStyledItem(ItemStack itemStack, Component component) {
        itemStack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        itemStack.set(DataComponents.ITEM_NAME, component);
        return itemStack;
    }

    public static ItemStack withTag(ItemStack itemStack, String... tag) {
        CustomData data = itemStack.get(DataComponents.CUSTOM_DATA);
        CompoundTag dataTag = Objects.isNull(data) ? new CompoundTag() : data.copyTag();
        ListTag tags = new ListTag();
        for (String s : tag) {
            tags.add(StringTag.valueOf(s));
        }
        dataTag.put("Tags", tags);
        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(dataTag));
        return itemStack;
    }
}
