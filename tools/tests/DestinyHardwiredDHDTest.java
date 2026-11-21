package com.dankscripts.sgjdestiny_dhd.compat;

import net.povstalec.sgjourney.common.misc.AutoCache;

public final class DestinyHardwiredDHDTest {
    private static final double SHIP_DISTANCE_SQUARED = 306;

    public static void main(String[] args) {
        check(DestinyHardwiredDHD.energyTarget(0, 0, 200_000) == 600_000, "three-crystal energy target");
        check(DestinyHardwiredDHD.energyTarget(800_000, 800_000, 200_000) == 800_000,
                "larger native target must be preserved");
        check(DestinyHardwiredDHD.energyTarget(100_000, -1, 200_000) == 100_000,
                "native unlimited-target sentinel must respect gate capacity");
        check(DestinyHardwiredDHD.energyTarget(0, 0, Long.MAX_VALUE) == Long.MAX_VALUE,
                "configured energy target must not overflow");
        check(DestinyHardwiredDHD.energyTransfer(0, 0, 20_000) == 20_000, "hardwired transfer");
        check(DestinyHardwiredDHD.energyTransfer(40_000, 40_000, 20_000) == 40_000,
                "larger native transfer must be preserved");
        check(DestinyHardwiredDHD.energyTransfer(5_000, -1, 20_000) == 5_000,
                "native unlimited-transfer sentinel must respect DHD configuration");
        check(DestinyHardwiredDHD.connectionRange(16) == 32, "hardwired range floor");
        check(DestinyHardwiredDHD.connectionRange(48) == 48, "larger native range");
        check(!DestinyHardwiredDHD.canDiscover(SHIP_DISTANCE_SQUARED, 16, false), "ordinary empty DHD range");
        check(DestinyHardwiredDHD.canDiscover(SHIP_DISTANCE_SQUARED, DestinyHardwiredDHD.CONNECTION_RANGE, false),
                "hardwired ship range");
        check(DestinyHardwiredDHD.canDiscover(1024, 32, false), "range boundary");
        check(!DestinyHardwiredDHD.canDiscover(1025, 32, false), "outside hardwired range");
        check(!DestinyHardwiredDHD.canDiscover(SHIP_DISTANCE_SQUARED, 32, true), "occupied gate");

        Gate free = new Gate();
        Console console = new Console(free, SHIP_DISTANCE_SQUARED, 32);
        check(console.cache.get() == free && free.cache.getCached() == console,
                "native discovery must establish both cache directions");
        console.cache.clear();
        check(free.cache.getCached() == console && console.cache.getCached() == null,
                "native one-sided clear leaves a cached owner");
        console.cache.markDirtyTwoWays();
        check(console.cache.get() == null && free.cache.getCached() == console,
                "dirtying an unpaired receiver must not steal an occupied gate");
        check(DestinyHardwiredDHD.canReleaseOneSidedOwner(free.cache.getCached() == console,
                console.cache.getCached() == null, true, true), "same idle owner is repairable");
        free.cache.clearTwoWays();
        check(free.cache.getCached() == null && console.cache.getCached() == null,
                "native two-way release must not directly force a link");
        check(console.cache.get() == free && free.cache.getCached() == console,
                "native discovery must be able to reconnect after release");

        Gate occupied = new Gate();
        Console other = new Console(occupied, 25, 32);
        check(other.cache.get() == occupied, "competing owner setup");
        Console candidate = new Console(occupied, SHIP_DISTANCE_SQUARED, 32);
        check(candidate.cache.get() == null && occupied.cache.getCached() == other
                        && other.cache.getCached() == occupied,
                "candidate must not replace another owner");
        check(!DestinyHardwiredDHD.canReleaseOneSidedOwner(occupied.cache.getCached() == candidate,
                candidate.cache.getCached() == null, true, true), "competing owner must not be released");
        check(!DestinyHardwiredDHD.canReleaseOneSidedOwner(true, true, false, true),
                "out-of-range owner must not be released");
        check(!DestinyHardwiredDHD.canReleaseOneSidedOwner(true, true, true, false),
                "busy gate must not be released");
        check(!DestinyHardwiredDHD.canReleaseOneSidedOwner(true, false, true, true),
                "receiver with a gate must not be released");

        Gate distant = new Gate();
        Console tooFar = new Console(distant, 1025, 32);
        check(tooFar.cache.get() == null && distant.cache.getCached() == null,
                "out-of-range discovery must not link either cache");
        System.out.println("PASS: native reciprocal caches, hardwired range, and competing-owner safeguards");
    }

    private static final class Console implements AutoCache.IController<Console, Gate> {
        private final AutoCache.Receiver<Console, Gate> cache = new AutoCache.Receiver<>(this);

        private Console(Gate gate, double distanceSqr, double range) {
            cache.setFetch(() -> DestinyHardwiredDHD.canDiscover(distanceSqr, range, gate.cache.isCached())
                    ? gate : null);
            cache.setRevalidate(() -> distanceSqr <= range * range);
        }

        @Override public AutoCache.Receiver<Console, Gate> receiverCache() { return cache; }
    }

    private static final class Gate implements AutoCache.IReceiver<Console, Gate> {
        private final AutoCache.Controller<Console, Gate> cache = new AutoCache.Controller<>(this);
        @Override public AutoCache.Controller<Console, Gate> controllerCache() { return cache; }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
