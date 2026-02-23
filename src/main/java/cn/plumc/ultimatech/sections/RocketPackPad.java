package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.DisplayEntityUtil;
import cn.plumc.ultimatech.utils.ItemUtil;
import cn.plumc.ultimatech.utils.PlayerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RocketPackPad extends Section {
    public static List<ServerPlayer> globalRockedPlayers = new ArrayList<>();

    public BoxHit.Relative triggerHit;
    public boolean triggered;
    public Entity rockEntity;


    public List<ServerPlayer> rockedPlayers = new ArrayList<>();

    public RocketPackPad(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3, 6), game);
        setProcess(0);
    }

    @Override
    public void init() {
        triggerHit = new BoxHit.Relative(()->content.getOrigin(), new Vec3(0, 0, 0), new Vec3(1, 1.6, 2));
        transform.applyRotationToRelativeHit(triggerHit);
        rockEntity = content.getContentEntity("uch.rocket_pack_pad.rock");
    }

    @Override
    public void tickRun(int tickTime) {
        ServerPlayer player = triggerHit.detectPlayer(game);
        if (!triggered && Objects.nonNull(player) && !rockedPlayers.contains(player)) {
            triggered = true;
            setProcess(SectionCounter.toTicks(4.0));
            process.start();
            DisplayEntityUtil.setVisible(rockEntity, "minecraft:lime_glazed_terracotta", false);
            player.getInventory().armor.set(3, getRocketItem());
            player.connection.send(PlayerUtil.createInventoryUpdatePacket(player,103));
            rockedPlayers.add(player);
            globalRockedPlayers.add(player);
        }
        if (triggered && process.at(4.0)) {
            triggered = false;
            DisplayEntityUtil.setVisible(rockEntity, "minecraft:lime_glazed_terracotta", true);
        }

        rockedPlayers.forEach(rockedPlayer -> handleRocket(server, game, rockedPlayer));
    }

    public static void handleRocket(MinecraftServer server, Game game, ServerPlayer player) {
        if (player.isShiftKeyDown()&&game.getStatus().getPlayings().contains(player)) {
            float yaw = player.getYRot();
            float pitch = player.getXRot();

            // 将角度转换为弧度
            double radiansYaw = Math.toRadians(yaw);
            double radiansPitch = Math.toRadians(pitch);

            // 计算向量
            double x = -Math.sin(radiansYaw) * Math.cos(radiansPitch);
            double y = -Math.sin(radiansPitch);
            double z = Math.cos(radiansYaw) * Math.cos(radiansPitch);

            Vec3 to = new Vec3(x, y, z).normalize().scale(0.25f);
            Vec3 movement = player.getDeltaMovement();

            player.setDeltaMovement(Math.min(.5f, to.x), Math.min(.75f, movement.y + 0.25f), Math.min(.5f, to.z));
            PlayerUtil.updateDeltaMovement(server.getPlayerList().getPlayers(), player);
        }
    }

    public static ItemStack getRocketItem(){
        ItemStack itemStack = new ItemStack(Items.LIME_GLAZED_TERRACOTTA);
        MutableComponent name = Component.literal("喷气背包")
                .withStyle(ChatFormatting.YELLOW)
                .withStyle(ChatFormatting.BOLD);

        ItemEnchantments.Mutable enchantment = ItemUtil.getItemEnchantment();
        enchantment.set(ItemUtil.getEnchantment(Enchantments.BINDING_CURSE), 1);

        itemStack.set(DataComponents.ENCHANTMENTS, enchantment.toImmutable());
        itemStack.set(DataComponents.ITEM_NAME, name);
        return itemStack;
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        rockedPlayers.forEach(player -> player.getInventory().armor.set(3, ItemStack.EMPTY));
        rockedPlayers.clear();
        globalRockedPlayers.clear();
        setProcess(0);
    }
}
