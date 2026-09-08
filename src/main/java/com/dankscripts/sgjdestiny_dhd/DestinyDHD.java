package com.dankscripts.sgjdestiny_dhd;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import com.dankscripts.sgjdestiny_dhd.registry.ModParticles;
import com.dankscripts.sgjdestiny_dhd.registry.ModSounds;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import com.dankscripts.sgjdestiny_dhd.config.DestinyServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.povstalec.sgjourney.common.init.BlockEntityInit;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashSet;
import java.util.Set;

@Mod(DestinyDHD.MOD_ID)
public final class DestinyDHD {
    public static final String MOD_ID = "sgjdestiny_dhd";

    public DestinyDHD() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DestinyServerConfig.SPEC);
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.registerOptionalBlocks();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreativeItems);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var universeDHDType = BlockEntityInit.UNIVERSE_DHD.get();
            var validBlocks = new HashSet<Block>(universeDHDType.validBlocks);
            validBlocks.add(ModBlocks.DESTINY_DHD.get());
            universeDHDType.validBlocks = Set.copyOf(validBlocks);
            SpawnPlacements.register(ModEntities.SQUIGGLER.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        });
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tab = event.getTabKey().location();
        if (tab.equals(new ResourceLocation("minecraft", "functional_blocks"))) {
            event.accept(ModItems.DESTINY_DHD.get());
            event.accept(ModItems.KINO_REMOTE.get());
            event.accept(ModItems.KINO.get());
        }
    }
}
