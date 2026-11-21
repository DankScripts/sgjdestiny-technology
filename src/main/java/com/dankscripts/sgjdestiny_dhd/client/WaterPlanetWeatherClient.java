package com.dankscripts.sgjdestiny_dhd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Ensures the custom planet's permanent storm is visible to each client. */
@EventBusSubscriber(modid = "sgjdestiny_dhd", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WaterPlanetWeatherClient {
    private static final ResourceLocation WATER_PLANET = ResourceLocation.fromNamespaceAndPath("sgjourney", "water_planet");
    private WaterPlanetWeatherClient() {}

    @SubscribeEvent
    public static void keepSnowVisible(ClientTickEvent.Post event) {
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.dimension().location().equals(WATER_PLANET)) {
            minecraft.level.setRainLevel(1.0F);
            minecraft.level.setThunderLevel(0.0F);
        }
    }
}
