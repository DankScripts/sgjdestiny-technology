package com.dankscripts.sgjdestiny_dhd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ShuttleTravelClient {
    private ShuttleTravelClient() {}

    public static void open(BlockPos anchor, String destination) {
        Minecraft.getInstance().setScreen(new ShuttleTravelScreen(anchor, destination));
    }

    public static void status(boolean launched, String message) {
        if (message.equals("ARRIVED")) {
            if (Minecraft.getInstance().screen instanceof ShuttleTravelScreen)
                Minecraft.getInstance().setScreen(null);
            return;
        }
        if (Minecraft.getInstance().screen instanceof ShuttleTravelScreen screen)
            screen.setStatus(launched, message);
    }
}
