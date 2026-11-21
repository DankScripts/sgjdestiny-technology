package com.dankscripts.sgjdestiny_dhd.block;

import com.mojang.serialization.MapCodec;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyShipAirlockBlockEntity;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** Three-by-three Destiny door made from the original shuttle hatch geometry. */
public final class DestinyShipAirlockBlock extends BaseEntityBlock {
    private static final MapCodec<DestinyShipAirlockBlock> CODEC = simpleCodec(ignored -> new DestinyShipAirlockBlock());
    @Override protected MapCodec<DestinyShipAirlockBlock> codec() { return CODEC; }
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 2);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 2);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    /** Use the corridor's molded doorway instead of the separate ship frame. */
    public static final BooleanProperty EMBEDDED = BooleanProperty.create("embedded");
    public static final double CORRIDOR_LEAF_WIDTH_SCALE = 1.30;
    private static final VoxelShape CLOSED = Shapes.block();
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);

    public DestinyShipAirlockBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL).noOcclusion().dynamicShape());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(COLUMN, 0).setValue(ROW, 0).setValue(OPEN, false)
                .setValue(POWERED, false).setValue(LOCKED, false).setValue(EMBEDDED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW, OPEN, POWERED, LOCKED, EMBEDDED);
    }

    private static BlockPos part(BlockPos anchor, Direction facing, int column, int row) {
        return anchor.relative(facing.getClockWise(), column).above(row);
    }

    public static BlockPos anchor(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getCounterClockWise(), state.getValue(COLUMN))
                .below(state.getValue(ROW));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        // The block the player targets is the middle of the doorway. This
        // aligns the ship and shuttle centerlines at the same block center.
        BlockPos anchor = context.getClickedPos().relative(facing.getCounterClockWise());
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos target = part(anchor, facing, column, row);
                if (!context.getLevel().getWorldBorder().isWithinBounds(target)
                        || !context.getLevel().getBlockState(target).canBeReplaced(context)) return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(COLUMN, 1);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        BlockPos anchor = anchor(pos, state);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos target = part(anchor, state.getValue(FACING), column, row);
                if (!target.equals(pos)) level.setBlock(target, state.setValue(COLUMN, column)
                        .setValue(ROW, row), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        InteractionHand hand = InteractionHand.MAIN_HAND;
        // The corridor deliberately has no automatically placed wall control.
        // Its one built-in leaf can be opened directly so the passage is usable.
        if (state.getValue(EMBEDDED) && level instanceof ServerLevel server)
            toggleFromController(server, pos);
        // The door leaves are not controls. Consume the click so held items do
        // not accidentally interact with blocks behind a separate airlock.
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Only the nearby wall controller may change the door's open state. */
    public boolean toggleFromController(ServerLevel level, BlockPos partPos) {
        BlockState partState = level.getBlockState(partPos);
        if (!partState.is(this)) return false;
        BlockPos anchor = anchor(partPos, partState);
        BlockState base = level.getBlockState(anchor);
        if (!base.is(this) || base.getValue(LOCKED)) return false;
        boolean open = !base.getValue(OPEN);
        setDoor(level, anchor, base.getValue(FACING), open, false);
        if (level.getBlockEntity(anchor) instanceof DestinyShipAirlockBlockEntity entity)
            entity.cancelAutoClose();
        return true;
    }

    public boolean setHardLocked(ServerLevel level, BlockPos partPos, boolean locked) {
        BlockState partState = level.getBlockState(partPos);
        if (!partState.is(this)) return false;
        BlockPos anchor = anchor(partPos, partState);
        BlockState base = level.getBlockState(anchor);
        if (!base.is(this)) return false;
        Direction facing = base.getValue(FACING);
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 3; column++) {
                BlockPos target = part(anchor, facing, column, row);
                BlockState current = level.getBlockState(target);
                if (current.is(this)) {
                    BlockState updated = current.setValue(LOCKED, locked);
                    if (locked) updated = updated.setValue(OPEN, false).setValue(POWERED, false);
                    level.setBlock(target, updated, Block.UPDATE_ALL);
                }
            }
        if (level.getBlockEntity(anchor) instanceof DestinyShipAirlockBlockEntity entity)
            entity.cancelAutoClose();
        refreshNearbyControllers(level, anchor);
        level.playSound(null, anchor, locked ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.IRON_DOOR_OPEN,
                SoundSource.BLOCKS, 0.75F, 0.8F);
        return true;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Ignore pending auto-close ticks saved by older versions. The wall
        // controller now owns both the open and close transitions.
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean moved) {
        // Redstone does not bypass the dedicated wall controller.
    }

    private void setDoor(Level level, BlockPos anchor, Direction facing, boolean open, boolean powered) {
        BlockState base = level.getBlockState(anchor);
        if (!base.is(this)) return;
        if (base.getValue(LOCKED) && open) return;
        boolean changed = open != base.getValue(OPEN);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                BlockPos target = part(anchor, facing, column, row);
                BlockState current = level.getBlockState(target);
                if (current.is(this) && (current.getValue(OPEN) != open || current.getValue(POWERED) != powered))
                    level.setBlock(target, current.setValue(OPEN, open).setValue(POWERED, powered), Block.UPDATE_ALL);
            }
        }
        if (changed) level.playSound(null, anchor, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, 0.8F, 0.85F);
        if (level instanceof ServerLevel server) refreshNearbyControllers(server, anchor);
    }

    private static void refreshNearbyControllers(ServerLevel level, BlockPos anchor) {
        for (BlockPos candidate : BlockPos.betweenClosed(anchor.offset(-5, -2, -5),
                anchor.offset(5, 4, 5))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof DestinyDoorControllerBlock controller)
                controller.refresh(level, candidate);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(EMBEDDED) ? getCollisionShape(state, level, pos, context) : CLOSED;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int column = state.getValue(COLUMN);
        int row = state.getValue(ROW);
        Direction facing = state.getValue(FACING);
        boolean alongX = facing == Direction.NORTH || facing == Direction.SOUTH;
        VoxelShape frame;
        if (row == 2) frame = alongX
                ? Block.box(0, 12, 4.5, 16, 16, 11.5)
                : Block.box(4.5, 12, 0, 11.5, 16, 16);
        else if (column == 0 || column == 2) {
            boolean lowSide = (column == 0) == (facing == Direction.NORTH || facing == Direction.EAST);
            if (alongX) frame = lowSide
                    ? Block.box(0, 0, 4.5, 3.5, 16, 11.5)
                    : Block.box(12.5, 0, 4.5, 16, 16, 11.5);
            else frame = lowSide
                    ? Block.box(4.5, 0, 0, 11.5, 16, 3.5)
                    : Block.box(4.5, 0, 12.5, 11.5, 16, 16);
        } else frame = Shapes.empty();

        BlockEntity blockEntity = level.getBlockEntity(anchor(pos, state));
        float progress = blockEntity instanceof DestinyShipAirlockBlockEntity door
                ? door.getOpenProgress(1.0F) : (state.getValue(OPEN) ? 1.0F : 0.0F);
        double slide = (state.getValue(EMBEDDED) ? 0.65 : 1.6)
                * Math.max(0.0, Math.min(1.0, (progress * 44.0 - 10.0) / 34.0));
        double widthScale = state.getValue(EMBEDDED) ? CORRIDOR_LEAF_WIDTH_SCALE : 1.0;
        VoxelShape shape = state.getValue(EMBEDDED) ? Shapes.empty() : frame;
        if (state.getValue(EMBEDDED) && row == 0)
            shape = Shapes.or(shape, Block.box(0, 0, 0, 16, 6, 16));
        shape = Shapes.or(shape, leafCollision(column, facing, alongX, 0.15 - slide, 1.5 - slide, widthScale));
        shape = Shapes.or(shape, leafCollision(column, facing, alongX, 1.5 + slide, 2.85 + slide, widthScale));
        return shape;
    }

    private static VoxelShape leafCollision(int column, Direction facing, boolean alongX,
                                            double start, double end, double widthScale) {
        start = 1.5 + (start - 1.5) * widthScale;
        end = 1.5 + (end - 1.5) * widthScale;
        double lo = Math.max(column, start) - column;
        double hi = Math.min(column + 1.0, end) - column;
        if (hi <= lo) return Shapes.empty();
        // A SOUTH/WEST placement reverses the local horizontal cell axis.
        if (facing == Direction.SOUTH || facing == Direction.WEST) {
            double oldLo = lo;
            lo = 1.0 - hi;
            hi = 1.0 - oldLo;
        }
        return alongX ? Block.box(lo * 16.0, 0, 4.5, hi * 16.0, 16, 11.5)
                : Block.box(4.5, 0, lo * 16.0, 11.5, 16, hi * 16.0);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(COLUMN) == 0 && state.getValue(ROW) == 0
                ? new DestinyShipAirlockBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DESTINY_SHIP_AIRLOCK.get(), DestinyShipAirlockBlockEntity::tick);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative())
            popResource(level, pos, new ItemStack(ModItems.DESTINY_SHIP_AIRLOCK.get()));
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide() && !state.is(replacement.getBlock()) && !REMOVING.get()) {
            REMOVING.set(true);
            try {
                BlockPos anchor = anchor(pos, state);
                for (int row = 0; row < 3; row++)
                    for (int column = 0; column < 3; column++) {
                        BlockPos target = part(anchor, state.getValue(FACING), column, row);
                        if (!target.equals(pos) && level.getBlockState(target).is(this))
                            level.removeBlock(target, false);
                    }
            } finally { REMOVING.set(false); }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
