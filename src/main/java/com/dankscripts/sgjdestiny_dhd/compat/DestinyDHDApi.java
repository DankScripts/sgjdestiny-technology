package com.dankscripts.sgjdestiny_dhd.compat;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.povstalec.sgjourney.api.stargate.BlockEntityStargate;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;

public final class DestinyDHDApi {
    private DestinyDHDApi() {}

    public static DHDControlledStargate connectedStargate(AbstractDHDEntity dhd) {
        var adapter = dhd.getDHD();
        if (adapter == null) {
            return null;
        }
        return adapter.getStargate();
    }

    public static AbstractStargateEntity<?> connectedStargateEntity(AbstractDHDEntity dhd) {
        DHDControlledStargate stargate = connectedStargate(dhd);
        if (stargate instanceof BlockEntityStargate<?> blockEntityStargate) {
            BlockEntity blockEntity = blockEntityStargate.getStargateBlockEntity();
            if (blockEntity instanceof AbstractStargateEntity<?> entity) {
                return entity;
            }
        }
        return null;
    }
}
