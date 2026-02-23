package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.game.SectionManager;
import cn.plumc.ultimatech.section.layer.LayerType;
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
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    public SectionManager manager;
    public SectionLocation location;
    public ServerLevel level;
    public Vec3 mapOrigin;

    public List<Tuple<Vec3, Entity>> templateEntities = new ArrayList<>();
    public Map<BlockPos, BlockState> templateBlocks = new HashMap<>();

    public SectionEntities entities = new SectionEntities();
    public HashSet<BlockPos> changedBlocks = new HashSet<>();

    public Vec3 origin;
    public Vec3 relativeOrigin = new Vec3(0, 0, 0);

    public ArrayList<Display.TextDisplay> debugEntities = new ArrayList<>();
    public IntCounter debugCounter = new IntCounter();

    public SectionContent(Section parent, SectionLocation location, ServerLevel level) {
        this.parent = parent;
        this.location = location;
        this.level = level;

        this.manager = this.parent.game.getSectionManager();
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
            if (templateType == EntityType.ITEM_DISPLAY) entity = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
            else if (templateType == EntityType.BLOCK_DISPLAY) entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
            else if (templateType == EntityType.TEXT_DISPLAY) entity = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
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

            if (!parent.mapSection) entity.moveTo(parent.owner.position());
            else {
                ServerPlayer player = parent.game.getPlayerManager().getPlayers().get(0);
                entity.moveTo(player.position());
            }
            entities.add(new Vec3(t_x, t_y, t_z), entity);
            level.addFreshEntity(entity);
        }
    }

    public void handleView(){
        setSectionViewInventory(parent.owner);
        Vec3 viewOrigin = parent.mapSection ? mapOrigin : PlayerUtil.getPlayerLooking(parent.owner, SECTION_VIEW_DISTANCE);
        BlockPos blockOrigin = new BlockPos(Mth.floor(viewOrigin.x), Mth.floor(viewOrigin.y), Mth.floor(viewOrigin.z));

        // recover
        for (BlockPos changedBlock : changedBlocks) {
            manager.getViewLayer().remove(changedBlock);
        }
        changedBlocks.clear();

        // block
        for (Map.Entry<BlockPos, BlockState> blockEntry : parent.rotation.rotated(templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            Tuple<BlockState, Optional<BlockEntity>> block = manager.getTopLayer().get(worldPos);
            if (!BlockUtil.isMultiBlock(parent.level, worldPos)) {
                manager.getViewLayer().set(worldPos, checkCanPlace(worldPos, block.getA()) ? blockEntry.getValue() : SECTION_NONPPLACABLE_BLOCK);
                changedBlocks.add(worldPos);
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
        Vec3 viewOrigin = parent.mapSection ? mapOrigin : PlayerUtil.getPlayerLooking(parent.owner, SECTION_VIEW_DISTANCE);
        BlockPos blockOrigin = new BlockPos(Mth.floor(viewOrigin.x), Mth.floor(viewOrigin.y), Mth.floor(viewOrigin.z));

        for (Map.Entry<BlockPos, BlockState> blockEntry : parent.rotation.rotated(templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos worldPos = new BlockPos(blockOrigin.getX() + relativePos.getX(),
                    blockOrigin.getY() + relativePos.getY(),
                    blockOrigin.getZ() + relativePos.getZ());
            Tuple<BlockState, Optional<BlockEntity>> block = manager.getTopLayer().get(worldPos);
            if (BlockUtil.isMultiBlock(parent.level, worldPos)||!checkCanPlace(worldPos, block.getA())) {
                return false;
            }
        }
        
        changedBlocks.forEach(pos -> {
            Tuple<BlockState, Optional<BlockEntity>> block = manager.getViewLayer().remove(pos);
            manager.getLayer(parent.getRunningLayer()).set(pos, block);
        });

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
        Vec3 viewOrigin = parent.mapSection ? mapOrigin : PlayerUtil.getPlayerLooking(parent.owner, SECTION_VIEW_DISTANCE);
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

    public void move(int x, int y, int z){
        this.relativeOrigin = new Vec3(x, y, z);
        Vec3 newOrigin = origin.add(x, y, z);
        BlockPos newBlockOrigin = new BlockPos(Mth.floor(newOrigin.x), Mth.floor(newOrigin.y), Mth.floor(newOrigin.z));

        parent.init();
        parent.setRunningLayer(LayerType.TOP);
        changedBlocks.forEach(pos -> manager.getTopLayer().remove(pos));
        changedBlocks.clear();
        for (Map.Entry<BlockPos, BlockState> blockEntry : parent.rotation.rotated(templateBlocks).entrySet()) {
            BlockPos relativePos = blockEntry.getKey();
            BlockPos newPos = new BlockPos(newBlockOrigin.getX() + relativePos.getX(),
                    newBlockOrigin.getY() + relativePos.getY(),
                    newBlockOrigin.getZ() + relativePos.getZ());
            if (!BlockUtil.isMultiBlock(parent.level, newPos)){
                manager.getTopLayer().set(newPos, blockEntry.getValue());
                changedBlocks.add(newPos);
            }
        }
        for (Tuple<Vec3, Entity> entityTuple : entities.entities) {
            Entity entity = entityTuple.getB();
            entity.moveTo(newBlockOrigin.getX(), newBlockOrigin.getY(), newBlockOrigin.getZ());
        }
    }

    public List<BlockPos> getBlocksPos() {return new ArrayList<>(changedBlocks);}
    public Tuple<BlockState, Optional<BlockEntity>> getBlock(BlockPos pos) {
        return manager.getLayer(parent.getRunningLayer()).get(pos);
    }
    public HashMap<BlockPos, BlockState> getBlocks() {
        HashMap<BlockPos, BlockState> blocks = new HashMap<>();
        getBlocksPos().forEach(pos -> blocks.put(pos, getBlock(pos).getA()));
        return blocks;
    }

    private void createPoint(Vec3 pos, Vec3 color){
        level.sendParticles(new DustParticleOptions(color.toVector3f(), 0.5f), pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
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
            itemEntity.kill();
        }

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            player.connection.send(PlayerUtil.createInventoryUpdatePacket(player,i));
        }
    }

    public void remove() {
        changedBlocks.forEach(pos -> manager.getLayer(parent.getRunningLayer()).remove(pos));
        entities.entities.forEach((entry) -> entry.getB().kill());
    }

    public void setOrigin(Vec3 origin) {
        this.origin = origin;
    }

    public Vec3 getOrigin() {
        return origin.add(relativeOrigin);
    }

    public void setMapOrigin(Vec3 origin){
        if (parent.mapSection) {
            this.mapOrigin = origin;
            this.origin = origin;
        };
    }

    public boolean containsBlock(BlockPos pos) {
        return changedBlocks.contains(pos);
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
