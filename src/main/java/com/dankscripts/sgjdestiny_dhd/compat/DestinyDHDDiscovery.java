package com.dankscripts.sgjdestiny_dhd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;

import javax.annotation.Nullable;

/** Native capability discovery restricted to already loaded chunks. */
public final class DestinyDHDDiscovery {
    private DestinyDHDDiscovery() {}

    @Nullable
    public static DHDControlledStargate findNearest(ServerLevel level, AbstractDHDEntity<?> dhd) {
        BlockPos origin = dhd.getBlockPos();
        int range = (int) Math.ceil(dhd.getMaxConnectionDistance());
        int minX = SectionPos.blockToSectionCoord(origin.getX() - range);
        int maxX = SectionPos.blockToSectionCoord(origin.getX() + range);
        int minZ = SectionPos.blockToSectionCoord(origin.getZ() - range);
        int maxZ = SectionPos.blockToSectionCoord(origin.getZ() + range);
        double closestDistance = Double.POSITIVE_INFINITY;
        DHDControlledStargate closest = null;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    double distance = origin.distSqr(pos);
                    if (distance >= closestDistance || distance > dhd.getMaxConnectionDistanceSqr()) continue;
                    var gate = level.getCapability(
                            DHDControlledStargate.DHD_CONTROLLED_STARGATE_CAPABILITY_BLOCK, pos, null);
                    if (gate != null && DestinyHardwiredDHD.canDiscover(distance,
                            dhd.getMaxConnectionDistance(), gate.controllerCache().isCached())) {
                        closestDistance = distance;
                        closest = gate;
                    }
                }
            }
        }
        return closest;
    }
}
