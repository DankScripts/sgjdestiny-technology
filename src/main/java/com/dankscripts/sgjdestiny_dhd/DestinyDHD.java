package com.dankscripts.sgjdestiny_dhd;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.povstalec.sgjourney.common.init.BlockEntityInit;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashSet;
import java.util.Set;

@Mod(DestinyDHD.MOD_ID)
public final class DestinyDHD {
    public static final String MOD_ID = "sgjdestiny_dhd";

    public DestinyDHD() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.registerOptionalBlocks();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreativeItems);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var universeDHDType = BlockEntityInit.UNIVERSE_DHD.get();
            var validBlocks = new HashSet<Block>(universeDHDType.validBlocks);
            validBlocks.add(ModBlocks.DESTINY_DHD.get());
            universeDHDType.validBlocks = Set.copyOf(validBlocks);
        });
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tab = event.getTabKey().location();
        if (tab.equals(new ResourceLocation("minecraft", "functional_blocks"))) {
            event.accept(ModItems.DESTINY_DHD.get());
        }
    }
}
