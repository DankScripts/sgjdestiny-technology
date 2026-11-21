package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.entity.DestinyLiftEntity;
import com.dankscripts.sgjdestiny_dhd.network.DestinyLiftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DestinyDHD.MOD_ID, value = Dist.CLIENT)
public final class DestinyLiftInput {
    private DestinyLiftInput() {}
    @SubscribeEvent public static void interact(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || player == null
                || !player.getMainHandItem().isEmpty()) return;
        for (var cab : player.level().getEntitiesOfClass(DestinyLiftEntity.class, player.getBoundingBox().inflate(4))) {
            if (!cab.insideCab(player)) continue;
            var panel = new AABB(-2.24, cab.getY() + 1.75, -386.6, -2.1, cab.getY() + 2.7, -385.4);
            var eye = player.getEyePosition();
            if (panel.clip(eye, eye.add(player.getLookAngle().scale(4.5))).isEmpty()) continue;
            DestinyLiftNetwork.control(cab.getId(), false);
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
    }
}
