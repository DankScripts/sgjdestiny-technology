package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DestinyItemTabEvents {
    private DestinyItemTabEvents() {}

    @SubscribeEvent
    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(ResourceLocation.fromNamespaceAndPath("minecraft", "tools_and_utilities"))) {
            event.accept(ModItems.DESTINY_LIFT_REPAIR_KIT.get());
            event.accept(ModItems.EARTH_GATE_DESIGNATOR.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_HELMET.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_CHESTPLATE.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_LEGGINGS.get());
            event.accept(ModItems.DESTINY_ENVIRONMENTAL_BOOTS.get());
            event.accept(ModItems.DESTINY_OXYGEN_TANK.get());
        }
        if (event.getTabKey().location().equals(ResourceLocation.fromNamespaceAndPath("minecraft", "ingredients")))
            event.accept(ModItems.SQUIGGLER_VENOM_SAC.get());
    }
}
