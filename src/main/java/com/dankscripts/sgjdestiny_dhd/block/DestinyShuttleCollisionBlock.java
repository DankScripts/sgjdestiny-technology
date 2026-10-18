package com.dankscripts.sgjdestiny_dhd.block;

import com.dankscripts.sgjdestiny_dhd.entity.DestinyPilotSeatEntity;
import com.dankscripts.sgjdestiny_dhd.network.ShuttleTravelNetwork;
import com.dankscripts.sgjdestiny_dhd.registry.ModBlocks;
import com.dankscripts.sgjdestiny_dhd.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Voxelized sections of the visible shuttle hull. */
public final class DestinyShuttleCollisionBlock extends Block {
    public static final DirectionProperty FACING = net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty SHAPE_ID = IntegerProperty.create("shape", 0, 255);
    // 196/201 combine each cabin deck edge with the adjacent inner wall;
    // 197-200 are the four passenger-chair collision/interaction variants.
    public static final int CABIN_RIGHT_DECK_WALL = 196;
    public static final int PASSENGER_SEAT_BASE = 196;
    public static final int CABIN_LEFT_DECK_WALL = 201;
    private static final VoxelShape[][] SHAPES = loadShapes();
    private static final int[] SEAT_FORWARDS = {5, 6, 8, 9, 11, 12, 14, 15};
    private static final int HELM_FORWARD = 21;
    // Exact model bounds: x=-0.16..1.16, y=1.00..1.73, forward=20.96..21.62.
    private static final VoxelShape[] HELM_TARGET = {
            Shapes.empty(), Shapes.empty(),
            Block.box(-2.56, 0, -9.92, 18.56, 13, 0.64), // NORTH
            Block.box(-2.56, 0, 15.36, 18.56, 13, 25.92), // SOUTH
            Block.box(-9.92, 0, -2.56, 0.64, 13, 18.56), // WEST
            Block.box(15.36, 0, -2.56, 25.92, 13, 18.56) // EAST
    };

    public DestinyShuttleCollisionBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(-1.0F, 3_600_000.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE_ID, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE_ID);
    }

    private static VoxelShape[][] loadShapes() {
        VoxelShape[][] shapes = new VoxelShape[Direction.values().length][256];
        String path = "/assets/sgjdestiny_dhd/shuttle_collision_shapes.csv";
        InputStream stream = DestinyShuttleCollisionBlock.class.getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing shuttle collision resource: " + path);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.charAt(0) == '#') continue;
                String[] fields = line.split(",");
                if (fields.length != 7) throw new IOException("Malformed shuttle collision shape: " + line);
                int id = Integer.parseInt(fields[0]);
                if (id < 0 || id >= 256) throw new IOException("Invalid shuttle collision shape id: " + id);
                int x0 = Integer.parseInt(fields[1]), x1 = Integer.parseInt(fields[2]);
                int y0 = Integer.parseInt(fields[3]), y1 = Integer.parseInt(fields[4]);
                int z0 = Integer.parseInt(fields[5]), z1 = Integer.parseInt(fields[6]);
                if (!(0 <= x0 && x0 < x1 && x1 <= 4 && 0 <= y0 && y0 < y1 && y1 <= 4
                        && 0 <= z0 && z0 < z1 && z1 <= 4))
                    throw new IOException("Invalid shuttle collision bounds: " + line);
                shapes[Direction.NORTH.ordinal()][id] = Block.box(x0 * 4, y0 * 4, z0 * 4,
                        x1 * 4, y1 * 4, z1 * 4);
                shapes[Direction.SOUTH.ordinal()][id] = Block.box((4 - x1) * 4, y0 * 4, (4 - z1) * 4,
                        (4 - x0) * 4, y1 * 4, (4 - z0) * 4);
                shapes[Direction.EAST.ordinal()][id] = Block.box((4 - z1) * 4, y0 * 4, x0 * 4,
                        (4 - z0) * 4, y1 * 4, x1 * 4);
                shapes[Direction.WEST.ordinal()][id] = Block.box(z0 * 4, y0 * 4, (4 - x1) * 4,
                        z1 * 4, y1 * 4, (4 - x0) * 4);
            }
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("Cannot read shuttle collision shapes", exception);
        }
        if (shapes[Direction.NORTH.ordinal()][0] == null) throw new IllegalStateException("Empty shuttle collision shapes");
        return shapes;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Players should see the shuttle, not outlines around its collision cells.
        if (helmAnchor(state, level, pos) != null)
            return HELM_TARGET[state.getValue(FACING).ordinal()];
        int seat = seatPart(state);
        return seat == 0 ? Shapes.empty() : seatShape(state.getValue(FACING), seat);
    }

    private static BlockPos helmAnchor(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(SHAPE_ID) != 81) return null;
        Direction facing = state.getValue(FACING);
        BlockPos anchor = pos.relative(facing.getOpposite(), HELM_FORWARD).below();
        BlockState shuttle = level.getBlockState(anchor);
        if (!shuttle.is(ModBlocks.DESTINY_SHUTTLE_AIRLOCK.get())
                || shuttle.getValue(DestinyShuttleAirlockBlock.FACING) != facing
                || shuttle.getValue(DestinyShuttleAirlockBlock.COLUMN) != 0
                || shuttle.getValue(DestinyShuttleAirlockBlock.ROW) != 0) return null;
        return anchor;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        Direction facing = state.getValue(FACING);
        int id = state.getValue(SHAPE_ID);
        if (id == CABIN_RIGHT_DECK_WALL)
            return Shapes.or(SHAPES[facing.ordinal()][32], innerWallShape(facing, false));
        if (id == CABIN_LEFT_DECK_WALL)
            return Shapes.or(SHAPES[facing.ordinal()][144], innerWallShape(facing, true));
        int seat = seatPart(state);
        if (seat >= 3) return seatShape(facing, seat);
        if (seat != 0) return Shapes.or(SHAPES[facing.ordinal()][81], seatShape(facing, seat));
        VoxelShape shape = SHAPES[facing.ordinal()][id];
        return shape == null ? Shapes.block() : shape;
    }

    private static int seatPart(BlockState state) {
        int id = state.getValue(SHAPE_ID);
        return id >= 197 && id <= 200 ? id - PASSENGER_SEAT_BASE : 0;
    }

    /** Quarter-block-thick collision directly behind the passenger seats. */
    private static VoxelShape innerWallShape(Direction facing, boolean left) {
        double x0 = left ? 8 : 4;
        double x1 = left ? 12 : 8;
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, 0, 0, 16 - x0, 16, 16);
            case EAST -> Block.box(0, 0, x0, 16, 16, x1);
            case WEST -> Block.box(0, 0, 16 - x1, 16, 16, 16 - x0);
            default -> Block.box(x0, 0, 0, x1, 16, 16);
        };
    }

    private static VoxelShape seatShape(Direction facing, int seat) {
        boolean left = (seat & 1) != 0;
        double x0 = left ? -3 : 10;
        double x1 = left ? 6 : 19;
        double y0 = seat <= 2 ? 7 : 0;
        double y1 = seat <= 2 ? 12 : 11;
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, y0, 0, 16 - x0, y1, 16);
            case EAST -> Block.box(0, y0, x0, 16, y1, x1);
            case WEST -> Block.box(0, y0, 16 - x1, 16, y1, 16 - x0);
            default -> Block.box(x0, y0, 0, x1, y1, 16);
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND) {
            BlockPos helm = helmAnchor(state, level, pos);
            if (helm != null && player.getVehicle() instanceof DestinyPilotSeatEntity pilot
                    && pilot.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(
                            DestinyPilotSeatBlock.lowerPos(helm, state.getValue(FACING)))) < 4.0) {
                if (level.isClientSide()) return InteractionResult.SUCCESS;
                if (player instanceof ServerPlayer serverPlayer
                        && pilot.isPilotAt(helm, state.getValue(FACING)))
                    ShuttleTravelNetwork.open(serverPlayer, helm);
                return InteractionResult.CONSUME;
            }
        }
        int seatPart = seatPart(state);
        if (hand != InteractionHand.MAIN_HAND || seatPart == 0) return InteractionResult.PASS;
        Direction facing = state.getValue(FACING);
        Direction right = facing.getClockWise();
        int side = (seatPart & 1) != 0 ? -1 : 1;
        BlockPos lower = pos.below(seatPart >= 3 ? 1 : 0);
        for (int forward : SEAT_FORWARDS) {
            BlockPos anchor = lower.below().relative(right, -side).relative(facing.getOpposite(), forward);
            BlockState airlock = level.getBlockState(anchor);
            if (!(airlock.getBlock() instanceof DestinyShuttleAirlockBlock)
                    || airlock.getValue(DestinyShuttleAirlockBlock.FACING) != facing
                    || airlock.getValue(DestinyShuttleAirlockBlock.COLUMN) != 0
                    || airlock.getValue(DestinyShuttleAirlockBlock.ROW) != 0) continue;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            double offset = side < 0 ? -0.4 : 0.4;
            double x = lower.getX() + 0.5 + right.getStepX() * offset;
            double z = lower.getZ() + 0.5 + right.getStepZ() * offset;
            // Passenger cushions top out near 0.66 above this block's base;
            // the pilot's taller cushion uses its own mount height.
            double y = lower.getY() + 0.66;
            AABB nearby = new AABB(x - 0.35, y - 0.5, z - 0.35, x + 0.35, y + 0.5, z + 0.35);
            if (!level.getEntitiesOfClass(DestinyPilotSeatEntity.class, nearby).isEmpty())
                return InteractionResult.CONSUME;
            DestinyPilotSeatEntity seat = ModEntities.PILOT_SEAT.get().create(level);
            if (seat == null) return InteractionResult.FAIL;
            seat.setAnchor(anchor, facing);
            seat.setDismountPos(anchor.relative(facing, forward).above());
            seat.setPos(x, y, z);
            Direction inward = side < 0 ? right : right.getOpposite();
            seat.setYRot(inward.toYRot());
            if (!level.addFreshEntity(seat) || !player.startRiding(seat, true)) {
                seat.discard();
                return InteractionResult.FAIL;
            }
            player.setYRot(inward.toYRot());
            player.setYHeadRot(inward.toYRot());
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
