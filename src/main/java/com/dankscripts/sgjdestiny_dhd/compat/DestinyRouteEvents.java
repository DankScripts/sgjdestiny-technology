package com.dankscripts.sgjdestiny_dhd.compat;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.common.sgjourney.AddressRegion;
import net.povstalec.sgjourney.common.sgjourney.SpaceLocation;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;
import net.povstalec.sgjourney.common.data.Universe;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.init.StargateInit;

/** Ensures SGJourney's built-in Destiny space location is joined to our seed-ship route. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinyRouteEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Route");

    private DestinyRouteEvents() {}

    @SubscribeEvent
    public static void linkRoute(ServerStartedEvent event) {
        ensureRoutes(event.getServer());
    }

    public static void ensureRoutes(MinecraftServer server) {
        link(server, "destiny", "destiny_route");
        link(server, "calcite_planet", "calcite_planet");
        link(server, "water_planet", "water_planet");
        link(server, "jungle_planet", "jungle_planet");
        // Stargate#getAddressRegion reads SGJourney's saved Universe mapping,
        // rather than the SpaceLocation object itself. Refresh that mapping
        // after applying our overrides so existing gates gain the new routes.
        Universe.get(server).assignSpaceLocationsToAddressRegions();
        repairDestinationGateRecords(server, "calcite_planet");
        repairDestinationGateRecords(server, "water_planet");
        repairDestinationGateRecords(server, "jungle_planet");
    }

    private static void link(MinecraftServer server, String dimensionName, String regionName) {
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("sgjourney", dimensionName));
        ResourceKey<AddressRegion> regionKey = ResourceKey.create(AddressRegion.REGISTRY_KEY,
                new ResourceLocation("sgjourney", regionName));
        AddressRegion region = server.registryAccess().registryOrThrow(AddressRegion.REGISTRY_KEY).get(regionKey);
        if (region == null) return;
        // Existing worlds keep a serialized Universe which predates regions added
        // by this mod. Import the datapack region into that saved Universe first;
        // this also inserts its galactic address into the saved Kaliem galaxy.
        Universe.get(server).addAddressRegionFromDataPack(regionKey, region);
        SpaceLocation location = SpaceLocation.fromDimension(server, dimension);
        location.setAddressRegion(region);
        SpaceLocation.addSpaceLocation(dimension, location);
    }

    /**
     * Terrain revisions moved the generated planet gates while SGJourney retained
     * their old serialized records. Load the intended gate chunk before checking
     * records, retain the record whose block entity still exists and discard only
     * stale records. Otherwise routing can choose a ghost gate and report that the
     * target cannot be reached.
     */
    private static void repairDestinationGateRecords(MinecraftServer server, String dimensionName) {
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("sgjourney", dimensionName));
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;

        // Our unique pedestal placement is anchored at a fixed chunk. A
        // newly added destination dimension may exist without ever generating
        // that chunk, leaving its valid address with no gate in the network.
        // Generate and retain the anchor chunk before inspecting gate records.
        int anchorChunkZ = "jungle_planet".equals(dimensionName) ? 8 : 0;
        level.setChunkForced(0, anchorChunkZ, true);
        level.getChunk(0, anchorChunkZ);

        StargateNetwork network = StargateNetwork.get(server);
        int kept = 0;
        int removed = 0;
        for (Stargate gate : new ArrayList<>(network.getStargatesInDimension(dimension))) {
            if (gate.getStargateType() != StargateInit.UNIVERSE.get()) continue;
            if (gate.getPosition() == null) {
                network.removeStargate(gate);
                removed++;
                LOGGER.warn("Removed malformed destination gate record with no position from {}",
                        dimension.location());
                continue;
            }
            BlockPos pos = BlockPos.containing(gate.getPosition());
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            level.setChunkForced(chunkX, chunkZ, true);
            level.getChunk(chunkX, chunkZ);
            try {
                if (gate.checkValidity()) {
                    kept++;
                } else {
                    network.removeStargate(gate);
                    removed++;
                }
            } catch (RuntimeException exception) {
                network.removeStargate(gate);
                removed++;
                LOGGER.warn("Removed unreadable destination gate record at {} in {}",
                        pos, dimension.location(), exception);
            }
        }
        LOGGER.info("Validated {} destination gate records in {}: kept {}, removed {} stale records",
                kept + removed, dimension.location(), kept, removed);
    }
}
