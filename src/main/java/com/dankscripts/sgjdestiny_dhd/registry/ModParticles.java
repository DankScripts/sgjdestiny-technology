package com.dankscripts.sgjdestiny_dhd.registry;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, DestinyDHD.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DESTINY_LIGHTNING =
            PARTICLES.register("destiny_lightning", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DESTINY_STEAM =
            PARTICLES.register("destiny_steam", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
