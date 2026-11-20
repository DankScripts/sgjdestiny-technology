package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDHDConsoleBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDecorativeDHDConsoleBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDHDInteractionBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyFloorVentBlock;
import com.dankscripts.sgjdestiny_dhd.block.LitDestinyBearingBlock;
import com.dankscripts.sgjdestiny_dhd.block.LitDestinyFloorChevronBlock;
import com.dankscripts.sgjdestiny_dhd.block.KinoDispenserBlock;
import com.dankscripts.sgjdestiny_dhd.block.SquigglerNestBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyShuttleAirlockBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyShuttleCollisionBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyPilotSeatBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyShipAirlockBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDoorControllerBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyCurvedStairBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinySafetyRailingBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyCorridorBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyCorridorCollisionBlock;
import com.dankscripts.sgjdestiny_dhd.block.AlwaysLitNaquadahCopperLampBlock;
import com.dankscripts.sgjdestiny_dhd.block.DiagonalWallFillerBlock;
import com.dankscripts.sgjdestiny_dhd.block.CornerNaquadahCopperLampBlock;
import com.dankscripts.sgjdestiny_dhd.block.CeilingRedstoneWireBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyControlHubBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyControlHubPartBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyOxygenGeneratorBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyOxygenStorageTankBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyWaterHoldingTankBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DestinyDHD.MOD_ID);

    public static final RegistryObject<Block> DESTINY_LIFT_ANCHOR = BLOCKS.register("destiny_elevator_anchor",
            com.dankscripts.sgjdestiny_dhd.block.DestinyLiftAnchorBlock::new);
    public static final RegistryObject<Block> DESTINY_LIFT_SUPPORT = BLOCKS.register("destiny_elevator_support",
            com.dankscripts.sgjdestiny_dhd.block.DestinyLiftSupportBlock::new);
    public static final RegistryObject<Block> DESTINY_LIFT_DOOR = BLOCKS.register("destiny_elevator_door",
            com.dankscripts.sgjdestiny_dhd.block.DestinyLiftDoorBlock::new);

    public static final RegistryObject<Block> WEATHERED_WALL_FILLER = BLOCKS.register("weathered_smooth_naquadah_copper_wall_filler", DiagonalWallFillerBlock::new);
    public static final RegistryObject<Block> OXIDIZED_WALL_FILLER = BLOCKS.register("oxidized_smooth_naquadah_copper_wall_filler", DiagonalWallFillerBlock::new);
    public static final RegistryObject<Block> WEATHERED_DIAGONAL_LAMP_FILLER = BLOCKS.register("always_lit_diagonal_corner_filler_weathered_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> OXIDIZED_DIAGONAL_LAMP_FILLER = BLOCKS.register("always_lit_diagonal_corner_filler_oxidized_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> WEATHERED_45_LEFT_LAMP = BLOCKS.register("always_lit_45_left_weathered_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> WEATHERED_45_RIGHT_LAMP = BLOCKS.register("always_lit_45_right_weathered_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> OXIDIZED_45_LEFT_LAMP = BLOCKS.register("always_lit_45_left_oxidized_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> OXIDIZED_45_RIGHT_LAMP = BLOCKS.register("always_lit_45_right_oxidized_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);

    public static final RegistryObject<Block> DESTINY_DHD =
            BLOCKS.register("destiny_dhd", DestinyDHDConsoleBlock::new);

    public static final RegistryObject<Block> DESTINY_DHD_DECORATIVE =
            BLOCKS.register("destiny_dhd_decorative", DestinyDecorativeDHDConsoleBlock::new);

    public static final RegistryObject<Block> DESTINY_DHD_INTERACTION =
            BLOCKS.register("destiny_dhd_interaction", DestinyDHDInteractionBlock::new);

    public static final RegistryObject<Block> DESTINY_CONTROL_HUB =
            BLOCKS.register("destiny_control_hub", DestinyControlHubBlock::new);
    public static final RegistryObject<Block> DESTINY_CONTROL_HUB_PART =
            BLOCKS.register("destiny_control_hub_part", DestinyControlHubPartBlock::new);
    public static final RegistryObject<Block> DESTINY_OXYGEN_GENERATOR =
            BLOCKS.register("destiny_oxygen_generator", DestinyOxygenGeneratorBlock::new);
    public static final RegistryObject<Block> DESTINY_OXYGEN_STORAGE_TANK =
            BLOCKS.register("destiny_oxygen_storage_tank", DestinyOxygenStorageTankBlock::new);
    public static final RegistryObject<Block> DESTINY_WATER_HOLDING_TANK =
            BLOCKS.register("destiny_water_holding_tank", DestinyWaterHoldingTankBlock::new);

    public static final RegistryObject<Block> DESTINY_FLOOR_VENT =
            BLOCKS.register("destiny_floor_vent", DestinyFloorVentBlock::new);

    public static final RegistryObject<Block> KINO_DISPENSER =
            BLOCKS.register("kino_dispenser", KinoDispenserBlock::new);

    public static final RegistryObject<Block> DESTINY_SHUTTLE_AIRLOCK =
            BLOCKS.register("destiny_shuttle_airlock", DestinyShuttleAirlockBlock::new);

    public static final RegistryObject<Block> DESTINY_SHUTTLE_COLLISION =
            BLOCKS.register("destiny_shuttle_collision", DestinyShuttleCollisionBlock::new);

    public static final RegistryObject<Block> DESTINY_PILOT_SEAT =
            BLOCKS.register("destiny_pilot_seat", DestinyPilotSeatBlock::new);

    public static final RegistryObject<Block> DESTINY_SHIP_AIRLOCK =
            BLOCKS.register("destiny_ship_airlock", DestinyShipAirlockBlock::new);

    public static final RegistryObject<Block> DESTINY_DOOR_CONTROLLER =
            BLOCKS.register("destiny_door_controller", DestinyDoorControllerBlock::new);

    public static final RegistryObject<Block> DESTINY_CURVED_STAIR =
            BLOCKS.register("destiny_curved_stair", DestinyCurvedStairBlock::new);

    public static final RegistryObject<Block> DESTINY_SAFETY_RAILING =
            BLOCKS.register("destiny_safety_railing", DestinySafetyRailingBlock::new);

    public static final RegistryObject<Block> DESTINY_CORRIDOR =
            BLOCKS.register("destiny_corridor", DestinyCorridorBlock::new);

    public static final RegistryObject<Block> DESTINY_CORRIDOR_COLLISION =
            BLOCKS.register("destiny_corridor_collision", DestinyCorridorCollisionBlock::new);

    public static final RegistryObject<Block> ALWAYS_LIT_WEATHERED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("always_lit_weathered_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> ALWAYS_LIT_OXIDIZED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("always_lit_oxidized_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> ALWAYS_LIT_DIAGONAL_WEATHERED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("always_lit_diagonal_weathered_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> ALWAYS_LIT_DIAGONAL_OXIDIZED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("always_lit_diagonal_oxidized_naquadah_copper_lamp", AlwaysLitNaquadahCopperLampBlock::new);
    public static final RegistryObject<Block> CORNER_WEATHERED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("corner_weathered_naquadah_copper_lamp", () -> new CornerNaquadahCopperLampBlock(false));
    public static final RegistryObject<Block> CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("corner_oxidized_naquadah_copper_lamp", () -> new CornerNaquadahCopperLampBlock(false));
    public static final RegistryObject<Block> ALWAYS_LIT_CORNER_WEATHERED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("always_lit_corner_weathered_naquadah_copper_lamp", () -> new CornerNaquadahCopperLampBlock(true));
    public static final RegistryObject<Block> ALWAYS_LIT_CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP =
            BLOCKS.register("always_lit_corner_oxidized_naquadah_copper_lamp", () -> new CornerNaquadahCopperLampBlock(true));
    public static final RegistryObject<Block> CEILING_REDSTONE_WIRE =
            BLOCKS.register("ceiling_redstone_wire", CeilingRedstoneWireBlock::new);

    public static final RegistryObject<Block> LIT_DESTINY_FLOOR_CHEVRON =
            BLOCKS.register("lit_destiny_floor_chevron", LitDestinyFloorChevronBlock::new);
    public static final RegistryObject<Block> SQUIGGLER_NEST = BLOCKS.register("squiggler_nest", SquigglerNestBlock::new);

        public static final RegistryObject<Block> DESTINY_BEARING_FALLBACK =
            BLOCKS.register("destiny_bearing_fallback", LitDestinyBearingBlock::new);

    public static RegistryObject<Block> litDestinyBearing;

        public static void registerOptionalBlocks() {
                if (ModList.get().isLoaded("sgj_deco")) {
                        litDestinyBearing = BLOCKS.register("lit_destiny_bearing", LitDestinyBearingBlock::new);
                }
        }

    private ModBlocks() {}
}
