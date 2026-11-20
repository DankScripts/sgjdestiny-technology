package com.dankscripts.sgjdestiny_dhd.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Never let vanilla or seasonal weather place snow in Destiny's airless space. */
@Mixin(value = Biome.class, priority = 2000)
public abstract class DestinySnowGuardMixin {
    // Official development mappings and the production SRG name differ here.
    @Inject(method = {"shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z",
            "m_198906_(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"},
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void sgjdestiny$noSnowInSpace(LevelReader reader, BlockPos pos,
                                           CallbackInfoReturnable<Boolean> result) {
        if (reader instanceof Level level && level.dimension().location().getNamespace().equals("sgjourney")
                && level.dimension().location().getPath().equals("destiny")) {
            result.setReturnValue(false);
        }
    }
}
