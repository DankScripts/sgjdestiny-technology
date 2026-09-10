package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Builds the immense artificial obelisk seen on Eden in SGU's Faith. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EdenPlanetEvents {
    private static final ResourceKey<Level> EDEN = ResourceKey.create(
            net.minecraft.core.registries.Registries.DIMENSION,
            new ResourceLocation("sgjourney", "eden"));
    private static final int OBELISK_X = 96;
    private static final int OBELISK_Z = 96;
    private static final int OBELISK_HEIGHT = 108;
    private static final int LAKE_X = OBELISK_X + 64;
    private static final int LAKE_Z = OBELISK_Z + 24;

    private EdenPlanetEvents() {}

    @SubscribeEvent
    public static void createObelisk(ServerStartedEvent event) {
        ServerLevel level = event.getServer().getLevel(EDEN);
        if (level == null) return;
        for (int cx = (OBELISK_X - 8) >> 4; cx <= (OBELISK_X + 8) >> 4; cx++)
            for (int cz = (OBELISK_Z - 8) >> 4; cz <= (OBELISK_Z + 8) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
            }

        BlockPos marker = new BlockPos(OBELISK_X, level.getMinBuildHeight() + 4, OBELISK_Z);
        BlockPos base = findExistingBase(level);
        if (base == null) {
            base = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,
                    new BlockPos(OBELISK_X, 0, OBELISK_Z));
        }
        buildObelisk(level, base);
        level.setBlock(marker, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 3);
        createLakeAndFood(level);
        updateNightBeam(level, base);
    }

    @SubscribeEvent
    public static void preventHostiles(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide()
                && event.getLevel().dimension().equals(EDEN)
                && event.getEntity() instanceof net.minecraft.world.entity.monster.Enemy) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void updateNightBeam(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        ServerLevel level = event.getServer().getLevel(EDEN);
        if (level == null) return;
        BlockPos base = findExistingBase(level);
        if (base != null) updateNightBeam(level, base);
    }

    private static BlockPos findExistingBase(ServerLevel level) {
        for (int y = level.getMinBuildHeight() + 5; y < level.getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(OBELISK_X, y, OBELISK_Z);
            if (level.getBlockState(pos).is(Blocks.OBSIDIAN)
                    || level.getBlockState(pos).is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                    || level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN)) {
                return pos;
            }
        }
        return null;
    }

    private static void updateNightBeam(ServerLevel level, BlockPos base) {
        BlockPos beacon = base.above(OBELISK_HEIGHT + 1);
        if (level.isNight()) {
            if (!level.getBlockState(beacon).is(Blocks.BEACON))
                level.setBlock(beacon, Blocks.BEACON.defaultBlockState(), 3);
        } else if (level.getBlockState(beacon).is(Blocks.BEACON)) {
            level.setBlock(beacon, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void createLakeAndFood(ServerLevel level) {
        BlockPos marker = new BlockPos(LAKE_X, level.getMinBuildHeight() + 4, LAKE_Z);
        if (level.getBlockState(marker).is(Blocks.REINFORCED_DEEPSLATE)) return;

        for (int cx = (LAKE_X - 16) >> 4; cx <= (LAKE_X + 16) >> 4; cx++)
            for (int cz = (LAKE_Z - 12) >> 4; cz <= (LAKE_Z + 12) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
                level.getChunk(cx, cz);
            }

        int waterY = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,
                new BlockPos(LAKE_X, 0, LAKE_Z)).getY() - 2;
        for (int x = -13; x <= 13; x++) for (int z = -9; z <= 9; z++) {
            double edge = (x * x) / 169.0 + (z * z) / 81.0;
            if (edge > 1.0) continue;
            int depth = edge < 0.38 ? 3 : (edge < 0.72 ? 2 : 1);
            BlockPos column = new BlockPos(LAKE_X + x, waterY, LAKE_Z + z);
            for (int y = 1; y <= 5; y++)
                level.setBlock(column.above(y), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(column.below(depth),
                    (edge < 0.55 ? Blocks.CLAY : Blocks.DIRT).defaultBlockState(), 3);
            for (int y = -depth + 1; y <= 0; y++)
                level.setBlock(column.offset(0, y, 0), Blocks.WATER.defaultBlockState(), 3);
        }

        int[][] food = {
                {-16, -4}, {-15, 5}, {-11, -11}, {-6, 12}, {4, 12},
                {12, 9}, {16, 3}, {15, -6}, {9, -12}, {-3, -13}
        };
        for (int i = 0; i < food.length; i++) {
            int x = LAKE_X + food[i][0];
            int z = LAKE_Z + food[i][1];
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, 0, z)).below();
            if (!level.getFluidState(ground.above()).isEmpty()) continue;
            level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            level.setBlock(ground.above(), i % 2 == 0
                    ? Blocks.MELON.defaultBlockState()
                    : Blocks.SWEET_BERRY_BUSH.defaultBlockState()
                            .setValue(SweetBerryBushBlock.AGE, 3), 3);
        }
        level.setBlock(marker, Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), 3);
    }

    private static void buildObelisk(ServerLevel level, BlockPos base) {
        // A restrained clearing keeps the monument visible without stripping
        // the surrounding forest and meadow landscape.
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++)
            for (int y = 0; y <= OBELISK_HEIGHT + 2; y++) {
                BlockPos pos = base.offset(x, y, z);
                if (level.getBlockState(pos).is(Blocks.OAK_LEAVES)
                        || level.getBlockState(pos).is(Blocks.BIRCH_LEAVES)
                        || level.getBlockState(pos).is(Blocks.OAK_LOG)
                        || level.getBlockState(pos).is(Blocks.BIRCH_LOG))
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }

        for (int y = 0; y <= OBELISK_HEIGHT; y++) {
            int radiusX = y < 2 ? 5 : (y < 5 ? 4 : (y > OBELISK_HEIGHT - 3 ? 2 : 3));
            int radiusZ = y < 2 ? 4 : (y < 5 ? 3 : 2);
            for (int x = -radiusX; x <= radiusX; x++) for (int z = -radiusZ; z <= radiusZ; z++) {
                boolean face = Math.abs(x) == radiusX || Math.abs(z) == radiusZ;
                boolean glyph = face && y > 6 && y < OBELISK_HEIGHT - 3 && y % 5 == 2
                        && ((Math.abs(x) == radiusX && z == 0)
                        || (Math.abs(z) == radiusZ && x == 0));
                level.setBlock(base.offset(x, y, z),
                        (glyph ? Blocks.CRYING_OBSIDIAN
                                : (face ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.OBSIDIAN))
                                .defaultBlockState(), 3);
            }
        }

        // A dark beacon pyramid is concealed inside the cap. The beacon itself
        // is added only at night and removed again at sunrise.
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            level.setBlock(base.offset(x, OBELISK_HEIGHT, z),
                    Blocks.NETHERITE_BLOCK.defaultBlockState(), 3);
    }
}
