package com.dankscripts.sgjdestiny_dhd.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/** A short-lived, camera-facing lightning ribbon generated entirely client-side. */
public final class DestinyLightningParticle extends Particle {
    private static final int MAIN_POINTS = 10;
    private static final int LIFE_TICKS = 4;

    private final Point[] mainPath;
    private final Point[][] branches;
    private int ticksRemaining = LIFE_TICKS;

    private DestinyLightningParticle(
            ClientLevel level, double x, double y, double z,
            double targetOffsetX, double targetOffsetY, double targetOffsetZ) {
        super(level, x, y, z);
        this.hasPhysics = false;
        this.mainPath = createMainPath(x, y, z, targetOffsetX, targetOffsetY, targetOffsetZ);
        this.branches = createBranches(mainPath, x, y, z, targetOffsetX, targetOffsetY, targetOffsetZ);
    }

    @Override
    public void tick() {
        if (--ticksRemaining <= 0) remove();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }

    @Override
    public void render(VertexConsumer ignored, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder vertices = Tesselator.getInstance().getBuilder();
        vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // A soft cyan sheath and a narrow white-hot core read as an electrical
        // discharge even against the black Universe gate ring.
        drawPath(vertices, cameraPosition, mainPath, 0.105, 52, 194, 255, 175);
        drawPath(vertices, cameraPosition, mainPath, 0.036, 235, 251, 255, 255);
        for (Point[] branch : branches) {
            drawPath(vertices, cameraPosition, branch, 0.060, 40, 174, 255, 145);
            drawPath(vertices, cameraPosition, branch, 0.021, 215, 246, 255, 235);
        }
        BufferUploader.drawWithShader(vertices.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawPath(
            VertexConsumer vertices, Vec3 camera, Point[] path, double halfWidth,
            int red, int green, int blue, int alpha) {
        for (int index = 0; index + 1 < path.length; index++) {
            Point start = path[index];
            Point end = path[index + 1];
            double sx = end.x - start.x;
            double sy = end.y - start.y;
            double sz = end.z - start.z;
            double mx = (start.x + end.x) * 0.5 - camera.x;
            double my = (start.y + end.y) * 0.5 - camera.y;
            double mz = (start.z + end.z) * 0.5 - camera.z;

            // Segment cross view gives a ribbon normal that always faces the camera.
            double px = sy * mz - sz * my;
            double py = sz * mx - sx * mz;
            double pz = sx * my - sy * mx;
            double length = Math.sqrt(px * px + py * py + pz * pz);
            if (length < 1.0e-5) {
                px = -sz;
                py = 0.0;
                pz = sx;
                length = Math.sqrt(px * px + pz * pz);
            }
            if (length < 1.0e-5) continue;
            px = px / length * halfWidth;
            py = py / length * halfWidth;
            pz = pz / length * halfWidth;

            vertex(vertices, start.x - camera.x + px, start.y - camera.y + py,
                    start.z - camera.z + pz, red, green, blue, alpha);
            vertex(vertices, start.x - camera.x - px, start.y - camera.y - py,
                    start.z - camera.z - pz, red, green, blue, alpha);
            vertex(vertices, end.x - camera.x - px, end.y - camera.y - py,
                    end.z - camera.z - pz, red, green, blue, alpha);
            vertex(vertices, end.x - camera.x + px, end.y - camera.y + py,
                    end.z - camera.z + pz, red, green, blue, alpha);
        }
    }

    private static void vertex(
            VertexConsumer vertices, double x, double y, double z,
            int red, int green, int blue, int alpha) {
        vertices.vertex(x, y, z).color(red, green, blue, alpha).endVertex();
    }

    private static Point[] createMainPath(
            double x, double y, double z, double dx, double dy, double dz) {
        Random random = seededRandom(x, y, z, dx, dy, dz);
        Point[] points = new Point[MAIN_POINTS];
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double ux = dx / length;
        double uy = dy / length;
        double uz = dz / length;
        // Two perpendicular axes allow true spatial zig-zags instead of a flat sine wave.
        double ax = -uz;
        double ay = 0.0;
        double az = ux;
        double axisLength = Math.sqrt(ax * ax + az * az);
        if (axisLength < 1.0e-5) {
            ax = 1.0;
            az = 0.0;
        } else {
            ax /= axisLength;
            az /= axisLength;
        }
        double bx = uy * az - uz * ay;
        double by = uz * ax - ux * az;
        double bz = ux * ay - uy * ax;

        for (int index = 0; index < MAIN_POINTS; index++) {
            double progress = index / (double) (MAIN_POINTS - 1);
            double envelope = Math.sin(Math.PI * progress);
            double sideways = (random.nextDouble() * 2.0 - 1.0) * 0.22 * envelope;
            double vertical = (random.nextDouble() * 2.0 - 1.0) * 0.17 * envelope;
            points[index] = new Point(
                    x + dx * progress + ax * sideways + bx * vertical,
                    y + dy * progress + ay * sideways + by * vertical,
                    z + dz * progress + az * sideways + bz * vertical);
        }
        return points;
    }

    private static Point[][] createBranches(
            Point[] main, double x, double y, double z, double dx, double dy, double dz) {
        Random random = seededRandom(z, x, y, dz, dx, dy);
        Point[][] result = new Point[2][];
        for (int branchIndex = 0; branchIndex < result.length; branchIndex++) {
            int sourceIndex = branchIndex == 0 ? 3 : 6;
            Point source = main[sourceIndex];
            Point[] branch = new Point[4];
            branch[0] = source;
            double sideX = -dz;
            double sideZ = dx;
            double sideLength = Math.sqrt(sideX * sideX + sideZ * sideZ);
            if (sideLength < 1.0e-5) sideLength = 1.0;
            sideX /= sideLength;
            sideZ /= sideLength;
            double sign = branchIndex == 0 ? -1.0 : 1.0;
            for (int index = 1; index < branch.length; index++) {
                double progress = index / (double) (branch.length - 1);
                double reach = 0.52 * progress;
                branch[index] = new Point(
                        source.x + sideX * sign * reach + (random.nextDouble() - 0.5) * 0.10,
                        source.y - 0.10 * progress + (random.nextDouble() - 0.5) * 0.13,
                        source.z + sideZ * sign * reach + (random.nextDouble() - 0.5) * 0.10);
            }
            result[branchIndex] = branch;
        }
        return result;
    }

    private static Random seededRandom(double... values) {
        long seed = 0x9e3779b97f4a7c15L;
        for (double value : values) {
            seed ^= Double.doubleToLongBits(value) + 0x9e3779b97f4a7c15L + (seed << 6) + (seed >>> 2);
        }
        return new Random(seed ^ System.nanoTime());
    }

    private record Point(double x, double y, double z) {}

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(
                SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double dx, double dy, double dz) {
            return new DestinyLightningParticle(level, x, y, z, dx, dy, dz);
        }
    }

}
