package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DestinyDHD.MOD_ID);

    public static final RegistryObject<SoundEvent> STEAM_RELEASE = SOUNDS.register("steam_release",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(DestinyDHD.MOD_ID, "steam_release")));

    private ModSounds() {}
}
