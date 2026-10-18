package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyShuttleAirlockBlockEntity;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Rear doorway; the lower-left block places the complete shuttle. */
public final class DestinyShuttleAirlockBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 1);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, 4);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    private static final int DOOR_ROWS = 5;
    private static final VoxelShape SILL_FLOOR = Block.box(0, 0, 0, 16, 16, 16);
    // The modeled hatch reaches just inside the anchor block (local z~-0.03)
    // while the old collision began at z=0.28, letting players enter its
    // visible inner face before meeting the collision on the outside.
    private static final double DOOR_INNER = -1.0;
    private static final double DOOR_OUTER = 9.5;
    private static final ThreadLocal<Boolean> REMOVING = ThreadLocal.withInitial(() -> false);
    private static final List<CollisionOffset> EXTERIOR_COLLISION = buildExteriorCollision();
    private static final int[] PASSENGER_SEAT_FORWARDS = {5, 6, 8, 9, 11, 12, 14, 15};

    public DestinyShuttleAirlockBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 12.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(COLUMN, 0).setValue(ROW, 0)
                .setValue(OPEN, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COLUMN, ROW, OPEN, POWERED);
    }

    public static BlockPos anchor(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getClockWise().getOpposite(), state.getValue(COLUMN))
                .below(state.getValue(ROW));
    }

    private static BlockPos part(BlockPos anchor, Direction facing, int column, int row) {
        return anchor.relative(facing.getClockWise(), column).above(row);
    }

    private static BlockPos collisionPart(BlockPos anchor, Direction facing, CollisionOffset offset) {
        return anchor.relative(facing.getClockWise(), offset.x())
                .relative(facing, -offset.z()).above(offset.y());
    }

    /** Place a shuttle on an already validated, naturally clear landing site. */
    public static boolean placeTravelShuttle(ServerLevel level, BlockPos anchor, Direction facing) {
        DestinyShuttleAirlockBlock block = (DestinyShuttleAirlockBlock) ModBlocks.DESTINY_SHUTTLE_AIRLOCK.get();
        for (int row = 0; row < DOOR_ROWS; row++) {
            for (int column = 0; column < 2; column++) {
                BlockPos target = part(anchor, facing, column, row);
                if (row > 0 && !level.getBlockState(target).isAir()) return false;
                if (row == 0 && level.getBlockEntity(target) != null) return false;
            }
        }
        for (CollisionOffset offset : EXTERIOR_COLLISION) {
            BlockPos target = collisionPart(anchor, facing, offset);
            if (offset.y() > 0 && !level.getBlockState(target).isAir()) return false;
            if (offset.y() == 0 && level.getBlockEntity(target) != null) return false;
        }
        // A clear collision shell alone does not guarantee an unobstructed cabin.
        // Reject trees, overhangs and player builds anywhere inside the model.
        Direction right = facing.getClockWise();
        for (int x = -12; x <= 12; x++) {
            for (int forward = 0; forward <= 26; forward++) {
                BlockPos ground = anchor.relative(right, x).relative(facing, forward);
                for (int y = 1; y <= 7; y++)
                    if (!level.getBlockState(ground.above(y)).isAir()) return false;
            }
        }
        BlockPos pilotSeat = DestinyPilotSeatBlock.lowerPos(anchor, facing);
        for (int layer = 0; layer < 2; layer++)
            if (!level.getBlockState(pilotSeat.above(layer)).isAir()) return false;
        for (int forward : PASSENGER_SEAT_FORWARDS) {
            for (int side : new int[]{-1, 1}) {
                BlockPos lower = anchor.relative(facing.getClockWise(), side).relative(facing, forward).above();
                for (int layer = 0; layer < 2; layer++)
                    if (!level.getBlockState(lower.above(layer)).isAir()) return false;
            }
        }
        BlockState state = block.defaultBlockState().setValue(FACING, facing);
        if (!level.setBlock(anchor, state, Block.UPDATE_ALL)) return false;
        block.setPlacedBy(level, anchor, state, null, ItemStack.EMPTY);
        return true;
    }

    private static boolean hasLiveShuttleOwner(Level level, BlockPos collision, Set<BlockPos> checked) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (CollisionOffset offset : EXTERIOR_COLLISION) {
                BlockPos candidate = collision.relative(direction.getClockWise(), -offset.x())
                        .relative(direction, offset.z()).below(offset.y());
                if (!checked.add(candidate)) continue;
                BlockState state = level.getBlockState(candidate);
                if (state.is(ModBlocks.DESTINY_SHUTTLE_AIRLOCK.get())
                        && state.getValue(FACING) == direction
                        && state.getValue(COLUMN) == 0 && state.getValue(ROW) == 0) return true;
            }
        }
        return false;
    }

    private static boolean clearForPlacement(BlockPlaceContext context, BlockPos target,
                                              boolean groundCell, boolean dockingRim, Set<BlockPos> checked) {
        Level level = context.getLevel();
        if (!level.getWorldBorder().isWithinBounds(target)) return placementBlocked(context, target, null);
        BlockState occupied = level.getBlockState(target);
        if (occupied.canBeReplaced(context)) return true;
        // The launch pad may have a thin surface-rock decoration in the same
        // cell as the lower hull. It is ground clutter, not an obstructing wall.
        // Leave existing terrain in place rather than replacing it with hull.
        if (groundCell && (occupied.isSolidRender(level, target)
                || occupied.getCollisionShape(level, target).max(Direction.Axis.Y) <= 0.5)) return true;
        // The rear rim of a docked shuttle sits against the Destiny airlock's
        // solid surround. Keep those wall blocks and their collision intact;
        // restoreExteriorCollision installs only into air cells.
        if (dockingRim && occupied.isSolidRender(level, target)) return true;
        // Older shuttle tests may leave invisible collision cells in otherwise
        // empty terrain. Reuse only cells that no placed shuttle still owns.
        if (occupied.is(ModBlocks.DESTINY_SHUTTLE_COLLISION.get())
                && !hasLiveShuttleOwner(level, target, checked)) return true;
        return placementBlocked(context, target, occupied);
    }

    private static boolean hasMatchingShipAirlock(Level level, BlockPos shuttleAnchor, Direction facing) {
        // The fixed ship door can be set back by up to two tunnel blocks from
        // the shuttle's rear hatch. Its threshold may sit one above the pad.
        for (int distance = 1; distance <= 3; distance++) {
            for (int rise = 0; rise <= 1; rise++) {
                BlockPos first = shuttleAnchor.relative(facing.getOpposite(), distance).above(rise);
                BlockPos second = first.relative(facing.getClockWise());
                BlockState a = level.getBlockState(first);
                BlockState b = level.getBlockState(second);
                if (a.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())
                        && b.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())
                        && a.getValue(DestinyShipAirlockBlock.FACING) == facing
                        && b.getValue(DestinyShipAirlockBlock.FACING) == facing
                        && a.getValue(DestinyShipAirlockBlock.ROW) == 0
                        && b.getValue(DestinyShipAirlockBlock.ROW) == 0
                        && DestinyShipAirlockBlock.anchor(first, a)
                                .equals(DestinyShipAirlockBlock.anchor(second, b))) return true;
            }
        }
        return false;
    }

    private static boolean placementBlocked(BlockPlaceContext context, BlockPos target,
                                             @Nullable BlockState occupied) {
        if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
            String reason = occupied == null ? "world boundary"
                    : occupied.is(ModBlocks.DESTINY_SHUTTLE_COLLISION.get())
                    ? "another placed shuttle" : occupied.getBlock().getName().getString();
            context.getPlayer().displayClientMessage(Component.literal(
                    "Shuttle needs a clear area. Blocked at " + target.toShortString() + " by " + reason + "."), true);
        }
        return false;
    }

    private static List<CollisionOffset> buildExteriorCollision() {
        // Rasterized from the same exterior OBJ used by the renderer. This
        // follows the nose, cockpit glazing and wings while leaving the cabin
        // hollow and the rear hatch free for its own moving collision.
        String path = "/assets/sgjdestiny_dhd/shuttle_collision_cells.csv";
        InputStream stream = DestinyShuttleAirlockBlock.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing shuttle collision resource: " + path);
        List<CollisionOffset> cells = new java.util.ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.charAt(0) == '#') continue;
                String[] fields = line.split(",");
                if (fields.length != 4) throw new IOException("Malformed shuttle collision cell: " + line);
                int x = Integer.parseInt(fields[0]);
                int y = Integer.parseInt(fields[1]);
                int z = Integer.parseInt(fields[2]);
                int shape = Integer.parseInt(fields[3]);
                // The inner cabin liners are at local x=-1.25 and x=2.25,
                // closer to the seats than the rasterized exterior shell.
                if (z >= -16 && y >= 1 && y <= 3) {
                    if (x == -2 && z <= -3) shape = y == 1
                            ? DestinyShuttleCollisionBlock.CABIN_LEFT_DECK_WALL : 68;
                    if (x == 2 && z <= -1) shape = y == 1
                            ? DestinyShuttleCollisionBlock.CABIN_RIGHT_DECK_WALL : 130;
                }
                cells.add(new CollisionOffset(x, y, z, shape));
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Cannot read shuttle collision cells", exception);
        }
        if (cells.isEmpty()) throw new IllegalStateException("Empty shuttle collision resource");
        // The right liner continues to the rear airlock, two blocks beyond
        // the seat bays. Its outboard thickness joins the exterior collision.
        for (int forward = 1; forward <= 16; forward++) {
            cells.add(new CollisionOffset(2, 2, -forward, 130));
            cells.add(new CollisionOffset(2, 3, -forward, 130));
        }
        // The final strip beside the hatch is at local z=0. Without these
        // cells the inner rear bulkhead can be walked through beside the door.
        cells.add(new CollisionOffset(2, 1, 0, DestinyShuttleCollisionBlock.CABIN_RIGHT_DECK_WALL));
        cells.add(new CollisionOffset(2, 2, 0, 130));
        cells.add(new CollisionOffset(2, 3, 0, 130));
        cells.add(new CollisionOffset(-2, 2, -16, 68));
        cells.add(new CollisionOffset(-2, 3, -16, 68));
        return List.copyOf(cells);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos pos = context.getClickedPos();
        boolean docking = hasMatchingShipAirlock(context.getLevel(), pos, facing);
        Set<BlockPos> checkedOwners = new HashSet<>();
        for (int row = 0; row < DOOR_ROWS; row++) {
            for (int column = 0; column < 2; column++) {
                BlockPos target = part(pos, facing, column, row);
                if (!clearForPlacement(context, target, row == 0, false, checkedOwners)) return null;
            }
        }
        // Refuse placement unless the whole invisible collision shell fits.
        // Silently skipping occupied cells would leave holes players could
        // walk through and would allow the shuttle to overlap another build.
        // This includes the thin deck surface over the cabin and cockpit.
        for (CollisionOffset offset : EXTERIOR_COLLISION) {
            BlockPos target = collisionPart(pos, facing, offset);
            boolean dockingRim = docking && offset.z() >= 0 && offset.z() <= 1 && offset.x() >= -3
                    && offset.x() <= 4 && offset.y() >= 1 && offset.y() <= 4;
            if (!clearForPlacement(context, target, offset.y() == 0, dockingRim, checkedOwners)) return null;
        }
        BlockPos seat = DestinyPilotSeatBlock.lowerPos(pos, facing);
        for (int layer = 0; layer < 2; layer++) {
            BlockPos target = seat.above(layer);
            if (!clearForPlacement(context, target, false, false, checkedOwners)) return null;
        }
        for (int forward : PASSENGER_SEAT_FORWARDS) {
            for (int side : new int[]{-1, 1}) {
                BlockPos upper = pos.relative(facing.getClockWise(), side).relative(facing, forward).above(2);
                if (!clearForPlacement(context, upper, false, false, checkedOwners)) return null;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        for (int row = 0; row < DOOR_ROWS; row++) {
            for (int column = 0; column < 2; column++) {
                if (row == 0 && column == 0) continue;
                level.setBlock(part(pos, facing, column, row), state.setValue(COLUMN, column)
                        .setValue(ROW, row), Block.UPDATE_ALL);
            }
        }
        restoreExteriorCollision(level, pos, facing);
        syncPower(level, pos, facing);
    }

    /** Backfill saved shuttles that were placed before the collision shell existed. */
    public static void restoreExteriorCollision(Level level, BlockPos anchor, Direction facing) {
        if (level.isClientSide()) return;
        // Remove stray full-height collision cells from earlier builds along
        // the aisle. The intended thin deck cells are restored below.
        for (int z = -20; z <= 1; z++) {
            for (int x = -1; x <= 2; x++) {
                for (int y = 1; y <= 3; y++) {
                    if (z == 0 && (x == 0 || x == 1)) continue;
                    BlockPos target = collisionPart(anchor, facing, new CollisionOffset(x, y, z));
                    if (level.getBlockState(target).is(ModBlocks.DESTINY_SHUTTLE_COLLISION.get()))
                        level.removeBlock(target, false);
                }
            }
        }
        for (CollisionOffset offset : EXTERIOR_COLLISION) {
            BlockPos target = collisionPart(anchor, facing, offset);
            BlockState occupied = level.getBlockState(target);
            BlockState desired = ModBlocks.DESTINY_SHUTTLE_COLLISION.get().defaultBlockState()
                    .setValue(DestinyShuttleCollisionBlock.FACING, facing)
                    .setValue(DestinyShuttleCollisionBlock.SHAPE_ID, offset.shapeId());
            if (occupied.isAir() || occupied.is(ModBlocks.DESTINY_SHUTTLE_COLLISION.get()) && !occupied.equals(desired))
                level.setBlock(target, desired, Block.UPDATE_CLIENTS);
        }
        BlockPos seat = DestinyPilotSeatBlock.lowerPos(anchor, facing);
        for (int layer = 0; layer < 2; layer++) {
            BlockPos target = seat.above(layer);
            if (level.getBlockState(target).isAir())
                level.setBlock(target, ModBlocks.DESTINY_PILOT_SEAT.get().defaultBlockState()
                        .setValue(DestinyPilotSeatBlock.FACING, facing)
                        .setValue(DestinyPilotSeatBlock.LAYER, layer), Block.UPDATE_CLIENTS);
        }
        for (int forward : PASSENGER_SEAT_FORWARDS) {
            for (int side : new int[]{-1, 1}) {
                BlockPos lower = anchor.relative(facing.getClockWise(), side).relative(facing, forward).above();
                for (int layer = 0; layer < 2; layer++) {
                    BlockPos target = lower.above(layer);
                    BlockState occupied = level.getBlockState(target);
                    if (!occupied.isAir() && !occupied.is(ModBlocks.DESTINY_SHUTTLE_COLLISION.get())) continue;
                    BlockState desired = ModBlocks.DESTINY_SHUTTLE_COLLISION.get().defaultBlockState()
                            .setValue(DestinyShuttleCollisionBlock.FACING, facing)
                            .setValue(DestinyShuttleCollisionBlock.SHAPE_ID,
                                    DestinyShuttleCollisionBlock.PASSENGER_SEAT_BASE
                                            + (side < 0 ? 1 : 2) + layer * 2);
                    if (!occupied.equals(desired)) level.setBlock(target, desired, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos base = anchor(pos, state);
        BlockState baseState = level.getBlockState(base);
        if (baseState.is(this)) setDoor(level, base, baseState.getValue(FACING), !baseState.getValue(OPEN),
                baseState.getValue(POWERED));
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide()) syncPower(level, anchor(pos, state), state.getValue(FACING));
    }

    private void syncPower(Level level, BlockPos base, Direction facing) {
        BlockState baseState = level.getBlockState(base);
        if (!baseState.is(this)) return;
        boolean powered = false;
        for (int row = 0; row < DOOR_ROWS; row++) {
            for (int column = 0; column < 2; column++) {
                powered |= level.hasNeighborSignal(part(base, facing, column, row));
            }
        }
        if (powered != baseState.getValue(POWERED)) setDoor(level, base, facing, powered, powered);
    }

    private void setDoor(Level level, BlockPos base, Direction facing, boolean open, boolean powered) {
        BlockState baseState = level.getBlockState(base);
        if (!baseState.is(this)) return;
        boolean changed = open != baseState.getValue(OPEN);
        for (int row = 0; row < DOOR_ROWS; row++) {
            for (int column = 0; column < 2; column++) {
                BlockPos target = part(base, facing, column, row);
                BlockState current = level.getBlockState(target);
                if (current.is(this) && (current.getValue(OPEN) != open || current.getValue(POWERED) != powered)) {
                    level.setBlock(target, current.setValue(OPEN, open).setValue(POWERED, powered), Block.UPDATE_ALL);
                }
            }
        }
        if (changed) level.playSound(null, base, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, 0.8F, 0.85F);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(ROW)) {
            case 0 -> SILL_FLOOR;
            case 1 -> doorShape(state, 2.24, 16);
            case 4 -> doorShape(state, 0, 5.48);
            default -> doorShape(state, 0, 16);
        };
    }

    private static VoxelShape doorShape(BlockState state, double lowY, double highY) {
        // The left panel reaches into the block to the left of the two-block
        // anchor, so include its actual modeled edge in the closed boundary.
        double leftX = state.getValue(COLUMN) == 0 ? -15 : 0;
        double rightX = 16;
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(16 - rightX, lowY, 16 - DOOR_OUTER,
                    16 - leftX, highY, 16 - DOOR_INNER);
            case EAST -> Block.box(16 - DOOR_OUTER, lowY, leftX,
                    16 - DOOR_INNER, highY, rightX);
            case WEST -> Block.box(DOOR_INNER, lowY, 16 - rightX,
                    DOOR_OUTER, highY, 16 - leftX);
            default -> Block.box(leftX, lowY, DOOR_INNER, rightX, highY, DOOR_OUTER);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ROW) == 0 || !state.getValue(OPEN)
                ? getShape(state, level, pos, context) : Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The shuttle extends far beyond one block. Rendering it as chunk geometry
        // makes optimized chunk renderers wrap its vertices at section boundaries.
        // The anchor block entity renders the complete model directly instead.
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(COLUMN) == 0 && state.getValue(ROW) == 0
                ? new DestinyShuttleAirlockBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.DESTINY_SHUTTLE_AIRLOCK.get(), DestinyShuttleAirlockBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.DESTINY_SHUTTLE_AIRLOCK.get(), DestinyShuttleAirlockBlockEntity::serverTick);
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
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative())
            popResource(level, pos, new ItemStack(ModItems.DESTINY_SHUTTLE_AIRLOCK.get()));
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide() && !state.is(replacement.getBlock()) && !REMOVING.get()) {
            REMOVING.set(true);
            try {
                BlockPos base = anchor(pos, state);
                Direction facing = state.getValue(FACING);
                for (int row = 0; row < DOOR_ROWS; row++) {
                    for (int column = 0; column < 2; column++) {
                        BlockPos target = part(base, facing, column, row);
                        if (!target.equals(pos) && level.getBlockState(target).is(this)) level.removeBlock(target, false);
                    }
                }
                for (CollisionOffset offset : EXTERIOR_COLLISION) {
                    BlockPos target = collisionPart(base, facing, offset);
                    if (level.getBlockState(target).is(ModBlocks.DESTINY_SHUTTLE_COLLISION.get())) {
                        level.removeBlock(target, false);
                    }
                }
                BlockPos seat = DestinyPilotSeatBlock.lowerPos(base, facing);
                for (int layer = 0; layer < 2; layer++) {
                    BlockPos target = seat.above(layer);
                    BlockState seatState = level.getBlockState(target);
                    if (seatState.is(ModBlocks.DESTINY_PILOT_SEAT.get())
                            && seatState.getValue(DestinyPilotSeatBlock.FACING) == facing
                            && seatState.getValue(DestinyPilotSeatBlock.LAYER) == layer)
                        level.removeBlock(target, false);
                }
                for (int forward : PASSENGER_SEAT_FORWARDS) {
                    for (int side : new int[]{-1, 1}) {
                        BlockPos upper = base.relative(facing.getClockWise(), side).relative(facing, forward).above(2);
                        BlockState seatState = level.getBlockState(upper);
                        if (seatState.is(ModBlocks.DESTINY_SHUTTLE_COLLISION.get())
                                && seatState.getValue(DestinyShuttleCollisionBlock.FACING) == facing
                                && seatState.getValue(DestinyShuttleCollisionBlock.SHAPE_ID)
                                == DestinyShuttleCollisionBlock.PASSENGER_SEAT_BASE + (side < 0 ? 3 : 4))
                            level.removeBlock(upper, false);
                    }
                }
            } finally {
                REMOVING.set(false);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    private record CollisionOffset(int x, int y, int z, int shapeId) {
        private CollisionOffset(int x, int y, int z) { this(x, y, z, 0); }
    }
}
