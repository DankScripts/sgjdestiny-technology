package com.dankscripts.sgjdestiny_dhd;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import com.dankscripts.sgjdestiny_dhd.registry.ModMenus;
import com.dankscripts.sgjdestiny_dhd.registry.ModParticles;
import com.dankscripts.sgjdestiny_dhd.registry.ModSounds;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModFeatures;
import com.dankscripts.sgjdestiny_dhd.config.DestinyServerConfig;
import com.dankscripts.sgjdestiny_dhd.network.ShuttleTravelNetwork;
import com.dankscripts.sgjdestiny_dhd.mixin.BlockEntityTypeAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.povstalec.sgjourney.common.init.BlockEntityInit;
import net.povstalec.sgjourney.api.dhd.DHD;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.HashSet;
import java.util.Set;

@Mod(DestinyDHD.MOD_ID)
public final class DestinyDHD {
    public static final String MOD_ID = "sgjdestiny_dhd";

    public DestinyDHD(IEventBus modBus, ModContainer modContainer) {
        ShuttleTravelNetwork.register();
        com.dankscripts.sgjdestiny_dhd.network.DestinyLiftNetwork.register();
        modContainer.registerConfig(ModConfig.Type.SERVER, DestinyServerConfig.SPEC);
        ModBlocks.registerOptionalBlocks();
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModFeatures.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreativeItems);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::registerCapabilities);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        ShuttleTravelNetwork.registerPayloads(event);
        com.dankscripts.sgjdestiny_dhd.network.DestinyLiftNetwork.registerPayloads(event);
        com.dankscripts.sgjdestiny_dhd.network.DestinyDialerNetwork.registerPayloads(event);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlock(DHD.DHD_CAPABILITY_BLOCK,
                (level, pos, state, blockEntity, side) ->
                        blockEntity instanceof UniverseDHDEntity dhd ? dhd.getDHD() : null,
                ModBlocks.DESTINY_DHD.get());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, blockEntity, side) ->
                        blockEntity instanceof UniverseDHDEntity dhd ? dhd.getEnergyHandler(side) : null,
                ModBlocks.DESTINY_DHD.get());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.DESTINY_WATER_HOLDING_TANK.get(),
                (tank, side) -> tank.fluidHandler());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var universeDHDType = BlockEntityInit.UNIVERSE_DHD.get();
            BlockEntityTypeAccessor blockEntityType =
                    (BlockEntityTypeAccessor) (Object) universeDHDType;
            var validBlocks = new HashSet<Block>(blockEntityType.sgjdestiny$getValidBlocks());
            validBlocks.add(ModBlocks.DESTINY_DHD.get());
            blockEntityType.sgjdestiny$setValidBlocks(Set.copyOf(validBlocks));
        });
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tab = event.getTabKey().location();
        if (tab.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "building_blocks"))) {
            event.accept(ModItems.WEATHERED_WALL_FILLER.get());
            event.accept(ModItems.OXIDIZED_WALL_FILLER.get());
        }
        if (tab.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "functional_blocks"))) {
            event.accept(ModItems.DESTINY_DHD.get());
            event.accept(ModItems.DESTINY_DHD_DECORATIVE.get());
            event.accept(ModItems.DESTINY_CONTROL_HUB.get());
            event.accept(ModItems.DESTINY_OXYGEN_GENERATOR.get());
            event.accept(ModItems.DESTINY_OXYGEN_STORAGE_TANK.get());
            event.accept(ModItems.DESTINY_WATER_HOLDING_TANK.get());
            event.accept(ModItems.KINO_REMOTE.get());
            event.accept(ModItems.KINO.get());
            event.accept(ModItems.KINO_DISPENSER.get());
            event.accept(ModItems.DESTINY_SHUTTLE_AIRLOCK.get());
            event.accept(ModItems.DESTINY_SHIP_AIRLOCK.get());
            event.accept(ModItems.DESTINY_DOOR_CONTROLLER.get());
            event.accept(ModItems.DESTINY_CURVED_STAIR_CLOCKWISE.get());
            event.accept(ModItems.DESTINY_CURVED_STAIR_COUNTERCLOCKWISE.get());
            event.accept(ModItems.DESTINY_SAFETY_RAILING.get());
            event.accept(ModItems.DESTINY_CORRIDOR.get());
            event.accept(ModItems.ALWAYS_LIT_WEATHERED_NAQUADAH_COPPER_LAMP.get());
            event.accept(ModItems.ALWAYS_LIT_OXIDIZED_NAQUADAH_COPPER_LAMP.get());
            event.accept(ModItems.ALWAYS_LIT_DIAGONAL_WEATHERED_NAQUADAH_COPPER_LAMP.get());
            event.accept(ModItems.ALWAYS_LIT_DIAGONAL_OXIDIZED_NAQUADAH_COPPER_LAMP.get());
            event.accept(ModItems.WEATHERED_DIAGONAL_LAMP_FILLER.get());
            event.accept(ModItems.OXIDIZED_DIAGONAL_LAMP_FILLER.get());
            event.accept(ModItems.WEATHERED_45_LEFT_LAMP.get());
            event.accept(ModItems.WEATHERED_45_RIGHT_LAMP.get());
            event.accept(ModItems.OXIDIZED_45_LEFT_LAMP.get());
            event.accept(ModItems.OXIDIZED_45_RIGHT_LAMP.get());
            event.accept(ModItems.ALWAYS_LIT_CORNER_WEATHERED_NAQUADAH_COPPER_LAMP.get());
            event.accept(ModItems.ALWAYS_LIT_CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP.get());
        }
        if (tab.equals(ResourceLocation.fromNamespaceAndPath("minecraft", "redstone_blocks"))) {
            event.accept(ModItems.CEILING_REDSTONE_WIRE.get());
            event.accept(ModItems.CORNER_WEATHERED_NAQUADAH_COPPER_LAMP.get());
            event.accept(ModItems.CORNER_OXIDIZED_NAQUADAH_COPPER_LAMP.get());
        }
    }
}
