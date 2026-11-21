package com.dankscripts.sgjdestiny_dhd.mixin;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyDialDelay;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyDHDApi;
import com.dankscripts.sgjdestiny_dhd.compat.DestinyHardwiredDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import net.povstalec.sgjourney.common.block_entities.dhd.UniverseDHDEntity;
import net.povstalec.sgjourney.common.config.SyncedConfig;
import net.povstalec.sgjourney.common.misc.AutoCache;
import net.povstalec.sgjourney.api.dhd.DHD;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.dankscripts.sgjdestiny_dhd.compat.KinoEvents;

/** Hooks the SGJourney entity used when existing Destiny consoles are loaded. */
@Mixin(value = AbstractDHDEntity.class, remap = false)
public abstract class AbstractDHDEntityMixin {
    @Unique
    private boolean sgjdestiny$isHardwiredConsole() {
        AbstractDHDEntity<?> dhd = (AbstractDHDEntity<?>) (Object) this;
        return dhd instanceof UniverseDHDEntity && dhd.getBlockState().is(ModBlocks.DESTINY_DHD.get());
    }

    @Inject(method = "setupAutoCache", at = @At("TAIL"), remap = false)
    private void sgjdestiny$loadedOnlyDiscovery(AutoCache.Receiver<DHD, DHDControlledStargate> cache,
                                                 CallbackInfo callback) {
        AbstractDHDEntity<?> dhd = (AbstractDHDEntity<?>) (Object) this;
        if (sgjdestiny$isHardwiredConsole()
                && dhd.getLevel() instanceof ServerLevel level) {
            cache.setFetch(() -> com.dankscripts.sgjdestiny_dhd.compat.DestinyDHDDiscovery.findNearest(level, dhd));
        }
    }

    @Inject(method = "advancedProtocolsActive", at = @At("HEAD"), cancellable = true, remap = false)
    private void sgjdestiny$hardwiredAdvancedProtocols(CallbackInfoReturnable<Boolean> callback) {
        if (sgjdestiny$isHardwiredConsole()) callback.setReturnValue(true);
    }

    @Inject(method = "getMaxConnectionDistance", at = @At("RETURN"), cancellable = true, remap = false)
    private void sgjdestiny$hardwiredRange(CallbackInfoReturnable<Double> callback) {
        if (sgjdestiny$isHardwiredConsole())
            callback.setReturnValue(DestinyHardwiredDHD.connectionRange(callback.getReturnValue()));
    }

    @Inject(method = "getMaxConnectionDistanceSqr", at = @At("RETURN"), cancellable = true, remap = false)
    private void sgjdestiny$hardwiredRangeSquared(CallbackInfoReturnable<Double> callback) {
        if (sgjdestiny$isHardwiredConsole())
            callback.setReturnValue(Math.max(callback.getReturnValue(),
                    (double) DestinyHardwiredDHD.CONNECTION_RANGE * DestinyHardwiredDHD.CONNECTION_RANGE));
    }

    @Inject(method = "getEnergyTarget", at = @At("RETURN"), cancellable = true, remap = false)
    private void sgjdestiny$hardwiredEnergyTarget(CallbackInfoReturnable<Long> callback) {
        if (sgjdestiny$isHardwiredConsole()) {
            long rawTarget = ((AbstractDHDEntityHardwareAccessor) this).sgjdestiny$getEnergyTarget();
            callback.setReturnValue(DestinyHardwiredDHD.energyTarget(callback.getReturnValue(), rawTarget,
                    SyncedConfig.energy_crystal_energy_target_increase.get()));
        }
    }

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
