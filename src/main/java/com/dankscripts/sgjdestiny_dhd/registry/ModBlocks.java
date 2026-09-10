package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDHDConsoleBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyDHDInteractionBlock;
import com.dankscripts.sgjdestiny_dhd.block.DestinyFloorVentBlock;
import com.dankscripts.sgjdestiny_dhd.block.LitDestinyBearingBlock;
import com.dankscripts.sgjdestiny_dhd.block.LitDestinyFloorChevronBlock;
import com.dankscripts.sgjdestiny_dhd.block.SquigglerNestBlock;
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

    public static final RegistryObject<Block> DESTINY_FLOOR_VENT =
            BLOCKS.register("destiny_floor_vent", DestinyFloorVentBlock::new);

    public static final RegistryObject<Block> LIT_DESTINY_FLOOR_CHEVRON =
            BLOCKS.register("lit_destiny_floor_chevron", LitDestinyFloorChevronBlock::new);
    public static final RegistryObject<Block> SQUIGGLER_NEST = BLOCKS.register("squiggler_nest", SquigglerNestBlock::new);

        public static RegistryObject<Block> litDestinyBearing;

        public static void registerOptionalBlocks() {
                if (ModList.get().isLoaded("sgj_deco")) {
                        litDestinyBearing = BLOCKS.register("lit_destiny_bearing", LitDestinyBearingBlock::new);
                }
        }

    private ModBlocks() {}
}
