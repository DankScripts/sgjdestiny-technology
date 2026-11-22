package com.dankscripts.sgjdestiny_dhd.block;

import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

public final class DestinyStairLayout {
    private static final int[][] ANCHOR_X = {{-13, -13, -13, -12, -11}, {-3, -3, -3, -4, -5}};

    private DestinyStairLayout() {}

    public static int anchorX(boolean right, int step) {
        return ANCHOR_X[right ? 1 : 0][step];
    }

    public static Step placement(BlockPos pos, boolean right, Direction facing, Function<BlockPos, Step> read) {
        for (int previousStep = 3; previousStep >= 0; previousStep--) {
            int dx = anchorX(right, previousStep + 1) - anchorX(right, previousStep);
            for (int turn = 0; turn < 4; turn++) {
                BlockPos offset = new BlockPos(-dx, -1, 1).rotate(Rotation.values()[turn]);
                Step previous = read.apply(pos.offset(offset));
                if (previous != null && previous.index() == previousStep
                        && previous.right() == right && previous.turn() == turn) {
                    return new Step(right, previousStep + 1, turn);
                }
            }
        }
        return new Step(right, 0, Math.floorMod(facing.get2DDataValue() + 2, 4));
    }

    public record Step(boolean right, int index, int turn) {}
}
