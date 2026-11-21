package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.entity.SquigglerEntity;
import com.dankscripts.sgjdestiny_dhd.entity.DestinyPilotSeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DestinyDHD.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<SquigglerEntity>> SQUIGGLER = ENTITIES.register("squiggler",
            () -> EntityType.Builder.of(SquigglerEntity::new, MobCategory.MONSTER)
                    .sized(0.55F, 0.28F)
                    .clientTrackingRange(8)
                    .build(DestinyDHD.MOD_ID + ":squiggler"));

    public static final DeferredHolder<EntityType<?>, EntityType<DestinyPilotSeatEntity>> PILOT_SEAT = ENTITIES.register("pilot_seat",
            () -> EntityType.Builder.of(DestinyPilotSeatEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.1F).clientTrackingRange(8).updateInterval(1).noSave()
                    .build(DestinyDHD.MOD_ID + ":pilot_seat"));

    private ModEntities() {}

    public static final DeferredHolder<EntityType<?>, EntityType<com.dankscripts.sgjdestiny_dhd.entity.DestinyLiftEntity>> DESTINY_LIFT =
            ENTITIES.register("destiny_elevator", () -> EntityType.Builder
                    .of(com.dankscripts.sgjdestiny_dhd.entity.DestinyLiftEntity::new, MobCategory.MISC)
                    .sized(2.0F, 1.0F).clientTrackingRange(8).updateInterval(1).fireImmune()
                    .build(DestinyDHD.MOD_ID + ":destiny_elevator"));
}
