package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;

/** Give every mod item a shared creative-search keyword without renaming it. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DestinyItemSearchTooltipEvents {
    private DestinyItemSearchTooltipEvents() {}

    @SubscribeEvent
    public static void addKeyword(ItemTooltipEvent event) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (id != null && DestinyDHD.MOD_ID.equals(id.getNamespace()))
            event.getToolTip().add(Component.literal("Destiny Technology").withStyle(ChatFormatting.DARK_GRAY));
    }
}
