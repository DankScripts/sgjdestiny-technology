package com.dankscripts.sgjdestiny_dhd.compat;

import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;

final class GateTerrainSurvey {
    private GateTerrainSurvey() {}

    static boolean isBuried(int x, int centerY, int z, IntBinaryOperator solidSurface) {
        int roof = centerY + 8;
        return solidSurface.applyAsInt(x, z) >= roof
                && solidSurface.applyAsInt(x - 8, z) >= roof
                && solidSurface.applyAsInt(x + 8, z) >= roof
                && solidSurface.applyAsInt(x, z - 8) >= roof
                && solidSurface.applyAsInt(x, z + 8) >= roof;
    }

    static int surfaceBelowVegetation(int top, int minY, IntPredicate canPass) {
        for (int y = top; y >= minY; y--) {
            if (!canPass.test(y)) return y + 1;
        }
        return minY;
    }

    static Result jungleClearing(int x, int z, int seaLevel, int maxY, int structureHeight,
                                 IntBinaryOperator ground) {
        Result clearing = measure(-3, 3, -3, 3, seaLevel + 1, maxY, structureHeight,
                (dx, dz) -> ground.applyAsInt(x + dx * 8, z + dz * 8));
        if (clearing == null || clearing.variation() > 8) return null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0)
                        && clearing.baseY() - ground.applyAsInt(x + dx * 48, z + dz * 48) > 20)
                    return null;
            }
        }
        return clearing;
    }

    static Result measure(int minX, int maxX, int minZ, int maxZ, int minY, int maxY,
                          int structureHeight, IntBinaryOperator surfaceHeight) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        long sum = 0;
        int count = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int height = surfaceHeight.applyAsInt(x, z);
                if (height <= minY) return null;
                lowest = Math.min(lowest, height);
                highest = Math.max(highest, height);
                sum += height;
                count++;
            }
        }
        if (highest + structureHeight > maxY) return null;
        int variation = highest - lowest;
        long fill = (long) highest * count - sum;
        return new Result(highest, variation, (long) variation * 10_000L + fill);
    }

    record Result(int baseY, int variation, long score) {}
}
