package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.entity.DestinyPilotSeatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/** The pilot chair is part of the shuttle mesh, so its mount renders nothing. */
public final class DestinyPilotSeatRenderer extends EntityRenderer<DestinyPilotSeatEntity> {
    public DestinyPilotSeatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DestinyPilotSeatEntity seat, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {}

    @Override
    public ResourceLocation getTextureLocation(DestinyPilotSeatEntity seat) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
