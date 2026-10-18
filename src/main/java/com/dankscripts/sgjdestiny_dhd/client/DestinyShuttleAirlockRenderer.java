package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.block.DestinyShuttleAirlockBlock;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyShuttleAirlockBlockEntity;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Original hatch leaves slide apart into the rear airlock's side pockets. */
public final class DestinyShuttleAirlockRenderer implements BlockEntityRenderer<DestinyShuttleAirlockBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;
    private final EnumMap<Direction, CachedShuttle> meshes = new EnumMap<>(Direction.class);

    public DestinyShuttleAirlockRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
        DestinyMeshBuffers.register(DestinyShuttleAirlockRenderer.class, () -> {
            meshes.values().forEach(CachedShuttle::close);
            meshes.clear();
        });
    }

    @Override
    public void render(DestinyShuttleAirlockBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof DestinyShuttleAirlockBlock)) return;

        // Render the oversized OBJ through an immediate buffer. Sending these
        // vertices through the normal chunk mesh path corrupts them in renderers
        // that encode positions relative to a 16-block chunk section.
        BakedModel shuttle = blockRenderer.getBlockModel(state);
        // This multipart model depends on facing only. A different wrapper
        // for an open/closed state must not trigger a huge GPU re-upload.
        CachedShuttle mesh = meshes.computeIfAbsent(state.getValue(DestinyShuttleAirlockBlock.FACING),
                ignored -> bakeShuttle(state, shuttle, light, overlay));
        float phase = entity.getOpenProgress(partialTick) * 44.0F;
        float lockTurn = Math.min(1.0F, phase / 10.0F);
        float slide = Math.max(0.0F, Math.min(1.0F, (phase - 10.0F) / 34.0F)) * 1.76F;
        Direction right = state.getValue(DestinyShuttleAirlockBlock.FACING).getClockWise();
        pose.pushPose();
        pose.translate(-right.getStepX() * slide, 0.0, -right.getStepZ() * slide);
        mesh.leftDoor().draw(pose);
        pose.popPose();
        pose.pushPose();
        pose.translate(right.getStepX() * slide, 0.0, right.getStepZ() * slide);
        mesh.rightDoor().draw(pose);
        pose.popPose();

        pose.pushPose();
        pose.translate(-right.getStepX() * slide, 0.0, -right.getStepZ() * slide);
        double[] pivot = mesh.gearPivot();
        pose.translate(pivot[0], pivot[1], pivot[2]);
        Axis axis = switch (state.getValue(DestinyShuttleAirlockBlock.FACING)) {
            case SOUTH -> Axis.ZN;
            case EAST -> Axis.XP;
            case WEST -> Axis.XN;
            default -> Axis.ZP;
        };
        pose.mulPose(axis.rotationDegrees(lockTurn * 360.0F));
        pose.translate(-pivot[0], -pivot[1], -pivot[2]);
        mesh.gear().draw(pose);
        pose.popPose();

        // Draw the opaque hatch first. When shut and viewed from the rear it
        // covers much of the cabin and hull, allowing depth rejection before
        // those much larger meshes are processed.
        mesh.staticMesh().draw(pose);

    }

    @Override
    public boolean shouldRenderOffScreen(DestinyShuttleAirlockBlockEntity entity) {
        // The hatch anchor can be behind the camera while the player looks
        // toward the nose. Keep the complete shuttle in the global block-
        // entity render list whenever the player is near it, including the
        // front cockpit where a tight bounding-box test can fail at its edge.
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        var eye = player.getEyePosition();
        var anchor = entity.getBlockPos();
        double dx = eye.x - (anchor.getX() + 0.5);
        double dy = eye.y - (anchor.getY() + 0.5);
        double dz = eye.z - (anchor.getZ() + 0.5);
        return dx * dx + dy * dy + dz * dz <= 40.0 * 40.0;
    }

    @Override
    public int getViewDistance() {
        // Allow the complete shuttle to remain visible through a longer client
        // render distance. The frustum still culls it when it is off screen.
        return 512;
    }

    private static CachedShuttle bakeShuttle(BlockState state, BakedModel model, int light, int overlay) {
        return new CachedShuttle(
                bakePart(state, model, light, overlay, MeshPart.STATIC),
                bakePart(state, model, light, overlay, MeshPart.LEFT_DOOR),
                bakePart(state, model, light, overlay, MeshPart.RIGHT_DOOR),
                bakePart(state, model, light, overlay, MeshPart.GEAR),
                gearPivot(state, model));
    }

    private static double[] gearPivot(BlockState state, BakedModel model) {
        double[] low = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] high = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        RandomSource random = RandomSource.create();
        for (RenderType layer : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            for (Direction side : Direction.values()) scanGear(model, state, random, side, layer, low, high);
            scanGear(model, state, random, null, layer, low, high);
        }
        if (!Double.isFinite(low[0])) return new double[]{0.5, 2.911, 0.315};
        return new double[]{(low[0] + high[0]) * 0.5, (low[1] + high[1]) * 0.5,
                (low[2] + high[2]) * 0.5};
    }

    private static void scanGear(BakedModel model, BlockState state, RandomSource random,
                                 Direction side, RenderType layer, double[] low, double[] high) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, layer)) {
            if (!MeshPart.GEAR.accepts(quad)) continue;
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            for (int corner = 0; corner < 4; corner++) {
                for (int axis = 0; axis < 3; axis++) {
                    double value = Float.intBitsToFloat(vertices[corner * stride + axis]);
                    low[axis] = Math.min(low[axis], value);
                    high[axis] = Math.max(high[axis], value);
                }
            }
        }
    }

    private static CachedMesh bakePart(BlockState state, BakedModel model, int light, int overlay, MeshPart part) {
        RandomSource random = RandomSource.create();
        PoseStack identity = new PoseStack();
        List<CachedLayer> layers = new ArrayList<>();
        for (RenderType layer : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            boolean twoSided = layer != RenderType.translucent();
            RenderType outputLayer = layer == RenderType.translucent()
                    ? Sheets.translucentCullBlockSheet()
                    : Sheets.solidBlockSheet();
            BufferBuilder builder = DestinyMeshBuffers.staging();
            builder.begin(outputLayer.mode(), outputLayer.format());
            for (Direction side : Direction.values()) {
                renderQuads(identity, builder, state, model, random, side, layer, light, overlay, part);
            }
            renderQuads(identity, builder, state, model, random, null, layer, light, overlay, part);
            VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            vertexBuffer.bind();
            vertexBuffer.upload(builder.end());
            VertexBuffer.unbind();
            layers.add(new CachedLayer(outputLayer, vertexBuffer, twoSided));
        }
        return new CachedMesh(layers);
    }

    private static void renderQuads(PoseStack pose, VertexConsumer out, BlockState state, BakedModel model,
                                    RandomSource random, Direction side, RenderType layer, int light, int overlay,
                                    MeshPart part) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, layer)) {
            if (part.accepts(quad)) {
                out.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, light, overlay);
            }
        }
    }

    private record CachedLayer(RenderType renderType, VertexBuffer buffer, boolean twoSided) implements AutoCloseable {
        void draw(PoseStack pose) {
            renderType.setupRenderState();
            // The source shuttle contains a mix of inward- and outward-wound
            // triangles. Its solid skin must therefore be rendered two-sided;
            // otherwise entire hull panels vanish from one viewing direction.
            if (twoSided) RenderSystem.disableCull();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                buffer.bind();
                buffer.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader);
                VertexBuffer.unbind();
            }
            if (twoSided) RenderSystem.enableCull();
            renderType.clearRenderState();
        }

        @Override
        public void close() {
            buffer.close();
        }
    }

    private record CachedMesh(List<CachedLayer> layers) implements AutoCloseable {
        void draw(PoseStack pose) {
            for (CachedLayer layer : layers) layer.draw(pose);
        }

        @Override
        public void close() {
            for (CachedLayer layer : layers) layer.close();
        }
    }

    private record CachedShuttle(CachedMesh staticMesh, CachedMesh leftDoor,
                                 CachedMesh rightDoor, CachedMesh gear,
                                 double[] gearPivot) implements AutoCloseable {
        @Override
        public void close() {
            staticMesh.close();
            leftDoor.close();
            rightDoor.close();
            gear.close();
        }
    }

    private enum MeshPart {
        STATIC,
        LEFT_DOOR,
        RIGHT_DOOR,
        GEAR;

        boolean accepts(BakedQuad quad) {
            String path = quad.getSprite().contents().name().getPath();
            boolean left = path.contains("destiny_shuttle_door_left");
            boolean right = path.contains("destiny_shuttle_door_right");
            boolean gear = path.contains("destiny_shuttle_door_gear");
            return switch (this) {
                case STATIC -> !left && !right && !gear;
                case LEFT_DOOR -> left;
                case RIGHT_DOOR -> right;
                case GEAR -> gear;
            };
        }
    }

}
