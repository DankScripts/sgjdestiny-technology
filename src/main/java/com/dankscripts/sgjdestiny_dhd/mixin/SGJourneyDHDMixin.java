package com.dankscripts.sgjdestiny_dhd.mixin;

import net.povstalec.sgjourney.api.dhd.BlockEntityDHD;
import net.povstalec.sgjourney.api.dhd.DHD;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.sgjourney.dhd.SGJourneyDHD;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SGJourneyDHD.class, remap = false)
public abstract class SGJourneyDHDMixin implements DHD {
    @Override
    public int autocloseTicks() {
        if (this instanceof BlockEntityDHD<?> blockDhd
                && blockDhd.getDHDBlockEntity() instanceof AbstractDHDEntity<?> dhd)
            return dhd.autocloseTicks();
        return DHD.super.autocloseTicks();
    }
}
