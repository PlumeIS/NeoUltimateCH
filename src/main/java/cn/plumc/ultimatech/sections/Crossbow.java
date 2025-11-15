package cn.plumc.ultimatech.sections;

import cn.plumc.ultimatech.game.Game;
import cn.plumc.ultimatech.section.Section;
import cn.plumc.ultimatech.section.SectionCounter;
import cn.plumc.ultimatech.section.SectionLocation;
import cn.plumc.ultimatech.utils.FixedLinkedList;
import cn.plumc.ultimatech.utils.PlayerUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import static cn.plumc.ultimatech.info.UCHInfos.SECTION_MAX_ENTITIES;

public class Crossbow extends Section {
    public Vec3 spawnPoint;
    public Vec3 movement;

    public FixedLinkedList<Arrow> arrows = new FixedLinkedList<>(SECTION_MAX_ENTITIES) {
        @Override
        public Arrow removeFirst() {
            getFirst().kill();
            return super.removeFirst();
        }
    };

    public Crossbow(ServerPlayer player, Game game) {
        super(player, SectionLocation.get(3, 5), game);
        setProcess(SectionCounter.toTicks(1.0));
    }

    @Override
    public void init() {
        spawnPoint = transform.rotateVector(new Vec3(0.5, 0.5, 2.5));
        movement = transform.rotateVector(new Vec3(0, 0, 0.2));
    }

    @Override
    public void tickRun(int tickTime) {
        if (process.at(0.0)){
            setCharged(false);
        }
        if (process.at(0.5)){
            setCharged(true);
        }
        if (process.at(1.0)) {
            Arrow arrow = new Arrow(EntityType.ARROW, level){
                @Override
                protected void onHitEntity(EntityHitResult result) {
                    if (result.getEntity() instanceof ServerPlayer player) Crossbow.this.kill(player);
                }

                @Override
                protected @NotNull ItemStack getDefaultPickupItem() {
                    return ItemStack.EMPTY;
                }
            };
            arrow.setNoGravity(true);
            arrow.addTag("uch.crossbow.arrow");
            arrow.setPos(transform.toAbsolute(spawnPoint));
            arrows.add(arrow);
            level.addFreshEntity(arrow);
        }
        arrows.forEach(arrow -> {
            arrow.setDeltaMovement(movement);
            PlayerUtil.updateDeltaMovement(server.getPlayerList().getPlayers(), arrow);
        });
    }

    private void setCharged(boolean charged) {
        Entity entity = content.getContentEntity("uch.crossbow");
        EntityDataAccessor accessor = new EntityDataAccessor(entity);
        CompoundTag data = accessor.getData();
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:crossbow");
        if (charged) {
            CompoundTag components = new CompoundTag();
            ListTag charged_projectiles = new ListTag();
            CompoundTag charged_projectiles_item = new CompoundTag();
            charged_projectiles_item.putString("id", "minecraft:arrow");
            CompoundTag charged_projectiles_item_components = new CompoundTag();
            charged_projectiles_item_components.put("minecraft:intangible_projectile", new CompoundTag());
            charged_projectiles_item.put("components", charged_projectiles_item_components);
            charged_projectiles.add(charged_projectiles_item);
            components.put("minecraft:charged_projectiles", charged_projectiles);
            item.put("components", components);
        }
        data.put("item", item);
        try {
            accessor.setData(data);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onRoundEnd() {
        super.onRoundEnd();
        arrows.forEach(entity -> entity.kill());
        arrows.clear();
    }
}
