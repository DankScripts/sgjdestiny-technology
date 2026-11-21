package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block.CeilingRedstoneWireBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CeilingRedstoneColorEvents {
    private CeilingRedstoneColorEvents() {}

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) ->
                        RedStoneWireBlock.getColorForPower(state.getValue(CeilingRedstoneWireBlock.POWER)),
                ModBlocks.CEILING_REDSTONE_WIRE.get());
    }
}
