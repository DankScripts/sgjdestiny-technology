package com.dankscripts.sgjdestiny_dhd.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.*;

/** Full-height triangular prism. Shapes are cached; there is no block entity or ticker. */
public final class DiagonalWallFillerBlock extends HorizontalDirectionalBlock {
    private static final MapCodec<DiagonalWallFillerBlock> CODEC = simpleCodec(ignored -> new DiagonalWallFillerBlock());
    @Override protected MapCodec<DiagonalWallFillerBlock> codec() { return CODEC; }
    private static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final VoxelShape[] SHAPES = buildShapes();

    public DiagonalWallFillerBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                .strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[4];
        for (int rotation = 0; rotation < 4; rotation++) {
            VoxelShape shape = Shapes.empty();
            // Fine horizontal slices follow the diagonal within 1/32 block.
            // Every slice is full-height, so walking beside it cannot cause stepping/floating.
            for (int i = 0; i < 32; i++) {
                double x0 = i / 32.0, x1 = (i + 1) / 32.0, z0 = 0, z1 = 1 - i / 32.0;
                for (int turn = 0; turn < rotation; turn++) {
                    double nx0 = 1 - z1, nx1 = 1 - z0;
                    z0 = x0; z1 = x1; x0 = nx0; x1 = nx1;
                }
                shape = Shapes.or(shape, Shapes.box(x0, 0, z0, x1, 1, z1));
            }
            shapes[rotation] = shape.optimize();
        }
        return shapes;
    }

    private static int index(Direction facing) {
        return switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos target = context.getClickedPos();
        // Continue a column even when the next block is placed against the backing wall.
        BlockState below = context.getLevel().getBlockState(target.below());
        if (below.getBlock() instanceof DiagonalWallFillerBlock) {
            return defaultBlockState().setValue(FACING, below.getValue(FACING));
        }
        BlockState above = context.getLevel().getBlockState(target.above());
        if (above.getBlock() instanceof DiagonalWallFillerBlock) {
            return defaultBlockState().setValue(FACING, above.getValue(FACING));
        }
        // The solid corner belongs toward the wall the builder is looking at.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        int i = index(state.getValue(FACING));
        return state.setValue(FACING, DIRECTIONS[switch (mirror) {
            case LEFT_RIGHT -> (3 - i) & 3;
            case FRONT_BACK -> (1 - i) & 3;
            default -> i;
        }]);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[index(state.getValue(FACING))];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPES[index(state.getValue(FACING))];
    }
}
