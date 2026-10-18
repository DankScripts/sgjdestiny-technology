package com.dankscripts.sgjdestiny_dhd.client;

import com.dankscripts.sgjdestiny_dhd.block.DestinyCorridorBlock;
import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyCorridorBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Renders the oversized corridor without passing it through chunk-local coordinates. */
public final class DestinyCorridorRenderer implements BlockEntityRenderer<DestinyCorridorBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;
    private final Map<BlockPos, CachedGeometry> cache = new LinkedHashMap<>();
    private Level cachedLevel;
    private long lastPrune = Long.MIN_VALUE;

    public DestinyCorridorRenderer(BlockEntityRendererProvider.Context context) {
        blockRenderer = context.getBlockRenderDispatcher();
        DestinyMeshBuffers.register(DestinyCorridorRenderer.class, this::clearCache);
    }

    private void clearCache() {
        cache.values().forEach(CachedGeometry::close);
        cache.clear();
        cachedLevel = null;
        lastPrune = Long.MIN_VALUE;
    }

    @Override
    public void render(DestinyCorridorBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof DestinyCorridorBlock)) return;
        Level level = entity.getLevel();
        if (level == null) return;
        if (cachedLevel != level) {
            clearCache();
            cachedLevel = level;
        }
        if (lastPrune != level.getGameTime() / 20) {
            lastPrune = level.getGameTime() / 20;
            cache.entrySet().removeIf(entry -> {
                boolean stale = !level.hasChunkAt(entry.getKey())
                        || !(level.getBlockState(entry.getKey()).getBlock() instanceof DestinyCorridorBlock);
                if (stale) entry.getValue().close();
                return stale;
            });
        }
        BlockPos origin = entity.getBlockPos();
        Direction facing = state.getValue(DestinyCorridorBlock.FACING);
        int signature = lightSignature(level, origin, facing);
        CachedGeometry geometry = cache.get(origin);
        if (geometry == null || geometry.state() != state || geometry.lightSignature() != signature) {
            if (geometry != null) cache.remove(origin).close();
            geometry = new CachedGeometry(state, signature,
                    bake(level, origin, facing, state, blockRenderer.getBlockModel(state), overlay));
            // Do not evict another visible corridor on every render: a 24-entry
            // FIFO rebuilt the entire scene each frame as soon as 25 were visible.
            if (cache.size() < 256) cache.put(origin, geometry);
        }
        int angle = switch (facing) {
            case EAST -> 90;
            case NORTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-.5, 0, -.5);
        for (CachedLayer layer : geometry.layers()) layer.draw(pose);
        pose.popPose();
        if (cache.get(origin) != geometry) geometry.close();
    }

    @Override
    public boolean shouldRenderOffScreen(DestinyCorridorBlockEntity entity) {
        // The anchor can leave the visible chunk list while the far end of this
        // 14-block model is still on screen. Keep it in the global renderer list.
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(DestinyCorridorBlockEntity entity, Vec3 cameraPos) {
        // Measure from the visible mesh, not from the anchor at one end.
        AABB bounds = entity.getRenderBoundingBox();
        double dx = Math.max(bounds.minX - cameraPos.x, Math.max(0, cameraPos.x - bounds.maxX));
        double dy = Math.max(bounds.minY - cameraPos.y, Math.max(0, cameraPos.y - bounds.maxY));
        double dz = Math.max(bounds.minZ - cameraPos.z, Math.max(0, cameraPos.z - bounds.maxZ));
        int range = getViewDistance();
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    private static BlockPos worldPos(BlockPos origin, Direction facing, double x, double y, double z) {
        double wx, wz;
        switch (facing) {
            case EAST -> { wx = z; wz = 1 - x; }
            case NORTH -> { wx = 1 - x; wz = 1 - z; }
            case WEST -> { wx = 1 - z; wz = x; }
            default -> { wx = x; wz = z; }
        }
        return BlockPos.containing(origin.getX() + wx, origin.getY() + y,
                origin.getZ() + wz);
    }

    private static int lightSignature(Level level, BlockPos origin, Direction facing) {
        int signature = 1;
        for (double z : new double[]{0.5, 3.5, 7.5, 11.5, 13.5})
            for (double y : new double[]{0.5, 2.5})
                signature = 31 * signature + LevelRenderer.getLightColor(level,
                        worldPos(origin, facing, 2.5, y, z));
        return signature;
    }

    private static List<CachedLayer> bake(Level level, BlockPos origin, Direction facing,
                                          BlockState state, BakedModel model, int overlay) {
        List<CachedLayer> result = new ArrayList<>();
        RandomSource random = RandomSource.create();
        for (RenderType source : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            RenderType output = source == RenderType.translucent()
                    ? Sheets.translucentCullBlockSheet() : Sheets.solidBlockSheet();
            BufferBuilder builder = DestinyMeshBuffers.staging();
            builder.begin(output.mode(), output.format());
            PoseStack identity = new PoseStack();
            for (Direction side : Direction.values())
                emit(builder, identity, level, origin, facing, model, state, random, side, source, overlay);
            emit(builder, identity, level, origin, facing, model, state, random, null, source, overlay);
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(builder.end());
            VertexBuffer.unbind();
            result.add(new CachedLayer(output, buffer));
        }
        return result;
    }

    private static void emit(BufferBuilder builder, PoseStack pose, Level level, BlockPos origin,
                             Direction facing, BakedModel model, BlockState state,
                             RandomSource random, Direction side, RenderType type, int overlay) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, type)) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            double x = 0, y = 0, z = 0;
            for (int i = 0; i < 4; i++) {
                x += Float.intBitsToFloat(vertices[i * stride]);
                y += Float.intBitsToFloat(vertices[i * stride + 1]);
                z += Float.intBitsToFloat(vertices[i * stride + 2]);
            }
            BlockPos sample = worldPos(origin, facing, x / 4, y / 4, z / 4);
            int localLight = LevelRenderer.getLightColor(level, sample);
            builder.putBulkData(pose.last(), quad, 1.0F, 1.0F, 1.0F, localLight, overlay);
        }
    }

    private record CachedGeometry(BlockState state, int lightSignature, List<CachedLayer> layers) {
        void close() { layers.forEach(CachedLayer::close); }
    }

    private record CachedLayer(RenderType renderType, VertexBuffer buffer) {
        void close() { buffer.close(); }
        void draw(PoseStack pose) {
            renderType.setupRenderState();
            RenderSystem.disableCull();
            ShaderInstance shader = RenderSystem.getShader();
            if (shader != null) {
                buffer.bind();
                buffer.drawWithShader(pose.last().pose(), RenderSystem.getProjectionMatrix(), shader);
                VertexBuffer.unbind();
            }
            RenderSystem.enableCull();
            renderType.clearRenderState();
        }
    }
}
