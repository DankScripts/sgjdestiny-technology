package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.block_entity.KinoDispenserBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, DestinyDHD.MOD_ID);

    public static final RegistryObject<BlockEntityType<KinoDispenserBlockEntity>> KINO_DISPENSER =
            BLOCK_ENTITIES.register("kino_dispenser", () ->
                    BlockEntityType.Builder.of(KinoDispenserBlockEntity::new,
                            ModBlocks.KINO_DISPENSER.get()).build(null));

    private ModBlockEntities() {}
}
