package com.dankscripts.sgjdestiny_dhd.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.common.sgjourney.AddressRegion;
import net.povstalec.sgjourney.common.sgjourney.Galaxy;
import net.povstalec.sgjourney.common.sgjourney.SpaceLocation;
import net.povstalec.sgjourney.common.sgjourney.stargate.Stargate;
import net.povstalec.sgjourney.common.data.Universe;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.init.StargateInit;

/** Ensures SGJourney's built-in Destiny space location is joined to our seed-ship route. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinyRouteEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Route");
    private static final ResourceKey<Galaxy> MEDIORA = ResourceKey.create(Galaxy.REGISTRY_KEY,
            new ResourceLocation("sgjdestiny_dhd", "mediora"));
    private static final Destination[] DESTINATIONS = {
            new Destination("calcite_planet", 0, 0),
            new Destination("water_planet", 0, 0),
            new Destination("jungle_planet", 0, 8),
            new Destination("justice_planet", 0, 0)
    };

    private DestinyRouteEvents() {}

    @SubscribeEvent
    public static void linkRoute(ServerStartedEvent event) {
        ensureRoutes(event.getServer());
    }

    public static void ensureRoutes(MinecraftServer server) {
        ensureMedioraGalaxy(server);
        link(server, "destiny", "destiny_route");
        for (Destination destination : DESTINATIONS)
            link(server, destination.name(), destination.name());
        // Eden belongs to Mediora, but canonically has no Stargate. It is
        // linked to the galaxy without joining the gate repair/dialer path.
        link(server, "eden", "eden");
        // Stargate#getAddressRegion reads SGJourney's saved Universe mapping,
        // rather than the SpaceLocation object itself. Refresh that mapping
        // after applying our overrides so existing gates gain the new routes.
        Universe.get(server).assignSpaceLocationsToAddressRegions();
        selectLowestJusticeGate(server);
        for (Destination destination : DESTINATIONS)
            repairDestinationGateRecords(server, destination);
    }

    /** Makes the original ground-level controller authoritative if old dev builds stacked duplicates. */
    private static void selectLowestJusticeGate(MinecraftServer server) {
        Destination destination = DESTINATIONS[DESTINATIONS.length - 1];
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("sgjourney", destination.name()));
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;
        int centerX = (destination.chunkX() << 4) + 8;
        int centerZ = (destination.chunkZ() << 4) + 8;
        for (int chunkX = -2; chunkX <= 2; chunkX++)
            for (int chunkZ = -2; chunkZ <= 2; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, true);
                level.getChunk(chunkX, chunkZ);
            }
        List<AbstractStargateEntity<?>> controllers = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = centerX - 32; x <= centerX + 32; x++)
            for (int z = centerZ - 32; z <= centerZ + 32; z++)
                for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                    cursor.set(x, y, z);
                    if (level.getBlockEntity(cursor) instanceof AbstractStargateEntity<?> gate)
                        controllers.add(gate);
                }
        if (controllers.size() <= 1) return;
        controllers.sort(Comparator.comparingInt(gate -> gate.getBlockPos().getY()));
        AbstractStargateEntity<?> lowest = controllers.get(0);
        StargateNetwork network = StargateNetwork.get(server);
        for (Stargate gate : new ArrayList<>(network.getStargatesInDimension(dimension)))
            if (gate.getStargateType() == StargateInit.UNIVERSE.get()) network.removeStargate(gate);
        lowest.setPrimary();
        lowest.generate();
        LOGGER.warn("Selected the lowest of {} stacked Justice Planet gate controllers at {} as the destination",
                controllers.size(), lowest.getBlockPos());
    }

    private static void ensureMedioraGalaxy(MinecraftServer server) {
        Universe universe = Universe.get(server);
        if (universe.hasGalaxy(MEDIORA)) return;
        Galaxy definition = server.registryAccess().registryOrThrow(Galaxy.REGISTRY_KEY).get(MEDIORA);
        if (definition == null) {
            LOGGER.error("Mediora galaxy definition is missing from the datapack registry");
            return;
        }
        // SGJourney registers datapack galaxies only when a Universe save is
        // first created. Add Mediora explicitly for worlds that predate it.
        universe.addGalaxy(MEDIORA, definition);
        LOGGER.info("Registered Mediora in the existing SGJourney Universe save");
    }

    private static void link(MinecraftServer server, String dimensionName, String regionName) {
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("sgjourney", dimensionName));
        ResourceKey<AddressRegion> regionKey = ResourceKey.create(AddressRegion.REGISTRY_KEY,
                new ResourceLocation("sgjourney", regionName));
        AddressRegion region = server.registryAccess().registryOrThrow(AddressRegion.REGISTRY_KEY).get(regionKey);
        if (region == null) return;
        // Existing worlds retain their serialized copy of these regions. Migrate
        // an older Kaliem route exactly once so it receives Mediora's address.
        Universe universe = Universe.get(server);
        var savedGalaxies = universe.getGalaxiesFromAddressRegionKey(regionKey);
        if (savedGalaxies == null || !savedGalaxies.containsKey(MEDIORA)) {
            AddressRegion savedRegion = universe.getAddressRegionFromKey(regionKey);
            if (savedRegion != null) universe.removeAddressRegion(savedRegion);
            universe.addAddressRegionFromDataPack(regionKey, region);
            LOGGER.info("Migrated {} into the Mediora galaxy", regionKey.location());
        }
        // Use the Universe's effective saved instance. Existing saves may have
        // just migrated from Kaliem, and attaching the datapack instance here
        // leaves Stargate#getAddressRegion disconnected from Mediora.
        AddressRegion effectiveRegion = universe.getAddressRegionFromKey(regionKey);
        if (effectiveRegion == null) effectiveRegion = region;
        SpaceLocation location = SpaceLocation.fromDimension(server, dimension);
        location.setAddressRegion(effectiveRegion);
        SpaceLocation.addSpaceLocation(dimension, location);
    }

    /**
     * Terrain revisions moved the generated planet gates while SGJourney retained
     * their old serialized records. Load the intended gate chunk before checking
     * records, retain the record whose block entity still exists and discard only
     * stale records. Otherwise routing can choose a ghost gate and report that the
     * target cannot be reached.
     */
    private static void repairDestinationGateRecords(MinecraftServer server, Destination destination) {
        String dimensionName = destination.name();
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("sgjourney", dimensionName));
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;

        // Our unique pedestal placement is anchored at a fixed chunk. A
        // newly added destination dimension may exist without ever generating
        // that chunk, leaving its valid address with no gate in the network.
        // Generate and retain the anchor chunk before inspecting gate records.
        level.setChunkForced(destination.chunkX(), destination.chunkZ(), true);
        level.getChunk(destination.chunkX(), destination.chunkZ());

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
        if (kept == 0 && placeMissingGate(level, destination)) {
            level.getChunk(destination.chunkX(), destination.chunkZ());
            kept = (int) network.getStargatesInDimension(dimension).stream()
                    .filter(gate -> gate.getStargateType() == StargateInit.UNIVERSE.get())
                    .filter(gate -> gate.getPosition() != null)
                    .count();
            LOGGER.info("Generated missing Universe Stargate for {} in permanently forced chunk {},{}; network now contains {} gate record(s)",
                    dimension.location(), destination.chunkX(), destination.chunkZ(), kept);
        }
        LOGGER.info("Validated {} destination gate records in {}: kept {}, removed {} stale records",
                kept + removed, dimension.location(), kept, removed);
    }

    private static boolean placeMissingGate(ServerLevel level, Destination destination) {
        ResourceLocation templateId = new ResourceLocation("sgjourney",
                "stargate/universe/pedestal/end/universe_stargate_pedestal_end_1");
        StructureTemplate template = level.getStructureManager().get(templateId).orElse(null);
        if (template == null) {
            LOGGER.error("Could not load Universe Stargate pedestal template {}", templateId);
            return false;
        }
        PlacementSite site = findFlattestPlacement(level, destination, template);
        int placementY = site.baseY() - (destination.name().equals("justice_planet") ? 3 : 0);
        BlockPos origin = new BlockPos(site.centerX() - template.getSize().getX() / 2, placementY,
                site.centerZ() - template.getSize().getZ() / 2);
        int minChunkX = origin.getX() >> 4;
        int maxChunkX = (origin.getX() + template.getSize().getX() - 1) >> 4;
        int minChunkZ = origin.getZ() >> 4;
        int maxChunkZ = (origin.getZ() + template.getSize().getZ() - 1) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
                level.setChunkForced(chunkX, chunkZ, true);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        ResourceLocation processorsId = new ResourceLocation("sgjourney", destination.name() + "_gate");
        StructureProcessorList processors = level.registryAccess().registryOrThrow(
                net.minecraft.core.registries.Registries.PROCESSOR_LIST).get(processorsId);
        if (processors != null) processors.list().forEach(settings::addProcessor);
        if (destination.name().equals("justice_planet")) {
            buildJusticeFoundation(level, origin, template);
        }
        boolean placed = template.placeInWorld(level, origin, origin, settings, level.random, 3);
        if (!placed) {
            LOGGER.error("Failed to place missing Universe Stargate template in {}", level.dimension().location());
            return false;
        }

        // Direct template placement does not run SGJourney's worldgen discovery
        // pass. Register the newly loaded controller block entity explicitly so
        // the address can be dialed during this same server session.
        int registered = 0;
        BlockPos size = new BlockPos(template.getSize());
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1,
                size.getY() - 1, size.getZ() - 1))) {
            if (level.getBlockEntity(pos) instanceof AbstractStargateEntity<?> gateEntity) {
                // This is SGJourney's normal post-structure initialization. It
                // assigns a valid nine-chevron identity before adding the gate
                // to the network; direct insertion rejects a blank identity.
                gateEntity.generate();
                registered++;
            }
        }
        if (registered == 0) {
            // Also asks SGJourney to discover controllers in loaded chunks; this
            // covers templates whose controller lies on a jigsaw boundary.
            StargateNetwork.findStargatesInLevel(level);
        }
        LOGGER.info("Registered {} Universe Stargate controller(s) after placing {}",
                registered, templateId);
        LOGGER.info("Placed {} gate near {},{} at y={} with sampled terrain variation {}",
                destination.name(), site.centerX(), site.centerZ(), placementY, site.variation());
        return placed;
    }

    private static PlacementSite findFlattestPlacement(ServerLevel level, Destination destination,
                                                        StructureTemplate template) {
        int anchorX = (destination.chunkX() << 4) + 8;
        int anchorZ = (destination.chunkZ() << 4) + 8;
        PlacementSite best = null;
        // Search chunk centers in expanding rings. Sampling the complete
        // pedestal footprint prevents a smooth-looking center point from being
        // chosen on the shoulder of a hill.
        for (int radius = 0; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int centerX = anchorX + dx * 16;
                    int centerZ = anchorZ + dz * 16;
                    PlacementSite candidate = scorePlacement(level, template, centerX, centerZ);
                    if (best == null || candidate.variation() < best.variation()) best = candidate;
                    if (candidate.variation() <= 1) return candidate;
                }
            }
            if (best != null && best.variation() <= 2) return best;
        }
        return best == null ? scorePlacement(level, template, anchorX, anchorZ) : best;
    }

    private static PlacementSite scorePlacement(ServerLevel level, StructureTemplate template,
                                                 int centerX, int centerZ) {
        int halfX = template.getSize().getX() / 2 + 1;
        int halfZ = template.getSize().getZ() / 2 + 1;
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int x = centerX - halfX; x <= centerX + halfX; x += 3)
            for (int z = centerZ - halfZ; z <= centerZ + halfZ; z += 3) {
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                minimum = Math.min(minimum, height);
                maximum = Math.max(maximum, height);
            }
        return new PlacementSite(centerX, centerZ, maximum, maximum - minimum);
    }

    /**
     * Supports the pedestal over gently sloping barren terrain. The structure
     * finder deliberately avoids cutting hills; this small additive terrace
     * fills only missing ground and tapers into the natural surface.
     */
    private static void buildJusticeFoundation(ServerLevel level, BlockPos origin,
                                                StructureTemplate template) {
        int minX = origin.getX();
        int maxX = origin.getX() + template.getSize().getX() - 1;
        int minZ = origin.getZ();
        int maxZ = origin.getZ() + template.getSize().getZ() - 1;
        int platformTop = origin.getY() - 1;
        int blendWidth = 4;

        for (int x = minX - blendWidth; x <= maxX + blendWidth; x++) {
            for (int z = minZ - blendWidth; z <= maxZ + blendWidth; z++) {
                int outsideX = Math.max(minX - x, Math.max(0, x - maxX));
                int outsideZ = Math.max(minZ - z, Math.max(0, z - maxZ));
                int ring = Math.max(outsideX, outsideZ);
                int naturalTop = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int targetTop = platformTop - ring;
                if (targetTop <= naturalTop) continue;

                for (int y = naturalTop + 1; y <= targetTop; y++) {
                    level.setBlock(new BlockPos(x, y, z),
                            y == targetTop ? Blocks.GRAVEL.defaultBlockState()
                                    : Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
    }

    private record Destination(String name, int chunkX, int chunkZ) {}
    private record PlacementSite(int centerX, int centerZ, int baseY, int variation) {}
}
