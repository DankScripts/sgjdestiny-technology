package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DestinyDHD.MOD_ID);

    public static final RegistryObject<Item> DESTINY_DHD = ITEMS.register("destiny_dhd",
            () -> new BlockItem(ModBlocks.DESTINY_DHD.get(), new Item.Properties()));

    private ModItems() {}
}
