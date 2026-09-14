package com.dankscripts.sgjdestiny_dhd.compat;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the Kino's armor-stand carrier from equipping the handheld. */
@Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KinoRemoteUseEvents {
    private KinoRemoteUseEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickKino(PlayerInteractEvent.EntityInteract event) {
        claimKinoClick(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickKinoAt(PlayerInteractEvent.EntityInteractSpecific event) {
        claimKinoClick(event, event.getTarget());
    }

    private static void claimKinoClick(PlayerInteractEvent event, Entity target) {
        if (!event.getItemStack().is(ModItems.KINO_REMOTE.get())
                || !target.getTags().contains("sgjdestiny_kino")) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            KinoRemoteItem remote = (KinoRemoteItem) ModItems.KINO_REMOTE.get();
            if (!player.getCooldowns().isOnCooldown(remote)) {
                remote.use(event.getLevel(), player, event.getHand());
            }
        }
    }
}
