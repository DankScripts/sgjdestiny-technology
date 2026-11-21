package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Adds the vent item without changing the accepted Destiny DHD initializer. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DestinyCreativeTabEvents {
    private DestinyCreativeTabEvents() {}

    @SubscribeEvent
    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(ResourceLocation.fromNamespaceAndPath("minecraft", "functional_blocks"))) {
            event.accept(ModItems.DESTINY_FLOOR_VENT.get());
        }
    }
}
