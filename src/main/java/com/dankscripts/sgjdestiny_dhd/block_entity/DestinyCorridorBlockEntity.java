package com.dankscripts.sgjdestiny_dhd.block_entity;

import com.dankscripts.sgjdestiny_dhd.block.DestinyCorridorBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Render anchor for the long OBJ mesh; collision lives in local helper cells. */
public final class DestinyCorridorBlockEntity extends BlockEntity {
    private boolean checkedLegacyFootprint;

    public DestinyCorridorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESTINY_CORRIDOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  DestinyCorridorBlockEntity entity) {
        if (!(state.getBlock() instanceof DestinyCorridorBlock corridor)) return;
        if (!entity.checkedLegacyFootprint) {
            entity.checkedLegacyFootprint = true;
            corridor.rebuildLegacyFootprint(level, pos, state);
            corridor.refreshCollisionFacings(level, pos, state.getValue(DestinyCorridorBlock.FACING));
        }
        // A removed controller leaves air in its socket. Restore the selectable
        // mount so the next controller can be placed on the same backing plate.
        if (level.getGameTime() % 20 == 0) {
            Direction forward = state.getValue(DestinyCorridorBlock.FACING);
            corridor.refreshControlMounts(level, pos, forward);
            corridor.refreshEntranceHandle(level, pos, forward);
        }
    }

    public AABB getRenderBoundingBox() {
        // Enclose the actual 5 x 4 x 14 mesh so camera culling can skip
        // corridors behind the player without clipping their far ends.
        Direction forward = getBlockState().getValue(DestinyCorridorBlock.FACING);
        BlockPos far = worldPosition.relative(forward.getCounterClockWise(), 4)
                .relative(forward, 13).above(3);
        return new AABB(Math.min(worldPosition.getX(), far.getX()) - 1,
                worldPosition.getY() - 1,
                Math.min(worldPosition.getZ(), far.getZ()) - 1,
                Math.max(worldPosition.getX(), far.getX()) + 2,
                worldPosition.getY() + 5,
                Math.max(worldPosition.getZ(), far.getZ()) + 2);
    }
}
