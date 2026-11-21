package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.MOD,
        value = net.neoforged.api.distmarker.Dist.CLIENT)
public final class ModEntityRenderers {
    private ModEntityRenderers() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SQUIGGLER.get(), SquigglerRenderer::new);
        event.registerEntityRenderer(ModEntities.PILOT_SEAT.get(), DestinyPilotSeatRenderer::new);
        event.registerEntityRenderer(ModEntities.DESTINY_LIFT.get(), DestinyLiftRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DESTINY_SHUTTLE_AIRLOCK.get(), DestinyShuttleAirlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DESTINY_SHIP_AIRLOCK.get(), DestinyShipAirlockRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DESTINY_CORRIDOR.get(), DestinyCorridorRenderer::new);
    }
}
