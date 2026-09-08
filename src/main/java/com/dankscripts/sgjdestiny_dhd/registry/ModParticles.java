package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, DestinyDHD.MOD_ID);

    public static final RegistryObject<SimpleParticleType> DESTINY_LIGHTNING =
            PARTICLES.register("destiny_lightning", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> DESTINY_STEAM =
            PARTICLES.register("destiny_steam", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
