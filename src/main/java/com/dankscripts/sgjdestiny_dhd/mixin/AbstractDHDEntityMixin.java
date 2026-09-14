package com.dankscripts.sgjdestiny_dhd.mixin;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyDialDelay;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.dankscripts.sgjdestiny_dhd.compat.KinoEvents;

@Mixin(value = AbstractDHDEntity.class, remap = false)
public abstract class AbstractDHDEntityMixin {
    @Inject(method = "encodeSymbol", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$preLightBearing(int symbol, CallbackInfo callback) {
        if (DestinyDialDelay.interceptFirstSymbol((AbstractDHDEntity) (Object) this, symbol)) {
            callback.cancel();
        }
    }

    @Inject(method = "engageStargate", at = @At("HEAD"), remap = false)
    private void sgjdestiny$prepareNineChevronDischarge(CallbackInfo callback) {
        DestinyDialDelay.observeEngage((AbstractDHDEntity) (Object) this);
    }

    @Inject(method = "autocloseTicks", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$holdKinoWormhole(CallbackInfoReturnable<Integer> callback) {
        AbstractDHDEntity dhd = (AbstractDHDEntity) (Object) this;
        if (KinoEvents.shouldHoldOpen(dhd.stargateCache.get())) {
            callback.setReturnValue(0);
        }
    }
}
