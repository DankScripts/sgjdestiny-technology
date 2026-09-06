package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDHDConsoleBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDHDInteractionBlock;
import com.dankscripts.sgjdestiny_dhd.block.LitDestinyBearingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DestinyDHD.MOD_ID);

    public static final RegistryObject<Block> DESTINY_DHD =
            BLOCKS.register("destiny_dhd", DestinyDHDConsoleBlock::new);

    public static final RegistryObject<Block> DESTINY_DHD_INTERACTION =
            BLOCKS.register("destiny_dhd_interaction", DestinyDHDInteractionBlock::new);

        public static RegistryObject<Block> litDestinyBearing;

        public static void registerOptionalBlocks() {
                if (ModList.get().isLoaded("sgj_deco")) {
                        litDestinyBearing = BLOCKS.register("lit_destiny_bearing", LitDestinyBearingBlock::new);
                }
        }

    private ModBlocks() {}
}
