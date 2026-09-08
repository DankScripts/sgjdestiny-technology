package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModParticles;
import com.dankscripts.sgjdestiny_dhd.registry.ModSounds;
import com.dankscripts.sgjdestiny_dhd.block.LitDestinyBearingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.fml.ModList;
import net.povstalec.sgjourney.common.block_entities.dhd.CrystalDHDEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.blocks.ChevronBlock;
import net.povstalec.sgjourney.common.items.crystals.CommunicationCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.CrystalCache;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;

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
    private static final ResourceLocation UNIVERSE_CHEVRON =
            new ResourceLocation("sgjourney", "universe_stargate_chevron");
    // SGJ Deco's Destiny Bearing belongs on the upper gate assembly. Keep the
    // discovery box tight so a dial never walks a 33x33x33 cube or loads chunks.
    private static final int BEARING_HORIZONTAL_RADIUS = 5;
    private static final int BEARING_SEARCH_BELOW = 2;
    private static final int BEARING_SEARCH_ABOVE = 10;
    private static final int IDLE_DEBOUNCE_TICKS = 5;
    private static final int CHEVRON_FLASH_TICKS = 8;
    private static final int FLOOR_CHEVRON_RADIUS = 8;
    private static final int NINE_CHEVRON_LENGTH = 9;
    private static final int DISCHARGE_START_GRACE_TICKS = 8;
    private static final int DISCHARGE_INTERVAL_TICKS = 3;
    private static final int VENT_HORIZONTAL_RADIUS = 8;
    private static final int VENT_VERTICAL_RADIUS = 3;
    private static final int STEAM_DELAY_TICKS = 20;
    private static final int STEAM_DURATION_TICKS = 90;
    private static final int STEAM_INTERVAL_TICKS = 2;
    private static final Map<ConsoleKey, Set<BlockPos>> LIT_BY_CONSOLE = new HashMap<>();
    private static final Map<ConsoleKey, Set<BlockPos>> KNOWN_BY_CONSOLE = new HashMap<>();
    private static final Map<ConsoleKey, Long> LAST_GATE_ACTIVITY = new HashMap<>();
    private static final Map<ConsoleKey, Integer> LAST_CHEVRONS = new HashMap<>();
    private static final Map<ConsoleKey, Long> BEARING_FLASH_UNTIL = new HashMap<>();
    private static final Set<ConsoleKey> NINE_CHEVRON_DIALS = new HashSet<>();
    private static final Map<ConsoleKey, Set<BlockPos>> FLOOR_CHEVRONS = new HashMap<>();
    private static final Set<ConsoleKey> FLOOR_CHEVRON_LATCHED = new HashSet<>();
    private static final Set<ConsoleKey> ACTIVE_CONSOLES = new HashSet<>();
    private static final Set<ConsoleKey> INITIALIZED_CONSOLES = new HashSet<>();
    private static final Map<ConsoleKey, DischargeState> NINE_CHEVRON_DISCHARGES = new HashMap<>();
    private static final Map<ConsoleKey, SteamState> STEAM_RELEASES = new HashMap<>();

    private DestinyBearingCompat() {}

    /** Marks a known nine-symbol dial before rotation begins so its bearing stays powered. */
    public static void beginNineChevronDial(ServerLevel level, UniverseDHDEntity dhd) {
        ConsoleKey key = new ConsoleKey(level.dimension().location(), dhd.getBlockPos());
        NINE_CHEVRON_DIALS.add(key);
        lightBeforeDial(level, dhd);
    }

    /** Called before SGJourney accepts a symbol into this Destiny console. */
    public static void observeEncodedSymbol(ServerLevel level, AbstractDHDEntity dhd, int symbol) {
        if (!(dhd instanceof UniverseDHDEntity universeDhd) || dhd.getAddress() == null
                || dhd.getAddress().getLength() != NINE_CHEVRON_LENGTH - 1) {
            return;
        }
        beginNineChevronDischarge(level, universeDhd);
    }

    /** Fallback for addresses filled by another controller or integration. */
    public static void observeEngage(ServerLevel level, AbstractDHDEntity dhd) {
        if (dhd instanceof UniverseDHDEntity universeDhd && dhd.getAddress() != null
                && dhd.getAddress().getLength() == NINE_CHEVRON_LENGTH) {
            beginNineChevronDischarge(level, universeDhd);
        }
    }

    private static void beginNineChevronDischarge(ServerLevel level, UniverseDHDEntity dhd) {
        if (!ModList.get().isLoaded("sgj_deco") || ModBlocks.litDestinyBearing == null) return;
        AbstractStargateEntity gate = dhd.stargateCache.get();
        if (gate == null || gate.isRemoved() || gate.isConnected()) return;
        BlockPos gateCenter = gate.getCenterPos();
        int range = communicationRange(dhd);
        if (gateCenter.distSqr(dhd.getBlockPos()) > (long) range * range) return;

        ConsoleKey key = new ConsoleKey(level.dimension().location(), dhd.getBlockPos());
        NINE_CHEVRON_DIALS.add(key);
        lightBeforeDial(level, dhd);
        BlockPos bearing = resolveDischargeBearing(level, key, gateCenter);
        NINE_CHEVRON_DISCHARGES.put(key, new DischargeState(level.getGameTime(), bearing));
        LOGGER.info("[Destiny Bearing] nine-chevron discharge started at {} using bearing {}",
                dhd.getBlockPos(), bearing);
    }

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
        ConsoleKey key = new ConsoleKey(level.dimension().location(), dhd.getBlockPos());
        long gameTime = level.getGameTime();
        tickSteamRelease(level, key, gameTime);
        boolean bearingEnabled = ModList.get().isLoaded("sgj_deco") && ModBlocks.litDestinyBearing != null;
        AbstractStargateEntity gate = dhd.stargateCache.get();
        if (gate == null || gate.isRemoved()) {
            NINE_CHEVRON_DISCHARGES.remove(key);
            LAST_GATE_ACTIVITY.remove(key);
            LAST_CHEVRONS.remove(key);
            BEARING_FLASH_UNTIL.remove(key);
            NINE_CHEVRON_DIALS.remove(key);
            FLOOR_CHEVRON_LATCHED.remove(key);
            if (ACTIVE_CONSOLES.remove(key) && bearingEnabled) restoreTracked(level, key);
            setFloorChevronLit(level, null, false, key, false);
            if (dhd.isRemoved()) KNOWN_BY_CONSOLE.remove(key);
            return;
        }

        // Server-tick fallback: SGJourney integrations can populate the DHD
        // address without taking the normal encode-symbol injection path. As
        // long as nine symbols are visibly waiting at this Destiny console,
        // guarantee that its discharge state exists.
        if (!NINE_CHEVRON_DISCHARGES.containsKey(key) && dhd.getAddress() != null
                && dhd.getAddress().getLength() == NINE_CHEVRON_LENGTH) {
            beginNineChevronDischarge(level, dhd);
        }

        BlockPos gateCenter = gate.getCenterPos();
        int range = communicationRange(dhd);
        boolean canReadGate = gateCenter.distSqr(dhd.getBlockPos()) <= (long) range * range;
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
        tickNineChevronDischarge(level, dhd, gate, key, canReadGate, gameTime);
        int chevrons = gate.getChevronsEngaged();
        Integer previousChevrons = LAST_CHEVRONS.put(key, chevrons);
        if (bearingEnabled && canReadGate && previousChevrons != null && chevrons > previousChevrons) {
            BEARING_FLASH_UNTIL.put(key, gameTime + CHEVRON_FLASH_TICKS);
            LOGGER.info("[Destiny Bearing] chevron {} brightness flash at {}", chevrons, key.position());
        }
        boolean firstReconciliation = INITIALIZED_CONSOLES.add(key);
        Long flashUntil = BEARING_FLASH_UNTIL.get(key);
        boolean flashing = flashUntil != null && gameTime < flashUntil;
        boolean nineChevronDial = NINE_CHEVRON_DIALS.contains(key);
        boolean finalChevronLocked = chevrons == NINE_CHEVRON_LENGTH
                || (!nineChevronDial && chevrons == 7);
        if (previousChevrons != null && chevrons > previousChevrons && finalChevronLocked) {
            FLOOR_CHEVRON_LATCHED.add(key);
            LOGGER.info("[Destiny Floor Chevron] final chevron {} locked at {}", chevrons, key.position());
        }
        boolean bearingLit = canReadGate && (gate.isConnected() || flashing
                || (nineChevronDial && active));

        if (active) {
            ACTIVE_CONSOLES.add(key);
        } else {
            LAST_GATE_ACTIVITY.remove(key);
            LAST_CHEVRONS.put(key, 0);
            BEARING_FLASH_UNTIL.remove(key);
            boolean wasActive = ACTIVE_CONSOLES.remove(key);
            if (wasActive) beginSteamRelease(level, key, gateCenter, gate.getDirection(), gameTime);
            NINE_CHEVRON_DIALS.remove(key);
            FLOOR_CHEVRON_LATCHED.remove(key);
        }

        if (bearingEnabled) {
            updateBearings(level, gateCenter, bearingLit, key, bearingLit || firstReconciliation);
            if (bearingLit) setBearingFlashing(level, key, flashing);
            if (flashUntil != null && !flashing) BEARING_FLASH_UNTIL.remove(key);
        }

        // The horizontal Universe chevron in the floor is a connection-status
        // lamp: it stays dark through dialing and lights only for an established wormhole.
        boolean floorChevronLit = canReadGate
                && (FLOOR_CHEVRON_LATCHED.contains(key) || gate.isConnected());
        setFloorChevronLit(level, gateCenter, floorChevronLit, key,
                floorChevronLit || firstReconciliation);
    }

    private static void tickNineChevronDischarge(
            ServerLevel level, UniverseDHDEntity dhd, AbstractStargateEntity gate, ConsoleKey key,
            boolean canReadGate, long gameTime) {
        DischargeState discharge = NINE_CHEVRON_DISCHARGES.get(key);
        if (discharge == null) return;

        int chevrons = gate.getChevronsEngaged();
        long age = gameTime - discharge.startedAt;
        boolean awaitingEngage = dhd.getAddress() != null
                && dhd.getAddress().getLength() == NINE_CHEVRON_LENGTH;
        boolean gateMotion = gate.isDialingOut();
        if (gate instanceof UniverseStargateEntity universeGate) {
            gateMotion = gateMotion || universeGate.dialFromBuffer() || universeGate.isRotating();
        }

        boolean outgoingConnection = gate.getConnectionState()
                == StargateConnection.State.OUTGOING_CONNECTION;
        boolean incomingConnection = gate.getConnectionState()
                == StargateConnection.State.INCOMING_CONNECTION;

        // A successful outgoing nine-chevron connection continues drawing from
        // the bearing until it closes. Incoming wormholes never show this local
        // power draw. Failure, cancellation, loss of range, and reset stop it.
        if (!canReadGate || incomingConnection
                || (!awaitingEngage && !outgoingConnection && age > DISCHARGE_START_GRACE_TICKS
                    && !gateMotion && chevrons == 0)) {
            NINE_CHEVRON_DISCHARGES.remove(key);
            LOGGER.info("[Destiny Bearing] nine-chevron discharge stopped at {}", key.position());
            return;
        }

        if (gameTime % DISCHARGE_INTERVAL_TICKS != 0) return;
        BlockPos bearing = discharge.bearing;
        if (!isBearingAt(level, bearing)) {
            bearing = resolveDischargeBearing(level, key, gate.getCenterPos());
            discharge.bearing = bearing;
        }
        if (bearing == null) {
            if (!discharge.missingBearingReported) {
                discharge.missingBearingReported = true;
                LOGGER.warn("[Destiny Bearing] discharge active but no loaded bearing was found near {}",
                        gate.getCenterPos());
            }
            return;
        }
        emitDischarge(level, bearing, gate.getCenterPos(), gate.getDirection(), gameTime);
        if (!discharge.emissionConfirmed) {
            discharge.emissionConfirmed = true;
            LOGGER.info("[Destiny Bearing] discharge particles emitted from {} toward {}",
                    bearing, gate.getCenterPos());
        }
    }

    private static BlockPos resolveDischargeBearing(
            ServerLevel level, ConsoleKey key, BlockPos gateCenter) {
        Set<BlockPos> known = KNOWN_BY_CONSOLE.get(key);
        if (known != null) {
            for (BlockPos position : known) {
                if (isBearingAt(level, position)) return position;
            }
        }

        // Rebuild the small loaded-area cache once instead of silently dropping
        // every particle when transient ownership data is absent.
        updateBearings(level, gateCenter, true, key, true);
        known = KNOWN_BY_CONSOLE.get(key);
        if (known != null) {
            for (BlockPos position : known) {
                if (isBearingAt(level, position)) return position;
            }
        }
        return null;
    }

    private static boolean isBearingAt(ServerLevel level, BlockPos position) {
        if (position == null || !level.hasChunkAt(position)
                || !BuiltInRegistries.BLOCK.containsKey(DECO_BEARING)
                || ModBlocks.litDestinyBearing == null) {
            return false;
        }
        BlockState state = level.getBlockState(position);
        return state.is(BuiltInRegistries.BLOCK.get(DECO_BEARING))
                || state.is(ModBlocks.litDestinyBearing.get());
    }

    /** Sends two custom bolt particles; each client renders continuous branching geometry. */
    private static void emitDischarge(
            ServerLevel level, BlockPos bearing, BlockPos gateCenter,
            Direction gateFacing, long gameTime) {
        double planeX = -gateFacing.getStepZ();
        double planeZ = gateFacing.getStepX();
        // This is the exact visible-face offset used by 0.0.8-dev, the last
        // field-tested discharge geometry confirmed visible in this gate room.
        double faceOffsetX = gateFacing.getStepX() * 0.62;
        double faceOffsetZ = gateFacing.getStepZ() * 0.62;
        double startX = bearing.getX() + 0.5 + faceOffsetX;
        double startY = bearing.getY() + 0.08;
        double startZ = bearing.getZ() + 0.5 + faceOffsetZ;

        for (int arc = 0; arc < 2; arc++) {
            long seed = gameTime * 341873128712L + bearing.hashCode() * 132897987541L + arc * 7919L;
            // Strike the upper-left and upper-right shoulders of the ring rather
            // than its very top. This keeps the bearing close to the gate like
            // Destiny's set piece while leaving a clearly visible diagonal arc.
            double targetSide = (arc == 0 ? -1.0 : 1.0) * (1.95 + 0.30 * unitNoise(seed + 1));
            double targetX = gateCenter.getX() + 0.5 + faceOffsetX + planeX * targetSide;
            double targetY = gateCenter.getY() + 3.00 + 0.26 * unitNoise(seed + 2);
            double targetZ = gateCenter.getZ() + 0.5 + faceOffsetZ + planeZ * targetSide;
            double arcStartX = startX + planeX * (arc == 0 ? -0.16 : 0.16);
            double arcStartZ = startZ + planeZ * (arc == 0 ? -0.16 : 0.16);
            // particle count zero preserves these three deltas as the exact
            // target vector instead of applying vanilla random velocity.
            level.sendParticles(ModParticles.DESTINY_LIGHTNING.get(),
                    arcStartX, startY, arcStartZ, 0,
                    targetX - arcStartX, targetY - startY, targetZ - arcStartZ, 1.0);
        }
    }

    private static void beginSteamRelease(
            ServerLevel level, ConsoleKey key, BlockPos gateCenter, Direction gateFacing, long gameTime) {
        Set<BlockPos> vents = new HashSet<>();
        for (BlockPos candidate : BlockPos.betweenClosed(
                gateCenter.offset(-VENT_HORIZONTAL_RADIUS, -VENT_VERTICAL_RADIUS, -VENT_HORIZONTAL_RADIUS),
                gateCenter.offset(VENT_HORIZONTAL_RADIUS, VENT_VERTICAL_RADIUS, VENT_HORIZONTAL_RADIUS))) {
            if (!level.hasChunkAt(candidate)) continue;
            if (level.getBlockState(candidate).is(ModBlocks.DESTINY_FLOOR_VENT.get())) {
                vents.add(candidate.immutable());
            }
        }
        if (vents.isEmpty()) return;

        long startsAt = gameTime + STEAM_DELAY_TICKS;
        STEAM_RELEASES.put(key, new SteamState(
                startsAt, startsAt + STEAM_DURATION_TICKS, vents, gateCenter, gateFacing));
        LOGGER.info("[Destiny Vents] shutdown steam queued from {} vent(s) near {}",
                vents.size(), gateCenter);
    }

    private static void tickSteamRelease(ServerLevel level, ConsoleKey key, long gameTime) {
        SteamState steam = STEAM_RELEASES.get(key);
        if (steam == null) return;
        if (gameTime < steam.startsAt) return;
        if (gameTime >= steam.endsAt) {
            STEAM_RELEASES.remove(key);
            return;
        }
        if (!steam.started) {
            steam.started = true;
            for (BlockPos vent : steam.vents) {
                for (int puff = 0; puff < 30; puff++) {
                    emitSteamPuff(level, vent, steam.gateCenter, steam.gateFacing,
                            gameTime, puff, true);
                }
            }
            BlockPos soundCenter = steam.vents.iterator().next();
            level.playSound(null, soundCenter, ModSounds.STEAM_RELEASE.get(),
                    SoundSource.BLOCKS, 1.70F, 1.0F);
            LOGGER.info("[Destiny Vents] delayed shutdown steam started from {} vent(s) near {}",
                    steam.vents.size(), steam.gateCenter);
        }
        if (gameTime % STEAM_INTERVAL_TICKS != 0) return;

        double remaining = (steam.endsAt - gameTime) / (double) STEAM_DURATION_TICKS;
        int count = remaining > 0.25 ? 14 : 10;
        steam.vents.removeIf(position -> !level.hasChunkAt(position)
                || !level.getBlockState(position).is(ModBlocks.DESTINY_FLOOR_VENT.get()));
        for (BlockPos vent : steam.vents) {
            for (int puff = 0; puff < count; puff++) {
                emitSteamPuff(level, vent, steam.gateCenter, steam.gateFacing,
                        gameTime, puff, false);
            }
        }
        if (steam.vents.isEmpty()) STEAM_RELEASES.remove(key);
    }

    /** Emits a narrow outward-and-upward stream with equal axes: a true 45-degree jet. */
    private static void emitSteamPuff(
            ServerLevel level, BlockPos vent, BlockPos gateCenter, Direction gateFacing,
            long gameTime, int index, boolean burst) {
        double planeX = -gateFacing.getStepZ();
        double planeZ = gateFacing.getStepX();
        double side = (vent.getX() + 0.5 - (gateCenter.getX() + 0.5)) * planeX
                + (vent.getZ() + 0.5 - (gateCenter.getZ() + 0.5)) * planeZ;
        double sign = side < 0.0 ? -1.0 : side > 0.0 ? 1.0
                : ((vent.hashCode() & 1) == 0 ? -1.0 : 1.0);
        long seed = gameTime * 341873128712L + vent.hashCode() * 132897987541L + index * 7919L;
        double speed = (burst ? 0.145 : 0.115) * (1.0 + 0.045 * unitNoise(seed + 1));
        // Fill most of the grate with parallel lanes. Plane spread makes the
        // plume broad from the front; face spread gives it real depth instead
        // of stacking every sprite on one thin centerline.
        double planeSpread = 0.38 * unitNoise(seed + 2);
        double faceSpread = 0.28 * unitNoise(seed + 3);
        double x = vent.getX() + 0.5 + planeX * planeSpread
                + gateFacing.getStepX() * faceSpread;
        double y = vent.getY() + 1.015;
        double z = vent.getZ() + 0.5 + planeZ * planeSpread
                + gateFacing.getStepZ() * faceSpread;
        double velocityX = planeX * sign * speed;
        double velocityY = speed;
        double velocityZ = planeZ * sign * speed;
        level.sendParticles(ModParticles.DESTINY_STEAM.get(), x, y, z, 0,
                velocityX, velocityY, velocityZ, 1.0);
    }

    private static double unitNoise(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
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

    private static void setBearingFlashing(ServerLevel level, ConsoleKey key, boolean flashing) {
        Set<BlockPos> known = KNOWN_BY_CONSOLE.get(key);
        if (known == null || ModBlocks.litDestinyBearing == null) return;
        for (BlockPos position : known) {
            if (!level.hasChunkAt(position)) continue;
            BlockState state = level.getBlockState(position);
            if (state.is(ModBlocks.litDestinyBearing.get())
                    && state.hasProperty(LitDestinyBearingBlock.FLASHING)
                    && state.getValue(LitDestinyBearingBlock.FLASHING) != flashing) {
                level.setBlock(position, state.setValue(LitDestinyBearingBlock.FLASHING, flashing),
                        Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void setFloorChevronLit(
            ServerLevel level, BlockPos gateCenter, boolean lit, ConsoleKey key,
            boolean allowDiscovery) {
        if (!BuiltInRegistries.BLOCK.containsKey(UNIVERSE_CHEVRON)) return;
        Block floorChevron = BuiltInRegistries.BLOCK.get(UNIVERSE_CHEVRON);
        Block litFloorChevron = ModBlocks.LIT_DESTINY_FLOOR_CHEVRON.get();
        Set<BlockPos> known = FLOOR_CHEVRONS.computeIfAbsent(key, ignored -> new HashSet<>());
        known.removeIf(position -> {
            if (!level.hasChunkAt(position)) return false;
            BlockState state = level.getBlockState(position);
            return !state.is(floorChevron) && !state.is(litFloorChevron);
        });

        if (allowDiscovery && known.isEmpty() && gateCenter != null) {
            for (BlockPos candidate : BlockPos.betweenClosed(
                    gateCenter.offset(-FLOOR_CHEVRON_RADIUS, -4, -FLOOR_CHEVRON_RADIUS),
                    gateCenter.offset(FLOOR_CHEVRON_RADIUS, 0, FLOOR_CHEVRON_RADIUS))) {
                if (!level.hasChunkAt(candidate)) continue;
                BlockState state = level.getBlockState(candidate);
                if ((state.is(floorChevron) || state.is(litFloorChevron))
                        && candidate.getY() <= gateCenter.getY() - 1) {
                    known.add(candidate.immutable());
                    LOGGER.info("[Destiny Floor Chevron] discovered {} with orientation {}",
                            candidate, state.getValue(ChevronBlock.ORIENTATION));
                }
            }
        }

        for (BlockPos position : known) {
            if (!level.hasChunkAt(position)) continue;
            BlockState state = level.getBlockState(position);
            boolean isSteadyLit = state.is(litFloorChevron);
            if (lit && !isSteadyLit) {
                BlockState updated = copySharedProperties(state, litFloorChevron.defaultBlockState())
                        .setValue(ChevronBlock.LIT, true);
                level.setBlock(position, updated, Block.UPDATE_CLIENTS);
                level.getChunkSource().getLightEngine().checkBlock(position);
                LOGGER.info("[Destiny Floor Chevron] lit {}", position);
            } else if (!lit && isSteadyLit) {
                BlockState updated = copySharedProperties(state, floorChevron.defaultBlockState())
                        .setValue(ChevronBlock.LIT, false);
                level.setBlock(position, updated, Block.UPDATE_CLIENTS);
                level.getChunkSource().getLightEngine().checkBlock(position);
                LOGGER.info("[Destiny Floor Chevron] darkened {}", position);
            }
        }
        if (known.isEmpty()) FLOOR_CHEVRONS.remove(key);
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

    private static final class DischargeState {
        private final long startedAt;
        private BlockPos bearing;
        private boolean missingBearingReported;
        private boolean emissionConfirmed;

        private DischargeState(long startedAt, BlockPos bearing) {
            this.startedAt = startedAt;
            this.bearing = bearing;
        }
    }

    private static final class SteamState {
        private final long startsAt;
        private final long endsAt;
        private final Set<BlockPos> vents;
        private final BlockPos gateCenter;
        private final Direction gateFacing;
        private boolean started;

        private SteamState(long startsAt, long endsAt, Set<BlockPos> vents,
                           BlockPos gateCenter, Direction gateFacing) {
            this.startsAt = startsAt;
            this.endsAt = endsAt;
            this.vents = vents;
            this.gateCenter = gateCenter;
            this.gateFacing = gateFacing;
        }
    }
}
