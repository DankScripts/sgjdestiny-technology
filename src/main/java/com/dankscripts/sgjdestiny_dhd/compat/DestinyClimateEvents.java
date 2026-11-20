package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Keeps the interior-space Destiny dimension permanently free of weather. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinyClimateEvents {
    private static final ResourceLocation DESTINY = new ResourceLocation("sgjourney", "destiny");
    // Bounds of the bundled ship. Only remove exposed snow layers on its exterior.
    private static final int MIN_X = -165;
    private static final int MAX_X = 149;
    private static final int MIN_Z = -424;
    private static final int MAX_Z = -40;
    private static final Map<ServerLevel, Queue<ChunkPos>> SNOW_CHECKS = new WeakHashMap<>();

    private DestinyClimateEvents() {}

    @SubscribeEvent
    public static void keepDestinyClear(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().location().equals(DESTINY)) return;
        if (level.getGameTime() % 20L == 0L) {
            level.setWeatherParameters(24_000, 0, false, false);
        }
        level.setRainLevel(0.0F);
        level.setThunderLevel(0.0F);

        Queue<ChunkPos> pending;
        synchronized (SNOW_CHECKS) {
            pending = SNOW_CHECKS.get(level);
        }
        if (pending == null) return;
        for (int budget = 0; budget < 8; budget++) {
            ChunkPos chunk = pending.poll();
            if (chunk == null) break;
            if (!level.hasChunk(chunk.x, chunk.z)) continue;
            clearExposedSnow(level, chunk);
        }
    }

    @SubscribeEvent
    public static void checkLoadedShipChunk(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().location().equals(DESTINY)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        ChunkPos pos = chunk.getPos();
        int[] bounds = DestinyShipInstallEvents.shipBounds(level);
        if (pos.getMaxBlockX() < bounds[0] || pos.getMinBlockX() > bounds[1]
                || pos.getMaxBlockZ() < bounds[2] || pos.getMinBlockZ() > bounds[3]) return;
        synchronized (SNOW_CHECKS) {
            SNOW_CHECKS.computeIfAbsent(level, ignored -> new ConcurrentLinkedQueue<>()).add(pos);
        }
    }

    private static void clearExposedSnow(ServerLevel level, ChunkPos chunk) {
        int[] bounds = DestinyShipInstallEvents.shipBounds(level);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(chunk.getMinBlockX(), bounds[0]); x <= Math.min(chunk.getMaxBlockX(), bounds[1]); x++) {
            for (int z = Math.max(chunk.getMinBlockZ(), bounds[2]); z <= Math.min(chunk.getMaxBlockZ(), bounds[3]); z++) {
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                if (y < 47 || y > 118) continue;
                pos.set(x, y, z);
                if (level.getBlockState(pos).is(Blocks.SNOW)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}
