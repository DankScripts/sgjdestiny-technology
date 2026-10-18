package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.LinkedHashMap;
import java.util.Map;

/** One reusable native staging buffer, rather than a native allocation per mesh bake. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, value = Dist.CLIENT)
public final class DestinyMeshBuffers {
    private static BufferBuilder staging;
    private static final Map<Class<?>, Runnable> CLEANUP = new LinkedHashMap<>();
    private DestinyMeshBuffers() {}

    public static BufferBuilder staging() {
        RenderSystem.assertOnRenderThread();
        if (staging == null) staging = new BufferBuilder(256 * 1024);
        // Upload releases the RenderedBuffer; clear resets offsets for the next bake.
        staging.clear();
        return staging;
    }

    public static void register(Class<?> type, Runnable cleanup) {
        Runnable old = CLEANUP.put(type, cleanup);
        if (old != null) onRenderThread(old);
    }

    private static void onRenderThread(Runnable action) {
        if (RenderSystem.isOnRenderThread()) action.run();
        else RenderSystem.recordRenderCall(action::run);
    }

    public static void clearCaches() {
        onRenderThread(() -> CLEANUP.values().forEach(Runnable::run));
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clearCaches(); }

    @Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> clearCaches());
        }
    }
}
