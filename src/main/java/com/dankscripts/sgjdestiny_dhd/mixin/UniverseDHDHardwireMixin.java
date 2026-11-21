package com.dankscripts.sgjdestiny_dhd.mixin;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyHardwiredDHD;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.config.SyncedConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = UniverseDHDEntity.class, remap = false)
public abstract class UniverseDHDHardwireMixin {
    @Inject(method = "maxEnergyTransfer", at = @At("RETURN"), cancellable = true, remap = false)
    private void sgjdestiny$hardwiredEnergyTransfer(CallbackInfoReturnable<Long> ci) {
        UniverseDHDEntity dhd = (UniverseDHDEntity) (Object) this;
        if (dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get())) {
            long rawTransfer = ((AbstractDHDEntityHardwareAccessor) (Object) dhd).sgjdestiny$getMaxEnergyTransfer();
            ci.setReturnValue(DestinyHardwiredDHD.energyTransfer(ci.getReturnValue(), rawTransfer,
                    SyncedConfig.transfer_crystal_max_transfer.get()));
        }
    }

    @Inject(method = "generateCrystals", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$noGeneratedCrystals(CallbackInfo ci) {
        UniverseDHDEntity dhd = (UniverseDHDEntity) (Object) this;
        if (dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get())) ci.cancel();
    }
}
