package com.dankscripts.sgjdestiny_dhd.block_entity;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyAtmosphere;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

/** Fixed power reserve, live loaded-tank water, and Destiny's live CO2 level. */
public final class DestinyControlHubBlockEntity extends BlockEntity {
    private static final int POWER_RESERVES = 27;
    private final int[] simulatedStatus = {POWER_RESERVES, 0, 25};
    private final ContainerData statusData = new ContainerData() {
        @Override public int get(int index) {
            if (index == 0) return POWER_RESERVES;
            if (index == 3) return level instanceof ServerLevel server && DestinyAtmosphere.isDestiny(server) ? 1 : 0;
            if (index == 4) return level instanceof ServerLevel server && DestinyAtmosphere.isDestiny(server)
                    ? DestinyAtmosphere.scrubberStatus(server) : 0;
            if (index == 8) return level instanceof ServerLevel server && DestinyAtmosphere.isDestiny(server)
                    ? DestinyAtmosphere.loadedScrubberCount(server) : 0;
            if (index == 2 && level instanceof ServerLevel server && DestinyAtmosphere.isDestiny(server))
                return DestinyAtmosphere.co2Percent(server);
            if (level instanceof ServerLevel server && index >= 5 && index <= 7) {
                var tanks = DestinyWaterHoldingTankBlockEntity.reading(server, worldPosition);
                return switch (index) {
                    case 5 -> tanks.present() ? 1 : 0;
                    case 6 -> tanks.waterBuckets();
                    default -> tanks.capacityBuckets();
                };
            }
            if (index == 1) return level instanceof ServerLevel server
                    ? DestinyWaterHoldingTankBlockEntity.reading(server, worldPosition).percent() : 0;
            return index >= 0 && index < 3 ? simulatedStatus[index] : 0;
        }
        @Override public void set(int index, int value) { setReading(index, value); }
        @Override public int getCount() { return 9; }
    };

    public DestinyControlHubBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESTINY_CONTROL_HUB.get(), pos, state);
    }

    public ContainerData statusData() { return statusData; }

    public void setReading(int index, int value) {
        if (index != 2) return;
        int bounded = Math.max(0, Math.min(100, value));
        if (simulatedStatus[index] == bounded) return;
        simulatedStatus[index] = bounded;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putIntArray("SimulatedShipStatus", simulatedStatus);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int[] values = tag.getIntArray("SimulatedShipStatus");
        if (values.length == 3)
            for (int i = 0; i < 3; i++) simulatedStatus[i] = Math.max(0, Math.min(100, values[i]));
        simulatedStatus[0] = POWER_RESERVES;
        simulatedStatus[1] = 0;
    }
}
