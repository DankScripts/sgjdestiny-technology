package com.dankscripts.sgjdestiny_dhd.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class DestinyServerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_DEPLOYED_KINOS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("kino");
        MAX_DEPLOYED_KINOS = builder
                .comment("Maximum number of deployed Kinos allowed across the entire server.")
                .defineInRange("maxDeployedKinos", 16, 1, 24);
        builder.pop();
        SPEC = builder.build();
    }

    private DestinyServerConfig() {}
}
