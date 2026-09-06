package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.ModList;
import net.povstalec.sgjourney.common.block_entities.dhd.CrystalDHDEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.items.crystals.CommunicationCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.CrystalCache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Optional SGJ Deco bridge driven directly by the proven Universe DHD ticker. */
public final class DestinyBearingCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Bearing");
    private static final ResourceLocation DECO_BEARING = new ResourceLocation("sgj_deco", "destiny_bearing");
    // SGJ Deco's Destiny Bearing belongs on the upper gate assembly. Keep the
    // discovery box tight so a dial never walks a 33x33x33 cube or loads chunks.
    private static final int BEARING_HORIZONTAL_RADIUS = 5;
    private static final int BEARING_SEARCH_BELOW = 2;
    private static final int BEARING_SEARCH_ABOVE = 10;
    private static final int IDLE_DEBOUNCE_TICKS = 5;
    private static final Map<ConsoleKey, Set<BlockPos>> LIT_BY_CONSOLE = new HashMap<>();
    private static final Map<ConsoleKey, Set<BlockPos>> KNOWN_BY_CONSOLE = new HashMap<>();
    private static final Map<ConsoleKey, Long> LAST_GATE_ACTIVITY = new HashMap<>();
    private static final Set<ConsoleKey> ACTIVE_CONSOLES = new HashSet<>();
    private static final Set<ConsoleKey> INITIALIZED_CONSOLES = new HashSet<>();

    private DestinyBearingCompat() {}

    /** Lights nearby Destiny bearings before SGJourney begins the dialing call. */
    public static void lightBeforeDial(ServerLevel level, UniverseDHDEntity dhd) {
        if (!ModList.get().isLoaded("sgj_deco") || ModBlocks.litDestinyBearing == null) {
            return;
        }

        AbstractStargateEntity gate = dhd.stargateCache.get();
        if (gate == null || gate.isRemoved() || gate.isConnected()) {
            return;
        }

        BlockPos gateCenter = gate.getCenterPos();
        int range = communicationRange(dhd);
        if (gateCenter.distSqr(dhd.getBlockPos()) <= (long) range * range) {
            ConsoleKey key = new ConsoleKey(level.dimension().location(), dhd.getBlockPos());
            INITIALIZED_CONSOLES.add(key);
            ACTIVE_CONSOLES.add(key);
            // Use the remembered bearing directly. Discovery runs only when this
            // console has no valid cached bearing position yet.
            updateBearings(level, gateCenter, true, key, true);
        }
    }

    public static void tickFromDHD(ServerLevel level, UniverseDHDEntity dhd) {
        if (!ModList.get().isLoaded("sgj_deco") || ModBlocks.litDestinyBearing == null) {
            return;
        }

        ConsoleKey key = new ConsoleKey(level.dimension().location(), dhd.getBlockPos());
        AbstractStargateEntity gate = dhd.stargateCache.get();
        if (gate == null || gate.isRemoved()) {
            LAST_GATE_ACTIVITY.remove(key);
            if (ACTIVE_CONSOLES.remove(key)) restoreTracked(level, key);
            if (dhd.isRemoved()) KNOWN_BY_CONSOLE.remove(key);
            return;
        }

        BlockPos gateCenter = gate.getCenterPos();
        int range = communicationRange(dhd);
        boolean canReadGate = gateCenter.distSqr(dhd.getBlockPos()) <= (long) range * range;
        long gameTime = level.getGameTime();
        boolean gateActive = isGateActive(gate);
        if (gateActive) DestinyDialDelay.gateActivityObserved(dhd);
        boolean preDialHold = DestinyDialDelay.shouldHoldLight(dhd, gameTime);
        if (gateActive || preDialHold) {
            LAST_GATE_ACTIVITY.put(key, gameTime);
        }
        Long lastActivity = LAST_GATE_ACTIVITY.get(key);
        boolean debounceActive = ACTIVE_CONSOLES.contains(key) && lastActivity != null
                && gameTime - lastActivity <= IDLE_DEBOUNCE_TICKS;
        boolean active = canReadGate && (preDialHold || gateActive || debounceActive);
        boolean firstReconciliation = INITIALIZED_CONSOLES.add(key);
        if (active) {
            if (ACTIVE_CONSOLES.add(key)) {
                updateBearings(level, gateCenter, true, key, true);
            }
        } else {
            LAST_GATE_ACTIVITY.remove(key);
            boolean wasActive = ACTIVE_CONSOLES.remove(key);
            restoreTracked(level, key);
            if (firstReconciliation) {
                // A lit replacement can survive a save/reload while the transient
                // ownership map cannot. Reconcile it once using the small, loaded
                // upper-gate search box. Normal active-to-idle changes use the exact
                // tracked positions above and do not scan the world at all.
                updateBearings(level, gateCenter, false, key, true);
            } else if (wasActive) {
                // Validate a remembered position without performing discovery.
                updateBearings(level, gateCenter, false, key, false);
            }
        }
    }

    private static boolean isGateActive(AbstractStargateEntity gate) {
        // The encoded-symbol collection can briefly remain populated after a rapid
        // cancel/reset, even though the Universe gate has returned to idle.  Using
        // it as an activity latch could therefore leave the bearing lit forever.
        // The Universe gate's dialing buffer and rotation state are its authoritative
        // in-progress signals and are reset with the gate.
        if (gate instanceof UniverseStargateEntity universeGate) {
            return universeGate.isConnected() || universeGate.isDialingOut()
                    || universeGate.dialFromBuffer() || universeGate.isRotating()
                    || universeGate.getChevronsEngaged() > 0;
        }
        return gate.isConnected() || gate.isDialingOut() || gate.getChevronsEngaged() > 0;
    }

    private static int communicationRange(UniverseDHDEntity dhd) {
        int range = 16;
        for (CrystalCache<CrystalDHDEntity>.Slot<CommunicationCrystalItem> slot
                : dhd.crystalCache.communicationCrystals().getSlots()) {
            range += slot.crystal.getRangeIncrease();
        }
        return range;
    }

    private static void updateBearings(
            ServerLevel level, BlockPos center, boolean lit, ConsoleKey key, boolean allowDiscovery) {
        if (!BuiltInRegistries.BLOCK.containsKey(DECO_BEARING)) return;
        Block original = BuiltInRegistries.BLOCK.get(DECO_BEARING);
        Block illuminated = ModBlocks.litDestinyBearing.get();
        Set<BlockPos> tracked = LIT_BY_CONSOLE.computeIfAbsent(key, ignored -> new HashSet<>());
        Set<BlockPos> known = KNOWN_BY_CONSOLE.computeIfAbsent(key, ignored -> new HashSet<>());

        known.removeIf(position -> !applyBearingState(level, position, lit, original, illuminated, tracked));

        if (allowDiscovery && known.isEmpty()) {
            discoverBearings(level, center, lit, original, illuminated, tracked, known);
        }

        if (tracked.isEmpty()) LIT_BY_CONSOLE.remove(key);
        if (known.isEmpty()) KNOWN_BY_CONSOLE.remove(key);
    }

    private static void discoverBearings(
            ServerLevel level, BlockPos center, boolean lit, Block original, Block illuminated,
            Set<BlockPos> tracked, Set<BlockPos> known) {

        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-BEARING_HORIZONTAL_RADIUS, -BEARING_SEARCH_BELOW, -BEARING_HORIZONTAL_RADIUS),
                center.offset(BEARING_HORIZONTAL_RADIUS, BEARING_SEARCH_ABOVE, BEARING_HORIZONTAL_RADIUS))) {
            // hasChunkAt is essential here: getBlockState must never synchronously
            // pull an unloaded chunk into an integrated ATM9 server during dialing.
            if (!level.hasChunkAt(candidate)) continue;
            BlockPos position = candidate.immutable();
            BlockState current = level.getBlockState(position);
            if (current.is(original) || current.is(illuminated)) {
                known.add(position);
                applyBearingState(level, position, lit, original, illuminated, tracked);
            }
        }
    }

    /** @return whether this remains a valid cached bearing position. */
    private static boolean applyBearingState(
            ServerLevel level, BlockPos position, boolean lit, Block original, Block illuminated,
            Set<BlockPos> tracked) {
        if (!level.hasChunkAt(position)) return true;
        BlockState current = level.getBlockState(position);
        if (!current.is(original) && !current.is(illuminated)) {
            tracked.remove(position);
            return false;
        }

        if (lit && current.is(illuminated)) {
            // Re-adopt a replacement left behind by an earlier session.
            tracked.add(position);
            return true;
        }

        if ((lit && current.is(original)) || (!lit && current.is(illuminated))) {
            Block target = lit ? illuminated : original;
            level.setBlock(position, copySharedProperties(current, target.defaultBlockState()), Block.UPDATE_ALL);
            level.getChunkSource().getLightEngine().checkBlock(position);
            if (lit) tracked.add(position); else tracked.remove(position);
            LOGGER.info("[Destiny Bearing] {} {}", lit ? "lit" : "restored", position);
        }
        return true;
    }

    private static void restoreTracked(ServerLevel level, ConsoleKey key) {
        Set<BlockPos> tracked = LIT_BY_CONSOLE.get(key);
        if (tracked == null || tracked.isEmpty() || !BuiltInRegistries.BLOCK.containsKey(DECO_BEARING)) return;
        Block original = BuiltInRegistries.BLOCK.get(DECO_BEARING);
        Block illuminated = ModBlocks.litDestinyBearing.get();
        Iterator<BlockPos> iterator = tracked.iterator();
        while (iterator.hasNext()) {
            BlockPos position = iterator.next();
            if (!level.hasChunkAt(position)) continue;
            BlockState current = level.getBlockState(position);
            if (current.is(illuminated)) {
                level.setBlock(position, copySharedProperties(current, original.defaultBlockState()), Block.UPDATE_ALL);
                level.getChunkSource().getLightEngine().checkBlock(position);
                LOGGER.info("[Destiny Bearing] restored {}", position);
            }
            iterator.remove();
        }
        if (tracked.isEmpty()) LIT_BY_CONSOLE.remove(key);
    }

    private static BlockState copySharedProperties(BlockState source, BlockState target) {
        for (Property<?> property : source.getProperties()) {
            if (target.hasProperty(property)) target = copyProperty(source, target, property);
        }
        return target;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState source, BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    private record ConsoleKey(ResourceLocation dimension, BlockPos position) {}
}
