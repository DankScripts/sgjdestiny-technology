package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;

@EventBusSubscriber(modid = DestinyDHD.MOD_ID, value = Dist.CLIENT)
public final class KinoVideoRenderEvents {
    private KinoVideoRenderEvents() {}

    @SubscribeEvent
    public static void beforeLivingRender(RenderLivingEvent.Pre<?, ?> event) {
        if (!WorldRenderInfo.isRendering() || !(event.getEntity() instanceof ArmorStand)) return;
        if (Minecraft.getInstance().screen instanceof SeedShipDialerScreen screen
                && screen.shouldHideKinoFromVideo(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
