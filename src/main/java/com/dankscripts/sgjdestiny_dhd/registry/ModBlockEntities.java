package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block_entity.KinoDispenserBlockEntity;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyShuttleAirlockBlockEntity;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyShipAirlockBlockEntity;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyCorridorBlockEntity;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyOxygenGeneratorBlockEntity;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyControlHubBlockEntity;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyWaterHoldingTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DestinyDHD.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KinoDispenserBlockEntity>> KINO_DISPENSER =
            BLOCK_ENTITIES.register("kino_dispenser", () ->
                    BlockEntityType.Builder.of(KinoDispenserBlockEntity::new,
                            ModBlocks.KINO_DISPENSER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestinyShuttleAirlockBlockEntity>> DESTINY_SHUTTLE_AIRLOCK =
            BLOCK_ENTITIES.register("destiny_shuttle_airlock", () ->
                    BlockEntityType.Builder.of(DestinyShuttleAirlockBlockEntity::new,
                            ModBlocks.DESTINY_SHUTTLE_AIRLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestinyShipAirlockBlockEntity>> DESTINY_SHIP_AIRLOCK =
            BLOCK_ENTITIES.register("destiny_ship_airlock", () ->
                    BlockEntityType.Builder.of(DestinyShipAirlockBlockEntity::new,
                            ModBlocks.DESTINY_SHIP_AIRLOCK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestinyCorridorBlockEntity>> DESTINY_CORRIDOR =
            BLOCK_ENTITIES.register("destiny_corridor", () ->
                    BlockEntityType.Builder.of(DestinyCorridorBlockEntity::new,
                            ModBlocks.DESTINY_CORRIDOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestinyOxygenGeneratorBlockEntity>> DESTINY_OXYGEN_GENERATOR =
            BLOCK_ENTITIES.register("destiny_oxygen_generator", () ->
                    BlockEntityType.Builder.of(DestinyOxygenGeneratorBlockEntity::new,
                            ModBlocks.DESTINY_OXYGEN_GENERATOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestinyControlHubBlockEntity>> DESTINY_CONTROL_HUB =
            BLOCK_ENTITIES.register("destiny_control_hub", () ->
                    BlockEntityType.Builder.of(DestinyControlHubBlockEntity::new,
                            ModBlocks.DESTINY_CONTROL_HUB.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DestinyWaterHoldingTankBlockEntity>> DESTINY_WATER_HOLDING_TANK =
            BLOCK_ENTITIES.register("destiny_water_holding_tank", () ->
                    BlockEntityType.Builder.of(DestinyWaterHoldingTankBlockEntity::new,
                            ModBlocks.DESTINY_WATER_HOLDING_TANK.get()).build(null));

    private ModBlockEntities() {}
}
