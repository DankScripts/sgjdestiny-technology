package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.item.EarthGateDesignatorItem;
import com.dankscripts.sgjdestiny_dhd.item.DestinyArmorMaterial;
import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import com.dankscripts.sgjdestiny_dhd.item.KinoItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DestinyDHD.MOD_ID);

    public static final RegistryObject<Item> DESTINY_DHD = ITEMS.register("destiny_dhd",
            () -> new BlockItem(ModBlocks.DESTINY_DHD.get(), new Item.Properties()));

    public static final RegistryObject<Item> DESTINY_FLOOR_VENT = ITEMS.register("destiny_floor_vent",
            () -> new BlockItem(ModBlocks.DESTINY_FLOOR_VENT.get(), new Item.Properties()));

    public static final RegistryObject<Item> EARTH_GATE_DESIGNATOR = ITEMS.register("earth_gate_designator",
            () -> new EarthGateDesignatorItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DESTINY_ENVIRONMENTAL_HELMET = ITEMS.register("destiny_environmental_helmet",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> DESTINY_ENVIRONMENTAL_CHESTPLATE = ITEMS.register("destiny_environmental_chestplate",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> DESTINY_ENVIRONMENTAL_LEGGINGS = ITEMS.register("destiny_environmental_leggings",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> DESTINY_ENVIRONMENTAL_BOOTS = ITEMS.register("destiny_environmental_boots",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> DESTINY_OXYGEN_TANK = ITEMS.register("destiny_oxygen_tank",
            () -> new Item(new Item.Properties().stacksTo(1).durability(3600)));
    public static final RegistryObject<Item> KINO_REMOTE = ITEMS.register("kino_remote",
            () -> new KinoRemoteItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> KINO = ITEMS.register("kino",
            () -> new KinoItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SQUIGGLER_VENOM_SAC = ITEMS.register("squiggler_venom_sac",
            () -> new Item(new Item.Properties()));

    private ModItems() {}
}
