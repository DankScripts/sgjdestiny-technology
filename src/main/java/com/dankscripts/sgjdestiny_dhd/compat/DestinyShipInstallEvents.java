package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block_entity.KinoDispenserBlockEntity;
import com.dankscripts.sgjdestiny_dhd.config.DestinyServerConfig;
import com.dankscripts.sgjdestiny_dhd.block.DestinyCurvedStairBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.Registries;
import net.povstalec.sgjourney.common.blocks.ChevronBlock;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

/** Installs the captured ship only after a world administrator opts in. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DestinyShipInstallEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny Ship");
    private static final ResourceKey<Level> DESTINY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("sgjourney", "destiny"));
    private static final ResourceLocation MANIFEST = ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID,
            "structures/destiny_ship/manifest.json");
    private static final ResourceLocation SPAWNER_POSITIONS = ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID,
            "structures/destiny_ship/spawner_positions.json");
    private static final BlockPos GATE_CONTROLLER = new BlockPos(-8, 74, -299);
    private static final BlockPos GATE_CHEVRON = new BlockPos(-8, 75, -300);
    private static final BlockPos GATE_CHEVRON_SUPPORT = GATE_CHEVRON.below();
    private static final BlockPos DIALING_CONSOLE = new BlockPos(-4, 75, -316);
    private static final BlockPos KINO_DISPENSER = new BlockPos(18, 85, -330);
    private static final BlockPos ORIGINAL_ROOM_MIN = new BlockPos(-16, 69, -310);
    private static final BlockPos ORIGINAL_ROOM_MAX = new BlockPos(0, 84, -278);
    private static final BlockPos CAPTURE_MIN = new BlockPos(-165, 47, -424);
    private static final BlockPos CAPTURE_MAX = new BlockPos(149, 117, -40);
    private static final int RESET_CLEAR_BLOCKS_PER_TICK = 8192;
    private static final int RESET_CLEAR_CHANGES_PER_TICK = 512;
    // The captured world's older entrance corridor extends beyond the
    // SGJourney gate-room structure and has no tile entries for its removed
    // stairs, wooden door, and trim. Clear it as part of the same reset.
    private static final int LEGACY_CORRIDOR_MIN_Z = -326;
    private static final int STRUCTURE_MIN_Y = 47;
    // The packaged ship no longer makes Simply Light a runtime requirement.
    // Legacy light entries are handled by the structure installer instead.
    private static final List<String> SHIP_BLOCK_MODS = List.of();
    private static final List<ShipAirlock> SHIP_AIRLOCKS = List.of(
            new ShipAirlock(-8, 85, -253, Direction.NORTH),
            new ShipAirlock(-8, 85, -246, Direction.NORTH),
            new ShipAirlock(-8, 75, -288, Direction.NORTH),
            new ShipAirlock(-8, 75, -325, Direction.SOUTH),
            new ShipAirlock(-8, 80, -325, Direction.SOUTH),
            new ShipAirlock(-20, 80, -288, Direction.NORTH),
            new ShipAirlock(-23, 75, -309, Direction.EAST),
            new ShipAirlock(4, 80, -289, Direction.NORTH),
            new ShipAirlock(7, 75, -309, Direction.WEST),
            new ShipAirlock(0, 85, -308, Direction.WEST),
            new ShipAirlock(16, 86, -324, Direction.SOUTH));
    private static final WeakHashMap<MinecraftServer, Progress> ACTIVE = new WeakHashMap<>();
    private static final WeakHashMap<MinecraftServer, SpawnerCleanupProgress> SPAWNER_CLEANUP = new WeakHashMap<>();
    private static final Set<MinecraftServer> FAILED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<MinecraftServer> STAIRS_CHECKED = Collections.newSetFromMap(new WeakHashMap<>());

    private DestinyShipInstallEvents() {}

    /** Converts a coordinate in the captured ship to this world's gate-relative coordinate. */
    public static BlockPos shipPos(ServerLevel level, BlockPos captured) {
        ShipInstallData saved = ShipInstallData.get(level.getServer());
        return saved.gateAnchor == null ? captured : transformed(captured, saved.gateAnchor, saved.gateRotation);
    }

    private static BlockPos transformed(BlockPos captured, BlockPos anchor, int quarterTurns) {
        int dx = captured.getX() - GATE_CONTROLLER.getX();
        int dz = captured.getZ() - GATE_CONTROLLER.getZ();
        return switch (quarterTurns) {
            case 1 -> new BlockPos(anchor.getX() - dz, captured.getY(), anchor.getZ() + dx);
            case 2 -> new BlockPos(anchor.getX() - dx, captured.getY(), anchor.getZ() - dz);
            case 3 -> new BlockPos(anchor.getX() + dz, captured.getY(), anchor.getZ() - dx);
            default -> new BlockPos(anchor.getX() + dx, captured.getY(), anchor.getZ() + dz);
        };
    }

    public static Direction shipDirection(ServerLevel level, Direction captured) {
        Direction result = captured;
        for (int i = 0; i < ShipInstallData.get(level.getServer()).gateRotation; i++) result = result.getClockWise();
        return result;
    }

    public static int shipQuarterTurns(ServerLevel level) {
        return ShipInstallData.get(level.getServer()).gateRotation;
    }

    public static BlockPos capturedPos(ServerLevel level, BlockPos world) {
        ShipInstallData saved = ShipInstallData.get(level.getServer());
        if (saved.gateAnchor == null) return world;
        int dx = world.getX() - saved.gateAnchor.getX();
        int dz = world.getZ() - saved.gateAnchor.getZ();
        return switch (saved.gateRotation) {
            case 1 -> new BlockPos(GATE_CONTROLLER.getX() + dz, world.getY(), GATE_CONTROLLER.getZ() - dx);
            case 2 -> new BlockPos(GATE_CONTROLLER.getX() - dx, world.getY(), GATE_CONTROLLER.getZ() - dz);
            case 3 -> new BlockPos(GATE_CONTROLLER.getX() - dz, world.getY(), GATE_CONTROLLER.getZ() + dx);
            default -> new BlockPos(GATE_CONTROLLER.getX() + dx, world.getY(), GATE_CONTROLLER.getZ() + dz);
        };
    }

    public static net.minecraft.world.phys.AABB shipBox(ServerLevel level,
                                                        double minX, double minY, double minZ,
                                                        double maxX, double maxY, double maxZ) {
        double[] a = shipXZ(level, minX, minZ);
        double[] b = shipXZ(level, maxX, maxZ);
        return new net.minecraft.world.phys.AABB(Math.min(a[0], b[0]), minY, Math.min(a[1], b[1]),
                Math.max(a[0], b[0]), maxY, Math.max(a[1], b[1]));
    }

    public static double[] shipXZ(ServerLevel level, double x, double z) {
        ShipInstallData saved = ShipInstallData.get(level.getServer());
        if (saved.gateAnchor == null) return new double[] {x, z};
        double dx = x - GATE_CONTROLLER.getX();
        double dz = z - GATE_CONTROLLER.getZ();
        return switch (saved.gateRotation) {
            case 1 -> new double[] {saved.gateAnchor.getX() - dz + 1, saved.gateAnchor.getZ() + dx};
            case 2 -> new double[] {saved.gateAnchor.getX() - dx + 1, saved.gateAnchor.getZ() - dz + 1};
            case 3 -> new double[] {saved.gateAnchor.getX() + dz, saved.gateAnchor.getZ() - dx + 1};
            default -> new double[] {saved.gateAnchor.getX() + dx, saved.gateAnchor.getZ() + dz};
        };
    }

    private static Rotation shipRotation(ShipInstallData saved) {
        return switch (saved.gateRotation) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static int shipX(ServerLevel level, int capturedX) {
        return shipPos(level, new BlockPos(capturedX, 0, 0)).getX();
    }

    public static int shipZ(ServerLevel level, int capturedZ) {
        return shipPos(level, new BlockPos(0, 0, capturedZ)).getZ();
    }

    /** Inclusive horizontal bounding box: minX, maxX, minZ, maxZ. */
    public static int[] shipBounds(ServerLevel level) {
        BlockPos a = shipPos(level, CAPTURE_MIN);
        BlockPos b = shipPos(level, CAPTURE_MAX);
        return new int[] {Math.min(a.getX(), b.getX()), Math.max(a.getX(), b.getX()),
                Math.min(a.getZ(), b.getZ()), Math.max(a.getZ(), b.getZ())};
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sgjdestiny")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("ship")
                        .then(Commands.literal("reset")
                                .executes(context -> requestReset(context.getSource())))
                        .then(Commands.literal("air")
                                .then(Commands.literal("reset")
                                        .executes(context -> resetAirReserve(context.getSource()))))
                        .then(Commands.literal("retrofitAirlocks")
                                .executes(context -> retrofitAirlocks(context.getSource())))
                        .then(Commands.literal("retrofitStairs")
                                .executes(context -> retrofitStairs(context.getSource())))));
    }

    private static int resetAirReserve(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(DESTINY);
        if (level == null) {
            source.sendFailure(Component.literal("The SGJourney Destiny dimension is not available."));
            return 0;
        }
        DestinyAtmosphere.resetAirReserve(level);
        source.sendSuccess(() -> Component.literal("Destiny air quality reset to 75% (CO2 at 25%)."), true);
        return 1;
    }

    private static int retrofitStairs(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(DESTINY);
        if (level == null) {
            source.sendFailure(Component.literal("The SGJourney Destiny dimension is not available."));
            return 0;
        }
        int[][] oldCurvedX = {{-15, -15, -14, -13, -12}, {-1, -1, -2, -3, -4}};
        int[] oldMacawX = {-15, -1};
        int[][] newX = {{-13, -13, -13, -12, -11}, {-3, -3, -3, -4, -5}};
        boolean alreadyInstalled = true;
        for (int side = 0; side < 2; side++) {
            for (int step = 0; step < 5; step++) {
                BlockState expected = ModBlocks.DESTINY_CURVED_STAIR.get().defaultBlockState()
                        .setValue(DestinyCurvedStairBlock.RIGHT, side == 1)
                        .setValue(DestinyCurvedStairBlock.STEP, step)
                        .setValue(DestinyCurvedStairBlock.TURN, shipQuarterTurns(level));
                if (!level.getBlockState(shipPos(level, new BlockPos(newX[side][step], 75 + step, -313 - step))).equals(expected)) {
                    alreadyInstalled = false;
                }
            }
        }
        if (alreadyInstalled) {
            source.sendSuccess(() -> Component.literal("Destiny's curved stairs are already installed."), false);
            return 1;
        }
        ResourceLocation firstId = BuiltInRegistries.BLOCK.getKey(
                level.getBlockState(shipPos(level, new BlockPos(-15, 75, -314))).getBlock());
        boolean macaw = "mcwbridges:iron_bridge_stair".equals(String.valueOf(firstId));
        boolean earlierCurved = level.getBlockState(shipPos(level, new BlockPos(-15, 75, -314)))
                .is(ModBlocks.DESTINY_CURVED_STAIR.get());
        if (!macaw && !earlierCurved) {
            source.sendFailure(Component.literal("No recognized older Destiny stair pair was found; no blocks were changed."));
            return 0;
        }
        // Validate the entire footprint before changing anything in an existing world.
        for (int side = 0; side < 2; side++) {
            for (int step = 0; step < 5; step++) {
                BlockPos oldPos = shipPos(level, new BlockPos(macaw ? oldMacawX[side] : oldCurvedX[side][step],
                        75 + step, -314 - step));
                BlockPos newPos = shipPos(level, new BlockPos(newX[side][step], 75 + step, -313 - step));
                BlockState oldState = level.getBlockState(oldPos);
                BlockState expectedCurved = ModBlocks.DESTINY_CURVED_STAIR.get().defaultBlockState()
                        .setValue(DestinyCurvedStairBlock.RIGHT, side == 1)
                        .setValue(DestinyCurvedStairBlock.STEP, step)
                        .setValue(DestinyCurvedStairBlock.TURN, shipQuarterTurns(level));
                ResourceLocation oldId = BuiltInRegistries.BLOCK.getKey(oldState.getBlock());
                if (!(macaw ? "mcwbridges:iron_bridge_stair".equals(String.valueOf(oldId))
                        : oldState.equals(expectedCurved)) || !level.getBlockState(newPos).isAir()) {
                    source.sendFailure(Component.literal("Stair footprint has been altered; no blocks were changed."));
                    return 0;
                }
            }
        }
        for (int side = 0; side < 2; side++) {
            for (int step = 0; step < 5; step++) {
                level.setBlock(shipPos(level, new BlockPos(macaw ? oldMacawX[side] : oldCurvedX[side][step],
                                75 + step, -314 - step)),
                        Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        for (int side = 0; side < 2; side++) {
            for (int step = 0; step < 5; step++) {
                level.setBlock(shipPos(level, new BlockPos(newX[side][step], 75 + step, -313 - step)),
                        ModBlocks.DESTINY_CURVED_STAIR.get().defaultBlockState()
                                .setValue(DestinyCurvedStairBlock.RIGHT, side == 1)
                                .setValue(DestinyCurvedStairBlock.STEP, step)
                                .setValue(DestinyCurvedStairBlock.TURN, shipQuarterTurns(level)), Block.UPDATE_ALL);
            }
        }
        source.sendSuccess(() -> Component.literal("Upgraded both Destiny gate-room staircases."), true);
        return 1;
    }

    private static int retrofitAirlocks(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(DESTINY);
        if (level == null) {
            source.sendFailure(Component.literal("The SGJourney Destiny dimension is not available."));
            return 0;
        }
        int converted = 0;
        int skipped = 0;
        for (ShipAirlock airlock : SHIP_AIRLOCKS) {
            BlockPos center = shipPos(level, new BlockPos(airlock.x(), airlock.y(), airlock.z()));
            Direction facing = shipDirection(level, airlock.facing());
            BlockPos anchor = center.relative(facing.getCounterClockWise());
            int old = 0;
            int current = 0;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    BlockPos pos = anchor.relative(facing.getClockWise(), column).above(row);
                    BlockState state = level.getBlockState(pos);
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (id != null && id.toString().equals("ad_astra:airlock")) old++;
                    else if (state.is(com.dankscripts.sgjdestiny_dhd.registry.ModBlocks.DESTINY_SHIP_AIRLOCK.get())) current++;
                }
            }
            if (current == 9) continue;
            if (old != 9) { skipped++; continue; }
            BlockState oldCenter = level.getBlockState(center);
            var property = oldCenter.getBlock().getStateDefinition().getProperty("open");
            boolean open = property instanceof BooleanProperty booleanProperty && oldCenter.getValue(booleanProperty);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    BlockPos pos = anchor.relative(facing.getClockWise(), column).above(row);
                    BlockState replacement = com.dankscripts.sgjdestiny_dhd.registry.ModBlocks.DESTINY_SHIP_AIRLOCK.get()
                            .defaultBlockState()
                            .setValue(com.dankscripts.sgjdestiny_dhd.block.DestinyShipAirlockBlock.FACING, facing)
                            .setValue(com.dankscripts.sgjdestiny_dhd.block.DestinyShipAirlockBlock.COLUMN, column)
                            .setValue(com.dankscripts.sgjdestiny_dhd.block.DestinyShipAirlockBlock.ROW, row)
                            .setValue(com.dankscripts.sgjdestiny_dhd.block.DestinyShipAirlockBlock.OPEN, open);
                    level.setBlock(pos, replacement, Block.UPDATE_ALL);
                }
            }
            converted++;
        }
        final int changed = converted;
        final int left = skipped;
        source.sendSuccess(() -> Component.literal("Destiny airlocks upgraded: " + changed
                + "; incomplete or altered doorways skipped: " + left + "."), true);
        return converted;
    }

    private static int requestReset(CommandSourceStack source) {
        if (!DestinyServerConfig.INSTALL_CUSTOM_DESTINY_SHIP.get()) {
            source.sendFailure(Component.literal("Enable destinyShip.installCustomDestinyShip in this world's server config first."));
            return 0;
        }
        List<String> missing = missingShipMods();
        if (!missing.isEmpty()) {
            source.sendFailure(Component.literal("Missing Destiny ship dependencies: " + String.join(", ", missing)));
            return 0;
        }
        MinecraftServer server = source.getServer();
        if (server.getLevel(DESTINY) == null) {
            source.sendFailure(Component.literal("The SGJourney Destiny dimension is not available."));
            return 0;
        }
        if (ACTIVE.containsKey(server)) {
            source.sendFailure(Component.literal("A Destiny ship installation is already running."));
            return 0;
        }
        ShipInstallData saved = ShipInstallData.get(server);
        BlockPos support = null;
        if (source.getEntity() instanceof ServerPlayer player && player.level().dimension().equals(DESTINY)) {
            BlockPos beneath = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
            if (!player.level().getBlockState(beneath).isAir()) support = beneath;
        }
        saved.requestReset(support);
        FAILED.remove(server);
        source.sendSuccess(() -> Component.literal("Destiny ship reset queued. The captured ship area will be cleared, preserving the Stargate and your standing block, then rebuilt from the package."), true);
        LOGGER.info("Operator requested Destiny ship reset");
        return 1;
    }

    @SubscribeEvent
    public static void requireShipBlockMods(ServerAboutToStartEvent event) {
        // Forge has loaded this world's SERVER config, but the server has not
        // begun loading the world. Abort here, before any ship chunks are touched.
        if (!DestinyServerConfig.INSTALL_CUSTOM_DESTINY_SHIP.get()) return;
        List<String> missingMods = missingShipMods();
        if (!missingMods.isEmpty()) {
            throw new IllegalStateException("Destiny ship is enabled, but required ship mods are missing: "
                    + String.join(", ", missingMods)
                    + ". Install these mods on the server and every client, or set "
                    + "destinyShip.installCustomDestinyShip=false before loading the world.");
        }
    }

    private static List<String> missingShipMods() {
        return SHIP_BLOCK_MODS.stream().filter(modId -> !ModList.get().isLoaded(modId)).toList();
    }

    @SubscribeEvent
    public static void installOnServerTick(ServerTickEvent.Post event) {
        if (!DestinyServerConfig.INSTALL_CUSTOM_DESTINY_SHIP.get()) return;
        MinecraftServer server = event.getServer();
        ShipInstallData saved = ShipInstallData.get(server);
        if (FAILED.contains(server)) return;
        List<String> missingMods = missingShipMods();
        if (!missingMods.isEmpty()) {
            FAILED.add(server);
            LOGGER.error("Destiny ship is enabled but these ship-block mods are missing: {}. "
                    + "Install them on both client and server before opening this world with the ship enabled.",
                    String.join(", ", missingMods));
            return;
        }
        if (saved.installed && !saved.resetRequested) {
            // Existing ships predate the curved stairs. Upgrade only the known
            // Macaw footprint, once per server, after validating every target.
            if (!STAIRS_CHECKED.contains(server)) {
                ServerLevel destiny = server.getLevel(DESTINY);
                if (destiny != null) {
                    STAIRS_CHECKED.add(server);
                    BlockPos firstStair = shipPos(destiny, new BlockPos(-15, 75, -314));
                    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(destiny.getBlockState(firstStair).getBlock());
                    if ("mcwbridges:iron_bridge_stair".equals(String.valueOf(id))
                            || destiny.getBlockState(firstStair).is(ModBlocks.DESTINY_CURVED_STAIR.get())) {
                        retrofitStairs(server.createCommandSourceStack());
                    }
                }
            }
            if (!saved.spawnersCleaned) cleanCapturedSpawners(server, saved);
            return;
        }
        ServerLevel level = server.getLevel(DESTINY);
        if (level == null) return;

        Progress progress = ACTIVE.get(server);
        if (progress == null) {
            if (server.getTickCount() % 100 != 0) return;
            progress = prepare(server, level, saved);
            if (progress == null) return;
            com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftEvents.beforeShipReset(level);
            ACTIVE.put(server, progress);
            LOGGER.info("Destiny ship {} started ({} tiles)", saved.resetRequested ? "reset" : "installation", progress.tiles.size());
        }

        try {
            if (progress.reset && !progress.clearComplete()) {
                progress.clearNextBatch(level);
                return;
            }
            if (progress.nextTile < progress.tiles.size()) {
                Tile tile = progress.tiles.get(progress.nextTile);
                BlockPos origin = shipPos(level,
                        new BlockPos(tile.chunkX << 4, STRUCTURE_MIN_Y, tile.chunkZ << 4));
                level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
                Optional<StructureTemplate> template = level.getStructureManager().get(
                        ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID,
                                "destiny_ship/" + tile.resource.substring(0, tile.resource.length() - 4)));
                if (template.isEmpty()) throw new IllegalStateException("Missing ship tile " + tile.resource);
                // First-time installation still clears SGJourney's generated
                // room. Reset has already cleared the entire captured volume.
                if (!progress.reset) clearOriginalRoomTile(level, tile, progress.preservedGatePositions);
                if (!template.get().placeInWorld(level, origin, origin,
                        new StructurePlaceSettings().setIgnoreEntities(true).setRotation(shipRotation(saved)),
                        level.random, Block.UPDATE_CLIENTS)) {
                    throw new IllegalStateException("Unable to place ship tile " + tile.resource);
                }
                // The captured structure has one SGJ Decorations bearing. Keep
                // the ship complete even when that optional mod is absent.
                if (!ModList.get().isLoaded("sgj_deco")
                        && tile.chunkX == -1 && tile.chunkZ == -19) {
                    BlockPos bearingPos = shipPos(level, new BlockPos(-8, 81, -299));
                    if (!progress.preservedGatePositions.contains(bearingPos))
                        level.setBlock(bearingPos, ModBlocks.DESTINY_BEARING_FALLBACK.get()
                                .defaultBlockState().setValue(
                                        com.dankscripts.sgjdestiny_dhd.block.LitDestinyBearingBlock.FACING,
                                        shipDirection(level, Direction.SOUTH)), Block.UPDATE_CLIENTS);
                }
                progress.restoreProtectedBlocks(level, tile);
                progress.nextTile++;
                return;
            }

            progress.restoreProtectedBlocks(level, null);
            if (!(level.getBlockEntity(shipPos(level, KINO_DISPENSER)) instanceof KinoDispenserBlockEntity dispenser)
                    || dispenser.getStoredKinos() != 2) {
                throw new IllegalStateException("Packaged dispenser did not retain its two Kino kits");
            }
            if (!level.getBlockState(shipPos(level, DIALING_CONSOLE)).is(
                    com.dankscripts.sgjdestiny_dhd.registry.ModBlocks.DESTINY_DHD.get())) {
                throw new IllegalStateException("Destiny dialing console was not installed");
            }
            if (!gateIntact(level, progress.gateBlocks, saved.gateAnchor, saved.gateRotation)) {
                throw new IllegalStateException("The original Destiny Stargate changed during installation");
            }
            LOGGER.info("Destiny elevator: {}", com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftEvents.install(level));
            saved.markInstalled();
            ACTIVE.remove(server);
            LOGGER.info("Opt-in Destiny ship installed. Original Stargate and address remain intact; dispenser contains two Kino kits.");
        } catch (Exception exception) {
            ACTIVE.remove(server);
            FAILED.add(server);
            saved.cancelReset();
            LOGGER.error("Destiny ship installation stopped without writing the completion marker. Fix the issue before explicitly retrying.", exception);
        }
    }

    private static void cleanCapturedSpawners(MinecraftServer server, ShipInstallData saved) {
        ServerLevel level = server.getLevel(DESTINY);
        if (level == null) return;
        try {
            SpawnerCleanupProgress progress = SPAWNER_CLEANUP.get(server);
            if (progress == null) {
                var resource = server.getResourceManager().getResource(SPAWNER_POSITIONS);
                if (resource.isEmpty()) throw new IllegalStateException("Packaged spawner position list is missing");
                List<BlockPos> positions = new ArrayList<>();
                try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                    JsonArray values = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("positions");
                    for (JsonElement value : values) positions.add(pos(value.getAsJsonArray()));
                }
                if (positions.size() != 662 || positions.stream().distinct().count() != 662) {
                    throw new IllegalStateException("Packaged spawner position list is incomplete");
                }
                progress = new SpawnerCleanupProgress(positions);
                SPAWNER_CLEANUP.put(server, progress);
            }
            int end = Math.min(progress.next + 32, progress.positions.size());
            for (; progress.next < end; progress.next++) {
                BlockPos position = shipPos(level, progress.positions.get(progress.next));
                if (level.getBlockState(position).is(Blocks.SPAWNER)) {
                    level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    progress.removed++;
                }
            }
            if (progress.next == progress.positions.size()) {
                saved.markSpawnersCleaned();
                SPAWNER_CLEANUP.remove(server);
                LOGGER.info("Removed {} captured armor-stand spawners from the installed Destiny ship", progress.removed);
            }
        } catch (Exception exception) {
            SPAWNER_CLEANUP.remove(server);
            FAILED.add(server);
            LOGGER.error("Unable to clean captured Destiny ship spawners", exception);
        }
    }

    private static Progress prepare(MinecraftServer server, ServerLevel level, ShipInstallData saved) {
        try {
            JsonObject manifest;
            var resource = server.getResourceManager().getResource(MANIFEST);
            if (resource.isEmpty()) throw new IllegalStateException("Ship manifest is missing");
            try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
                manifest = JsonParser.parseReader(reader).getAsJsonObject();
            }
            if (!"sgjourney:destiny".equals(manifest.get("source_dimension").getAsString())
                    || manifest.get("kino_dispenser_stored_kits").getAsInt() != 2) {
                throw new IllegalStateException("Ship manifest has an unexpected dimension or Kino inventory");
            }
            if (!ORIGINAL_ROOM_MIN.equals(pos(manifest.getAsJsonArray("original_room_min")))
                    || !ORIGINAL_ROOM_MAX.equals(pos(manifest.getAsJsonArray("original_room_max")))
                    || !CAPTURE_MIN.equals(pos(manifest.getAsJsonArray("capture_min")))
                    || !CAPTURE_MAX.equals(pos(manifest.getAsJsonArray("capture_max")))) {
                throw new IllegalStateException("Ship manifest has an unexpected capture footprint");
            }
            List<GateBlock> gateBlocks = new ArrayList<>();
            for (JsonElement item : manifest.getAsJsonArray("preserved_gate_positions")) {
                JsonObject block = item.getAsJsonObject();
                gateBlocks.add(new GateBlock(block.get("name").getAsString(), pos(block.getAsJsonArray("pos"))));
            }
            if (gateBlocks.size() != 26 || gateBlocks.stream().noneMatch(
                    block -> block.pos.equals(GATE_CONTROLLER))) {
                throw new IllegalStateException("Ship manifest does not identify the original 26-block Stargate");
            }
            GateAnchor gateAnchor = findGateAnchor(level, saved, gateBlocks);
            if (gateAnchor == null) {
                if (saved.resetRequested) {
                    saved.cancelReset();
                    FAILED.add(server);
                    LOGGER.error("Destiny ship reset canceled: no intact SGJourney Destiny Stargate was found. The reset will not repeat on restart.");
                } else {
                    LOGGER.warn("Opt-in Destiny ship is waiting for SGJourney's generated Destiny Stargate");
                }
                return null;
            }
            saved.setGateAnchor(gateAnchor);
            List<Tile> tiles = new ArrayList<>();
            for (JsonElement item : manifest.getAsJsonArray("tiles")) {
                JsonObject tile = item.getAsJsonObject();
                String name = tile.get("resource").getAsString();
                if (!name.matches("tile_-?\\d+_-?\\d+\\.nbt")) {
                    throw new IllegalStateException("Invalid ship tile name: " + name);
                }
                tiles.add(new Tile(tile.get("chunk_x").getAsInt(), tile.get("chunk_z").getAsInt(), name));
            }
            if (tiles.size() != manifest.get("tile_count").getAsInt() || tiles.isEmpty()) {
                throw new IllegalStateException("Ship tile count does not match the manifest");
            }
            // Fail before touching the world if a JAR is missing any of its ship tiles.
            for (Tile tile : tiles) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID,
                        "destiny_ship/" + tile.resource.substring(0, tile.resource.length() - 4));
                if (level.getStructureManager().get(id).isEmpty()) {
                    throw new IllegalStateException("Ship tile is missing: " + tile.resource);
                }
            }
            return new Progress(tiles, gateBlocks, saved.resetRequested, saved.resetSupport, level);
        } catch (Exception exception) {
            FAILED.add(server);
            saved.cancelReset();
            LOGGER.error("Cannot prepare opt-in Destiny ship installation", exception);
            return null;
        }
    }

    private static GateAnchor findGateAnchor(ServerLevel level, ShipInstallData saved, List<GateBlock> gateBlocks) {
        if (saved.gateAnchor != null && gateIntact(level, gateBlocks, saved.gateAnchor, saved.gateRotation))
            return new GateAnchor(saved.gateAnchor, saved.gateRotation);
        level.getChunk(GATE_CONTROLLER.getX() >> 4, GATE_CONTROLLER.getZ() >> 4);
        restoreMissingGateChevron(level, gateBlocks, GATE_CONTROLLER, 0);
        if (gateIntact(level, gateBlocks, GATE_CONTROLLER, 0)) return new GateAnchor(GATE_CONTROLLER, 0);
        // SGJourney may generate the Destiny gate at a different coordinate in
        // a fresh world. Resolve its physical controller from the gate network
        // and align our captured ship with it, leaving its address untouched.
        for (var record : StargateNetwork.get(level.getServer()).getStargatesInDimension(DESTINY)) {
            if (record.getPosition() == null) continue;
            BlockPos center = BlockPos.containing(record.getPosition());
            for (int x = center.getX() - 8; x <= center.getX() + 8; x++)
                for (int y = center.getY() - 8; y <= center.getY() + 8; y++)
                    for (int z = center.getZ() - 8; z <= center.getZ() + 8; z++) {
                        BlockPos candidate = new BlockPos(x, y, z);
                        if (level.getBlockEntity(candidate) instanceof AbstractStargateEntity<?>
                                && candidate.getY() == GATE_CONTROLLER.getY())
                            for (int rotation = 0; rotation < 4; rotation++)
                                if (gateIntact(level, gateBlocks, candidate, rotation))
                                    return new GateAnchor(candidate, rotation);
                    }
        }
        return null;
    }

    private static BlockPos pos(JsonArray values) {
        return new BlockPos(values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt());
    }

    private static void restoreMissingGateChevron(ServerLevel level, List<GateBlock> gateBlocks,
                                                  BlockPos anchor, int rotation) {
        BlockPos chevron = transformed(GATE_CHEVRON, anchor, rotation);
        BlockPos support = chevron.below();
        // dev-61 briefly removed this floor support during the original-room clear.
        // SGJourney's attached chevron consequently popped off. Repair only that
        // known failure when every other captured gate block still matches.
        if (!level.getBlockState(chevron).isAir()
                || level.getBlockState(support).isAir()
                || !(level.getBlockEntity(anchor) instanceof
                        net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity<?>)) return;
        boolean expectedChevron = false;
        for (GateBlock gate : gateBlocks) {
            if (gate.pos.equals(GATE_CHEVRON)) {
                expectedChevron = gate.name.equals("sgjourney:universe_stargate_chevron");
                continue;
            }
            BlockPos actual = transformed(gate.pos, anchor, rotation);
            ResourceLocation found = BuiltInRegistries.BLOCK.getKey(level.getBlockState(actual).getBlock());
            if (found == null || !found.toString().equals(gate.name)) return;
        }
        if (!expectedChevron) return;
        Block chevronBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("sgjourney", "universe_stargate_chevron"));
        if (!(chevronBlock instanceof ChevronBlock)) return;
        BlockState state = chevronBlock.defaultBlockState().setValue(ChevronBlock.ORIENTATION, FrontAndTop.UP_SOUTH);
        if (level.setBlock(chevron, state, Block.UPDATE_CLIENTS)) {
            LOGGER.warn("Restored the Destiny Stargate chevron displaced by the previous ship reset at {}", chevron);
        }
    }

    private static void clearOriginalRoomTile(ServerLevel level, Tile tile, Set<BlockPos> preservedGatePositions) {
        int minX = Math.max(ORIGINAL_ROOM_MIN.getX(), tile.chunkX << 4);
        int maxX = Math.min(ORIGINAL_ROOM_MAX.getX(), (tile.chunkX << 4) + 15);
        int minZ = Math.max(LEGACY_CORRIDOR_MIN_Z, tile.chunkZ << 4);
        int maxZ = Math.min(ORIGINAL_ROOM_MAX.getZ(), (tile.chunkZ << 4) + 15);
        if (minX > maxX || minZ > maxZ) return;
        for (BlockPos position : BlockPos.betweenClosed(minX, ORIGINAL_ROOM_MIN.getY(), minZ,
                maxX, ORIGINAL_ROOM_MAX.getY(), maxZ)) {
            BlockPos actual = shipPos(level, position);
            if (preservedGatePositions.contains(actual)) continue;
            if (!level.getBlockState(actual).isAir())
                level.setBlock(actual, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean gateIntact(ServerLevel level, List<GateBlock> gateBlocks,
                                      BlockPos anchor, int rotation) {
        for (GateBlock gate : gateBlocks) {
            BlockPos actual = transformed(gate.pos, anchor, rotation);
            level.getChunk(actual.getX() >> 4, actual.getZ() >> 4);
            ResourceLocation found = BuiltInRegistries.BLOCK.getKey(level.getBlockState(actual).getBlock());
            if (found == null || !found.toString().equals(gate.name)) return false;
        }
        return level.getBlockEntity(anchor) instanceof
                net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity<?>;
    }

    private record GateAnchor(BlockPos pos, int rotation) {}
    private record Tile(int chunkX, int chunkZ, String resource) {}
    private record ClearColumn(int chunkX, int chunkZ, int minX, int maxX, int minZ, int maxZ) {
        private int volume() {
            return (maxX - minX + 1) * (maxZ - minZ + 1)
                    * (CAPTURE_MAX.getY() - CAPTURE_MIN.getY() + 1);
        }
    }
    private record PreservedBlock(BlockPos pos, BlockState state, CompoundTag entityTag) {}
    private record ShipAirlock(int x, int y, int z, Direction facing) {}
    private record GateBlock(String name, BlockPos pos) {}

    private static final class SpawnerCleanupProgress {
        private final List<BlockPos> positions;
        private int next;
        private int removed;

        private SpawnerCleanupProgress(List<BlockPos> positions) {
            this.positions = positions;
        }
    }

    private static final class Progress {
        private final List<Tile> tiles;
        private final List<GateBlock> gateBlocks;
        private final Set<BlockPos> preservedGatePositions;
        private final List<PreservedBlock> protectedBlocks = new ArrayList<>();
        private final List<ClearColumn> clearColumns = new ArrayList<>();
        private final boolean reset;
        private int clearColumnIndex;
        private int clearCellIndex;
        private long clearedBlocks;
        private int nextTile;

        private Progress(List<Tile> tiles, List<GateBlock> gateBlocks, boolean reset,
                         BlockPos standingBlock, ServerLevel level) {
            this.tiles = tiles;
            this.gateBlocks = gateBlocks;
            this.reset = reset;
            preservedGatePositions = new HashSet<>();
            for (GateBlock gate : gateBlocks) {
                BlockPos gatePos = shipPos(level, gate.pos());
                preservedGatePositions.add(gatePos);
                snapshot(level, gatePos);
            }
            // This floor block supports the free-standing Universe chevron.
            BlockPos support = shipPos(level, GATE_CHEVRON_SUPPORT);
            preservedGatePositions.add(support);
            snapshot(level, support);
            if (reset) {
                BlockPos cornerA = shipPos(level, CAPTURE_MIN);
                BlockPos cornerB = shipPos(level, CAPTURE_MAX);
                int minX = Math.min(cornerA.getX(), cornerB.getX());
                int maxX = Math.max(cornerA.getX(), cornerB.getX());
                int minZ = Math.min(cornerA.getZ(), cornerB.getZ());
                int maxZ = Math.max(cornerA.getZ(), cornerB.getZ());
                if (standingBlock != null && standingBlock.getX() >= minX
                        && standingBlock.getX() <= maxX
                        && standingBlock.getY() >= CAPTURE_MIN.getY()
                        && standingBlock.getY() <= CAPTURE_MAX.getY()
                        && standingBlock.getZ() >= minZ
                        && standingBlock.getZ() <= maxZ) {
                    if (preservedGatePositions.add(standingBlock)) snapshot(level, standingBlock);
                }
                for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                    for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
                        clearColumns.add(new ClearColumn(chunkX, chunkZ,
                                Math.max(minX, chunkX << 4),
                                Math.min(maxX, (chunkX << 4) + 15),
                                Math.max(minZ, chunkZ << 4),
                                Math.min(maxZ, (chunkZ << 4) + 15)));
                    }
                }
            }
        }

        private void snapshot(ServerLevel level, BlockPos pos) {
            if (protectedBlocks.stream().anyMatch(block -> block.pos().equals(pos))) return;
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) return;
            BlockEntity entity = level.getBlockEntity(pos);
            protectedBlocks.add(new PreservedBlock(pos.immutable(), state,
                    entity == null ? null : entity.saveWithFullMetadata(level.registryAccess())));
        }

        private boolean clearComplete() {
            return clearColumnIndex == clearColumns.size();
        }

        private void clearNextBatch(ServerLevel level) {
            int budget = RESET_CLEAR_BLOCKS_PER_TICK;
            int changed = 0;
            int height = CAPTURE_MAX.getY() - CAPTURE_MIN.getY() + 1;
            BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
            while (budget > 0 && changed < RESET_CLEAR_CHANGES_PER_TICK && !clearComplete()) {
                ClearColumn column = clearColumns.get(clearColumnIndex);
                if (clearCellIndex == 0) level.getChunk(column.chunkX(), column.chunkZ());
                int width = column.maxX() - column.minX() + 1;
                int index = clearCellIndex++;
                int x = column.minX() + (index / height) % width;
                int y = CAPTURE_MIN.getY() + index % height;
                int z = column.minZ() + index / (height * width);
                position.set(x, y, z);
                if (!preservedGatePositions.contains(position) && !level.getBlockState(position).isAir()) {
                    if (!level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS))
                        throw new IllegalStateException("Unable to clear old ship block at " + position);
                    clearedBlocks++;
                    changed++;
                }
                budget--;
                if (clearCellIndex == column.volume()) {
                    clearColumnIndex++;
                    clearCellIndex = 0;
                }
            }
            if (clearComplete()) LOGGER.info("Cleared {} old Destiny blocks across the captured ship volume before reset", clearedBlocks);
        }

        private void restoreProtectedBlocks(ServerLevel level, Tile tile) {
            for (PreservedBlock block : protectedBlocks) {
                BlockPos pos = block.pos();
                if (tile != null) {
                    BlockPos captured = capturedPos(level, pos);
                    if ((captured.getX() >> 4) != tile.chunkX() || (captured.getZ() >> 4) != tile.chunkZ()) continue;
                }
                if (!level.getBlockState(pos).equals(block.state())) {
                    if (!level.setBlock(pos, block.state(), Block.UPDATE_CLIENTS))
                        throw new IllegalStateException("Protected standing or gate support block was lost at " + pos);
                }
                if (block.entityTag() != null) {
                    BlockEntity entity = level.getBlockEntity(pos);
                    if (entity == null) throw new IllegalStateException("Protected block entity was lost at " + pos);
                    entity.loadWithComponents(block.entityTag().copy(), level.registryAccess());
                    entity.setChanged();
                }
            }
        }
    }

    private static final class ShipInstallData extends SavedData {
        private static final String FILE_NAME = "sgjdestiny_ship_install";
        private boolean installed;
        private boolean resetRequested;
        private boolean spawnersCleaned;
        private BlockPos resetSupport;
        private BlockPos gateAnchor;
        private int gateRotation;

        private static ShipInstallData get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(ShipInstallData::new, ShipInstallData::load), FILE_NAME);
        }

        private static ShipInstallData load(CompoundTag tag, HolderLookup.Provider registries) {
            ShipInstallData data = new ShipInstallData();
            data.installed = tag.getBoolean("Installed");
            data.resetRequested = tag.getBoolean("ResetRequested");
            data.spawnersCleaned = tag.getBoolean("SpawnersCleaned");
            int[] support = tag.getIntArray("ResetSupport");
            if (support.length == 3) data.resetSupport = new BlockPos(support[0], support[1], support[2]);
            int[] anchor = tag.getIntArray("GateAnchor");
            if (anchor.length == 3) data.gateAnchor = new BlockPos(anchor[0], anchor[1], anchor[2]);
            data.gateRotation = Math.floorMod(tag.getInt("GateRotation"), 4);
            return data;
        }

        private void setGateAnchor(GateAnchor anchor) {
            if (!anchor.pos().equals(gateAnchor) || anchor.rotation() != gateRotation) {
                gateAnchor = anchor.pos().immutable();
                gateRotation = anchor.rotation();
                setDirty();
                LOGGER.info("Aligned bundled Destiny ship with SGJourney Stargate at {} ({} quarter turns)",
                        gateAnchor, gateRotation);
            }
        }

        private void requestReset(BlockPos standingBlock) {
            resetRequested = true;
            resetSupport = standingBlock == null ? null : standingBlock.immutable();
            setDirty();
        }

        private void cancelReset() {
            if (resetRequested) {
                resetRequested = false;
                resetSupport = null;
                setDirty();
            }
        }

        private void markInstalled() {
            installed = true;
            resetRequested = false;
            resetSupport = null;
            spawnersCleaned = true;
            setDirty();
        }

        private void markSpawnersCleaned() {
            spawnersCleaned = true;
            setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("Installed", installed);
            tag.putBoolean("ResetRequested", resetRequested);
            tag.putBoolean("SpawnersCleaned", spawnersCleaned);
            if (resetSupport != null) tag.putIntArray("ResetSupport",
                    new int[] {resetSupport.getX(), resetSupport.getY(), resetSupport.getZ()});
            if (gateAnchor != null) tag.putIntArray("GateAnchor",
                    new int[] {gateAnchor.getX(), gateAnchor.getY(), gateAnchor.getZ()});
            tag.putInt("GateRotation", gateRotation);
            return tag;
        }
    }
}
