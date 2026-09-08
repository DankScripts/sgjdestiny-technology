package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DestinyItemTabEvents {
    private DestinyItemTabEvents() {}

    @SubscribeEvent
    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(new ResourceLocation("minecraft", "tools_and_utilities"))) {
            event.accept(ModItems.EARTH_GATE_DESIGNATOR.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_HELMET.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_CHESTPLATE.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_LEGGINGS.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_BOOTS.get());
            event.accept(ModItems.DESTINY_OXYGEN_TANK.get());
        }
    }
}
