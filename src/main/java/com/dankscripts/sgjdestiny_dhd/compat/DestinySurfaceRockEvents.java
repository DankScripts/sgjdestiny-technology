package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.Registries;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** GregTech's ore indicators have no ore to mark on the Destiny's ship deck. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class DestinySurfaceRockEvents {
    private static final ResourceKey<Level> DESTINY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("sgjourney", "destiny"));
    // The captured ship's bounds. Never change surface rocks in other dimensions.
    private static final int MIN_X = -165, MAX_X = 149;
    private static final int MIN_Y = 47, MAX_Y = 117;
    private static final int MIN_Z = -424, MAX_Z = -40;
    private static final ConcurrentHashMap<ServerLevel, Queue<ChunkPos>> PENDING = new ConcurrentHashMap<>();

    private DestinySurfaceRockEvents() {}

    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(DESTINY)
                || !(event.getChunk() instanceof LevelChunk chunk)) return;
        ChunkPos pos = chunk.getPos();
        int[] bounds = DestinyShipInstallEvents.shipBounds(level);
        if ((pos.x << 4) > bounds[1] || ((pos.x << 4) + 15) < bounds[0]
                || (pos.z << 4) > bounds[3] || ((pos.z << 4) + 15) < bounds[2]) return;
        // GTCEu injects indicator placement during biome decoration. Scan on
        // the server tick, after the generated chunk has finished loading.
        PENDING.computeIfAbsent(level, ignored -> new ConcurrentLinkedQueue<>()).add(pos);
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        
        ServerLevel level = event.getServer().getLevel(DESTINY);
        if (level == null) return;
        Queue<ChunkPos> queue = PENDING.get(level);
        if (queue == null) return;
        for (int i = 0; i < 4; i++) {
            ChunkPos pos = queue.poll();
            if (pos == null) break;
            if (level.hasChunk(pos.x, pos.z)) removeSurfaceRocks(level, level.getChunk(pos.x, pos.z));
        }
    }

    private static void removeSurfaceRocks(ServerLevel level, LevelChunk chunk) {
        int[] bounds = DestinyShipInstallEvents.shipBounds(level);
        int minX = Math.max(bounds[0], chunk.getPos().getMinBlockX());
        int maxX = Math.min(bounds[1], chunk.getPos().getMaxBlockX());
        int minZ = Math.max(bounds[2], chunk.getPos().getMinBlockZ());
        int maxZ = Math.min(bounds[3], chunk.getPos().getMaxBlockZ());
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            int sectionY = level.getMinBuildHeight() + index * 16;
            if (sectionY > MAX_Y || sectionY + 15 < MIN_Y) continue;
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir() || !section.getStates().maybeHas(DestinySurfaceRockEvents::isSurfaceRock))
                continue;
            for (int y = Math.max(MIN_Y, sectionY); y <= Math.min(MAX_Y, sectionY + 15); y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos at = new BlockPos(x, y, z);
                        if (isSurfaceRock(chunk.getBlockState(at)))
                            level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static boolean isSurfaceRock(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("gtceu") && id.getPath().endsWith("_indicator")
                && state.getBlock().getClass().getName()
                        .equals("com.gregtechceu.gtceu.common.block.SurfaceRockBlock");
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        PENDING.keySet().removeIf(level -> level.getServer() == event.getServer());
    }
}
