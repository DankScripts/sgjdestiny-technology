package com.dankscripts.sgjdestiny_dhd.block_entity;

import com.dankscripts.sgjdestiny_dhd.block.DestinyShipAirlockBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Animation state shared by rendering and the moving door-leaf collision. */
public final class DestinyShipAirlockBlockEntity extends BlockEntity {
    private float previousOpen;
    private float open;
    private long autoCloseAt;

    public DestinyShipAirlockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESTINY_SHIP_AIRLOCK.get(), pos, state);
        open = state.getValue(DestinyShipAirlockBlock.OPEN) ? 1.0F : 0.0F;
        previousOpen = open;
    }

    public static void tick(Level level, BlockPos pos, BlockState state,
                            DestinyShipAirlockBlockEntity entity) {
        entity.previousOpen = entity.open;
        float target = state.getValue(DestinyShipAirlockBlock.OPEN) ? 1.0F : 0.0F;
        entity.open = Mth.clamp(entity.open + Math.signum(target - entity.open) / 44.0F, 0.0F, 1.0F);
        if (Math.abs(entity.open - target) < 1.0F / 44.0F) entity.open = target;
    }

    public float getOpenProgress(float partialTick) {
        return Mth.lerp(partialTick, previousOpen, open);
    }

    public void scheduleAutoClose(long gameTime) {
        autoCloseAt = gameTime;
        setChanged();
    }

    public void cancelAutoClose() {
        autoCloseAt = 0;
        setChanged();
    }

    public long getAutoCloseAt() {
        return autoCloseAt;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("AutoCloseAt", autoCloseAt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        autoCloseAt = tag.getLong("AutoCloseAt");
    }

    public AABB getRenderBoundingBox() {
        // Include the 3-block frame and both leaves at full travel, without
        // keeping a large cube around every airlock in the camera frustum.
        Direction right = getBlockState().getValue(DestinyShipAirlockBlock.FACING).getClockWise();
        BlockPos low = worldPosition.relative(right, -2);
        BlockPos high = worldPosition.relative(right, 5);
        return new AABB(Math.min(low.getX(), high.getX()), worldPosition.getY(),
                Math.min(low.getZ(), high.getZ()), Math.max(low.getX(), high.getX()) + 1,
                worldPosition.getY() + 3, Math.max(low.getZ(), high.getZ()) + 1);
    }
}
