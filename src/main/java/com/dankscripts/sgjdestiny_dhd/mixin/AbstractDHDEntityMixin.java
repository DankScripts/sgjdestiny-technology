package com.dankscripts.sgjdestiny_dhd.mixin;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyDialDelay;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyDHDApi;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.dankscripts.sgjdestiny_dhd.compat.KinoEvents;

/** Hooks the SGJourney entity used when existing Destiny consoles are loaded. */
@Mixin(value = AbstractDHDEntity.class, remap = false)
public abstract class AbstractDHDEntityMixin {
    @Inject(method = "encodeSymbol", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$preLightBearing(int symbol, CallbackInfoReturnable<StargateInfo.FeedbackMessage> callback) {
        if (DestinyDialDelay.interceptFirstSymbol((AbstractDHDEntity) (Object) this, symbol)) {
            callback.setReturnValue(StargateInfo.Feedback.NONE.withInfo());
        }
    }

    @Inject(method = "engageStargate", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$prepareNineChevronDischarge(CallbackInfoReturnable<StargateInfo.FeedbackMessage> callback) {
        if (DestinyDialDelay.interceptEngage((AbstractDHDEntity) (Object) this))
            callback.setReturnValue(StargateInfo.Feedback.NONE.withInfo());
    }

    @Inject(method = "autocloseTicks", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$holdKinoWormhole(CallbackInfoReturnable<Integer> callback) {
        AbstractDHDEntity dhd = (AbstractDHDEntity) (Object) this;
        if (KinoEvents.shouldHoldOpen(DestinyDHDApi.connectedStargateEntity(dhd))) {
            callback.setReturnValue(0);
        }
    }
}
