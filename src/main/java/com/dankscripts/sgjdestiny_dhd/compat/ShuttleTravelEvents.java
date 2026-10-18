package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block.DestinyShuttleAirlockBlock;
import com.dankscripts.sgjdestiny_dhd.entity.DestinyPilotSeatEntity;
import com.dankscripts.sgjdestiny_dhd.network.ShuttleTravelNetwork;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A fixed-route shuttle trip: the ship remains at both berths while everyone aboard travels together. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShuttleTravelEvents {
    public static final ResourceKey<Level> DESTINY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("sgjourney", "destiny"));
    public static final ResourceKey<Level> EDEN = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("sgjourney", "eden"));
    private static final List<Trip> ACTIVE = new ArrayList<>();
    private static final List<LandingSearch> SEARCHING = new ArrayList<>();
    private static final int FLIGHT_TICKS = 120;
    private static final int OBELISK_X = 96, OBELISK_Z = 96;
    private static final int[] SEARCH_RADII = {2600, 3000, 3500, 4000, 4500, 4900};
    // Search connected patches of ground instead of isolated points. Neighboring
    // anchors reuse generated chunks and can discover a clearing between spokes.
    private static final int[] PATCH_OFFSETS = {0, 8, -8, 16, -16};
    private static final int SEARCH_SPOKES = 24;
    private static final int PATCH_SIZE = PATCH_OFFSETS.length * PATCH_OFFSETS.length;
    private static final int SEARCH_COUNT = SEARCH_RADII.length * SEARCH_SPOKES * PATCH_SIZE;

    private ShuttleTravelEvents() {}

    public static String destinationName(ServerPlayer player) {
        if (player.level().dimension().equals(DESTINY)) return "EDEN";
        if (player.level().dimension().equals(EDEN)) return "DESTINY";
        return null;
    }

    public static void launch(ServerPlayer pilot, BlockPos sourceAnchor) {
        if (!(pilot.getVehicle() instanceof DestinyPilotSeatEntity seat)
                || !sourceAnchor.equals(seat.getShuttleAnchor())) {
            ShuttleTravelNetwork.status(pilot, false, "Sit in the shuttle pilot seat to launch.");
            return;
        }
        ResourceKey<Level> sourceKey = pilot.level().dimension();
        if (!sourceKey.equals(DESTINY) && !sourceKey.equals(EDEN)) {
            ShuttleTravelNetwork.status(pilot, false, "Shuttle travel is available on Destiny and Eden.");
            return;
        }
        ServerLevel source = (ServerLevel) pilot.level();
        BlockState sourceState = source.getBlockState(sourceAnchor);
        if (!sourceState.is(ModBlocks.DESTINY_SHUTTLE_AIRLOCK.get())
                || sourceState.getValue(DestinyShuttleAirlockBlock.COLUMN) != 0
                || sourceState.getValue(DestinyShuttleAirlockBlock.ROW) != 0) {
            ShuttleTravelNetwork.status(pilot, false, "The pilot seat is no longer linked to a shuttle.");
            return;
        }
        if (!seat.isPilotAt(sourceAnchor, sourceState.getValue(DestinyShuttleAirlockBlock.FACING))) {
            ShuttleTravelNetwork.status(pilot, false, "Only the pilot can launch the shuttle.");
            return;
        }
        if (ACTIVE.stream().anyMatch(trip -> trip.server == pilot.getServer()
                && trip.sourceKey.equals(sourceKey) && trip.sourceAnchor.equals(sourceAnchor))
                || SEARCHING.stream().anyMatch(search -> search.server == pilot.getServer()
                && search.pending.stream().anyMatch(request -> request.sourceAnchor.equals(sourceAnchor)))) {
            ShuttleTravelNetwork.status(pilot, false, "This shuttle is already in transit.");
            return;
        }

        MinecraftServer server = pilot.getServer();
        ResourceKey<Level> targetKey = sourceKey.equals(DESTINY) ? EDEN : DESTINY;
        ServerLevel target = server.getLevel(targetKey);
        if (target == null) {
            ShuttleTravelNetwork.status(pilot, false, "Destination dimension is unavailable.");
            return;
        }
        ShuttleTravelData route = ShuttleTravelData.get(server);
        BlockPos targetAnchor = targetKey.equals(EDEN) ? route.eden() : route.destiny();
        if (targetAnchor == null || !isShuttle(target, targetAnchor)) {
            if (targetKey.equals(EDEN)) {
                LandingSearch search = searchFor(server);
                if (search == null) {
                    if (route.edenSearchCandidate() >= SEARCH_COUNT) {
                        ShuttleTravelNetwork.status(pilot, false,
                                "No naturally clear Eden landing site was found 2,500-5,000 blocks from the obelisk.");
                        return;
                    }
                    search = new LandingSearch(server, route.edenSearchCandidate());
                    SEARCHING.add(search);
                }
                search.pending.add(new PendingLaunch(pilot.getUUID(), sourceAnchor));
                ShuttleTravelNetwork.status(pilot, true, "Preparing Eden's natural shuttle berth...");
                return;
            }
            ShuttleTravelNetwork.status(pilot, false, "Destiny's departure shuttle is missing.");
            return;
        }
        if (sourceKey.equals(DESTINY)) route.setDestiny(sourceAnchor);

        Direction facing = sourceState.getValue(DestinyShuttleAirlockBlock.FACING);
        List<UUID> aboard = new ArrayList<>();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate.level() == source && isAboard(candidate, sourceAnchor, facing))
                aboard.add(candidate.getUUID());
        }
        if (!aboard.contains(pilot.getUUID())) aboard.add(pilot.getUUID());
        ACTIVE.add(new Trip(server, sourceKey, sourceAnchor, targetKey, targetAnchor, aboard, FLIGHT_TICKS));
        for (UUID id : aboard) {
            ServerPlayer passenger = server.getPlayerList().getPlayer(id);
            if (passenger != null) {
                String message = "Launching for " + (targetKey.equals(EDEN) ? "Eden" : "Destiny") + "...";
                ShuttleTravelNetwork.status(passenger, true, message);
                passenger.displayClientMessage(Component.literal(message), true);
            }
        }
    }

    private static boolean isShuttle(ServerLevel level, BlockPos anchor) {
        BlockState state = level.getBlockState(anchor);
        return state.is(ModBlocks.DESTINY_SHUTTLE_AIRLOCK.get())
                && state.getValue(DestinyShuttleAirlockBlock.COLUMN) == 0
                && state.getValue(DestinyShuttleAirlockBlock.ROW) == 0;
    }

    private static boolean isAboard(ServerPlayer player, BlockPos anchor, Direction facing) {
        if (player.getVehicle() instanceof DestinyPilotSeatEntity seat
                && anchor.equals(seat.getShuttleAnchor())) return true;
        Direction right = facing.getClockWise();
        double dx = player.getX() - anchor.getX(), dz = player.getZ() - anchor.getZ();
        double across = dx * right.getStepX() + dz * right.getStepZ();
        double forward = dx * facing.getStepX() + dz * facing.getStepZ();
        return across > -1.5 && across < 2.5 && forward > 0.4 && forward < 22.0
                && player.getY() > anchor.getY() + 0.7 && player.getY() < anchor.getY() + 4.8;
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel eden = server.getLevel(EDEN);
        if (eden == null) return;
        ShuttleTravelData route = ShuttleTravelData.get(server);
        if (route.eden() != null && isShuttle(eden, route.eden())) return;
        if (route.edenSearchCandidate() < SEARCH_COUNT && searchFor(server) == null)
            SEARCHING.add(new LandingSearch(server, route.edenSearchCandidate()));
    }

    private static LandingSearch searchFor(MinecraftServer server) {
        for (LandingSearch search : SEARCHING)
            if (search.server == server) return search;
        return null;
    }

    /** Search distant terrain; never cut trees, flatten ground, or replace structures. */
    private static BlockPos findNaturalLanding(ServerLevel level, Direction facing, int candidate) {
        Direction right = facing.getClockWise();
        int radius = SEARCH_RADII[candidate / (SEARCH_SPOKES * PATCH_SIZE)];
        int spoke = (candidate / PATCH_SIZE) % SEARCH_SPOKES;
        int patch = candidate % PATCH_SIZE;
        int lateral = PATCH_OFFSETS[patch % PATCH_OFFSETS.length];
        int radial = PATCH_OFFSETS[patch / PATCH_OFFSETS.length];
        double angle = spoke * Math.PI * 2 / SEARCH_SPOKES;
        int cx = OBELISK_X + (int) Math.round(Math.cos(angle) * (radius + radial) - Math.sin(angle) * lateral);
        int cz = OBELISK_Z + (int) Math.round(Math.sin(angle) * (radius + radial) + Math.cos(angle) * lateral);
        double distance = Math.hypot(cx - OBELISK_X, cz - OBELISK_Z);
        if (distance < 2500 || distance > 4950) return null;

        // getHeight may read an incomplete remote chunk (with only its bedrock
        // heightmap). Generate the whole footprint to FULL before evaluating it.
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int x : new int[]{-12, 12}) {
            for (int forward : new int[]{0, 26}) {
                int wx = cx + right.getStepX() * x + facing.getStepX() * forward;
                int wz = cz + right.getStepZ() * x + facing.getStepZ() * forward;
                minX = Math.min(minX, wx); maxX = Math.max(maxX, wx);
                minZ = Math.min(minZ, wz); maxZ = Math.max(maxZ, wz);
            }
        }
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++)
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++)
                level.getChunk(chunkX, chunkZ);

        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int x = -12; x <= 12; x += 4) {
            for (int forward = 0; forward <= 26; forward += 4) {
                int wx = cx + right.getStepX() * x + facing.getStepX() * forward;
                int wz = cz + right.getStepZ() * x + facing.getStepZ() * forward;
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                minY = Math.min(minY, height);
                maxY = Math.max(maxY, height);
                if (maxY - minY > 1 || height <= level.getMinBuildHeight() + 5) return null;
            }
        }
        for (int x = -12; x <= 12; x++) {
            for (int forward = 0; forward <= 26; forward++) {
                int wx = cx + right.getStepX() * x + facing.getStepX() * forward;
                int wz = cz + right.getStepZ() * x + facing.getStepZ() * forward;
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                BlockPos ground = new BlockPos(wx, height - 1, wz);
                BlockState groundState = level.getBlockState(ground);
                if (Math.abs(height - maxY) > 1 || groundState.is(Blocks.BEDROCK)
                        || groundState.getCollisionShape(level, ground).isEmpty()
                        || !level.getFluidState(ground).isEmpty()) return null;
            }
        }
        BlockPos anchor = new BlockPos(cx, maxY, cz);
        if (DestinyShuttleAirlockBlock.placeTravelShuttle(level, anchor, facing)) return anchor;
        return null;
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (int i = SEARCHING.size() - 1; i >= 0; i--) {
            LandingSearch search = SEARCHING.get(i);
            if (search.server != event.getServer()) continue;
            ServerLevel eden = search.server.getLevel(EDEN);
            if (eden == null) continue;
            ShuttleTravelData route = ShuttleTravelData.get(search.server);
            if (route.eden() != null && isShuttle(eden, route.eden())) {
                SEARCHING.remove(i);
                launchPending(search);
                continue;
            }
            if (search.candidate >= SEARCH_COUNT) {
                SEARCHING.remove(i);
                failPending(search);
                continue;
            }
            BlockPos landing = findNaturalLanding(eden, Direction.NORTH, search.candidate++);
            route.setEdenSearchCandidate(search.candidate);
            if (landing != null) {
                SEARCHING.remove(i);
                route.setEden(landing);
                launchPending(search);
            } else if (search.candidate >= SEARCH_COUNT) {
                SEARCHING.remove(i);
                failPending(search);
            }
        }
        for (int i = ACTIVE.size() - 1; i >= 0; i--) {
            Trip trip = ACTIVE.get(i);
            if (trip.server != event.getServer()) continue;
            if (--trip.ticksRemaining > 0) continue;
            ACTIVE.remove(i);
            arrive(trip);
        }
    }

    private static void launchPending(LandingSearch search) {
        for (PendingLaunch request : search.pending) {
            ServerPlayer pilot = search.server.getPlayerList().getPlayer(request.pilot);
            if (pilot != null) launch(pilot, request.sourceAnchor);
        }
    }

    private static void failPending(LandingSearch search) {
        for (PendingLaunch request : search.pending) {
            ServerPlayer pilot = search.server.getPlayerList().getPlayer(request.pilot);
            if (pilot != null) ShuttleTravelNetwork.status(pilot, false,
                    "No naturally clear Eden landing site was found 2,500-5,000 blocks from the obelisk.");
        }
    }

    private static void arrive(Trip trip) {
        ServerLevel source = trip.server.getLevel(trip.sourceKey);
        ServerLevel target = trip.server.getLevel(trip.targetKey);
        if (source == null || target == null || !isShuttle(source, trip.sourceAnchor)
                || !isShuttle(target, trip.targetAnchor)) {
            for (UUID id : trip.passengers) {
                ServerPlayer passenger = trip.server.getPlayerList().getPlayer(id);
                if (passenger != null) ShuttleTravelNetwork.status(passenger, false,
                        "Shuttle trip canceled: a berth is unavailable.");
            }
            return;
        }
        Direction sourceFacing = source.getBlockState(trip.sourceAnchor)
                .getValue(DestinyShuttleAirlockBlock.FACING);
        Direction targetFacing = target.getBlockState(trip.targetAnchor)
                .getValue(DestinyShuttleAirlockBlock.FACING);
        int index = 0;
        for (UUID id : trip.passengers) {
            ServerPlayer player = trip.server.getPlayerList().getPlayer(id);
            if (player == null || player.level() != source
                    || !isAboard(player, trip.sourceAnchor, sourceFacing)) continue;
            player.stopRiding();
            // The aisle is clear even if a traveler launched from a side seat.
            int forward = 4 + index++;
            BlockPos aisle = trip.targetAnchor.relative(targetFacing, Math.min(forward, 20)).above();
            target.getChunkAt(aisle);
            player.teleportTo(target, aisle.getX() + 0.5, aisle.getY() + 0.3,
                    aisle.getZ() + 0.5, targetFacing.toYRot(), player.getXRot());
            ShuttleTravelNetwork.status(player, true, "ARRIVED");
            player.displayClientMessage(Component.literal("Shuttle arrived at "
                    + (trip.targetKey.equals(EDEN) ? "Eden" : "Destiny") + "."), true);
        }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        ACTIVE.removeIf(trip -> trip.server == event.getServer());
        SEARCHING.removeIf(search -> search.server == event.getServer());
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sgjdestiny")
                .then(Commands.literal("shuttle")
                        .then(Commands.literal("linkBerth")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!(player.getVehicle() instanceof DestinyPilotSeatEntity seat)) {
                                        context.getSource().sendFailure(Component.literal("Sit in the pilot seat of the berth shuttle first."));
                                        return 0;
                                    }
                                    BlockPos anchor = seat.getShuttleAnchor();
                                    ServerLevel level = player.serverLevel();
                                    if (!isShuttle(level, anchor)) return 0;
                                    ShuttleTravelData data = ShuttleTravelData.get(player.getServer());
                                    if (level.dimension().equals(EDEN)) {
                                        double distance = Math.hypot(anchor.getX() - OBELISK_X, anchor.getZ() - OBELISK_Z);
                                        if (distance < 2500 || distance > 5000) {
                                            context.getSource().sendFailure(Component.literal("Eden berth must be 2,500-5,000 blocks from the obelisk."));
                                            return 0;
                                        }
                                        data.setEden(anchor);
                                    } else if (level.dimension().equals(DESTINY)) data.setDestiny(anchor);
                                    else return 0;
                                    context.getSource().sendSuccess(() -> Component.literal("Shuttle berth saved for this world."), true);
                                    return 1;
                                }))));
    }

    private static final class LandingSearch {
        final MinecraftServer server;
        final List<PendingLaunch> pending = new ArrayList<>();
        int candidate;

        LandingSearch(MinecraftServer server, int candidate) {
            this.server = server;
            this.candidate = candidate;
        }
    }

    private record PendingLaunch(UUID pilot, BlockPos sourceAnchor) {}

    private static final class Trip {
        final MinecraftServer server;
        final ResourceKey<Level> sourceKey, targetKey;
        final BlockPos sourceAnchor, targetAnchor;
        final List<UUID> passengers;
        int ticksRemaining;

        Trip(MinecraftServer server, ResourceKey<Level> sourceKey, BlockPos sourceAnchor,
             ResourceKey<Level> targetKey, BlockPos targetAnchor, List<UUID> passengers, int ticksRemaining) {
            this.server = server;
            this.sourceKey = sourceKey;
            this.sourceAnchor = sourceAnchor;
            this.targetKey = targetKey;
            this.targetAnchor = targetAnchor;
            this.passengers = passengers;
            this.ticksRemaining = ticksRemaining;
        }
    }
}
