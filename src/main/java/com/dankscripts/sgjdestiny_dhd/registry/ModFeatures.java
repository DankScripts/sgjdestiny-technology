package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, DestinyDHD.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> EDEN_BEDROCK_SEAL =
            FEATURES.register("eden_bedrock_seal", () -> new Feature<>(Codec.unit(NoneFeatureConfiguration.INSTANCE)) {
                @Override
                public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
                    int minX = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(context.origin().getX()));
                    int minZ = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(context.origin().getZ()));
                    int minY = context.level().getMinBuildHeight();
                    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = minY; y < minY + 3; y++) {
                                cursor.set(minX + x, y, minZ + z);
                                context.level().setBlock(cursor, Blocks.BEDROCK.defaultBlockState(), 2);
                            }
                        }
                    }
                    return true;
                }
            });

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> EDEN_SHALLOW_CAVE =
            FEATURES.register("eden_shallow_cave", () -> new Feature<>(Codec.unit(NoneFeatureConfiguration.INSTANCE)) {
                @Override
                public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
                    int chunkX = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(context.origin().getX()));
                    int chunkZ = SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(context.origin().getZ()));
                    int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

                    for (int attempt = 0; attempt < 12; attempt++) {
                        int x = chunkX + context.random().nextInt(16);
                        int z = chunkZ + context.random().nextInt(16);
                        int highY = context.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                        for (int[] direction : directions) {
                            int lowX = x + direction[0] * 4;
                            int lowZ = z + direction[1] * 4;
                            int lowY = context.level().getHeight(Heightmap.Types.WORLD_SURFACE_WG, lowX, lowZ);
                            if (highY - lowY < 5) continue;

                            int length = 6 + context.random().nextInt(5);
                            for (int step = 0; step < length; step++) {
                                int centerX = lowX - direction[0] * step;
                                int centerZ = lowZ - direction[1] * step;
                                int centerY = lowY + 2 + step / 4;
                                for (int ox = -1; ox <= 1; ox++) for (int oy = -1; oy <= 1; oy++)
                                    for (int oz = -1; oz <= 1; oz++) {
                                        if (ox * ox + oy * oy + oz * oz > 2) continue;
                                        cursor.set(centerX + ox, centerY + oy, centerZ + oz);
                                        if (!context.level().getBlockState(cursor).is(Blocks.BEDROCK))
                                            context.level().setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
                                    }
                            }
                            return true;
                        }
                    }
                    return false;
                }
            });

    private ModFeatures() {}

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }
}
