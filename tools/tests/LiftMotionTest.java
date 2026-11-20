package com.dankscripts.sgjdestiny_dhd.elevator;

/** Dependency-free test runner for the actual shared movement calculation, run by Gradle check. */
public final class LiftMotionTest {
    public static void main(String[] args) {
        int[] floors = {70, 75, 80, 85, 93, 99, 105, 111};
        int routes = 0;
        for (int from : floors) for (int to : floors) {
            int ticks = LiftMotion.duration(from, to);
            check(LiftMotion.height(from, to, -10, ticks) == from, "Departure delay changed position");
            check(LiftMotion.height(from, to, ticks + 10, ticks) == to, "Overshot destination");
            double prev = from;
            for (int quarter = 1; quarter <= ticks * 4; quarter++) {
                double now = LiftMotion.height(from, to, quarter / 4.0, ticks);
                check(now >= Math.min(from, to) && now <= Math.max(from, to), "Left route bounds");
                check(Math.abs(now - prev) <= .03751, "Motion exceeds 3 blocks/second");
                if (from != to) check((now - prev) * (to - from) > 0, "Paused or reversed between stops");
                prev = now;
            }
            check(Math.abs(LiftMotion.height(from, to, 1, ticks) - from) < .011, "Abrupt start");
            check(Math.abs(to - LiftMotion.height(from, to, ticks - 1, ticks)) < .011, "Abrupt stop");
            for (int elapsed = 0; elapsed < ticks; elapsed++) {
                double live = LiftMotion.height(from, to, elapsed + .5, ticks);
                double resumed = LiftMotion.height(from, to, (1000L + elapsed) - 1000L + .5, ticks);
                check(live == resumed, "Reload changed trajectory");
            }
            long serverStart = 9_000_000_000L;
            for (int at : new int[] {-10, -5, 0, ticks / 2, ticks - 1, ticks, ticks + 100}) {
                int snapshot = LiftMotion.elapsed(serverStart + at, serverStart, ticks);
                double observed = LiftMotion.height(from, to, snapshot, ticks);
                check(observed == LiftMotion.height(from, to, at, ticks), "Progress snapshot shifted the cab");
                check(snapshot >= -10 && snapshot <= ticks, "Invalid synchronized progress");
                if (at >= 0) check(LiftMotion.doorOpen(snapshot) == 0, "Doors opened while travelling");
                check(!LiftMotion.canOpenDoors(true, 100, observed, to, true), "Moving cab unlocked a landing");
            }
            for (int stopped = 0; stopped < LiftMotion.DOCKING_TICKS; stopped++)
                check(!LiftMotion.canOpenDoors(false, stopped, to, to, true), "Doors opened before docking completed");
            check(LiftMotion.canOpenDoors(false, LiftMotion.DOCKING_TICKS, to, to, true), "Docked cab failed to unlock");
            check(!LiftMotion.canOpenDoors(false, 100, to - .01, to, true), "Cab below landing unlocked doors");
            check(!LiftMotion.canOpenDoors(false, 100, to + .01, to, true), "Cab above landing unlocked doors");
            check(!LiftMotion.canOpenDoors(false, 100, to, to, false), "Missing platform unlocked doors");
            routes++;
        }
        check(LiftMotion.doorOpen(-10) == 1, "Doors not open at start of boarding close");
        int boarding = LiftMotion.BOARDING_TICKS;
        for (int i = 0; i < 159; i++) {
            boarding = LiftMotion.nextBoardingTicks(boarding, false);
            check(boarding > 0, "Boarding closed before eight seconds");
        }
        check(LiftMotion.nextBoardingTicks(boarding, false) == 0, "Doors did not close after eight seconds");
        check(LiftMotion.nextBoardingTicks(1, true) == 160, "Occupied doorway did not extend boarding");
        check(LiftMotion.nextBoardingTicks(0, true) == 0, "Parked doors opened without a call");
        check(LiftMotion.doorOpen(-5) == .5F, "Door animation not using travel progress");
        for (double stopped : new double[] {70.125, 82.75, 104.5}) for (int landing : floors) {
            int duration = LiftMotion.duration(stopped, landing);
            check(LiftMotion.height(stopped, landing, -10, duration) == stopped, "Emergency restart snapped to old floor");
            check(LiftMotion.height(stopped, landing, duration, duration) == landing, "Emergency restart missed landing");
            check(!LiftMotion.canOpenDoors(false, 100, stopped, landing, true), "Normal interlock opened between landings");
        }
        System.out.println("PASS: " + routes + " routes, fractional-tick continuity, speed limit, easing, resumption, late tracking, synchronized door progress and docking interlocks.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
