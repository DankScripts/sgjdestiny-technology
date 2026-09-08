package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.DestinyDHD;
import com.dankscripts.sgjdestiny_dhd.entity.SquigglerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class SquigglerRenderer extends MobRenderer<SquigglerEntity, SilverfishModel<SquigglerEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(DestinyDHD.MOD_ID, "textures/entity/squiggler.png");

    public SquigglerRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.25F);
    }

    @Override
    protected void scale(SquigglerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.82F, 0.72F, 1.18F);
    }

    @Override
    public ResourceLocation getTextureLocation(SquigglerEntity entity) {
        return TEXTURE;
    }
}
