package com.dankscripts.sgjdestiny_dhd.mixin;

import com.dankscripts.sgjdestiny_dhd.compat.HandheldAutoclose;
import net.minecraft.server.level.ServerLevel;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.UniverseStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = AbstractStargateEntity.class, remap = false)
public abstract class HandheldStargateLifecycleMixin {
    @Inject(method = "connectStargate", at = @At("HEAD"))
    private void sgjdestiny$bindHandheldConnection(UUID id, StargateConnection.State state, CallbackInfo callback) {
        if ((Object) this instanceof UniverseStargateEntity gate && gate.getLevel() instanceof ServerLevel)
            HandheldAutoclose.onConnect(gate, id, state);
    }

    @Inject(method = "resetStargate(Lnet/povstalec/sgjourney/common/sgjourney/StargateInfo$FeedbackMessage;)Lnet/povstalec/sgjourney/common/sgjourney/StargateInfo$FeedbackMessage;", at = @At("HEAD"))
    private void sgjdestiny$clearHandheldConnection(StargateInfo.FeedbackMessage feedback,
                                                   CallbackInfoReturnable<StargateInfo.FeedbackMessage> callback) {
        if ((Object) this instanceof UniverseStargateEntity gate && gate.getLevel() instanceof ServerLevel)
            HandheldAutoclose.onReset(gate);
    }
}
