package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Top-level subscriber ensures the scrubber screen is discovered during client setup. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DestinyAirScrubberScreenRegistration {
    private DestinyAirScrubberScreenRegistration() {}

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.DESTINY_AIR_SCRUBBER.get(), DestinyAirScrubberScreen::new);
    }
}
