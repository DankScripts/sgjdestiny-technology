package com.dankscripts.sgjdestiny_dhd.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Dust that clings to a sturdy ceiling face and carries a horizontal redstone signal. */
public final class CeilingRedstoneWireBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    private static final VoxelShape SHAPE = Block.box(0, 15, 0, 16, 16, 16);
    private static boolean readingExternalPower;

    public CeilingRedstoneWireBlock() {
        super(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.EMPTY));
        registerDefaultState(stateDefinition.any().setValue(POWER, 0)
                .setValue(NORTH, false).setValue(EAST, false)
                .setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos support = pos.above();
        return level.getBlockState(support).isFaceSturdy(level, support, Direction.DOWN);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return direction.getAxis().isHorizontal() ? withConnections(state, level, pos) : state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, 1);
            notifyNeighbors(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(state.getBlock())) {
            notifyNeighbors(level, pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide) {
            if (!canSurvive(state, level, pos)) {
                dropResources(state, level, pos);
                level.removeBlock(pos, false);
            } else {
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    public void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        int power = externalPower(level, pos);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState adjacent = level.getBlockState(pos.relative(direction));
            if (adjacent.is(this)) {
                power = Math.max(power, adjacent.getValue(POWER) - 1);
            }
        }
        if (power != state.getValue(POWER)) {
            level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_CLIENTS);
            notifyNeighbors(level, pos);
        }
    }

    private static int externalPower(Level level, BlockPos pos) {
        readingExternalPower = true;
        try {
            int power = 0;
            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = pos.relative(direction);
                BlockState neighbor = level.getBlockState(neighborPos);
                // Treat dust as another wire, rather than as a full-strength power source.
                // Otherwise a dust/wire pair can keep powering itself after its source goes away.
                if (neighbor.getBlock() instanceof RedStoneWireBlock) {
                    power = Math.max(power, neighbor.getValue(RedStoneWireBlock.POWER) - 1);
                } else if (!(neighbor.getBlock() instanceof CeilingRedstoneWireBlock)) {
                    power = Math.max(power, level.getSignal(neighborPos, direction));
                }
            }
            return power;
        } finally {
            readingExternalPower = false;
        }
    }

    private void notifyNeighbors(Level level, BlockPos pos) {
        level.updateNeighborsAt(pos, this);
        for (Direction direction : Direction.values()) {
            level.updateNeighborsAt(pos.relative(direction), this);
        }
    }

    private static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(NORTH, connects(level, pos.north(), Direction.SOUTH))
                .setValue(EAST, connects(level, pos.east(), Direction.WEST))
                .setValue(SOUTH, connects(level, pos.south(), Direction.NORTH))
                .setValue(WEST, connects(level, pos.west(), Direction.EAST));
    }

    private static boolean connects(BlockGetter level, BlockPos pos, Direction towardWire) {
        BlockState neighbor = level.getBlockState(pos);
        return neighbor.getBlock() instanceof CeilingRedstoneWireBlock
                || neighbor.canRedstoneConnectTo(level, pos, towardWire)
                || neighbor.isSignalSource();
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return readingExternalPower ? 0 : state.getValue(POWER);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return readingExternalPower || direction != Direction.UP ? 0 : state.getValue(POWER);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return direction != null && direction.getAxis().isHorizontal();
    }
}
