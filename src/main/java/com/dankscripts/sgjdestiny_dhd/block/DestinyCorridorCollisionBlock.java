package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

/** Invisible, local collision cells following the corridor's floor and shell. */
public final class DestinyCorridorCollisionBlock extends Block {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public static final int FLOOR = 0;
    public static final int WALL = 1;
    public static final int LEFT_SHOULDER = 2;
    public static final int RIGHT_SHOULDER = 3;
    public static final int ROOF = 4;
    public static final int ROOF_LIGHT = 5;
    public static final int CONTROL_MOUNT_LEFT = 6;
    public static final int CONTROL_MOUNT_RIGHT = 7;
    public static final int ENTRANCE_HANDLE = 8;

    public DestinyCorridorCollisionBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 12.0F)
                .sound(SoundType.METAL).noOcclusion().noLootTable()
                .lightLevel(state -> state.getValue(PART) == ROOF_LIGHT ? 13 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) == ENTRANCE_HANDLE) return Block.box(0, 0, 0, 16, 1, 16);
        BlockPos adjacentController = overhangingController(state, level, pos);
        if (adjacentController != null) {
            Direction forward = state.getValue(FACING);
            VoxelShape controller = level.getBlockState(adjacentController)
                    .getShape(level, adjacentController, context);
            return Shapes.join(controller.move(-forward.getStepX(), 0, -forward.getStepZ()),
                    Shapes.block(), BooleanOp.AND);
        }
        if (!isControlMount(state)) return Shapes.empty();
        // The rendered plate is shorter than a cell and sits close to the
        // outside of the tunnel. Shape coordinates may cross a cell boundary:
        // the near and far plates both extend beyond their helper cells.
        // Keep collision separate below.
        boolean near = state.getValue(PART) == CONTROL_MOUNT_RIGHT;
        double crossMin = near ? 13.8 : -0.7;
        double crossMax = near ? 16.7 : 2.3;
        double alongMin = near ? -3.8 : -8.5;
        double alongMax = near ? 8.5 : 3.8;
        double yMin = 1.4;
        double yMax = 15.0;
        VoxelShape plate = switch (state.getValue(FACING)) {
            case NORTH -> Block.box(16 - crossMax, yMin, 16 - alongMax,
                    16 - crossMin, yMax, 16 - alongMin);
            case EAST -> Block.box(alongMin, yMin, 16 - crossMax,
                    alongMax, yMax, 16 - crossMin);
            case WEST -> Block.box(16 - alongMax, yMin, crossMin,
                    16 - alongMin, yMax, crossMax);
            default -> Block.box(crossMin, yMin, alongMin, crossMax, yMax, alongMax);
        };
        Direction forward = state.getValue(FACING);
        return near ? plate : plate.move(forward.getStepX(), 0, forward.getStepZ());
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (state.getValue(PART) == ENTRANCE_HANDLE)
            return context.getItemInHand().is(ModItems.DESTINY_SHIP_AIRLOCK.get());
        return isControlMount(state) && context.getItemInHand().is(ModItems.DESTINY_DOOR_CONTROLLER.get());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos adjacentController = overhangingController(state, level, pos);
        if (adjacentController != null) {
            BlockState controller = level.getBlockState(adjacentController);
            return controller.use(level, player, hand,
                    new BlockHitResult(hit.getLocation(), hit.getDirection(), adjacentController, hit.isInside()));
        }
        if (!isControlMount(state) || !player.getItemInHand(hand).is(ModItems.DESTINY_DOOR_CONTROLLER.get()))
            return InteractionResult.PASS;
        if (!level.isClientSide()) {
            Direction forward = state.getValue(FACING);
            Direction inward = state.getValue(PART) == CONTROL_MOUNT_LEFT
                    ? forward.getCounterClockWise() : forward.getClockWise();
            BlockState controller = ModBlocks.DESTINY_DOOR_CONTROLLER.get().defaultBlockState()
                    .setValue(DestinyDoorControllerBlock.FACING, inward)
                    .setValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET, 1);
            if (level.setBlock(pos, controller, Block.UPDATE_ALL) && !player.getAbilities().instabuild)
                player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static boolean isControlMount(BlockState state) {
        if (!(state.getBlock() instanceof DestinyCorridorCollisionBlock)) return false;
        int part = state.getValue(PART);
        return part == CONTROL_MOUNT_LEFT || part == CONTROL_MOUNT_RIGHT;
    }

    /** Select the small part of a controller that crosses into the next helper cell. */
    private static BlockPos overhangingController(BlockState state, BlockGetter level, BlockPos pos) {
        int part = state.getValue(PART);
        if (part != LEFT_SHOULDER && part != RIGHT_SHOULDER) return null;
        Direction forward = state.getValue(FACING);
        BlockPos neighbor = pos.relative(forward.getOpposite());
        BlockState candidate = level.getBlockState(neighbor);
        if (!candidate.is(ModBlocks.DESTINY_DOOR_CONTROLLER.get())
                || candidate.getValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET) != 1) return null;
        Direction expected = part == LEFT_SHOULDER
                ? forward.getCounterClockWise() : forward.getClockWise();
        return candidate.getValue(DestinyDoorControllerBlock.FACING) == expected ? neighbor : null;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(PART) == ENTRANCE_HANDLE) {
            Direction forward = state.getValue(FACING);
            BlockPos anchor = pos.relative(forward.getClockWise(), 2);
            if (level.getBlockState(anchor).is(ModBlocks.DESTINY_CORRIDOR.get())) {
                if (!player.isCreative())
                    popResource(level, pos, new net.minecraft.world.item.ItemStack(ModItems.DESTINY_CORRIDOR.get()));
                level.destroyBlock(anchor, false, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        int part = state.getValue(PART);
        if (part == FLOOR || part == ENTRANCE_HANDLE) return Block.box(0, 0, 0, 16, 1, 16);
        if (part == WALL) return Shapes.block();
        if (part == ROOF || part == ROOF_LIGHT) return Block.box(0, 0, 0, 16, 12, 16);
        boolean left = part == LEFT_SHOULDER || part == CONTROL_MOUNT_LEFT;
        Direction facing = state.getValue(FACING);
        if (facing == Direction.NORTH || facing == Direction.EAST) left = !left;
        if (facing == Direction.SOUTH || facing == Direction.NORTH)
            return left ? Block.box(0, 0, 0, 3, 16, 16) : Block.box(13, 0, 0, 16, 16, 16);
        return left ? Block.box(0, 0, 0, 16, 16, 3) : Block.box(0, 0, 13, 16, 16, 16);
    }
}
