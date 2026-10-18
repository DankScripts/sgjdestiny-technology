package com.dankscripts.sgjdestiny_dhd.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four independent railing edges share the air cell above one floor block. */
public final class DestinySafetyRailingBlock extends Block {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    private static final Direction[] EDGES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final VoxelShape[] OUTLINES = new VoxelShape[16];
    private static final VoxelShape[] COLLISIONS = new VoxelShape[16];

    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape outline = Shapes.empty();
            VoxelShape collision = Shapes.empty();
            for (int edge = 0; edge < 4; edge++) {
                if ((mask & (1 << edge)) == 0) continue;
                outline = Shapes.or(outline, edgeShape(edge, 19));
                collision = Shapes.or(collision, edgeShape(edge, 24));
            }
            OUTLINES[mask] = outline.optimize();
            COLLISIONS[mask] = collision.optimize();
        }
    }

    private static VoxelShape edgeShape(int edge, double height) {
        return switch (edge) {
            case 0 -> Block.box(0, 0, 0, 16, height, 1);
            case 1 -> Block.box(15, 0, 0, 16, height, 16);
            case 2 -> Block.box(0, 0, 15, 16, height, 16);
            default -> Block.box(0, 0, 0, 1, height, 16);
        };
    }

    public DestinySafetyRailingBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                .strength(3.5F, 8.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(NORTH, false)
                .setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    public static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("A railing edge must be horizontal");
        };
    }

    public static Direction nearestEmptyEdge(BlockState state, Vec3 hit, BlockPos pos) {
        double x = hit.x - pos.getX();
        double z = hit.z - pos.getZ();
        double[] distance = {z, 1 - x, 1 - z, x};
        Direction result = null;
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < EDGES.length; i++) {
            if (!state.getValue(property(EDGES[i])) && distance[i] < best) {
                result = EDGES[i];
                best = distance[i];
            }
        }
        return result;
    }

    private static int mask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
                | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (!canSurvive(defaultBlockState(), context.getLevel(), pos)) return null;
        Direction edge = nearestEmptyEdge(defaultBlockState(), context.getClickLocation(), pos);
        return defaultBlockState().setValue(property(edge), true);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINES[mask(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISIONS[mask(state)];
    }
}
