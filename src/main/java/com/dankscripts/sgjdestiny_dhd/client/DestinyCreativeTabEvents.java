package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Keeps the vanilla entries and provides one tab for all Destiny items. */
@EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DestinyCreativeTabEvents {
    private DestinyCreativeTabEvents() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(
                ResourceLocation.fromNamespaceAndPath(DestinyDHD.MOD_ID, "destiny_technology"),
                CreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.sgjdestiny_dhd"))
                        .icon(() -> new ItemStack(ModItems.DESTINY_DHD.get()))
                        .displayItems((parameters, output) -> BuiltInRegistries.ITEM.entrySet().stream()
                                .filter(entry -> entry.getKey().location().getNamespace().equals(DestinyDHD.MOD_ID))
                                .map(java.util.Map.Entry::getValue)
                                .filter(item -> item != Items.AIR)
                                .forEach(output::accept))
                        .build()));
    }

    @SubscribeEvent
    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(ResourceLocation.fromNamespaceAndPath("minecraft", "functional_blocks"))) {
            event.accept(ModItems.DESTINY_FLOOR_VENT.get());
        }
    }
}
