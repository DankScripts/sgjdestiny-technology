package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.entity.SquigglerEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DestinyDHD.MOD_ID);

    public static final RegistryObject<EntityType<SquigglerEntity>> SQUIGGLER = ENTITIES.register("squiggler",
            () -> EntityType.Builder.of(SquigglerEntity::new, MobCategory.MONSTER)
                    .sized(0.55F, 0.28F)
                    .clientTrackingRange(8)
                    .build(DestinyDHD.MOD_ID + ":squiggler"));

    private ModEntities() {}
}
