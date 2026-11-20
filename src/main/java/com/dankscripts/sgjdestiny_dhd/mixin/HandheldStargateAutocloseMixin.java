package com.dankscripts.sgjdestiny_dhd.mixin;

import com.dankscripts.sgjdestiny_dhd.compat.HandheldAutoclose;
import net.povstalec.sgjourney.common.misc.Trinary;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.stargate.SGJourneyStargate;
import net.povstalec.sgjourney.common.sgjourney.stargate.universe.UniverseBlockEntityStargate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SGJourneyStargate.class, remap = false)
public abstract class HandheldStargateAutocloseMixin {
    @Inject(method = "shouldAutoclose", at = @At("RETURN"), cancellable = true)
    private void sgjdestiny$applyHandheldAutoclose(StargateConnection connection, boolean dialing,
                                                  CallbackInfoReturnable<Trinary> callback) {
        if (dialing && callback.getReturnValue() == Trinary.DEFAULT
                && (Object) this instanceof UniverseBlockEntityStargate record) {
            var gate = record.getStargateBlockEntity();
            if (gate != null && HandheldAutoclose.shouldClose(gate, connection))
                callback.setReturnValue(Trinary.TRUE);
        }
    }
}
