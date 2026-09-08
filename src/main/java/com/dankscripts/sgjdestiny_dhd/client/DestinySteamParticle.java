package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A compact pressure plume that grows and softens after leaving a floor vent. */
public final class DestinySteamParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float startingSize;

    private DestinySteamParticle(
            ClientLevel level, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ);
        this.sprites = sprites;
        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
        this.hasPhysics = false;
        this.friction = 0.965F;
        this.gravity = -0.0025F;
        this.lifetime = 22 + this.random.nextInt(9);
        this.startingSize = 0.25F + this.random.nextFloat() * 0.085F;
        this.quadSize = this.startingSize;
        this.alpha = 0.88F;
        this.rCol = 0.86F;
        this.gCol = 0.91F;
        this.bCol = 0.94F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;
        float progress = this.age / (float) this.lifetime;
        this.quadSize = this.startingSize * (0.95F + progress * 1.85F);
        this.alpha = 0.88F * (1.0F - progress * progress);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double dx, double dy, double dz) {
            return new DestinySteamParticle(level, x, y, z, dx, dy, dz, sprites);
        }
    }

    @Mod.EventBusSubscriber(modid = DestinyDHD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        private static final Logger LOGGER = LoggerFactory.getLogger("SGJ Destiny DHD/Particles");

        private Registration() {}

        @SubscribeEvent
        public static void registerParticles(RegisterParticleProvidersEvent event) {
            // One callback owns both providers. This is the same callback proven
            // active by the visible custom steam, so lightning cannot be lost to
            // independent nested-subscriber discovery.
            event.registerSpecial(ModParticles.DESTINY_LIGHTNING.get(),
                    new DestinyLightningParticle.Provider());
            event.registerSpriteSet(ModParticles.DESTINY_STEAM.get(), Provider::new);
            LOGGER.info("[Destiny Particles] lightning and steam providers registered");
        }
    }
}
