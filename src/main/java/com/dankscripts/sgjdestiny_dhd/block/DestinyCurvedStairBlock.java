package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** One fifth of either mirrored Destiny gate-room staircase. */
public final class DestinyCurvedStairBlock extends Block {
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");
    public static final IntegerProperty STEP = IntegerProperty.create("step", 0, 4);
    public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);
    private static final int[][] ANCHOR_X = {{-13, -13, -13, -12, -11}, {-3, -3, -3, -4, -5}};
    private static final VoxelShape[][][] SHAPES = new VoxelShape[2][5][4];

    static {
        for (int side = 0; side < 2; side++) {
            for (int step = 0; step < 5; step++) {
                VoxelShape shape = Shapes.empty();
                for (int tread = 0; tread < 3; tread++) {
                    double start = step + tread / 3.0;
                    double end = step + (tread + 1) / 3.0;
                    double x0 = pathX(side, start) - ANCHOR_X[side][step];
                    double x1 = pathX(side, end) - ANCHOR_X[side][step];
                    double z0 = pathZ(start) - (-313 - step);
                    double z1 = pathZ(end) - (-313 - step);
                    shape = Shapes.or(shape, Block.box(
                            (Math.min(x0, x1) - 0.54) * 16, 0, (Math.min(z0, z1) - 0.08) * 16,
                            (Math.max(x0, x1) + 0.54) * 16, (tread + 1) * 16 / 3.0,
                            (Math.max(z0, z1) + 0.08) * 16));
                }
                SHAPES[side][step][0] = shape.optimize();
                for (int turn = 1; turn < 4; turn++) {
                    VoxelShape rotated = Shapes.empty();
                    for (var aabb : SHAPES[side][step][turn - 1].toAabbs())
                        rotated = Shapes.or(rotated, Block.box((1 - aabb.maxZ) * 16, aabb.minY * 16, aabb.minX * 16,
                                (1 - aabb.minZ) * 16, aabb.maxY * 16, aabb.maxX * 16));
                    SHAPES[side][step][turn] = rotated.optimize();
                }
            }
        }
    }

    private static double pathX(int side, double t) {
        double u = Math.max(0.0, Math.min(1.0, t / 5.0));
        // Match the model's cubic path: the foot turns toward the gate
        // walkway, while the head bends inward and joins the balcony.
        double left = (2 * u * u * u - 3 * u * u + 1) * -12.5
                + (u * u * u - 2 * u * u + u) * -5.0
                + (-2 * u * u * u + 3 * u * u) * -9.9
                + (u * u * u - u * u) * 7.0;
        return side == 0 ? left : -15.0 - left;
    }

    private static double pathZ(double t) {
        if (t <= 4.0) return -312.0 - t;
        double finish = t - 4.0;
        return -316.0 - finish + 0.5 * finish * finish;
    }

    public DestinyCurvedStairBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                .strength(4.0F, 9.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(RIGHT, false).setValue(STEP, 0).setValue(TURN, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        boolean right = context.getItemInHand().is(ModItems.DESTINY_CURVED_STAIR_COUNTERCLOCKWISE.get());
        // Advance only the flight selected by the item in hand.
        for (int previousStep = 3; previousStep >= 0; previousStep--) {
            int side = right ? 1 : 0;
            int dx = ANCHOR_X[side][previousStep + 1] - ANCHOR_X[side][previousStep];
            BlockState previous = context.getLevel().getBlockState(pos.offset(-dx, -1, 1));
            if (previous.is(this) && previous.getValue(STEP) == previousStep
                    && previous.getValue(RIGHT) == right) {
                return defaultBlockState().setValue(STEP, previousStep + 1).setValue(RIGHT, right);
            }
        }
        return defaultBlockState().setValue(RIGHT, right);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RIGHT, STEP, TURN);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        int turns = switch (rotation) {
            case CLOCKWISE_90 -> 1;
            case CLOCKWISE_180 -> 2;
            case COUNTERCLOCKWISE_90 -> 3;
            default -> 0;
        };
        return state.setValue(TURN, (state.getValue(TURN) + turns) % 4);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(RIGHT) ? 1 : 0][state.getValue(STEP)][state.getValue(TURN)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
