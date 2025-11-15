package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.utils.BlockUtil;
import cn.plumc.ultimatech.utils.IntCounter;
import cn.plumc.ultimatech.utils.PlayerUtil;
import cn.plumc.ultimatech.utils.RotationUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static cn.plumc.ultimatech.info.UCHInfos.*;

public class SectionContent {
    public static class SectionEntities{
        public final List<Tuple<Vec3, Entity>> entities = new ArrayList<>();
        public final HashMap<UUID, Vec3> positions = new HashMap<>();
        public void add(Vec3 pos, Entity entity){
            entities.add(new Tuple<>(pos, entity));
            positions.put(entity.getUUID(), pos);
        }
    }

    public Section parent;
    public SectionLocation location;
    public ServerLevel level;

    public List<Tuple<Vec3, Entity>> templateEntities = new ArrayList<>();
    public Map<BlockPos, BlockState> templateBlocks = new HashMap<>();

    public SectionEntities entities = new SectionEntities();
    public HashMap<BlockPos, BlockState> blocks = new HashMap<>();
    public Vec3 origin;

    public Map<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> worldRecoverCache = new HashMap<>();

    public ArrayList<Display.TextDisplay> debugEntities = new ArrayList<>();
    public IntCounter debugCounter = new IntCounter();

    public SectionContent(Section parent, SectionLocation location, ServerLevel level) {
        this.parent = parent;
        this.location = location;
        this.level = level;

        this.load();
    }

    public void load(){
        Vec3 originPoint = null;

        for (int y = location.y(); y < location.y()+location.h(); y++){
            for (int z = location.z(); z < location.z()+location.w(); z++){
                for (int x = location.x(); x < location.x()+location.l(); x++){
                    BlockState blockState = level.getBlockState(new BlockPos(x, y, z));
                    if (!blockState.isAir()){
                        if (Objects.isNull(originPoint)) originPoint = new Vec3(x, y, z);
                        this.templateBlocks.put(
                                new BlockPos(Mth.floor(x-originPoint.x),
                                        Mth.floor(y-originPoint.y),
                                        Mth.floor(z-originPoint.z)),
                                blockState);
                    }
                }
            }
        }
        if (Objects.isNull(originPoint)) originPoint = new Vec3(location.x(), location.y(), location.z());

        BlockPos minPos = new BlockPos(location.x(), location.y(), location.z());
        BlockPos maxPos = new BlockPos(location.x()+location.w(), location.y()+location.h(), location.z()+location.l());
        AABB aabb = AABB.encapsulatingFullBlocks(minPos, maxPos);
        Vec3 copiedOriginPoint = originPoint;
        level.getEntities().get(aabb, (entity -> {
            if (TEMPLATE_ENTITIES.contains(entity.getType())){
                Vec3 position = entity.position();
                double x = position.x - copiedOriginPoint.x;
                double y = position.y - copiedOriginPoint.y;
                double z = position.z - copiedOriginPoint.z;
                templateEntities.add(new Tuple<>(new Vec3(x,y,z), entity));};
        }));
    }

    public void createViewEntities(){
        for (Tuple<Vec3, Entity> entityEntry : templateEntities) {
            Vec3 relative = entityEntry.getA();
            EntityType<?> templateType = entityEntry.getB().getType();

            Display entity;
            if (templateType == EntityType.ITEM_DISPLAY) entity = new Display.ItemDisplay(templateType, level);
            else if (templateType == EntityType.BLOCK_DISPLAY) entity = new Display.BlockDisplay(templateType, level);
            else if (templateType == EntityType.TEXT_DISPLAY) entity = new Display.TextDisplay(templateType, level);
            else throw new RuntimeException("Unsupported type: " + templateType.getDescriptionId());

            EntityDataAccessor templateDataAccessor = new EntityDataAccessor(entityEntry.getB());
            CompoundTag templateData = templateDataAccessor.getData();
            CompoundTag transformation = (CompoundTag)templateData.get("transformation");
            ListTag translation = (ListTag)transformation.get("translation");
            float t_x;
            float t_y;
            float t_z;
            translation.set(0, FloatTag.valueOf(t_x = (float) (translation.getFloat(0) + relative.x)));
            translation.set(1, FloatTag.valueOf(t_y = (float) (translation.getFloat(1) + relative.y)));
            translation.set(2, FloatTag.valueOf(t_z = (float) (translation.getFloat(2) + relative.z)));
            transformation.put("translation", translation);
            templateData.put("transformation", transformation);

            try {
                new EntityDataAccessor(entity).setData(templateData);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }

            entities.add(new Vec3(t_x, t_y, t_z), entity);
            level.addFreshEntity(entity);
        }
    }

    public void handleView(){
        setSectionViewInventory(parent.owner);
        Vec3 viewOrigin = PlayerUtil.getPlayerLooking(parent.owner, SECTION_VIEW_DISTANCE);
        BlockPos blockOrigin = new BlockPos(Mth.floor(viewOrigin.x), Mth.floor(viewOrigin.y), Mth.floor(viewOrigin.z));

        // recover
        for (Map.Entry<BlockPos, Tuple<BlockState, Optional<BlockEntity>>> recoverEntry : worldRecoverCache.entrySet()) {
            parent.level.setBlockAndUpdate(recoverEntry.getKey(), recoverEntry.getValue().getA());
            Optional<BlockEntity> blockEntity = recoverEntry.getValue().getB();
            blockEntity.ifPresent(entity -> parent.level.setBlockEntity(entity));
        }
        worldRecoverCache.clear();

        // block
        for (Map.Entry<BlockPos, BlockState> blockEntry : parent.rotation.rotated(templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            Tuple<BlockState, Optional<BlockEntity>> recoverBlock = parent.game.getSectionManager().getRecoverBlockOrWorldBlock(worldPos);
            if (!BlockUtil.isMultiBlock(parent.level, worldPos)) {
                worldRecoverCache.put(worldPos, recoverBlock);
                parent.level.setBlockAndUpdate(worldPos, checkCanPlace(worldPos, recoverBlock.getA()) ? blockEntry.getValue() : SECTION_NONPPLACABLE_BLOCK);
            }
        }

        // entities
        parent.rotation.rotated(entities);
        for (Entity entityEntry : entities.entities.stream().map(Tuple::getB).toList()) {
            entityEntry.moveTo(blockOrigin.getX(), blockOrigin.getY(), blockOrigin.getZ());
        }

        if (DEBUG) handleViewDebug();
    }

    public boolean handlePlace(){
        if (parent.placed&&!parent.viewing) return false;
        Vec3 viewOrigin = PlayerUtil.getPlayerLooking(parent.owner, SECTION_VIEW_DISTANCE);
        BlockPos blockOrigin = new BlockPos(Mth.floor(viewOrigin.x), Mth.floor(viewOrigin.y), Mth.floor(viewOrigin.z));

        for (Map.Entry<BlockPos, BlockState> blockEntry : parent.rotation.rotated(templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            BlockState worldBlockState;
            if (worldRecoverCache.containsKey(worldPos)) worldBlockState = worldRecoverCache.get(worldPos).getA();
            else worldBlockState = parent.level.getBlockState(worldPos);
            blocks.put(worldPos, blockEntry.getValue());
            if (BlockUtil.isMultiBlock(parent.level, worldPos)||!checkCanPlace(worldPos, worldBlockState)){
                blocks.clear();
                return false;
            }
        }
        parent.game.getSectionManager().announcePlace(parent);

        this.origin = viewOrigin;
        parent.placed = true;
        parent.viewing = false;
        return true;
    }

    public void handleViewDebug(){
        if (debugEntities.isEmpty()) {
            Display.TextDisplay x = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
            Display.TextDisplay y = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
            Display.TextDisplay z = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);

            x.setText(Component.literal("X").withStyle(ChatFormatting.RED));
            y.setText(Component.literal("Y").withStyle(ChatFormatting.GREEN));
            z.setText(Component.literal("Z").withStyle(ChatFormatting.BLUE));

            level.addFreshEntity(x);
            level.addFreshEntity(y);
            level.addFreshEntity(z);

            debugEntities.add(x);
            debugEntities.add(y);
            debugEntities.add(z);
        };
        debugCounter.add();
        if (debugCounter.get()%5==0) return;
        Vec3 viewOrigin = PlayerUtil.getPlayerLooking(parent.owner, SECTION_VIEW_DISTANCE);
        SectionRegistry.SectionInfo info = SectionRegistry.instance.getSectionInfo(this.parent.getClass());
        SectionRegistry.SectionSize size = info.size();
        createPoint(viewOrigin, new Vec3(0, 0, 0));
        // X
        for (float dx = 0; dx <= size.width(); dx += 0.1f) {
            createPoint(RotationUtil.rotatePoint(viewOrigin.add(dx, 0, 0), viewOrigin, parent.rotation.getRotations()), new Vec3(1, 0, 0));
        }
        Vec3 pointX = RotationUtil.rotatePoint(new Vec3(viewOrigin.x + size.width() + 0.5, viewOrigin.y, viewOrigin.z), viewOrigin, parent.rotation.getRotations());
        debugEntities.get(0).teleportTo(pointX.x, pointX.y, pointX.z);
        // Y
        for (float dy = 0; dy <= size.height(); dy += 0.1f) {
            createPoint(RotationUtil.rotatePoint(viewOrigin.add(0, dy, 0), parent.rotation.getRotations()), new Vec3(0, 1, 0));
        }
        Vec3 pointY = RotationUtil.rotatePoint(new Vec3(viewOrigin.x, viewOrigin.y+size.height()+0.5, viewOrigin.z), viewOrigin, parent.rotation.getRotations());
        debugEntities.get(1).teleportTo(pointY.x, pointY.y, pointY.z);
        // Z
        for (float dz = 0; dz <= size.length(); dz += 0.1f) {
            createPoint(RotationUtil.rotatePoint(viewOrigin.add(0, 0, dz), parent.rotation.getRotations()), new Vec3(0, 0, 1));
        }
        Vec3 pointZ = RotationUtil.rotatePoint(new Vec3(viewOrigin.x, viewOrigin.y, viewOrigin.z+size.length()+0.5), viewOrigin, parent.rotation.getRotations());
        debugEntities.get(2).teleportTo(pointZ.x, pointZ.y, pointZ.z);
    }

    private void createPoint(Vec3 pos, Vec3 color){
        level.sendParticles(new DustParticleOptions(ARGB.color(color), 0.5f), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }

    public boolean checkCanPlace(BlockPos pos, BlockState lastBlockState){
        return (lastBlockState.isAir()) && parent.game.getStatus().map.canPlace(pos);
    }

    public void setSectionViewInventory(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (ItemStack itemStack : inventory.items){
            if (ItemStack.isSameItem(itemStack, SECTION_PLACE_ITEM)||
                    ItemStack.isSameItem(itemStack, SECTION_ROTATE_X_ITEM)||
                    ItemStack.isSameItem(itemStack, SECTION_ROTATE_Y_ITEM)||
                    ItemStack.isSameItem(itemStack, SECTION_ROTATE_Z_ITEM)){
                inventory.clearContent();
            }
        }

        if (inventory.findSlotMatchingItem(SECTION_PLACE_ITEM)!=4||
                inventory.findSlotMatchingItem(SECTION_ROTATE_X_ITEM)!=6||
                inventory.findSlotMatchingItem(SECTION_ROTATE_Y_ITEM)!=7||
                inventory.findSlotMatchingItem(SECTION_ROTATE_Z_ITEM)!=8){

            inventory.clearContent();
            inventory.setItem(0, Items.AIR.getDefaultInstance());
            inventory.setItem(4, SECTION_PLACE_ITEM);
            inventory.setItem(6, SECTION_ROTATE_X_ITEM);
            inventory.setItem(7, SECTION_ROTATE_Y_ITEM);
            inventory.setItem(8, SECTION_ROTATE_Z_ITEM);
        }
        List<? extends ItemEntity> itemEntities = level.getEntities(EntityType.ITEM,
                itemEntity -> itemEntity.getItem().is(SECTION_PLACE_ITEM.getItem()) ||
                        itemEntity.getItem().is(SECTION_ROTATE_X_ITEM.getItem()) ||
                        itemEntity.getItem().is(SECTION_ROTATE_Y_ITEM.getItem()) ||
                        itemEntity.getItem().is(SECTION_ROTATE_Z_ITEM.getItem()));
        for (ItemEntity itemEntity : itemEntities){
            itemEntity.kill(level);
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            player.connection.send(inventory.createInventoryUpdatePacket(i));
        }
    }

    public void remove() {
        blocks.forEach((key, value) -> level.setBlockAndUpdate(key, Blocks.AIR.defaultBlockState()));
        entities.entities.forEach((entry) -> entry.getB().kill(level));
    }

    public boolean containsBlock(BlockPos pos) {
        for (BlockPos blockPos : blocks.keySet()) {
            if (blockPos.equals(pos)) return true;
        }
        return false;
    }

    public Entity getContentEntity(String tag) {
        for (Entity entity : entities.entities.stream().map(Tuple::getB).toList()) {
            if (entity.getTags().contains(tag)) return entity;
        }
        return null;
    }

    public List<Entity> getContentEntities(String tag) {
        List<Entity> find = new ArrayList<>();
        for (Entity entity : entities.entities.stream().map(Tuple::getB).toList()) {
            if (entity.getTags().contains(tag)) find.add(entity);
        }
        return find;
    }

    public Vec3 getRelativeEntityPosition(String tag) {
        for (Tuple<Vec3, Entity> entityEntry : entities.entities) {
            if (entityEntry.getB().getTags().contains(tag)) return entityEntry.getA();
        }
        return null;
    }

    public Vec3 getEntityPosition(String tag) {
        for (Tuple<Vec3, Entity> entityEntry : entities.entities) {
            if (entityEntry.getB().getTags().contains(tag)) return entityEntry.getA().add(origin);
        }
        return null;
    }
}
