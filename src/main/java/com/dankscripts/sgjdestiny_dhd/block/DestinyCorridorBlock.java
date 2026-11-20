package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.block_entity.DestinyCorridorBlockEntity;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlockEntities;
import com.dankscripts.sgjdestiny_dhd.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** The visible four-bay corridor is rendered from its left front bulkhead. */
public final class DestinyCorridorBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    private static final int WIDTH = 5;
    private static final int HEIGHT = 4;
    private static final int LENGTH = 14;
    private static final int PREVIOUS_FAR_ROW = 14;

    public DestinyCorridorBlock() {
        super(BlockBehaviour.Properties.of().strength(5.0F, 12.0F)
                .sound(SoundType.METAL).noOcclusion().noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    private static BlockPos at(BlockPos anchor, Direction forward, int x, int y, int z) {
        return anchor.relative(forward.getCounterClockWise(), x).relative(forward, z).above(y);
    }

    private static int collisionPart(int x, int y, int z) {
        if (y == 0 && z == 0 && x == 2)
            return DestinyCorridorCollisionBlock.ENTRANCE_HANDLE;
        if (z == 0 && x >= 1 && x <= 3 && y <= 2) return -1; // clear wall opening
        if (x == 0 || x == WIDTH - 1) return DestinyCorridorCollisionBlock.WALL;
        if (y == HEIGHT - 1 && (x == 1 || x == 3) && isLampBay(z))
            return DestinyCorridorCollisionBlock.ROOF_LIGHT;
        if (y == HEIGHT - 1) return DestinyCorridorCollisionBlock.ROOF;
        if (z == 0 && y == HEIGHT - 1) return DestinyCorridorCollisionBlock.WALL;
        if (y == 0) return DestinyCorridorCollisionBlock.FLOOR;
        if (y == 1 && z == 1 && x == 3)
            return DestinyCorridorCollisionBlock.CONTROL_MOUNT_RIGHT;
        if (y == 1 && z == LENGTH - 2 && x == 1)
            return DestinyCorridorCollisionBlock.CONTROL_MOUNT_LEFT;
        if (x == 1) return DestinyCorridorCollisionBlock.LEFT_SHOULDER;
        if (x == 3) return DestinyCorridorCollisionBlock.RIGHT_SHOULDER;
        return -1;
    }

    private static boolean isLampBay(int z) {
        return z == 1 || z == 4 || z == 8 || z == 11;
    }

    private static boolean isControlBay(int z) {
        return z == 1 || z == LENGTH - 2 || z == LENGTH - 1;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction forward = context.getHorizontalDirection();
        BlockPos origin = context.getClickedPos();
        for (int z = 0; z < LENGTH; z++)
            for (int y = 0; y < HEIGHT; y++)
                for (int x = 0; x < WIDTH; x++) {
                    BlockPos target = at(origin, forward, x, y, z);
                    if (!context.getLevel().getWorldBorder().isWithinBounds(target)) {
                        placementBlocked(context, target, "world boundary");
                        return null;
                    }
                    BlockState occupied = context.getLevel().getBlockState(target);
                    if (!occupied.canBeReplaced(context)) {
                        placementBlocked(context, target, occupied.getBlock().getName().getString());
                        return null;
                    }
                }
        return defaultBlockState().setValue(FACING, forward);
    }

    private static void placementBlocked(BlockPlaceContext context, BlockPos target, String reason) {
        if (!context.getLevel().isClientSide() && context.getPlayer() != null)
            context.getPlayer().displayClientMessage(Component.literal(
                    "Corridor needs a clear 5 x 4 x 14 area. Blocked at "
                            + target.toShortString() + " by " + reason + "."), true);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction forward = state.getValue(FACING);
        BlockState collision = ModBlocks.DESTINY_CORRIDOR_COLLISION.get().defaultBlockState()
                .setValue(DestinyCorridorCollisionBlock.FACING, forward);
        for (int z = 0; z < LENGTH; z++)
            for (int y = 0; y < HEIGHT; y++)
                for (int x = 0; x < WIDTH; x++) {
                    BlockPos target = at(pos, forward, x, y, z);
                    if (target.equals(pos)) continue;
                    int part = collisionPart(x, y, z);
                    if (part >= 0)
                        level.setBlock(target, collision.setValue(DestinyCorridorCollisionBlock.PART, part),
                                Block.UPDATE_ALL);
                }
        // Leave the entrance clear. A normal ship airlock can be placed
        // separately in the existing 3 x 3 Minecraft wall opening.
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DestinyCorridorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.DESTINY_CORRIDOR.get(),
                        DestinyCorridorBlockEntity::serverTick);
    }

    /** Remove old built-in doors and replace oversized helper shells in test worlds. */
    public void rebuildLegacyFootprint(Level level, BlockPos pos, BlockState state) {
        Direction forward = state.getValue(FACING);
        BlockState oldDoor = level.getBlockState(at(pos, forward, 2, 0, 0));
        if (!oldDoor.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())
                || !oldDoor.getValue(DestinyShipAirlockBlock.EMBEDDED)
                || oldDoor.getValue(DestinyShipAirlockBlock.COLUMN) != 0) {
            removeEmbeddedDoor(level, at(pos, forward, 1, 0, 0));
            releasePreviousFarRow(level, pos, forward);
            refreshRoofLights(level, pos, forward);
            refreshControlMounts(level, pos, forward);
            refreshEntranceHandle(level, pos, forward);
            return;
        }
        level.removeBlock(at(pos, forward, 2, 0, 0), false);
        for (BlockPos control : new BlockPos[]{at(pos, forward, 1, 1, 0), at(pos, forward, 4, 1, 1)})
            if (level.getBlockState(control).is(ModBlocks.DESTINY_DOOR_CONTROLLER.get()))
                level.removeBlock(control, false);
        for (int z = 0; z < 18; z++)
            for (int y = 0; y < 5; y++)
                for (int x = 0; x < 7; x++) {
                    BlockPos target = at(pos, forward, x, y, z);
                    if (level.getBlockState(target).is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get()))
                        level.removeBlock(target, false);
                }
        setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
    }

    /** Free the row beyond the new tube end in worlds that placed the longer corridor. */
    private static void releasePreviousFarRow(Level level, BlockPos pos, Direction forward) {
        BlockPos oldControl = at(pos, forward, 1, 1, PREVIOUS_FAR_ROW);
        BlockState oldState = level.getBlockState(oldControl);
        BlockState oldFloor = level.getBlockState(at(pos, forward, 2, 0, PREVIOUS_FAR_ROW));
        boolean oldMount = oldState.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())
                && oldState.getValue(DestinyCorridorCollisionBlock.PART)
                == DestinyCorridorCollisionBlock.CONTROL_MOUNT_LEFT;
        boolean oldController = oldState.is(ModBlocks.DESTINY_DOOR_CONTROLLER.get())
                && oldState.getValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET) == 2;
        boolean oldFloorCell = oldFloor.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())
                && oldFloor.getValue(DestinyCorridorCollisionBlock.PART)
                == DestinyCorridorCollisionBlock.FLOOR;
        if (!oldMount && !oldController && !oldFloorCell) return;
        if (oldController) {
            BlockPos newControl = at(pos, forward, 1, 1, LENGTH - 2);
            BlockState destination = level.getBlockState(newControl);
            if (destination.isAir() || destination.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())) {
                level.setBlock(newControl, oldState.setValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET, 1),
                        Block.UPDATE_ALL);
            } else {
                popResource(level, oldControl, new ItemStack(ModItems.DESTINY_DOOR_CONTROLLER.get()));
            }
            level.removeBlock(oldControl, false);
        }
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++) {
                BlockPos target = at(pos, forward, x, y, PREVIOUS_FAR_ROW);
                if (level.getBlockState(target).is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get()))
                    level.removeBlock(target, false);
            }
    }

    /** Move the selectable entrance tile to the doorway in existing corridors. */
    public void refreshEntranceHandle(Level level, BlockPos pos, Direction forward) {
        BlockPos oldHandle = at(pos, forward, 2, 0, 1);
        BlockState oldState = level.getBlockState(oldHandle);
        if (oldState.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())
                && oldState.getValue(DestinyCorridorCollisionBlock.PART)
                == DestinyCorridorCollisionBlock.ENTRANCE_HANDLE)
            level.setBlock(oldHandle, oldState.setValue(DestinyCorridorCollisionBlock.PART,
                    DestinyCorridorCollisionBlock.FLOOR), Block.UPDATE_ALL);
        BlockPos handle = at(pos, forward, 2, 0, 0);
        BlockState current = level.getBlockState(handle);
        if (current.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())) {
            if (current.getValue(DestinyCorridorCollisionBlock.PART)
                    != DestinyCorridorCollisionBlock.ENTRANCE_HANDLE)
                level.setBlock(handle, current.setValue(DestinyCorridorCollisionBlock.PART,
                        DestinyCorridorCollisionBlock.ENTRANCE_HANDLE), Block.UPDATE_ALL);
        } else if (current.isAir()) {
            level.setBlock(handle, ModBlocks.DESTINY_CORRIDOR_COLLISION.get().defaultBlockState()
                    .setValue(DestinyCorridorCollisionBlock.FACING, forward)
                    .setValue(DestinyCorridorCollisionBlock.PART,
                            DestinyCorridorCollisionBlock.ENTRANCE_HANDLE), Block.UPDATE_ALL);
        }
    }

    /** Correct helper facings from ship templates installed before helpers supported rotation. */
    public void refreshCollisionFacings(Level level, BlockPos pos, Direction forward) {
        for (int z = 0; z < LENGTH; z++)
            for (int y = 0; y < HEIGHT; y++)
                for (int x = 0; x < WIDTH; x++) {
                    BlockPos target = at(pos, forward, x, y, z);
                    BlockState current = level.getBlockState(target);
                    if (current.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())
                            && current.getValue(DestinyCorridorCollisionBlock.FACING) != forward)
                        level.setBlock(target, current.setValue(DestinyCorridorCollisionBlock.FACING, forward),
                                Block.UPDATE_CLIENTS);
                }
    }

    /** Existing corridors gain the same ceiling lighting as newly placed ones. */
    private static void refreshRoofLights(Level level, BlockPos pos, Direction forward) {
        for (int z = 0; z < LENGTH; z++) {
            if (!isLampBay(z)) continue;
            for (int x : new int[]{1, 3}) {
                BlockPos roof = at(pos, forward, x, HEIGHT - 1, z);
                BlockState roofState = level.getBlockState(roof);
                if (roofState.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())
                        && roofState.getValue(DestinyCorridorCollisionBlock.PART)
                        != DestinyCorridorCollisionBlock.ROOF_LIGHT)
                    level.setBlock(roof, roofState.setValue(DestinyCorridorCollisionBlock.PART,
                            DestinyCorridorCollisionBlock.ROOF_LIGHT), Block.UPDATE_ALL);
            }
        }
    }

    /** Bring existing corridors' clickable sockets onto their visible backing plates. */
    public void refreshControlMounts(Level level, BlockPos pos, Direction forward) {
        // The far controller's old helper was one cell behind most of its
        // visible model, so rays through the panel missed its hitbox. Shift
        // the block inward and change offset 2 to 1; the rendered model stays
        // in precisely the same world position and retains its saved state.
        BlockPos oldFarControl = at(pos, forward, 1, 1, LENGTH - 1);
        BlockState oldFarState = level.getBlockState(oldFarControl);
        if (oldFarState.is(ModBlocks.DESTINY_DOOR_CONTROLLER.get())
                && oldFarState.getValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET) == 2) {
            BlockPos newFarControl = at(pos, forward, 1, 1, LENGTH - 2);
            BlockState destination = level.getBlockState(newFarControl);
            if (destination.isAir() || destination.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())) {
                BlockState moved = oldFarState.setValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET, 1);
                if (level.setBlock(newFarControl, moved, Block.UPDATE_ALL)) {
                    level.setBlock(oldFarControl, ModBlocks.DESTINY_CORRIDOR_COLLISION.get()
                            .defaultBlockState().setValue(DestinyCorridorCollisionBlock.FACING, forward)
                            .setValue(DestinyCorridorCollisionBlock.PART,
                                    DestinyCorridorCollisionBlock.LEFT_SHOULDER), Block.UPDATE_ALL);
                }
            }
        }
        for (int z : new int[]{1, LENGTH - 2, LENGTH - 1})
            for (int x : new int[]{1, 3}) {
                BlockPos mount = at(pos, forward, x, 1, z);
                BlockState current = level.getBlockState(mount);
                int part = collisionPart(x, 1, z);
                if (current.is(ModBlocks.DESTINY_DOOR_CONTROLLER.get())) {
                    // Bring controllers already placed in test worlds onto the
                    // visible plate without replacing or resetting them.
                    int offset = z == 1 && x == 3 || x == 1 && z == LENGTH - 2 ? 1 : 0;
                    if (offset != 0 && current.getValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET) != offset)
                        level.setBlock(mount, current.setValue(DestinyDoorControllerBlock.CORRIDOR_OFFSET, offset),
                                Block.UPDATE_ALL);
                    continue;
                }
                if (current.is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get())) {
                    if (current.getValue(DestinyCorridorCollisionBlock.PART) != part)
                        level.setBlock(mount, current.setValue(DestinyCorridorCollisionBlock.PART, part),
                                Block.UPDATE_ALL);
                } else if (current.isAir() && (part == DestinyCorridorCollisionBlock.CONTROL_MOUNT_LEFT
                        || part == DestinyCorridorCollisionBlock.CONTROL_MOUNT_RIGHT)) {
                    level.setBlock(mount, ModBlocks.DESTINY_CORRIDOR_COLLISION.get().defaultBlockState()
                            .setValue(DestinyCorridorCollisionBlock.FACING, forward)
                            .setValue(DestinyCorridorCollisionBlock.PART, part), Block.UPDATE_ALL);
                }
            }
    }

    private static void removeEmbeddedDoor(Level level, BlockPos doorAnchor) {
        BlockState door = level.getBlockState(doorAnchor);
        if (door.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())
                && door.getValue(DestinyShipAirlockBlock.EMBEDDED)
                && door.getValue(DestinyShipAirlockBlock.COLUMN) == 0
                && door.getValue(DestinyShipAirlockBlock.ROW) == 0)
            level.removeBlock(doorAnchor, false);
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
            popResource(level, pos, new ItemStack(ModItems.DESTINY_CORRIDOR.get()));
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide() && !state.is(replacement.getBlock())) {
            Direction forward = state.getValue(FACING);
            BlockState oldDoor = level.getBlockState(at(pos, forward, 2, 0, 0));
            boolean legacy = oldDoor.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())
                    && oldDoor.getValue(DestinyShipAirlockBlock.EMBEDDED)
                    && oldDoor.getValue(DestinyShipAirlockBlock.COLUMN) == 0;
            if (!legacy) legacy = level.getBlockState(at(pos, forward, 6, 0, 0))
                    .is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get());
            BlockPos doorAnchor = at(pos, forward, legacy ? 2 : 1, 0, 0);
            BlockState door = level.getBlockState(doorAnchor);
            if (door.is(ModBlocks.DESTINY_SHIP_AIRLOCK.get())
                    && door.getValue(DestinyShipAirlockBlock.EMBEDDED)) level.removeBlock(doorAnchor, false);
            if (legacy)
                for (BlockPos control : new BlockPos[]{at(pos, forward, 1, 1, 0), at(pos, forward, 4, 1, 1)})
                    if (level.getBlockState(control).is(ModBlocks.DESTINY_DOOR_CONTROLLER.get()))
                        level.removeBlock(control, false);
            for (int z = 0; z < (legacy ? 18 : LENGTH); z++)
                for (int y = 0; y < (legacy ? 5 : HEIGHT); y++)
                    for (int x = 0; x < (legacy ? 7 : WIDTH); x++) {
                        BlockPos target = at(pos, forward, x, y, z);
                        if (level.getBlockState(target).is(ModBlocks.DESTINY_CORRIDOR_COLLISION.get()))
                            level.removeBlock(target, false);
                        else if (!legacy && y == 1 && isControlBay(z) && (x == 1 || x == 3)
                                && level.getBlockState(target).is(ModBlocks.DESTINY_DOOR_CONTROLLER.get()))
                            level.destroyBlock(target, true);
                    }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
