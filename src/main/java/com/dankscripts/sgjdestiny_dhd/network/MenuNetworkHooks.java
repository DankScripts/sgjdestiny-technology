package com.dankscripts.sgjdestiny_dhd.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

/** NeoForge 1.21 menu-opening adapter for the Forge 1.20 call sites. */
public final class MenuNetworkHooks {
    private MenuNetworkHooks() {}

    public static void openScreen(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        player.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
    }
}
