package com.dankscripts.sgjdestiny_dhd.mixin;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyDialDelay;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractDHDEntity.class, remap = false)
public abstract class AbstractDHDEntityMixin {
    @Inject(method = "encodeSymbol", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$preLightBearing(int symbol, CallbackInfo callback) {
        if (DestinyDialDelay.interceptFirstSymbol((AbstractDHDEntity) (Object) this, symbol)) {
            callback.cancel();
        }
    }
}
