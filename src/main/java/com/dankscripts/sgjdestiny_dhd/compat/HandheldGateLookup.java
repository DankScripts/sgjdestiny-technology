package com.dankscripts.sgjdestiny_dhd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.data.BlockEntityList;
import net.povstalec.sgjourney.common.sgjourney.stargate.universe.UniverseBlockEntityStargate;

import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

public final class HandheldGateLookup {
    private HandheldGateLookup() {}

    public static UniverseStargateEntity nearest(ServerLevel level, Vec3 origin) {
        return registered(level)
                .sorted(Comparator.comparingDouble(record -> record.getBlockPos().getCenter().distanceToSqr(origin)))
                .map(record -> resolve(level, record))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public static UniverseStargateEntity at(ServerLevel level, BlockPos target) {
        return registered(level)
                .filter(record -> target.equals(record.getBlockPos()))
                .map(record -> resolve(level, record))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private static Stream<UniverseBlockEntityStargate> registered(ServerLevel level) {
        return BlockEntityList.get(level).getStargates().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof UniverseBlockEntityStargate record
                        && level.dimension().equals(record.getDimension())
                        && entry.getKey().equals(record.get9ChevronAddress())
                        && record.getBlockPos() != null
                        && !level.isOutsideBuildHeight(record.getBlockPos()))
                .map(entry -> (UniverseBlockEntityStargate) entry.getValue());
    }

    private static UniverseStargateEntity resolve(ServerLevel level, UniverseBlockEntityStargate record) {
        BlockPos pos = record.getBlockPos();
        level.getChunkAt(pos);
        if (level.getBlockEntity(pos) instanceof UniverseStargateEntity gate
                && record.get9ChevronAddress().equals(gate.get9ChevronAddress())
                && gate.getStargate() == record) return gate;
        return null;
    }
}
