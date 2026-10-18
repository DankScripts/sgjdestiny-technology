package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block.CeilingRedstoneWireBlock;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CeilingRedstoneColorEvents {
    private CeilingRedstoneColorEvents() {}

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) ->
                        RedStoneWireBlock.getColorForPower(state.getValue(CeilingRedstoneWireBlock.POWER)),
                ModBlocks.CEILING_REDSTONE_WIRE.get());
    }
}
