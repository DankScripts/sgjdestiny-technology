package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.compat.DestinyAtmosphere;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Wall-mounted manual controller for the nearest three-by-three Destiny ship airlock. */
public final class DestinyDoorControllerBlock extends Block {
    private static final int MAX_DOOR_DISTANCE_SQUARED = 3 * 3;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    // 0: ordinary wall mount; 1: current corridor sockets; 2: legacy far socket.
    public static final IntegerProperty CORRIDOR_OFFSET = IntegerProperty.create("corridor_offset", 0, 2);
    private static final VoxelShape NORTH = Block.box(3.75, 1.75, 13.5, 12.25, 14.25, 16);
    private static final VoxelShape SOUTH = Block.box(3.75, 1.75, 0, 12.25, 14.25, 2.5);
    private static final VoxelShape EAST = Block.box(0, 1.75, 3.75, 2.5, 14.25, 12.25);
    private static final VoxelShape WEST = Block.box(13.5, 1.75, 3.75, 16, 14.25, 12.25);

    public DestinyDoorControllerBlock() {
        super(BlockBehaviour.Properties.of().strength(3.0F, 8.0F).sound(SoundType.METAL)
                .noOcclusion().noCollission().lightLevel(state -> state.getValue(LOCKED) ? 7
                        : state.getValue(ACTIVE) ? 11 : 0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(PRESSED, false).setValue(LOCKED, false).setValue(ACTIVE, false)
                .setValue(CORRIDOR_OFFSET, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSED, LOCKED, ACTIVE, CORRIDOR_OFFSET);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState target = context.getLevel().getBlockState(context.getClickedPos());
        if (DestinyCorridorCollisionBlock.isControlMount(target)) {
            Direction forward = target.getValue(DestinyCorridorCollisionBlock.FACING);
            Direction inward = target.getValue(DestinyCorridorCollisionBlock.PART)
                    == DestinyCorridorCollisionBlock.CONTROL_MOUNT_LEFT
                    ? forward.getCounterClockWise() : forward.getClockWise();
            return defaultBlockState().setValue(FACING, inward).setValue(CORRIDOR_OFFSET, 1);
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        VoxelShape shape = switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
        int offset = state.getValue(CORRIDOR_OFFSET);
        if (offset == 0) return shape;
        Direction shift = offset == 1 ? state.getValue(FACING).getClockWise()
                : state.getValue(FACING).getCounterClockWise();
        double distance = offset == 1 ? 0.355 : 0.645;
        return shape.move(shift.getStepX() * distance, 0, shift.getStepZ() * distance);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ServerLevel server = (ServerLevel) level;
        BlockPos doorPos = nearestDoor(server, pos);
        if (doorPos == null) {
            player.displayClientMessage(Component.literal("No Destiny airlock nearby"), true);
            return InteractionResult.CONSUME;
        }
        DestinyShipAirlockBlock door = (DestinyShipAirlockBlock) ModBlocks.DESTINY_SHIP_AIRLOCK.get();
        if (player.isShiftKeyDown()) {
            BlockState doorState = server.getBlockState(doorPos);
            BlockPos anchor = DestinyShipAirlockBlock.anchor(doorPos, doorState);
            boolean locked = !server.getBlockState(anchor).getValue(DestinyShipAirlockBlock.LOCKED);
            door.setHardLocked(server, doorPos, locked);
            player.displayClientMessage(Component.literal(locked ? "Airlock hard locked" : "Airlock unlocked"), true);
            refresh(server, pos);
            return InteractionResult.CONSUME;
        }
        if (state.getValue(PRESSED)) return InteractionResult.CONSUME;
        server.setBlock(pos, state.setValue(PRESSED, true), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, 20);
        server.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.45F, 0.8F);
        if (!door.toggleFromController(server, doorPos))
            player.displayClientMessage(Component.literal("Airlock hard locked"), true);
        refresh(server, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide() && !oldState.is(this)) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this)) return;
        if (state.getValue(PRESSED)) {
            level.setBlock(pos, state.setValue(PRESSED, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.75F);
        }
        refresh(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                BlockPos neighborPos, boolean moving) {
        if (level instanceof ServerLevel server) refresh(server, pos);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return false;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public void refresh(ServerLevel level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return;
        BlockPos doorPos = nearestDoor(level, pos);
        boolean locked = false;
        boolean active = false;
        if (doorPos != null) {
            BlockState doorPart = level.getBlockState(doorPos);
            BlockState doorBase = level.getBlockState(DestinyShipAirlockBlock.anchor(doorPos, doorPart));
            if (doorBase.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())) {
                locked = doorBase.getValue(DestinyShipAirlockBlock.LOCKED);
                active = doorBase.getValue(DestinyShipAirlockBlock.OPEN) && !locked;
            }
        }
        if (current.getValue(LOCKED) != locked || current.getValue(ACTIVE) != active)
            level.setBlock(pos, current.setValue(LOCKED, locked).setValue(ACTIVE, active), Block.UPDATE_CLIENTS);
    }

    private static BlockPos nearestDoor(ServerLevel level, BlockPos controller) {
        BlockPos best = null;
        int bestDistance = MAX_DOOR_DISTANCE_SQUARED + 1;
        boolean destiny = DestinyAtmosphere.isDestiny(level);
        BlockState controllerState = level.getBlockState(controller);
        Direction front = controllerState.getValue(FACING);
        Direction left = front.getClockWise();
        for (BlockPos candidate : BlockPos.betweenClosed(controller.offset(-4, -2, -4),
                controller.offset(4, 2, 4))) {
            BlockState state = level.getBlockState(candidate);
            if (!state.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())) continue;
            BlockPos center = DestinyShipAirlockBlock.anchor(candidate, state)
                    .relative(state.getValue(DestinyShipAirlockBlock.FACING).getClockWise())
                    .above();
            int dx = center.getX() - controller.getX();
            int dy = center.getY() - controller.getY();
            int dz = center.getZ() - controller.getZ();
            if (destiny) {
                // A player facing the panel sees the door to its left. The
                // panel may be on a corridor shoulder, one block in front of
                // the door plane, so allow a little depth but no right door.
                int sideways = dx * left.getStepX() + dz * left.getStepZ();
                int depth = dx * front.getStepX() + dz * front.getStepZ();
                if (sideways < 1 || sideways > 3 || depth < -1 || depth > 2
                        || Math.abs(dy) > 2) continue;
            }
            int distance = dx * dx + dy * dy + dz * dz;
            if (distance <= MAX_DOOR_DISTANCE_SQUARED && distance < bestDistance) {
                best = candidate.immutable();
                bestDistance = distance;
            }
        }
        return best;
    }
}
