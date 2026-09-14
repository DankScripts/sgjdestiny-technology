package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.network.DestinyDialerNetwork.ClientEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Keeps every client-only screen reference out of the common network class. */
public final class DestinyDialerClientPackets {
    private DestinyDialerClientPackets() {}

    public static void openDatabase(BlockPos console, boolean directGate, boolean handheld,
                                    List<ClientEntry> entries, int gateState, boolean kinoInitially,
                                    boolean kinoDeployed) {
        Minecraft.getInstance().setScreen(new SeedShipDialerScreen(console, directGate, handheld,
                entries, gateState, kinoInitially, kinoDeployed));
    }

    public static void updateKinoStatus(boolean deployed) {
        if (Minecraft.getInstance().screen instanceof SeedShipDialerScreen screen) {
            screen.setKinoDeployed(deployed);
        }
    }

    public static void updateKinoVideo(String dimension, double x, double y, double z,
                                       float yaw, float pitch) {
        if (Minecraft.getInstance().screen instanceof SeedShipDialerScreen screen) {
            screen.setKinoCamera(dimension, x, y, z, yaw, pitch);
        }
    }

}
