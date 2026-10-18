package com.dankscripts.sgjdestiny_dhd.entity;

import com.dankscripts.sgjdestiny_dhd.block.DestinyShuttleAirlockBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyPilotSeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Short-lived, invisible mount that puts a player on the modeled pilot cushion. */
public final class DestinyPilotSeatEntity extends Entity {
    private BlockPos anchor;
    private BlockPos dismountPos;
    private Direction facing = Direction.NORTH;

    public DestinyPilotSeatEntity(EntityType<? extends DestinyPilotSeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public void setAnchor(BlockPos anchor, Direction facing) {
        this.anchor = anchor.immutable();
        this.facing = facing;
    }

    public BlockPos getShuttleAnchor() {
        return anchor;
    }

    /** Passenger mounts use this entity type too; only the forward chair may navigate. */
    public boolean isPilotAt(BlockPos shuttleAnchor, Direction shuttleFacing) {
        return shuttleAnchor.equals(anchor)
                && distanceToSqr(Vec3.atCenterOf(DestinyPilotSeatBlock.lowerPos(shuttleAnchor, shuttleFacing))) < 4.0;
    }

    public void setDismountPos(BlockPos dismountPos) {
        this.dismountPos = dismountPos.immutable();
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty() || anchor == null
                || !(level().getBlockState(anchor).getBlock() instanceof DestinyShuttleAirlockBlock)
                || level().getBlockState(anchor).getValue(DestinyShuttleAirlockBlock.FACING) != facing)) {
            discard();
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (anchor == null) return position();
        BlockPos aisle = dismountPos == null ? anchor.relative(facing, 18).above() : dismountPos;
        // The cockpit deck occupies the bottom quarter of this block. Placing
        // feet at the block's base embeds the passenger inside that deck.
        var deckShape = level().getBlockState(aisle).getCollisionShape(level(), aisle);
        double deckTop = deckShape.isEmpty() ? 0.0 : deckShape.max(Direction.Axis.Y);
        return Vec3.atBottomCenterOf(aisle).add(0.0, deckTop, 0.0);
    }
}
