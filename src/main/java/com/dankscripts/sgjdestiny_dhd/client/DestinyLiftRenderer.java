package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.entity.DestinyLiftEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small static cab mesh; only its doors and entity transform move. */
public final class DestinyLiftRenderer extends EntityRenderer<DestinyLiftEntity> {
    private final BlockRenderDispatcher blocks;
    public DestinyLiftRenderer(EntityRendererProvider.Context context) {
        super(context); blocks = context.getBlockRenderDispatcher(); shadowRadius = 0;
    }
    @Override public void render(DestinyLiftEntity cab, float yaw, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        light = LightTexture.pack(Math.max(11, LightTexture.block(light)), LightTexture.sky(light));
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-90 * cab.rotation()));
        var green = Blocks.GREEN_TERRACOTTA.defaultBlockState();
        var trim = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        var panel = Blocks.BLACK_CONCRETE.defaultBlockState();
        var lamp = Blocks.SEA_LANTERN.defaultBlockState();
        // Always draw the floor with the cab. Dock blocks provide invisible support only,
        // so chunk updates cannot display a separate platform ahead of the interior.
        for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++)
            box(lamp, x, 0, z, 1, 1, 1, pose, buffers, light);
        // West-facing entry matches the original shaft and all eight landings.
        box(green, .92, 1, -1, .08, 2.65, 2, pose, buffers, light);
        box(green, -1, 1, -.999, 1.92, 2.65, .08, pose, buffers, light);
        box(green, -1, 1, .92, 1.92, 2.65, .08, pose, buffers, light);
        box(trim, -1, 3.55, -1, 2, .1, 2, pose, buffers, light);
        box(trim, -.99, 1, -.92, .08, 2.55, .1, pose, buffers, light);
        box(trim, -.99, 1, .82, .08, 2.55, .1, pose, buffers, light);
        box(trim, -.92, 1, -.91, 1.84, .1, .06, pose, buffers, light);
        box(trim, -.92, 1, .85, 1.84, .1, .06, pose, buffers, light);
        box(lamp, -.7, 3.52, -.65, 1.4, .035, .16, pose, buffers, LightTexture.FULL_BRIGHT);
        box(lamp, -.7, 3.52, .49, 1.4, .035, .16, pose, buffers, LightTexture.FULL_BRIGHT);
        // Control panel is on the back wall, facing riders entering the cab.
        box(trim, .84, 1.78, -.55, .08, .84, 1.1, pose, buffers, light);
        box(panel, .815, 1.83, -.49, .025, .72, .98, pose, buffers, light);
        for (int row = 0; row < 4; row++) for (int col = 0; col < 2; col++)
            box(Blocks.CUT_COPPER.defaultBlockState(), .798, 1.9 + row * .1, -.3 + col * .45,
                    .02, .065, .19, pose, buffers, light);
        pose.pushPose();
        pose.translate(.79, 2.46, 0);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.scale(-.007F, -.007F, .007F);
        Font font = Minecraft.getInstance().font;
        String label = "SELECT FLOOR";
        font.drawInBatch(label, -font.width(label) / 2F, 0, 0xFFE3D29B, false, pose.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
        float closed = 1 - cab.doorOpen(partialTick);
        if (closed > .001) {
            box(trim, -.99, 1, -.82, .065, 2.5, .82 * closed, pose, buffers, light);
            box(trim, -.99, 1, .82 - .82 * closed, .065, 2.5, .82 * closed, pose, buffers, light);
        }
        pose.popPose();
        super.render(cab, yaw, partialTick, pose, buffers, light);
    }
    private void box(BlockState state, double x, double y, double z, double sx, double sy, double sz,
                     PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose(); pose.translate(x, y, z); pose.scale((float) sx, (float) sy, (float) sz);
        blocks.renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(DestinyLiftEntity cab) { return TextureAtlas.LOCATION_BLOCKS; }
}
