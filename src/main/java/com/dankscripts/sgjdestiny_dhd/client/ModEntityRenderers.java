package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = net.minecraftforge.api.distmarker.Dist.CLIENT)
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
