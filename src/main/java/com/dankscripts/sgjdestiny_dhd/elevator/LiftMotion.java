package com.dankscripts.sgjdestiny_dhd.elevator;

/** Shared server/client trajectory. One continuous trip, with no floor-by-floor pauses. */
public final class LiftMotion {
    public static final int DOCKING_TICKS = 10;
    public static final int BOARDING_TICKS = 160;
    public static int nextBoardingTicks(int remaining, boolean doorwayOccupied) {
        if (remaining <= 0) return 0;
        return doorwayOccupied ? BOARDING_TICKS : remaining - 1;
    }
    private LiftMotion() {}

    public static int duration(double from, double to) {
        return Math.max(40, (int) Math.ceil(Math.abs(to - from) * 10.0));
    }

    /** Evaluated on the server only, then sent with the cab's synchronized data. */
    public static int elapsed(long serverTime, long start, int duration) {
        return (int) Math.max(-10L, Math.min(Math.max(0, duration), serverTime - start));
    }

    public static float doorOpen(double elapsed) {
        return (float) Math.max(0, Math.min(1, -elapsed / 10.0));
    }

    public static boolean canOpenDoors(boolean moving, int stoppedTicks, double feet, int landing, boolean supported) {
        return !moving && stoppedTicks >= DOCKING_TICKS && supported && Math.abs(feet - landing) < 0.000001;
    }

    public static double height(double from, double to, double elapsed, int duration) {
        double t = Math.max(0.0, Math.min(1.0, elapsed / Math.max(1, duration)));
        return from + (to - from) * t * t * (3.0 - 2.0 * t);
    }
}
