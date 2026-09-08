package com.dankscripts.sgjdestiny_dhd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.Tags;

/** Weather and sparse cliff-bound ice formations for SGU's Water Planet. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WaterPlanetClimateEvents {
    private static final ResourceLocation WATER_PLANET = new ResourceLocation("sgjourney", "water_planet");
    private static final java.util.Map<ServerLevel, java.util.Set<Long>> CHECKED_CHUNKS = new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerLevel, java.util.ArrayDeque<ChunkPos>> PENDING_CHUNKS = new java.util.WeakHashMap<>();
    private static final java.util.Map<ServerLevel, Integer> LANDING_HEIGHTS = new java.util.WeakHashMap<>();
    private static final int GATE_X = 0;
    private static final int GATE_Z = 800;
    private static final int FLAT_RADIUS = 208;
    // Give the terrain almost 200 blocks to rise from the landing plain into the
    // native mountains. The old 64-block blend could expose sliced mountain faces.
    private static final int BLEND_RADIUS = 400;

    private WaterPlanetClimateEvents() {}

    @SubscribeEvent
    public static void keepSnowing(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.dimension().location().equals(WATER_PLANET) && level.getGameTime() % 20L == 0L) {
            level.setWeatherParameters(0, 24_000, true, false);
        }
        if (!level.dimension().location().equals(WATER_PLANET)) return;
        java.util.ArrayDeque<ChunkPos> queue = PENDING_CHUNKS.get(level);
        if (queue == null || queue.isEmpty()) return;
        // Prepare the complete landing zone during world startup, before an arriving
        // player can watch terrain disappear. Spread the one-time work across a few ticks.
        for (int budget = 0; budget < 16 && !queue.isEmpty(); budget++) {
            ChunkPos cp = queue.removeFirst();
            LevelChunk loaded = level.getChunk(cp.x, cp.z);
            findGateHeight(level, loaded);
            if (chunkTouchesLandingZone(cp)) {
                Integer landingY = LANDING_HEIGHTS.get(level);
                if (landingY == null) {
                    queue.addLast(cp);
                    continue;
                }
                flattenLandingZone(level, cp, landingY);
            } else {
                generateFrozenWaterfall(level, cp);
            }
        }
    }

    @SubscribeEvent
    public static void preloadLandingZone(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().location().equals(WATER_PLANET)) return;
        java.util.ArrayDeque<ChunkPos> queue = PENDING_CHUNKS.computeIfAbsent(level,
                ignored -> new java.util.ArrayDeque<>());
        java.util.Set<Long> checked = CHECKED_CHUNKS.computeIfAbsent(level,
                ignored -> new java.util.HashSet<>());
        int gateChunkX = GATE_X >> 4;
        int gateChunkZ = GATE_Z >> 4;
        int chunkRadius = (BLEND_RADIUS + 15) >> 4;
        java.util.List<ChunkPos> landingChunks = new java.util.ArrayList<>();
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos cp = new ChunkPos(gateChunkX + dx, gateChunkZ + dz);
                if (chunkTouchesLandingZone(cp)) landingChunks.add(cp);
            }
        }
        landingChunks.sort(java.util.Comparator.comparingLong(cp ->
                (long) (cp.x - gateChunkX) * (cp.x - gateChunkX)
                        + (long) (cp.z - gateChunkZ) * (cp.z - gateChunkZ)));
        for (ChunkPos cp : landingChunks) {
            if (checked.add(cp.toLong())) queue.addLast(cp);
        }
    }

    @SubscribeEvent
    public static void addFrozenWaterfalls(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!level.dimension().location().equals(WATER_PLANET)) return;
        ChunkPos cp = chunk.getPos();
        findGateHeight(level, chunk);
        if (!CHECKED_CHUNKS.computeIfAbsent(level, ignored -> new java.util.HashSet<>()).add(cp.toLong())) return;
        PENDING_CHUNKS.computeIfAbsent(level, ignored -> new java.util.ArrayDeque<>()).addLast(cp);
    }

    private static void findGateHeight(ServerLevel level, LevelChunk chunk) {
        chunk.getBlockEntities().values().stream()
                .filter(UniverseStargateEntity.class::isInstance)
                .map(blockEntity -> blockEntity.getBlockPos().getY())
                .findFirst().ifPresent(y -> LANDING_HEIGHTS.put(level, y));
    }

    private static boolean chunkTouchesLandingZone(ChunkPos cp) {
        int nearestX = Math.max(cp.getMinBlockX(), Math.min(GATE_X, cp.getMaxBlockX()));
        int nearestZ = Math.max(cp.getMinBlockZ(), Math.min(GATE_Z, cp.getMaxBlockZ()));
        long dx = nearestX - GATE_X;
        long dz = nearestZ - GATE_Z;
        return dx * dx + dz * dz <= (long) BLEND_RADIUS * BLEND_RADIUS;
    }

    private static void flattenLandingZone(ServerLevel level, ChunkPos cp, int gateBaseY) {
        int flatY = gateBaseY;
        for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++) {
            for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                double distance = Math.sqrt((double) (x - GATE_X) * (x - GATE_X)
                        + (double) (z - GATE_Z) * (z - GATE_Z));
                if (distance > BLEND_RADIUS) continue;
                int oldSurface = surface(level, x, z);
                double blend = Math.max(0.0, Math.min(1.0,
                        (distance - FLAT_RADIUS) / (BLEND_RADIUS - FLAT_RADIUS)));
                blend = blend * blend * (3.0 - 2.0 * blend);
                int target = (int) Math.round(flatY + (oldSurface - flatY) * blend);
                reshapeNaturalColumn(level, x, z, oldSurface, target);
            }
        }
    }

    private static void reshapeNaturalColumn(ServerLevel level, int x, int z, int oldSurface, int target) {
        if (oldSurface > target) {
            for (int y = oldSurface; y > target; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (isNaturalTerrain(level.getBlockState(pos))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        } else if (oldSurface < target) {
            for (int y = oldSurface + 1; y <= target; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (level.getBlockState(pos).canBeReplaced()) {
                    level.setBlock(pos, y == target ? Blocks.SNOW_BLOCK.defaultBlockState()
                            : Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        BlockPos cap = new BlockPos(x, target, z);
        if (isNaturalTerrain(level.getBlockState(cap))) level.setBlock(cap, Blocks.SNOW_BLOCK.defaultBlockState(), 2);
    }

    private static boolean isNaturalTerrain(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
                || state.is(Tags.Blocks.ORES)
                || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.DIRT)
                || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE) || state.is(Blocks.WATER)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SAND) || state.is(Blocks.CLAY)
                || state.is(Blocks.TUFF) || state.is(Blocks.CALCITE);
    }

    private static void generateFrozenWaterfall(ServerLevel level, ChunkPos cp) {
        RandomSource random = RandomSource.create(level.getSeed() ^ cp.toLong() ^ 0x574154455246414CL);
        if (random.nextInt(4) != 0) return;

        for (int attempt = 0; attempt < 18; attempt++) {
            int x = cp.getMinBlockX() + 2 + random.nextInt(12);
            int z = cp.getMinBlockZ() + 2 + random.nextInt(12);
            if (tryWaterfall(level, x, z, random)) return;
        }
    }

    private static boolean tryWaterfall(ServerLevel level, int x, int z, RandomSource random) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int highY = surface(level, x, z);
        for (int[] direction : directions) {
            int dx = direction[0];
            int dz = direction[1];
            int previousY = highY;
            for (int step = 2; step <= 16; step += 2) {
                int nextX = x + dx * step;
                int nextZ = z + dz * step;
                int nextY = surface(level, nextX, nextZ);
                // Waterfalls belong on elevated cliff faces, not on low fissure walls.
                if (highY >= 86 && highY - nextY >= 14) {
                    buildCurtain(level, nextX, nextZ, highY, nextY, dx, dz, random);
                    return true;
                }
                previousY = nextY;
            }
        }
        return false;
    }

    private static void buildCurtain(ServerLevel level, int x, int z, int top, int bottom,
                                     int dx, int dz, RandomSource random) {
        int halfWidth = 1 + random.nextInt(2);
        int px = -dz;
        int pz = dx;
        for (int width = -halfWidth; width <= halfWidth; width++) {
            int wx = x + px * width;
            int wz = z + pz * width;
            int localBottom = Math.max(bottom + 1, top - 24 - random.nextInt(9));
            for (int y = top; y >= localBottom; y--) {
                BlockPos pos = new BlockPos(wx, y, wz);
                if (!level.getBlockState(pos).canBeReplaced()) continue;
                level.setBlock(pos, random.nextInt(7) == 0 ? Blocks.BLUE_ICE.defaultBlockState()
                        : Blocks.PACKED_ICE.defaultBlockState(), 2);
            }
        }
    }

    private static int surface(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }
}
