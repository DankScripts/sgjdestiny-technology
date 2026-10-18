package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ordinary dust also places our ceiling wire when used against a block's underside. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CeilingRedstonePlacementEvents {
    private CeilingRedstonePlacementEvents() {}

    @SubscribeEvent
    public static void placeCeilingDust(PlayerInteractEvent.RightClickBlock event) {
        if (event.getFace() != Direction.DOWN || !event.getItemStack().is(Items.REDSTONE)) {
            return;
        }
        BlockItem ceilingWire = (BlockItem) ModItems.CEILING_REDSTONE_WIRE.get();
        InteractionResult result = ceilingWire.useOn(new UseOnContext(event.getEntity(), event.getHand(), event.getHitVec()));
        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
