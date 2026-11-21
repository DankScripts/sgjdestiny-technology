package com.dankscripts.sgjdestiny_dhd.compat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.IntBinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.blocks.stargate.AbstractStargateBaseBlock;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.povstalec.sgjourney.common.sgjourney.AddressRegion;
import net.povstalec.sgjourney.common.sgjourney.Galaxy;
import net.povstalec.sgjourney.common.sgjourney.SpaceLocation;
import net.povstalec.sgjourney.api.stargate.BlockEntityStargate;
import net.povstalec.sgjourney.api.stargate.Stargate;
import net.povstalec.sgjourney.common.data.Universe;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.init.StargateInit;
import net.povstalec.sgjourney.common.init.BlockInit;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.data.BlockEntityList;
import net.povstalec.sgjourney.common.sgjourney.Address;

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
        preloadDestinyGate(server);
        for (Destination destination : DESTINATIONS)
            repairDestinationGateRecords(server, destination);
    }

    private static void preloadDestinyGate(MinecraftServer server) {
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("sgjourney", "destiny"));
        ServerLevel level = server.getLevel(dimension);
        if (level == null || !StargateNetwork.get(server).getStargatesInDimension(dimension).isEmpty()) return;
        LOGGER.info("Discovering the Destiny Stargate using SGJourney's structure search");
        StargateNetwork.findStargatesInLevel(level);
        if (StargateNetwork.get(server).getStargatesInDimension(dimension).isEmpty())
            LOGGER.warn("No Destiny Stargate was registered by SGJourney's structure search");
    }

    private static void ensureMedioraGalaxy(MinecraftServer server) {
        Universe universe = Universe.get(server);
        Galaxy existing = universe.getGalaxy(MEDIORA);
        if (existing != null && MEDIORA.equals(existing.getResourceKey())) return;
        Galaxy definition = server.registryAccess().registryOrThrow(Galaxy.REGISTRY_KEY).get(MEDIORA);
        if (definition == null) {
            LOGGER.error("Mediora galaxy definition is missing from the datapack registry");
            return;
        }
        // SGJourney registers datapack galaxies only when a Universe save is
        // first created. Add Mediora explicitly for worlds that predate it.
        // Registry values are keyless templates. SGJourney's own registration
        // copies them with a key before any region is attached. Using the raw
        // template creates a null galaxy entry in every attached region.
        var retained = new java.util.LinkedHashMap<net.povstalec.sgjourney.common.sgjourney.Address.Immutable, AddressRegion>();
        if (existing != null) {
            for (var entry : existing.getAddressRegions(e -> true)) retained.put(entry.getKey(), entry.getValue());
            universe.getGalaxiesWithPrefix(existing.symbolPrefix).remove(existing);
        }
        universe.addGalaxy(MEDIORA, definition.copyTemplateWithKey(MEDIORA, retained));
        universe.setDirty();
        LOGGER.info("Registered Mediora in the existing SGJourney Universe save");
    }

    private static void link(MinecraftServer server, String dimensionName, String regionName) {
        ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation("sgjourney", dimensionName));
        ResourceKey<AddressRegion> regionKey = ResourceKey.create(AddressRegion.REGISTRY_KEY,
                new ResourceLocation("sgjourney", regionName));
        AddressRegion region = server.registryAccess().registryOrThrow(AddressRegion.REGISTRY_KEY).get(regionKey);
        if (region == null) return;
        // Seven-chevron addresses belong to the saved region, not the physical gate.
        Universe universe = Universe.get(server);
        if (universe.getAddressRegionFromKey(regionKey) == null)
            universe.addAddressRegionFromDataPack(regionKey, region);
        AddressRegion effectiveRegion = universe.getAddressRegionFromKey(regionKey);
        if (effectiveRegion == null) {
            LOGGER.error("Could not register route region {}; refusing to attach a keyless template", regionKey.location());
            return;
        }
        // Repair the null-key residue and the galaxy's reverse lookup together.
        // Keep saved addresses and existing physical Stargates intact.
        Galaxy galaxy = universe.getGalaxy(MEDIORA);
        if (galaxy != null && DestinyRouteRepair.repair(galaxy, effectiveRegion, region.getAddressInGalaxy(MEDIORA))) {
            universe.setDirty();
            LOGGER.warn("Repaired Mediora address indexes for {}", regionKey.location());
        }
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
        var anchor = level.getChunk(destination.chunkX(), destination.chunkZ());

        StargateNetwork network = StargateNetwork.get(server);
        var records = BlockEntityList.get(server);
        var savedPrimary = network.getPrimaryAddressFromDimension(dimension);
        boolean removedPrimary = false;
        int unresolved = 0;
        int removed = 0;
        List<UniverseStargateEntity> validControllers = new ArrayList<>();
        var candidates = Collections.newSetFromMap(new IdentityHashMap<Stargate, Boolean>());
        candidates.addAll(network.getStargatesInDimension(dimension));
        records.getStargates().values().stream()
                .filter(gate -> gate != null && dimension.equals(gate.getDimension()))
                .forEach(candidates::add);
        for (Stargate gate : candidates) {
            if (gate.getStargateType() != StargateInit.UNIVERSE.get()) continue;
            var address = gate.get9ChevronAddress();
            if (!(gate instanceof BlockEntityStargate<?> blockGate) || !dimension.equals(gate.getDimension())
                    || blockGate.getBlockPos() == null || address == null
                    || address.getType() != Address.Type.ADDRESS_9_CHEVRON) {
                unresolved++;
                LOGGER.warn("Retaining destination gate record with unverifiable controller in {}: {}",
                        dimension.location(), address);
                continue;
            }
            BlockPos pos = blockGate.getBlockPos();
            if (!Level.isInSpawnableBounds(pos) || level.isOutsideBuildHeight(pos)) {
                unresolved++;
                LOGGER.warn("Retaining destination gate record {} with out-of-bounds controller {} in {}",
                        address, pos, dimension.location());
                continue;
            }
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            var owner = records.getStargate(address);
            var entity = level.getBlockEntity(pos);
            var controller = entity instanceof UniverseStargateEntity universeGate ? universeGate : null;
            boolean present = entity instanceof AbstractStargateEntity<?>
                    || level.getBlockState(pos).getBlock() instanceof AbstractStargateBaseBlock;
            var state = GateRecordAudit.classify(gate, owner, present, owner == gate && controller != null
                    && address.equals(controller.get9ChevronAddress()) && controller.getStargate() == gate);
            if (state == GateRecordAudit.RecordState.LIVE) {
                validControllers.add(controller);
            } else if (state == GateRecordAudit.RecordState.STALE && network.removeStargate(gate)) {
                removed++;
                removedPrimary |= address.equals(savedPrimary);
                LOGGER.info("Removed stale destination gate record {} at {} in {}: controller is gone",
                        address, pos, dimension.location());
            } else {
                unresolved++;
                LOGGER.warn("Retaining unresolved destination gate record {} at {} in {}: controller present={}, address ownership={}",
                        address, pos, dimension.location(), present,
                        owner == gate ? "this record" : owner == null ? "missing" : "another record in " + owner.getDimension());
            }
        }
        for (BlockPos pos : List.copyOf(anchor.getBlockEntitiesPos())) {
            if (level.getBlockEntity(pos) instanceof UniverseStargateEntity controller
                    && !validControllers.contains(controller)) {
                unresolved++;
                LOGGER.warn("Unverified Universe controller at {} in anchor chunk of {}: {}",
                        pos, dimension.location(), controller.get9ChevronAddress());
            }
        }
        LOGGER.info("Validated destination {}: {} live controllers, {} unresolved records, {} stale records removed",
                dimension.location(), validControllers.size(), unresolved, removed);
        var controller = validControllers.size() == 1 ? validControllers.get(0) : null;
        switch (GateRecordAudit.decide(validControllers.size(), unresolved > 0, savedPrimary,
                controller == null ? null : controller.get9ChevronAddress())) {
            case HOLD -> LOGGER.warn("Skipping gate placement and primary changes in {}: saved primary={} ({}), live gates={}",
                    dimension.location(), savedPrimary, removedPrimary ? "stale record removed; key preserved" : "unchanged",
                    validControllers.stream().map(gate -> gate.getBlockPos() + " " + gate.get9ChevronAddress()).toList());
            case USE_EXISTING -> {
                boolean buried = isBuried(level, controller);
                boolean jungle = destination.name().equals("jungle_planet");
                if ((!buried && !jungle) || !placeGate(level, destination, controller, buried))
                    restorePrimaryIndexes(server, dimension, controller);
            }
            case GENERATE -> placeGate(level, destination, null, false);
        }
    }

    private static void restorePrimaryIndexes(MinecraftServer server, ResourceKey<Level> dimension,
                                              AbstractStargateEntity<?> controller) {
        Stargate registered = controller.getStargate();
        var address = controller.get9ChevronAddress();
        if (registered == null || address == null || registered.getAddressRegionKey() == null) {
            LOGGER.error("Cannot restore primary indexes for gate at {} in {}: record, address, or region is missing",
                    controller.getBlockPos(), dimension.location());
            return;
        }
        StargateNetwork network = StargateNetwork.get(server);
        var region = Universe.get(server).getAddressRegionFromDimension(dimension);
        if (region == null || !registered.getAddressRegionKey().equals(region.getResourceKey())
                || network.findStargateInRegion(region.getResourceKey(), address) != registered) {
            LOGGER.error("Cannot restore primary gate {} in {}: the registered gate is not indexed in the destination region",
                    address, dimension.location());
            return;
        }
        controller.setPrimary();
        if (network.getPrimaryStargateFromDimension(dimension) != registered) {
            // SGJourney's setter otherwise skips refreshing the cached gate when the saved key already matches.
            if (address.equals(network.getPrimaryAddressFromDimension(dimension)))
                network.setPrimaryAddressForDimension(dimension, null);
            network.setPrimaryAddressForDimension(dimension, address);
        }
        // These expedition gates are the only route home from their generated
        // planets and their pedestals contain no external power source. Keep
        // the authoritative primary gate charged when its indexes are repaired
        // so both fresh and existing worlds can dial out reliably.
        long maxEnergy = controller.energyStorage.getTrueMaxEnergyStored();
        if (controller.energyStorage.getTrueEnergyStored() < maxEnergy) {
            controller.energyStorage.setEnergy(maxEnergy);
            LOGGER.info("Powered expedition gate at {} in {} to {}/{} energy",
                    controller.getBlockPos(), dimension.location(),
                    controller.energyStorage.getTrueEnergyStored(), maxEnergy);
        }
        var stargateAdapter = controller.getStargate();
        if (stargateAdapter != null) {
            controller.setupAutoCache(stargateAdapter.controllerCache());
        }
        controller.updateStargate();
        controller.setChanged();
        if (controller.getLevel() instanceof ServerLevel level)
            WaterPlanetClimateEvents.prepareLandingZone(level, controller.getBlockPos());
        LOGGER.info("Restored primary gate at {} in {} with preserved address {} and region {}",
                controller.getBlockPos(), dimension.location(), address, registered.getAddressRegionKey().location());
    }

    private static boolean isBuried(ServerLevel level, UniverseStargateEntity gate) {
        BlockPos center = gate.getCenterPos();
        return GateTerrainSurvey.isBuried(center.getX(), center.getY(), center.getZ(),
                (x, z) -> level.getChunk(x >> 4, z >> 4)
                        .getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15));
    }

    private static boolean placeGate(ServerLevel level, Destination destination, UniverseStargateEntity previous,
                                     boolean buried) {
        var records = BlockEntityList.get(level);
        Address.Immutable preserved = previous == null ? null : previous.get9ChevronAddress();
        if (previous != null && (preserved == null || preserved.getType() != Address.Type.ADDRESS_9_CHEVRON
                || previous.isConnected() || previous.isDialing() || previous.isRotating()
                || records.getStargate(preserved) != previous.getStargate()
                || previous.getStargate() == null)) {
            LOGGER.warn("Skipping gate relocation at {}: it is busy or does not own its saved address", previous.getBlockPos());
            return false;
        }
        ResourceLocation templateId = new ResourceLocation("sgjourney",
                "stargate/universe/pedestal/end/universe_stargate_pedestal_end_1");
        StructureTemplate template = level.getStructureManager().get(templateId).orElse(null);
        if (template == null) {
            LOGGER.error("Could not load Universe Stargate pedestal template {}", templateId);
            return false;
        }
        boolean jungle = destination.name().equals("jungle_planet");
        PlacementSite site = jungle ? junglePlacement(level, destination, template, previous, buried)
                : destination.name().equals("water_planet") ? waterPlanetPlacement(level, destination, template, previous)
                : findFlattestPlacement(level, destination, template);
        if (site == null && jungle) return false; // The Jungle survey logs why it did not place a gate.
        if (site == null || site.variation() > 16) {
            LOGGER.error("Could not find a supported surface for the {} gate", destination.name());
            return false;
        }
        int placementY = site.baseY();
        BlockPos origin = new BlockPos(site.centerX() - template.getSize().getX() / 2, placementY,
                site.centerZ() - template.getSize().getZ() / 2);
        StructurePlaceSettings settings = new StructurePlaceSettings();
        ResourceLocation processorsId = new ResourceLocation("sgjourney", destination.name() + "_gate");
        StructureProcessorList processors = level.registryAccess().registryOrThrow(
                net.minecraft.core.registries.Registries.PROCESSOR_LIST).get(processorsId);
        if (processors != null) processors.list().forEach(settings::addProcessor);
        var controllers = template.filterBlocks(origin, settings, BlockInit.UNIVERSE_STARGATE.get());
        if (controllers.size() != 1) {
            LOGGER.error("Expected one controller in the {} gate template", destination.name());
            return false;
        }
        BlockPos controllerPos = controllers.get(0).pos();
        BlockPos obstruction = findFootprintObstruction(level, template, origin, jungle);
        if (obstruction != null) {
            LOGGER.warn("Skipping {} gate placement: the surface footprint is occupied at {}", destination.name(), obstruction);
            return false;
        }
        Stargate existing = previous == null ? null : previous.getStargate();
        var currentPrimary = StargateNetwork.get(level).getPrimaryAddressFromDimension(level.dimension());
        if ((currentPrimary != null && !currentPrimary.equals(preserved))
                || StargateNetwork.get(level).getStargatesInDimension(level.dimension()).stream()
                        .anyMatch(gate -> gate != existing)
                || records.getStargates().values().stream()
                        .anyMatch(gate -> gate != null && gate != existing && level.dimension().equals(gate.getDimension()))) {
            LOGGER.warn("Skipping {} gate placement: loading the site revealed another gate or primary address",
                    destination.name());
            return false;
        }
        if (jungle) {
            clearJungleFootprint(level, template, origin);
            buildJungleFoundation(level, origin, template);
        } else {
            buildFoundation(level, origin, template);
        }
        boolean placed = template.placeInWorld(level, origin, origin, settings, level.random, 3);
        if (!placed) {
            LOGGER.error("Failed to place missing Universe Stargate template in {}", level.dimension().location());
            return false;
        }

        // Direct template placement does not run SGJourney's worldgen discovery
        // pass. Register the newly loaded controller block entity explicitly so
        // the address can be dialed during this same server session.
        if (!(level.getBlockEntity(controllerPos) instanceof UniverseStargateEntity replacement)) {
            LOGGER.error("Placed {} pedestal but its controller is missing at {}", destination.name(), controllerPos);
            return false;
        }
        replacement.localPointOfOrigin();
        replacement.setProtected(true);
        replacement.setPrimary();
        if (previous != null) {
            BlockPos oldPos = previous.getBlockPos();
            // Native removal clears the old network identity and dismantles the old ring.
            previous.markItemAsDropped();
            if (!level.setBlockAndUpdate(oldPos, Blocks.AIR.defaultBlockState())
                    || records.getStargate(preserved) != null)
                throw new IllegalStateException("Could not release gate address " + preserved + " at " + oldPos);
            replacement.set9ChevronAddress(preserved);
            replacement.generate();
            if (!preserved.equals(replacement.get9ChevronAddress()) || replacement.getStargate() == null
                    || records.getStargate(preserved) != replacement.getStargate())
                throw new IllegalStateException("Could not restore gate address " + preserved + " at " + controllerPos);
            restorePrimaryIndexes(level.getServer(), level.dimension(), replacement);
            LOGGER.info("Relocated {} gate from {} to {} with unchanged address {}",
                    destination.name(), oldPos, controllerPos, preserved);
        } else {
            replacement.generate();
            restorePrimaryIndexes(level.getServer(), level.dimension(), replacement);
        }
        int maxChunkX = (origin.getX() + template.getSize().getX() - 1) >> 4;
        int maxChunkZ = (origin.getZ() + template.getSize().getZ() - 1) >> 4;
        for (int chunkX = origin.getX() >> 4; chunkX <= maxChunkX; chunkX++)
            for (int chunkZ = origin.getZ() >> 4; chunkZ <= maxChunkZ; chunkZ++)
                level.setChunkForced(chunkX, chunkZ, true);
        LOGGER.info("Placed {} gate near {},{} at y={} with sampled terrain variation {}",
                destination.name(), site.centerX(), site.centerZ(), placementY, site.variation());
        return placed;
    }

    private static PlacementSite junglePlacement(ServerLevel level, Destination destination,
                                                  StructureTemplate template, UniverseStargateEntity previous,
                                                  boolean buried) {
        var controllers = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(),
                BlockInit.UNIVERSE_STARGATE.get());
        if (controllers.size() != 1) {
            LOGGER.error("Jungle pedestal must contain exactly one Universe Stargate controller");
            return null;
        }
        var chunkSource = level.getChunkSource();
        var generator = chunkSource.getGenerator();
        var randomState = chunkSource.randomState();
        var heights = new HashMap<Long, Integer>();
        IntBinaryOperator ground = (x, z) -> heights.computeIfAbsent(ChunkPos.asLong(x, z),
                key -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, randomState));
        BlockPos oldPos = previous == null ? null : previous.getBlockPos();
        int seaLevel = generator.getSeaLevel();
        int maxY = level.getMaxBuildHeight();
        int height = template.getSize().getY();
        if (!buried && oldPos != null
                && GateTerrainSurvey.jungleClearing(oldPos.getX(), oldPos.getZ(), seaLevel, maxY, height, ground) != null) {
            LOGGER.info("Keeping Jungle gate at {}: surrounding terrain meets clearing requirements", oldPos);
            return null;
        }
        BlockPos offset = controllers.get(0).pos();
        int anchorX = oldPos == null ? (destination.chunkX() << 4) + 8 : oldPos.getX();
        int anchorZ = oldPos == null ? (destination.chunkZ() << 4) + 8 : oldPos.getZ();
        record Candidate(int x, int z, GateTerrainSurvey.Result clearing) {}
        List<Candidate> candidates = new ArrayList<>();
        // Sample generator heights without loading chunks. Only validate the best few actual footprints.
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (oldPos != null && dx == 0 && dz == 0) continue;
                int x = anchorX + dx * 32;
                int z = anchorZ + dz * 32;
                var clearing = GateTerrainSurvey.jungleClearing(x, z, seaLevel, maxY, height, ground);
                if (clearing != null) candidates.add(new Candidate(x, z, clearing));
            }
        }
        candidates.sort(Comparator.comparingLong((Candidate c) -> c.clearing().score())
                .thenComparingInt(c -> Math.abs(c.x() - anchorX) + Math.abs(c.z() - anchorZ)));
        for (Candidate candidate : candidates.stream().limit(4).toList()) {
            int centerX = candidate.x() - offset.getX() + template.getSize().getX() / 2;
            int centerZ = candidate.z() - offset.getZ() + template.getSize().getZ() / 2;
            PlacementSite site = scoreJunglePlacement(level, template, centerX, centerZ);
            if (site == null || site.variation() > 8 || site.baseY() > candidate.clearing().baseY() + 3) continue;
            var origin = new BlockPos(candidate.x() - offset.getX(), site.baseY(), candidate.z() - offset.getZ());
            if (findFootprintObstruction(level, template, origin, true) == null) return site;
        }
        LOGGER.warn("No suitable Jungle gate clearing near {},{} ({} terrain candidates); {}",
                anchorX, anchorZ, candidates.size(), previous == null ? "placement skipped" : "original gate retained");
        return null;
    }

    private static BlockPos findFootprintObstruction(ServerLevel level, StructureTemplate template, BlockPos origin,
                                                     boolean jungle) {
        var size = template.getSize();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1,
                size.getY() - 1, size.getZ() - 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !(jungle && isJungleVegetation(state))) return pos.immutable();
        }
        return null;
    }

    private static boolean isJungleVegetation(BlockState state) {
        return !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS) || state.is(Blocks.VINE) || state.is(Blocks.BAMBOO)
                || state.is(Blocks.BAMBOO_SAPLING) || state.is(Blocks.COCOA) || state.is(Blocks.GRASS)
                || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM)
                || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.GLOW_LICHEN));
    }

    private static int jungleGroundHeight(ServerLevel level, int x, int z) {
        int top = level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
        int surface = GateTerrainSurvey.surfaceBelowVegetation(top, level.getMinBuildHeight(), y -> {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            return state.isAir() || isJungleVegetation(state);
        });
        if (surface > level.getMinBuildHeight()
                && !level.getBlockState(new BlockPos(x, surface - 1, z)).getFluidState().isEmpty())
            return level.getMinBuildHeight();
        return surface;
    }

    private static PlacementSite scoreJunglePlacement(ServerLevel level, StructureTemplate template,
                                                       int centerX, int centerZ) {
        int halfX = template.getSize().getX() / 2 + 1;
        int halfZ = template.getSize().getZ() / 2 + 1;
        GateTerrainSurvey.Result result = GateTerrainSurvey.measure(
                centerX - halfX, centerX + halfX, centerZ - halfZ, centerZ + halfZ,
                level.getMinBuildHeight(), level.getMaxBuildHeight(), template.getSize().getY(),
                (x, z) -> jungleGroundHeight(level, x, z));
        return result == null ? null
                : new PlacementSite(centerX, centerZ, result.baseY(), result.variation(), result.score());
    }

    private static void clearJungleFootprint(ServerLevel level, StructureTemplate template, BlockPos origin) {
        var size = template.getSize();
        for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1,
                size.getY() - 1, size.getZ() - 1))) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && (!isJungleVegetation(state) || !level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18)))
                throw new IllegalStateException("Could not clear Jungle gate vegetation at " + pos);
        }
    }

    private static void buildJungleFoundation(ServerLevel level, BlockPos origin, StructureTemplate template) {
        for (int x = origin.getX(); x < origin.getX() + template.getSize().getX(); x++) {
            for (int z = origin.getZ(); z < origin.getZ() + template.getSize().getZ(); z++) {
                for (int y = origin.getY() - 1; y >= level.getMinBuildHeight(); y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !isJungleVegetation(state)) break;
                    if (!level.setBlock(pos, Blocks.STONE.defaultBlockState(), 18))
                        throw new IllegalStateException("Could not support Jungle gate foundation at " + pos);
                }
            }
        }
    }

    private static PlacementSite waterPlanetPlacement(ServerLevel level, Destination destination,
                                                       StructureTemplate template, UniverseStargateEntity buried) {
        // Align the controller, not the center of the asymmetric pedestal, with the landing circle.
        var controllers = template.filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(),
                BlockInit.UNIVERSE_STARGATE.get());
        if (controllers.size() != 1) {
            LOGGER.error("Water Planet pedestal must contain exactly one Universe Stargate controller");
            return null;
        }
        BlockPos offset = controllers.get(0).pos();
        int centerX = WaterPlanetClimateEvents.GATE_X - offset.getX() + template.getSize().getX() / 2;
        int centerZ = WaterPlanetClimateEvents.GATE_Z - offset.getZ() + template.getSize().getZ() / 2;
        if (hasWaterGateNear(level, destination.chunkX(), destination.chunkZ(), buried)
                || hasWaterGateNear(level, WaterPlanetClimateEvents.GATE_X >> 4, WaterPlanetClimateEvents.GATE_Z >> 4, buried)) {
            LOGGER.warn("Not placing a new Water Planet gate: an existing controller was found near a landing site");
            return null;
        }
        return scorePlacement(level, template, centerX, centerZ);
    }

    private static boolean hasWaterGateNear(ServerLevel level, int chunkX, int chunkZ, UniverseStargateEntity buried) {
        for (int x = chunkX - 1; x <= chunkX + 1; x++) {
            for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
                var chunk = level.getChunk(x, z);
                for (BlockPos pos : List.copyOf(chunk.getBlockEntitiesPos())) {
                    if (level.getBlockEntity(pos) instanceof UniverseStargateEntity gate && gate != buried) return true;
                }
            }
        }
        return false;
    }

    private static PlacementSite findFlattestPlacement(ServerLevel level, Destination destination,
                                                        StructureTemplate template) {
        int anchorX = (destination.chunkX() << 4) + 8;
        int anchorZ = (destination.chunkZ() << 4) + 8;
        PlacementSite best = null;
        // Loading sampled chunks is required to obtain real, fully generated heights.
        // Keep the search bounded so a missing gate does not generate hundreds of chunks.
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int centerX = anchorX + dx * 16;
                    int centerZ = anchorZ + dz * 16;
                    PlacementSite candidate = scorePlacement(level, template, centerX, centerZ);
                    if (candidate != null && (best == null || candidate.score() < best.score())) best = candidate;
                    if (best != null && best.score() == 0) return best;
                }
            }
        }
        return best;
    }

    private static PlacementSite scorePlacement(ServerLevel level, StructureTemplate template,
                                                 int centerX, int centerZ) {
        int halfX = template.getSize().getX() / 2 + 1;
        int halfZ = template.getSize().getZ() / 2 + 1;
        GateTerrainSurvey.Result result = GateTerrainSurvey.measure(
                centerX - halfX, centerX + halfX, centerZ - halfZ, centerZ + halfZ,
                level.getMinBuildHeight(), level.getMaxBuildHeight(), template.getSize().getY(),
                (x, z) -> level.getChunk(x >> 4, z >> 4)
                        .getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1);
        return result == null ? null
                : new PlacementSite(centerX, centerZ, result.baseY(), result.variation(), result.score());
    }

    /**
     * Supports the pedestal over gently sloping barren terrain. The structure
     * finder deliberately avoids cutting hills; this small additive terrace
     * fills only missing ground and tapers into the natural surface.
     */
    private static void buildFoundation(ServerLevel level, BlockPos origin,
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
                int naturalTop = level.getChunk(x >> 4, z >> 4)
                        .getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                int targetTop = platformTop - ring;
                if (targetTop <= naturalTop) continue;

                var surface = level.getBlockState(new BlockPos(x, naturalTop, z));
                if (surface.isAir()) surface = Blocks.STONE.defaultBlockState();
                for (int y = naturalTop + 1; y <= targetTop; y++)
                    level.setBlock(new BlockPos(x, y, z), y == targetTop
                            ? surface : Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    private record Destination(String name, int chunkX, int chunkZ) {}
    private record PlacementSite(int centerX, int centerZ, int baseY, int variation, long score) {}
}
