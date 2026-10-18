package com.dankscripts.sgjdestiny_dhd.elevator;

import com.dankscripts.sgjdestiny_dhd.config.DestinyServerConfig;

public final class DestinyLiftStops {
    public static final int[] HEIGHTS = {70, 75, 80, 85, 93, 99, 105, 111};
    public static final String[] DEFAULT_NAMES = {"Lower landing", "Gate Room", "Landing Y80",
            "Landing Y85", "Landing Y93", "Landing Y99", "Command Crew Quarters", "Bridge"};
    private DestinyLiftStops() {}

    public static int index(int y) {
        for (int i = 0; i < HEIGHTS.length; i++) if (HEIGHTS[i] == y) return i;
        return -1;
    }

    public static String[] names() {
        String[] names = new String[HEIGHTS.length];
        for (int i = 0; i < names.length; i++) names[i] = DestinyServerConfig.LIFT_STOP_NAMES.get(i).get();
        return names;
    }
}
