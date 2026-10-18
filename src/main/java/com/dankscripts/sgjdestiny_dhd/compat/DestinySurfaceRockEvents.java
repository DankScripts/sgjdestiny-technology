package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** GregTech's ore indicators have no ore to mark on the Destiny's ship deck. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinySurfaceRockEvents {
    private static final ResourceKey<Level> DESTINY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation("sgjourney", "destiny"));
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
        if ((pos.x << 4) > MAX_X || ((pos.x << 4) + 15) < MIN_X
                || (pos.z << 4) > MAX_Z || ((pos.z << 4) + 15) < MIN_Z) return;
        // GTCEu injects indicator placement during biome decoration. Scan on
        // the server tick, after the generated chunk has finished loading.
        PENDING.computeIfAbsent(level, ignored -> new ConcurrentLinkedQueue<>()).add(pos);
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
        int minX = Math.max(MIN_X, chunk.getPos().getMinBlockX());
        int maxX = Math.min(MAX_X, chunk.getPos().getMaxBlockX());
        int minZ = Math.max(MIN_Z, chunk.getPos().getMinBlockZ());
        int maxZ = Math.min(MAX_Z, chunk.getPos().getMaxBlockZ());
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
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && id.getNamespace().equals("gtceu") && id.getPath().endsWith("_indicator")
                && state.getBlock().getClass().getName()
                        .equals("com.gregtechceu.gtceu.common.block.SurfaceRockBlock");
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        PENDING.keySet().removeIf(level -> level.getServer() == event.getServer());
    }
}
