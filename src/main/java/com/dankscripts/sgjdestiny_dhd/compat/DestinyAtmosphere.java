package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyOxygenGeneratorBlockEntity;
import com.dankscripts.sgjdestiny_dhd.block.DestinyCorridorCollisionBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/** Destiny's hull supplies oxygen; occupants produce CO2 that wall scrubbers remove. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinyAtmosphere {
    private static final ResourceLocation DESTINY = new ResourceLocation("sgjourney", "destiny");
    private static final WeakHashMap<ServerLevel, Set<BlockPos>> LOADED_BAYS = new WeakHashMap<>();
    private static final int MAX_CO2 = 24000;
    private static final int CO2_PER_PLAYER_PER_SECOND = 3;
    private DestinyAtmosphere() {}

    public static boolean isDestiny(ServerLevel level) { return level.dimension().location().equals(DESTINY); }

    public static void register(ServerLevel level, BlockPos pos) {
        LOADED_BAYS.computeIfAbsent(level, unused -> new HashSet<>()).add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        Set<BlockPos> bays = LOADED_BAYS.get(level);
        if (bays != null) bays.remove(pos);
    }

    public static int co2Percent(ServerLevel level) { return data(level).co2 * 100 / MAX_CO2; }
    public static int airPercent(ServerLevel level) { return 100 - co2Percent(level); }

    /** Count actual loaded scrubber units, not the two blocks making up each unit. */
    public static int loadedScrubberCount(ServerLevel level) {
        Set<BlockPos> bays = LOADED_BAYS.get(level);
        if (bays == null) return 0;
        int count = 0;
        for (BlockPos pos : bays)
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof DestinyOxygenGeneratorBlockEntity bay
                    && !bay.isRemoved()) count++;
        return count;
    }

    /** 0 = none loaded, 1 = running, 2 = dry fault, 3 = all switched off for service. */
    public static int scrubberStatus(ServerLevel level) {
        Set<BlockPos> bays = LOADED_BAYS.get(level);
        if (bays == null || bays.isEmpty()) return 0;
        boolean present = false;
        boolean running = false;
        for (BlockPos pos : bays) {
            if (!level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof DestinyOxygenGeneratorBlockEntity bay)) continue;
            present = true;
            if (bay.dryFault()) return 2;
            if (bay.enabled()) running = true;
        }
        return !present ? 0 : running ? 1 : 3;
    }

    /** Re-arm loaded dry scrubbers after their water and calcite have been replenished. */
    public static int resetScrubbers(ServerLevel level) {
        Set<BlockPos> bays = LOADED_BAYS.get(level);
        if (bays == null) return 0;
        int reset = 0;
        for (BlockPos pos : bays)
            if (level.hasChunkAt(pos)
                    && level.getBlockEntity(pos) instanceof DestinyOxygenGeneratorBlockEntity bay
                    && bay.resetAfterRefill()) reset++;
        return reset;
    }

    /** Preserve the operator command while treating 75% air quality as 25% CO2 buildup. */
    public static void resetAirReserve(ServerLevel level) {
        AirData atmosphere = data(level);
        atmosphere.co2 = MAX_CO2 / 4;
        atmosphere.setDirty();
    }

    /** Only enclosed spaces inside the captured Destiny hull have ship-supplied oxygen. */
    public static boolean isShipInterior(ServerLevel level, BlockPos pos) {
        if (!isDestiny(level) || pos.getX() < -165 || pos.getX() > 149
                || pos.getY() < 47 || pos.getY() > 117
                || pos.getZ() < -424 || pos.getZ() > -40) return false;
        // Destiny has no skylight, so canSeeSky is not a reliable roof test here.
        boolean roof = false;
        for (int dy = 2; dy <= 24; dy++) {
            BlockPos above = pos.above(dy);
            if (!level.hasChunkAt(above)) continue;
            var roofState = level.getBlockState(above);
            boolean corridorRoof = roofState.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())
                    && (roofState.getValue(DestinyCorridorCollisionBlock.PART) == DestinyCorridorCollisionBlock.ROOF
                    || roofState.getValue(DestinyCorridorCollisionBlock.PART) == DestinyCorridorCollisionBlock.ROOF_LIGHT);
            if (corridorRoof || Block.isShapeFullBlock(roofState.getCollisionShape(level, above))) {
                roof = true;
                break;
            }
        }
        if (!roof) return false;
        int walls = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int distance = 1; distance <= 24; distance++) {
                BlockPos side = pos.relative(direction, distance).above();
                if (!level.hasChunkAt(side)) continue;
                if (Block.isShapeFullBlock(level.getBlockState(side).getCollisionShape(level, side))) {
                    walls++;
                    break;
                }
            }
        }
        return walls >= 2;
    }

    public static boolean breathable(ServerLevel level, BlockPos pos) {
        return isShipInterior(level, pos);
    }

    /** Maximum mix a running scrubber can use each second, by crew load. */
    private static int scrubberRate(int occupants) {
        if (occupants <= 1) return 3;
        if (occupants <= 6) return 4;
        if (occupants <= 10) return 5;
        return 6;
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !isDestiny(level) || level.getGameTime() % 20 != 0) return;
        AirData data = data(level);
        int occupants = 0;
        for (ServerPlayer player : level.players())
            if (!player.isCreative() && !player.isSpectator() && isShipInterior(level, player.blockPosition())) occupants++;
        data.co2 = Math.min(MAX_CO2, data.co2 + occupants * CO2_PER_PLAYER_PER_SECOND);
        Set<BlockPos> bays = LOADED_BAYS.get(level);
        if (bays != null) {
            bays.removeIf(pos -> !level.hasChunkAt(pos)
                    || !level.getBlockState(pos).is(ModBlocks.DESTINY_OXYGEN_GENERATOR.get())
                    || !(level.getBlockEntity(pos) instanceof DestinyOxygenGeneratorBlockEntity));
            for (BlockPos pos : bays) {
                DestinyOxygenGeneratorBlockEntity bay = (DestinyOxygenGeneratorBlockEntity) level.getBlockEntity(pos);
                if (bay == null) continue;
                bay.processBatch();
                if (bay.enabled() && data.co2 > 0)
                    data.co2 -= bay.scrub(Math.min(scrubberRate(occupants), data.co2));
                bay.latchDryFaultIfEmpty();
            }
        }
        data.setDirty();
    }

    private static AirData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AirData::load, AirData::new, "sgjdestiny_ship_air");
    }

    private static final class AirData extends SavedData {
        // Existing ships begin with 75% clean air while scrubbers are fitted.
        private int co2 = MAX_CO2 / 4;
        private static AirData load(CompoundTag tag) {
            AirData data = new AirData();
            if (tag.contains("CO2")) data.co2 = Math.max(0, Math.min(MAX_CO2, tag.getInt("CO2")));
            else if (tag.contains("Air")) data.co2 = MAX_CO2 - Math.max(0, Math.min(MAX_CO2, tag.getInt("Air")));
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            tag.putInt("CO2", co2);
            return tag;
        }
    }
}
