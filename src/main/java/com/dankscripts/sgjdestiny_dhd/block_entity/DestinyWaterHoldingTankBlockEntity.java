package com.dankscripts.sgjdestiny_dhd.block_entity;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Persistent water store for the 3x3x3 ship tank. Touching tanks share fluid access. */
public final class DestinyWaterHoldingTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 108_000;
    private static final int MONITOR_RADIUS = 256;
    private static final Direction[] HORIZONTAL = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final WeakHashMap<ServerLevel, Set<BlockPos>> LOADED_TANKS = new WeakHashMap<>();

    public record Reading(int waterBuckets, int capacityBuckets) {
        public boolean present() { return capacityBuckets > 0; }
        public int percent() { return capacityBuckets == 0 ? 0
                : (int) (100L * waterBuckets / capacityBuckets); }
    }

    /** Only loaded tanks on this ship are monitored; no distant chunks are forced to load. */
    public static Reading reading(ServerLevel level, BlockPos console) {
        long waterBuckets = 0;
        long capacityBuckets = 0;
        for (BlockPos pos : LOADED_TANKS.getOrDefault(level, Set.of())) {
            if (pos.distSqr(console) > (long) MONITOR_RADIUS * MONITOR_RADIUS) continue;
            if (level.getBlockEntity(pos) instanceof DestinyWaterHoldingTankBlockEntity tank) {
                waterBuckets += tank.waterAmount() / 1000;
                capacityBuckets += tank.capacity() / 1000;
            }
        }
        return new Reading(bounded(waterBuckets), bounded(capacityBuckets));
    }

    private static int bounded(long amount) { return (int) Math.min(Integer.MAX_VALUE, amount); }

    /** A connected bank follows touching vertical tank faces, including offset placements. */
    private List<DestinyWaterHoldingTankBlockEntity> connectedTanks() {
        if (!(level instanceof ServerLevel server)) return List.of(this);
        Set<BlockPos> loaded = LOADED_TANKS.get(server);
        if (loaded == null || !loaded.contains(worldPosition)) return List.of(this);
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        List<DestinyWaterHoldingTankBlockEntity> members = new ArrayList<>();
        visited.add(worldPosition);
        pending.add(worldPosition);
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (!loaded.contains(pos) || !server.hasChunkAt(pos)
                    || !(server.getBlockEntity(pos) instanceof DestinyWaterHoldingTankBlockEntity tank)
                    || tank.isRemoved()) continue;
            members.add(tank);
            for (Direction direction : HORIZONTAL)
                for (int vertical = -2; vertical <= 2; vertical++)
                    for (int lateral = -2; lateral <= 2; lateral++) {
                        BlockPos next = direction.getAxis() == Direction.Axis.X
                                ? pos.offset(direction.getStepX() * 3, vertical, lateral)
                                : pos.offset(lateral, vertical, direction.getStepZ() * 3);
                        if (loaded.contains(next) && visited.add(next)) pending.addLast(next);
                    }
        }
        if (members.isEmpty()) return List.of(this);
        members.sort(Comparator.comparingLong(tank -> tank.worldPosition.asLong()));
        return members;
    }

    public int bankWaterAmount() {
        long amount = 0;
        for (DestinyWaterHoldingTankBlockEntity tank : connectedTanks()) amount += tank.waterAmount();
        return bounded(amount);
    }

    public int bankCapacity() { return bounded((long) connectedTanks().size() * CAPACITY); }

    private void updateBankSignals() {
        if (level == null) return;
        for (DestinyWaterHoldingTankBlockEntity tank : connectedTanks())
            level.updateNeighbourForOutputSignal(tank.worldPosition, tank.getBlockState().getBlock());
    }

    private final FluidTank water = new FluidTank(CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    private final IFluidHandler bankWater = new IFluidHandler() {
        @Override public int getTanks() { return 1; }

        @Override public FluidStack getFluidInTank(int tank) {
            int amount = tank == 0 ? bankWaterAmount() : 0;
            return amount == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, amount);
        }

        @Override public int getTankCapacity(int tank) { return tank == 0 ? bankCapacity() : 0; }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && stack.getFluid() == Fluids.WATER;
        }

        @Override public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != Fluids.WATER) return 0;
            int remaining = resource.getAmount();
            for (DestinyWaterHoldingTankBlockEntity tank : connectedTanks()) {
                if (remaining == 0) break;
                remaining -= tank.water.fill(new FluidStack(Fluids.WATER, remaining), action);
            }
            int filled = resource.getAmount() - remaining;
            if (filled > 0 && action.execute()) updateBankSignals();
            return filled;
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            return resource.isEmpty() || resource.getFluid() != Fluids.WATER
                    ? FluidStack.EMPTY : drain(resource.getAmount(), action);
        }

        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            int remaining = maxDrain;
            for (DestinyWaterHoldingTankBlockEntity tank : connectedTanks()) {
                if (remaining == 0) break;
                remaining -= tank.water.drain(remaining, action).getAmount();
            }
            int drained = maxDrain - remaining;
            if (drained > 0 && action.execute()) updateBankSignals();
            return drained == 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, drained);
        }
    };
    public DestinyWaterHoldingTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESTINY_WATER_HOLDING_TANK.get(), pos, state);
    }

    public int waterAmount() { return water.getFluidAmount(); }
    public int capacity() { return water.getCapacity(); }
    public IFluidHandler fluidHandler() { return bankWater; }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server)
            LOADED_TANKS.computeIfAbsent(server, unused -> new HashSet<>()).add(worldPosition.immutable());
    }

    @Override public void setRemoved() {
        if (level instanceof ServerLevel server) {
            Set<BlockPos> tanks = LOADED_TANKS.get(server);
            if (tanks != null) tanks.remove(worldPosition);
        }
        super.setRemoved();
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("WaterTank", water.writeToNBT(registries, new CompoundTag()));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(registries, tag.getCompound("WaterTank"));
    }
}
