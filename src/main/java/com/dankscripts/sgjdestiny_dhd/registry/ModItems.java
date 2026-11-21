package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.item.EarthGateDesignatorItem;
import com.dankscripts.sgjdestiny_dhd.item.DestinyArmorMaterial;
import com.dankscripts.sgjdestiny_dhd.item.KinoRemoteItem;
import com.dankscripts.sgjdestiny_dhd.item.KinoItem;
import com.dankscripts.sgjdestiny_dhd.item.DestinySafetyRailingItem;
import com.dankscripts.sgjdestiny_dhd.item.DestinyCurvedStairCounterclockwiseItem;
import com.dankscripts.sgjdestiny_dhd.item.DestinyCorridorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DestinyDHD.MOD_ID);
    public static final DeferredHolder<Item, Item> DESTINY_LIFT_REPAIR_KIT = ITEMS.register("destiny_elevator_repair_kit",
            com.dankscripts.sgjdestiny_dhd.item.DestinyLiftRepairKitItem::new);

    public static final DeferredHolder<Item, Item> WEATHERED_WALL_FILLER = ITEMS.register("weathered_smooth_naquadah_copper_wall_filler", () -> new BlockItem(ModBlocks.WEATHERED_WALL_FILLER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> OXIDIZED_WALL_FILLER = ITEMS.register("oxidized_smooth_naquadah_copper_wall_filler", () -> new BlockItem(ModBlocks.OXIDIZED_WALL_FILLER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WEATHERED_DIAGONAL_LAMP_FILLER = ITEMS.register("always_lit_diagonal_corner_filler_weathered_naquadah_copper_lamp", () -> new BlockItem(ModBlocks.WEATHERED_DIAGONAL_LAMP_FILLER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> OXIDIZED_DIAGONAL_LAMP_FILLER = ITEMS.register("always_lit_diagonal_corner_filler_oxidized_naquadah_copper_lamp", () -> new BlockItem(ModBlocks.OXIDIZED_DIAGONAL_LAMP_FILLER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WEATHERED_45_LEFT_LAMP = ITEMS.register("always_lit_45_left_weathered_naquadah_copper_lamp", () -> new BlockItem(ModBlocks.WEATHERED_45_LEFT_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WEATHERED_45_RIGHT_LAMP = ITEMS.register("always_lit_45_right_weathered_naquadah_copper_lamp", () -> new BlockItem(ModBlocks.WEATHERED_45_RIGHT_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> OXIDIZED_45_LEFT_LAMP = ITEMS.register("always_lit_45_left_oxidized_naquadah_copper_lamp", () -> new BlockItem(ModBlocks.OXIDIZED_45_LEFT_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> OXIDIZED_45_RIGHT_LAMP = ITEMS.register("always_lit_45_right_oxidized_naquadah_copper_lamp", () -> new BlockItem(ModBlocks.OXIDIZED_45_RIGHT_LAMP.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DESTINY_DHD = ITEMS.register("destiny_dhd",
            () -> new BlockItem(ModBlocks.DESTINY_DHD.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DESTINY_DHD_DECORATIVE = ITEMS.register("destiny_dhd_decorative",
            () -> new BlockItem(ModBlocks.DESTINY_DHD_DECORATIVE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DESTINY_CONTROL_HUB = ITEMS.register("destiny_control_hub",
            () -> new BlockItem(ModBlocks.DESTINY_CONTROL_HUB.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> DESTINY_OXYGEN_GENERATOR = ITEMS.register("destiny_oxygen_generator",
            () -> new BlockItem(ModBlocks.DESTINY_OXYGEN_GENERATOR.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> DESTINY_OXYGEN_STORAGE_TANK = ITEMS.register("destiny_oxygen_storage_tank",
            () -> new BlockItem(ModBlocks.DESTINY_OXYGEN_STORAGE_TANK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> DESTINY_WATER_HOLDING_TANK = ITEMS.register("destiny_water_holding_tank",
            () -> new BlockItem(ModBlocks.DESTINY_WATER_HOLDING_TANK.get(), new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> DESTINY_FLOOR_VENT = ITEMS.register("destiny_floor_vent",
            () -> new BlockItem(ModBlocks.DESTINY_FLOOR_VENT.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> KINO_DISPENSER = ITEMS.register("kino_dispenser",
            () -> new BlockItem(ModBlocks.KINO_DISPENSER.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DESTINY_SHUTTLE_AIRLOCK = ITEMS.register("destiny_shuttle_airlock",
            () -> new BlockItem(ModBlocks.DESTINY_SHUTTLE_AIRLOCK.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> DESTINY_SHIP_AIRLOCK = ITEMS.register("destiny_ship_airlock",
            () -> new BlockItem(ModBlocks.DESTINY_SHIP_AIRLOCK.get(), new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> DESTINY_DOOR_CONTROLLER = ITEMS.register("destiny_door_controller",
            () -> new BlockItem(ModBlocks.DESTINY_DOOR_CONTROLLER.get(), new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<Item, Item> DESTINY_CURVED_STAIR_CLOCKWISE = ITEMS.register("destiny_curved_stair",
            () -> new BlockItem(ModBlocks.DESTINY_CURVED_STAIR.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DESTINY_CURVED_STAIR_COUNTERCLOCKWISE = ITEMS.register("destiny_curved_stair_counterclockwise",
            () -> new DestinyCurvedStairCounterclockwiseItem(ModBlocks.DESTINY_CURVED_STAIR.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DESTINY_SAFETY_RAILING = ITEMS.register("destiny_safety_railing",
            () -> new DestinySafetyRailingItem(ModBlocks.DESTINY_SAFETY_RAILING.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> DESTINY_CORRIDOR = ITEMS.register("destiny_corridor",
            () -> new DestinyCorridorItem(ModBlocks.DESTINY_CORRIDOR.get(), new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> ALWAYS_LIT_WEATHERED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("always_lit_weathered_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.ALWAYS_LIT_WEATHERED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ALWAYS_LIT_OXIDIZED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("always_lit_oxidized_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.ALWAYS_LIT_OXIDIZED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ALWAYS_LIT_DIAGONAL_WEATHERED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("always_lit_diagonal_weathered_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.ALWAYS_LIT_DIAGONAL_WEATHERED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ALWAYS_LIT_DIAGONAL_OXIDIZED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("always_lit_diagonal_oxidized_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.ALWAYS_LIT_DIAGONAL_OXIDIZED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CORNER_WEATHERED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("corner_weathered_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.CORNER_WEATHERED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("corner_oxidized_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ALWAYS_LIT_CORNER_WEATHERED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("always_lit_corner_weathered_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.ALWAYS_LIT_CORNER_WEATHERED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> ALWAYS_LIT_CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP =
            ITEMS.register("always_lit_corner_oxidized_naquadah_copper_lamp",
                    () -> new BlockItem(ModBlocks.ALWAYS_LIT_CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CEILING_REDSTONE_WIRE = ITEMS.register("ceiling_redstone_wire",
            () -> new BlockItem(ModBlocks.CEILING_REDSTONE_WIRE.get(), new Item.Properties()));

    public static final DeferredHolder<Item, Item> EARTH_GATE_DESIGNATOR = ITEMS.register("earth_gate_designator",
            () -> new EarthGateDesignatorItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, Item> DESTINY_ENVIRONMENTAL_HELMET = ITEMS.register("destiny_environmental_helmet",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final DeferredHolder<Item, Item> DESTINY_ENVIRONMENTAL_CHESTPLATE = ITEMS.register("destiny_environmental_chestplate",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final DeferredHolder<Item, Item> DESTINY_ENVIRONMENTAL_LEGGINGS = ITEMS.register("destiny_environmental_leggings",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final DeferredHolder<Item, Item> DESTINY_ENVIRONMENTAL_BOOTS = ITEMS.register("destiny_environmental_boots",
            () -> new ArmorItem(DestinyArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final DeferredHolder<Item, Item> DESTINY_OXYGEN_TANK = ITEMS.register("destiny_oxygen_tank",
            () -> new Item(new Item.Properties().stacksTo(1).durability(3600)));
    public static final DeferredHolder<Item, Item> KINO_REMOTE = ITEMS.register("kino_remote",
            () -> new KinoRemoteItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> KINO = ITEMS.register("kino",
            () -> new KinoItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> SQUIGGLER_VENOM_SAC = ITEMS.register("squiggler_venom_sac",
            () -> new Item(new Item.Properties()));

    private ModItems() {}
}
