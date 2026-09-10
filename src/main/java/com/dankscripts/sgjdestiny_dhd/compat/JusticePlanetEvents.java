package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Creates the half-buried non-Ancient wreck seen on the barren world in Justice. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class JusticePlanetEvents {
    private static final ResourceKey<Level> PLANET = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            new ResourceLocation("sgjourney", "justice_planet"));

    private JusticePlanetEvents() {}

    @SubscribeEvent
    public static void createWreck(ServerStartedEvent event) {
        ServerLevel level = event.getServer().getLevel(PLANET);
        if (level == null) return;
        // Keep the wreck discoverable, but far enough from the arrival site
        // that it reads as a separate expedition objective.
        int x = 264, z = 168;
        level.setChunkForced(x >> 4, z >> 4, true);
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z));
        BlockPos center = surface.above(1);
        BlockPos marker = center.offset(0, -2, 0);
        if (level.getBlockState(marker).is(Blocks.CRYING_OBSIDIAN)) return;
        buildWreck(level, center);
        level.setBlock(marker, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
    }

    private static void buildWreck(ServerLevel level, BlockPos center) {
        for (int x = -10; x <= 10; x++) {
            double taper = 1.0D - Math.max(0, Math.abs(x) - 6) / 5.0D;
            for (int y = -2; y <= 3; y++) for (int z = -3; z <= 3; z++) {
                double shape = (y / 2.8D) * (y / 2.8D) + (z / 3.1D) * (z / 3.1D);
                if (shape > taper || shape < Math.max(0, taper - 0.48D)) continue;
                BlockPos pos = center.offset(x, y, z);
                boolean rib = Math.floorMod(x, 4) == 0;
                level.setBlock(pos, (rib ? Blocks.OXIDIZED_CUT_COPPER : Blocks.POLISHED_BLACKSTONE_BRICKS)
                        .defaultBlockState(), 3);
            }
        }
        for (int x = -7; x <= 6; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
            level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        for (int z = -1; z <= 1; z++) level.setBlock(center.offset(-10, 1, z), Blocks.TINTED_GLASS.defaultBlockState(), 3);
        level.setBlock(center.offset(3, 0, -3), Blocks.IRON_TRAPDOOR.defaultBlockState(), 3);
        level.setBlock(center.offset(0, -2, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
    }
}
