package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.section.hit.BoxHit;
import cn.plumc.ultimatech.utils.DisplayEntityUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

import static cn.plumc.ultimatech.sections.RocketPackPad.getRocketItem;
import static cn.plumc.ultimatech.sections.RocketPackPad.globalRockedPlayers;

public class RocketPack extends Section {
    public BoxHit.Relative triggerHit;
    public boolean picked = false;
    public ServerPlayer rockedPlayer;
    public Entity rockEntity;

    public RocketPack(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(5, 6), game);
        setProcess(0);
    }

    @Override
    public void init() {
        triggerHit = new BoxHit.Relative(content.origin, new Vec3(0, 0, 0), new Vec3(1, 1.6, 2));
        transform.applyRotationToRelativeHit(triggerHit);
        rockEntity = content.getContentEntity("uch.rocket_pack_pad.rock");
    }

    @Override
    public void tickRun(int tickTime) {
        ServerPlayer player = triggerHit.detectPlayer(game);
        if (!picked && Objects.nonNull(player)) {
            picked = true;
            rockedPlayer = player;
            setProcess(SectionCounter.toTicks(4.0));
            process.start();
            DisplayEntityUtil.setVisible(rockEntity, "minecraft:lime_glazed_terracotta", false);
            player.getInventory().armor.set(3, getRocketItem());
            player.connection.send(player.getInventory().createInventoryUpdatePacket(103));
            globalRockedPlayers.add(player);
        }
        RocketPackPad.handleRocket(server, game, rockedPlayer);
    }

    @Override
    public void onRoundEnd() {
        if (picked) {
            rockedPlayer.getInventory().armor.set(3, ItemStack.EMPTY);
            remove();
        };
    }
}
