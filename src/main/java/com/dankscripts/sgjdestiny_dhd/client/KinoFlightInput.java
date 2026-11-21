package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.network.DestinyDialerNetwork;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

/** Steers a linked Kino whenever its handheld is actively held. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class KinoFlightInput {
    private static int inputTick;

    private KinoFlightInput() {}

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        if (++inputTick % 2 != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        if (!minecraft.player.getMainHandItem().is(ModItems.KINO_REMOTE.get())
                && !minecraft.player.getOffhandItem().is(ModItems.KINO_REMOTE.get())) return;

        long window = minecraft.getWindow().getWindow();
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP)) DestinyDialerNetwork.kinoCommand(3);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN)) DestinyDialerNetwork.kinoCommand(4);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT)) DestinyDialerNetwork.kinoCommand(5);
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT)) DestinyDialerNetwork.kinoCommand(6);
    }
}
