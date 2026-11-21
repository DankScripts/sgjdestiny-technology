package com.dankscripts.sgjdestiny_dhd.mixin;

import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractDHDEntity.class, remap = false)
public interface AbstractDHDEntityHardwareAccessor {
    @Accessor("energyTarget")
    long sgjdestiny$getEnergyTarget();

    @Accessor("maxEnergyTransfer")
    long sgjdestiny$getMaxEnergyTransfer();
}
