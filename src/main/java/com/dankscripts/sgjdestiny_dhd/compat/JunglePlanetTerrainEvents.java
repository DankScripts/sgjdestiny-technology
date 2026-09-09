package com.dankscripts.sgjdestiny_dhd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Repairs the deterministic Jungle Planet arrival area into a broad jungle island. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JunglePlanetTerrainEvents {
    private static final ResourceLocation JUNGLE_PLANET = new ResourceLocation("sgjourney", "jungle_planet");
    private static final Map<ServerLevel, ArrayDeque<ChunkPos>> PENDING = new WeakHashMap<>();
    private static final Map<ServerLevel, Set<Long>> QUEUED = new WeakHashMap<>();
    private static final Map<ServerLevel, BlockPos> GATES = new WeakHashMap<>();
    private static final int EXPECTED_CHUNK_X = 0;
    private static final int EXPECTED_CHUNK_Z = 8;
    private static final int LAND_RADIUS = 144;
    private static final int CLEAR_RADIUS = 28;

    private JunglePlanetTerrainEvents() {}

    @SubscribeEvent
    public static void load(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !isJungle(level)) return;
        enqueue(level, new ChunkPos(EXPECTED_CHUNK_X, EXPECTED_CHUNK_Z));
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || !isJungle(level)) return;
        ArrayDeque<ChunkPos> queue = PENDING.computeIfAbsent(level, ignored -> new ArrayDeque<>());
        BlockPos gate = GATES.get(level);
        if (gate == null) {
            LevelChunk anchor = level.getChunk(EXPECTED_CHUNK_X, EXPECTED_CHUNK_Z);
            gate = anchor.getBlockEntities().values().stream()
                    .filter(UniverseStargateEntity.class::isInstance)
                    .map(be -> be.getBlockPos()).findFirst().orElse(null);
            if (gate == null) return;
            GATES.put(level, gate);
            int radius = (LAND_RADIUS + 15) >> 4;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) enqueue(level,
                        new ChunkPos((gate.getX() >> 4) + dx, (gate.getZ() >> 4) + dz));
            }
        }
        for (int budget = 0; budget < 10 && !queue.isEmpty(); budget++) reshape(level, queue.removeFirst(), gate);
    }

    private static void enqueue(ServerLevel level, ChunkPos pos) {
        if (QUEUED.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.toLong()))
            PENDING.computeIfAbsent(level, ignored -> new ArrayDeque<>()).addLast(pos);
    }

    private static void reshape(ServerLevel level, ChunkPos chunk, BlockPos gate) {
        level.getChunk(chunk.x, chunk.z);
        RandomSource random = RandomSource.create(level.getSeed() ^ chunk.toLong() ^ 0x4A554E474C45L);
        int baseY = gate.getY() - 1;
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                double distance = Math.hypot(x - gate.getX(), z - gate.getZ());
                if (distance > LAND_RADIUS) continue;
                double edge = Math.max(0, (distance - LAND_RADIUS * .72) / (LAND_RADIUS * .28));
                int rolling = distance < CLEAR_RADIUS ? 0
                        : (int)Math.round(Math.sin(x * .075) * 2 + Math.cos(z * .063) * 2);
                int nativeY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                int target = (int)Math.round((baseY + rolling) * (1.0 - edge) + nativeY * edge);
                fillLand(level, x, z, target);
                if (distance > CLEAR_RADIUS && distance < LAND_RADIUS * .82 && random.nextInt(170) == 0)
                    plantTree(level, x, target + 1, z, random);
            }
        }
    }

    private static void fillLand(ServerLevel level, int x, int z, int targetY) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        for (int y = Math.min(surface + 1, targetY - 7); y <= targetY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.WATER)
                    || level.getBlockState(pos).is(Blocks.SEAGRASS) || level.getBlockState(pos).is(Blocks.KELP)) {
                level.setBlock(pos, y == targetY ? Blocks.GRASS_BLOCK.defaultBlockState()
                        : y >= targetY - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
            }
        }
        for (int y = targetY + 1; y <= Math.max(surface, targetY + 5); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).is(Blocks.WATER)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void plantTree(ServerLevel level, int x, int y, int z, RandomSource random) {
        BlockPos ground = new BlockPos(x, y - 1, z);
        if (!level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) return;
        int height = 7 + random.nextInt(5);
        for (int dy = 0; dy < height; dy++) {
            BlockPos trunk = new BlockPos(x, y + dy, z);
            if (!level.getBlockState(trunk).canBeReplaced()) return;
        }
        for (int dy = 0; dy < height; dy++) level.setBlock(new BlockPos(x, y + dy, z), Blocks.JUNGLE_LOG.defaultBlockState(), 2);
        for (int dy = height - 3; dy <= height + 1; dy++) {
            int radius = dy >= height ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextBoolean()) continue;
                BlockPos leaf = new BlockPos(x + dx, y + dy, z + dz);
                if (level.getBlockState(leaf).canBeReplaced()) level.setBlock(leaf, Blocks.JUNGLE_LEAVES.defaultBlockState(), 2);
            }
        }
    }

    private static boolean isJungle(ServerLevel level) {
        return level.dimension().location().equals(JUNGLE_PLANET);
    }
}
