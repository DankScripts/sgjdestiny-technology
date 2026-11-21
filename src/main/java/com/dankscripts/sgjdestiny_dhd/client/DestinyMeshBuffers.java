package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.LinkedHashMap;
import java.util.Map;

/** One reusable native staging buffer, rather than a native allocation per mesh bake. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, value = Dist.CLIENT)
public final class DestinyMeshBuffers {
    private static ByteBufferBuilder staging;
    private static final Map<Class<?>, Runnable> CLEANUP = new LinkedHashMap<>();
    private DestinyMeshBuffers() {}

    public static BufferBuilder staging(RenderType output) {
        RenderSystem.assertOnRenderThread();
        if (staging == null) staging = new ByteBufferBuilder(256 * 1024);
        staging.clear();
        return new BufferBuilder(staging, output.mode(), output.format());
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

    @EventBusSubscriber(modid = DestinyDHD.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent public static void register(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager -> clearCaches());
        }
    }
}
