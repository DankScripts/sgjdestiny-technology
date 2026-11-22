package com.dankscripts.sgjdestiny_dhd.block;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

public final class DestinyStairPlacementTest {
    public static void main(String[] args) {
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        int[][] anchors = {{-13, -13, -13, -12, -11}, {-3, -3, -3, -4, -5}};
        for (int side = 0; side < 2; side++) {
            for (int turn = 0; turn < 4; turn++) {
                Map<BlockPos, DestinyStairLayout.Step> world = new HashMap<>();
                BlockPos pos = new BlockPos(20, 70, 20);
                var first = DestinyStairLayout.placement(pos, side == 1, facings[turn], world::get);
                check(first.turn() == turn, "first stair follows player facing");
                check(first.right() == (side == 1), "item's mirrored flight");
                world.put(pos, first);
                for (int step = 1; step < 5; step++) {
                    pos = pos.offset(new BlockPos(anchors[side][step] - anchors[side][step - 1], 1, -1)
                            .rotate(Rotation.values()[turn]));
                    var next = DestinyStairLayout.placement(pos, side == 1, facings[(turn + 1) % 4], world::get);
                    check(next.index() == step, "rotated flight advances each tread");
                    check(next.turn() == turn, "flight retains its parent's rotation");
                    world.put(pos, next);
                }
                check(DestinyStairLayout.placement(pos.above(), side == 0, facings[turn], world::get)
                        .index() == 0, "opposite mirrored item does not extend flight");
            }
        }
        System.out.println("PASS: both stair flights place and extend in all four directions");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
