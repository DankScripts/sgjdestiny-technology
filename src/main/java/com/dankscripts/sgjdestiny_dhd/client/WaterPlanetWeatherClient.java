package com.dankscripts.sgjdestiny_dhd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ensures the custom planet's permanent storm is visible to each client. */
@Mod.EventBusSubscriber(modid = "sgjdestiny_dhd", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WaterPlanetWeatherClient {
    private static final ResourceLocation WATER_PLANET = new ResourceLocation("sgjourney", "water_planet");
    private WaterPlanetWeatherClient() {}

    @SubscribeEvent
    public static void keepSnowVisible(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.dimension().location().equals(WATER_PLANET)) {
            minecraft.level.setRainLevel(1.0F);
            minecraft.level.setThunderLevel(0.0F);
        }
    }
}
