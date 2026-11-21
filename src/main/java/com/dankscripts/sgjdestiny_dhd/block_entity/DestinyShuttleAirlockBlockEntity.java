package com.dankscripts.sgjdestiny_dhd.block_entity;

import com.dankscripts.sgjdestiny_dhd.block.DestinyShuttleAirlockBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Client-side motion for the two sliding airlock panels. */
public final class DestinyShuttleAirlockBlockEntity extends BlockEntity {
    private float previousOpen;
    private float open;
    private boolean collisionRestored;

    public DestinyShuttleAirlockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESTINY_SHUTTLE_AIRLOCK.get(), pos, state);
        open = state.getValue(DestinyShuttleAirlockBlock.OPEN) ? 1.0F : 0.0F;
        previousOpen = open;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  DestinyShuttleAirlockBlockEntity entity) {
        entity.previousOpen = entity.open;
        float target = state.getValue(DestinyShuttleAirlockBlock.OPEN) ? 1.0F : 0.0F;
        entity.open = Mth.clamp(entity.open + Math.signum(target - entity.open) / 44.0F, 0.0F, 1.0F);
        if (Math.abs(entity.open - target) < 1.0F / 44.0F) entity.open = target;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  DestinyShuttleAirlockBlockEntity entity) {
        if (!entity.collisionRestored && state.getBlock() instanceof DestinyShuttleAirlockBlock) {
            DestinyShuttleAirlockBlock.restoreExteriorCollision(level, pos, state.getValue(DestinyShuttleAirlockBlock.FACING));
            entity.collisionRestored = true;
        }
    }

    public float getOpenProgress(float partialTick) {
        return Mth.lerp(partialTick, previousOpen, open);
    }

    public AABB getRenderBoundingBox() {
        // The hull is broad but directional. Keep the full nose and wings in
        // view without treating a 68-block cube around the hatch as visible.
        Direction facing = getBlockState().getValue(DestinyShuttleAirlockBlock.FACING);
        Direction right = facing.getClockWise();
        BlockPos a = worldPosition.relative(right, -12).relative(facing, -2);
        BlockPos b = worldPosition.relative(right, 13).relative(facing, 26);
        return new AABB(Math.min(a.getX(), b.getX()), worldPosition.getY(),
                Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()) + 1,
                worldPosition.getY() + 9, Math.max(a.getZ(), b.getZ()) + 1);
    }
}
