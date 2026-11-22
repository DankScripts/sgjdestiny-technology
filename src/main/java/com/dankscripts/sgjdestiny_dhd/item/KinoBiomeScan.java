package com.dankscripts.sgjdestiny_dhd.item;

import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class KinoBiomeScan {
    private KinoBiomeScan() {}

    static Component message(Optional<ResourceLocation> biomeId) {
        return Component.literal("BIOME: ").append(biomeId
                .map(id -> Component.translatable(Util.makeDescriptionId("biome", id)))
                .orElse(Component.literal("unknown")));
    }
}
