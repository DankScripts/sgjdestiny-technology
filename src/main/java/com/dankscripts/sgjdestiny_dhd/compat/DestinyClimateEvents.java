package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the interior-space Destiny dimension permanently free of weather. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DestinyClimateEvents {
    private static final ResourceLocation DESTINY = new ResourceLocation("sgjourney", "destiny");

    private DestinyClimateEvents() {}

    @SubscribeEvent
    public static void keepDestinyClear(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (!level.dimension().location().equals(DESTINY) || level.getGameTime() % 20L != 0L) return;
        level.setWeatherParameters(24_000, 0, false, false);
    }
}
