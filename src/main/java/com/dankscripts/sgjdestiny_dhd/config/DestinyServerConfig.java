package com.dankscripts.sgjdestiny_dhd.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DestinyServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_DEPLOYED_KINOS;
    public static final ModConfigSpec.BooleanValue INSTALL_CUSTOM_DESTINY_SHIP;
    public static final java.util.List<ModConfigSpec.ConfigValue<String>> LIFT_STOP_NAMES = new java.util.ArrayList<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("kino");
        MAX_DEPLOYED_KINOS = builder
                .comment("Maximum number of deployed Kinos allowed across the entire server.")
                .defineInRange("maxDeployedKinos", 16, 1, 24);
        builder.pop();

        builder.push("destinyShip");
        INSTALL_CUSTOM_DESTINY_SHIP = builder
                .comment(
                        "Install the mod's packaged Destiny ship around SGJourney's existing Destiny Stargate.",
                        "Disabled by default so existing worlds and custom-built Destiny ships are never replaced unexpectedly.",
                        "To opt in, launch the world once to generate this file, stop the game/server, then set this to true.",
                        "Leave this true afterward. The ship is installed only once per world; later starts do not replace it again.")
                .define("installCustomDestinyShip", false);
        builder.pop();
        builder.comment("Names displayed on Destiny's elevator destination panel. Heights are the existing ship landings.")
                .push("destinyElevator");
        for (int i = 0; i < com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftStops.HEIGHTS.length; i++) {
            int y = com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftStops.HEIGHTS[i];
            LIFT_STOP_NAMES.add(builder.define("floorNameY" + y,
                    com.dankscripts.sgjdestiny_dhd.elevator.DestinyLiftStops.DEFAULT_NAMES[i],
                    value -> value instanceof String name && !name.isBlank() && name.length() <= 48
                            && name.chars().noneMatch(Character::isISOControl)));
        }
        builder.pop();
        SPEC = builder.build();
    }

    private DestinyServerConfig() {}
}
