package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Invisible, addressable shell; each part stores its offset to the real hub. */
public final class DestinyControlHubPartBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 79);

    public DestinyControlHubPartBlock() {
        super(BlockBehaviour.Properties.of().strength(6.0F, 12.0F)
                .sound(SoundType.METAL).noOcclusion().noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, 0));
    }

    public static int encode(int x, int y, int z) { return y*16 + (z+1)*4 + (x+1); }
    private static int x(int part) { return part%4-1; }
    private static int y(int part) { return part/16; }
    private static int z(int part) { return (part%16)/4-1; }

    private static BlockPos anchor(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        return pos.relative(facing.getCounterClockWise(), -x(part))
                .relative(facing, -z(part)).below(y(part));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return null; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return DestinyControlHubBlock.activate(level, anchor(pos, state), player);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos root = anchor(pos, state);
            if (level.getBlockState(root).is(ModBlocks.DESTINY_CONTROL_HUB.get()))
                level.destroyBlock(root, !player.isCreative());
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PART);
        if (y(part) == 4) return Block.box(0, 7, 0, 16, 16, 16);
        if (y(part) == 1) {
            if (z(part) == 0) return Shapes.block();
            if (z(part) == 2) return consoleLip(state.getValue(FACING));
            return Block.box(0, 0, 0, 16, 11, 16);
        }
        // The outer plinth is low enough to step onto; it is not a four-wide wall.
        if (y(part) == 0) return Block.box(0, 0, 0, 16, 6, 16);
        return Shapes.block();
    }

    private static VoxelShape consoleLip(Direction facing) {
        return switch (facing) {
            case NORTH -> Block.box(0, 0, 5.5, 16, 11, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 11, 10.5);
            case WEST -> Block.box(5.5, 0, 0, 16, 11, 16);
            case EAST -> Block.box(0, 0, 0, 10.5, 11, 16);
            default -> Shapes.block();
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        // The low decorative plinth spreads beyond the visible circular base.
        // Keep its selection shape for opening/breaking the hub, but do not
        // let these helper cells lift players as they approach the console.
        if (y(state.getValue(PART)) == 0) return Shapes.empty();
        return getShape(state, level, pos, context);
    }
}
